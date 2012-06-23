package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    List<ReceivePortIdentifier> falselyConnected;
    /**
     * Under-the-hood send port.
     */
    final SendPort sendPort;
    /**
     * This send port's identifier.
     */
    final SendPortIdentifier sendPortIdentifier;
    /**
     * This port's current message (if it exists). we know there is at most one
     * at any moment in time.
     */
    volatile WriteMessage currentMessage;
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
        sendPort = ibis.baseIbis.createSendPort(portType, name, cU, props);
        sendPortIdentifier = new CacheSendPortIdentifier(
                ibis.identifier(), name);

        falselyConnected = new ArrayList<ReceivePortIdentifier>();
        cacheManager = ibis.cacheManager;
    }

    public void revive() throws IOException {
        /**
         * Some or all of this ports connections have been closed. Restore them.
         */
        for(ReceivePortIdentifier rpi : falselyConnected) {
            guaranteeOneFreeResourceFrom(rpi);
            sendPort.connect(rpi);
        }
    }
    
    public boolean cache(CacheReceivePortIdentifier rpi) throws IOException {
        /**
         * I cannot cache (disconnect) the sendport from any receive port whilst
         * a message is alive.
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
    
    /**
     * This method makes sure that the receiving ibis has/will have
     * an open receive port to let this sendport connect to it.
     * @param rpi
     * @param timeoutMillis
     * @param fillTimeout
     * @throws ConnectionFailedException 
     */
    private synchronized void safeConnect(ReceivePortIdentifier rpi, long timeoutMillis, 
            boolean fillTimeout) throws ConnectionFailedException {
        guaranteeOneFreeResourceFrom(rpi);
        sendPort.connect(rpi, timeoutMillis, fillTimeout);
    }

    /**
     * messages exchanged:
     * SP: need at least 1 free resource
     * RP: if I don't have it, I'll make one free. 
     *     Then i'll reserve it for you.
     * @param rpi 
     */
    private void guaranteeOneFreeResourceFrom(ReceivePortIdentifier rpi) {
        // TODO: fill this up
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
        safeConnect(receiver, timeoutMillis, fillTimeout);
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

        safeConnect(rpi, timeoutMillis, fillTimeout);
        
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
        for(ReceivePortIdentifier rpi : receivePortIdentifiers) {
            try {
                safeConnect(rpi, timeoutMillis, fillTimeout);
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
            rpi[size] = new CacheReceivePortIdentifier(ii, ports.get(ii));
            try {
                safeConnect(rpi[size++], timeoutMillis, fillTimeout);
            } catch (ConnectionFailedException ex) {
                throw new ConnectionsFailedException(ex.getMessage());
            }
        }

        return rpi;
    }

    @Override
    public synchronized ReceivePortIdentifier[] connectedTo() {
        ReceivePortIdentifier[] retVal = new ReceivePortIdentifier[
                falselyConnected.size() + sendPort.connectedTo().length];

        for (int i = 0; i < falselyConnected.size(); i++) {
            retVal[i] = falselyConnected.get(i);
        }
        System.arraycopy(sendPort.connectedTo(), 0,
                retVal, falselyConnected.size(), retVal.length);

        return retVal;
    }

    @Override
    public synchronized void disconnect(ReceivePortIdentifier receiver)
            throws IOException {
        cacheManager.removeConnection(sendPortIdentifier, receiver);
        falselyConnected.remove(receiver);
    }

    @Override
    public synchronized void disconnect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws IOException {
        ReceivePortIdentifier rpi = new CacheReceivePortIdentifier(
                ibisIdentifier, receivePortName);
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
