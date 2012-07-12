package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class CacheSendPort implements SendPort {

    /**
     * Static variable which is incremented every time an anonymous (nameless)
     * send port is created.
     */
    static AtomicInteger anonymousPortCounter;
    /**
     * Prefix for anonymous ports.
     */
    static final String ANONYMOUS_PREFIX;
    /**
     * Map to store identifiers to the cachesendports.
     */
    public static final Map<SendPortIdentifier, CacheSendPort> map;

    static {
        anonymousPortCounter = new AtomicInteger(0);
        ANONYMOUS_PREFIX = "anonymous cache send port";

        map = new HashMap<SendPortIdentifier, CacheSendPort>();
    }
    /**
     * Under-the-hood send port.
     */
    public final SendPort sendPort;
    /**
     * Reference to the cache manager.
     */
    public final CacheManager cacheManager;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * Lock required for the aliveMessage field.
     */
    final Object messageLock;
    /**
     * The current written message.
     */
    public CacheWriteMessage currentMsg;
    public final Object cacheAckLock = new Object();
    public boolean cacheAckReceived = false;
    public final Set<ReceivePortIdentifier> reserveAckReceived;

    public CacheSendPort(PortType portType, CacheIbis ibis, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }

        /*
         * Add whatever additional port capablities are required. i.e.
         * CONNECTION_UPCALLS
         */
        Set<String> portCap = new HashSet<String>(Arrays.asList(
                portType.getCapabilities()));
        portCap.addAll(CacheIbis.additionalPortCapabilities);
        PortType wrapperPortType = new PortType(portCap.toArray(
                new String[portCap.size()]));

        SendPortDisconnectUpcall wrapperDiscUp =
                new SendPortDisconnectUpcaller(cU, this);

        sendPort = ibis.baseIbis.createSendPort(wrapperPortType, name,
                wrapperDiscUp, props);

        intialPortType = portType;

        cacheManager = ibis.cacheManager;
        messageLock = new Object();
        currentMsg = null;
        
        reserveAckReceived = new HashSet<ReceivePortIdentifier>();
        /*
         * Send this to the map only when it has been filled up with all data.
         */
        map.put(this.identifier(), this);
    }

    /**
     * This method will cache the connection between this send port and the
     * given receive port. This method is to be called when we know the
     * connection is actually alive.
     *
     * This method will be called only from the Cache Manager under a
     * synchronized context. If so, I am guaranteed that the underlying
     * connection will have no alive message. (search for sendPort.newMessage())
     *
     * @param rpi the receive port to which to cache the connection
     * @param doesHeKnow if the receive port knows the next disconnect call he
     * gets from this send port is for caching purposes.
     * @throws IOException
     */
    public void cache(ReceivePortIdentifier rpi, boolean doesHeKnow)
            throws IOException {
        if (!doesHeKnow) {
            cacheAckReceived = false;
            /*
             * Send message through the side channel of this connection, because
             * the receive port alone cannot distinguish caching from true
             * disconnection.
             */
            cacheManager.sideChannelHandler.newThreadSendProtocol(this.identifier(), rpi,
                    SideChannelProtocol.CACHE_FROM_SP);

            /*
             * Wait for ack from ReceivePort side, so we know that the RP side
             * knows about the to-be-cached-connection.
             */
            waitForCacheAck();
        }

        /*
         * Now we can safely disconnect from the receive port, since we are
         * guaranteed that he will know to cache this connection.
         */
        sendPort.disconnect(rpi.ibisIdentifier(), rpi.name());
    }

    private void waitForCacheAck() {
        synchronized (cacheAckLock) {
            while (!cacheAckReceived) {
                try {
                    cacheAckLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (messageLock) {
            while (currentMsg != null) {
                throw new IOException(
                        "Trying to close the send port while a message is alive!");
            }
        }
        cacheManager.lock.lock();
        try {
            /*
             * Send a DISCONNECT message to the receive ports with whom we have
             * cached connections. Otherwise, they won't get the
             * lostConnection() upcall.
             */
            for (ReceivePortIdentifier rpi :
                    cacheManager.cachedRpisFrom(this.identifier())) {

                cacheManager.sideChannelHandler.sendProtocol(this.identifier(),
                        rpi, SideChannelProtocol.DISCONNECT);

            }

            cacheManager.removeAllConnections(this.identifier());
            /*
             * Disconnect from whoever is connected to the base send port.
             */
            sendPort.close();
        } finally {
            cacheManager.lock.unlock();
        }
    }

    // Don't modify anything below here.-------------------------------
    @Override
    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws ConnectionFailedException {
        return connect(ibisIdentifier, receivePortName, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        ReceivePortIdentifier rpi = new ibis.ipl.impl.ReceivePortIdentifier(
                receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier);
        connect(rpi);
        return rpi;
    }

    @Override
    public ReceivePortIdentifier[] connect(
            Map<IbisIdentifier, String> ports,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        List<ReceivePortIdentifier> rpis = new ArrayList<ReceivePortIdentifier>(ports.size());
        for (IbisIdentifier ii : ports.keySet()) {
            rpis.add(new ibis.ipl.impl.ReceivePortIdentifier(ports.get(ii),
                    (ibis.ipl.impl.IbisIdentifier) ii));
        }
        ReceivePortIdentifier[] retVal =
                rpis.toArray(new ReceivePortIdentifier[rpis.size()]);

        connect(retVal);
        return retVal;
    }

    @Override
    public void connect(ReceivePortIdentifier rpi,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        try {
            connect(new ReceivePortIdentifier[]{rpi}, timeoutMillis, fillTimeout);
        } catch (ConnectionsFailedException ex) {
            throw new ConnectionFailedException(ex.toString(), rpi);
        }
    }

    @Override
    public void disconnect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws IOException {
        ReceivePortIdentifier rpi = new ibis.ipl.impl.ReceivePortIdentifier(
                receivePortName, (ibis.ipl.impl.IbisIdentifier) ibisIdentifier);
        disconnect(rpi);
    }
    // Don't modify anything above this.-------------------------------

    @Override
    public synchronized void connect(ReceivePortIdentifier[] rpis,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        synchronized (messageLock) {
            if (currentMsg != null) {
                throw new ConnectionsFailedException(
                        "A message was alive while adding new connections");
            }
        }
        
        long deadline = 0;
        if (timeoutMillis > 0) {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }

        Set<ReceivePortIdentifier> rpiSet = new HashSet<ReceivePortIdentifier>(
                Arrays.asList(rpis));
        Set<ReceivePortIdentifier> connected;

        /*
         * While there still are some receive ports to which I have to
         * connect...
         */
        while (!rpiSet.isEmpty()) {
            int initialSize = rpiSet.size();
            /*
             * Tell the cache manager to connect the send port to some of the
             * receive ports received as params. This method guarantees at least
             * 1 successfull connection.
             */
            cacheManager.lock.lock();
            try {
                if (deadline > 0) {
                    connected = cacheManager.getSomeConnections(
                            this, rpiSet,
                            deadline - System.currentTimeMillis(), fillTimeout);
                } else {
                    connected = cacheManager.getSomeConnections(
                            this, rpiSet,
                            0, true);
                }
                rpiSet.removeAll(connected);
                assert rpiSet.size() < initialSize;
            } catch (ConnectionsFailedException timedout) {
                ConnectionsFailedException ex = new ConnectionsFailedException();
                for (ReceivePortIdentifier rpi : rpiSet) {
                    ex.add(new ConnectionTimedOutException(
                            "Out of time, connection not even tried", rpi));
                }
                throw ex;
            } catch (ibis.ipl.IbisIOException connFailed) {
                throw (ConnectionsFailedException) connFailed;
            } finally {
                cacheManager.lock.unlock();
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        cacheManager.lock.lock(); 
        try {
            return cacheManager.allRpisFrom(this.identifier());
        } finally {
            cacheManager.lock.unlock();
        }
    }

    @Override
    public void disconnect(ReceivePortIdentifier rpi)
            throws IOException {
        synchronized (messageLock) {
            if (currentMsg != null) {
                throw new IOException(
                        "Trying to disconnect while a message is alive!");
            }
        }
        cacheManager.lock.lock();
        try {
            if (cacheManager.isConnAlive(this.identifier(), rpi)) {
                sendPort.disconnect(rpi);
            } else {
                /*
                 * Send a DISCONNECT message to the receive ports with whom we
                 * have cached connections. Otherwise, they won't get the
                 * lostConnection() upcall.
                 */
                cacheManager.sideChannelHandler.sendProtocol(this.identifier(),
                        rpi, SideChannelProtocol.DISCONNECT);
            }
            /*
             * Remove the connection.
             */
            cacheManager.removeConnection(this.identifier(), rpi);
        } finally {
            cacheManager.lock.unlock();
        }
    }

    @Override
    public PortType getPortType() {
        return this.intialPortType;
    }

    @Override
    public String name() {
        return sendPort.name();
    }

    @Override
    public SendPortIdentifier identifier() {
        return sendPort.identifier();
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        return sendPort.lostConnections();
    }

    @Override
    public WriteMessage newMessage() throws IOException {
        synchronized (messageLock) {
            while (currentMsg != null) {
                try {
                    messageLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            /*
             * The field currentMsg is set to null in this object's finish()
             * methods.
             */
            currentMsg = new CacheWriteMessage(this);
            return currentMsg;
        }
    }

    @Override
    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return sendPort.getManagementProperty(key);
    }

    @Override
    public Map<String, String> managementProperties() {
        return sendPort.managementProperties();
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        sendPort.printManagementProperties(stream);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        sendPort.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        sendPort.setManagementProperty(key, value);
    }
}
