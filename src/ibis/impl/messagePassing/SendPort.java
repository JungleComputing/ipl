package ibis.ipl.impl.messagePassing;

import java.util.Vector;

import ibis.ipl.IbisIOException;
import ibis.ipl.ConditionVariable;

public class SendPort implements ibis.ipl.SendPort, Protocol {

    ibis.ipl.impl.messagePassing.PortType type;
    SendPortIdentifier ident;

    ReceivePortIdentifier[] splitter;
    Syncer[] syncer;

    String name;

    boolean aMessageIsAlive = false;
    ConditionVariable portIsFree;
    int newMessageWaiters;
    int messageCount;

    ibis.ipl.impl.messagePassing.WriteMessage message = null;

    OutputConnection outConn;

    ibis.ipl.impl.messagePassing.ByteOutputStream out;


    SendPort() {
    }

    public SendPort(ibis.ipl.impl.messagePassing.PortType type,
		    String name,
		    OutputConnection conn,
		    boolean syncMode,
		    boolean makeCopy)
	    throws IbisIOException {
	this.name = name;
	this.type = type;
	ident = new SendPortIdentifier(name, type.name());
	portIsFree = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
	outConn = conn;
	out = ibis.ipl.impl.messagePassing.Ibis.myIbis.createByteOutputStream(this, syncMode, makeCopy);
    }

    public SendPort(ibis.ipl.impl.messagePassing.PortType type, String name, OutputConnection conn) throws IbisIOException {
	this(type, name, conn, true, false);
    }


    int addConnection(ReceivePortIdentifier rid)
	    throws IbisIOException {

	int	my_split;
	if (splitter == null) {
	    my_split = 0;
	} else {
	    my_split = splitter.length;
	}

	if (rid.cpu < 0) {
	    throw new IbisIOException("invalid ReceivePortIdentifier");
	}

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(name + " connecting to " + rid);
	}

	if (!type.name().equals(rid.type())) {
	    throw new IbisIOException("Cannot connect ports of different PortTypes: " + type.name() + " vs. " + rid.type());
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
			int timeout)
	    throws IbisIOException {

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    ReceivePortIdentifier rid = (ReceivePortIdentifier)receiver;

	    // Add the new receiver to our tables.
	    int my_split = addConnection(rid);

	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + "Now do native connect call to " + rid + "; me = " + ident);
	    }
	    outConn.ibmp_connect(rid.cpu, rid.port, ident.port, ident.type,
				 ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name(),
				 syncer[my_split], type.serializationType);
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + "Done native connect call to " + rid + "; me = " + ident);
	    }

	    if (! syncer[my_split].s_wait(timeout)) {
		throw new ibis.ipl.IbisConnectionTimedOutException("No connection to " + rid);
	    }
	    if (! syncer[my_split].accepted) {
		throw new ibis.ipl.IbisConnectionRefusedException("No connection to " + rid);
	    }
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver) throws IbisIOException {
	connect(receiver, 0);
    }


    ibis.ipl.WriteMessage cachedMessage() throws IbisIOException {
	if (message == null) {
	    message = new WriteMessage(this);
	}

	return message;
    }


    public ibis.ipl.WriteMessage newMessage() throws IbisIOException {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    while (aMessageIsAlive) {
		newMessageWaiters++;
		portIsFree.cv_wait();
		newMessageWaiters--;
	    }

	    aMessageIsAlive = true;
	}

	ibis.ipl.WriteMessage m = cachedMessage();
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Create a new writeMessage SendPort " + this + " serializationType " + type.serializationType + " message " + m);
	}

	return m;
    }


    void registerSend() {
	messageCount++;
    }


    void reset() throws IbisIOException {
	// Should already be taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	aMessageIsAlive = false;
	if (newMessageWaiters > 0) {
	    portIsFree.cv_signal();
	}
    }

    public ibis.ipl.DynamicProperties properties() {
	return null;
    }

    public ibis.ipl.SendPortIdentifier identifier() {
	return ident;
    }


    public void free() throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free start");
	}

	for (int i = 0; i < splitter.length; i++) {
	    ReceivePortIdentifier rid = splitter[i];
	    outConn.ibmp_disconnect(rid.cpu, rid.port, ident.port, messageCount);
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free DONE");
	}
    }

}
