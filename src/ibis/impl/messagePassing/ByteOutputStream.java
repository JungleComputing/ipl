package ibis.impl.messagePassing;

import ibis.util.ConditionVariable;
import ibis.util.TypedProperties;

import ibis.io.DataAllocator;

import java.io.IOException;


final class ByteOutputStream
	// extends java.io.OutputStream
	extends ibis.io.ArrayOutputStream
	implements PollClient {

    private SendPort sport;

    private ConditionVariable sendComplete = Ibis.myIbis.createCV();

    /**
     * This field is read and <standout>written</standout> from native code
     *
     * Count the number of outstandig fragments that live within the current
     * message.
     */
    private int outstandingFrags;

    /**
     * This field is read from native code
     */
    private boolean waitingInPoll = false;

    private boolean syncMode;

    private int msgSeqno = 0;

    private boolean firstFrag = true;	// Enforce firstFrag setting in native

    /**
     * This field is read from native code
     */
    private boolean makeCopy;

    /**
     * This field is read and <standout>written</standout> from native code
     */
    private long msgCount;


    /**
     * This field is read and <standout>written</standout> from native code
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


    ByteOutputStream(ibis.ipl.SendPort p, boolean syncMode, boolean makeCopy) {
	this.syncMode = syncMode;
	this.makeCopy = makeCopy;
	if (Ibis.DEBUG) {
	    System.err.println("@@@@@@@@@@@@@@@@@@@@@ a ByteOutputStream makeCopy = " + makeCopy);
	}
	sport = (SendPort)p;
	nativeByteOS = init();
    }


    void setAllocator(DataAllocator allocator) {
	if (TypedProperties.booleanProperty("ibis.mp.allocator")) {
	    System.err.println(this + ": set allocator " + allocator);
	    this.allocator = allocator;
	}
    }


    void send(boolean lastFrag) throws IOException {
	Ibis.myIbis.checkLockOwned();
// if (lastFrag)
// System.err.print("L");

	int n = sport.splitter.length;

	boolean send_acked;

	outstandingFrags++;

	if (sport.group != SendPort.NO_BCAST_GROUP) {
	    send_acked = msg_bcast(sport.group,
				   msgSeqno,
				   firstFrag,
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
				      firstFrag,
				      lastFrag);
	    }
	}

	firstFrag = lastFrag;	// Set state for next time round

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
	try {
	    sendComplete.cv_wait(timeout);
	} catch (InterruptedException e) {
	    // ignore
	}
// System.err.println("ByteOutputStream woke up");
    }

    private Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    void reset(boolean finish) throws IOException {
	Ibis.myIbis.checkLockOwned();
	if (outstandingFrags > 0) {
	    Ibis.myIbis.pollLocked();

	    if (outstandingFrags > 0) {
// System.err.println(Thread.currentThread() + "Start wait to finish msg for stream " + this);
		waitingInPoll = true;
		Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
		waitingInPoll = false;
	    }
	}

// System.err.println(Thread.currentThread() + "Done  wait to finish msg for stream " + this);

	msgSeqno++;
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
    private native byte[]	getCachedByteBuffer();
    private native char[]	getCachedCharBuffer();
    private native short[]	getCachedShortBuffer();
    private native int[]	getCachedIntBuffer();
    private native long[]	getCachedLongBuffer();
    private native float[]	getCachedFloatBuffer();
    private native double[]	getCachedDoubleBuffer();
    private native void		clearGlobalRefs();


    private int releaseBuffers(boolean recycle) {
	Ibis.myIbis.checkLockOwned();

	if (! recycle) {
	    // Hand them to the GC and clear the global refs
	    releaseCachedBuffers();

	    return 0;
	}

	int returned = 0;

// System.err.println("Try to release cached bufs.. nativeByteOs " + Integer.toHexString(nativeByteOS));
	byte[] byteBuffer;
	while ((byteBuffer = getCachedByteBuffer()) != null) {
	    allocator.putByteArray(byteBuffer);
	    returned++;
	}
	char[] charBuffer;
	while ((charBuffer = getCachedCharBuffer()) != null) {
	    allocator.putCharArray(charBuffer);
	    returned++;
	}
	short[] shortBuffer;
	while ((shortBuffer = getCachedShortBuffer()) != null) {
// System.err.println("put ShortArray " + shortBuffer);
	    allocator.putShortArray(shortBuffer);
	    returned++;
	}
	int[] intBuffer;
	while ((intBuffer = getCachedIntBuffer()) != null) {
// System.err.println("put IntArray " + intBuffer);
	    allocator.putIntArray(intBuffer);
	    returned++;
	}
	long[] longBuffer;
	while ((longBuffer = getCachedLongBuffer()) != null) {
	    allocator.putLongArray(longBuffer);
	    returned++;
	}
	float[] floatBuffer;
	while ((floatBuffer = getCachedFloatBuffer()) != null) {
	    allocator.putFloatArray(floatBuffer);
	    returned++;
	}
	double[] doubleBuffer;
	while ((doubleBuffer = getCachedDoubleBuffer()) != null) {
	    allocator.putDoubleArray(doubleBuffer);
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
	    returned = releaseBuffers(anyOutstandingFrags);
	    return ! anyOutstandingFrags
		&& returned == 0
		// && allocator == null
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
				    boolean forceFirstFrag,
				    boolean lastFrag) throws IOException;
    private native boolean msg_bcast(int group,
				     int msgSeqno,
				     boolean forceFirstFrag,
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
