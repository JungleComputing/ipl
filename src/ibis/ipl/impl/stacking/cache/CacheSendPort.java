package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
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
     * List of receive port identifiers to which this send port is logically
     * connected, but the under-the-hood-sendport is disconnected from them.
     */
    public List<ReceivePortIdentifier> falselyConnected;
    /**
     * Under-the-hood send port.
     */
    final SendPort sendPort;
    /**
     * Reference to the cache manager.
     */
    final CacheManager cacheManager;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * Lock required for the aliveMessage field.
     */
    final Object messageLock;
    /**
     * Variable to determine whether this send port has a current alive message.
     */
    volatile boolean aMessageIsAlive;

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

        sendPort = ibis.baseIbis.createSendPort(wrapperPortType, name, cU, props);

        intialPortType = portType;

        falselyConnected = new ArrayList<ReceivePortIdentifier>();
        cacheManager = ibis.cacheManager;
        messageLock = new Object();
        aMessageIsAlive = false;
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
     * connection will have no alive message.
     *
     * @param rpi the receive port to which to cache the connection
     * @param doesHeKnow if the receive port knows the next disconnect call he
     * gets from this send port is for caching purposes.
     * @throws IOException
     */
    public void cache(ReceivePortIdentifier rpi, boolean doesHeKnow)
            throws IOException {
        if (!doesHeKnow) {
            /*
             * Send message through the side channel of this connection, because
             * the receive port alone cannot distinguish caching from true
             * disconnection.
             */
            cacheManager.sideChannelHandler.sendProtocol(this.identifier(), rpi,
                    SideChannelProtocol.CACHE_FROM_SP);

            /*
             * Wait for ack from ReceivePort side, so we know that the RP side
             * knows about the to-be-cached-connection.
             */
            waitForAck();
        }

        /*
         * Now we can safely disconnect from the receive port, since we are
         * guaranteed that he will know to cache this connection.
         */
        sendPort.disconnect(rpi.ibisIdentifier(), rpi.name());
        falselyConnected.add(rpi);
    }

    /**
     * This method will restore the connection between this send port and the
     * given receive port. This method is to be called when we know the
     * connection is actually closed.
     *
     * This method will be called only from the Cache Manager under a
     * synchronized context.
     *
     * @param rpi
     * @throws IOException
     */
    public void revive(ReceivePortIdentifier rpi) throws IOException {
        sendPort.connect(rpi);
        falselyConnected.remove(rpi);
    }

    private void waitForAck() {
        synchronized (SideChannelMessageHandler.ackLock) {
            while (!SideChannelMessageHandler.ackReceived) {
                try {
                    SideChannelMessageHandler.ackLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            SideChannelMessageHandler.ackReceived = false;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (cacheManager) {
            /*
             * Send a DISCONNECT message to the receive ports with whom we have
             * cached connections. Otherwise, they won't get the
             * lostConnection() upcall.
             */
            for (ReceivePortIdentifier rpi : falselyConnected) {
                cacheManager.sideChannelHandler.sendProtocol(this.identifier(),
                        rpi, SideChannelProtocol.DISCONNECT);
            }
            cacheManager.removeAllConnections(this.identifier());
            /*
             * Disconnect from whoever is connected to the base send port.
             */
            sendPort.close();
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
    public void connect(ReceivePortIdentifier[] rpis,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        synchronized (messageLock) {
            if (aMessageIsAlive) {
                throw new ConnectionsFailedException(
                        "A message was alive while adding new connections");
            }
        }

        // TODO: I have to manipulate timeout and fillTimeout.

        Set<ReceivePortIdentifier> rpiList = new HashSet<ReceivePortIdentifier>(
                Arrays.asList(rpis));
        ReceivePortIdentifier[] connected;
        synchronized (cacheManager) {
            while (!rpiList.isEmpty()) {
                int initialSize = rpiList.size();
                /*
                 * Tell the cache manager to connect the send port to some of
                 * the receive ports received as params. This method guarantees
                 * at least 1 successfull connection.
                 */
                try {
                    connected = cacheManager.getSomeConnections(
                            this, rpiList, timeoutMillis);
                } catch (IbisIOException ex) {
                    throw (ConnectionsFailedException) ex;
                }
                rpiList.removeAll(Arrays.asList(connected));
                assert rpiList.size() < initialSize;
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        ReceivePortIdentifier[] trueConnections = sendPort.connectedTo();
        ReceivePortIdentifier[] retVal =
                new ReceivePortIdentifier[falselyConnected.size() + trueConnections.length];

        for (int i = 0; i < falselyConnected.size(); i++) {
            retVal[i] = falselyConnected.get(i);
        }
        System.arraycopy(trueConnections, 0,
                retVal, falselyConnected.size(), trueConnections.length);

        return retVal;
    }

    @Override
    public void disconnect(ReceivePortIdentifier rpi)
            throws IOException {
        synchronized (messageLock) {
            if (aMessageIsAlive) {
                throw new IOException(
                        "Trying to disconnect while a message is alive!");
            }
        }
        synchronized (cacheManager) {
            if (!falselyConnected.contains(rpi)) {
                sendPort.disconnect(rpi);
                /*
                 * Remove the connection only if it was trully alive.
                 */
                cacheManager.removeConnection(this.identifier(), rpi);
            } else {
                falselyConnected.remove(rpi);
                /*
                 * Send a DISCONNECT message to the receive ports with whom we
                 * have cached connections. Otherwise, they won't get the
                 * lostConnection() upcall.
                 */
                cacheManager.sideChannelHandler.sendProtocol(this.identifier(),
                        rpi, SideChannelProtocol.DISCONNECT);
            }
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
            while (aMessageIsAlive) {
                try {
                    messageLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            aMessageIsAlive = true;
        }

        /*
         * The field aliveMessage is set to false in this object's finish()
         * methods.
         */
        return new CacheWriteMessage(this);
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
