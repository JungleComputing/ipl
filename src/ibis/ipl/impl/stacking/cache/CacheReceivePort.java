package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
     * List of receive port identifiers to which this send port is logically
     * connected, but the under-the-hood-sendport is disconnected from them.
     */
    private List<SendPortIdentifier> falselyConnected;
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
    public final Set<SendPortIdentifier> toBeCachedSet;

    /**
     * This class forwards upcalls with the proper receive port.
     */
    private static final class ConnectUpcaller
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
            System.out.println("\t\tGot connection from: " + spi);
            
            synchronized(port.cacheManager) {
                if(port.falselyConnected.contains(spi)) {
                    port.falselyConnected.remove(spi);
                }
            port.cacheManager.addConnection(port.identifier(), spi);
            }
            
            if (upcaller != null) {
                return upcaller.gotConnection(port, spi);
            } else {
                return true;
            }
        }

        @Override
        public void lostConnection(ReceivePort me,
                SendPortIdentifier spi, Throwable reason) {
            System.out.println("\t\tConnection lost with: " + spi
                    + "\n\tReason: " + reason);
            
            // conn may be cached or a true disconnect or a close, or error.
            synchronized(port.cacheManager) {
                // check it this disconnect call is actually a caching
                if(port.toBeCachedSet.contains(spi)) {
                    port.toBeCachedSet.remove(spi);
                    port.falselyConnected.add(spi);
                }
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
        
        connectUpcall = new ConnectUpcaller(connectUpcall, this);
        
        if(upcall != null) {
            upcall = new MessageUpcaller(upcall, this);
        }

        /*
         * Add whatever additional port capablities are required.
         * i.e. CONNECTION_UPCALLS
         */
        Set<String> portCap = new HashSet<String>(Arrays.asList(
                    portType.getCapabilities()));
        portCap.addAll(CacheIbis.additionalPortCapabilities);
        PortType wrapperPortType = new PortType(portCap.toArray(
                new String[portCap.size()]));

        recvPort = ibis.baseIbis.createReceivePort(
                wrapperPortType, name, upcall, connectUpcall, properties);
        
        falselyConnected = new ArrayList<SendPortIdentifier>();
        cacheManager = ibis.cacheManager;
        intialPortType = portType;
        
        map.put(this.identifier(), this);
        
        toBeCachedSet = new HashSet<SendPortIdentifier>();
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
        ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                spi.ibisIdentifier(), CacheManager.sideChnRPName);
        WriteMessage msg = cacheManager.sideChannelSendPort.newMessage();
        msg.writeByte(SideChannelProtocol.CACHE_FROM_RP_AT_SP);
        msg.writeObject(spi);
        msg.writeObject(this.identifier());
        msg.finish();
        cacheManager.sideChannelSendPort.disconnect(sideRpi);
        
        falselyConnected.add(spi);
    }
    
    /*
     * This method is called when the passed SPI will want to cache a connection.
     * The next lostConnection(sendport) will be a connection caching, and
     * not a true disconnect upcall.
     */
    void futureCachedConnection(SendPortIdentifier spi) {
        toBeCachedSet.add(spi);
    }

    @Override
    public void close() throws IOException {
        close(0);
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        synchronized (cacheManager) {
            /*
             * TODO: my biggest problem: example: 1-1 cached connection
             * rp.close() should block but the underlying close() will not, and
             * it will close the receive port whilst it should wait for the send
             * port to be closed.
             */
            recvPort.close(timeoutMillis);
        }
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        synchronized (cacheManager) {
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
