/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.impl.multi;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ibis.ipl.Ibis;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.util.ThreadPool;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

public class MultiReceivePort implements ReceivePort {

    // private Logger logger = LoggerFactory.getLogger(MultiReceivePort.class);

    private final Map<String, ReceivePort> subPortMap = Collections.synchronizedMap(new HashMap<String, ReceivePort>());

    private final MultiReceivePortIdentifier id;

    private final ManageableMapper ManageableMapper;

    private final PortType portType;

    private final ArrayList<MultiReadMessage> messageQueue = new ArrayList<>();
    private final ArrayList<IOException> exceptionQueue = new ArrayList<>();

    private final ArrayList<DowncallHandler> handlers = new ArrayList<>();

    private final MultiIbis ibis;

    private boolean handlersStarted;

    private final class DowncallHandler implements Runnable {

        private final ReceivePort subPort;
        private final String ibisName;

        boolean running = false;

        public DowncallHandler(ReceivePort subPort, String ibisName) {
            this.subPort = subPort;
            this.ibisName = ibisName;
        }

        @Override
        public void run() {
            running = true;
            while (running) {
//                logger.debug("Reading message");
                ReadMessage message = null;
                IOException exception = null;

                try {
                    message = subPort.receive();
                } catch (IOException e) {
                    exception = e;
                }
//                logger.debug("Run Locking Message Queue");
                synchronized (messageQueue) {
//                    logger.debug("Run setting result: " + message + " : " + exception);
                    if (message != null || exception != null) {
                        if (message != null) {
                            messageQueue.add(new MultiReadMessage(message, ibis.receivePortMap.get(subPort)));
                        } else if (exception != null) {
                            exceptionQueue.add(exception);
                        } else {
                            exceptionQueue.add(new ReceiveTimedOutException("Timeout waiting for message."));
                        }
//                        logger.debug("Run notifying");
                        messageQueue.notifyAll();
                    }
                }
            }
//            logger.debug("Handler exiting");
        }
    }

    /**
     * This class forwards upcalls with the proper receive port.
     */
    private final class ConnectUpcaller implements ReceivePortConnectUpcall {
        final MultiReceivePort port;
        final ReceivePortConnectUpcall upcaller;
        final String ibisName;

        public ConnectUpcaller(String ibisName, MultiReceivePort port, ReceivePortConnectUpcall upcaller) {
            this.port = port;
            this.upcaller = upcaller;
            this.ibisName = ibisName;
        }

        @Override
        public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
            return upcaller.gotConnection(port, applicant);
        }

        @Override
        public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe, Throwable reason) {
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

        @Override
        public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
            upcaller.upcall(new MultiReadMessage(m, port));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MultiReceivePort(PortType type, MultiIbis ibis, String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties) throws IOException {

        this.id = new MultiReceivePortIdentifier(ibis.identifier(), name);

        // Wrap the upcaller if there is one
        if (upcall != null) {
            upcall = new Upcaller(upcall, this);
        }

        for (String ibisName : ibis.subIbisMap.keySet()) {
            Ibis subIbis = ibis.subIbisMap.get(ibisName);
            ConnectUpcaller upcaller = null;
            if (connectUpcall != null) {
                upcaller = new ConnectUpcaller(ibisName, this, connectUpcall);
            }
            ReceivePort subPort = subIbis.createReceivePort(type, name, upcall, upcaller, properties);
            subPortMap.put(ibisName, subPort);
            ibis.receivePortMap.put(subPort, this);
            id.addSubId(ibisName, subPort.identifier());
            if (type.hasCapability(PortType.RECEIVE_EXPLICIT) || type.hasCapability(PortType.RECEIVE_POLL)
                    || type.hasCapability(PortType.RECEIVE_POLL_UPCALLS) || type.hasCapability(PortType.RECEIVE_TIMEOUT)) {
                DowncallHandler handler = new DowncallHandler(subPort, ibisName);
                handlers.add(handler);
            }
        }

        this.ManageableMapper = new ManageableMapper((Map) subPortMap);
        this.portType = type;
        this.ibis = ibis;
    }

    @Override
    public synchronized void close() throws IOException {
        for (DowncallHandler handler : handlers) {
            handler.running = false;
            // TODO connect to wake? Or will close wake?
        }
        for (ReceivePort port : subPortMap.values()) {
            try {
                port.close();
            } catch (IOException e) {
                // TODO Bundle up exceptions
            }
        }
        ibis.closeReceivePort(this);
    }

    @Override
    public synchronized void close(long timeoutMillis) throws IOException {
        // TODO Should we kick off threads to close with timeout?
        long timeout = timeoutMillis / subPortMap.size();
        for (ReceivePort port : subPortMap.values()) {
            try {
                port.close(timeout);
            } catch (IOException e) {
                // TODO Bundle up exceptions
            }
        }
    }

    @Override
    public synchronized SendPortIdentifier[] connectedTo() {
        HashMap<SendPortIdentifier, String> connectedTo = new HashMap<>();
        for (String ibisName : subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.connectedTo();
            for (SendPortIdentifier id2 : ids) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(id2, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    @Override
    public synchronized void disableConnections() {
        for (ReceivePort port : subPortMap.values()) {
            port.disableConnections();
        }
    }

    @Override
    public synchronized void disableMessageUpcalls() {
        for (ReceivePort port : subPortMap.values()) {
            port.disableMessageUpcalls();
        }
    }

    @Override
    public synchronized void enableConnections() {
        for (ReceivePort port : subPortMap.values()) {
            port.enableConnections();
        }
    }

    @Override
    public synchronized void enableMessageUpcalls() {
        for (ReceivePort port : subPortMap.values()) {
            port.enableMessageUpcalls();
        }
    }

    @Override
    public synchronized PortType getPortType() {
        return portType;
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return id;
    }

    @Override
    public synchronized SendPortIdentifier[] lostConnections() {
        HashMap<SendPortIdentifier, String> connectedTo = new HashMap<>();
        for (String ibisName : subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.lostConnections();
            for (SendPortIdentifier id2 : ids) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(id2, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO: What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    @Override
    public String name() {
        return id.name();
    }

    @Override
    public synchronized SendPortIdentifier[] newConnections() {
        HashMap<SendPortIdentifier, String> connectedTo = new HashMap<>();
        for (String ibisName : subPortMap.keySet()) {
            ReceivePort port = subPortMap.get(ibisName);
            SendPortIdentifier[] ids = port.newConnections();
            for (SendPortIdentifier id2 : ids) {
                try {
                    connectedTo.put(ibis.mapSendPortIdentifier(id2, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO What the hell to do here?
                }
            }
        }
        return connectedTo.keySet().toArray(new SendPortIdentifier[connectedTo.size()]);
    }

    @Override
    public synchronized ReadMessage poll() throws IOException {
        ReadMessage result = null;
        synchronized (messageQueue) {
            if (messageQueue.size() == 0) {
                // Poll all subports for fairness
                for (ReceivePort port : subPortMap.values()) {
                    ReadMessage message = port.poll();
                    if (message != null) {
                        messageQueue.add(new MultiReadMessage(message, this));
                    }
                }
            }
            if (messageQueue.size() > 0) {
                result = messageQueue.remove(0);
            }
        }
        return result;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    @Override
    public synchronized ReadMessage receive(long timeoutMillis) throws IOException {
        if (handlers.size() == 0) {
            throw new IOException("Downcalls Not Configured!");
        }
//        logger.debug("> receive");
        // TODO: What is the cost of this lazy init?
        if (!handlersStarted) {
//            logger.debug("Starting handlers.");
            for (DowncallHandler handler : handlers) {
                ThreadPool.createNew(handler, "Handler Thread: " + handler.ibisName);
            }
            handlersStarted = true;
        }
        ReadMessage ret = null;
        synchronized (messageQueue) {
//            logger.debug(">> messageQueue locked");
            if (messageQueue.size() == 0) {
                do {
                    try {
//                        logger.debug("Waiting for message.");
                        if (timeoutMillis > 0) {
                            messageQueue.wait(timeoutMillis);
                        } else {
                            messageQueue.wait();
                        }
//                        logger.debug("Woke from wait.");
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                } while (messageQueue.size() == 0 && exceptionQueue.size() == 0);
                if (messageQueue.size() == 0) {
//                    logger.debug("Throwing exception!");
                    IOException e = exceptionQueue.remove(0);
                    exceptionQueue.clear();
                    throw e; // Added (nothing was done with it) --Ceriel
                }
            }
            ret = messageQueue.remove(0);
        }
//        logger.debug("< receive");
        return ret;
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        return ManageableMapper.getManagementProperty(key);
    }

    @Override
    public Map<String, String> managementProperties() {
        return ManageableMapper.managementProperties();
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        ManageableMapper.printManagementProperties(stream);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        ManageableMapper.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        ManageableMapper.setManagementProperty(key, value);
    }
}
