package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import ibis.ipl.IbisIOException;
import ibis.ipl.impl.generic.ConditionVariable;

final class SerializeShadowSendPort extends ShadowSendPort {

    java.io.ObjectInput obj_in;
    boolean initializing = false;
    ConditionVariable objectStreamOpened = Ibis.myIbis.createCV();


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(String type,
			    String name,
			    String ibisId,
			    int send_cpu,
			    byte[] inetAddr,
			    int send_port,
			    int rcve_port)
	    throws IbisIOException {
	super(type, name, ibisId, send_cpu, inetAddr, send_port, rcve_port);
// System.err.println("In SerializeShadowSendPort.<init>");
    }


    ibis.ipl.impl.messagePassing.ReadMessage getMessage(int msgSeqno)
	    throws IbisIOException {
	ibis.ipl.impl.messagePassing.ReadMessage msg = cachedMessage;

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(this + ": Get a Serialize ReadMessage ");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new SerializeReadMessage(this, receivePort);
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + ": Create a -sun- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;

	return msg;
    }


    boolean checkStarted(ibis.ipl.impl.messagePassing.ReadMessage msg)
	    throws IbisIOException {

	if (initializing) {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + ": Negotiate the ObjectStream init race " + this + " -- BINGO BINGO");
	    }
	    /* Right. We hit a race here. Some thread is reading the
	     * initial message, and has to unlock for that.
	     * We must wait until it's finished. */
	    while (obj_in == null) {
		objectStreamOpened.cv_wait();
	    }
	    if (((SerializeReadMessage)msg).obj_in != null) {
		System.err.println("NNNNNNNNNNNNNNNOOOOOOOOOOOOOOOOOO this cannot be");
	    }
	    ((SerializeReadMessage)msg).obj_in = obj_in;
	}

	if (obj_in != null) {
	    return true;
	}

	initializing = true;
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + ": Lock ShadowSendPort " + this + " to avoid ObjectStream init race");
	}

	/* This is so sick. Panda has a message abstraction, Ibis has a
	 * message abstraction, but ObjectInputStream flattens all.
	 * This _is_ a problem: in the constructor, the ObjectInputStream
	 * starts reading. However, the application may previously have posted
	 * a receive. Therefore we cannot just follow the normal route here
	 * and get a ReadMessage from the Ibis connection -- there may be
	 * a pending ReadMessage that blocks us, and wants to read before us.
	 * So we must circumvent the queue and handle the message specially.
	 */

	in.setMsgHandle(msg);

	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	try {
	    try {
		obj_in = new ObjectInputStream(new BufferedInputStream(in));
	    } catch (java.io.IOException e) {
		if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.err.println("new ObjectInputStream throws an exception " + e);
		    e.printStackTrace(System.err);
		    System.err.println("Reading msg " + msg + " native 0x" + Integer.toHexString(msg.fragmentFront.msgHandle));
		}
		throw new IbisIOException(e);
	    }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + " ShadowSendPort " + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message " + msg + " handle 0x" + Integer.toHexString(msg.fragmentFront.msgHandle) + " that contains the ObjectStream init stuff");
	}
	msg.clear();

	objectStreamOpened.cv_bcast();

	initializing = false;

	return false;
    }

}
