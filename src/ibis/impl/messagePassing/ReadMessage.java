package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import ibis.ipl.impl.generic.ConditionVariable;

public class ReadMessage
	implements ibis.ipl.ReadMessage,
		   PollClient {

    long sequenceNr = -1;
    ReceivePort port;
    ShadowSendPort shadowSendPort;

    ByteInputStream in;

    int msgSeqno;

    boolean	finished;	// Use these for upcall receive without
    Thread	creator;	// explicit finish() call

    ibis.ipl.impl.messagePassing.ReadMessage next;
    ReadFragment fragmentFront;
    ReadFragment fragmentTail;
    int		 sleepers = 0;

    ReadMessage(ibis.ipl.SendPort s,
		ReceivePort port) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

// System.err.println("**************************************************Creating new ReadMessage");

	this.port = port;
	this.shadowSendPort = (ShadowSendPort)s;
	in = this.shadowSendPort.in;
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
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Now clear fragment " + f + " handle " + Integer.toHexString(f.msgHandle) + "; next " + f.next);
	    }
	    next = f.next;
	    f.clear();
	    shadowSendPort.putFragment(f);
	}
	fragmentFront = null;
    }


    /* The PollClient interface */

    boolean	signalled;
    boolean	accepted;
    ConditionVariable cv = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

    PollClient	poll_next;
    PollClient	poll_prev;

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
	cv.cv_wait(timeout);
	sleepers--;
// System.err.println("ReadMessage woke up");
    }

    Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    /* End of the PollClient interface */


    public void nextFragment() throws IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	while (fragmentFront.next == null) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this,
								 0,
								 Poll.PREEMPTIVE);
	}
	ReadFragment prev = fragmentFront;
	fragmentFront = fragmentFront.next;
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
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


    public void finish() {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	finishLocked();
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
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


    public boolean readBoolean() throws IbisIOException {
	throw new IbisIOException("Read Boolean not supported");
    }


    public byte readByte() throws IbisIOException {
	throw new IbisIOException("Read Byte not supported");
    }


    public char readChar() throws IbisIOException {
	throw new IbisIOException("Read Char not supported");
    }


    public short readShort() throws IbisIOException {
	throw new IbisIOException("Read Short not supported");
    }


    public int  readInt() throws IbisIOException {
	return in.read();
    }


    public long readLong() throws IbisIOException {
	throw new IbisIOException("Read Long not supported");
    }

    public float readFloat() throws IbisIOException {
	throw new IbisIOException("Read Float not supported");
    }

    public double readDouble() throws IbisIOException {
	throw new IbisIOException("Read Double not supported");
    }

    public String readString() throws IbisIOException {
	throw new IbisIOException("Read String not supported");
    }

    public Object readObject() throws IbisIOException {
	throw new IbisIOException("Read Object not supported");
    }

    public void readArrayBoolean(boolean[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayBoolean not supported");
    }

    public void readArrayByte(byte[] destination) throws IbisIOException {
	in.read(destination);
    }

    public void readArrayChar(char[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayChar not supported");
    }

    public void readArrayShort(short[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayShort not supported");
    }

    public void readArrayInt(int[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayInt not supported");
    }

    public void readArrayLong(long[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayLong not supported");
    }

    public void readArrayFloat(float[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayFloat not supported");
    }

    public void readArrayDouble(double[] destination) throws IbisIOException {
	throw new IbisIOException("Read ArrayDouble not supported");
    }

    public void readSubArrayBoolean(boolean[] destination, int offset,
				    int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayBoolean not supported");
    }

    public void readSubArrayByte(byte[] destination, int offset,
				 int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayByte not supported");
    }

    public void readSubArrayChar(char[] destination, int offset,
				 int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayChar not supported");
    }

    public void readSubArrayShort(short[] destination, int offset,
				  int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayShort not supported");
    }

    public void readSubArrayInt(int[] destination, int offset,
				int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayInt not supported");
    }

    public void readSubArrayLong(long[] destination, int offset,
				 int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayLong not supported");
    }

    public void readSubArrayFloat(float[] destination, int offset,
				  int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayFloat not supported");
    }

    public void readSubArrayDouble(double[] destination, int offset,
				   int size) throws IbisIOException {
	throw new IbisIOException("Read SubArrayDouble not supported");
    }

}
