package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import ibis.ipl.impl.generic.ConditionVariable;


final class ByteOutputStream
	extends java.io.OutputStream
	implements PollClient {

    SendPort sport;

    private ConditionVariable sendComplete = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
    private int outstandingFrags;
    boolean waitingInPoll = false;
    boolean syncMode;

    int msgHandle;
    int msgSeqno = 0;

    boolean makeCopy;

    ByteOutputStream(ibis.ipl.SendPort p, boolean syncMode, boolean makeCopy) {
	this.syncMode = syncMode;
	this.makeCopy = makeCopy;
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("@@@@@@@@@@@@@@@@@@@@@ a ByteOutputStream makeCopy = " + makeCopy);
	}
	sport = (SendPort)p;
	init();
    }


    void send(boolean lastFrag) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// if (lastFrag)
// System.err.print("L");

	int n = sport.splitter.length;

	boolean send_acked = true;

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && this.msgHandle == 0) {
	    System.err.println("%%%%%%:::::::%%%%%%% Yeck -- message handle is NULL in " + this);
	}
	int msgHandle = this.msgHandle;
	this.msgHandle = 0;

	outstandingFrags++;

	for (int i = 0; i < n; i++) {
	    ReceivePortIdentifier r = sport.splitter[i];
	    if (msg_send(r.cpu,
			 r.port,
			 sport.ident.port,
			 msgSeqno,
			 msgHandle,
			 i == n - 1,
			 lastFrag)) {
		send_acked = false;
	    }
	}

	if (false && ! lastFrag) {
	    try {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.pollLocked();
	    } catch (IbisIOException e) {
		System.err.println("pollLocked throws " + e);
	    }
	}

	if (send_acked) {
	    outstandingFrags--;
	    if (msgHandle != 0) {
		if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.err.println(">>>>>>>>>>>>>>>>>> After sync send set msgHandle to 0x" + Integer.toHexString(msgHandle));
		}
		this.msgHandle = msgHandle;
		resetMsg();
	    }
	} else {
	    /* Do it from the sent upcall */
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println(":::::::::::::::::::: Yeck -- message 0x" + Integer.toHexString(msgHandle) + " is sent unacked");
	    }
	}
    }


    void send() {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	send(true);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }


    /* Called from native */
    private void finished_upcall() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	outstandingFrags--;
	sendComplete.cv_signal();
// System.err.println(Thread.currentThread() + "Signal finish msg for stream " + this + "; outstandingFrags " + outstandingFrags);
    }

    PollClient next;
    PollClient prev;

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
	return outstandingFrags == 0;
    }

    public void wakeup() {
//	if (outstandingFrags == 0) {
//	No, we may also be woken up because we are scheduled to poll.
// System.err.println("ByteOutputStream wakeup");
		sendComplete.cv_signal();
//	}
    }

    public void poll_wait(long timeout) {
// System.err.println("ByteOutputStream poll_wait");
	sendComplete.cv_wait(timeout);
// System.err.println("ByteOutputStream woke up");
    }

    private Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    void reset(boolean finish) throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (outstandingFrags > 0) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.pollLocked();

	    if (outstandingFrags > 0) {
// System.err.println(Thread.currentThread() + "Start wait to finish msg for stream " + this);
		waitingInPoll = true;
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
		waitingInPoll = false;
	    }
	}

// System.err.println(Thread.currentThread() + "Done  wait to finish msg for stream " + this);

	msgSeqno++;
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("}}}}}}}}}}}}}}} ByteOutputStream: reset(finish=" + finish + ") increment msgSeqno to " + msgSeqno);
	}

	if (finish) {
	    sport.reset();
	}
    }


    void reset() throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    reset(false);
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    void finish() throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    reset(true);
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    boolean completed() {
	return outstandingFrags == 0;
    }


    public void flush() {
	flush(false);
    }


    private void flush(boolean lastFrag) {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("+++++++++++ Now flush/Lazy this ByteOutputStream " + this + "; msgHandle 0x" + Integer.toHexString(msgHandle));
	}
// manta.runtime.RuntimeSystem.DebugMe(this, null);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	send(lastFrag);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }


    public void write(byte[] b) throws IbisIOException {
	write(b, 0, b.length);
    }


    native void init();

    native boolean msg_send(int cpu,
			    int port,
			    int my_port,
			    int msgSeqno,
			    int msgHandle,
			    boolean lastSplitter,
			    boolean lastFrag);

    /* Pass our current msgHandle field: we only want to reset
     * a fragment that has been sent-acked */
    native void resetMsg();

    public native void close();

    public native void write(int b) throws IbisIOException;

    public void write(byte[] b, int off, int len) throws IbisIOException {
	writeByteArray(b, off, len);
	if (syncMode) {
	    flush();
	}
    }

    native void writeBooleanArray(boolean[] array, int off, int len);
    native void writeByteArray(byte[] array, int off, int len);
    native void writeCharArray(char[] array, int off, int len);
    native void writeShortArray(short[] array, int off, int len);
    native void writeIntArray(int[] array, int off, int len);
    native void writeLongArray(long[] array, int off, int len);
    native void writeFloatArray(float[] array, int off, int len);
    native void writeDoubleArray(double[] array, int off, int len);

    native void report();
}
