package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.util.Hashtable;

import java.io.IOException;

import ibis.ipl.ConditionVariable;
import ibis.ipl.IbisException;
import ibis.ipl.StaticProperties;

public abstract class Ibis extends ibis.ipl.Ibis {

    static final boolean DEBUG = false;

    private IbisIdentifier ident;

    protected Registry pandaRegistry;

    private int poolSize;

    private Hashtable portTypeList = new Hashtable();

    private boolean started = false;
    private Vector joinedIbises = new Vector();
    private Vector ibisNameService = new Vector();

    private final StaticProperties systemProperties = new StaticProperties();

    static Ibis	myIbis;

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

    long tPandaPoll;
    long tSend;
    long tReceive;
    long tPandaSend;

    public Ibis() throws IbisException {

	// Set my properties.
	systemProperties.add("reliability", "true");
	systemProperties.add("multicast", "false");
	systemProperties.add("totally ordered", "false");
	systemProperties.add("open world", "false");
    }

    public ibis.ipl.PortType createPortType(String name,
				   StaticProperties p) throws IbisException {

	return new PortType(this, name, p);
    }

    public ibis.ipl.Registry registry() {
	return pandaRegistry;
    }


    protected abstract ReceivePortNameServer createReceivePortNameServer() throws IOException;
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
System.err.println("Receive join message " + ident_name + "; now joinedIbises.size() = " + (joinedIbises == null ? -1 : joinedIbises.size()));
	synchronized (myIbis) {
//manta.runtime.RuntimeSystem.DebugMe(ibisNameService, world);
	    IbisIdentifier id = new IbisIdentifier(ident_name, cpu);
	    ibisNameService.add(id);
	    world.join(cpu, id);
	}
    }

    /* Called from native */
    void leave_upcall(String ident_name, int cpu) {
	synchronized (myIbis) {
	    IbisIdentifier id = new IbisIdentifier(ident_name, cpu);
	    ibisNameService.remove(id);
	    world.leave(cpu, id);
	}
    }


    void join(IbisIdentifier id) {
if (DEBUG) System.err.println("An Ibis.join call for " + id);
	if (resizeHandler != null) {
	    resizeHandler.join(id);
	}
    }


    void leave(IbisIdentifier id) {
	if (resizeHandler != null) {
	    resizeHandler.leave(id);
	}
    }


    protected void init() throws IbisException {

	if (myIbis != null) {
	    throw new IbisException("Only one Ibis allowed");
	} else {
	    myIbis = this;
	}

	System.loadLibrary("ibis_panda");

	try {
	    pandaRegistry = new Registry();
	} catch (java.io.IOException e) {
	    throw new IbisException("Cannot create registry: " + e);
	}

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

	pandaRegistry.init();
	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Registry lives...");
	}

	synchronized (myIbis) {
	    System.err.println("myCpu " + myCpu + " nrCpus " + nrCpus + " joinedIbises.size() " + joinedIbises.size());
	    for (int i = 0; i < nrCpus; i++) {
		if (i != myCpu) {
System.err.println("Send join message to " + i);
		    send_join(i, ident.name());
		}
	    }

	    ibisNameService.add(ident);
	    world.join(myCpu, ident);
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + "Ibis lives...");
	}
    }


    public void openWorld() {
	synchronized (myIbis) {
	    world.open();
	}
    }

    public void closeWorld() {
	synchronized (myIbis) {
	    world.close();
	}
    }


    Poll rcve_poll = createPoll();

    protected abstract Poll createPoll();

    void waitPolling(PollClient client, long timeout, boolean preempt)
	    throws IbisException {
	rcve_poll.waitPolling(client, timeout, preempt);
    }

    protected abstract SendPort createSendPort(PortType type) throws IbisException;
    protected abstract SendPort createSendPort(PortType type, String name) throws IbisException;

    native void lock();
    native void unlock();
    protected abstract long currentTime();
    protected abstract double t2d(long t);

    void checkLockOwned() {
	if (! DEBUG) {
	    return;
	}
	try {
	    notify();
	} catch (IllegalMonitorStateException e) {
	    System.err.println("Ibis.ibis not locked");
	    Thread.dumpStack();
	    System.exit(97);
	}
    }

    void checkLockNotOwned() {
	if (! DEBUG) {
	    return;
	}
	try {
	    notify();
	    System.err.println("Ibis.ibis locked");
	    Thread.dumpStack();
	    System.exit(98);
	} catch (IllegalMonitorStateException e) {
	}
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

    int[] inputStreamMsgTags = new int[4];

    void inputStreamPoll() throws IbisException {
	if (getInputStreamMsg(inputStreamMsgTags)) {
	    createReadMessage(inputStreamMsgTags[0],
			      inputStreamMsgTags[1],
			      inputStreamMsgTags[2],
			      inputStreamMsgTags[3]);
	}
    }

    ibis.ipl.impl.messagePassing.ReadMessage createReadMessage(
				int src_cpu,
				int src_port,
				int dest_port,
				int msg) throws IbisException {
	// This is already taken: synchronized (Ibis.ibis)
	// checkLockOwned();
// System.err.println(Thread.currentThread() + "createReadMessage");
	ReceivePort port = lookupReceivePort(dest_port);
// System.err.println(Thread.currentThread() + "createReadMessage port " + port);
	ShadowSendPort origin = lookupSendPort(src_cpu, src_port);
// System.err.println(Thread.currentThread() + "createReadMessage origin " + origin);

	if (origin == null) {
	    throw new IbisException("They want to lookup sendPort cpu " + src_cpu + " port " + src_port + " which apparently has already been cleared");
	}

	return port.createReadMessage(origin, msg);
    }


    protected abstract ByteInputStream createByteInputStream();
    protected abstract ByteOutputStream createByteOutputStream(SendPort port);

    protected abstract ibis.io.ArrayInputStream createMantaInputStream(ByteInputStream byte_in);
    protected abstract ibis.io.MantaOutputStream createMantaOutputStream(ByteOutputStream byte_out);


    public void resetStats() {
	rcve_poll.reset_stats();
    }

    public synchronized void end() {
	ibisNameService.remove(ident);
	for (int i = 0; i < nrCpus; i++) {
	    if (i != myCpu) {
// System.err.println("Send join message to " + i);
		send_leave(i, ident.name());
	    }
	}
	world.leave(myCpu, ident);

	System.err.println("t native poll " + t2d(tPandaPoll) + " send " + t2d(tPandaSend));
	System.err.println("t java   send " + t2d(tSend) + " rcve " + t2d(tReceive));
	ConditionVariable.report(System.out);
	rcve_poll.finalize();
	ibmp_end();
    }

}
