package ibis.impl.messagePassing;

import ibis.util.ConditionVariable;

import java.io.IOException;
import java.util.Vector;

class ReceivePort
    implements ibis.ipl.ReceivePort, Runnable, PollClient {

    /** A connection between a send port and a receive port within the
     * same Ibis should not lead to polling for the reply, but to quick
     * reversion to some other thread */
    private static final boolean HOME_CONNECTION_PREEMPTS = false;

    /** After serving a message, the receive thread may optimistically
     * poll for a while. A new request might arrive in a short while,
     * and that saves an interrupt. Set this to 0 if you don't want
     * optimistic polling. */
    // private static final int polls_before_yield = Poll.polls_before_yield / 2;
    private static final int polls_before_yield = 500;

    static {
	if (Ibis.myIbis.myCpu == 0) {
	    System.err.println("ReceivePort: Do " + polls_before_yield + " optimistic polls after serving an asynchronous upcall");
	}
    }

    private static final boolean DEBUG = false;

    private static final boolean STATISTICS = true;
    private static int threadsCreated;
    private static int threadsCached;
    private static long explicitFinish;
    private static long implicitFinish;

    private static int livingPorts = 0;
    private static Syncer portCounter = new Syncer();

    private PortType type;
    private ReceivePortIdentifier ident;
    private int connectCount = 0;
    private String name;	// needed to unbind

    private ReadMessage queueFront;
    private ReadMessage queueTail;
    private ConditionVariable messageArrived = Ibis.myIbis.createCV();
    private int arrivedWaiters = 0;

    private boolean aMessageIsAlive = false;
    private boolean mePolling = false;
    private ConditionVariable messageHandled = Ibis.myIbis.createCV();
    private int liveWaiters = 0;
    private ReadMessage currentMessage = null;

    private Thread thread;
    private ibis.ipl.Upcall upcall;
    private int upcallThreads;

    private volatile boolean stop = false;

    private ibis.ipl.ReceivePortConnectUpcall connectUpcall;
    private boolean allowConnections = false;
    private AcceptThread acceptThread;
    private boolean allowUpcalls = false;
    private ConditionVariable enable = Ibis.myIbis.createCV();

    private int availableUpcallThread = 0;
    /*
     * If the receive port is connected only to a LOCAL send port,
     * homeConnection is true. In that case, don't poll optimistically
     * but immediately yield.
     */
    private boolean homeConnection = true;

    private Vector connections = new Vector();
    private ConditionVariable disconnected = Ibis.myIbis.createCV();

    private long upcall_poll;

    // STATISTICS
    private int enqueued_msgs;
    private int upcall_msgs;
    private int dequeued_msgs;

    static {
	if (DEBUG) {
	    if (Ibis.myIbis.myCpu == 0) {
		System.err.println(Thread.currentThread() + "Turn on ReceivePort.DEBUG");
	    }
	}
    }

    ReceivePort(PortType type, String name) throws IOException {
	this(type, name, null, null, false);
    }

    ReceivePort(PortType type,
	        String name,
		ibis.ipl.Upcall upcall,
		ibis.ipl.ReceivePortConnectUpcall connectUpcall,
		boolean connectionAdministration)
	    throws IOException {
	this.type = type;
	this.name = name;
	this.upcall = upcall;
	this.connectUpcall = connectUpcall;

/// @@@ implement connectionAdministration --Rob

	ident = new ReceivePortIdentifier(name, type.name());
// System.err.println(this + ": create ReceivePort; ibisIdent = " + ident);

	Ibis.myIbis.lock();
	livingPorts++;
	Ibis.myIbis.unlock();
    }

    /**
     * {@inheritDoc}
     **/
    public long getCount() {
	// TODO
	return 0;
    }

    /**
     * {@inheritDoc}
     **/
    public void resetCount() {
	// TODO
    }

    private boolean firstCall = true;

    public synchronized void enableConnections() {
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Enable connections on " + this + " firstCall=" + firstCall);
	}
	if (firstCall) {
	    firstCall = false;
	    if (upcall != null) {
		thread = new Thread(this, "ReceivePort " + this + " upcall thread " + upcallThreads);
		upcallThreads++;
		availableUpcallThread++;
		thread.setDaemon(true);
		thread.start();
	    }
	    if (connectUpcall != null) {
		acceptThread = new AcceptThread(this, connectUpcall);
// System.err.println("And start another AcceptThread(this=" + this + ")");
		acceptThread.setName("ReceivePort accept thread");
		acceptThread.start();
	    }
// System.err.println("In enableConnections: want to bind locally RPort " + this);
	    Ibis.myIbis.lock();
	    Ibis.myIbis.bindReceivePort(this, ident.port);
	    Ibis.myIbis.unlock();
	    try {
// System.err.println("In enableConnections: want to bind RPort " + this);
		((Registry)Ibis.myIbis.registry()).bind(name, ident);
	    } catch (IOException e) {
		System.err.println("registry bind of ReceivePortName fails: " + e);
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
		System.err.println(Thread.currentThread() + "*********** Enable upcalls");
	}
	Ibis.myIbis.lock();
	allowUpcalls = true;
	enable.cv_signal();
	Ibis.myIbis.unlock();
    }

    public synchronized void disableUpcalls() {
	if (DEBUG) {
		System.err.println(Thread.currentThread() + "*********** Disable upcalls");
	}
	allowUpcalls = false;
    }


    boolean connect(ShadowSendPort sp) {
	Ibis.myIbis.checkLockOwned();
	ibis.ipl.SendPortIdentifier id = sp.identifier();
	if (! id.ibis().equals(ident.ibis())) {
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
	} else {
	    return false;
	}
    }


    void disconnect(ShadowSendPort sp) {
	Ibis.myIbis.checkLockOwned();
	connections.remove(sp);
	if (connections.size() == 0) {
	    disconnected.cv_signal();
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
	}
	else {
// System.err.println(Ibis.myIbis.myCpu + ": Create another UpcallThread because the previous one didn't terminate");
// Thread.dumpStack();
	    Thread thread = new Thread(this, "ReceivePort upcall thread " + upcallThreads);
	    upcallThreads++;
	    availableUpcallThread++;
	    thread.start();
	    if (STATISTICS) {
		threadsCreated++;
	    }
	}
    }


    private ReadMessage locate(ShadowSendPort ssp, int msgSeqno) {
	if (ssp.msgSeqno > msgSeqno && ssp.msgSeqno != -1) {
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "This is a SERIOUS BUG: the msgSeqno goes BACK!!!!!!!!!");
	    }
	    ssp.msgSeqno = msgSeqno;
	    return null;
	}

	if (currentMessage != null &&
		currentMessage.shadowSendPort == ssp && currentMessage.msgSeqno == msgSeqno) {
	    return currentMessage;
	}

	ReadMessage scan;
	for (scan = queueFront;
		scan != null &&
		    (scan.shadowSendPort != ssp ||
		     scan.msgSeqno != msgSeqno);
		scan = scan.next) {
	}

	return scan;
    }


    void enqueue(ReadMessage msg) {
	Ibis.myIbis.checkLockOwned();
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Enqueue message " + msg + " in port " + this + " msgHandle " + Integer.toHexString(msg.fragmentFront.msgHandle) + " current queueFront " + queueFront);
	}

// new Throwable().printStackTrace();

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

	if (arrivedWaiters > 0 && ! mePolling) {
// System.err.println("Receiveport signalled");
	    messageArrived.cv_signal();
	}

	if (upcall != null) {
//	    This wakeup() call is not needed. It is already done above. (wakeup does a
//	    messageArrived.cv_signal()).
//	    wakeup();

	    if (Ibis.STATISTICS) {
		upcall_msgs++;
	    }
	    if (availableUpcallThread == 0 && ! aMessageIsAlive) {
// System.err.println("enqueue: Create another UpcallThread because the previous one didn't terminate");
// Thread.dumpStack();
		createNewUpcallThread();
	    }
	}
    }


    private ReadMessage dequeue() {
	Ibis.myIbis.checkLockOwned();
	ReadMessage msg = queueFront;

	if (msg != null) {
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Now dequeue msg " + dequeued_msgs + " 0x" + Integer.toHexString(msg.fragmentFront.msgHandle) + " " + msg);
	    }
	    queueFront = msg.next;
	    if (Ibis.STATISTICS) {
		dequeued_msgs++;
	    }
	}

	return msg;
    }


    void finishMessage() {

	Ibis.myIbis.checkLockOwned();

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "******* Now finish this ReceivePort message: " + currentMessage);
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
	currentMessage = null;
	aMessageIsAlive = false;
	if (liveWaiters > 0) {
	    messageHandled.cv_signal();
	}

	if (queueFront != null) {
	    if (arrivedWaiters > 0) {
		messageArrived.cv_signal();
	    }
	    else if (upcall != null && availableUpcallThread == 0) {
// System.err.println("finishMessage: Create another UpcallThread because the previous one didn't terminate");
		createNewUpcallThread();
	    }
	}

	ssp.tickReceive();
    }


    private PollClient next;
    private PollClient prev;

    public boolean satisfied() {
	return queueFront != null || stop;
    }

    public void wakeup() {
	messageArrived.cv_signal();
    }

    public void poll_wait(long timeout) {
	arrivedWaiters++;
// System.err.println("ReceivePort poll_wait, arrivedWaiters = " + arrivedWaiters);
	try {
	    messageArrived.cv_wait(timeout);
	} catch (InterruptedException e) {
	    // ignore
	}
	arrivedWaiters--;
// System.err.println("ReceivePort woke up, arrivedWaiters = " + arrivedWaiters);
    }

    public PollClient next() {
	return next;
    }

    public PollClient prev() {
	return prev;
    }

    public void setNext(PollClient c) {
	next = c;
    }

    public void setPrev(PollClient c) {
	prev = c;
    }

    private Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }


    void receiveFragment(ShadowSendPort origin,
			 int msgHandle,
			 int msgSize,
			 int msgSeqno,
			 int group)
	    throws IOException {
	Ibis.myIbis.checkLockOwned();

	/* Let's see whether we already have an envelope for this fragment. */
	ReadMessage msg = locate(origin, msgSeqno);
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + " Port " + this +
			       " receive a fragment seqno " + msgSeqno +
			       " size " + msgSize + " that belongs to msg " +
			       msg + "; currentMessage = " + currentMessage +
			       (currentMessage == null ? "" :
				(" .seqno " + currentMessage.msgSeqno)));
	}

// System.err.println(Thread.currentThread() + "Enqueue message in port " + this + " id " + identifier() + " msgHandle " + Integer.toHexString(msgHandle) + " current queueFront " + queueFront);
	boolean lastFrag = (msgSeqno < 0);
	if (lastFrag) {
	    msgSeqno = -msgSeqno;
	}

	/* Let's see whether our ShadowSendPort has a fragment cached */
	ReadFragment f = origin.getFragment();

	boolean firstFrag = (msg == null);
	if (firstFrag) {
	    /* This must be the first fragment of a new message.
	     * Let our ShadowSendPort create an envelope, i.e. a ReadMessage
	     * for it. */
	    msg = origin.getMessage(msgSeqno);
	}

	f.msg       = msg;
	f.lastFrag  = lastFrag;
	f.msgHandle = msgHandle;
	f.msgSize   = msgSize;

	/* Hook up the fragment in the message envelope */
	msg.enqueue(f);
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + " Port " + this +
			       " rcve frag; firstFrag=" + firstFrag);
	}

	    /* Must set in.msgHandle and in.msgSize from here: cannot wait
	     * until we do a read:
	     *  - a message may be empty and still must be able to clear it
	     *  - a Serialized stream starts reading in the constructor */
	if (firstFrag && origin.checkStarted(msg)) {
	    enqueue(msg);
	}
	else if (DEBUG) {
	    System.err.println(Thread.currentThread() + " Port " + this +
			       " rcve frag; NOT checkStarted!!!");
	}
    }


    private ReadMessage doReceive(boolean block) throws IOException {
	Ibis.myIbis.checkLockOwned();

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "******** enter ReceivePort.receive()" + ident);
	}
	while (aMessageIsAlive && ! stop) {
	    liveWaiters++;
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Hit wait in ReceivePort.receive()" + ident + " aMessageIsAlive is true");
	    }
	    try {
		messageHandled.cv_wait();
	    } catch (InterruptedException e) {
		// ignore
	    }
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Past wait in ReceivePort.receive()" + ident + " aMessageIsAlive is true");
	    }
	    liveWaiters--;
	}

	if (upcall != null && queueFront == null) {
	    return null;
	}

	aMessageIsAlive = true;

	// long t = Ibis.currentTime();

// if (upcall != null) System.err.println("Hit receive() in an upcall()");
// for (int i = 0; queueFront == null && i < polls_before_yield; i++) {
// Ibis.myIbis.pollLocked();
// }

	if (queueFront == null) {
	    if (! block) {
		return null;
	    }

	    if (DEBUG) {
		System.err.println(Thread.currentThread() + ", port " + this + ". Hit wait in ReceivePort.receive()" + ident + " queue " + queueFront + " " + messageArrived);
	    }
	    Ibis.myIbis.waitPolling(this, 0, (HOME_CONNECTION_PREEMPTS || ! homeConnection) ? Poll.PREEMPTIVE : Poll.NON_POLLING);

	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Past wait in ReceivePort.receive()" + ident);
	    }
	}

	currentMessage = dequeue();
	if (currentMessage == null) {
	    return null;
	}
	currentMessage.setMsgHandle();

	// Ibis.myIbis.tReceive += Ibis.currentTime() - t;

	return currentMessage;
    }


    private ibis.ipl.ReadMessage receive(ReadMessage finishMe, boolean block)
	    throws IOException {
	Ibis.myIbis.lock();
	try {
// manta.runtime.RuntimeSystem.DebugMe(this, this);
	    if (finishMe != null) {
		finishMe.finishLocked();
	    }
	    return doReceive(block);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public ibis.ipl.ReadMessage receive(long timeout) throws IOException {
	return receive();
    }


    public ibis.ipl.ReadMessage receive(ibis.ipl.ReadMessage finishMe, long timeout) throws IOException {
	return receive(finishMe);
    }


    public ibis.ipl.ReadMessage receive() throws IOException {
	return receive(null, true);
    }


    public ibis.ipl.ReadMessage receive(ibis.ipl.ReadMessage finishMe)
	    throws IOException {
	return receive((ReadMessage)finishMe, true);
    }


    public ibis.ipl.ReadMessage poll() throws IOException {
	return receive(null, false);
    }


    public ibis.ipl.ReadMessage poll(ibis.ipl.ReadMessage finishMe)
	    throws IOException {
	return receive((ReadMessage)finishMe, false);
    }


    public ibis.ipl.DynamicProperties properties() {
	return null;
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
	int n = connections.size();
	ibis.ipl.SendPortIdentifier[] s = new ibis.ipl.SendPortIdentifier[n];
	for (int i = 0; i < n; i++) {
	    s[i] = (SendPortIdentifier)connections.elementAt(i);
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


    private class Shutdown implements PollClient {

	PollClient next;
	PollClient prev;

	public boolean satisfied() {
	    return connections.size() == 0;
	}

	public void wakeup() {
	    disconnected.cv_signal();
	}

	public void poll_wait(long timeout) {
	    try {
		disconnected.cv_wait(timeout);
	    } catch (InterruptedException e) {
		// ignore
	    }
	}

	public PollClient next() {
	    return next;
	}

	public PollClient prev() {
	    return prev;
	}

	public void setNext(PollClient c) {
	    next = c;
	}

	public void setPrev(PollClient c) {
	    prev = c;
	}

	Thread me;

	public Thread thread() {
	    return me;
	}

	public void setThread(Thread thread) {
	    me = thread;
	}

    }


    public void free() {

	if (DEBUG) {
	    System.out.println(Thread.currentThread() + name + ":Starting receiveport.free upcall = " + upcall);
	}

	Shutdown shutdown = new Shutdown();

	Ibis.myIbis.lock();

	if (DEBUG) {
	    System.out.println(Thread.currentThread() + name + ": got Ibis lock");
	}

	stop = true;

	messageHandled.cv_bcast();
	messageArrived.cv_bcast();

	if (DEBUG) {
	    System.out.println(Thread.currentThread() + name + ": Enter shutdown.waitPolling; connections = " + connectionToString());
	}
	try {
	    while (connections.size() > 0) {
// System.out.println(connectionToString());
		Ibis.myIbis.waitPolling(shutdown, 0, Poll.NON_PREEMPTIVE);
	    }
	} catch (IOException e) {
	    /* well, if it throws an exception, let's quit.. */
	}
	if (DEBUG) {
	    System.out.println(Thread.currentThread() + name + ": Past shutdown.waitPolling");
	}
	/*
	while (connections.size() > 0) {
	    try {
		disconnected.cv_wait();
	    } catch (InterruptedException e) {
		// ignore
	    }

	    if (upcall != null) {
		if (DEBUG) {
		    System.out.println(name +
				       " waiting for all connections to close ("
				       + connections.size() + ")");
		}
		try {
		    wait();
		} catch (InterruptedException e) {
		    // Ignore.
		}
	    } else {
		if (DEBUG) {
		    System.out.println(name +
				       " trying to close all connections (" +
				       connections.size() + ")");
		}

	    }
	}
	*/

	if (connectUpcall != null) {
	    acceptThread.free();
	}

	Ibis.myIbis.unlock();

	/* unregister with name server */
	try {
	    if (DEBUG) {
		System.out.println(Thread.currentThread() + name + ": unregister with name server");
	    }
	    type.freeReceivePort(name);
	} catch(Exception e) {
	    // Ignore.
	}

	if (DEBUG) {
	    System.out.println(Thread.currentThread() + name + ":done receiveport.free");
	}

	Ibis.myIbis.lock();
	    livingPorts--;
	    if (livingPorts == 0) {
		portCounter.s_signal(true);
	    }
	Ibis.myIbis.unlock();
    }


    public void forcedClose() {
	free();
    }


    public void forcedClose(long timeout) {
	forcedClose();
    }


    private String connectionToString() {
	String t = "Connections =";
	for (int i = 0; i < connections.size(); i++) {
	    t = t + " " + (ShadowSendPort)connections.elementAt(i);
	}
	return t;
    }


    public void run() {

	if (upcall == null) {
	    System.err.println(Thread.currentThread() + "ReceivePort " + name + ", daemon = " + this + " runs but upcall == null");
	}
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + " ReceivePort " + name + ", daemon = " + this + " runs");
	}

	Ibis.myIbis.lock();

	try {
	    Thread me = Thread.currentThread();

	    while (true) {
		ReadMessage msg = null;
		
		if (stop) {
		    if (DEBUG) {
			System.err.println(Thread.currentThread() + "Receive port daemon " + this +
					   " upcall thread polls " + upcall_poll);
		    }
		    upcallThreads--;
		    break;
		}

		/* Nowadays, use NON_POLLING for the preempt flag. */
		if (DEBUG) {
		    System.err.println(Thread.currentThread() + "*********** This ReceivePort daemon hits wait, daemon " + this + " queueFront = " + queueFront);
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

		while (queueFront == null && ! stop) {
		    // // // Ibis.myIbis.waitPolling(this, 0, Poll.NON_PREEMPTIVE);
		    // Ibis.myIbis.waitPolling(this, 0, (HOME_CONNECTION_PREEMPTS || ! homeConnection) ? Poll.NON_PREEMPTIVE : Poll.NON_POLLING);
		    Ibis.myIbis.waitPolling(this, 0, Poll.NON_POLLING);
		}
		if (DEBUG) {
		    upcall_poll++;
		    System.err.println(Thread.currentThread() + "*********** This ReceivePort daemon past wait, daemon " + this + " queueFront = " + queueFront);
		}

		while (! allowUpcalls) {
		    try {
			enable.cv_wait();
		    } catch (InterruptedException e) {
			// ignore
		    }
		}

		msg = doReceive(true /* block */);	// May throw an IOException

		if (msg != null) {
// System.err.println(Thread.currentThread() + ": perform upcall for msg " + msg);
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
		    if (! msg.finished && msg.creator == me) {
			msg.finishLocked();
			if (STATISTICS) {
			    implicitFinish++;
			}
		    }
		}
	    }

	} catch (IOException e) {
	    if (e == null) {
		System.err.println("A NULL Exception?????");
		System.err.println("My stack: ");
		Thread.dumpStack();
		// manta.runtime.RuntimeSystem.DebugMe(e, e);
	    } else {
		System.err.println(e);
		e.printStackTrace();
	    }

	    // System.err.println("My stack: ");
	    // Thread.dumpStack();
	    // System.exit(44);

	} finally {
	    Ibis.myIbis.unlock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Receive port " + name +
			       " upcall thread polls " + upcall_poll);
	}
// System.err.println("ReceivePort " + this + " upcallThread " + Thread.currentThread().getName() + " snuffs it");
    }


    static void report(java.io.PrintStream out) {
	if (STATISTICS) {
	    out.println(Ibis.myIbis.myCpu + ": ReceivePort threads created " + threadsCreated +
			" (cached " + threadsCached + "); finish Xpl " + (explicitFinish - implicitFinish) + " Mpl " + implicitFinish);
	}
    }


    static void end() {
	// assert(Ibis.myIbis.locked();
	while (livingPorts > 0) {
	    try {
		portCounter.s_wait(0);
	    } catch (IOException e) {
		break;
	    }
	}
    }

}
