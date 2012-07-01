package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CacheReceivePort implements ReceivePort {

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
     * Map to store identifiers to the cachereceiveports.
     */
    public static final Map<ReceivePortIdentifier, CacheReceivePort> map;

    static {
        anonymousPortCounter = new AtomicInteger(0);
        ANONYMOUS_PREFIX = "anonymous cache receive port";
        
        map = new HashMap<ReceivePortIdentifier, CacheReceivePort>();
    }
    /**
     * List of send port identifiers to which this receive port is logically
     * connected, but the under-the-hood-receiveport is disconnected from them.
     */
    private List<SendPortIdentifier> falselyConnected;
    /**
     * All the unde-the-hood-alive and cached connections.
     */
    private Set<SendPortIdentifier> logicallyAlive;
    /*
     * Set containing live connections which will be cached.
     */
    public final Set<SendPortIdentifier> toBeCachedSet;
    /**
     * Under-the-hood send port.
     */
    final private ReceivePort recvPort;
    /**
     * Reference to the cache manager.
     */
    final CacheManager cacheManager;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * A reference to this receive port's connection upcaller.
     * Need to call lostConnection() from the side channel.
     */
    public final ConnectUpcaller connectUpcall;
    /**
     * Boolean for the CacheReceivePort connection.
     */
    private boolean closed;
    
    /**
     * This class forwards upcalls with the proper receive port.
     */
    public static final class ConnectUpcaller
            implements ReceivePortConnectUpcall {

        CacheReceivePort port;
        ReceivePortConnectUpcall upcaller;

        public ConnectUpcaller(ReceivePortConnectUpcall upcaller,
                CacheReceivePort port) {
            this.port = port;
            this.upcaller = upcaller;
        }

        @Override
        public boolean gotConnection(ReceivePort me,
                SendPortIdentifier spi) {
            boolean retVal = true;
            
            synchronized (port.cacheManager) {
                if (port.closed) {
                    return false;
                }
                if (port.falselyConnected.contains(spi)) {
                    port.falselyConnected.remove(spi);
                    port.cacheManager.notifyAll();
                }
                port.cacheManager.addConnection(port.identifier(), spi);
            }

            if (upcaller != null) {
                retVal = upcaller.gotConnection(port, spi);
            }
            
            if(retVal) {
                port.logicallyAlive.add(spi);
            }
            
            return retVal;
        }

        /*
         * Synchronized in order to guarantee that at most 1 is alive at any
         * time, because this method is also called manually from the side
         * channel handling class.
         */
        @Override
        public synchronized void lostConnection(ReceivePort me,
                SendPortIdentifier spi, Throwable reason) {
            synchronized (port.cacheManager) {
                // this method is called manually
                // and the connection was cached, but now it needs to be closed.
                if (me == null) {
                    port.falselyConnected.remove(spi);
                    return;
                }
                
                if (port.toBeCachedSet.contains(spi)) {
                    // check it this disconnect call is actually a caching
                    port.toBeCachedSet.remove(spi);
                    port.falselyConnected.add(spi);
                } else {
                    // this connection is lost for good - no caching.
                    port.logicallyAlive.remove(spi);
                    port.cacheManager.notifyAll();
                }

                // this connection exists no longer.
                port.cacheManager.removeConnection(me.identifier(), spi);
            }

            if (upcaller != null) {
                upcaller.lostConnection(port, spi, reason);
            }
        }
    }

    /**
     * This class forwards message upcalls with the proper message.
     */
    private static final class MessageUpcaller implements MessageUpcall {

        MessageUpcall upcaller;
        CacheReceivePort port;

        public MessageUpcaller(MessageUpcall upcaller, CacheReceivePort port) {
            this.upcaller = upcaller;
            this.port = port;
        }

        @Override
        public void upcall(ReadMessage m) throws IOException, ClassNotFoundException {
            upcaller.upcall(new CacheReadMessage(m, port));
        }
    }

    public CacheReceivePort(PortType portType, CacheIbis ibis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties)
            throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }

        this.connectUpcall = new ConnectUpcaller(connectUpcall, this);

        if (upcall != null) {
            upcall = new MessageUpcaller(upcall, this);
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

        recvPort = ibis.baseIbis.createReceivePort(
                wrapperPortType, name, upcall, this.connectUpcall, properties);

        falselyConnected = new ArrayList<SendPortIdentifier>();
        logicallyAlive = new HashSet<SendPortIdentifier>();
        cacheManager = ibis.cacheManager;
        intialPortType = portType;

        map.put(this.identifier(), this);

        toBeCachedSet = new HashSet<SendPortIdentifier>();
        
        closed = false;
    }

    /*
     * Tell this spi to cache the connection between itself and this
     * receiveport.
     */
    public void cache(SendPortIdentifier spi) throws IOException {
        /*
         * Tell the SP side to cache the connection.
         * I will count this connection at the lostConnection upcall.
         */
        cacheManager.sideChannelHandler.sendProtocol(spi, this.identifier(), 
                SideChannelProtocol.CACHE_FROM_RP_AT_SP);
        
        falselyConnected.add(spi);
    }    
    
    @Override
    public void close() throws IOException {
        close(0);
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        /*
         * Wait until all logically alive connections are closed.
         */
        synchronized (cacheManager) {
            closed = true;
            while (!falselyConnected.isEmpty() || recvPort.connectedTo().length > 0) {
                System.out.println("\tFalsely conn:\t" + falselyConnected.size());
                System.out.println("\tReal conn:\t" + recvPort.connectedTo()[0]);
                try {
                    cacheManager.wait();
                } catch (InterruptedException ignoreMe) {}
            }
            recvPort.close(timeoutMillis);
        }
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        SendPortIdentifier[] trueConnections = recvPort.connectedTo();
        SendPortIdentifier[] retVal =
                new SendPortIdentifier[falselyConnected.size() + trueConnections.length];

        for (int i = 0; i < falselyConnected.size(); i++) {
            retVal[i] = falselyConnected.get(i);
        }

        System.arraycopy(trueConnections, 0,
                retVal, falselyConnected.size(), trueConnections.length);

        return retVal;
    }

    @Override
    public void disableConnections() {
        recvPort.disableConnections();
    }

    @Override
    public void disableMessageUpcalls() {
        recvPort.disableMessageUpcalls();
    }

    @Override
    public void enableConnections() {
        recvPort.enableConnections();
    }

    @Override
    public void enableMessageUpcalls() {
        recvPort.enableMessageUpcalls();
    }

    @Override
    public PortType getPortType() {
        return this.intialPortType;
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return recvPort.identifier();
    }

    @Override
    public SendPortIdentifier[] lostConnections() {
        return recvPort.lostConnections();
    }

    @Override
    public String name() {
        return recvPort.name();
    }

    @Override
    public SendPortIdentifier[] newConnections() {
        return recvPort.newConnections();
    }

    @Override
    public ReadMessage poll() throws IOException {
        ReadMessage m = recvPort.poll();
        if (m != null) {
            m = new CacheReadMessage(m, this);
        }
        return m;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    @Override
    public ReadMessage receive(long timeoutMillis) throws IOException {
        return recvPort.receive(timeoutMillis);
    }

    @Override
    public Map<String, String> managementProperties() {
        return recvPort.managementProperties();
    }

    @Override
    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        return recvPort.getManagementProperty(key);
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        recvPort.setManagementProperties(properties);
    }

    @Override
    public void setManagementProperty(String key, String val)
            throws NoSuchPropertyException {
        recvPort.setManagementProperty(key, val);
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        recvPort.printManagementProperties(stream);
    }
}
