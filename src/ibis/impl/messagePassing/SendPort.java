package ibis.impl.messagePassing;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.Replacer;
import ibis.ipl.DynamicProperties;
import ibis.util.ConditionVariable;
import ibis.util.TypedProperties;

import java.io.IOException;

public class SendPort implements ibis.ipl.SendPort {

    private final static boolean DEBUG = /* true || */ Ibis.DEBUG;

    private final static boolean BCAST_VERBOSE = /* true || */ Ibis.DEBUG;

    private final static boolean DEFAULT_USE_BCAST = false;
    private final static boolean USE_BCAST =
		TypedProperties.stringProperty("ibis.mp.broadcast", "native");
    private final static boolean USE_BCAST_ALL =
		TypedProperties.booleanProperty("ibis.mp.broadcast.all");
    private final static boolean USE_BCAST_AT_TWO =
		TypedProperties.booleanProperty("ibis.mp.broadcast.2");

    static {
	if (USE_BCAST && Ibis.myIbis.myCpu == 0) {
	    System.err.println("Use native MessagePassing broadcast");
	}
    }

    private final static boolean DEFAULT_SERIALIZE_SENDS = false;
    private final static boolean SERIALIZE_SENDS_PER_CPU;
    static {
	SERIALIZE_SENDS_PER_CPU =
		TypedProperties.booleanProperty("ibis.mp.serialize-sends");
    }

    protected PortType type;
    protected SendPortIdentifier ident;

    protected ReceivePortIdentifier[] splitter;

    private int[] connectedCpu;

    protected static final int NO_BCAST_GROUP = -1;
    protected int group = NO_BCAST_GROUP;

    protected Syncer[] syncer;

    private String name;

    protected boolean aMessageIsAlive = false;
    protected int messageCount;
    private ConditionVariable portIsFree;
    private int newMessageWaiters;

    /*
     * If one of the connections is a Home connection, do some polls
     * after our send to see to it that the receive side doesn't have
     * to await a time slice.
     */
    private boolean homeConnection;
    final private static int homeConnectionPolls = 4;

    protected WriteMessage message = null;

    protected OutputConnection outConn;

    protected long count;

    ByteOutputStream out;


    SendPort() {
    }

    public SendPort(PortType type,
		    String name,
		    OutputConnection conn,
		    boolean syncMode,
		    boolean makeCopy)
	    throws IOException {
	this.name = name;
	this.type = type;
	ident = new SendPortIdentifier(name, type.name());
	portIsFree = Ibis.myIbis.createCV();
	outConn = conn;
	out = new ByteOutputStream(this, syncMode, makeCopy);
	count = 0;
    }

    public void setReplacer(Replacer r) {
    }

    public SendPort(PortType type, String name, OutputConnection conn)
	    throws IOException {
	this(type, name, conn, true, false);
    }


    protected int addConnection(ReceivePortIdentifier rid) throws IOException {

	int	my_split;
	if (splitter == null) {
	    my_split = 0;
	} else {
	    my_split = splitter.length;
	}

	if (rid.cpu < 0) {
	    throw new IllegalArgumentException("invalid ReceivePortIdentifier");
	}

	Ibis.myIbis.checkLockNotOwned();

	if (DEBUG) {
	    System.out.println(name + " connecting to " + rid);
	}

	if (!type.name().equals(rid.type())) {
	    throw new PortMismatchException("Cannot connect ports of different PortTypes: " + type.name() + " vs. " + rid.type());
	}

	int n;

	if (splitter == null) {
	    n = 0;
	} else {
	    n = splitter.length;
	}

	ReceivePortIdentifier[] v = new ReceivePortIdentifier[n + 1];
	for (int i = 0; i < n; i++) {
	    if (splitter[i].cpu == rid.cpu && splitter[i].port == rid.port) {
		throw new Error("Double connection between two ports not allowed");
	    }
	    v[i] = splitter[i];
	}
	v[n] = rid;
	splitter = v;

	Syncer[] s = new Syncer[n + 1];
	for (int i = 0; i < n; i++) {
	    s[i] = syncer[i];
	}
	s[n] = new Syncer();
	syncer = s;

	if (connectedCpu == null) {
	    connectedCpu = new int[1];
	    connectedCpu[0] = rid.cpu;
	} else {
	    boolean isDouble = false;
	    for (int i = 0; i < connectedCpu.length; i++) {
		if (connectedCpu[i] == rid.cpu) {
		    isDouble = true;
		    break;
		}
	    }
	    if (! isDouble) {
		int[] c = new int[connectedCpu.length + 1];
		for (int i = 0; i < connectedCpu.length; i++) {
		    c[i] = connectedCpu[i];
		}
		c[connectedCpu.length] = rid.cpu;
		connectedCpu = c;
	    }
	}

	return my_split;
    }


    private native void requestGroupID(Syncer syncer);

    /**
     * {@inheritDoc}
     */
    public long getCount() {
	return count;
    }

    /**
     * {@inheritDoc}
     */
    public void resetCount() {
	count = 0;
    }


    protected void checkBcastGroup() throws IOException {
	if (! USE_BCAST
		|| (USE_BCAST_ALL ?
			splitter.length != Ibis.myIbis.nrCpus :
			splitter.length != Ibis.myIbis.nrCpus - 1)
		|| (! USE_BCAST_AT_TWO && splitter.length == 1)) {
	    group = NO_BCAST_GROUP;
	    return;
	}

	for (int i = 0, n = splitter.length; i < n; i++) {
	    ReceivePortIdentifier ri = (ReceivePortIdentifier)splitter[i];
	    for (int j = 0; j < i; j++) {
		ReceivePortIdentifier rj = (ReceivePortIdentifier)splitter[j];
		if (ri.cpu == rj.cpu) {
		    group = NO_BCAST_GROUP;
		    return;
		}
	    }
	    if (ri.cpu == Ibis.myIbis.myCpu) {
		if (Ibis.myIbis.myCpu == 0) {
		    System.err.println("Do something special for a group with a home connection -- currently disabled");
		}
		if (! USE_BCAST_ALL) {
		    group = NO_BCAST_GROUP;
		    return;
		}
	    }
	}

	// Apply for a bcast group id with the group id server
	Syncer s = new Syncer();
	requestGroupID(s);
	if (! s.s_wait(0)) {
	    throw new ConnectionRefusedException("No connection to group ID server");
	}
	if (! s.accepted()) {
	    throw new ConnectionRefusedException("No connection to group ID server");
	}
	if (group == NO_BCAST_GROUP) {
	    throw new IOException("Retrieval of group ID failed");
	}
	if (BCAST_VERBOSE) {
	    System.err.println(ident + ": have broadcast group " + group + " receiver(s) ");
	    for (int i = 0, n = splitter.length; i < n; i++) {
		System.err.println("    " + (ReceivePortIdentifier)splitter[i]);
	    }
	}
    }


    private native void sendBindGroupRequest(int to, byte[] senderId, int group)
	    throws IOException;


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			long timeout)
	    throws IOException {

	Ibis.myIbis.lock();
	try {
	    ReceivePortIdentifier rid = (ReceivePortIdentifier)receiver;

	    // Add the new receiver to our tables.
	    int my_split = addConnection(rid);

	    int oldGroup = group;

	    checkBcastGroup();

	    if (group != NO_BCAST_GROUP && oldGroup == NO_BCAST_GROUP) {
		/* The extant connections are not aware that this is now
		 * a broadcast group. Notify them. */
		for (int i = 0, n = splitter.length; i < n; i++) {
		    ReceivePortIdentifier ri = (ReceivePortIdentifier)splitter[i];
		    if (! ri.equals(rid)) {
			sendBindGroupRequest(ri.cpu, ident.getSerialForm(), group);
		    }
		}
	    }

	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Now do native connect call to " + rid + "; me = " + ident);
	    }
	    IbisIdentifier ibisId = (IbisIdentifier)Ibis.myIbis.identifier();
	    outConn.ibmp_connect(rid.cpu,
				 rid.getSerialForm(),
				 ident.getSerialForm(),
				 syncer[my_split],
				 group);
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Done native connect call to " + rid + "; me = " + ident);
	    }

	    if (! syncer[my_split].s_wait(timeout)) {
		throw new ConnectionTimedOutException("No connection to " + rid);
	    }
	    if (! syncer[my_split].accepted()) {
		throw new ConnectionRefusedException("No connection to " + rid);
	    }

	    if (ident.ibis().equals(receiver.ibis())) {
		homeConnection = true;
// System.err.println("This IS a home connection, my Ibis " + ident.ibis() + " their Ibis " + receiver.ibis());
	    } else {
// System.err.println("This is NOT a home connection, my Ibis " + ident.ibis() + " their Ibis " + receiver.ibis());
// Thread.dumpStack();
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver)
	    throws IOException {
	connect(receiver, 0);
    }


    ibis.ipl.WriteMessage cachedMessage() throws IOException {
	if (message == null) {
	    message = new WriteMessage(this);
	}

	return message;
    }


    public ibis.ipl.WriteMessage newMessage() throws IOException {

	Ibis.myIbis.lock();
	while (aMessageIsAlive) {
	    newMessageWaiters++;
	    try {
		portIsFree.cv_wait();
	    } catch (InterruptedException e) {
		// ignore
	    }
	    newMessageWaiters--;
	}

	aMessageIsAlive = true;

	if (SERIALIZE_SENDS_PER_CPU) {
	    Ibis.myIbis.sendSerializer.lockAll(connectedCpu);
	}

	Ibis.myIbis.unlock();

	ibis.ipl.WriteMessage m = cachedMessage();
	if (DEBUG) {
	    System.err.println("Create a new writeMessage SendPort " + this + " serializationType " + type.serializationType + " message " + m);
	}

	return m;
    }


    void finishMessage() {
	Ibis.myIbis.checkLockOwned();
	if (SERIALIZE_SENDS_PER_CPU) {
	    Ibis.myIbis.sendSerializer.unlockAll(connectedCpu);
	}
    }


    void registerSend() throws IOException {
	messageCount++;
	if (homeConnection) {
	    for (int i = 0; i < homeConnectionPolls; i++) {
		while (Ibis.myIbis.pollLocked());
	    }
	}
    }


    void reset() {
	Ibis.myIbis.checkLockOwned();
	aMessageIsAlive = false;
	if (newMessageWaiters > 0) {
	    portIsFree.cv_signal();
	}
    }


    public DynamicProperties properties() {
	return DynamicProperties.NoDynamicProperties;
    }


    public String name() {
	return name;
    }


    public ibis.ipl.SendPortIdentifier identifier() {
	return ident;
    }


    public ibis.ipl.ReceivePortIdentifier[] connectedTo() {
	ibis.ipl.ReceivePortIdentifier[] r = new ibis.ipl.ReceivePortIdentifier[splitter.length];
	for (int i = 0; i < splitter.length; i++) {
	    r[i] = splitter[i];
	}
	return r;
    }


    public ibis.ipl.ReceivePortIdentifier[] lostConnections() {
	return null;	/* Or should this be an empty array or? */
    }


    public void disconnect(ibis.ipl.ReceivePortIdentifier r)
	    throws IOException
    {
	if (splitter == null) {
	    throw new IOException("disconnect: no connections");
	}
	Ibis.myIbis.lock();
	try {
	    int n = splitter.length;
	    boolean found = false;
	    for (int i = 0; i < n; i++) {
		ReceivePortIdentifier rid = splitter[i];
		if (rid.equals(r)) {
		    byte[] sf = ident.getSerialForm();
		    outConn.ibmp_disconnect(rid.cpu, rid.getSerialForm(), sf, messageCount);
		    ReceivePortIdentifier[] v = new ReceivePortIdentifier[n - 1];
		    for (int j = 0; j < n - 1; j++) {
			v[j] = splitter[j];
		    }
		    if (i < n-1) {
			v[i] = splitter[n-1];
		    }
		    splitter = v;
		    found = true;
		    break;
		}
	    }
	    if (! found) {
		throw new IOException("disconnect: no connection to " + r);
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void close() throws IOException {
	if (DEBUG) {
	    System.out.println(Ibis.myIbis.name() + ": ibis.ipl.SendPort.free " + this + " start");
	}

	if (splitter == null) {
	    // Seems we were created but never connected to anybody
	    return;
	}

	Ibis.myIbis.lock();
	try {
	    byte[] sf = ident.getSerialForm();
	    for (int i = 0; i < splitter.length; i++) {
		ReceivePortIdentifier rid = splitter[i];
		outConn.ibmp_disconnect(rid.cpu, rid.getSerialForm(), sf, messageCount);
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}

	if (DEBUG) {
	    System.out.println(Ibis.myIbis.name() + ": ibis.ipl.SendPort.free " + this + " DONE");
	}
    }

}
