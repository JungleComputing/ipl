package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheSendPort implements SendPort, CacheObject {

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
     * Boolean which determines if the under-the-hood port is actually
     * alive or cached.
     */
    boolean isCached = false;
    /**
     * Boolean which determines if the port is closed or not.
     */
    boolean closed = false;
    /**
     * List of receive port identifiers to which this send port is logically
     * connected to.
     */
    List<ReceivePortIdentifier> connectedTo;
    /**
     * Set of lost connections. This is properly updated even after caching and
     * re-using.
     */
    Set<ReceivePortIdentifier> lostConnections;
    /**
     * Required information to restore the SendPort after it has been closed
     * (cached).
     */
    SendPortCacheInfo cacheInfo;
    /**
     * Under-the-hood send port.
     */
    SendPort sendPort;
    /**
     * This send port's identifier.
     */
    SendPortIdentifier sendPortIdentifier;
    /**
     * This port's current message (if it exists). we know there is at
     * most one at any moment in time.
     */
    WriteMessage currentMessage;

    public CacheSendPort(PortType portType, CacheIbis ibis, String name,
            SendPortDisconnectUpcall cU, Properties props) throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }
        cacheInfo = new SendPortCacheInfo(ibis, portType, name, cU, props);
        sendPort = cacheInfo.restoreSendPort();
        sendPortIdentifier = new CacheSendPortIdentifier(
                ibis.identifier(), name);

        connectedTo = new ArrayList<ReceivePortIdentifier>();
        lostConnections = new HashSet<ReceivePortIdentifier>();
    }

    @Override
    synchronized public void revive() throws CachingException {
        try {
            if(!isCached) {
                return ;
            }
            
            /**
             * Create a new send port object with the same properties.
             */
            sendPort = cacheInfo.restoreSendPort();
            
            /**
             * Connect it to the receive ports it should be connected to.
             */
            sendPort.connect(connectedTo.toArray(
                    new ReceivePortIdentifier[connectedTo.size()]));

            isCached = false;
        } catch (IOException ex) {
            throw new CachingException("Failed to revive cached port:\n", ex);
        }
    }

    @Override
    synchronized public void cache() throws CachingException {
        try {
            if(isCached) {
                return ;
            }
            /*
             * This port may have some lost connections. When closed, those lost 
             * connections will be lost. Need to store them.
             */
            lostConnections.addAll(Arrays.asList(sendPort.lostConnections()));
            
            /*
             * TODO: serious problem
             * What if there is a live message?! -> uncacheble.
             */
            if(currentMessage != null) {
                throw new CachingException("Cannot cache send port: message still alive.");
            }
            
            /*
             * Safe to close the port, since I know for sure it's open.
             */
            sendPort.close();
            
            /**
             * This send port is as good as dead. Kill the reference.
             * Now that I look at it, the isCached field is redundant:
             * isCached == true iff sendPort == null.
             * but lets say it's better for readability.
             */
            sendPort = null;
            
            /*
             * Got all the information I need in: connectedTo, lostConnections
             * and cacheInfo.
             */
            isCached = true;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to revive cached port:\n", ex);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if(closed) {
            throw new IOException("Port already closed.");
        }
        closed = true;
        
        if(!this.isCached) {
            sendPort.close();
            sendPort = null;
        }
        /*
         * Who cares if it's cached or not?! The port is closed.
         */
        isCached = false;

        /*
         * Delete references to the inside objects. 
         * Not sure if this is necessary.
         */
        connectedTo = null;
        lostConnections = null;
        cacheInfo = null;
        sendPort = null;
    }

    @Override
    public void connect(ReceivePortIdentifier receiver)
            throws ConnectionFailedException {
        connect(receiver, 0, true);
    }

    /**
     * Synchronized method to connect to a {@link ibis.ipl.ReceivePortIdentifier}.
     * @param receiver
     * @param timeoutMillis
     * @param fillTimeout
     * @throws ConnectionFailedException 
     */
    @Override
    public synchronized void connect(ReceivePortIdentifier receiver,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        /*
         * When connecting, we must open the port, otherwise 
         * we cannot throw exceptions such as: connection refused, 
         * already connected, etc. Plus, we cannot state that we will 
         * connect this port later on when we re-use it, since we have 
         * a timeout to respect.
         * Connecting means taking the port from the cache (if it is there).
         */
        if(isCached) {
                /*
                 * I think I'm doing this right:
                 * ask the DataStructure to revive me (the SendPort)
                 * since it may want to cache something else....
                 * 
                 * but then, I want to make sure I also get my work done
                 * 
                 * But this works:
                 * - when I enter this method, I get the lock on this SendPort's monitor
                 * - I ask the cache structure to revive me
                 * - I get revived, I'm back here and I do my stuff.
                 */
                CacheStructure.revive(this);
        }
        sendPort.connect(receiver, timeoutMillis, fillTimeout);
        connectedTo.add(receiver);
    }

    @Override
    public ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName) throws ConnectionFailedException {
        return connect(ibisIdentifier, receivePortName, 0, true);
    }

    @Override
    public synchronized ReceivePortIdentifier connect(IbisIdentifier ibisIdentifier,
            String receivePortName, long timeoutMillis, boolean fillTimeout)
            throws ConnectionFailedException {
        if(isCached) {
            CacheStructure.revive(this);
        }
        ReceivePortIdentifier rpi = sendPort.connect(ibisIdentifier,
                receivePortName, timeoutMillis, fillTimeout);
        connectedTo.add(rpi);
        return rpi;
    }

    @Override
    public void connect(ReceivePortIdentifier[] receivePortIdentifiers)
            throws ConnectionsFailedException {
        connect(receivePortIdentifiers, 0, true);
    }

    @Override
    public synchronized void connect(ReceivePortIdentifier[] receivePortIdentifiers,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        if(isCached) {
            CacheStructure.revive(this);
        }
        sendPort.connect(receivePortIdentifiers, timeoutMillis, fillTimeout);
        connectedTo.addAll(Arrays.asList(receivePortIdentifiers));
    }

    @Override
    public ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports)
            throws ConnectionsFailedException {
        return connect(ports, 0, true);
    }

    @Override
    public synchronized ReceivePortIdentifier[] connect(Map<IbisIdentifier, String> ports,
            long timeoutMillis, boolean fillTimeout)
            throws ConnectionsFailedException {
        if(isCached) {
            CacheStructure.revive(this);
        }
        ReceivePortIdentifier[] retVal = sendPort.connect(ports,
                timeoutMillis, fillTimeout);
        connectedTo.addAll(Arrays.asList(retVal));
        return retVal;
    }

    @Override
    public synchronized ReceivePortIdentifier[] connectedTo() {
        return connectedTo.toArray(new ReceivePortIdentifier[connectedTo.size()]);
    }

    @Override
    public synchronized void disconnect(ReceivePortIdentifier receiver) 
            throws IOException {
        if(!isCached) {
            sendPort.disconnect(receiver);
        }
        connectedTo.remove(receiver);
    }

    @Override
    public synchronized void disconnect(IbisIdentifier ibisIdentifier, 
            String receivePortName) throws IOException {
        if(!isCached) {
            sendPort.disconnect(ibisIdentifier, receivePortName);
        }

        /*
         * This will work, because the equals method is overriden,
         * and all elements in connectedTo are CacheReceivePortIdentifiers.
         */
        connectedTo.remove(new CacheReceivePortIdentifier(
                ibisIdentifier, receivePortName));
    }

    @Override
    public PortType getPortType() {
        return cacheInfo.portType;
    }
    
    @Override
    public String name() {
        return cacheInfo.name;
    }

    @Override
    public SendPortIdentifier identifier() {
        return sendPortIdentifier;
    }

    @Override
    public synchronized ReceivePortIdentifier[] lostConnections() {
        if (!isCached) {
            lostConnections.addAll(Arrays.asList(sendPort.lostConnections()));
        }
        /*
         * The lostConnections object was already updated when cached. Just
         * return it and clear it.
         */
        ReceivePortIdentifier[] result = lostConnections.toArray(
                new ReceivePortIdentifier[lostConnections.size()]);
        lostConnections.clear();
        return result;
    }

    @Override
    public synchronized WriteMessage newMessage() throws IOException {
        if(isCached) {
            CacheStructure.revive(this);
        }
        /*
         * currentMessage will become null in the finish() methods.
         */
        currentMessage = new CacheWriteMessage(sendPort.newMessage(), this);
        return currentMessage;
    }

    /**
     * *
     * Manageable items: (low priority, but nice to have)
     * "Messages"           the number of messages sent
     * "MessageBytes"       the number of bytes sent in messages
     *                              (multicasts are counted once) 
     * "Bytes"              the total number of bytes sent
     * "Connections"        the total number of connections made with this port
     * "LostConnections"    the number of lost connections
     * "ClosedConnections"  the number of closed or disconnected connections
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
