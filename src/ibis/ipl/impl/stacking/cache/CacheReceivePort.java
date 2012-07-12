package ibis.ipl.impl.stacking.cache;

import ibis.ipl.*;
import ibis.ipl.impl.stacking.cache.manager.CacheManager;
import ibis.ipl.impl.stacking.cache.sidechannel.SideChannelProtocol;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

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
    public final ReceivePort recvPort;
    /**
     * Reference to the cache manager.
     */
    public final CacheManager cacheManager;
    /**
     * Keep this port's original capabilities for the user to see.
     */
    private final PortType intialPortType;
    /**
     * A reference to this receive port's connection upcaller. Need to call
     * lostConnection() from the side channel.
     */
    public final ReceivePortConnectionUpcaller connectUpcall;
    /**
     * A reference to this receive port's message upcaller.
     */
    public final MessageUpcaller msgUpcall;
    /*
     * Boolean too see if this cacheReceivePort is closed.
     */
    protected boolean closed;
    /*
     * The current message being read.
     */
    public ReadMessage currentReadMsg;
    /*
     * Boolean to check whether msg upcalls are enabled.
     */
    private boolean enabledMessageUpcalls;
    /*
     * When a msg is alive, any send ports who wish to write to this receive
     * port must wait until the read message is no longer alive. thus they are
     * placed in a queue.
     */
    public final Queue<SendPortIdentifier> toHaveMyFutureAttention;
    public boolean readMsgRequested;

    public CacheReceivePort(PortType portType, CacheIbis ibis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties)
            throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }

        this.connectUpcall = new ReceivePortConnectionUpcaller(connectUpcall, this);

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
        cacheManager.sideChannelHandler.newThreadSendProtocol(this.identifier(), spi,
                SideChannelProtocol.CACHE_FROM_RP_AT_SP);
    }

    @Override
    public void close() throws IOException {
        close(0);
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        long deadline;
        if(timeoutMillis < 0) {
            deadline = -1;
        } else if(timeoutMillis == 0) {
            deadline = Long.MAX_VALUE;
        } else {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }
        
        /*
         * Wait until all logically alive connections are closed.
         */
        synchronized (cacheManager) {
            if (closed) {
                return;
            }
            closed = true;
            while (cacheManager.hasConnections(this.identifier()) && 
                    (System.currentTimeMillis() < deadline)) {
                try {
                    CacheManager.log.log(Level.INFO, "Waiting for some"
                            + " connections to close.");
                    cacheManager.wait(deadline - System.currentTimeMillis());
                } catch (InterruptedException ignoreMe) {
                }
            }
        }
        
        CacheManager.log.log(Level.INFO, "Closing base receive port...");
        recvPort.close(timeoutMillis);
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        return cacheManager.allSpisFrom(this.identifier());
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

        if (currentReadMsg != null) {
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
        if (enabledMessageUpcalls) {
            throw new IbisConfigurationException("Using explicit receive"
                    + " when message upcalls are enabled.");
        }
        long deadline = 0;
        if (timeoutMillis > 0) {
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

        if (deadline > 0) {
            timeoutMillis = deadline - System.currentTimeMillis();
            if (timeoutMillis <= 0) {
                throw new ReceiveTimedOutException();
            }
        }
        ReadMessage m = recvPort.receive(timeoutMillis);
        synchronized (this) {
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
