package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cache.util.CacheStatistics;
import ibis.ipl.impl.stacking.cache.util.Loggers;
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
    public final SendPort baseSendPort;
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
    public final Map<ReceivePortIdentifier, Byte> reserveAcks;

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

        baseSendPort = ibis.baseIbis.createSendPort(wrapperPortType, name,
                wrapperDiscUp, props);

        intialPortType = portType;

        cacheManager = ibis.cacheManager;
        messageLock = new Object();
        currentMsg = null;
        
        reserveAcks = new HashMap<ReceivePortIdentifier, Byte>();
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
     * This method will be called only from the Cache Manager with the
     * lock locked. Thus, I am guaranteed that the underlying
     * connection will have no alive message. (search for sendPort.newMessage())
     *
     * @param rpi the receive port to which to cache the connection
     * @param heKnows if the receive port knows the next disconnect call he
     * gets from this send port is for caching purposes.
     * @throws IOException
     */
    public void cache(ReceivePortIdentifier rpi, boolean heKnows)
            throws IOException {
        Loggers.cacheLog.log(Level.INFO, "\nGoing to cache from"
                + " {0} to {1}; heKnows={2}", new Object[] {
                    this.identifier(), rpi, heKnows});
        
        /*
         * ISSUES:
         * - the disconnect() will block until the lostConnection() upcall
         * is finished.
         * - waiting for RcvPort's ack whilst holding the lock.
         * 
         * SCENARIO: 
         * 2 machines simultaneously disconnect (whilst holding
         * the lock here) and they both are blocked in the upcall
         * because none of them can get the lock.
         * 
         * SOLUTION:
         * move the connection to reserved state, release the lock,
         * wait for rcv port's ack, disconnect, 
         * reaquire the lock and move the connection back.
         */
        cacheManager.reserveLiveConnection(this.identifier(), rpi);
        cacheManager.lock.unlock();
        Loggers.lockLog.log(Level.INFO, "Lock released for {0} to disconnect.", this.identifier());

        try {
            if (!heKnows) {
                cacheAckReceived = false;
                /*
                 * Send message through the side channel of this connection,
                 * because the receive port alone cannot distinguish caching
                 * from true disconnection.
                 */
                cacheManager.sideChannelHandler.newThreadSendProtocol(this.identifier(), rpi,
                        SideChannelProtocol.CACHE_FROM_SP);

                /*
                 * Wait for ack from ReceivePort side, so we know that the RP
                 * side knows about the to-be-cached-connection.
                 */
                waitForCacheAck();
            }

            /*
             * Now we can safely disconnect from the receive port, since we are
             * guaranteed that he will know to cache this connection.
             */
            baseSendPort.disconnect(rpi.ibisIdentifier(), rpi.name());
            CacheStatistics.cache(this.identifier(), rpi);

            Loggers.cacheLog.log(Level.FINEST, "\nBase send port now connected"
                    + " to {0} recv ports.", baseSendPort.connectedTo().length);
        } catch (Exception ex) {
            Loggers.cacheLog.log(Level.SEVERE, "\nBase send port "
                    + this.identifier() + " failed to "
                    + "properly disconnect from "
                    + rpi + ".", ex);
        } finally {
            cacheManager.lock.lock();
            Loggers.lockLog.log(Level.INFO, "\n\t{0} reaquired lock.", this.identifier());
            cacheManager.unReserveLiveConnection(this.identifier(), rpi);
        }
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
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        Loggers.conLog.log(Level.INFO, "Closing cache send port\t{0}", this.identifier());
        try {
            cacheManager.closeSendPort(this.identifier());
        } finally {
            cacheManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
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
            Loggers.lockLog.log(Level.INFO, "Lock locked.");
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
                Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
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
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        try {
            /*
             * Remove the connection.
             */
            cacheManager.removeConnection(this.identifier(), rpi);
        } finally {
            cacheManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
        }
    }

    @Override
    public PortType getPortType() {
        return this.intialPortType;
    }

    @Override
    public String name() {
        return baseSendPort.name();
    }

    @Override
    public SendPortIdentifier identifier() {
        return baseSendPort.identifier();
    }

    @Override
    public ReceivePortIdentifier[] lostConnections() {
        return baseSendPort.lostConnections();
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
        return baseSendPort.getManagementProperty(key);
    }

    @Override
    public Map<String, String> managementProperties() {
        return baseSendPort.managementProperties();
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        baseSendPort.printManagementProperties(stream);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        baseSendPort.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        baseSendPort.setManagementProperty(key, value);
    }
}
