package ibis.impl.messagePassing;

import ibis.io.DataAllocator;
import ibis.util.ConditionVariable;
import ibis.util.TypedProperties;

import java.io.IOException;


/**
 * Stream to manage native code for ArrayOutputStreams
 */
final class ByteOutputStream
	extends ibis.io.ArrayOutputStream {

    static final int FIRST_FRAG_BIT  = (1 << 30);
    static final int LAST_FRAG_BIT   = (1 << 31);
    static final int SEQNO_FRAG_BITS = (FIRST_FRAG_BIT | LAST_FRAG_BIT);

    private SendPort sport;

    private SendComplete sendComplete = new SendComplete();
    private ConditionVariable fragCv = Ibis.myIbis.createCV();

    /**
     * This field is read and <strong>written</strong> from native code
     *
     * Count the number of outstandig fragments that live within the current
     * message.
     */
    private int outstandingFrags;

    /**
     * This field counts the number of sent fragments within a message.
     */
    private int sentFrags = 0;

    /**
     * This field is read from native code
     */
    private boolean waitingInPoll = false;

    /**
     * This field is read from native code
     */
    private boolean fragWaiting = false;

    private boolean syncMode;

    private int msgSeqno = 0;

    /**
     * This field is read from native code
     */
    private boolean makeCopy;

    /**
     * This field is read and <strong>written</strong> from native code
     */
    private long msgCount;


    /**
     * This field is read and <strong>written</strong> from native code
     *
     * It is a pointer to a native data structure that mirrors the java
     * stream; it builds up the data vector for the current message and
     * keeps a GlobalRef to this.
     */
    private int		nativeByteOS;


    /**
     * The buffer allocator used by an IbisSerializationOutputStream on
     * top of us. Used to reclaim buffers.
     */
    private ibis.io.DataAllocator allocator = null;


    static private boolean warningPrinted = false;


    ByteOutputStream(ibis.ipl.SendPort p, boolean syncMode, boolean makeCopy) {
	this.syncMode = syncMode;
	this.makeCopy = makeCopy;
	if (Ibis.DEBUG) {
	    System.err.println("@@@@@@@@@@@@@@@@@@@@@ a ByteOutputStream makeCopy = " + makeCopy);
	}
	sport = (SendPort)p;
	nativeByteOS = init();
    }

    int getSentFrags() {
	return sentFrags;
    }

    int getMsgSeqno() {
	return msgSeqno;
    }

    void waitForFragno(int ticket) {
	Ibis.myIbis.checkLockOwned();
	while (sentFrags - outstandingFrags < ticket) {
	    fragWaiting = true;
	    try {
		fragCv.cv_wait();
	    } catch(InterruptedException e) {
	    }
	}
	fragWaiting = false;
    }

    void wakeupFragWaiter() {
	fragCv.cv_signal();
    }

    void setAllocator(DataAllocator allocator) {
	if (Ibis.myIbis.myCpu == 0 && ! warningPrinted && allocator != null) {
	    System.err.println(this + ": set allocator " + allocator);
	    warningPrinted = true;
	}
	this.allocator = allocator;
    }


    void send(boolean lastFrag) throws IOException {
	Ibis.myIbis.checkLockOwned();
// if (lastFrag)
// System.err.print("L");

	int n = sport.splitter.length;

	boolean send_acked;

	outstandingFrags++;
	sentFrags++;

	if (sport.group != SendPort.NO_BCAST_GROUP) {
	    send_acked = msg_bcast(sport.group,
				   msgSeqno,
				   lastFrag);
	} else {
	    send_acked = true;
	    for (int i = 0; i < n; i++) {
		ReceivePortIdentifier r = sport.splitter[i];

		/* The call for the last connection knows whether the
		 * send has been acked. Believe the last call. */
		send_acked = msg_send(r.cpu,
				      r.port,
				      sport.ident.port,
				      msgSeqno,
				      i,
				      n,
				      lastFrag);
	    }
	}

	if (send_acked) {
	    outstandingFrags--;
	} else {
	    /* Decrement outstandingFrags from the sent upcall */
	    if (Ibis.DEBUG) {
		System.err.println(":::::::::::::::::::: Yeck -- message " + this + " is sent unacked");
	    }
	}
    }


    void send() throws IOException {
	Ibis.myIbis.lock();
	send(true);
	Ibis.myIbis.unlock();
    }


    /* Called from native */
    private void finished_upcall() {
	Ibis.myIbis.checkLockOwned();
	sendComplete.signal();
	if (fragWaiting) {
	    fragCv.cv_signal();
	}
// System.err.println(Thread.currentThread() + "Signal finish msg for stream " + this + "; outstandingFrags " + outstandingFrags);
    }

    private class SendComplete extends Syncer {

	public boolean satisfied() {
	    return outstandingFrags == 0;
	}

	public void signal() {
	    Ibis.myIbis.checkLockOwned();
	    outstandingFrags--;
	    wakeup();
	}

    }

    void reset(boolean finish) throws IOException {
	Ibis.myIbis.checkLockOwned();
	if (outstandingFrags > 0) {
	    Ibis.myIbis.pollLocked();

	    if (outstandingFrags > 0) {
// System.err.println(Thread.currentThread() + "Start wait to finish msg for stream " + this);
		waitingInPoll = true;
		Ibis.myIbis.waitPolling(sendComplete, 0, Poll.PREEMPTIVE);
		waitingInPoll = false;
	    }
	}

// System.err.println(Thread.currentThread() + "Done  wait to finish msg for stream " + this);

	msgSeqno++;
	sentFrags = 0;
	if (Ibis.DEBUG) {
	    System.err.println("}}}}}}}}}}}}}}} ByteOutputStream: reset(finish=" + finish + ") increment msgSeqno to " + msgSeqno);
	}

	if (finish) {
	    sport.reset();
	}
    }


    void reset() throws IOException {
	Ibis.myIbis.lock();
	try {
	    reset(false);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void finish() throws IOException {
	Ibis.myIbis.lock();
	try {
	    reset(true);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    private native void		releaseCachedBuffers();
    private native Object	getCachedBuffer();
    private native void		clearGlobalRefs();


    private int releaseBuffers() {
	Ibis.myIbis.checkLockOwned();

	int returned = 0;

// System.err.println("Try to release cached bufs.. nativeByteOs " + Integer.toHexString(nativeByteOS));

	Object buffer;

	while ((buffer = getCachedBuffer()) != null) {
	    if (buffer instanceof byte[]) {
		allocator.putByteArray((byte[]) buffer);
	    } else if (buffer instanceof char[]) {
		allocator.putCharArray((char[]) buffer);
	    } else if (buffer instanceof short[]) {
		allocator.putShortArray((short[]) buffer);
	    } else if (buffer instanceof int[]) {
		allocator.putIntArray((int[]) buffer);
	    } else if (buffer instanceof long[]) {
		allocator.putLongArray((long[]) buffer);
	    } else if (buffer instanceof float[]) {
		allocator.putFloatArray((float[]) buffer);
	    } else if (buffer instanceof double[]) {
		allocator.putDoubleArray((double[]) buffer);
	    }
	    returned++;
	}

	clearGlobalRefs();

	return returned;
    }


    public boolean finished() throws IOException {
	if (allocator == null) {
	    return outstandingFrags == 0;
	}

	int returned = 0;
// System.err.println("finished -> outstandingFrags " + outstandingFrags);
	Ibis.myIbis.lock();
	try {
	    if (outstandingFrags > 0) {
		Ibis.myIbis.pollLocked();
	    }
	    boolean anyOutstandingFrags = (outstandingFrags > 0);
	    returned = releaseBuffers();
	    return ! anyOutstandingFrags
		&& returned == 0
		;
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void flush() throws IOException {
	flush(false);
    }


    private void flush(boolean lastFrag) throws IOException {
// manta.runtime.RuntimeSystem.DebugMe(this, null);
	Ibis.myIbis.lock();
	send(lastFrag);
	Ibis.myIbis.unlock();
    }


    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }


    private native int init();

    private native boolean msg_send(int cpu,
				    int port,
				    int my_port,
				    int msgSeqno,
				    int splitCount,
				    int splitTotal,
				    boolean lastFrag) throws IOException;
    private native boolean msg_bcast(int group,
				     int msgSeqno,
				     boolean lastFrag) throws IOException;

    public native void close();

    public native void write(int b) throws IOException;

    public void write(byte[] b, int off, int len) throws IOException {
	writeArray(b, off, len);
	if (syncMode) {
	    flush();
	}
    }

    public long getCount() {
	return msgCount;
    }

    public void resetCount() {
	msgCount = 0;
    }

    public final long bytesWritten() { 
	return getCount();
    }

    public final void resetBytesWritten() {
	resetCount();
    }

    public native void writeArray(boolean[] array, int off, int len)
	    throws IOException;
    public native void writeArray(byte[] array, int off, int len)
	    throws IOException;
    public native void writeArray(char[] array, int off, int len)
	    throws IOException;
    public native void writeArray(short[] array, int off, int len)
	    throws IOException;
    public native void writeArray(int[] array, int off, int len)
	    throws IOException;
    public native void writeArray(long[] array, int off, int len)
	    throws IOException;
    public native void writeArray(float[] array, int off, int len)
	    throws IOException;
    public native void writeArray(double[] array, int off, int len)
	    throws IOException;

    native void report();
}
