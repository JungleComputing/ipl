package ibis.ipl.impl.messagePassing;

import java.util.Vector;

import ibis.ipl.IbisIOException;
import ibis.ipl.ConditionVariable;

class ReceivePort
    implements ibis.ipl.ReceivePort, Protocol, Runnable, PollClient {

    private static final boolean DEBUG = false;

    ibis.ipl.impl.messagePassing.PortType type;
    ReceivePortIdentifier ident;
    int connectCount = 0;
    String name;	// needed to unbind

    ibis.ipl.impl.messagePassing.ReadMessage queueFront;
    ibis.ipl.impl.messagePassing.ReadMessage queueTail;
    ConditionVariable messageArrived = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
    int arrivedWaiters = 0;

    boolean aMessageIsAlive = false;
    ConditionVariable messageHandled = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
    int liveWaiters = 0;
    private ibis.ipl.impl.messagePassing.ReadMessage currentMessage = null;

    Thread thread;
    ibis.ipl.Upcall upcall;

    volatile boolean stop = false;

    private ibis.ipl.ConnectUpcall connectUpcall;
    private boolean allowConnections = false;
    private AcceptThread acceptThread;
    private boolean allowUpcalls = false;
    ConditionVariable enable = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

    private int handlingReceive = 0;

    Vector connections = new Vector();
    ConditionVariable disconnected = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

    private long upcall_poll;

    static {
	if (DEBUG) {
	    System.err.println("Turn on ReceivePort.DEBUG");
	}
    }

    ReceivePort(ibis.ipl.impl.messagePassing.PortType type, String name) throws IbisIOException {
	this(type, name, null, null);
    }

    ReceivePort(ibis.ipl.impl.messagePassing.PortType type,
	        String name,
		ibis.ipl.Upcall upcall,
		ibis.ipl.ConnectUpcall connectUpcall)
			throws IbisIOException {
	this.type = type;
	this.name = name;
	this.upcall = upcall;
	this.connectUpcall = connectUpcall;

	ident = new ReceivePortIdentifier(name, type.name());
    }

    private boolean firstCall = true;

    public synchronized void enableConnections() {
	if (firstCall) {
	    firstCall = false;
	    if (upcall != null) {
		thread = new Thread(this);
		thread.start();
	    }
	    if (connectUpcall != null) {
		acceptThread = new AcceptThread(this, connectUpcall);
		acceptThread.start();
	    }
	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
// System.err.println("In enableConnections: want to bind locally RPort " + this);
	    // synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
		ibis.ipl.impl.messagePassing.Ibis.myIbis.bindReceivePort(this, ident.port);
	    // }
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    try {
// System.err.println("In enableConnections: want to bind RPort " + this);
		((Registry)ibis.ipl.impl.messagePassing.Ibis.myIbis.registry()).bind(name, ident);
	    } catch (ibis.ipl.IbisIOException e) {
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
	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    allowUpcalls = true;
	    enable.cv_signal();
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }

    public synchronized void disableUpcalls() {
	allowUpcalls = false;
    }


    boolean connect(ShadowSendPort sp) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (connectUpcall == null || acceptThread.checkAccept(sp.identifier())) {
	    connections.add(sp);
	    return true;
	} else {
	    return false;
	}
    }


    void disconnect(ShadowSendPort sp) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	connections.remove(sp);
// System.err.println(Thread.currentThread() + "Disconnect SendPort " + sp + " from ReceivePort " + this + ", remaining connections " + connections.size());
	if (connections.size() == 0) {
	    disconnected.cv_signal();
	}
    }


    private void createNewUpcallThread() {
	new Thread(this).start();
    }


    private ibis.ipl.impl.messagePassing.ReadMessage locate(ShadowSendPort ssp,
							    int msgSeqno) {
	if (ssp.msgSeqno > msgSeqno) {
	    ssp.msgSeqno = msgSeqno;
	    return null;
	}

	if (currentMessage != null &&
		currentMessage.shadowSendPort == ssp && currentMessage.msgSeqno == msgSeqno) {
	    return currentMessage;
	}

	ibis.ipl.impl.messagePassing.ReadMessage scan;
	for (scan = queueFront;
		scan != null &&
		    scan.shadowSendPort != ssp &&
		    scan.msgSeqno != msgSeqno;
		scan = scan.next) {
	}

	return scan;
    }


    void enqueue(ibis.ipl.impl.messagePassing.ReadMessage msg) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Enqueue message " + msg + " in port " + this + " msgHandle " + Integer.toHexString(msg.fragmentFront.msgHandle) + " current queueFront " + queueFront);
	}
	if (queueFront == null) {
	    queueFront = msg;
	} else {
	    queueTail.next = msg;
	}
	queueTail = msg;
	msg.next = null;

	if (arrivedWaiters > 0) {
	    messageArrived.cv_signal();
	}

	if (upcall != null) {
// System.err.println("Notify this ReceivePort upcall, this " + this + " queueFront " + queueFront + " msg " + msg + " msgHandle " + Integer.toHexString(msg.msgHandle));
	    wakeup();

	    if (handlingReceive == 0) {
		createNewUpcallThread();
	    }
	}
    }


    private ibis.ipl.impl.messagePassing.ReadMessage dequeue() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	ibis.ipl.impl.messagePassing.ReadMessage msg = queueFront;

	if (msg != null) {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Now dequeue msg " + msg);
		System.err.println("Now dequeue msg " + msg + " msgHandle " + Integer.toHexString(msg.fragmentFront.msgHandle) + " for ReadMessage " + this + " queueFront := " + msg.next);
	    }
	    queueFront = msg.next;
	}

	return msg;
    }


    void finishMessage() {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("******* Now finish this ReceivePort message: " + currentMessage);
	    // Thread.dumpStack();
	}

	ShadowSendPort ssp = currentMessage.shadowSendPort;
	if (ssp.cachedMessage == null) {
	    ssp.cachedMessage = currentMessage;
	}
	currentMessage = null;
	aMessageIsAlive = false;
	if (liveWaiters > 0) {
	    messageHandled.cv_signal();
	}
	if (queueFront != null && arrivedWaiters > 0) {
	    messageArrived.cv_signal();
	}

	ssp.tickReceive();
    }


    PollClient next;
    PollClient prev;

    public boolean satisfied() {
	return queueFront != null || stop;
    }

    public void wakeup() {
	messageArrived.cv_signal();
    }

    public void poll_wait(long timeout) {
	messageArrived.cv_wait(timeout);
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


    void receiveFragment(ShadowSendPort origin,
			 int msgHandle,
			 int msgSize,
			 int msgSeqno)
	    throws IbisIOException {
	// checkLockOwned();

	/* Let's see whether we already have an envelope for this fragment. */
	ibis.ipl.impl.messagePassing.ReadMessage msg = locate(origin, msgSeqno);
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + " Port " + this + " receive a fragment seqno " + msgSeqno + " size " + msgSize + " that belongs to msg " + msg + "; currentMessage = " + currentMessage + (currentMessage == null ? "" : (" .seqno " + currentMessage.msgSeqno)));
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

	    /* Must set in.msgHandle and in.msgSize from here: cannot wait
	     * until we do a read:
	     *  - a message may be empty and still must be able to clear it
	     *  - a Serialized stream starts reading in the constructor */
	if (firstFrag && origin.checkStarted(msg)) {
	    enqueue(msg);
	}
    }


    ibis.ipl.ReadMessage doReceive() throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "******** enter ReceivePort.receive()" + this.ident);
	}
	while (aMessageIsAlive && ! stop) {
	    liveWaiters++;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + "Hit wait in ReceivePort.receive()" + this.ident + " aMessageIsAlive is true");
	    }
	    messageHandled.cv_wait();
	    liveWaiters--;
	}
	aMessageIsAlive = true;

	// long t = Ibis.currentTime();

// if (upcall != null) System.err.println("Hit receive() in an upcall()");
for (int i = 0; queueFront == null && i < Poll.polls_before_yield; i++) {
ibis.ipl.impl.messagePassing.Ibis.myIbis.rcve_poll.poll();
}

	if (queueFront == null) {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + "Hit wait in ReceivePort.receive()" + this.ident + " queue " + queueFront + " " + messageArrived);
	    }
	    arrivedWaiters++;
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, true);
	    arrivedWaiters--;
	}
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "Past wait in ReceivePort.receive()" + this.ident);
	}

	currentMessage = dequeue();
	if (currentMessage == null) {
	    return null;
	}
	currentMessage.in.setMsgHandle(currentMessage);

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.tReceive += Ibis.currentTime() - t;

	return currentMessage;
    }


    public ibis.ipl.ReadMessage receive(ibis.ipl.ReadMessage finishMe)
	    throws IbisIOException {
	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
// manta.runtime.RuntimeSystem.DebugMe(this, this);
	    if (finishMe != null) {
		finishMe.finish();
	    }
	    return doReceive();
	// }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    public ibis.ipl.ReadMessage receive() throws IbisIOException {
	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
// manta.runtime.RuntimeSystem.DebugMe(this, this);
	    return doReceive();
	// }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    public ibis.ipl.DynamicProperties properties() {
	return null;
    }

    public ibis.ipl.ReceivePortIdentifier identifier() {
	return ident;
    }


    class Shutdown implements PollClient {

	PollClient next;
	PollClient prev;

	public boolean satisfied() {
	    return connections.size() == 0;
	}

	public void wakeup() {
	    disconnected.cv_signal();
	}

	public void poll_wait(long timeout) {
	    disconnected.cv_wait(timeout);
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

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(name + ":Starting receiveport.free upcall = " + upcall);
	}

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	Shutdown shutdown = new Shutdown();

	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.out.println(name + ": got Ibis lock");
	    }

	    stop = true;

	    messageHandled.cv_bcast();
	    messageArrived.cv_bcast();

	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.out.println(name + ": Enter shutdown.waitPolling");
	    }
	    try {
		while (connections.size() > 0) {
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(shutdown, 0, false);
		}
	    } catch (IbisIOException e) {
		/* well, if it throws an exception, let's quit.. */
	    }
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.out.println(name + ": Past shutdown.waitPolling");
	    }
	    /*
	    while (connections.size() > 0) {
		disconnected.cv_wait();

		if (upcall != null) {
		    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
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
		    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
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
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();

	/* unregister with name server */
	try {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.out.println(name + ": unregister with name server");
	    }
	    type.freeReceivePort(name);
	} catch(Exception e) {
	    // Ignore.
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(name + ":done receiveport.free");
	}
    }

    public void run() {

	if (upcall == null) {
	    System.err.println(Thread.currentThread() + "ReceivePort runs but upcall == null");
	}
System.err.println(Thread.currentThread() + " ReceivePort " + name + " runs");

	try {
	    while (true) {
		ibis.ipl.ReadMessage msg;
		
		msg = null;

		// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
		try {
		    if (stop || handlingReceive > 0) {
			if (DEBUG) {
			    System.err.println("Receive port " + name +
					       " upcall thread polls " + upcall_poll);
			}
			break;
		    }

		    /* Avoid waiting threads in waitPolling. Having too many
		     * (like always one server in this ReceivePort) makes the
		     * poll for an expected reply very expensive.
		     * Nowadays, pass 'false' for the preempt flag. */
		    handlingReceive++;
// System.err.println("*********** This ReceivePort daemon hits wait, daemon " + this + " queueFront = " + queueFront);
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, false);
		    if (DEBUG) {
			upcall_poll++;
		    }
// System.err.println("*********** This ReceivePort daemon past wait, daemon " + this + " queueFront = " + queueFront);

		    while (! allowUpcalls) {
			enable.cv_wait();
		    }

		    msg = doReceive();	// May throw an IbisIOException

		    handlingReceive--;
		// }
		} finally {
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
		}

		if (msg != null) {
		    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
			System.err.println("Now process this msg " + msg + " msg.fragmentFront.msgHandle " + Integer.toHexString(((ibis.ipl.impl.messagePassing.ReadMessage)msg).fragmentFront.msgHandle));
		    }
// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
		    upcall.upcall(msg);
		}
	    }

	} catch (IbisIOException e) {
	    System.err.println(e);
	    e.printStackTrace();

	    // System.err.println("My stack: ");
	    // Thread.dumpStack();
	    // System.exit(44);
	}

	if (DEBUG) {
	    System.err.println("Receive port " + name +
			       " upcall thread polls " + upcall_poll);
	}
    }

}
