package ibis.impl.net;

import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceiveTimedOutException;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides an implementation of the {@link ReceivePort} and {@link
 * ReadMessage} interfaces of the IPL.
 */
public final class NetReceivePort extends NetPort implements ReceivePort,
        ReadMessage, NetInputUpcall, NetEventQueueConsumer {

    /* ___ INTERNAL CLASSES ____________________________________________ */

    /* --- incoming connection manager thread -- */

    /**
     * The incoming connection management thread class.
     */
    private final class AcceptThread extends Thread {

        /**
         * Flag indicating whether thread termination was requested.
         */
        private boolean end = false;

        public AcceptThread(String name) {
            super("NetReceivePort.AcceptThread: " + name);
            setDaemon(true);
        }

        /**
         * The incoming connection management function.
         */
        /*
         * // Note: the thread is <strong>uninterruptible</strong>
         * // during the network input locking.
         */
        public void run() {
            log.in("accept thread starting");

            accept_loop: while (!end) {
                NetServiceLink link = null;
                try {
                    link = new NetServiceLink(eventQueue, serverSocket);
                } catch (ConnectionTimedOutException e) {
                    continue accept_loop;
                } catch (InterruptedIOException e) {
                    continue accept_loop;
                } catch (IOException e) {
                    __.fwdAbort__(e);
                }

                Integer num = null;
                NetSendPortIdentifier spi = null;
                long startSeqno = 0;

                num = new Integer(nextSendPortNum++);

                try {
                    link.init(num);
                } catch (IOException e) {
                    __.fwdAbort__(e);
                }

                String peerPrefix = null;

                try {
                    ObjectInputStream is = new ObjectInputStream(link
                            .getInputSubStream("__port__"));
                    spi = (NetSendPortIdentifier) is.readObject();
                    int rank = is.readInt();
                    int spmid = is.readInt();
                    startSeqno = is.readLong();

                    trace.disp(receivePortTracePrefix
                            + "New connection from: _s" + rank + "-" + spmid
                            + "_");

                    peerPrefix = "_s" + rank + "-" + spmid + "_";

                    is.close();
                    ObjectOutputStream os = new ObjectOutputStream(link
                            .getOutputSubStream("__port__"));
                    os.writeInt(receivePortMessageRank);
                    os.writeInt(receivePortMessageId);
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    __.fwdAbort__(e);
                } catch (ClassNotFoundException e) {
                    __.fwdAbort__(e);
                }

                connect_loop: while (!end) {
                    try {
                        connectionLock.ilock();
                        try {
                            //inputLock.lock();

                            NetConnection cnx = new NetConnection(
                                    NetReceivePort.this, num, spi, identifier,
                                    link, startSeqno);

                            synchronized (connectionTable) {
                                connectionTable.put(num, cnx);

                                connectedPeers.add(spi);
                                if (rpcu != null) {
                                    rpcu
                                            .gotConnection(NetReceivePort.this,
                                                    spi);
                                }

                                if (connectionTable.size() == 1) {
                                    singleConnection = cnx;
                                } else {
                                    singleConnection = null;
                                }
                                if (connectionTable.size() > maxLiveConnections) {
                                    maxLiveConnections = connectionTable.size();
                                }
                            }

                            input.setupConnection(cnx);
                            input.startReceive();

                            //inputLock.unlock();
                        } finally {
                            connectionLock.unlock();
                        }
                    } catch (InterruptedIOException e) {
                        System.err.println(NetIbis.hostName()
                                + ": While connecting meet " + e);
                        continue connect_loop;
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }

                    break connect_loop;
                }
            }

            log.out("accept thread leaving");
        }

        /**
         * Requests for the thread completion.
         */
        /*
         protected void prepareEnd() {
         end = true;

         if (serverSocket != null) {
         try {
         serverSocket.close();
         } catch (Exception e) {
         throw new Error(e);
         }
         }
         }

         protected void end() {

         this.interrupt();
         }
         */

        protected void end() throws IOException {
            log.in();
            synchronized (connectionLock) {
                end = true;
            }

            if (serverSocket != null) {
                serverSocket.close();
            }

            log.out();
        }

    }

    /* ___ CONFIGURATION FLAGS _________________________________________ */

    private boolean useUpcall = false;

    /**
     * Flag indicating whether unsuccessful active polling should be followed by
     * a yield.
     *
     * Note: this flag only affects asynchronous multithreaded
     * polling or synchronous {@link #receive} operations. In
     * particular, the synchronous {@link #poll} operation is not
     * affected.
     */
    private boolean useYield = true;

    /**
     * Flag indicating whether receive should block in the poll()
     * that necessarily precedes it, or whether we want to poll in a
     * busy-wait style from this.
     */
    public static final boolean useBlockingPoll = true;

    /* ___ EVENT QUEUE _________________________________________________ */

    private NetEventQueue eventQueue = null;

    private NetEventQueueListener eventQueueListener = null;

    /* ___ LESS-IMPORTANT OBJECTS ______________________________________ */

    /**
     * The upcall callback function.
     */
    private Upcall upcall = null;

    private ReceivePortConnectUpcall rpcu = null;

    /**
     * The port identifier.
     */
    private NetReceivePortIdentifier identifier = null;

    /**
     * The TCP server socket.
     */
    private ServerSocket serverSocket = null;

    /**
     * The next send port integer number.
     */
    private int nextSendPortNum = 0;

    /**
     * Performance statistic
     */
    // private int                  n_yield;
    /**
     * Maintain a linked list for cleanup
     */
    NetReceivePort next = null;

    /* ___ IMPORTANT OBJECTS ___________________________________________ */

    /**
     * Cache a single connection for fast lookup in the frequent case of one
     * connection
     */
    private NetConnection singleConnection = null;

    /**
     * The port's topmost input.
     */
    private NetInput input = null;

    /* ___ THREADS _____________________________________________________ */

    /**
     * The incoming connection management thread.
     */
    private AcceptThread acceptThread = null;

    /* ___ STATE _______________________________________________________ */

    /**
     * The current active peer port.
     */
    private Integer activeSendPortNum = null;

    /**
     * Flag indicating whether incoming connections are currently enabled.
     */
    private boolean connectionEnabled = false;

    /**
     * Flag indicating whether successful polling operation should
     * generate an upcall or not.
     */
    private boolean upcallsEnabled = false;

    /**
     * The empty message detection flag.
     *
     * The flag is set on each new {@link #_receive} call and should
     * be cleared as soon as at least a byte as been added to the living message.
     */
    private boolean emptyMsg = true;

    /**
     * Indicate whether {@link #finish} should unlock the {@link #finishMutex}.
     */
    private boolean finishNotify = false;

    /**
     * Indicate whether {@link #finish} should unlock the {@link #pollingLock}
     */
    private boolean pollingNotify = false;

    /**
     * Reference the current upcall thread.
     *
     */
    private Runnable currentThread = null;

    /**
     * Internal receive port counter, for debugging.
     */
    static private int receivePortCount = 0;

    /**
     * Seqno for numbered messages
     */
    private long messageSeqno = -1;

    /**
     * Internal receive port id, for debugging.
     */
    private int receivePortMessageId = -1;

    /**
     * Process rank, for debugging.
     */
    private int receivePortMessageRank = 0;

    /**
     * Tracing log message prefix, for debugging.
     *
     */
    private String receivePortTracePrefix = null;

    private Vector connectedPeers = null;

    private Vector disconnectedPeers = null;

    private int maxLiveConnections = 0;

    private boolean closed = false;

    /* ___ LOCKS _______________________________________________________ */

    /**
     * The polling autorisation lock.
     */
    private NetMutex pollingLock = null;

    /**
     * The message extraction autorisation lock.
     */
    private NetMutex polledLock = null;

    /**
     * The incoming connection acceptation lock.
     */
    private NetMutex connectionLock = null;

    /**
     * The network input synchronization lock.
     */
    private ibis.util.Monitor inputLock = null;

    private NetMutex finishMutex = null;

    private Object dummyUpcallSync = new Object();

    private int upcallsPending = 0;

    /**
     * Make a fast path for the (frequent) case that there is only one
     * connection
     */
    private NetConnection getActiveConnection() {
        if (activeSendPortNum == null) {
            return null;
        }

        if (singleConnection != null) {
            return singleConnection;
        }

        return (NetConnection) connectionTable.get(activeSendPortNum);
    }

    private NetConnection checkClose() {

        NetConnection cnx = getActiveConnection();
        cnx.msgSeqno++;
        if (cnx.msgSeqno >= cnx.closeSeqno) {
            synchronized (connectionTable) {
                connectionTable.notifyAll();
            }
        }

        return cnx;
    }

    /* --- Upcall from main input object -- */
    public void inputUpcall(NetInput input, Integer spn) throws IOException {
        log.in();
        if (this.input == null) {
            __.warning__("message lost");
            return;
        }

        if (spn == null) {
            throw new Error("invalid state: NetReceivePort.inputUpcall");
        }

        activeSendPortNum = spn;

        if (upcall != null && !upcallsEnabled) {
            synchronized (dummyUpcallSync) {
                upcallsPending++;
                while (!upcallsEnabled) {
                    try {
                        dummyUpcallSync.wait();
                    } catch (InterruptedException e) {
                        // Go on waiting
                    }
                }
                upcallsPending--;
            }
        }

        if (upcall != null && upcallsEnabled) {
            final ReadMessage rm = _receive();
            Thread me = Thread.currentThread();
            currentThread = me;
            upcall.upcall(rm);
            if (me == currentThread) {
                currentThread = null;

                if (emptyMsg) {
                    input.handleEmptyMsg();

                    emptyMsg = false;
                }

                checkClose();

                trace.disp(receivePortTracePrefix, "message receive <--");
            }
        } else {
            synchronized (dummyUpcallSync) {
                finishNotify = true;
                polledLock.unlock();
                finishMutex.lock();
            }
        }
        log.out();
    }

    public void closeFromRemote(NetConnection cnx) {

        synchronized (connectionTable) {
            if (cnx.regularClosers > 0) {
                connectionTable.notifyAll();
            }
        }
    }

    /* --- NetEventQueueConsumer part --- */
    public void event(NetEvent e) {
        log.in();
        NetPortEvent event = (NetPortEvent) e;

        log.disp("IN: event.code() = ", event.code());

        switch (event.code()) {
        case NetPortEvent.CLOSE_EVENT: {
            Integer num = (Integer) event.arg();
            NetConnection cnx;

            boolean timed_out = false;
            while (true) {
                synchronized (connectionTable) {
                    cnx = (NetConnection) connectionTable.get(num);
                }
                if (cnx == null) {
                    return;
                }

                if (timed_out) {
                    break;
                }

                if (cnx.closeSeqno != Long.MAX_VALUE) {
                    // Maybe we overtook the regular disconnect.
                    // Give it a little time to finish
                    try {
                        Thread.sleep(1000);
                        timed_out = true;
                    } catch (InterruptedException ei) {
                        // Give up
                    }
                }
            }

            NetSendPortIdentifier nspi = null;

            synchronized (connectionTable) {

                nspi = cnx.getSendId();

                cnx = (NetConnection) connectionTable.remove(cnx.getNum());
                if (connectionTable.size() == 1) {
                    Enumeration elts = connectionTable.elements();
                    singleConnection = (NetConnection) elts.nextElement();
                } else {
                    singleConnection = null;
                }

                disconnectedPeers.add(nspi);
            }

            if (rpcu != null) {
                rpcu.lostConnection(this, nspi, new Exception());
            }

            try {
                close(cnx, false);
            } catch (IOException ei) {
                throw new Error("close fails");
            }

            synchronized (connectionTable) {
                cnx.closeSeqno = 0;
                connectionTable.notifyAll();
            }
        }
            break;

        default:
            throw new Error("invalid event code");
        }
        log.out();
    }

    /* --- NetReceivePort part --- */

    /*
     * Constructor.
     *
     * @param type the {@linkplain ibis.impl.net.NetPortType port type}.
     * @param name the name of the port.
     * @param upcall the reception upcall callback.
     */
    public NetReceivePort(NetPortType type, String name, Upcall upcall,
            ReceivePortConnectUpcall rpcu, boolean connectionAdministration)
            throws IOException {
        this.type = type;
        this.name = name;
        this.upcall = upcall;
        this.rpcu = rpcu;
        this.ibis = type.getIbis();

        initDebugStreams();
        initPassiveObjects();
        initGlobalSettings(upcall != null);
        initMainInput();
        initServerSocket();
        initIdentifier();
        initActiveObjects();

        ibis.register(this);

        start();
    }

    private void initDebugStreams() {
        receivePortMessageId = receivePortCount++;
        receivePortMessageRank = ((NetIbis) type.getIbis()).closedPoolRank();
        receivePortTracePrefix = "_r" + receivePortMessageRank + "-"
                + receivePortMessageId + "_ ";

        String s = "//" + type.name() + " receivePort(" + name + ")/";

        boolean log = type.getBooleanStringProperty(null, "Log", false);
        boolean trace = type.getBooleanStringProperty(null, "Trace", false);
        boolean disp = type.getBooleanStringProperty(null, "Disp",
                TypedProperties.booleanProperty("net.disp"));

        this.log = new NetLog(log, s, "LOG");
        this.trace = new NetLog(trace, s, "TRACE");
        this.disp = new NetLog(disp, s, "DISP");

        this.trace.disp(receivePortTracePrefix, " receive port created");
    }

    private void initPassiveObjects() {
        log.in();
        connectionTable = new Hashtable();

        polledLock = new NetMutex(true);
        pollingLock = new NetMutex(false);
        connectionLock = new NetMutex(true);
        inputLock = new ibis.util.Monitor();
        finishMutex = new NetMutex(true);
        props = new NetDynamicProperties();
        log.out();
    }

    private void initMainInput() throws IOException {
        log.in();
        String mainDriverName = type.getStringProperty("/", "Driver");

        if (mainDriverName == null) {
            throw new IbisConfigurationException("root driver not specified");
        }

        driver = ibis.getDriver(mainDriverName);
        if (driver == null) {
            throw new IbisConfigurationException("driver not found");
        }

        input = driver.newInput(type, null, useUpcall ? this : null);
        log.out();
    }

    private void initGlobalSettings(boolean upcallSpecified) {
        log.in();
        useYield = type.getBooleanStringProperty(null, "UseYield",
                TypedProperties.booleanProperty(NetIbis.port_yield, useYield));
        useUpcall = type.getBooleanStringProperty(null, "UseUpcall", useUpcall);
        //                useBlockingPoll        = type.getBooleanStringProperty(null, "UseBlockingPoll",     useBlockingPoll    );

        if (!useUpcall && upcallSpecified) {
            useUpcall = true;
        }
        if (!useYield) {
            System.err.println("useYield " + useYield);
        }

        disp.disp("__ Configuration ____");
        disp.disp("Upcall engine........", __.state__(useUpcall));
        disp.disp("Yield................", __.state__(useYield));
        disp.disp("Blocking poll........", __.state__(useBlockingPoll));
        disp.disp("_____________________");
        log.out();
    }

    private void initServerSocket() throws IOException {
        log.in();
        serverSocket = NetIbis.socketFactory.createServerSocket(0, 0, IPUtils
                .getLocalHostAddress());
        log.out();
    }

    private void initIdentifier() {
        log.in();
        Hashtable info = new Hashtable();

        InetAddress addr = serverSocket.getInetAddress();
        int port = serverSocket.getLocalPort();

        info.put("accept_address", addr);
        info.put("accept_port", new Integer(port));
        NetIbisIdentifier ibisId = (NetIbisIdentifier) ibis.identifier();
        identifier = new NetReceivePortIdentifier(name, type.name(), ibisId,
                info);
        log.out();
    }

    private void initActiveObjects() {
        log.in();
        connectedPeers = new Vector();
        disconnectedPeers = new Vector();
        acceptThread = new AcceptThread(name);
        eventQueue = new NetEventQueue();
        eventQueueListener = new NetEventQueueListener(this, "ReceivePort: "
                + name, eventQueue);
        log.out();
    }

    private void start() {
        log.in();
        eventQueueListener.setDaemon(true);
        eventQueueListener.start();
        acceptThread.start();
        log.out();
    }

    private Thread pollerThread;

    /**
     * The internal synchronous polling function.
     *
     * The calling thread is <strong>uninterruptible</strong> during
     * the network input locking operation. The function may block
     * if the {@linkplain #inputLock network input lock} is not available.
     */
    private boolean _doPoll(boolean block) throws IOException {
        log.in();
        inputLock.lock();
        pollerThread = Thread.currentThread();
        try {
            activeSendPortNum = input.poll(block);
        } finally {
            pollerThread = null;
            inputLock.unlock();
        }

        if (activeSendPortNum == null) {
            log.out("activeSendPortNum = null");
            return false;
        }

        log.out("activeSendPortNum = ", activeSendPortNum);
        return true;
    }

    public long sequenceNumber() {
        return messageSeqno;
    }

    /**
     * Internally initializes a new reception.
     */
    private ReadMessage _receive() throws IOException {
        log.in();
        emptyMsg = true;
        if (type.numbered()) {
            emptyMsg = false;
            messageSeqno = input.readSeqno();
            // System.err.println(NetIbis.hostName() + " " + this + ": receive msg with seqno " + messageSeqno);
        }

        if (trace.on()) {
            final String messageId = readString();
            trace.disp(receivePortTracePrefix, "message receive --> ",
                    messageId);
        }

        log.out();
        return this;
    }

    /**
     * Blockingly attempts to receive a message.
     *
     * Note: if upcalls are currently enabled, this function is bypassed
     * by the upcall callback unless no callback has been specified.
     *
     * @return A {@link ReadMessage} instance.
     */
    public ReadMessage receive() throws IOException {
        log.in();
        if (useUpcall) {
            polledLock.lock();
        } else {
            if (useYield) {
                while (!_doPoll(useBlockingPoll)) {
                    NetIbis.yield();
                    // n_yield++;
                }
            } else {
                while (!_doPoll(useBlockingPoll)) { /* do noting */
                }
            }

        }

        log.out();
        return _receive();
    }

    public ReadMessage receive(long millis) throws IOException {
        if (millis == 0) {
            return receive();
        } else {
            long top = System.currentTimeMillis();
            ReadMessage rm = null;

            do {
                rm = poll();
            } while (rm == null && (System.currentTimeMillis() - top) < millis);

            if (rm == null) {
                throw new ReceiveTimedOutException("timeout expired in receive");
            }
            return rm;
        }
    }

    /**
     * Unblockingly attempts to receive a message.
     *
     * Note: if upcalls are currently enabled, this function is bypassed
     * by the upcall callback unless no callback has been specified.
     *
     * @return A {@link ReadMessage} instance or <code>null</code> if polling
     * was unsuccessful.
     */
    public ReadMessage poll() throws IOException {
        log.in();
        if (useUpcall) {
            if (!polledLock.trylock()) {
                log.out("poll failure 1");
                return null;
            }
        } else {
            if (!_doPoll(false)) {
                log.out("poll failure 2");
                return null;
            }
        }

        log.out("poll success");

        return _receive();
    }

    public ReceivePortIdentifier identifier() {
        log.in();
        log.out();
        return identifier;
    }

    /**
     * Returns the identifier of the current active port peer or <code>null</code>
     * if no peer port is active.
     *
     * @return The identifier of the port.
     */
    protected NetSendPortIdentifier getActiveSendPortIdentifier() {
        log.in();

        NetConnection cnx = getActiveConnection();
        if (cnx == null) {
            throw new Error("no active sendPort");
        }
        if (cnx.getSendId() == null) {
            throw new Error("invalid state: cnx.getSendId");
        }
        NetSendPortIdentifier id = cnx.getSendId();

        log.out();
        return id;
    }

    public SendPortIdentifier[] connectedTo() {
        synchronized (connectionTable) {
            SendPortIdentifier t[] = new SendPortIdentifier[connectionTable
                    .size()];

            Iterator it = connectionTable.values().iterator();
            int i = 0;

            while (it.hasNext()) {
                NetConnection cnx = (NetConnection) it.next();
                t[i++] = cnx.getSendId();
            }

            return t;
        }
    }

    public SendPortIdentifier[] lostConnections() {
        synchronized (connectionTable) {
            SendPortIdentifier t[] = new SendPortIdentifier[disconnectedPeers
                    .size()];
            disconnectedPeers.copyInto(t);
            disconnectedPeers.clear();

            return t;
        }
    }

    public SendPortIdentifier[] newConnections() {
        synchronized (connectionTable) {
            SendPortIdentifier t[] = new SendPortIdentifier[connectedPeers
                    .size()];
            connectedPeers.copyInto(t);
            connectedPeers.clear();

            return t;
        }
    }

    public ReceivePort localPort() {
        return this;
    }

    public synchronized void enableConnections() {
        log.in();
        if (!connectionEnabled) {
            connectionEnabled = true;
            connectionLock.unlock();
        }
        log.out();
    }

    public synchronized void disableConnections() {
        log.in();
        if (connectionEnabled) {
            connectionEnabled = false;
            while (true) {
                try {
                    connectionLock.lock();
                    break;
                } catch (InterruptedIOException e) {
                    System.err
                            .println("InterruptedIOException ignored in NetReceivePort.disableConnections");
                }
            }
        }
        log.out();
    }

    public synchronized void enableUpcalls() {
        log.in();
        synchronized (dummyUpcallSync) {
            upcallsEnabled = true;
            if (upcallsPending > 0) {
                dummyUpcallSync.notify();
            }
        }
        log.out();
    }

    public synchronized void disableUpcalls() {
        log.in();
        upcallsEnabled = false;
        log.out();
    }

    private void close(NetConnection cnx, boolean forced) throws IOException {
        log.in();
        if (cnx == null) {
            log.out("cnx = null");
            return;
        }
        synchronized (connectionTable) {
            while (!forced && cnx.msgSeqno < cnx.closeSeqno) {
                try {
                    cnx.regularClosers++;
                    connectionTable.wait();
                    cnx.regularClosers--;
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        trace.disp(receivePortTracePrefix, "network connection shutdown-->");
        input.close(cnx.getNum());
        trace.disp(receivePortTracePrefix, "network connection shutdown<--");

        try {
            cnx.close();
        } catch (Exception e) {
            throw new Error(e.getMessage());
        }
        log.out();
    }

    /**
     * Closes the port.
     */
    public void close() throws IOException {
        close(false, 0);
    }

    /**
     * Closes the port.
     */
    public void close(long timeout) throws IOException {
        if (timeout == 0) {
            close(false, 0);
        } else if (timeout < 0) {
            close(true, 0L);
        } else {
            close(false, timeout);
        }
    }

    private void close(boolean force, long timeout) throws IOException {
        log.in();
        trace.disp(receivePortTracePrefix, "receive port shutdown-->");
        if (timeout != 0) {
            __.unimplemented__("void close(long timeout)");
        }

        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }

        if (!force && connectionTable != null) {
            synchronized (connectionTable) {
                boolean closing;
                // Complicated looping construct. In the wait, we
                // release the lock on connectionTable, so some other
                // thread may/will modify the table. Then the Iterator
                // is no longer valid, and we must start the whole
                // procedure again.
                outer: do {
                    closing = false;
                    Iterator i = connectionTable.values().iterator();
                    middle: while (i.hasNext()) {
                        NetConnection cnx = (NetConnection) i.next();
                        while (cnx.msgSeqno < cnx.closeSeqno) {
                            try {
                                cnx.regularClosers++;
                                connectionTable.wait();
                                cnx.regularClosers--;
                                closing = true;
                                // System.err.println("Do the cycle again, NetConnection iterator broken");
                                break middle;
                            } catch (InterruptedException e) {
                                break outer;
                            }
                        }
                    }
                } while (closing);
            }
        }

        synchronized (this) {

            trace.disp(receivePortTracePrefix,
                    "receive port shutdown: input locked");

            if (acceptThread != null) {
                acceptThread.end();

                while (true) {
                    try {
                        acceptThread.join();
                        break;
                    } catch (InterruptedException e) {
                        //
                    }
                }
            }

            trace.disp(receivePortTracePrefix,
                    "receive port shutdown: accept thread terminated");

            if (connectionTable != null) {
                while (true) {
                    NetConnection cnx = null;

                    synchronized (connectionTable) {
                        Iterator i = connectionTable.values().iterator();
                        if (!i.hasNext())
                            break;

                        cnx = (NetConnection) i.next();
                        if (rpcu != null) {
                            rpcu.lostConnection(this, cnx.getSendId(),
                                    new Exception());
                        }
                        i.remove();
                    }

                    if (cnx != null) {
                        close(cnx, force);
                    }
                }
            }

            trace.disp(receivePortTracePrefix,
                    "receive port shutdown: all connections closed");

            if (input != null) {
                inputLock.lock();
                try {
                    input.free();
                    input = null;
                } finally {
                    inputLock.unlock();
                }
            }
            trace.disp(receivePortTracePrefix,
                    "receive port shutdown: all inputs freed");

            trace.disp(receivePortTracePrefix,
                    "receive port shutdown: input lock released");
        }

        if (type.manyToOne() && maxLiveConnections == 1) {
            System.err.println(this
                    + ": ManyToOne portType but only one connection");
        }

        ibis.unregister(this);

        trace.disp(receivePortTracePrefix, "receive port shutdown<--");
        log.out();
    }

    protected void finalize() throws Throwable {
        log.in();
        close();

        if (eventQueueListener != null) {
            eventQueueListener.end();

            while (true) {
                try {
                    eventQueueListener.join();
                    break;
                } catch (InterruptedException e) {
                    //
                }
            }

            eventQueueListener = null;
        }

        super.finalize();
        log.out();
    }

    public long getCount() {
        // TODO
        return 0;
    }

    public void resetCount() {
        // TODO
    }

    /* --- ReadMessage Part --- */
    public long finish() throws IOException {
        log.in();
        if (emptyMsg) {
            input.handleEmptyMsg();
            emptyMsg = false;
        }
        trace.disp(receivePortTracePrefix, "message receive <--");

        NetConnection cnx = checkClose();

        NetSendPortIdentifier id = cnx.getSendId();
        activeSendPortNum = null;
        currentThread = null;
        input.finish();

        if (finishNotify) {
            finishNotify = false;
            finishMutex.unlock();
        }

        if (pollingNotify) {
            pollingNotify = false;
            pollingLock.unlock();
        }
        log.out();
        // TODO: return byte count of message
        return 0;
    }

    public void finish(IOException e) {
        // What to do here? Rutger?
        try {
            finish();
        } catch (IOException e2) {
            // Give up
        }
    }

    public SendPortIdentifier origin() {
        log.in();
        SendPortIdentifier spi = getActiveSendPortIdentifier();
        log.out();
        return spi;
    }

    public boolean readBoolean() throws IOException {
        log.in();
        emptyMsg = false;
        boolean v = input.readBoolean();
        log.out();
        return v;
    }

    public byte readByte() throws IOException {
        log.in();
        emptyMsg = false;
        byte v = input.readByte();
        log.out();
        return v;
    }

    public char readChar() throws IOException {
        log.in();
        emptyMsg = false;
        char v = input.readChar();
        log.out();
        return v;
    }

    public short readShort() throws IOException {
        log.in();
        emptyMsg = false;
        short v = input.readShort();
        log.out();
        return v;
    }

    public int readInt() throws IOException {
        log.in();
        emptyMsg = false;
        int v = input.readInt();
        log.out();
        return v;
    }

    public long readLong() throws IOException {
        log.in();
        emptyMsg = false;
        long v = input.readLong();
        log.out();
        return v;
    }

    public float readFloat() throws IOException {
        log.in();
        emptyMsg = false;
        float v = input.readFloat();
        log.out();
        return v;
    }

    public double readDouble() throws IOException {
        log.in();
        emptyMsg = false;
        double v = input.readDouble();
        log.out();
        return v;
    }

    public String readString() throws IOException {
        log.in();
        emptyMsg = false;
        String v = input.readString();
        log.out();
        return v;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
        log.in();
        emptyMsg = false;
        Object v = input.readObject();
        log.out();
        return v;
    }

    public void readArray(boolean[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(byte[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(char[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(short[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(int[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(long[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(float[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(double[] b) throws IOException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(Object[] b) throws IOException,
            ClassNotFoundException {
        log.in();
        readArray(b, 0, b.length);
        log.out();
    }

    public void readArray(boolean[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(byte[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(char[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(short[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(int[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(long[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(float[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(double[] b, int o, int l) throws IOException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
    }

    public void readArray(Object[] b, int o, int l) throws IOException,
            ClassNotFoundException {
        log.in();
        if (l == 0) {
            log.out("l = 0");
            return;
        }

        emptyMsg = false;
        input.readArray(b, o, l);
        log.out();
    }

}