package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.util.Hashtable;

import ibis.ipl.impl.generic.ConditionVariable;
import ibis.ipl.impl.generic.Monitor;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

public abstract class Ibis extends ibis.ipl.Ibis {

    public static final boolean DEBUG = false;

    private IbisIdentifier ident;

    protected Registry registry;

    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean started = false;
    private Vector ibisNameService = new Vector();

    private final StaticProperties systemProperties = new StaticProperties();

    public static Ibis	myIbis;

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

    protected abstract void ibmp_init();
    protected abstract void ibmp_start();
    protected abstract void ibmp_end();

    long tMsgPoll;
    long tSend;
    long tReceive;
    long tMsgSend;

    public Ibis() throws IbisException {

	// Set my properties.
	systemProperties.add("reliability", "true");
	systemProperties.add("multicast", "false");
	systemProperties.add("totally ordered", "false");
	systemProperties.add("open world", "false");
	if (DEBUG) {
	    System.err.println("Turn on Ibis.DEBUG");
	}
    }

    public ibis.ipl.PortType createPortType(String name,
				   StaticProperties p) throws IbisException {

	return new PortType(this, name, p);
    }

    public ibis.ipl.Registry registry() {
	return registry;
    }


    protected abstract ReceivePortNameServer createReceivePortNameServer() throws IbisIOException;
    protected abstract ReceivePortNameServerClient createReceivePortNameServerClient();

    protected abstract boolean getInputStreamMsg(int tags[]);

    public StaticProperties properties() {
	return systemProperties;
    }

    public ibis.ipl.IbisIdentifier identifier() {
	return ident;
    }


    protected abstract void send_join(int to, String ident_name);
    protected abstract void send_leave(int to, String ident_name);

    /* Called from native */
    void join_upcall(String ident_name, int cpu) {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Receive join message " + ident_name + "; now world = " + world);
	}
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
//manta.runtime.RuntimeSystem.DebugMe(ibisNameService, world);
	IbisIdentifier id = new IbisIdentifier(ident_name, cpu);
	ibisNameService.add(id);
	world.join(cpu, id);
    }

    /* Called from native */
    void leave_upcall(String ident_name, int cpu) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	IbisIdentifier id = new IbisIdentifier(ident_name, cpu);
	ibisNameService.remove(id);
	world.leave(cpu, id);
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

	System.loadLibrary("ibis_panda");

	rcve_poll = createPoll();

	registry = new Registry();

	    /* Fills in:
		nrCpus;
		myCpu;
	     */
	ibmp_init();
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

	// synchronized (myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (DEBUG) {
		System.err.println("myCpu " + myCpu + " nrCpus " + nrCpus + " world " + world);
	    }
	    for (int i = 0; i < nrCpus; i++) {
		if (i != myCpu) {
		    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
			System.err.println("Send join message to " + i);
		    }
		    send_join(i, ident.name());
		}
	    }

	    ibisNameService.add(ident);
	    world.join(myCpu, ident);
	// }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Ibis lives...");
	}
    }


    public void openWorld() {
	// synchronized (myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    world.open();
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }

    public void closeWorld() {
	// synchronized (myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    world.close();
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }


    Poll rcve_poll;

    protected abstract Poll createPoll();

    final void waitPolling(PollClient client, long timeout, boolean preempt)
	    throws IbisIOException {
	rcve_poll.waitPolling(client, timeout, preempt);
    }

    protected abstract SendPort createSendPort(PortType type) throws IbisIOException;
    protected abstract SendPort createSendPort(PortType type, String name) throws IbisIOException;

    protected abstract long currentTime();
    protected abstract double t2d(long t);



    final public void lock() {
	monitor.lock();
    }

    final public void unlock() {
	monitor.unlock();
    }

    final public void checkLockOwned() {
	monitor.checkImOwner();
    }

    final public void checkLockNotOwned() {
	monitor.checkImNotOwner();
    }


    public IbisIdentifier lookupIbis(String name) {
// System.err.println("Want to look up IbisId " + name);
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis.ident.name().equals(name)) {
	    return ibis.ipl.impl.messagePassing.Ibis.myIbis.ident;
	}

	for (int i = 0; i < ibisNameService.size(); i++) {
	    IbisIdentifier id = (IbisIdentifier)ibisNameService.get(i);
	    if (id.name().equals(name)) {
// System.err.println("Found IbisId " + name);
		return id;
	    }
	}

// System.err.println("Not found IbisId " + name);
	return null;
    }

    public ibis.ipl.PortType getPortType(String name) {
	return (ibis.ipl.PortType)portTypeList.get(name);
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

    void unbindSendPort(ShadowSendPort p) {
	// checkLockOwned();
	sendPorts[p.ident.cpu].unbind(p.ident.port);
    }

    ReceivePort lookupReceivePort(int port) {
	// checkLockOwned();
	return (ReceivePort)rcvePorts.lookup(port);
    }

    int[] inputStreamMsgTags = new int[6];

    final void inputStreamPoll() throws IbisIOException {
	if (getInputStreamMsg(inputStreamMsgTags)) {
	    receiveFragment(inputStreamMsgTags[0],
			    inputStreamMsgTags[1],
			    inputStreamMsgTags[2],
			    inputStreamMsgTags[3],
			    inputStreamMsgTags[4],
			    inputStreamMsgTags[5]);
	}
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
	ibis.ipl.impl.messagePassing.ReceivePort port = lookupReceivePort(dest_port);
// System.err.println(Thread.currentThread() + "receiveFragment port " + port);
	ShadowSendPort origin = lookupSendPort(src_cpu, src_port);
// System.err.println(Thread.currentThread() + "receiveFragment origin " + origin);

	if (origin == null) {
	    throw new IbisIOException("Receive message from sendport we're not connected to");
	}

	port.receiveFragment(origin, msgHandle, msgSize, msgSeqno);
    }


    protected abstract ByteInputStream createByteInputStream();
    protected abstract ByteOutputStream createByteOutputStream(SendPort port,
							       boolean syncMode,
							       boolean makeCopy);

    protected abstract ibis.io.ArrayInputStream createMantaInputStream(ByteInputStream byte_in);
    protected abstract ibis.io.MantaOutputStream createMantaOutputStream(ByteOutputStream byte_out);


    public void resetStats() {
	rcve_poll.reset_stats();
    }

    public void end() {
	// synchronized (myIbis) {
	registry.end();
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    ibisNameService.remove(ident);
	    for (int i = 0; i < nrCpus; i++) {
		if (i != myCpu) {
    // System.err.println("Send join message to " + i);
		    send_leave(i, ident.name());
		}
	    }
	    world.leave(myCpu, ident);

	    System.err.println("t native poll " + t2d(tMsgPoll) + " send " + t2d(tMsgSend));
	    System.err.println("t java   send " + t2d(tSend) + " rcve " + t2d(tReceive));
	    ConditionVariable.report(System.out);
	    rcve_poll.finalize();
	    ibmp_end();
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }

}
