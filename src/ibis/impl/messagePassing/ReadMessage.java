package ibis.impl.messagePassing;

import java.io.IOException;

/**
 * messagePassing ReadMessage. Supports only Byte serialization
 */
public class ReadMessage implements ibis.ipl.ReadMessage {

    ShadowSendPort	shadowSendPort;

    private long	sequenceNr = -1;
    private ReceivePort	port;

    ByteInputStream	in;

    int			msgSeqno;

    boolean		finished;	// Use these for upcall receive without
    Thread		creator;	// explicit finish() call

    ReadMessage		next;
    ReadFragment	fragmentFront;
    ReadFragment	fragmentTail;

    boolean		multiFragment;

    long before;

    ReadMessage(ibis.ipl.SendPort s,
		ReceivePort port) {
	Ibis.myIbis.checkLockOwned();

// System.err.println("**************************************************Creating new ReadMessage");

	this.port = port;
	this.shadowSendPort = (ShadowSendPort)s;
	in = this.shadowSendPort.in;
    }


    void enableInterrupts() {
	in.enableInterrupts();
    }


    void disableInterrupts() {
	in.disableInterrupts();
    }


    public ibis.ipl.ReceivePort localPort() {
	return port;
    }


    private FragmentArrived fragmentArrived = new FragmentArrived();

    private class FragmentArrived extends Syncer {

	public boolean satisfied() {
	    return fragmentFront.next != null;
	}

    }


    void enqueue(ReadFragment f) {
	if (fragmentFront == null) {
	    fragmentFront = f;
	} else {
	    fragmentTail.next = f;
	}
	fragmentTail = f;
	fragmentArrived.signal();
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
	ByteInputStream.resetMsg(msgHandle);
    }


    public void nextFragment() throws IOException {
	Ibis.myIbis.checkLockOwned();
	while (! fragmentArrived.satisfied()) {
	    Ibis.myIbis.waitPolling(fragmentArrived, 0, Poll.PREEMPTIVE);
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


    long finishLocked() throws IOException {
	long after = in.getCount();
	clear();
	port.count += after - before;
	port.finishMessage();
	return after - before;
    }


    public long finish() throws IOException {
	Ibis.myIbis.lock();
	long retval = finishLocked();
	Ibis.myIbis.unlock();

	return retval;
    }


    public void finish(IOException e) {
	// What to do here? Rutger?
	try {
	    finish();
	} catch (IOException eio) {
	    throw new Error(eio);
	}
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
