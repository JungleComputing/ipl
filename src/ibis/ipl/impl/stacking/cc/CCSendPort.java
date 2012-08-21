package ibis.ipl.impl.stacking.cc;

import ibis.io.SerializationFactory;
import ibis.io.SerializationOutput;
import ibis.ipl.*;
import ibis.ipl.impl.stacking.cc.io.BufferedDataOutputStream;
import ibis.ipl.impl.stacking.cc.manager.CCManager;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cc.util.CCStatistics;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CCSendPort implements SendPort {
    
    private final static Logger logger = 
            LoggerFactory.getLogger(CCSendPort.class);

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
     * Map to store identifiers to the send ports.
     */
    public static final Map<SendPortIdentifier, CCSendPort> map;

    static {
        anonymousPortCounter = new AtomicInteger(0);
        ANONYMOUS_PREFIX = "anonymous CC send port";

        map = new HashMap<SendPortIdentifier, CCSendPort>();
    }
    /**
     * Under-the-hood send port.
     */
    public final SendPort baseSendPort;
    /**
     * Reference to the CCManager.
     */
    public final CCManager ccManager;
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
    public volatile CCWriteMessage currentMsg;
    public final Object cacheAckLock = new Object();
    public boolean cacheAckReceived = false;
    public final Map<ReceivePortIdentifier, Byte> reserveAcks;
    
    /**
     * Reference to the ConnectionCaching ibis.
     */
    public final CCIbis ccIbis;
    
    /**
     * For WriteMessage.
     */
    protected final SerializationOutput serOut;
    public final BufferedDataOutputStream dataOut;

    public CCSendPort(PortType portType, CCIbis ibis, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }
        
        ccIbis = ibis;

        /*
         * Add whatever additional port capablities are required. i.e.
         * CONNECTION_UPCALLS
         */
        Set<String> portCap = new HashSet<String>(Arrays.asList(
                portType.getCapabilities()));
        
        portCap.removeAll(CCIbis.removablePortCapabilities);
        
        portCap.addAll(CCIbis.additionalPortCapabilities);
        PortType wrapperPortType = new PortType(portCap.toArray(
                new String[portCap.size()]));

        SendPortDisconnectUpcall wrapperDiscUp =
                new SendPortDisconnectUpcaller(cU, this);

        baseSendPort = ibis.baseIbis.createSendPort(wrapperPortType, name,
                wrapperDiscUp, props);

        intialPortType = portType;

        ccManager = ibis.ccManager;
        messageLock = new Object();
        currentMsg = null;
        
        reserveAcks = new HashMap<ReceivePortIdentifier, Byte>();

        String serialization;
        if (portType.hasCapability(PortType.SERIALIZATION_DATA)) {
            serialization = "data";
        } else if (portType.hasCapability(PortType.SERIALIZATION_OBJECT_SUN)) {
            serialization = "sun";
        } else if (portType.hasCapability(PortType.SERIALIZATION_OBJECT_IBIS)) {
            serialization = "ibis";
        } else if (portType.hasCapability(PortType.SERIALIZATION_OBJECT)) {
            serialization = "object";
        } else {
            serialization = "byte";
        }

        dataOut = new BufferedDataOutputStream(this);
        serOut = SerializationFactory.createSerializationOutput(serialization,
                dataOut);

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
     * This method will be called only from the CCManager with the
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
        logger.debug("\nGoing to cache from"
                + " {} to {}; heKnows={}", new Object[] {
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
        ccManager.reserveLiveConnection(this.identifier(), rpi);
        logger.debug("Unlocking lock for {} to disconnect.", this.identifier());
        ccManager.lock.unlock();

        try {
            if (!heKnows) {
                cacheAckReceived = false;
                /*
                 * Send message through the side channel of this connection,
                 * because the receive port alone cannot distinguish caching
                 * from true disconnection.
                 */
                ccManager.sideChannelHandler.newThreadSendProtocol(this.identifier(), rpi,
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
            CCStatistics.cache(this.identifier(), rpi);
        } catch (Exception ex) {
            logger.error("\nBase send port "
                    + this.identifier() + " failed to "
                    + "properly disconnect from "
                    + rpi + ".", ex);
        } finally {
            ccManager.lock.lock();
            logger.debug("\n\t{} reaquired lock.", this.identifier());
            ccManager.unReserveLiveConnection(this.identifier(), rpi);
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
                try {
                    messageLock.wait();
                } catch (InterruptedException ex) {}
            }
        }
        
        ccManager.lock.lock();
        logger.debug("Lock locked.");
        logger.debug("Closing CC send port\t{}", this.identifier());
        try {
            ccManager.closeSendPort(this.identifier());
        } finally {
            logger.debug("Unlocking lock.");
            ccManager.lock.unlock();
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
             * Tell the CCManager to connect the send port to some of the
             * receive ports received as params. This method guarantees at least
             * 1 successfull connection.
             */
            ccManager.lock.lock();
            logger.debug("Lock locked.");
            try {
                if (deadline > 0) {
                    connected = ccManager.getSomeConnections(
                            this, rpiSet,
                            deadline - System.currentTimeMillis(), fillTimeout);
                } else {
                    connected = ccManager.getSomeConnections(
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
                logger.debug("Unlocking lock.");
                ccManager.lock.unlock();                
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        ccManager.lock.lock();
        try {
            return ccManager.allRpisFrom(this.identifier());
        } finally {
            ccManager.lock.unlock();
        }
    }

    @Override
    public void disconnect(ReceivePortIdentifier rpi)
            throws IOException {
        synchronized (messageLock) {
            while (currentMsg != null) {
                try {
                    messageLock.wait();
                } catch (InterruptedException ex) {}
            }
        }
        ccManager.lock.lock();
        logger.debug("Lock locked.");
        try {
            /*
             * Remove the connection.
             */
            ccManager.removeConnection(this.identifier(), rpi);
        } finally {
            logger.debug("Unlocking lock.");
            ccManager.lock.unlock();            
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
        logger.debug("newWriteMessage requested; writing to {}",
                Arrays.asList(connectedTo()));
        synchronized (messageLock) {
            logger.debug("before curMsg={}", currentMsg);
            while (currentMsg != null) {
                try {                    
                    messageLock.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
            logger.debug("after curMsg={}", currentMsg);
            /*
             * The field currentMsg is set to null in this object's finish()
             * methods.
             */
            currentMsg = new CCWriteMessage(this);
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
