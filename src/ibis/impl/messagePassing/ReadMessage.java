package ibis.impl.messagePassing;

import ibis.util.ConditionVariable;

import java.io.IOException;

public class ReadMessage
	implements ibis.ipl.ReadMessage, PollClient {

    ShadowSendPort shadowSendPort;

    private long sequenceNr = -1;
    private ReceivePort port;

    private ByteInputStream in;

    int msgSeqno;

    boolean	finished;	// Use these for upcall receive without
    Thread	creator;	// explicit finish() call

    ReadMessage	next;
    ReadFragment fragmentFront;
    ReadFragment fragmentTail;

    private int		 sleepers = 0;

    ReadMessage(ibis.ipl.SendPort s,
		ReceivePort port) {
	Ibis.myIbis.checkLockOwned();

// System.err.println("**************************************************Creating new ReadMessage");

	this.port = port;
	this.shadowSendPort = (ShadowSendPort)s;
	in = this.shadowSendPort.in;
    }


    public ibis.ipl.ReceivePort localPort() {
	return port;
    }

    void enqueue(ReadFragment f) {
	if (fragmentFront == null) {
	    fragmentFront = f;
	} else {
	    fragmentTail.next = f;
	}
	fragmentTail = f;
	wakeup();
    }


    void clear() {
	ReadFragment next;
	for (ReadFragment f = fragmentFront; f != null; f = next) {
	    if (Ibis.DEBUG) {
		System.err.println("Now clear fragment " + f + " handle " + Integer.toHexString(f.msgHandle) + "; next " + f.next);
	    }
	    next = f.next;
	    f.clear();
	    shadowSendPort.putFragment(f);
	}
	fragmentFront = null;
    }


    void setMsgHandle() {
	in.setMsgHandle(this);
    }


    void resetMsg(int msgHandle) {
	in.resetMsg(msgHandle);
    }


    /* The PollClient interface */

    private boolean	signalled;
    private boolean	accepted;
    private ConditionVariable cv = Ibis.myIbis.createCV();

    private PollClient	poll_next;
    private PollClient	poll_prev;

    public PollClient next() {
	return poll_next;
    }

    public PollClient prev() {
	return poll_prev;
    }

    public void setNext(PollClient c) {
	poll_next = c;
    }

    public void setPrev(PollClient c) {
	poll_prev = c;
    }

    public boolean satisfied() {
	return fragmentFront.next != null;
    }

    public void wakeup() {
	if (sleepers != 0) {
// System.err.println("Readmessage signalled");
	    cv.cv_signal();
	}
    }

    public void poll_wait(long timeout) {
// System.err.println("ReadMessage poll_wait");
	sleepers++;
	try {
	    cv.cv_wait(timeout);
	} catch (InterruptedException e) {
	    // ignore
	}
	sleepers--;
// System.err.println("ReadMessage woke up");
    }

    private Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    /* End of the PollClient interface */


    public void nextFragment() throws IOException {
	Ibis.myIbis.checkLockOwned();
	while (fragmentFront.next == null) {
	    Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
	}
	ReadFragment prev = fragmentFront;
	fragmentFront = fragmentFront.next;
	if (Ibis.DEBUG) {
	    System.err.println("Now set msg " + this + " the next fragment " + Integer.toHexString(fragmentFront.msgHandle));
	}
	in.setMsgHandle(this);
	prev.clear();
	shadowSendPort.putFragment(prev);
    }


    void finishLocked() {
	clear();
	port.finishMessage();
    }


    public long finish() {
	Ibis.myIbis.lock();
	finishLocked();
	Ibis.myIbis.unlock();

	// TODO: return size of message
	return 0;
    }


    public ibis.ipl.SendPortIdentifier origin() {
	return shadowSendPort.identifier();
    }


    void setSequenceNumber(long s) {
	sequenceNr = s;
    }


    public long sequenceNumber() {
	return sequenceNr;
    }


    public boolean readBoolean() throws IOException {
	throw new IOException("Read Boolean not supported");
    }


    public byte readByte() throws IOException {
	throw new IOException("Read Byte not supported");
    }


    public char readChar() throws IOException {
	throw new IOException("Read Char not supported");
    }


    public short readShort() throws IOException {
	throw new IOException("Read Short not supported");
    }


    public int  readInt() throws IOException {
	return in.read();
    }


    public long readLong() throws IOException {
	throw new IOException("Read Long not supported");
    }

    public float readFloat() throws IOException {
	throw new IOException("Read Float not supported");
    }

    public double readDouble() throws IOException {
	throw new IOException("Read Double not supported");
    }

    public String readString() throws IOException {
	throw new IOException("Read String not supported");
    }

    public Object readObject() throws IOException, ClassNotFoundException {
	throw new IOException("Read Object not supported");
    }

    public void readArray(boolean[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(byte[] destination) throws IOException {
	in.read(destination);
    }

    public void readArray(char[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(short[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(int[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(long[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(float[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(double[] destination) throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(Object[] destination)
	    throws IOException, ClassNotFoundException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(boolean[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array  not supported");
    }

    public void readArray(byte[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(char[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(short[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(int[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(long[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(float[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(double[] destination, int offset, int size)
	    throws IOException {
	throw new IOException("Read Array not supported");
    }

    public void readArray(Object[] destination, int offset, int size)
	    throws IOException, ClassNotFoundException {
	throw new IOException("Read Array not supported");
    }

}
