package ibis.ipl.impl.messagePassing;

import java.util.Vector;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import ibis.ipl.IbisIOException;

class SerializeShadowSendPort extends ShadowSendPort {

    java.io.ObjectInput obj_in;


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(String type,
			    String name,
			    String ibisId,
			    int send_cpu,
			    int send_port,
			    int rcve_port)
	    throws IbisIOException {
	super(type, name, ibisId, send_cpu, send_port, rcve_port);
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
		System.err.println("Create a -sun- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;

	return msg;
    }


    boolean checkStarted(ibis.ipl.impl.messagePassing.ReadMessage msg)
	    throws IbisIOException {

	if (obj_in != null) {
	    return true;
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
		throw new IbisIOException(e);
	    }
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("ShadowSendPort " + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message that contains the ObjectStream init stuff");
	}
	msg.clear();

	return false;
    }

}
