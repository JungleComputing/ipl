package ibis.impl.messagePassing;

import ibis.io.SunSerializationInputStream;

import ibis.util.ConditionVariable;

import java.io.BufferedInputStream;
import java.io.IOException;
//import java.io.ObjectInputStream;

final class SerializeShadowSendPort
	extends ShadowSendPort
	implements PollClient {

    java.io.ObjectInput obj_in;

    // private
    boolean initializing = false;
    private ConditionVariable objectStreamOpened = Ibis.myIbis.createCV();


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(ReceivePortIdentifier rId,
			    SendPortIdentifier sId,
			    int group)
	    throws IOException {
	super(rId, sId, group);
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

    // interface PollClient

    private PollClient	next;
    private PollClient	prev;
    private Thread	me;

    public PollClient next() {
	return next;
    }

    public PollClient prev() {
	return prev;
    }

    public void setNext(PollClient c) {
	next = c;
    }

    public void setPrev(PollClient c) {
	prev = c;
    }

    public boolean satisfied() {
	return obj_in != null;
    }

    public void wakeup() {
	objectStreamOpened.cv_bcast();
    }

    public void poll_wait(long timeout) {
	try {
	    objectStreamOpened.cv_wait();
	} catch (InterruptedException e) {
	    // ignore
	}
    }

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }


    void disconnect() throws IOException {
	while (! satisfied()) {
	    /* Right. We hit a race here. We disconnect before the connection
	     * has actually established, and before the ObjectIOStream header
	     * has been consumed. Await that. */
	    if (Ibis.DEBUG) {
		System.err.println(this + ": OOOOPS obj_in not yet initialized. We should poll for connection establishment...");
		Thread.dumpStack();
	    }
	    Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
	}

	obj_in = null;
    }


    boolean checkStarted(ReadMessage msg) throws IOException {

	if (Ibis.DEBUG) {
	    System.err.println(this + ": checkStarted(msg=" + msg
				+ ") initializing " + initializing
				+ " obj_in " + obj_in);
	}

	if (initializing) {
	    if (Ibis.DEBUG) {
		System.err.println(Thread.currentThread() + ": Negotiate the ObjectStream init race " + this + " -- BINGO BINGO");
		Thread.dumpStack();
	    }
	    /* Right. We hit a race here. Some thread is reading the
	     * initial message, and has to unlock for that.
	     * We must wait until it's finished. */
	    while (! satisfied()) {
		Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
	    }
	    SerializeReadMessage smsg = (SerializeReadMessage)msg;
	    if (smsg.obj_in != null && smsg.obj_in != obj_in) {
		System.err.println("NNNNNNNNNNNNNNNNNNNOOOOOOOOOOOOOO this cannot be");
	    }
	    smsg.obj_in = obj_in;
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
	    obj_in = new SunSerializationInputStream(new BufferedInputStream(in));
	} finally {
	    Ibis.myIbis.lock();
	}

	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + " ShadowSendPort " + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message " + msg + " handle 0x" + Integer.toHexString(msg.fragmentFront.msgHandle) + " that contains the ObjectStream init stuff");
	}
	msg.clear();

	wakeup();

	initializing = false;

	return false;
    }

}
