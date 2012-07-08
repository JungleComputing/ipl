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
    /*
     * Set containing the send ports which will disconnect from me, but only
     * because I asked them to.
     */
    public final Set<SendPortIdentifier> initiatedCachingByMe;
    /**
     * Under-the-hood send port.
     */
    final protected ReceivePort recvPort;
    /**
     * Reference to the cache manager.
     */
    final CacheManager cacheManager;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * A reference to this receive port's connection upcaller. Need to call
     * lostConnection() from the side channel.
     */
    public final ConnectUpcaller connectUpcall;
    /**
     * A reference to this receive port's message upcaller.
     */
    public final MessageUpcaller msgUpcall;
    /*
     * Boolean too see if this cacheReceivePort is closed.
     */
    private boolean closed;
    /*
     * The current message being read.
     */
    public ReadMessage currentReadMsg;
    /*
     * Boolean to check whether msg upcalls are enabled.
     */
    private boolean enabledMessageUpcalls;
    /*
     * When a msg is alive, any send ports who wish to write to 
     * this receive port must wait until the read message is no longer alive.
     * thus they are placed in a queue.
     */
    protected final Queue<SendPortIdentifier> toHaveMyFutureAttention;
    boolean readMsgRequested;

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
            CacheManager.log.log(Level.INFO, "Created ConnectionUpcaller");
        }

        @Override
        public boolean gotConnection(ReceivePort me,
                SendPortIdentifier spi) {
            if (port.closed) {
                return false;
            }
            CacheManager.log.log(Level.INFO, "Got Connection");

            boolean accepted = true;

            if (upcaller != null) {
                accepted = upcaller.gotConnection(port, spi);
            }

            if (!accepted) {
                return false;
            }

            synchronized (port.cacheManager) {
                if (port.falselyConnected.contains(spi)) {
                    // connection was cached
                    port.falselyConnected.remove(spi);
                    port.cacheManager.restoreConnection(port.identifier(), spi);
                    port.cacheManager.notifyAll();
                } else {
                    // new connection
                    port.cacheManager.addConnection(port.identifier(), spi);
                    port.logicallyAlive.add(spi);
                }
                port.cacheManager.notifyAll();
            }

            return true;
        }

        /**
         * Synchronized in order to guarantee that at most 1 is alive at any
         * time, because this method is also called manually from the side
         * channel handling class.
         *
         * This method is called in one of the following situations: 1) a true
         * disconnect()/close() from the receive port 2) a connection caching
         * (but the SPI would be in the toBeCachedSet thanks to the side
         * channel) 3) a disconnect/close is called from the receive port, but
         * the connection was cached; (the disc/close is sent through the side
         * channel; to mark this, I set "me" to null) 4) a disconnect/close
         * generated by a caching initiated from this side; the side channel
         * sends the caching msg to sendport and it will close this connection.
         */
        @Override
        public synchronized void lostConnection(ReceivePort me,
                SendPortIdentifier spi, Throwable reason) {
            CacheManager.log.log(Level.INFO, "\n\tGot lost connection....");
            if (reason != null) {
                CacheManager.log.log(Level.INFO, "\tbecause of exception:\n{0}", reason.toString());
            }
            
            synchronized (port.cacheManager) {
                /*
                 * The connection was cached, but now it needs to be closed.
                 * scenario 3).
                 */
                if (me == null) {
                    port.falselyConnected.remove(spi);
                    port.logicallyAlive.remove(spi);
                    port.cacheManager.notifyAll();
                    return;
                }

                if (port.toBeCachedSet.contains(spi)) {
                    /*
                     * This disconnect call is actually a connection caching.
                     * scenario 2).
                     */
                    port.toBeCachedSet.remove(spi);
                    port.falselyConnected.add(spi);
                } else if (port.initiatedCachingByMe.contains(spi)) {
                    /*
                     * The connection is cached because I wanted it cached.
                     * scenario 4)
                     */
                    port.initiatedCachingByMe.remove(spi);
                    port.falselyConnected.add(spi);
                } else {
                    /*
                     * This connection is lost for good - and it was't cached.
                     * scenario 1).
                     */
                    port.logicallyAlive.remove(spi);
                }

                /*
                 * I don't want to do: port.cacheManager.cacheConnection(),
                 * because from the receive side, I never want to uncache a
                 * connection. So just remove it.
                 */
                // this connection is trully alive no longer.
                port.cacheManager.removeConnection(me.identifier(), spi);
                port.cacheManager.notifyAll();
            }

            if (upcaller != null) {
                upcaller.lostConnection(port, spi, reason);
            }
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
            this.msgUpcall = new MessageUpcaller(upcall, this);
        } else {
            this.msgUpcall = null;
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
                wrapperPortType, name, this.msgUpcall, this.connectUpcall, properties);

        falselyConnected = new ArrayList<SendPortIdentifier>();
        logicallyAlive = new HashSet<SendPortIdentifier>();
        toBeCachedSet = new HashSet<SendPortIdentifier>();
        initiatedCachingByMe = new HashSet<SendPortIdentifier>();

        cacheManager = ibis.cacheManager;
        intialPortType = portType;
        closed = false;

        enabledMessageUpcalls = false;
        currentReadMsg = null;
        readMsgRequested = false;
        
        toHaveMyFutureAttention = new LinkedList<SendPortIdentifier>();

        /*
         * Send this to the map only when it has been filled up with all data.
         */
        map.put(this.identifier(), this);
    }

    /*
     * Tell this spi to cache the connection between itself and this
     * receiveport.
     */
    public void cache(SendPortIdentifier spi) throws IOException {
        initiatedCachingByMe.add(spi);
        /*
         * Tell the SP side to cache the connection. I will count this
         * connection at the lostConnection upcall.
         */
        cacheManager.sideChannelHandler.newThreadSendProtocol(spi, this.identifier(),
                SideChannelProtocol.CACHE_FROM_RP_AT_SP);
    }

    @Override
    public void close() throws IOException {
        close(0);
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        /*
         * Wait until all logically alive connections are closed. TODO: handle
         * timeoutMillis.
         */
        synchronized (cacheManager) {
            if(closed) {
                return ;
            }
            closed = true;
            while (!logicallyAlive.isEmpty()) {
                try {
                    CacheManager.log.log(Level.INFO, "Waiting for these "
                            + "connections to close: {0}", logicallyAlive);
                    cacheManager.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
        CacheManager.log.log(Level.INFO, "Closing base receive port...");
        recvPort.close(timeoutMillis);
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        return logicallyAlive.toArray(
                new SendPortIdentifier[logicallyAlive.size()]);
    }

    @Override
    public void disableConnections() {
        recvPort.disableConnections();
    }

    @Override
    public void disableMessageUpcalls() {
        enabledMessageUpcalls = false;
        recvPort.disableMessageUpcalls();
    }

    @Override
    public void enableConnections() {
        recvPort.enableConnections();
    }

    @Override
    public void enableMessageUpcalls() {
        recvPort.enableMessageUpcalls();
        enabledMessageUpcalls = true;
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
    public synchronized ReadMessage poll() throws IOException {
        
        if(currentReadMsg != null) {
            return null;
        }
        
        ReadMessage msg = recvPort.poll();
        if (msg != null) {
            readMsgRequested = false;
            currentReadMsg = new CacheReadMessage.CacheReadDowncallMessage(msg, this);
            return currentReadMsg;
        }
        return null;
    }

    @Override
    public ReadMessage receive() throws IOException {
        return receive(0);
    }

    @Override
    public ReadMessage receive(long timeoutMillis) throws IOException {
        if(enabledMessageUpcalls) {
            throw new IbisConfigurationException("Using explicit receive"
                    + " when message upcalls are enabled.");
        }
        long deadline = 0;
        if(timeoutMillis > 0) {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }
        
        synchronized (this) {
            while (currentReadMsg != null) {
                try {
                    this.wait();
                } catch (InterruptedException ignoreMe) {
                }
            }
        }

        if(deadline > 0) {
            timeoutMillis = deadline - System.currentTimeMillis();
            if (timeoutMillis <= 0) {
                throw new ReceiveTimedOutException();
            }
        }
        ReadMessage m = recvPort.receive(timeoutMillis);
        synchronized(this) {
            readMsgRequested = false;
            currentReadMsg = new CacheReadMessage.CacheReadDowncallMessage(m, this);
        }

        return currentReadMsg;
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
