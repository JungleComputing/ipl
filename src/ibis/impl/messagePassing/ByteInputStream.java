package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;


final class ByteInputStream
	extends java.io.InputStream
	implements ibis.io.TypeSize {

    int msgHandle;
    int msgSize;
    ibis.ipl.impl.messagePassing.ReadMessage msg;


    void setMsgHandle(ibis.ipl.impl.messagePassing.ReadMessage msg) {
// manta.runtime.RuntimeSystem.DebugMe(this, msg);
	this.msg  = msg;

	// Cache msgHandle and msgSize here to save on dereferences later
	ReadFragment front = msg.fragmentFront;
	msgHandle = front.msgHandle;
	msgSize   = front.msgSize;

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "ByteInputStream.msgHandle := " + Integer.toHexString(msgHandle) + " msgSize := " + msgSize + " msg " + msg);
	}
    }


    public int read(byte b[]) throws IbisIOException {
	return read(b, 0, b.length);
    }
    

    static native boolean getInputStreamMsg(int tags[]);

    private native int lockedRead();

    public int read() throws IbisIOException {
	int x;
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	x = lockedRead();
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	return x;
    }

    public native long skip(long n);
    public native int available();

    native void resetMsg(int msgHandle);

    private native int readBooleanArray(boolean[] array, int off, int len);
    private native int readByteArray(byte[] array, int off, int len);
    private native int readCharArray(char[] array, int off, int len);
    private native int readShortArray(short[] array, int off, int len);
    private native int readIntArray(int[] array, int off, int len);
    private native int readLongArray(long[] array, int off, int len);
    private native int readFloatArray(float[] array, int off, int len);
    private native int readDoubleArray(double[] array, int off, int len);

    int read(boolean b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readBooleanArray(b, off, len);
	    msgSize -= rd * SIZEOF_BOOLEAN;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(boolean[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    public int read(byte b[], int off, int len) throws IbisIOException {
// manta.runtime.RuntimeSystem.DebugMe(msgHandle, this);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Now want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
	    }
	    if (msgSize == 0) {
		msg.nextFragment();
		if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.err.println("After nextFragment() want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
		}
	    }
	    int rd = readByteArray(b, off, len);
	    msgSize -= rd * SIZEOF_BYTE;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Now msgSize := " + msgSize);
	    }
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(byte[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(char b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readCharArray(b, off, len);
	    msgSize -= rd * SIZEOF_CHAR;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(char[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(short b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readShortArray(b, off, len);
	    msgSize -= rd * SIZEOF_SHORT;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(short[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(int b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readIntArray(b, off, len);
	    msgSize -= rd * SIZEOF_INT;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(int[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(long b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readLongArray(b, off, len);
	    msgSize -= rd * SIZEOF_LONG;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(long[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(float b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readFloatArray(b, off, len);
	    msgSize -= rd * SIZEOF_FLOAT;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(float[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }


    int read(double b[], int off, int len) throws IbisIOException {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    int rd = readDoubleArray(b, off, len);
	    msgSize -= rd * SIZEOF_DOUBLE;
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG && msgSize < 0) {
		throw new IbisIOException("read(double[]): insufficient data");
	    }
	    return rd;
	} finally {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	}
    }



    public void close() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
    }


    public synchronized void mark(int readlimit) {
    }


    public synchronized void reset() throws IbisIOException {
	throw new IbisIOException("mark/reset not supported");
    }


    public boolean markSupported() {
	return false;
    }

}
