package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import java.util.Vector;

import ibis.ipl.IbisException;
import ibis.ipl.ConditionVariable;

class ReceivePort
    implements ibis.ipl.ReceivePort, Protocol, Runnable, PollClient {

    ibis.ipl.impl.messagePassing.PortType type;
    ReceivePortIdentifier ident;
    int connectCount = 0;
    String name;	// needed to unbind

    ibis.ipl.impl.messagePassing.ReadMessage queueFront;
    ibis.ipl.impl.messagePassing.ReadMessage queueTail;
    ConditionVariable messageArrived = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
    int arrivedWaiters = 0;

    boolean aMessageIsAlive = false;
    ConditionVariable messageHandled = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
    int liveWaiters = 0;
    ibis.ipl.impl.messagePassing.ReadMessage currentMessage = null;

    Thread thread;
    ibis.ipl.Upcall upcall;

    volatile boolean stop = false;

    private ibis.ipl.ConnectUpcall connectUpcall;
    private boolean allowConnections = false;
    private AcceptThread acceptThread;
    private boolean allowUpcalls = false;
    ConditionVariable enable = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);

    private int handlingReceive = 0;

    Vector connections = new Vector();
    ConditionVariable disconnected = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);


    ReceivePort(ibis.ipl.impl.messagePassing.PortType type, String name) throws IbisException {
	this(type, name, null, null);
    }

    ReceivePort(ibis.ipl.impl.messagePassing.PortType type,
	        String name,
		ibis.ipl.Upcall upcall,
		ibis.ipl.ConnectUpcall connectUpcall)
			throws IbisException {
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
	    synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.bindReceivePort(this, ident.port);
	    }
	    try {
// System.err.println("In enableConnections: want to bind RPort " + this);
		((Registry)ibis.ipl.impl.messagePassing.Ibis.myIbis.registry()).bind(name, ident);
	    } catch (ibis.ipl.IbisException e) {
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
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    allowUpcalls = true;
	    enable.cv_signal();
	}
    }

    public synchronized void disableUpcalls() {
	allowUpcalls = false;
    }


    boolean connect(ShadowSendPort sp) {
	// already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (connectUpcall == null || acceptThread.checkAccept(sp.identifier())) {
	    connections.add(sp);
	    return true;
	} else {
	    return false;
	}
    }


    void disconnect(ShadowSendPort sp) {
	// already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
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


    void enqueue(ibis.ipl.impl.messagePassing.ReadMessage msg) {
	// Is already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
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
// System.err.println("Notify this ReceivePort upcall, this " + this + " queueFront " + queueFront + " msg " + msg + " pandaMessage " + Integer.toHexString(msg.pandaMessage));
	    wakeup();

	    if (handlingReceive == 0) {
		createNewUpcallThread();
	    }
	}
    }


    private ibis.ipl.impl.messagePassing.ReadMessage dequeue() {
	// Is already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	ibis.ipl.impl.messagePassing.ReadMessage msg = queueFront;

	if (msg != null) {
	    queueFront = msg.next;
// System.err.println("Now dequeue msg " + msg + " pandaMessage " + Integer.toHexString(msg.pandaMessage) + " for ReadMessage " + this + " queueFront := " + queueFront);
	}

	return msg;
    }


    void finishMessage() {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

// System.err.println("Now finish this ReceivePort message:");
// Thread.dumpStack();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    ShadowSendPort ssp = currentMessage.shadowSendPort;
	    ssp.in.clearPandaMessage();
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


    ibis.ipl.impl.messagePassing.ReadMessage createReadMessage(ShadowSendPort origin,
				       int msg) throws IbisException {
	// checkLockOwned();
	ibis.ipl.impl.messagePassing.ReadMessage m = origin.cachedMessage;

	if (m == null) {
	    switch (type.serializationType) {
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
		m = new ibis.ipl.impl.messagePassing.ReadMessage(origin, this, msg);
System.err.println("Create a -none- ReadMessage " + m);
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
		m = new SerializeReadMessage(origin, this, msg);
System.err.println("Create a -sun- ReadMessage " + m);
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
		m = new MantaReadMessage(origin, this, msg);
System.err.println("Create a -manta- ReadMessage " + m);
		break;
	    }
	} else {
	    /*
	    if (type.serializationType == ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN) {
		if (((SerializeReadMessage)m).obj_in != origin.obj_in) {
		    throw new IbisException("ShadowSendPort obj_in has changed under our hands");
		}
	    }

	    if (m.shadowSendPort != origin) {
		throw new IbisException("ReadMessage shadowSendPort has changed under our hands");
	    }
	    if (m.port != this) {
		throw new IbisException("ReadMessage port has changed under our hands");
	    }
	*/

	    origin.cachedMessage = null;
	    m.pandaMessage = msg;
	}

// System.err.println(Thread.currentThread() + "Enqueue message in port " + this + " id " + identifier() + " pandaMessage " + Integer.toHexString(msg) + " current queueFront " + queueFront);
	enqueue(m);

	return m;
    }


    public ibis.ipl.ReadMessage receive() throws IbisException {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {

	    while (aMessageIsAlive && ! stop) {
		liveWaiters++;
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
// System.err.println(Thread.currentThread() + "Hit wait in ReceivePort.receive()" + this.ident + " queue " + queueFront + " " + messageArrived);
		arrivedWaiters++;
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, true);
		arrivedWaiters--;
	    }
// System.err.println(Thread.currentThread() + "Past wait in ReceivePort.receive()" + this.ident);

	    currentMessage = dequeue();

	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.tReceive += Ibis.currentTime() - t;
	}

	if (currentMessage != null) {
// System.err.println(Thread.currentThread() + "Set pandaMessage for reader in ReadMessage.receive()");
	    currentMessage.shadowSendPort.checkConnection(currentMessage);

// manta.runtime.RuntimeSystem.DebugMe(0, 0);
// System.err.println("Serialization NONE " + ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE + " SUN " + ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN + " MANTA " + ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA + " current " + type.serializationType);
	    switch (type.serializationType) {
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
		break;
	    default:
		// System.err.println(Thread.currentThread() + "Request serialization type " + type.serializationType + ": unimplemented");
		System.exit(33);
		break;
	    }
// manta.runtime.RuntimeSystem.DebugMe(0, 1);
	}

	return currentMessage;
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
	    System.out.println(name + ":Starting receiveport.free");
	}

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	Shutdown shutdown = new Shutdown();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {

	    stop = true;

	    messageHandled.cv_bcast();
	    messageArrived.cv_bcast();

	    try {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(shutdown, 0, false);
	    } catch (IbisException e) {
		/* well, if it throws an exception, let's quit.. */
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
		    } catch(Exception e) {
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
	}

	/* unregister with name server */
	try {
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

	try {
	    while (true) {
		ibis.ipl.ReadMessage m;
		
		m = null;

		synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		    if (stop || handlingReceive > 0) {
			break;
		    }

		    /* Avoid waiting threads in waitPolling. Having too many
		     * (like always one server in this ReceivePort) makes the
		     * poll for an expected reply very expensive.
		     * Nowadays, pass 'false' for the preempt flag. */
		    handlingReceive++;
// System.err.println("*********** This ReceivePort daemon hits wait, daemon " + this + " queueFront = " + queueFront);
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, false);
// System.err.println("*********** This ReceivePort daemon past wait, daemon " + this + " queueFront = " + queueFront);

		    while (! allowUpcalls) {
			enable.cv_wait();
		    }

		    m = receive();

		    handlingReceive--;
		}

		if (m != null) {
System.err.println("Now process this msg " + m + " pandaMessage " + Integer.toHexString(((ibis.ipl.impl.messagePassing.ReadMessage)m).pandaMessage));
// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
		    upcall.upcall(m);
		}
	    }
	} catch (IbisException e) {
	    System.err.println(e);
	    e.printStackTrace();

	    System.err.println("My stack: ");
	    Thread.dumpStack();
	    System.exit(44);
	}
    }

}
