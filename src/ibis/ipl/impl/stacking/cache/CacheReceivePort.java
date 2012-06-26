package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheReceivePort implements ReceivePort {

    /**
     * Static variable which is incremented every time an anonymous (nameless)
     * send port is created.
     */
    static AtomicInteger anonymousPortCounter;
    /**
     * Prefix for anonymous ports.
     */
    static final String ANONYMOUS_PREFIX;

    static {
        anonymousPortCounter = new AtomicInteger(0);
        ANONYMOUS_PREFIX = "anonymous cache receive port";
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
     * This send port's identifier.
     */
    final ReceivePortIdentifier recvPortIdentifier;
    /**
     * Reference to the cache manager.
     */
    CacheManager cacheManager;

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
                SendPortIdentifier applicant) {
            System.out.println("\t\tGot connection from: " + applicant);
            /*
             * TODO: update cache information
             */
            if (upcaller != null) {
                return upcaller.gotConnection(port, applicant);
            } else {
                return true;
            }
        }

        @Override
        public void lostConnection(ReceivePort me,
                SendPortIdentifier johnDoe, Throwable reason) {
            System.out.println("\t\tConnection lost with: " + johnDoe
                    + "\n\tReason: " + reason);
            /*
             * TODO: update cache information
             */
            if (upcaller != null) {
                upcaller.lostConnection(port, johnDoe, reason);
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
        recvPortIdentifier = new CacheReceivePortIdentifier(
                ibis.identifier(), name);
        falselyConnected = new ArrayList<SendPortIdentifier>();
        cacheManager = ibis.cacheManager;
    }

    /**
     * Tell this spi to cache the connection between itself and this
     * receiveport.
     * 
     * @param spi
     * @return
     * @throws IOException
     */
    public synchronized boolean cache(CacheSendPortIdentifier spi) throws IOException {
        tellSPToCacheMe(spi);
        falselyConnected.add(spi);
        return true;
    }

    private void tellSPToCacheMe(CacheSendPortIdentifier spi) throws IOException {
        ReceivePortIdentifier sideRpi = cacheManager.sideChannelSendPort.connect(
                spi.ibisIdentifier(), CacheManager.sideChnRPName);
        WriteMessage msg = cacheManager.sideChannelSendPort.newMessage();
        msg.writeByte(SideChannelProtocol.CACHE_SP);
        msg.writeObject(spi);
        msg.writeObject(recvPortIdentifier);
        // where do i count -1?
        // here or at lost connection?!
        // i'd say at lost connection.
        msg.finish();
        cacheManager.sideChannelSendPort.disconnect(sideRpi);
    }

    @Override
    public void close() throws IOException {
        recvPort.close();
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        recvPort.close(timeoutMillis);
    }

    @Override
    public synchronized SendPortIdentifier[] connectedTo() {
        SendPortIdentifier[] retVal = new SendPortIdentifier[falselyConnected.size() + recvPort.connectedTo().length];

        for (int i = 0; i < falselyConnected.size(); i++) {
            retVal[i] = falselyConnected.get(i);
        }
        System.arraycopy(recvPort.connectedTo(), 0,
                retVal, falselyConnected.size(), retVal.length);

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
        return recvPort.getPortType();
    }

    @Override
    public ReceivePortIdentifier identifier() {
        return recvPortIdentifier;
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
