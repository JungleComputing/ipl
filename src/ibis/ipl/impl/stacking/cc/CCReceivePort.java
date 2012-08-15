package ibis.ipl.impl.stacking.cc;

import ibis.io.SerializationFactory;
import ibis.io.SerializationInput;
import ibis.ipl.*;
import ibis.ipl.impl.stacking.cc.io.BufferedDataInputStream;
import ibis.ipl.impl.stacking.cc.io.DowncallBufferedDataInputStream;
import ibis.ipl.impl.stacking.cc.io.UpcallBufferedDataInputStream;
import ibis.ipl.impl.stacking.cc.manager.CCManager;
import ibis.ipl.impl.stacking.cc.sidechannel.SideChannelProtocol;
import ibis.ipl.impl.stacking.cc.util.Loggers;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class CCReceivePort implements ReceivePort {

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
     * Map to store identifiers to the receive ports.
     */
    public static final Map<ReceivePortIdentifier, CCReceivePort> map;

    static {
        anonymousPortCounter = new AtomicInteger(0);
        ANONYMOUS_PREFIX = "anonymous CC receive port";

        map = new HashMap<ReceivePortIdentifier, CCReceivePort>();
    }
    /*
     * Set containing live connections which will be cached.
     */
    public final Set<SendPortIdentifier> toBeCachedSet;
    /*
     * Set containing the send ports which will disconnect from me, but only
     * because I asked them to.
     */
    public final Set<SendPortIdentifier> cachingInitiatedByMeSet;
    /**
     * Under-the-hood send port.
     */
    public final ReceivePort recvPort;
    /**
     * Reference to the CCManager.
     */
    public final CCManager ccManager;
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
     * Boolean too see if this ReceivePort is closed.
     */
    protected boolean closed;
    /*
     * The current message being read.
     */
    public CCReadMessage currentLogicalReadMsg;
    /*
     * Boolean to check whether msg upcalls are enabled.
     */
    private boolean enabledMessageUpcalls;
    /*
     * When a msg is alive, any send ports who wish to write to this receive
     * port must wait until the read message is no longer alive. thus they are
     * placed in a queue.
     */
    public final List<SequencedSpi> toHaveMyFutureAttention;
    public boolean readMsgRequested;
    private long localSeqNo;
    
    /**
     * For the ReadMessage.
     */
    public final BufferedDataInputStream upcallDataIn;
    protected final BufferedDataInputStream downcallDataIn;
    protected final SerializationInput upcallSerIn;
    protected final SerializationInput downcallSerIn;
    protected BufferedDataInputStream dataIn;
    protected SerializationInput serIn;
    
    public CCIbis ccIbis;

    public CCReceivePort(PortType portType, CCIbis ccIbis,
            String name, MessageUpcall upcall, ReceivePortConnectUpcall connectUpcall,
            Properties properties)
            throws IOException {
        if (name == null) {
            name = ANONYMOUS_PREFIX + " "
                    + anonymousPortCounter.getAndIncrement();
        }
        
        this.ccIbis = ccIbis;

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
        portCap.addAll(CCIbis.additionalPortCapabilities);
        PortType wrapperPortType = new PortType(portCap.toArray(
                new String[portCap.size()]));

        recvPort = ccIbis.baseIbis.createReceivePort(
                wrapperPortType, name, this.msgUpcall, this.connectUpcall, properties);

        toBeCachedSet = new HashSet<SendPortIdentifier>();
        cachingInitiatedByMeSet = new HashSet<SendPortIdentifier>();

        ccManager = ccIbis.ccManager;
        intialPortType = portType;
        closed = false;

        enabledMessageUpcalls = false;
        currentLogicalReadMsg = null;
        readMsgRequested = false;

        toHaveMyFutureAttention = new LinkedList<SequencedSpi>();
        localSeqNo = -1;
        
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
        this.upcallDataIn = new UpcallBufferedDataInputStream(this);
        this.upcallSerIn = SerializationFactory.createSerializationInput(serialization, upcallDataIn);
        
        this.downcallDataIn = new DowncallBufferedDataInputStream(this);
        this.downcallSerIn = SerializationFactory.createSerializationInput(serialization, downcallDataIn);
        
        this.dataIn = downcallDataIn;
        this.serIn = downcallSerIn;

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
        ccManager.reserveLiveConnection(this.identifier(), spi);
        ccManager.lock.unlock();
        Loggers.lockLog.log(Level.INFO, "Lock released before"
                + " caching initiation from RP.");

        try {
            synchronized (cachingInitiatedByMeSet) {
                cachingInitiatedByMeSet.add(spi);
                /*
                 * Tell the SP side to cache the connection. I will count this
                 * connection at the lostConnection upcall.
                 */
                ccManager.sideChannelHandler.newThreadSendProtocol(
                        this.identifier(), spi,
                        SideChannelProtocol.CACHE_FROM_RP_AT_SP);

                while (cachingInitiatedByMeSet.contains(spi)) {
                    try {
                        cachingInitiatedByMeSet.wait();
                    } catch (InterruptedException ignoreMe) {
                    }
                }
            }
        } finally {
            ccManager.lock.lock();
            Loggers.lockLog.log(Level.INFO, "Lock reaquired and connection is "
                    + "cached.");
            Loggers.lockLog.log(Level.INFO, "");
            ccManager.unReserveLiveConnection(this.identifier(), spi);
        }
    }

    @Override
    public void close() throws IOException {
        close(0);
    }

    @Override
    public void close(long timeoutMillis) throws IOException {
        long deadline;
        if(timeoutMillis < 0) {
            throw new IOException("Negative timeout value at receivePort.close()");
        } else if(timeoutMillis == 0) {
            deadline = Long.MAX_VALUE;
        } else {
            deadline = System.currentTimeMillis() + timeoutMillis;
        }
        
        Loggers.conLog.log(Level.INFO, "Closing receive port\t{0}", this.identifier());
        
        /*
         * Wait until all logically alive connections are closed.
         */
        ccManager.lock.lock();
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        try {
            if (closed) {
                return;
            }
            closed = true;
            dataIn.close();
            while (ccManager.hasConnections(this.identifier()) && 
                    (System.currentTimeMillis() < deadline)) {
                try {
                    Loggers.lockLog.log(Level.INFO, "Lock will be released:"
                            + " waiting on connections to be closed.");
                    ccManager.allClosedCondition.await(
                            deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    Loggers.lockLog.log(Level.INFO, "Lock reaquired.");
                } catch (InterruptedException ignoreMe) {
                }
            }
        } finally {
            ccManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
        }
        
        Loggers.conLog.log(Level.INFO, "{0} is closing base receive port...",
                this.identifier());
        timeoutMillis = deadline - System.currentTimeMillis();
        if(timeoutMillis == 0) {
            timeoutMillis = -1;
        }
        recvPort.close(timeoutMillis);
    }

    @Override
    public SendPortIdentifier[] connectedTo() {
        ccManager.lock.lock();
        Loggers.lockLog.log(Level.INFO, "Lock locked.");
        try {
            return ccManager.allSpisFrom(this.identifier());
        } finally {
            ccManager.lock.unlock();
            Loggers.lockLog.log(Level.INFO, "Lock unlocked.");
        }
    }

    @Override
    public void disableConnections() {
        recvPort.disableConnections();
    }

    @Override
    public void disableMessageUpcalls() {
        enabledMessageUpcalls = false;
        synchronized(this) {
            while (this.currentLogicalReadMsg != null) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {}
            }
        }
        recvPort.disableMessageUpcalls();
        dataIn = downcallDataIn;
        serIn = downcallSerIn;
    }

    @Override
    public void enableConnections() {
        recvPort.enableConnections();
    }

    @Override
    public void enableMessageUpcalls() {
        enabledMessageUpcalls = true;
        synchronized(this) {
            while (this.currentLogicalReadMsg != null) {
                try {
                    this.wait();
                } catch (InterruptedException ex) {}
            }
        }
        recvPort.enableMessageUpcalls();
        dataIn = upcallDataIn;
        serIn = upcallSerIn;
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

        if (currentLogicalReadMsg != null) {
            return null;
        }

        ReadMessage msg = recvPort.poll();
        if (msg != null) {
            readMsgRequested = false;
            dataIn.isLastPart = msg.readBoolean();
            dataIn.remainingBytes = msg.readInt();
            currentLogicalReadMsg = new CCReadMessage(msg, this);
            return currentLogicalReadMsg;
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
            while (currentLogicalReadMsg != null) {
                try {
                    if(deadline > 0) {
                        long timeout = deadline - System.currentTimeMillis();
                        if(timeout <= 0) {
                            throw new ReceiveTimedOutException();
                        }
                        wait(timeout);
                    } else {
                        wait();
                    }
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
        ReadMessage msg = recvPort.receive(timeoutMillis);
        synchronized (this) {
            readMsgRequested = false;
            dataIn.isLastPart = msg.readBoolean();
            dataIn.remainingBytes = msg.readInt();
            currentLogicalReadMsg = new CCReadMessage(msg, this);
        }

        return currentLogicalReadMsg;
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
    
    
    public boolean isNextSeqNo(long seqNo) {
        if(seqNo < 0 || seqNo == localSeqNo + 1) {
            return true;
        }
        
        return false;
    }
    
    public void incSeqNo(long seqNo) {
        if(seqNo >= 0) {
            localSeqNo = seqNo;
        }
    }
    
    public static final class SequencedSpi {
        
        SendPortIdentifier spi;
        long seqNo;

        public SequencedSpi(long seqNo, SendPortIdentifier spi) {
            this.seqNo = seqNo;
            this.spi = spi;
        }
    }
}
