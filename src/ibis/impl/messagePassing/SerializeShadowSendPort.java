package ibis.impl.messagePassing;

import java.io.ObjectInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import ibis.util.ConditionVariable;

final class SerializeShadowSendPort extends ShadowSendPort {

    java.io.ObjectInput obj_in;
    boolean initializing = false;
    ConditionVariable objectStreamOpened = Ibis.myIbis.createCV();


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(ReceivePortIdentifier rId, SendPortIdentifier sId)
	    throws IOException {
	super(rId, sId);
// System.err.println("In SerializeShadowSendPort.<init>");
    }


    ReadMessage getMessage(int msgSeqno) throws IOException {
	ReadMessage msg = cachedMessage;

	if (Ibis.DEBUG) {
	    System.err.println(this + ": Get a Serialize ReadMessage ");
	}

	if (msg != null) {
	    cachedMessage = null;

	} else {
	    msg = new SerializeReadMessage(this, receivePort);
	    if (Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + ": Create a -sun- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;

	return msg;
    }


    boolean checkStarted(ReadMessage msg) throws IOException {

	if (initializing) {
	    if (Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + ": Negotiate the ObjectStream init race " + this + " -- BINGO BINGO");
	    }
	    /* Right. We hit a race here. Some thread is reading the
	     * initial message, and has to unlock for that.
	     * We must wait until it's finished. */
	    while (obj_in == null) {
		try {
		    objectStreamOpened.cv_wait();
		} catch (InterruptedException e) {
		    // ignore
		}
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
	if (Ibis.DEBUG) {
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

	Ibis.myIbis.unlock();
	try {
	    obj_in = new ObjectInputStream(new BufferedInputStream(in));
	} finally {
	    Ibis.myIbis.lock();
	}

	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + " ShadowSendPort " + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message " + msg + " handle 0x" + Integer.toHexString(msg.fragmentFront.msgHandle) + " that contains the ObjectStream init stuff");
	}
	msg.clear();

	objectStreamOpened.cv_bcast();

	initializing = false;

	return false;
    }

}
