package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.io.IOException;

import ibis.ipl.IbisException;
import ibis.ipl.ConditionVariable;

public abstract class SendPort implements ibis.ipl.SendPort, Protocol {

    ibis.ipl.impl.messagePassing.PortType type;
    SendPortIdentifier ident;

    ReceivePortIdentifier[] splitter;
    Syncer[] syncer;

    String name;

    boolean aMessageIsAlive = false;
    ConditionVariable portIsFree;
    int newMessageWaiters;
    int messageCount;

    ibis.ipl.WriteMessage message = null;

    SendPort() {
    }

    protected SendPort(ibis.ipl.impl.messagePassing.PortType type) throws IbisException {
	this(type, null);
    }

    protected SendPort(ibis.ipl.impl.messagePassing.PortType type, String name) throws IbisException {
	this.name = name;
	this.type = type;
	ident = new SendPortIdentifier(name, type.name());
	portIsFree = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
    }


    void addConnection(ReceivePortIdentifier rid) {
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
    }


    protected abstract void ibmp_connect(int cpu, int port,
				    int my_port, String type, String ibisId,
				    Syncer syncer);

    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			int timeout)
			    throws IbisException {

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    ReceivePortIdentifier rid = (ReceivePortIdentifier)receiver;
	    int	my_split;
	    if (splitter == null) {
		my_split = 0;
	    } else {
		my_split = splitter.length;
	    }

	    if (rid.cpu < 0) {
		throw new IbisException("invalid ReceivePortIdentifier");
	    }

	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.out.println(name + " connecting to " + receiver);
	    }

// manta.runtime.RuntimeSystem.DebugMe(rid, null);
// System.err.println("receiver.type() " + receiver.type());
// System.err.println("rid.type() " + rid.type());
	    /* first check the types */
	    /*
	    if (!type.name().equals(receiver.type())) {
		throw new IbisException("Cannot connect ports of different PortTypes: " + type.name() + " vs. " + receiver.type());
	    }
	    */
// System.err.println("Tell Ronald is broken in interface calling: rid().tyype() is OK, receiver.type() crashes");
// System.err.println(rid.type());
// System.err.println(receiver.type());
	    if (!type.name().equals(rid.type())) {
		throw new IbisException("Cannot connect ports of different PortTypes: " + type.name() + " vs. " + rid.type());
	    }

	    // Add the new receiver to our tables.
	    addConnection(rid);

	    switch (type.serializationType) {
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
// System.err.println(Thread.currentThread() + "Now do native connect call to " + rid + "; me = " + ident);
		ibmp_connect(rid.cpu, rid.port, ident.port, ident.type,
			    ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name(),
			    syncer[my_split]);
// System.err.println(Thread.currentThread() + "Done native connect call to " + rid + "; me = " + ident);

		if (! syncer[my_split].s_wait(timeout)) {
		    throw new ibis.ipl.IbisConnectionTimedOutException("No connection to " + rid);
		}
		if (! syncer[my_split].accepted) {
		    throw new ibis.ipl.IbisConnectionRefusedException("No connection to " + rid);
		}
		break;

	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
		// Reset all our previous connections so the
		// ObjectStream(BufferedStream()) may go through a stop/restart.
		for (int i = 0; i < my_split; i++) {
		    ReceivePortIdentifier r = splitter[i];
		    ibmp_disconnect(r.cpu, r.port, ident.port, messageCount);
		}
		messageCount = 0;

		for (int i = 0; i < splitter.length; i++) {
		    ReceivePortIdentifier r = splitter[i];
// System.err.println(Thread.currentThread() + "Now do native connect call to " + r + "; me = " + ident);
// System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis " + ibis.ipl.impl.messagePassing.Ibis.myIbis);
// System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier() " + ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier());
// System.err.println("ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name() " + ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name());
		    ibmp_connect(r.cpu, r.port, ident.port, ident.type,
				ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier().name(),
				i == my_split ? syncer[i] : null);
// System.err.println(Thread.currentThread() + "Done native connect call to " + r + "; me = " + ident);
		}

		if (! syncer[my_split].s_wait(timeout)) {
		    throw new ibis.ipl.IbisConnectionTimedOutException("No connection to " + rid);
		}
		if (! syncer[my_split].accepted) {
		    throw new ibis.ipl.IbisConnectionRefusedException("No connection to " + rid);
		}

		/* For the ObjectStream messages: we must reset etc the
		 * stream so we get a real fresh connection. Accomplish this
		 * by discarding the cached message: */
		message = null;
		break;

	    default:
		System.out.println("EEK");
		System.exit(1);
	    }
	}
    }

    public void connect(ibis.ipl.ReceivePortIdentifier receiver) throws IbisException {
	connect(receiver, 0);
    }


    private ibis.ipl.WriteMessage cachedMessage() throws IbisException {
	if (message == null) {
	    switch (type.serializationType) {
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_NONE:
		message = new WriteMessage(this);
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_SUN:
		message = new SerializeWriteMessage(this);
		break;
	    case ibis.ipl.impl.messagePassing.PortType.SERIALIZATION_MANTA:
		message = new MantaWriteMessage(this);
		break;
	    default:
		System.out.println("EEK");
		System.exit(1);
	    }
	}

	return message;
    }


    public ibis.ipl.WriteMessage newMessage() throws IbisException {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    while (aMessageIsAlive) {
		newMessageWaiters++;
		portIsFree.cv_wait();
		newMessageWaiters--;
	    }

	    aMessageIsAlive = true;
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Create a new writeMessage SendPort " + this + " serializationType " + type.serializationType + " message " + message);
	}

	return cachedMessage();
    }


    void registerSend() {
	messageCount++;
    }


    void reset() throws IbisException {
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

    protected abstract void ibmp_disconnect(int cpu, int port, int receiver_port, int messageCount);

    public void free() {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free start");
	}

	for (int i = 0; i < splitter.length; i++) {
	    ReceivePortIdentifier rid = splitter[i];
	    ibmp_disconnect(rid.cpu, rid.port, ident.port, messageCount);
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(type.myIbis.name() + ": ibis.ipl.SendPort.free DONE");
	}
    }

}
