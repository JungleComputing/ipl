/* $Id$ */

package ibis.impl.messagePassing;

import ibis.ipl.ReceivePortConnectUpcall;
import ibis.util.ConditionVariable;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Vector;

/**
 * messagePassing ReceivePort
 */
class ReceivePort implements ibis.ipl.ReceivePort, Runnable {

    /**
     * A connection between a send port and a receive port within the
     * same Ibis should not lead to polling for the reply, but to quick
     * reversion to some other thread */
    private static final boolean HOME_CONNECTION_PREEMPTS = false;

    /**
     * After serving a message, the receive thread may optimistically
     * poll for a while. A new request might arrive in a short while,
     * and that saves an interrupt. Set this to 0 if you don't want
     * optimistic polling. */
    private static final int DEFAULT_OPTIMISTIC_POLLS = 500;

    private static final int polls_before_yield = TypedProperties.intProperty(
            MPProps.s_polls_optim, DEFAULT_OPTIMISTIC_POLLS);
    // private static final int DEFAULT_OPTIMISTIC_POLLS = Poll.DEFAULT_YIELD_POLLS / 2;

    static {
        if (Ibis.myIbis.myCpu == 0) {
            System.err.println("ReceivePort: Do " + polls_before_yield
                    + " optimistic polls after serving an asynchronous upcall");
        }
    }

    private static final boolean DEBUG = Ibis.DEBUG;

    private static final boolean STATISTICS = true;

    private static int threadsCreated;

    private static int threadsCached;

    private static long explicitFinish;

    private static long implicitFinish;

    static int livingPorts = 0;

    private static PortCounter portCounter = new PortCounter();

    private PortType type;

    private ReceivePortIdentifier ident;

    private String name; // needed to unbind

    ReadMessage queueFront;

    private ReadMessage queueTail;

    private MessageArrived messageArrived = new MessageArrived();

    private int arrivedWaiters = 0;

    private boolean aMessageIsAlive = false;

    private boolean mePolling = false;

    private ConditionVariable messageHandled = Ibis.myIbis.createCV();

    private int liveWaiters = 0;

    private ReadMessage currentMessage = null;

    private Thread thread;

    private ibis.ipl.Upcall upcall;

    private int upcallThreads;

    volatile boolean stop = false;

    private ReceivePortConnectUpcall connectUpcall;

    private boolean allowConnections = false;

    private AcceptThread acceptThread;

    private boolean allowUpcalls = false;

    private ConditionVariable enable = Ibis.myIbis.createCV();

    private long seqno = ibis.ipl.ReadMessage.INITIAL_SEQNO;

    private int availableUpcallThread = 0;

    /*
     * If the receive port is connected only to a LOCAL send port,
     * homeConnection is true. In that case, don't poll optimistically
     * but immediately yield.
     */
    private boolean homeConnection = true;

    Vector connections = new Vector();

    private Shutdown shutdown = new Shutdown();

    private static final boolean DISABLE_INTR_MULTIFRAGMENT
        = TypedProperties.booleanProperty(MPProps.s_intr_dis_multi, false);

    private boolean interruptsDisabled;

    // DEBUG
    private long upcall_poll;

    // STATISTICS
    private int enqueued_msgs;

    private int upcall_msgs;

    private int dequeued_msgs;

    long count;

    ReceivePort next;

    static {
        if (DEBUG) {
            if (Ibis.myIbis.myCpu == 0) {
                System.err.println(Thread.currentThread()
                        + "Turn on ReceivePort.DEBUG");
            }
        }
    }

    ReceivePort(PortType type, String name) throws IOException {
        this(type, name, null, null, false);
    }

    ReceivePort(PortType type, String name, ibis.ipl.Upcall upcall,
            ReceivePortConnectUpcall connectUpcall,
            boolean connectionAdministration) throws IOException {
        this.type = type;
        this.name = name;
        this.upcall = upcall;
        this.connectUpcall = connectUpcall;

        /// @@@ implement connectionAdministration --Rob

        ident = new ReceivePortIdentifier(name, type.name());

        Ibis.myIbis.lock();
        try {
            Ibis.myIbis.registerReceivePort(this);
            livingPorts++;
        } finally {
            Ibis.myIbis.unlock();
        }
    }

    /**
     * A thread to process accepts
     */
    private static class AcceptThread extends Thread {

        private ReceivePort port;

        private ibis.ipl.ReceivePortConnectUpcall upcall;

        private ConditionVariable there_is_work = Ibis.myIbis.createCV();

        private boolean stopped;

        private static class AcceptQ {
            AcceptQ next;

            boolean finished;

            boolean accept;

            ibis.ipl.SendPortIdentifier port;

            ConditionVariable decided = Ibis.myIbis.createCV();
        }

        AcceptQ acceptQ_front;

        AcceptQ acceptQ_tail;

        AcceptQ acceptQ_freelist;

        AcceptThread(ReceivePort port,
                ibis.ipl.ReceivePortConnectUpcall upcall) {
            this.port = port;
            this.upcall = upcall;
        }

        private void enqueue(AcceptQ q) {
            q.next = null;
            if (acceptQ_front == null) {
                acceptQ_front = q;
            } else {
                acceptQ_tail.next = q;
            }
            there_is_work.cv_signal();
            acceptQ_tail = q;
        }

        private AcceptQ dequeue() {
            if (acceptQ_front == null) {
                return null;
            }

            AcceptQ q = acceptQ_front;
            acceptQ_front = q.next;

            return q;
        }

        private AcceptQ get() {
            if (acceptQ_freelist == null) {
                return new AcceptQ();
            }

            AcceptQ q = acceptQ_freelist;
            acceptQ_freelist = q.next;

            return q;
        }

        private void release(AcceptQ q) {
            q.next = acceptQ_freelist;
            acceptQ_freelist = q;
        }

        boolean checkAccept(ibis.ipl.SendPortIdentifier p) {
            boolean accept;

            Ibis.myIbis.checkLockOwned();

            AcceptQ q = get();

            q.port = p;
            enqueue(q);

            while (!q.finished) {
                try {
                    q.decided.cv_wait();
                } catch (InterruptedException e) {
                    // ignore
                }
            }

            accept = q.accept;

            release(q);

            return accept;
        }

        public void run() {

            Ibis.myIbis.lock();

            AcceptQ q;

            while (true) {
                while ((q = dequeue()) == null && !stopped) {
                    try {
                        there_is_work.cv_wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

                if (q == null) {
                    break;
                }

                q.accept = upcall.gotConnection(port, q.port);
                q.finished = true;
                q.decided.cv_signal();
            }

            Ibis.myIbis.unlock();
        }

        void free() {
            stopped = true;
            there_is_work.cv_signal();
        }

    }

    public long getCount() {
        return count;
    }

    public void resetCount() {
        count = 0;
    }

    private boolean firstCall = true;

    public synchronized void enableConnections() {
        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "Enable connections on " + this + " firstCall="
                    + firstCall);
        }
        if (firstCall) {
            firstCall = false;
            if (upcall != null) {
                thread = new Thread(this, "ReceivePort " + this
                        + " upcall thread " + upcallThreads);
                upcallThreads++;
                availableUpcallThread++;
                thread.setDaemon(true);
                thread.start();
            }
            if (connectUpcall != null) {
                acceptThread = new AcceptThread(this, connectUpcall);
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("And start another AcceptThread(this="
                            + this + ")");
                }
                acceptThread.setName("ReceivePort accept thread");
                acceptThread.start();
            }
            if (Ibis.DEBUG_RUTGER) {
                System.err.println("In enableConnections: "
                        + " want to bind locally RPort " + this);
            }
            Ibis.myIbis.lock();
            Ibis.myIbis.bindReceivePort(this, ident.port);
            Ibis.myIbis.unlock();
            try {
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("In enableConnections: "
                            + " want to bind RPort " + this);
                }
                ((Registry) Ibis.myIbis.registry()).bind(name, ident);
            } catch (IOException e) {
                System.err.println("registry bind of ReceivePortName fails: "
                        + e);
                System.exit(4);
            }
        }
        allowConnections = true;
    }

    public synchronized void disableConnections() {
        allowConnections = false;
    }

    public synchronized void enableUpcalls() {
        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "*********** Enable upcalls");
        }
        Ibis.myIbis.lock();
        allowUpcalls = true;
        enable.cv_signal();
        Ibis.myIbis.unlock();
    }

    public synchronized void disableUpcalls() {
        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "*********** Disable upcalls");
        }
        allowUpcalls = false;
    }

    boolean connect(ShadowSendPort sp) {
        Ibis.myIbis.checkLockOwned();
        ibis.ipl.SendPortIdentifier id = sp.identifier();
        if (!id.ibis().equals(ident.ibis())) {
            homeConnection = false;
            if (DEBUG) {
                System.err.println("This is NOT a home-only connection");
            }
        } else {
            if (DEBUG) {
                System.err.println("This IS a home-only connection");
                // System.err.println("My ibis is " + ident.ibis());
                // System.err.println("Remote ibis is " + id.ibis());
            }
        }

        if (connectUpcall == null || acceptThread.checkAccept(id)) {
            connections.add(sp);
            return true;
        }
        return false;
    }

    void disconnect(ShadowSendPort sp) {
        Ibis.myIbis.checkLockOwned();
        connections.remove(sp);
        if (connections.size() == 0) {
            shutdown.signal();
        }
        if (connectUpcall != null) {
            Ibis.myIbis.unlock();
            connectUpcall.lostConnection(this, sp.identifier(), null);
            Ibis.myIbis.lock();
        }
        /* TODO:
         * maybe reset homeConnection
         */
    }

    private void createNewUpcallThread() {
        if (availableUpcallThread != 0) {
            if (STATISTICS) {
                threadsCreated++;
                threadsCached++;
            }
        } else {
            if (Ibis.DEBUG_RUTGER) {
                System.err.println(Ibis.myIbis.myCpu
                        + ": Create another UpcallThread "
                        + " because the previous one didn't terminate");
            }
            Thread upcallthread = new Thread(this, "ReceivePort upcall thread "
                    + upcallThreads);
            upcallThreads++;
            availableUpcallThread++;
            upcallthread.setDaemon(true);
            upcallthread.start();
            if (STATISTICS) {
                threadsCreated++;
            }
        }
    }

    private ReadMessage locate(ShadowSendPort ssp, int msgSeqno) {
        if (ssp.msgSeqno > msgSeqno && ssp.msgSeqno != -1) {
            if (DEBUG) {
                System.err.println(Thread.currentThread()
                        + "This is a SERIOUS BUG: "
                        + " the msgSeqno goes BACK!!!!!!!!!");
            }
            ssp.msgSeqno = msgSeqno;
            return null;
        }

        if (currentMessage != null && currentMessage.shadowSendPort == ssp
                && currentMessage.msgSeqno == msgSeqno) {
            return currentMessage;
        }

        ReadMessage scan;
        for (scan = queueFront;
                scan != null
                    && (scan.shadowSendPort != ssp
                        || scan.msgSeqno != msgSeqno);
                scan = scan.next) {
            // try the next list element
        }

        return scan;
    }

    void enqueue(ReadMessage msg) {
        Ibis.myIbis.checkLockOwned();
        if (type.numbered) {
            msg.setSequenceNumber(seqno++);
        }
        if (DEBUG) {
            System.err.println(Thread.currentThread() + "Enqueue message "
                    + msg + " in port " + this + " msgHandle "
                    + Integer.toHexString(msg.fragmentFront.msgHandle)
                    + " current queueFront " + queueFront);
        }

        if (queueFront == null) {
            queueFront = msg;
        } else {
            queueTail.next = msg;
        }
        queueTail = msg;
        msg.next = null;

        if (Ibis.STATISTICS) {
            enqueued_msgs++;
        }

        if (arrivedWaiters > 0 && !mePolling) {
            messageArrived.signal();
        }

        if (upcall != null) {
            if (Ibis.STATISTICS) {
                upcall_msgs++;
            }
            if (availableUpcallThread == 0 && !aMessageIsAlive) {
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("enqueue: "
                            + " Create another UpcallThread "
                            + " because the previous one didn't terminate");
                }
                createNewUpcallThread();
            }
        }
    }

    private ReadMessage dequeue() {
        Ibis.myIbis.checkLockOwned();
        ReadMessage msg = queueFront;

        if (msg != null) {
            if (DEBUG) {
                System.err.println(Thread.currentThread() + "Now dequeue msg "
                        + dequeued_msgs + " 0x"
                        + Integer.toHexString(msg.fragmentFront.msgHandle)
                        + " " + msg);
            }
            queueFront = msg.next;
            if (Ibis.STATISTICS) {
                dequeued_msgs++;
            }
            msg.before = msg.in.getCount();
        }

        return msg;
    }

    void finishMessage() throws IOException {

        Ibis.myIbis.checkLockOwned();

        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "******* Now finish this ReceivePort message: "
                    + currentMessage);
            // Thread.dumpStack();
        }
        if (STATISTICS) {
            explicitFinish++;
        }

        ShadowSendPort ssp = currentMessage.shadowSendPort;
        if (ssp.cachedMessage == null) {
            ssp.cachedMessage = currentMessage;
        }
        currentMessage.finished = true;

        if (DISABLE_INTR_MULTIFRAGMENT && interruptsDisabled) {
            interruptsDisabled = false;
            currentMessage.enableInterrupts();
        }

        currentMessage = null;
        aMessageIsAlive = false;
        if (liveWaiters > 0) {
            messageHandled.cv_signal();
        }

        if (queueFront != null) {
            if (arrivedWaiters > 0) {
                messageArrived.signal();
            } else if (upcall != null && availableUpcallThread == 0) {
                if (Ibis.DEBUG_RUTGER) {
                    System.err.println("finishMessage: "
                            + " Create another UpcallThread "
                            + " because the previous one didn't terminate");
                }
                createNewUpcallThread();
            }
        }

        ssp.tickReceive();
    }

    private class MessageArrived extends Syncer {

        public boolean satisfied() {
            return queueFront != null || stop;
        }

    }

    void receiveFragment(ShadowSendPort origin, int msgHandle, int msgSize,
            int msgSeqno) throws IOException {
        Ibis.myIbis.checkLockOwned();

        if (DEBUG) {
            System.err.println(Thread.currentThread() + " Port " + this
                    + " receive a fragment seqno " + msgSeqno
                    + " size " + msgSize + " that belongs to msg "
                    + locate(origin,
                        msgSeqno & ~ByteOutputStream.SEQNO_FRAG_BITS)
                    + "; currentMessage = " + currentMessage
                    + (currentMessage == null ? ""
                            : (" .seqno " + currentMessage.msgSeqno)));
            System.err.println(Thread.currentThread() + "Enqueue message "
                    + (msgSeqno & ~ByteOutputStream.SEQNO_FRAG_BITS) + " SSP "
                    + origin + " in port " + this + " id " + identifier()
                    + " msgHandle " + Integer.toHexString(msgHandle)
                    + " current queueFront " + queueFront);
        }

        boolean lastFrag = (msgSeqno & ByteOutputStream.LAST_FRAG_BIT) != 0;
        boolean firstFrag = (msgSeqno & ByteOutputStream.FIRST_FRAG_BIT) != 0;
        msgSeqno &= ~ByteOutputStream.SEQNO_FRAG_BITS;

        /* Let's see whether our ShadowSendPort has a fragment cached */
        ReadFragment f = origin.getFragment();

        ReadMessage msg;
        if (firstFrag) {
            /* This must be the first fragment of a new message.
             * Let our ShadowSendPort create an envelope, i.e. a ReadMessage
             * for it. */
            msg = origin.getMessage(msgSeqno);
            msg.multiFragment = !lastFrag;
        } else {
            msg = locate(origin, msgSeqno);
        }

        f.msg = msg;
        f.msgHandle = msgHandle;
        f.msgSize = msgSize;

        /* Hook up the fragment in the message envelope */
        msg.enqueue(f);
        if (DEBUG) {
            System.err.println(Thread.currentThread() + " Port " + this
                    + " rcve frag; firstFrag=" + firstFrag);
        }

        /* Must set in.msgHandle and in.msgSize from here: cannot wait
         * until we do a read:
         *  - a message may be empty and still must be able to clear it
         *  - a Serialized stream starts reading in the constructor */
        if (firstFrag && origin.checkStarted(msg)) {
            enqueue(msg);
        } else if (DEBUG) {
            System.err.println(Thread.currentThread() + " Port " + this
                    + " rcve frag; NOT checkStarted!!!");
        }
    }

    private ReadMessage doReceive(boolean block) throws IOException {
        Ibis.myIbis.checkLockOwned();

        if (DEBUG) {
            System.err.println(Thread.currentThread()
                    + "******** enter ReceivePort.receive()" + ident);
        }
        while (aMessageIsAlive && !stop) {
            if (! block) {
                return null;
            }
            liveWaiters++;
            if (DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Hit wait in ReceivePort.receive()" + ident
                        + " aMessageIsAlive is true");
            }
            try {
                messageHandled.cv_wait();
            } catch (InterruptedException e) {
                // ignore
            }
            if (DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Past wait in ReceivePort.receive()" + ident
                        + " aMessageIsAlive is true");
            }
            liveWaiters--;
        }

        if (upcall != null && queueFront == null) {
            return null;
        }

        aMessageIsAlive = true;

        // long t = Ibis.currentTime();

        while (queueFront == null) {
            if (!block) {
                aMessageIsAlive = false;
                return null;
            }

            if (DEBUG) {
                System.err.println(Thread.currentThread() + ", port " + this
                        + ". Hit wait in ReceivePort.receive()" + ident
                        + " queue " + queueFront + " " + messageArrived);
            }
            arrivedWaiters++;
            Ibis.myIbis.waitPolling(messageArrived, 0,
                    (HOME_CONNECTION_PREEMPTS || !homeConnection)
                        ? Poll.PREEMPTIVE : Poll.NON_POLLING);
            arrivedWaiters--;

            if (DEBUG) {
                System.err.println(Thread.currentThread()
                        + "Past wait in ReceivePort.receive()" + ident);
            }
        }

        currentMessage = dequeue();
        if (currentMessage == null) {
            return null;
        }
        currentMessage.setMsgHandle();

        if (DISABLE_INTR_MULTIFRAGMENT && currentMessage.multiFragment) {
            currentMessage.disableInterrupts();
            interruptsDisabled = true;
        }

        // Ibis.myIbis.tReceive += Ibis.currentTime() - t;

        return currentMessage;
    }

    private ibis.ipl.ReadMessage receive(boolean block) throws IOException {
        Ibis.myIbis.lock();
        try {
            return doReceive(block);
        } finally {
            Ibis.myIbis.unlock();
        }
    }

    public ibis.ipl.ReadMessage receive(long timeout) throws IOException {
        return receive();
    }

    public ibis.ipl.ReadMessage receive() throws IOException {
        return receive(true);
    }

    public ibis.ipl.ReadMessage poll() throws IOException {
        if (! type.properties().isProp("communication", "Poll")) {
            throw new IOException("ReceivePort not configured for poll");
        }
        return receive(false);
    }

    public ibis.ipl.DynamicProperties properties() {
        return ibis.ipl.DynamicProperties.NoDynamicProperties;
    }

    public String name() {
        return name;
    }

    PortType type() {
        return type;
    }

    public ibis.ipl.ReceivePortIdentifier identifier() {
        return ident;
    }

    public ibis.ipl.SendPortIdentifier[] connectedTo() {

        ibis.ipl.SendPortIdentifier[] s = null;

        Ibis.myIbis.lock();
        try {
            int n = connections.size();
            s = new ibis.ipl.SendPortIdentifier[n];
            for (int i = 0; i < n; i++) {
                s[i] = ((SendPort) connections.elementAt(i)).identifier();
            }
        } finally {
            Ibis.myIbis.unlock();
        }
        return s;
    }

    public ibis.ipl.SendPortIdentifier[] lostConnections() {
        return null;
    }

    public ibis.ipl.SendPortIdentifier[] newConnections() {
        System.err.println("Do not know how to implement this.");
        return null;
    }

    private class Shutdown extends Syncer {

        public boolean satisfied() {
            return connections.size() == 0;
        }

    }

    private static class PortCounter extends Syncer {

        public boolean satisfied() {
            return livingPorts == 0;
        }

        public void signal() {
            livingPorts--;
            if (livingPorts == 0) {
                wakeup();
            }
        }

    }

    void closeLocked(boolean forced) throws IOException {

        if (DEBUG) {
            System.out.println(Thread.currentThread() + name
                    + ": got Ibis lock");
        }

        stop = true;

        messageHandled.cv_bcast();
        messageArrived.wakeupAll();

        if (DEBUG) {
            System.out.println(Thread.currentThread() + name
                    + ": Enter shutdown.waitPolling; connections = "
                    + connectionToString());
        }
        if (!forced) {
            try {
                shutdown.waitPolling();
            } catch (IOException e) {
                /* well, if it throws an exception, let's quit.. */
            }
        }
        if (DEBUG) {
            System.out.println(Thread.currentThread() + name
                    + ": Past shutdown.waitPolling");
        }

        if (connectUpcall != null) {
            acceptThread.free();
        }

        Ibis.myIbis.unregisterReceivePort(this);

        Ibis.myIbis.unlock();

        /* unregister with name server */
        try {
            if (DEBUG) {
                System.out.println(Thread.currentThread() + name
                        + ": unregister with name server");
            }
            type.freeReceivePort(name);
        } catch (Exception e) {
            // Ignore.
        }

        if (DEBUG) {
            System.out.println(Thread.currentThread() + name
                    + ":done receiveport.free");
        }

        Ibis.myIbis.lock();
        portCounter.signal();
    }

    public void close() throws IOException {

        if (DEBUG) {
            System.out.println(Thread.currentThread() + name
                    + ":Starting receiveport.close upcall = " + upcall);
        }

        Ibis.myIbis.lock();
        try {
            closeLocked(false);
        } finally {
            Ibis.myIbis.unlock();
        }

    }

    public void close(long timeout) throws IOException {
        Ibis.myIbis.lock();
        try {
            // TODO: implement close with timeout > 0
            closeLocked(timeout != 0L);
        } finally {
            Ibis.myIbis.unlock();
        }
    }

    private String connectionToString() {
        String t = "Connections =";
        for (int i = 0; i < connections.size(); i++) {
            t = t + " " + connections.elementAt(i);
        }
        return t;
    }

    public void run() {

        if (upcall == null) {
            System.err.println(Thread.currentThread() + "ReceivePort " + name
                    + ", daemon = " + this + " runs but upcall == null");
        }
        if (DEBUG) {
            System.err.println(Thread.currentThread() + " ReceivePort " + name
                    + ", daemon = " + this + " runs");
        }

        Ibis.myIbis.lock();

        try {
            Thread me = Thread.currentThread();

            while (true) {
                ReadMessage msg = null;

                if (stop) {
                    if (DEBUG) {
                        System.err.println(Thread.currentThread()
                                + "Receive port daemon " + this
                                + " upcall thread polls " + upcall_poll);
                    }
                    upcallThreads--;
                    break;
                }

                /* Nowadays, use NON_POLLING for the preempt flag. */
                if (DEBUG) {
                    System.err.println(Thread.currentThread()
                            + "*********** This ReceivePort daemon hits wait, "
                            + " daemon " + this
                            + " queueFront = " + queueFront);
                }

                mePolling = true;
                for (int i = 0;
                        queueFront == null && i < polls_before_yield;
                        i++) {
                    if (Ibis.myIbis.pollLocked()) {
                        // break;
                    }
                }
                mePolling = false;

                while (queueFront == null && !stop) {
                    // // // Ibis.myIbis.waitPolling(this, 0, Poll.NON_PREEMPTIVE);
                    // Ibis.myIbis.waitPolling(this, 0, (HOME_CONNECTION_PREEMPTS || ! homeConnection) ? Poll.NON_PREEMPTIVE : Poll.NON_POLLING);
                    arrivedWaiters++;
                    Ibis.myIbis.waitPolling(messageArrived, 0,
                            Poll.NON_POLLING);
                    arrivedWaiters--;
                }
                if (DEBUG) {
                    upcall_poll++;
                    System.err.println(Thread.currentThread()
                            + "*********** This ReceivePort daemon past wait, "
                            + " daemon " + this
                            + " queueFront = " + queueFront);
                }

                while (!allowUpcalls) {
                    try {
                        enable.cv_wait();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

                msg = doReceive(true /* block */); // May throw an IOException

                if (msg != null) {
                    if (Ibis.DEBUG_RUTGER) {
                        System.err.println(Thread.currentThread()
                                + ": perform upcall for msg " + msg);
                    }
                    availableUpcallThread--;
                    msg.creator = me;
                    msg.finished = false;

                    Ibis.myIbis.unlock();

                    try {
                        upcall.upcall(msg);
                    } finally {
                        Ibis.myIbis.lock();
                    }

                    /* Be sure to signal the presence of an upcall thread
                     * before calling finish(). Otherwise, finish() would
                     * spawn an extra popup thread. */
                    availableUpcallThread++;
                    if (!msg.finished && msg.creator == me) {
                        msg.finishLocked();
                        if (STATISTICS) {
                            implicitFinish++;
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();

        } finally {
            Ibis.myIbis.unlock();
        }

        if (DEBUG) {
            System.err.println(Thread.currentThread() + "Receive port " + name
                    + " upcall thread polls " + upcall_poll);
        }
        if (Ibis.DEBUG_RUTGER) {
            System.err.println("ReceivePort " + this + " upcallThread "
                    + Thread.currentThread().getName() + " snuffs it");
        }
    }

    static void report(java.io.PrintStream out) {
        if (STATISTICS) {
            out.println(Ibis.myIbis.myCpu + ": ReceivePort threads created "
                    + threadsCreated + " (cached " + threadsCached
                    + "); finish Xpl " + (explicitFinish - implicitFinish)
                    + " Mpl " + implicitFinish);
        }
    }

    static void end() {
        // assert(Ibis.myIbis.locked();
        while (livingPorts > 0) {
            try {
                portCounter.waitPolling();
            } catch (IOException e) {
                break;
            }
        }
    }

}
