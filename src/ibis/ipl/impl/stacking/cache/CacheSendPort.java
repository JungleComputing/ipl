package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private List<ReceivePortIdentifier> falselyConnected;
    /**
     * Under-the-hood send port.
     */
    final private SendPort sendPort;
    /**
     * Reference to the cache manager.
     */
    final CacheManager cacheManager;
    /**
     * Mapping from (rpi.ibisIdentifier().name() + rpi.name()) to the rpi.
     */
    private final Map<String, ReceivePortIdentifier> rpiMap;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * Lock required for the aliveMessage field.
     */
    final Object messageLock;
    /**
     * Variable to determine wether this send port has a current alive message.
     */
    volatile boolean aliveMessage;
    
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

        rpiMap = new HashMap<String, ReceivePortIdentifier>();
        
        map.put(this.identifier(), this);
        
        messageLock = new Object();
        aliveMessage = false;
    }

    /*
     * Some or all of this ports connections have been closed. Restore them.
     *
     * This method will only be called if the CacheManager is certain there are
     * available ports.
     *
     * This method will be called only from the Cache Manager under a
     * synchronized context.
     */
    public void revive() throws IOException {
        for (ReceivePortIdentifier rpi : falselyConnected) {
            // with connection upcalls enabled,
            // the receive port upcaller will make room for this connection.
            sendPort.connect(rpi);
            falselyConnected.remove(rpi);
        }
    }

    /*
     * This method return true if it successfully caches the connection between
     * this send port and the given receive port.
     *
     * This method will be called only from the Cache Manager under a
     * synchronized context.
     */
    public boolean cache(ReceivePortIdentifier rpi)
            throws IOException {
        /**
         * I cannot disconnect the sendport from any receive port whilst a
         * message is alive.
         */
        synchronized (messageLock) {
            if (aliveMessage) {
                return false;
            } else {
                /*
                 * Send message through the side channel of this connection,
                 * because the receive port alone cannot distinguish caching
                 * from true disconnection.
                 */
                ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                        rpi.ibisIdentifier(), CacheManager.sideChnRPName);
                WriteMessage msg = cacheManager.sideChannelSendPort.newMessage();
                msg.writeByte(SideChannelProtocol.CACHE_FROM_SP);
                msg.writeObject(this.identifier());
                msg.writeObject(rpi);
                msg.finish();
                cacheManager.sideChannelSendPort.disconnect(sideRpi);
                
                /*
                 * Wait for ack from ReceivePort side, so we know that
                 * the RP side knows about the to-be-cached-connection.
                 */
                waitForAck();

                /*
                 * now properly disconnect from the receive port.
                 */
                sendPort.disconnect(rpi);
                falselyConnected.add(rpi);
                return true;
            }
        }
    }
    
    private void waitForAck() {
        synchronized(SideChannelMessageUpcall.ackLock) {
            while(!SideChannelMessageUpcall.ackReceived) {
                try {
                    SideChannelMessageUpcall.ackLock.wait();
                } catch (InterruptedException ignoreMe) {}
            }
            SideChannelMessageUpcall.ackReceived = false;
        }
    }
    
    @Override
    public void close() throws IOException {
        synchronized (cacheManager) {
            sendPort.close();
            cacheManager.removeAllConnections(this.identifier());
        }
    }

    @Override
    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier rpi,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        synchronized (cacheManager) {
            // tell the cache manager to make room for this one connection.
            cacheManager.makeWayForSendPort(this.identifier(), 1);
            // actually connect.
            sendPort.connect(rpi, timeoutMillis, fillTimeout);
            // add the connection to the manager - it'll handle it
            cacheManager.addConnections(this.identifier(), new ReceivePortIdentifier[] {rpi});
        }
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws ConnectionFailedException {
        return connect(ibisIdentifier, receivePortName, 0, true);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        synchronized (cacheManager) {
            // guarantee free room for connection
            cacheManager.makeWayForSendPort(this.identifier(), 1);
            // actually connect
            ReceivePortIdentifier rpi = sendPort.connect(ibisIdentifier,
                    receivePortName, timeoutMillis, fillTimeout);
            // store info for the disconnect call
            rpiMap.put(rpi.ibisIdentifier().name() + rpi.name(), rpi);
            // add the connection information to the manager
            cacheManager.addConnections(this.identifier(), 
                    new ReceivePortIdentifier[] {rpi});

            return rpi;
        }
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] rpis,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        synchronized (cacheManager) {
            cacheManager.makeWayForSendPort(this.identifier(), rpis.length);
            sendPort.connect(rpis, timeoutMillis, fillTimeout);
            cacheManager.addConnections(this.identifier(), rpis);
        }
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(
            Map<IbisIdentifier, String> ports,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        synchronized (cacheManager) {
            cacheManager.makeWayForSendPort(this.identifier(), ports.size());

            ReceivePortIdentifier[] retValRpis = sendPort.connect(
                    ports, timeoutMillis, fillTimeout);
            for (ReceivePortIdentifier rpi : retValRpis) {
                rpiMap.put(rpi.ibisIdentifier().name() + rpi.name(), rpi);
            }
            
            cacheManager.addConnections(this.identifier(), retValRpis);

            return retValRpis;
        }
    }

    @Override
    public ReceivePortIdentifier[] connectedTo() {
        synchronized (cacheManager) {
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
    }

    @Override
    public void disconnect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws IOException {
        synchronized (cacheManager) {
            ReceivePortIdentifier rpi = rpiMap.get(ibisIdentifier.name() + receivePortName);
            if (rpi == null) {
                throw new IOException("Cannot disconnect from (" + ibisIdentifier.name()
                        + ", " + receivePortName + "), since we are not connected with it."
                        + "\n\t OR I MADE A BUBU with this map");
            }
            if (!falselyConnected.contains(rpi)) {
                sendPort.disconnect(ibisIdentifier, receivePortName);
            } else {
                falselyConnected.remove(rpi);
            }
            cacheManager.removeConnection(this.identifier(), rpi);
        }
    }

    @Override
    public void disconnect(ReceivePortIdentifier rpi)
            throws IOException {
        synchronized (cacheManager) {
            if (!falselyConnected.contains(rpi)) {
                sendPort.disconnect(rpi);
            } else {
                falselyConnected.remove(rpi);
            }
            cacheManager.removeConnection(this.identifier(), rpi);
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
            while (aliveMessage) {
                try {
                    messageLock.wait();
                } catch (InterruptedException ignoreMe) {}
            }
            aliveMessage = true;
        }

        /*
         * Make sure all connections are open from this send port.
         */
        synchronized (cacheManager) {
            if (!falselyConnected.isEmpty()) {
                cacheManager.reviveSendPort(this.identifier());
            }
        }

        /*
         * The field aliveMessage is set to false in this object's finish() methods.
         */
        return new CacheWriteMessage(sendPort.newMessage(), this);
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

    int getNoCachedConnections() {
        return falselyConnected.size();
    }
    
    int getNoTrueConnections() {
        return sendPort.connectedTo().length;
    }
}
