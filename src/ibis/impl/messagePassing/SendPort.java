package ibis.impl.messagePassing;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.Replacer;
import ibis.util.ConditionVariable;

import java.io.IOException;

public class SendPort implements ibis.ipl.SendPort, Protocol {

    private final static boolean DEBUG = Ibis.DEBUG;

    PortType type;
    SendPortIdentifier ident;
    Replacer replacer;

    ReceivePortIdentifier[] splitter;
    Syncer[] syncer;

    String name;

    boolean aMessageIsAlive = false;
    ConditionVariable portIsFree;
    int newMessageWaiters;
    int messageCount;

    /*
     * If one of the connections is a Home connection, do some polls
     * after our send to see to it that the receive side doesn't have
     * to await a time slice.
     */
    private boolean homeConnection;
    final private static int homeConnectionPolls = 4;

    WriteMessage message = null;

    OutputConnection outConn;

    ByteOutputStream out;


    SendPort() {
    }

    public SendPort(PortType type,
		    String name,
		    OutputConnection conn,
		    Replacer r,
		    boolean syncMode,
		    boolean makeCopy)
	    throws IOException {
	this.name = name;
	this.type = type;
	this.replacer = r;
	ident = new SendPortIdentifier(name, type.name());
	portIsFree = Ibis.myIbis.createCV();
	outConn = conn;
	out = new ByteOutputStream(this, syncMode, makeCopy);
    }

    public SendPort(PortType type, String name, OutputConnection conn)
	    throws IOException {
	this(type, name, conn, null, true, false);
    }


    int addConnection(ReceivePortIdentifier rid) throws IOException {

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

	return my_split;
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			long timeout)
	    throws IOException {

	Ibis.myIbis.lock();
	try {
	    ReceivePortIdentifier rid = (ReceivePortIdentifier)receiver;
// System.out.println("Connecting to " + rid);

	    // Add the new receiver to our tables.
	    int my_split = addConnection(rid);

	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Now do native connect call to " + rid + "; me = " + ident);
	    }
	    IbisIdentifier ibisId = (IbisIdentifier)Ibis.myIbis.identifier();
	    outConn.ibmp_connect(rid.cpu,
				 rid.getSerialForm(),
				 ident.getSerialForm(),
				 syncer[my_split]);
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + "Done native connect call to " + rid + "; me = " + ident);
	    }

	    if (! syncer[my_split].s_wait(timeout)) {
		throw new ConnectionTimedOutException("No connection to " + rid);
	    }
	    if (! syncer[my_split].accepted) {
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
	Ibis.myIbis.unlock();

	ibis.ipl.WriteMessage m = cachedMessage();
	if (DEBUG) {
	    System.err.println("Create a new writeMessage SendPort " + this + " serializationType " + type.serializationType + " message " + m);
	}
	m.resetCount();

	return m;
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

    public ibis.ipl.DynamicProperties properties() {
	return null;
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


    public void free() throws IOException {
	if (DEBUG) {
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free " + this + " start");
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
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free " + this + " DONE");
	}
    }

}
