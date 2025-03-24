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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.ConnectionsFailedException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.util.ThreadPool;

public class MultiSendPort implements SendPort {

    /** Debugging output. */
    private static final Logger logger = LoggerFactory.getLogger(MultiSendPort.class);

    private final ManageableMapper ManageableMapper;

    private final HashMap<String, SendPort> subPortMap = new HashMap<>();

    private ArrayList<DowncallHandler> handlers = new ArrayList<>();

    private final PortType portType;

    private final MultiSendPortIdentifier id;

    private final String name;

    private final MultiIbis ibis;

    private SendPort activeSendPort;
    private String activeIbisName;

    private final ArrayList<ReceivePortIdentifier> idQueue = new ArrayList<>();
    private final ArrayList<IOException> errorQueue = new ArrayList<>();

    private int handlerCount;

    private final class DowncallHandler implements Runnable {

        private static final int OPP_NOOP = -1;
        private static final int OPP_QUIT = 0;
        private static final int OPP_CONNECT_RPID_ARRAY = 1;
        private static final int OPP_CONNECT_IID_NAME_MAP = 2;
        private static final int OPP_CONNECT_RPID = 3;
        private static final int OPP_CONNECT_IID_NAME = 4;

        private final SendPort subPort;
        private final String ibisName;

        int opcode = OPP_NOOP;

        ReceivePortIdentifier[] rpids;
        Map<IbisIdentifier, String> iidMap;
        IbisIdentifier id;
        String name;
        ReceivePortIdentifier rpid;
        long timeout;
        boolean fillTimeout;

        public DowncallHandler(SendPort subPort, String ibisName) {
            this.subPort = subPort;
            this.ibisName = ibisName;
        }

        private boolean setActive() {
            boolean set = false;
            synchronized (idQueue) {
                if (activeSendPort == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Setting active SendPort port: " + subPort);
                    }
                    activeSendPort = subPort;
                    activeIbisName = ibisName;
                    idQueue.notifyAll();
                    set = true;
                } else {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Closing Inactive SendPort port: " + subPort);
                        }
                        subPort.close();
                    } catch (IOException e) {
                        // Ignored
                    }
                }
                opcode = OPP_NOOP;
            }
            return set;
        }

        private boolean setActive(ReceivePortIdentifier portId) {
            synchronized (idQueue) {
                boolean ret = setActive();
                if (ret) {
                    idQueue.add(portId);
                }
                return ret;
            }
        }

        private void setError(IOException e) {
            synchronized (idQueue) {
                if (activeSendPort == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Got Error While Connecting SendPort: " + subPort + " : " + e.getMessage());
                    }
                    errorQueue.add(e);
                    handlerCount++;
                    if (handlerCount >= handlers.size()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Notifying due to all handlers being done.");
                        }
                        idQueue.notifyAll();
                    }
                }
                opcode = OPP_NOOP;
            }
        }

        @Override
        public void run() {
            while (opcode != OPP_QUIT) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Running:" + opcode + " for: " + subPort);
                }
                switch (opcode) {
                case OPP_NOOP:
                    synchronized (idQueue) {
                        // Check again to make sure it didn't change while we locked.
                        // This makes sure we hold the idQueue lock as little as possible
                        if (opcode == OPP_NOOP) {
                            try {
                                logger.debug("Handler waiting.");
                                idQueue.wait();
                            } catch (InterruptedException e) {
                                // Ignored
                            }
                        }
                        if (activeSendPort != null) {
                            logger.debug("We got beat to connect!");
                            opcode = OPP_NOOP;
                        }
                    }
                    break;
                case OPP_QUIT:
                    break;
                case OPP_CONNECT_RPID_ARRAY:
                    try {
                        for (int i = 0; i < rpids.length; i++) {
                            rpids[i] = ((MultiReceivePortIdentifier) rpids[i]).getSubId(ibisName);
                        }
                        subPort.connect(rpids, timeout, fillTimeout);
                        setActive();
                    } catch (ConnectionsFailedException e) {
                        setError(e);
                    }
                    break;
                case OPP_CONNECT_IID_NAME_MAP:
                    try {
                        HashMap<IbisIdentifier, String> ids = new HashMap<>();
                        for (IbisIdentifier id : iidMap.keySet()) {
                            MultiIbisIdentifier mid = (MultiIbisIdentifier) id;
                            ids.put(mid.subIdForIbis(ibisName), iidMap.get(id));
                        }
                        ReceivePortIdentifier[] portId = subPort.connect(ids, timeout, fillTimeout);
                        // TODO: Should add all of them.
                        idQueue.add(portId[0]);
                        setActive();
                    } catch (ConnectionsFailedException e) {
                        errorQueue.add(e);
                    }
                    break;
                case OPP_CONNECT_RPID:
                    try {
                        rpid = ((MultiReceivePortIdentifier) rpid).getSubId(ibisName);
                        subPort.connect(rpid, timeout, fillTimeout);
                        setActive();
                    } catch (ConnectionFailedException e) {
                        errorQueue.add(e);
                    }
                    break;
                case OPP_CONNECT_IID_NAME:
                    try {
                        MultiIbisIdentifier mid = (MultiIbisIdentifier) id;
                        ReceivePortIdentifier portId = subPort.connect(mid.subIdForIbis(ibisName), name, timeout, fillTimeout);
                        setActive(portId);
                    } catch (ConnectionFailedException e) {
                        errorQueue.add(e);
                    }
                    break;
                }
            }
        }
    }

    private final class DisconnectUpcaller implements SendPortDisconnectUpcall {
        SendPortDisconnectUpcall upcaller;
        String ibisName;

        public DisconnectUpcaller(String ibisName, MultiSendPort port, SendPortDisconnectUpcall upcaller) {
            this.upcaller = upcaller;
            this.ibisName = ibisName;
        }

        @Override
        public void lostConnection(SendPort me, ReceivePortIdentifier johnDoe, Throwable reason) {
            if (logger.isDebugEnabled()) {
                logger.debug("Passing lost connection along: " + me + " : " + johnDoe + " : " + reason.getMessage());
            }
            try {
                upcaller.lostConnection(ibis.sendPortMap.get(me), ibis.mapReceivePortIdentifier(johnDoe, ibisName), reason);
            } catch (IOException e) {
                // TODO What the hell to do here?
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public MultiSendPort(PortType type, MultiIbis ibis, String name, SendPortDisconnectUpcall connectUpcall, Properties props) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("Constructing MultiSendPort");
        }

        for (String ibisName : ibis.subIbisMap.keySet()) {
            Ibis subIbis = ibis.subIbisMap.get(ibisName);
            DisconnectUpcaller upcaller = null;
            if (connectUpcall != null) {
                upcaller = new DisconnectUpcaller(ibisName, this, connectUpcall);
            }
            SendPort subPort = subIbis.createSendPort(type, name, upcaller, props);
            DowncallHandler handler = new DowncallHandler(subPort, ibisName);
            handlers.add(handler);
            subPortMap.put(ibisName, subPort);
            ibis.sendPortMap.put(subPort, this);
            ThreadPool.createNew(handler, "Connect Handler: " + ibisName);
        }
        ManageableMapper = new ManageableMapper((Map) subPortMap);
        this.portType = type;
        this.id = new MultiSendPortIdentifier(ibis.identifier(), name);
        this.name = name;
        this.ibis = ibis;
    }

    @Override
    public void close() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Closing port: " + this);
        }
        for (SendPort port : subPortMap.values()) {
            try {
                port.close();
            } catch (IOException e) {
                // TODO Bundle up exceptions
            }
        }
        ibis.closeSendPort(this);
    }

    @Override
    public void connect(ReceivePortIdentifier receiver) throws ConnectionFailedException {
        connect(receiver, 0L, true);
    }

    @Override
    public synchronized void connect(ReceivePortIdentifier receiver, long timeoutMillis, boolean fillTimeout) throws ConnectionFailedException {
        synchronized (idQueue) {
            logger.debug("Attempting to connect.");
            errorQueue.clear();
            idQueue.clear();
            handlerCount = 0;
            if (activeSendPort == null) {
                logger.debug("No active sendport.");
                for (DowncallHandler handler : handlers) {
                    handler.timeout = timeoutMillis;
                    handler.fillTimeout = fillTimeout;
                    handler.opcode = DowncallHandler.OPP_CONNECT_RPID;
                    handler.rpid = receiver;
                }
                idQueue.notifyAll();
                while (activeSendPort == null && handlerCount < handlers.size()) {
                    try {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Waiting for connection to open.");
                        }
                        idQueue.wait();
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Done waiting for connection.");
                }
                if (activeSendPort == null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("No Connection. Throwing exception.");
                    }
                    // TODO: Map exception properly
                    if (errorQueue.size() == 0) {
                        throw new ConnectionFailedException("Unable to open connection.", receiver);
                    } else {
                        throw new ConnectionFailedException("Unable to open connection.", receiver, errorQueue.get(0));
                    }
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Using active send port to connect");
                }
                activeSendPort.connect(receiver, timeoutMillis, fillTimeout);
            }
        }
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier id, String name) throws ConnectionFailedException {
        return connect(id, name, 0L, true);
    }

    @Override
    public synchronized ReceivePortIdentifier connect(IbisIdentifier id, String name, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        synchronized (idQueue) {
            if (id == null) {
                throw new IllegalArgumentException("Null ibis identifier!");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Connecting to: " + id + ":" + name + " timeout: " + timeoutMillis + " fill: " + fillTimeout);
            }
            errorQueue.clear();
            idQueue.clear();
            handlerCount = 0;
            if (activeSendPort == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No active connection...");
                }
                for (DowncallHandler handler : handlers) {
                    handler.timeout = timeoutMillis;
                    handler.fillTimeout = fillTimeout;
                    handler.opcode = DowncallHandler.OPP_CONNECT_IID_NAME;
                    handler.id = id;
                    handler.name = name;
                }
                // Wake all the handlers up.
                idQueue.notifyAll();
                while (activeSendPort == null && handlerCount < handlers.size()) {
                    try {
                        logger.debug("Waiting for handler to connect.");
                        idQueue.wait();
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
                if (activeSendPort == null) {
                    // TODO Map exceptions properly
                    if (errorQueue.size() == 0) {
                        throw new ConnectionFailedException("Unable to open connection.", id, name);
                    } else {
                        throw new ConnectionFailedException("Unable to open connection.", id, name, errorQueue.get(0));
                    }
                } else {
                    try {
                        return ibis.mapReceivePortIdentifier(idQueue.get(0), activeIbisName);
                    } catch (IOException e) {
                        // TODO Howto fill in right identifier?
                        throw new ConnectionFailedException("Unable to map identifier.", null, idQueue.get(0).name(), e);
                    }
                }
            } else {
                logger.debug("Using active send port to connect.");
                // TODO catch and map exception
                try {
                    MultiIbisIdentifier ident = (MultiIbisIdentifier) id;
                    return ibis.mapReceivePortIdentifier(activeSendPort.connect(ident.subIdForIbis(activeIbisName), name, timeoutMillis, fillTimeout),
                            activeIbisName);
                } catch (IOException e) {
                    throw new ConnectionFailedException("Unable to map identifier.", null, name, e);
                }
            }
        }
    }

    @Override
    public void connect(ReceivePortIdentifier[] ports) throws ConnectionsFailedException {
        connect(ports, 0L, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] ports, long timeoutMillis, boolean fillTimeout) throws ConnectionsFailedException {
        synchronized (idQueue) {
            logger.debug("Connecting...");
            errorQueue.clear();
            idQueue.clear();
            handlerCount = 0;
            if (activeSendPort == null) {
                logger.debug("No active connection");
                for (DowncallHandler handler : handlers) {
                    handler.timeout = timeoutMillis;
                    handler.fillTimeout = fillTimeout;
                    handler.opcode = DowncallHandler.OPP_CONNECT_RPID_ARRAY;
                    handler.rpids = ports;
                }
                idQueue.notifyAll();
                while (activeSendPort == null && handlerCount < handlers.size()) {
                    try {
                        logger.debug("Waiting for handler to connect...");
                        idQueue.wait();
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }

                if (activeSendPort == null) {
                    // TODO Handle Connections Failed properly.
                    throw new ConnectionsFailedException("All of them!");
                }
            } else {
                logger.debug("Using active send port to connect");
                // TODO Catch exception and remap
                activeSendPort.connect(ports, timeoutMillis, fillTimeout);
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports) throws ConnectionsFailedException {
        return connect(ports, 0L, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports, long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        synchronized (idQueue) {
            logger.debug("Connecting...");
            errorQueue.clear();
            idQueue.clear();
            handlerCount = 0;
            if (activeSendPort == null) {
                logger.debug("No active sendport");
                for (DowncallHandler handler : handlers) {
                    handler.timeout = timeoutMillis;
                    handler.fillTimeout = fillTimeout;
                    handler.opcode = DowncallHandler.OPP_CONNECT_IID_NAME_MAP;
                    handler.iidMap = ports;
                }
                idQueue.notifyAll();
                while (activeSendPort == null && handlerCount < handlers.size()) {
                    try {
                        logger.debug("Waiting for connection...");
                        idQueue.wait();
                    } catch (InterruptedException e) {
                        // Ignored
                    }
                }
                if (activeSendPort == null) {
                    // TODO Handle Connections Failed properly.
                    throw new ConnectionsFailedException("All of them!");
                } else {
                    ReceivePortIdentifier[] ids = activeSendPort.connectedTo();
                    for (int i = 0; i < ids.length; i++) {
                        try {
                            ids[i] = ibis.mapReceivePortIdentifier(ids[i], activeIbisName);
                        } catch (IOException e) {
                            // TODO Should we ignore this?
                        }
                    }
                    return ids;
                }
            } else {
                logger.debug("Using active send port to connect.");
                // TODO Catch and map exception
                ReceivePortIdentifier[] ids = activeSendPort.connect(ports, timeoutMillis, fillTimeout);
                for (int i = 0; i < ids.length; i++) {
                    try {
                        ids[i] = ibis.mapReceivePortIdentifier(ids[i], activeIbisName);
                    } catch (IOException e) {
                        // TODO Should we ignore this?
                    }
                }
                return ids;
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        HashMap<ReceivePortIdentifier, String> idList = new HashMap<>();
        for (String ibisName : subPortMap.keySet()) {
            SendPort subPort = subPortMap.get(ibisName);
            ReceivePortIdentifier[] ids = subPort.connectedTo();
            for (ReceivePortIdentifier id : ids) {
                try {
                    idList.put(ibis.mapReceivePortIdentifier(id, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we ignore this?
                }
            }
        }
        return idList.keySet().toArray(new ReceivePortIdentifier[idList.size()]);
    }

    @Override
    public void disconnect(ReceivePortIdentifier receiver) throws IOException {
        MultiIbisIdentifier id = (MultiIbisIdentifier) receiver.ibisIdentifier();
        for (String ibisName : subPortMap.keySet()) {
            SendPort subPort = subPortMap.get(ibisName);
            IbisIdentifier subId = id.subIdForIbis(ibisName);
            try {
                subPort.disconnect(subId, receiver.name());
            } catch (IOException e) {
                // TODO: Bundle IO Exceptions
            }
        }
    }

    @Override
    public void disconnect(IbisIdentifier identifier, String name) throws IOException {
        MultiIbisIdentifier id = (MultiIbisIdentifier) identifier;
        for (String ibisName : subPortMap.keySet()) {
            SendPort subPort = subPortMap.get(ibisName);
            IbisIdentifier subId = id.subIdForIbis(ibisName);
            try {
                subPort.disconnect(subId, name);
            } catch (IOException e) {
                // TODO: Bundle IO Exceptions
            }
        }
    }

    @Override
    public PortType getPortType() {
        return portType;
    }

    @Override
    public SendPortIdentifier identifier() {
        return id;
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        HashMap<ReceivePortIdentifier, String> idList = new HashMap<>();
        for (String ibisName : subPortMap.keySet()) {
            SendPort subPort = subPortMap.get(ibisName);
            ReceivePortIdentifier[] ids = subPort.lostConnections();
            for (ReceivePortIdentifier id2 : ids) {
                try {
                    idList.put(ibis.mapReceivePortIdentifier(id2, ibisName), ibisName);
                } catch (IOException e) {
                    // TODO Should we ignore this?
                }
            }
        }
        return idList.keySet().toArray(new ReceivePortIdentifier[idList.size()]);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public WriteMessage newMessage() throws IOException {
        // TODO: Throw error if activeSendPort is null?
        return new MultiWriteMessage(activeSendPort.newMessage(), this);
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

    public void quit(MultiSendPort port) {
        for (DowncallHandler handler : port.handlers) {
            handler.opcode = DowncallHandler.OPP_QUIT;
            synchronized (handler) {
                handler.notifyAll();
            }
        }
    }
}
