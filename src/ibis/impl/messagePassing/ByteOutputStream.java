package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import ibis.ipl.ConditionVariable;


public abstract class ByteOutputStream
	extends java.io.OutputStream
	implements PollClient {

    SendPort sport;

    private ConditionVariable sendComplete = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
    private int outstandingFrags;
    protected boolean waitingInPoll = false;
    protected boolean syncMode;

    protected int msgHandle;
    protected int msgSeqno = 0;

    protected boolean makeCopy;

    protected abstract void init();

    protected ByteOutputStream(ibis.ipl.SendPort p, boolean syncMode, boolean makeCopy) {
	this.syncMode = syncMode;
	this.makeCopy = makeCopy;
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("@@@@@@@@@@@@@@@@@@@@@ a ByteOutputStream makeCopy = " + makeCopy);
	}
	sport = (SendPort)p;
	init();
    }


    protected abstract boolean msg_send(int cpu,
					int port,
					int my_port,
					int msgSeqno,
					int msgHandle,
					boolean lastSplitter,	// Frag is sent to last of the splitters in our 1-to-many
					boolean lastFrag	// Frag is sent as last frag of a message
					);


    public void send(boolean lastFrag) throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

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


    public void send() throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    send(true);
	}
    }


    /* Called from native */
    private void finished_upcall() {
	// Already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
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
	sendComplete.cv_signal();
    }

    public void poll_wait(long timeout) {
	sendComplete.cv_wait(timeout);
    }

    Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    /* Pass our current msgHandle field: we only want to reset
     * a fragment that has been sent-acked */
    protected abstract void resetMsg();

    void reset(boolean finish) throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	if (outstandingFrags > 0) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.rcve_poll.poll();

	    if (outstandingFrags > 0) {
// System.err.println(Thread.currentThread() + "Start wait to finish msg for stream " + this);
		waitingInPoll = true;
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, true);
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


    public void reset() throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    reset(false);
	}
    }

    public void finish() throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    reset(true);
	}
    }

    public boolean completed() {
	return outstandingFrags == 0;
    }

    public abstract void close();


    public void flush() throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("+++++++++++ Now flush/Lazy this ByteOutputStream " + this + "; msgHandle 0x" + Integer.toHexString(msgHandle));
	}
// manta.runtime.RuntimeSystem.DebugMe(this, null);
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    send(false /* not lastFrag */);
	}
    }

    public abstract void write(byte[] b, int off, int len) throws IbisIOException;

    public void write(byte[] b) throws IbisIOException {
	write(b, 0, b.length);
    }

    public abstract void write(int b) throws IbisIOException;

    public abstract void report();
}
