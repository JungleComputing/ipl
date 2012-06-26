package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheSendPort implements SendPort {

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
        ANONYMOUS_PREFIX = "anonymous cache send port";
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
     * This send port's identifier.
     */
    final SendPortIdentifier sendPortIdentifier;
    /**
     * This port's current message (if it exists). we know there is at most one
     * at any moment in time.
     */
    WriteMessage currentMessage;
    /**
     * Reference to the cache manager.
     */
    CacheManager cacheManager;

    public CacheSendPort(PortType portType, CacheIbis ibis, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
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
        
        sendPort = ibis.baseIbis.createSendPort(wrapperPortType, name, cU, props);
        System.out.println("baseibis:" + ibis.baseIbis.toString());
        System.out.println("sendport:" + sendPort.toString());
        sendPortIdentifier = new CacheSendPortIdentifier(
                ibis.identifier(), name);

        falselyConnected = new ArrayList<ReceivePortIdentifier>();
        cacheManager = ibis.cacheManager;
    }

    /**
     * Some or all of this ports connections have been closed. Restore them.
     */
    public synchronized void revive() throws IOException {
        for (ReceivePortIdentifier rpi : falselyConnected) {
            // with connection upcalls enabled,
            // the receive port upcaller will make room for this connection.
            sendPort.connect(rpi);
        }
        falselyConnected.clear();
    }

    /**
     * This method return true if it successfully caches the connection
     * between this send port and the given receive port.
     * @param rpi
     * @return
     * @throws IOException 
     */
    public synchronized boolean cache(CacheReceivePortIdentifier rpi)
            throws IOException {
        /**
         * I cannot disconnect the sendport from any receive port whilst a
         * message is alive.
         */
        if (currentMessage != null) {
            return false;
        }
        sendPort.disconnect(rpi);
        falselyConnected.add(rpi);
        return true;
    }

    @Override
    public synchronized void close() throws IOException {
        sendPort.close();
    }

    @Override
    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier receiver,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        sendPort.connect(receiver, timeoutMillis, fillTimeout);
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
        ReceivePortIdentifier rpi = new CacheReceivePortIdentifier(
                ibisIdentifier, receivePortName);
        connect(rpi, timeoutMillis, fillTimeout);

        return rpi;
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        for (ReceivePortIdentifier rpi : receivePortIdentifiers) {
            try {
                sendPort.connect(rpi, timeoutMillis, fillTimeout);
            } catch (ConnectionFailedException ex) {
                throw new ConnectionsFailedException(ex.getMessage());
            }
        }
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        int size = 0;
        ReceivePortIdentifier[] rpi =
                new CacheReceivePortIdentifier[ports.size()];
        for (IbisIdentifier ii : ports.keySet()) {
            rpi[size++] = new CacheReceivePortIdentifier(ii, ports.get(ii));
        }

        connect(rpi, timeoutMillis, fillTimeout);

        return rpi;
    }

    @Override
    public synchronized ReceivePortIdentifier[] connectedTo() {
        ReceivePortIdentifier[] retVal = new ReceivePortIdentifier[falselyConnected.size() + sendPort.connectedTo().length];

        for (int i = 0; i < falselyConnected.size(); i++) {
            retVal[i] = falselyConnected.get(i);
        }
        System.arraycopy(sendPort.connectedTo(), 0,
                retVal, falselyConnected.size(), retVal.length);

        return retVal;
    }

    @Override
    public void disconnect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws IOException {
        ReceivePortIdentifier rpi = new CacheReceivePortIdentifier(
                ibisIdentifier, receivePortName);
        disconnect(rpi);
    }

    @Override
    public synchronized void disconnect(ReceivePortIdentifier rpi)
            throws IOException {
        cacheManager.removeConnection(sendPortIdentifier, rpi);
        falselyConnected.remove(rpi);
    }

    @Override
    public PortType getPortType() {
        return sendPort.getPortType();
    }

    @Override
    public String name() {
        return sendPort.name();
    }

    @Override
    public SendPortIdentifier identifier() {
        return sendPortIdentifier;
    }

    @Override
    public synchronized ReceivePortIdentifier[] lostConnections() {
        return sendPort.lostConnections();
    }

    @Override
    public synchronized WriteMessage newMessage() throws IOException {
        /**
         * Make sure all connections are open from this send port.
         *
         * Logic: until the written message is not finished, all connections
         * must remain connected.
         */
        cacheManager.revive(this.sendPortIdentifier);
        
        /*
         * currentMessage will become null in the finish() method.
         */
        currentMessage = new CacheWriteMessage(sendPort.newMessage(), this);
        return currentMessage;
    }

    /**
     * *
     * Manageable items: (low priority, but nice to have) "Messages" the number
     * of messages sent "MessageBytes" the number of bytes sent in messages
     * (multicasts are counted once) "Bytes" the total number of bytes sent
     * "Connections" the total number of connections made with this port
     * "LostConnections" the number of lost connections "ClosedConnections" the
     * number of closed or disconnected connections
     */
    @Override
    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in CacheSendPort");
    }

    @Override
    public Map<String, String> managementProperties() {
        return null;
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
    }

    @Override
    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in CacheSendPort");
    }

    @Override
    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException("No properties in CacheSendPort");
    }
}
