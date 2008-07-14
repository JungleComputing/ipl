/* $Id: $ */

package ibis.ipl.impl.multi;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.util.ThreadPool;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

public class MultiReceivePort implements ReceivePort {

    private Logger logger = Logger.getLogger(MultiReceivePort.class);

    private final Map<String, ReceivePort>subPortMap = Collections.synchronizedMap(new HashMap<String, ReceivePort>());

    private final MultiReceivePortIdentifier id;

    private final ManageableMapper ManageableMapper;

    private final PortType portType;

    private final ArrayList<MultiReadMessage>messageQueue = new ArrayList<MultiReadMessage>();
    private final ArrayList<IOException>exceptionQueue = new ArrayList<IOException>();

    private final ArrayList<DowncallHandler>handlers = new ArrayList<DowncallHandler>();

    private final MultiIbis ibis;

    private final class DowncallHandler implements Runnable {

        private final ReceivePort subPort;

        boolean poll = false;
        boolean receive = false;
        long timeout = 0;
        boolean quit = false;

        public DowncallHandler(ReceivePort subPort) {
            this.subPort = subPort;
        }

        public void run() {
            while (!quit) {
                ReadMessage message = null;
                IOException exception = null;
                if (poll) {
                    try {
                        message = subPort.poll();
                    }
                    catch (IOException e) {
                        exception = e;
                    }
                    poll = false;
                }
                else if (receive) {
                    try {
                        if (timeout > 0) {
                            message = subPort.receive(timeout);
                        }
                        else {
                            message = subPort.receive();
                        }
                    } catch (IOException e) {
                        exception = e;
                    }
                    receive = false;
                }
                if (message != null || exception != null) {
                    synchronized (messageQueue) {
                        if (message != null) {
                            messageQueue.add(new MultiReadMessage(message, ibis.receivePortMap.get(subPort)));
                            messageQueue.notify();
                        }
                        else if (exception != null) {
                            exceptionQueue.add(exception);
                            messageQueue.notify();
                        }
                    }
                }
                synchronized (this) {
                    try {
                        this.wait();
                    }
                    catch (InterruptedException e) {
                        // Ignored
                    }
                }
            }
        }
    }

    /**
     * This class forwards upcalls with the proper receive port.
     */
    private final class ConnectUpcaller
            implements ReceivePortConnectUpcall {
        final MultiReceivePort port;
        final ReceivePortConnectUpcall upcaller;
        final String ibisName;

        public ConnectUpcaller(String ibisName, MultiReceivePort port,
                ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
            this.ibisName = ibisName;
        }

        public boolean gotConnection(ReceivePort me,
                SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        public void lostConnection(ReceivePort me,
                SendPortIdentifier johnDoe, Throwable reason) {
            try {
                upcaller.lostConnection(port, ibis.mapSendPortIdentifier(johnDoe, ibisName), reason);
            } catch (IOException e) {
                // TODO What the hell to do here?
            }
        }
    }

    /**
     * This class forwards message upcalls with the proper message.
     */
    private static final class Upcaller implements MessageUpcall {
        MessageUpcall upcaller;
        MultiReceivePort port;

        public Upcaller(MessageUpcall upcaller, MultiReceivePort port) {
            this.upcaller = upcaller;
            this.port = port;
        }

        public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
            upcaller.upcall(new MultiReadMessage(m, port));
        }
    }

    @SuppressWarnings("unchecked")
    public MultiReceivePort(PortType type, MultiIbis ibis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties)
            throws IOException {
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }
        this.id = new MultiReceivePortIdentifier(ibis.identifier(), name);

        for (String ibisName:ibis.subIbisMap.keySet()) {
            Ibis subIbis = ibis.subIbisMap.get(ibisName);
            ConnectUpcaller upcaller = null;
            if (connectUpcall != null) {
                upcaller = new ConnectUpcaller(ibisName, this, connectUpcall);
            }
            ReceivePort subPort = subIbis.createReceivePort(type, name, upcall, upcaller, properties);
            subPortMap.put(ibisName, subPort);
            ibis.receivePortMap.put(subPort, this);
            id.addSubId(ibisName, subPort.identifier());
            DowncallHandler handler = new DowncallHandler(subPort);
            handlers.add(handler);
            ThreadPool.createNew(handler, "ReceivePort: " + ibisName + ":" + name);
        }

        this.ManageableMapper = new ManageableMapper((Map)subPortMap);
        this.portType = type;
        this.ibis = ibis;
    }

    private void quit(List<DowncallHandler> handlers) {
        for (DowncallHandler handler: handlers) {
            synchronized(handler) {
                handler.quit = true;
                handler.notify();
            }
        }
    }

    public synchronized void close() throws IOException {
        for(ReceivePort port:subPortMap.values()) {
            try {
                port.close();
            }
            catch (IOException e) {
                // TODO Bundle up exceptions
            }
        }
        quit(handlers);
    }

    public synchronized void close(long timeoutMillis) throws IOException {
        // TODO Should we kick off threads to close with timeout?
        long timeout = timeoutMillis / subPortMap.size();
        for (ReceivePort port:subPortMap.values()) {
            try {
                port.close(timeout);
            }
            catch (IOException e) {
                // TODO Bundle up exceptions
            }
        }
        quit(handlers);
    }

    public synchronized SendPortIdentifier[] connectedTo() {
        HashMap<SendPortIdentifier, String>connectedTo = new HashMap<SendPortIdentifier,String>();
        for (String ibisName:subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.connectedTo();
            for (int i=0; i<ids.length;i++) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(ids[i], ibisName), ibisName);
                } catch (IOException e) {
                    // TODO What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    public synchronized void disableConnections() {
        for (ReceivePort port:subPortMap.values()) {
            port.disableConnections();
        }
    }

    public synchronized void disableMessageUpcalls() {
        for (ReceivePort port:subPortMap.values()) {
            port.disableMessageUpcalls();
        }
    }

    public synchronized void enableConnections() {
        for (ReceivePort port:subPortMap.values()) {
            port.enableConnections();
        }
    }

    public synchronized void enableMessageUpcalls() {
        for (ReceivePort port:subPortMap.values()) {
            port.enableMessageUpcalls();
        }
    }

    public synchronized PortType getPortType() {
        return portType;
    }

    public ReceivePortIdentifier identifier() {
        return id;
    }

    public synchronized SendPortIdentifier[] lostConnections() {
        HashMap<SendPortIdentifier, String>connectedTo = new HashMap<SendPortIdentifier,String>();
        for (String ibisName:subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.lostConnections();
            for (int i=0; i<ids.length;i++) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(ids[i], ibisName), ibisName);
                }
                catch (IOException e) {
                    // TODO: What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    public String name() {
        return id.name();
    }

    public synchronized SendPortIdentifier[] newConnections() {
        HashMap<SendPortIdentifier, String>connectedTo = new HashMap<SendPortIdentifier,String>();
        for (String ibisName:subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.newConnections();
            for (int i=0; i<ids.length;i++) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(ids[i], ibisName), ibisName);
                } catch (IOException e) {
                    // TODO What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    public synchronized ReadMessage poll() throws IOException {
        synchronized (messageQueue) {
            if (messageQueue.size() == 0) {
                for (DowncallHandler handler:handlers) {
                    synchronized(handler) {
                        handler.poll = true;
                        handler.notify();
                    }
                }
                do {
                    try {
                        messageQueue.wait();
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
                while (messageQueue.size() == 0 && exceptionQueue.size() == 0);
                if (messageQueue.size() == 0) {
                    throw exceptionQueue.get(0);
                }
            }
            return messageQueue.remove(0);
        }
    }

    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    public synchronized ReadMessage receive(long timeoutMillis) throws IOException {
        synchronized (messageQueue) {
            if (messageQueue.size() == 0) {
                for (DowncallHandler handler:handlers) {
                    synchronized(handler) {
                        handler.receive = true;
                        handler.timeout = timeoutMillis;
                        handler.notify();
                    }
                }
                do {
                    try {
                        messageQueue.wait();
                    }
                    catch (InterruptedException e) {
                        // Ignore
                    }
                }
                while (messageQueue.size() == 0 && exceptionQueue.size() == 0);
                if (messageQueue.size() == 0) {
                    throw exceptionQueue.get(0);
                }
            }
            return messageQueue.remove(0);
        }
    }

    public String getManagementProperty(String key)
    throws NoSuchPropertyException {
        return ManageableMapper.getManagementProperty(key);
    }

    public Map<String, String> managementProperties() {
        return ManageableMapper.managementProperties();
    }

    public void printManagementProperties(PrintStream stream) {
        ManageableMapper.printManagementProperties(stream);
    }

    public void setManagementProperties(Map<String, String> properties)
    throws NoSuchPropertyException {
        ManageableMapper.setManagementProperties(properties);
    }

    public void setManagementProperty(String key, String value)
    throws NoSuchPropertyException {
        ManageableMapper.setManagementProperty(key, value);
    }
}
