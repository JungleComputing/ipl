package ibis.ipl.impl.messagePassing;

import ibis.ipl.impl.generic.IbisIdentifierTable;

import java.util.Vector;
import java.util.Hashtable;

import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;

import ibis.ipl.impl.generic.ConditionVariable;
import ibis.ipl.impl.generic.Monitor;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.io.Replacer;
import ibis.ipl.StaticProperties;
import ibis.ipl.impl.generic.IbisIdentifierTable;

public class Ibis extends ibis.ipl.Ibis {

    static final boolean DEBUG = false;
    static final boolean STATISTICS = false;

    static Ibis globalIbis;

    private IbisIdentifier ident;

    IbisIdentifierTable identTable = new IbisIdentifierTable();

    Registry registry;

    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean started = false;
    private Vector ibisNameService = new Vector();

    private final StaticProperties systemProperties = new StaticProperties();

    static Ibis	myIbis;

    private Poll rcve_poll;


    Monitor monitor = new Monitor();

    ConditionVariable createCV() {
		// return new ConditionVariable(this);
		return monitor.createCV();
    }

    int nrCpus;
    int myCpu;

    IbisWorld world;

    PortHash[] sendPorts;
    PortHash rcvePorts;

    int sendPort;
    int receivePort;

    native String[] ibmp_init(String[] args);
    native void ibmp_start();
    native void ibmp_end();

    long tMsgPoll;
    long tSend;
    long tReceive;
    long tMsgSend;

    Ibis() throws IbisException {

	if (globalIbis == null) {
	    globalIbis = this;
	}

	// Set my properties.
	systemProperties.add("reliability", "true");
	systemProperties.add("multicast", "false");
	systemProperties.add("totally ordered", "false");
	systemProperties.add("open world", "false");
	if (DEBUG) {
	    System.err.println("Turn on Ibis.DEBUG");
	}

//      the message passing version doesn't use conversions --Rob
//	ibis.io.Conversion.classInit();

	/*
	 * This is an 1.3 feature; cannot we use it please?
	 */
	Runtime.getRuntime().addShutdownHook(new Thread("Ibis ShutdownHook") {
			public void run() {
				report();
			}
		});
	/* */
    }


    public ibis.ipl.PortType createPortType(String name,
					    StaticProperties p)
	    throws IbisException {

		myIbis.lock();
		PortType tp = new PortType(this, name, p);
		portTypeList.put(name, tp);
		myIbis.unlock();
		
		return tp;
    }


    public ibis.ipl.Registry registry() {
		return registry;
    }


    ReceivePortNameServer createReceivePortNameServer() throws IbisIOException{
		return new ReceivePortNameServer();
    }


    ReceivePortNameServerClient createReceivePortNameServerClient() {
		return new ReceivePortNameServerClient();
    }



    boolean getInputStreamMsg(int tags[]) {
		return ByteInputStream.getInputStreamMsg(tags);
    }

    public StaticProperties properties() {
		return systemProperties;
    }

    public ibis.ipl.IbisIdentifier identifier() {
		return ident;
    }


    native void send_join(int to, byte[] serialForm);
    native void send_leave(int to, byte[] serialForm);

    /* Called from native */
    void join_upcall(byte[] serialForm) throws IbisIOException {
		// checkLockOwned();
		//manta.runtime.RuntimeSystem.DebugMe(ibisNameService, world);

		IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
		if (DEBUG) {
			System.err.println("Receive join message " + id.name() + "; now world = " + world + "; serialForm[" + serialForm.length + "] = " + serialForm);
		}
		ibisNameService.add(id);
		world.join(id);
    }

    /* Called from native */
    void leave_upcall(byte[] serialForm) {
	// checkLockOwned();
	try {
	    IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
	    ibisNameService.remove(id);
	    world.leave(id);
	} catch (IbisIOException e) {
	    // just ignore the leave call, then
	}
    }


    public void join(ibis.ipl.IbisIdentifier id) {
	    if (DEBUG) System.err.println(myCpu + ": An Ibis.join call for " + id);
	if (resizeHandler != null) {
	    resizeHandler.join(id);
	}
    }


    public void leave(ibis.ipl.IbisIdentifier id) {
	if (resizeHandler != null) {
	    resizeHandler.leave(id);
	}
    }


    static void dumpStack() {
	new Throwable().printStackTrace();
    }


    protected void init() throws IbisException, IbisIOException {

	if (myIbis != null) {
	    throw new IbisIOException("Only one Ibis allowed");
	} else {
	    myIbis = this;
	}

// System.err.println("Gonna load libibis_mp.so");
	// System.loadLibrary("ibis_mp");
// System.err.println("Loaded libibis_mp.so");

	rcve_poll = new Poll();

	registry = new Registry();

	    /* Fills in:
		nrCpus;
		myCpu;
	     */
	ibmp_init(null);
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "ibp lives...");
	    System.err.println(Thread.currentThread() + "Ibis.poll = " + rcve_poll);
	}

	world = new IbisWorld();

	ident = new IbisIdentifier(name, myCpu);

	sendPorts = new PortHash[myIbis.nrCpus];
	for (int i = 0; i < myIbis.nrCpus; i++) {
	    sendPorts[i] = new PortHash();
	}
	rcvePorts = new PortHash();

	ibmp_start();
	rcve_poll.wakeup();

	registry.init();
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Registry lives...");
	}

	myIbis.lock();
	try {
	    if (DEBUG) {
		System.err.println("myCpu " + myCpu + " nrCpus " + nrCpus + " world " + world);
	    }

	    for (int i = 0; i < nrCpus; i++) {
		if (i != myCpu) {
		    if (DEBUG) {
			System.err.println("Send join message to " + i);
		    }
		    send_join(i, ident.getSerialForm());
		}
	    }

	    ibisNameService.add(ident);
	    world.join(ident);
	} finally {
	    myIbis.unlock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Ibis lives...");
	}
    }


    public void openWorld() {
	myIbis.lock();
	world.open();
	myIbis.unlock();
    }

    public void closeWorld() {
	myIbis.lock();
	world.close();
	myIbis.unlock();
    }


    final void waitPolling(PollClient client, long timeout, int preempt)
	    throws IbisIOException {
		rcve_poll.waitPolling(client, timeout, preempt);
    }

    SendPort createSendPort(PortType type)
	    throws IbisIOException {
		return createSendPort(type, "noname", null);
    }

    SendPort createSendPort(PortType type, String name)
	    throws IbisIOException {
	return createSendPort(type, null, name);
    }

    SendPort createSendPort(PortType type, Replacer r)
	    throws IbisIOException {
	return createSendPort(type, r, null);
    }

    SendPort createSendPort(PortType type, String name, Replacer r)
	    throws IbisIOException {
	return createSendPort(type, r, name);
    }

    SendPort createSendPort(PortType type, Replacer r, String name)
	    throws IbisIOException {
	switch (type.serializationType) {
        case PortType.SERIALIZATION_NONE:
	    System.err.println("MSG: NO SER, name = " + name);
	    return new SendPort(type, name, new OutputConnection());

	case PortType.SERIALIZATION_SUN:
	    System.err.println("MSG: SUN SER, name = " + name);
	    return new SerializeSendPort(type, name, new OutputConnection(), r);

	case PortType.SERIALIZATION_IBIS:
	    System.err.println("MSG: IBIS SER, name = " + name);
	    return new IbisSendPort(type, name, new OutputConnection(), r);

	default:
	    throw new IbisIOException("No such serialization type " + type.serializationType);
	}
    }

    native long currentTime();
    native double t2d(long t);

    final void lock() {
	monitor.lock();
    }

    final void unlock() {
	monitor.unlock();
    }

    final void checkLockOwned() {
	monitor.checkImOwner();
    }

    final void checkLockNotOwned() {
	monitor.checkImNotOwner();
    }


    IbisIdentifier lookupIbis(String name, int cpu) throws IbisIOException {
// System.err.println("Ibis.lookup(): Want to look up IbisId \"" + name + "\"");
// manta.runtime.RuntimeSystem.DebugMe(myIbis.ident, myIbis.ident.name());
// System.err.println("Ibis.lookup(): My ibis.ident = " + myIbis.ident + " ibis.ident.name() = " + myIbis.ident.name());
	if (myIbis.ident.name().equals(name)) {
	    return myIbis.ident;
	}

	for (int i = 0; i < ibisNameService.size(); i++) {
	    IbisIdentifier id = (IbisIdentifier)ibisNameService.get(i);
	    if (id.name().equals(name)) {
// System.err.println("Found IbisId " + name);
		return id;
	    }
	}

	return null;
    }


    IbisIdentifier lookupIbis(byte[] serialForm) throws IbisIOException {
// System.err.println("Ibis.lookup(): Want to look up IbisId \"" + name + "\"");
// manta.runtime.RuntimeSystem.DebugMe(myIbis.ident, myIbis.ident.name());
// System.err.println("Ibis.lookup(): My ibis.ident = " + myIbis.ident + " ibis.ident.name() = " + myIbis.ident.name());
	
	IbisIdentifier id = IbisIdentifier.createIbisIdentifier(serialForm);
	String name = id.name();
	if (lookupIbis(id.name(), id.getCPU()) == null) {
	    ibisNameService.add(id);
	}

	return id;
    }


    PortType getPortTypeLocked(String name) {
	return (PortType)portTypeList.get(name);
    }


    public ibis.ipl.PortType getPortType(String name) {
	myIbis.lock();
	PortType tp = (PortType)getPortTypeLocked(name);
	myIbis.unlock();
	return tp;
    }


    void bindSendPort(ShadowSendPort p, int cpu, int port) {
	// checkLockOwned();
	sendPorts[cpu].bind(port, p);
    }

    void bindReceivePort(ReceivePort p, int port) {
	// checkLockOwned();
	rcvePorts.bind(port, p);
    }

    ShadowSendPort lookupSendPort(int cpu, int port) {
	// checkLockOwned();
	return (ShadowSendPort)sendPorts[cpu].lookup(port);
    }

    void unbindSendPort(int cpu, int port) {
	// checkLockOwned();
	sendPorts[cpu].unbind(port);
    }

    ReceivePort lookupReceivePort(int port) {
	// checkLockOwned();
	return (ReceivePort)rcvePorts.lookup(port);
    }


    int[] inputStreamMsgTags = new int[6];

    final boolean inputStreamPoll() throws IbisIOException {
	if (getInputStreamMsg(inputStreamMsgTags)) {
	    receiveFragment(inputStreamMsgTags[0],
			    inputStreamMsgTags[1],
			    inputStreamMsgTags[2],
			    inputStreamMsgTags[3],
			    inputStreamMsgTags[4],
			    inputStreamMsgTags[5]);
	    return true;
	}

	return false;
    }


    private void receiveFragment(int src_cpu,
				 int src_port,
				 int dest_port,
				 int msgHandle,
				 int msgSize,
				 int msgSeqno)
	    throws IbisIOException {
	// checkLockOwned();
// System.err.println(Thread.currentThread() + "receiveFragment");
	ReceivePort port = lookupReceivePort(dest_port);
// System.err.println(Thread.currentThread() + "receiveFragment port " + port);
	ShadowSendPort origin = lookupSendPort(src_cpu, src_port);
// System.err.println(Thread.currentThread() + "receiveFragment origin " + origin);

	if (origin == null) {
	    throw new IbisIOException("Receive message from sendport we're not connected to");
	}

	port.receiveFragment(origin, msgHandle, msgSize, msgSeqno);
    }


    boolean pollLocked() throws IbisIOException {
	return rcve_poll.poll();
    }


    public void poll() throws IbisIOException {
	try {
	    myIbis.lock();
	    pollLocked();
	} finally {
	    myIbis.unlock();
	}
    }


    public static void resetStats() {
	myIbis.rcve_poll.reset_stats();
    }


    private native void ibmp_report(int out);

    private void report() {
	ConditionVariable.report(System.out);
	ReceivePort.report(System.out);
	if (rcve_poll != null) {
	    rcve_poll.report(System.out);
	}
//	IbisSerializationOutputStream.statistics();
	ibmp_report(1);
    }


    private boolean ended = false;

    public void end() {
	if (ended) {
	    return;
	}
	ended = true;

	registry.end();

// System.err.println("Ibis.end(): grab Ibis lock");
	myIbis.lock();

	ibisNameService.remove(ident);
	try {
	    byte[] sf = ident.getSerialForm();
	    for (int i = 0; i < nrCpus; i++) {
		if (i != myCpu) {
// System.err.println("Send leave message to " + i);
		    send_leave(i, sf);
		}
	    }
	} catch (IbisIOException e) {
	    System.err.println("Cannot send leave msg");
	}
	world.leave(ident);

	System.err.println("t native poll " + t2d(tMsgPoll) + " send " + t2d(tMsgSend));
	System.err.println("t java   send " + t2d(tSend) + " rcve " + t2d(tReceive));
	// report();

	// ReceivePort.end();

System.err.println("Call Ibis.ibmp_end");
	ibmp_end();

	myIbis.unlock();
    }


}
