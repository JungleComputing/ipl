package ibis.impl.messagePassing;

import ibis.io.SunSerializationInputStream;
import ibis.util.ConditionVariable;

import java.io.BufferedInputStream;
import java.io.IOException;

final class SerializeShadowSendPort extends ShadowSendPort {

    private final static boolean DEBUG = Ibis.DEBUG || ShadowSendPort.DEBUG;
    private final static boolean REQUIRE_SYNC_AT_CONNECT_TIME = false;

    java.io.ObjectInput obj_in;

    private int syncer;

    // private
    private final static int UNCONNECTED = 0;
    private final static int CONNECTING = UNCONNECTED + 1;
    private final static int CONNECTED = CONNECTING + 1;

    private int connectState = UNCONNECTED;


    /* Create a shadow SendPort, used by the local ReceivePort to refer to */
    SerializeShadowSendPort(ReceivePortIdentifier rId,
			    SendPortIdentifier sId,
			    int startSeqno,
			    int group,
			    int groupStartSeqno,
			    int syncer)
	    throws IOException {
	super(rId, sId, startSeqno, group, groupStartSeqno);

	Ibis.myIbis.checkLockOwned();

	this.syncer = syncer;
    }


    ReadMessage getMessage(int msgSeqno) throws IOException {
	if (DEBUG) {
	    if (obj_in == null || connectState != CONNECTED) {
		System.err.println(this + ": OOOOOPS getMessage(), cachedMessage " + cachedMessage + " obj_in " + obj_in + " connectState " + connectState);
	    }
	}

	while (false && connectState != CONNECTED) {
	    objectStreamSyncer.s_wait(0);
	}

	ReadMessage msg = cachedMessage;

	if (DEBUG) {
	    System.err.println(this + ": Get a Serialize ReadMessage ");
	}

	if (msg != null) {
	    if (connectState != CONNECTED) {
		System.err.println(this + ": OOOOOPS getMessage(), cachedMessage nonnull but connectState " + connectState + " (i.e. not connected)");
	    }
	    cachedMessage = null;

	} else {
	    msg = new SerializeReadMessage(this, receivePort);
	    if (DEBUG) {
		System.err.println(Thread.currentThread() + ": Create a -sun- ReadMessage " + msg); 
	    }
	}

	msg.msgSeqno = msgSeqno;

	return msg;
    }


    private class ObjectStreamSyncer extends Syncer {

	public boolean satisfied() {
	    return obj_in != null;
	}

    }

    private ObjectStreamSyncer objectStreamSyncer = new ObjectStreamSyncer();


    void disconnect() throws IOException {

	connectState = CONNECTING;
	while (cachedMessage != null) {
	    // During our disconnect/connect, some thread is reading a message.
	    // Wait until it is done.
	    Ibis.myIbis.waitPolling(objectStreamSyncer, 0, Poll.PREEMPTIVE);
	}

	while (! objectStreamSyncer.satisfied()) {
	    /* Right. We hit a race here. We disconnect before the connection
	     * has actually established, and before the ObjectIOStream header
	     * has been consumed. Await that. */
	    if (DEBUG) {
		System.err.println(this + ": OOOOPS obj_in not yet initialized. We should poll for connection establishment...");
		Thread.dumpStack();
	    }
	    Ibis.myIbis.waitPolling(objectStreamSyncer, 0, Poll.PREEMPTIVE);
	}
	if (DEBUG) {
	    System.err.println(this + ": received a disconnect message; currently group " + group);
	}

	if (cachedMessage != null) {
	    System.err.println(this + ": OOOOPS disconnect but cachedMessage " + cachedMessage);
	}
	connectState = UNCONNECTED;
	obj_in = null;
    }


    boolean checkStarted(ReadMessage msg) throws IOException {

	if (DEBUG) {
	    System.err.println(this + ": checkStarted(msg=" + msg
				+ ") connectState " + connectState
				+ " obj_in " + obj_in);
	}

	if (REQUIRE_SYNC_AT_CONNECT_TIME) {
	    if (connectState == CONNECTING) {
		if (DEBUG) {
		    System.err.println(Thread.currentThread() + ": Negotiate the ObjectStream init race " + this + " -- BINGO BINGO");
		    Thread.dumpStack();
		}
		/* Right. We hit a race here. Some thread is reading the
		 * initial message, and has to unlock for that.
		 * We must wait until it's finished. */
		while (connectState != CONNECTED) {
		    Ibis.myIbis.waitPolling(objectStreamSyncer, 0, Poll.PREEMPTIVE);
		}
		SerializeReadMessage smsg = (SerializeReadMessage)msg;
		if (smsg.obj_in != null && smsg.obj_in != obj_in) {
		    System.err.println("NNNNNNNNNNNNNNNNNNNOOOOOOOOOOOOOO this cannot be");
		}
		smsg.obj_in = obj_in;
	    }
	}

	if (connectState == CONNECTED) {
	    return true;
	}

	connectState = CONNECTING;
	while (cachedMessage != null) {
	    // During our disconnect/connect, some thread is reading a message.
	    // Wait until it is done.
	    Ibis.myIbis.waitPolling(objectStreamSyncer, 0, Poll.PREEMPTIVE);
	}

	if (DEBUG) {
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
	    obj_in = new SunSerializationInputStream(new BufferedInputStream(new InputStream(in)));
	} finally {
	    Ibis.myIbis.lock();
	}

	if (DEBUG) {
	    System.err.println(Thread.currentThread() + " ShadowSendPort "
		    + this + " has created ObjectInputStream " + obj_in);
	    System.err.println("Clear the message " + msg + " handle 0x"
		    + Integer.toHexString(msg.fragmentFront.msgHandle)
		    + " that contains the ObjectStream init stuff");
	}
	msg.clear();
	tickReceive();

	connectState = CONNECTED;

	sendConnectAck(ident.cpu, syncer, true);
	if (REQUIRE_SYNC_AT_CONNECT_TIME) {
	    objectStreamSyncer.s_bcast(true);
	}
	if (DEBUG) {
	    System.err.println(this +": handled connect, msg " + msg
		    + " syncer " + Integer.toHexString(syncer)
		    + " startSeqno " + messageCount
		    + " groupStartSeqno " + groupStartSeqno);
	}

	return false;
    }

}
