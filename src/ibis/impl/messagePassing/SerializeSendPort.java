package ibis.impl.messagePassing;

import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Replacer;

import ibis.util.ConditionVariable;

import java.io.BufferedOutputStream;
import java.io.IOException;

final public class SerializeSendPort extends SendPort {

    private final boolean DEBUG = SendPort.DEBUG;

    ibis.io.SunSerializationOutputStream obj_out;

    private Replacer replacer;

    private ConnectAcker connectAcker = new ConnectAcker();

    private int		connectWaiters = 0;
    private boolean	connecting = false;
    private ConditionVariable	connectFinished = Ibis.myIbis.createCV();


    SerializeSendPort() {
    }

    public SerializeSendPort(PortType type, String name)
	    throws IOException {
	super(type,
	      name,
	      true,	/* syncMode */
	      true	/* makeCopy */);
	if (DEBUG) {
	    System.err.println("/////////// Created a new SerializeSendPort " + this);
	}
    }


    public void connect(ibis.ipl.ReceivePortIdentifier receiver,
			long timeout)
	    throws IOException {

	if (aMessageIsAlive) {
	    throw new IOException("First finish extant WriteMessage");
	}

	Ibis.myIbis.lock();
	while (connecting) {
	    connectWaiters++;
	    try {
		connectFinished.cv_wait();
	    } catch (InterruptedException e) {
		// Ignore
	    }
	    connectWaiters--;
	}
	connecting = true;

	try {

	    if (splitter != null && splitter.length > 0) {

		Ibis.myIbis.unlock();
		// Reset all our previous connections so the
		// ObjectStream(BufferedStream()) may go through a stop/restart.
		if (obj_out != null) {
		    obj_out.reset();
		    // obj_out.flush();
		}
		// if (message != null) {
		    // message.reset(true);
		// }
		Ibis.myIbis.lock();

		connectAcker.setAcks(splitter.length);

		byte[] sf = ident.getSerialForm();
		for (int i = 0; i < splitter.length; i++) {
		    ReceivePortIdentifier r = splitter[i];
		    if (DEBUG) {
			System.err.println(Thread.currentThread() + "Now do native DISconnect call to " + r + "; me = " + ident);
		    }
		    ibmp_disconnect(r.cpu, r.getSerialForm(), sf,
				    connectAcker, messageCount);
		}

		Ibis.myIbis.waitPolling(connectAcker, 0, Poll.PREEMPTIVE);
	    }

	    // Add the new receiver to our tables.
	    int my_split = addConnection((ReceivePortIdentifier)receiver);

	    checkBcastGroup();

	    if (DEBUG) {
		System.err.println(this + ": have bcast group " + group);
	    }

	    connectAcker.setAcks(splitter.length);

	    for (int i = 0; i < splitter.length; i++) {
		ReceivePortIdentifier r = splitter[i];
		if (DEBUG) {
		    System.err.println(Thread.currentThread() + "Now do native connect call to " + r + "; me = " + ident + " group " + group + " seqno " + out.getMsgSeqno());
		    // System.err.println("Ibis.myIbis " + Ibis.myIbis);
		    // System.err.println("Ibis.myIbis.identifier() " + Ibis.myIbis.identifier());
		    // System.err.println("Ibis.myIbis.identifier().name() " + Ibis.myIbis.identifier().name());
		}
		ibmp_connect(r.cpu, r.getSerialForm(), ident.getSerialForm(),
			     null, // syncer[i],
			     connectAcker,
			     messageCount,
			     group, out.getMsgSeqno());
		if (DEBUG) {
		    System.err.println(Thread.currentThread() + "Done native connect call to " + r + "; me = " + ident);
		}
	    }

	    if (ident.ibis().equals(receiver.ibis())) {
		homeConnection = true;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}

	obj_out = new ibis.io.SunSerializationOutputStream(new BufferedOutputStream(new OutputStream(out)));
	if (replacer != null) {
	    obj_out.setReplacer(replacer);
	}
	if (message != null) {
	    ((SerializeWriteMessage)message).obj_out = obj_out;
	}
	obj_out.flush();

	Ibis.myIbis.lock();
	try {
	    out.send(true);
	    out.reset(true);
	    registerSend();
	    Ibis.myIbis.waitPolling(connectAcker, 0, Poll.PREEMPTIVE);
	} finally {
	    if (connectWaiters > 0) {
		connectFinished.cv_signal();
	    }
	    connecting = false;
	    Ibis.myIbis.unlock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + ">>>>>>>>>>>> Created ObjectOutputStream " + obj_out + " on top of " + out);
	}
    }

    public void setReplacer(Replacer r) {
	replacer = r;
	if (obj_out != null) obj_out.setReplacer(replacer);
    }

    ibis.ipl.WriteMessage cachedMessage() throws IOException {
	if (message == null) {
	    message = new SerializeWriteMessage(this);
	}

	return message;
    }

}
