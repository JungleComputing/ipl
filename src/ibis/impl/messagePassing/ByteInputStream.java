package ibis.impl.messagePassing;

import java.io.IOException;

/**
 * Stream to manage native code for ArrayInputStreams
 */
final class ByteInputStream
	extends ibis.io.ArrayInputStream
	implements ibis.io.IbisStreamFlags {

    private int msgHandle;
    private int msgSize;
    private ReadMessage msg;
    private int msgCount;


    void setMsgHandle(ReadMessage msg) {
	this.msg  = msg;

	// Cache msgHandle and msgSize here to save on dereferences later
	ReadFragment front = msg.fragmentFront;
	msgHandle = front.msgHandle;
	msgSize   = front.msgSize;

	if (Ibis.DEBUG) {
	    System.err.println(Thread.currentThread() + "ByteInputStream.msgHandle := " + Integer.toHexString(msgHandle) + " msgSize := " + msgSize + " msg " + msg);
	}
    }


    static native void enableAllInterrupts();
    static native void disableAllInterrupts();


    void enableInterrupts() {
	enableAllInterrupts();
    }


    void disableInterrupts() {
	disableAllInterrupts();
    }


    public int read(byte b[]) throws IOException {
	return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    if (Ibis.DEBUG) {
		System.err.println("Now want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
		Thread.dumpStack();
	    }
	    if (msgSize == 0) {
		msg.nextFragment();
		if (Ibis.DEBUG) {
		    System.err.println("After nextFragment() want to read " + len + " bytes, avaible in fragment " + Integer.toHexString(msgHandle) + ": " + msgSize);
		}
	    }
	    int rd = readByteArray(b, off, len, msgHandle);
	    msgSize -= rd * SIZEOF_BYTE;
	    msgCount += rd * SIZEOF_BYTE;
	    if (Ibis.DEBUG) {
		System.err.println("Now msgSize := " + msgSize);
	    }
	    if (Ibis.DEBUG && msgSize < 0) {
		throw new ArrayIndexOutOfBoundsException("readArray(byte[]): insufficient data");
	    }
	    return rd;
	} finally {
	    Ibis.myIbis.unlock();
	}
    }

    static native boolean getInputStreamMsg(int tags[]);

    static native int cloneMsg(int handle);

    private native int lockedRead();

    public int read() throws IOException {
	int x = -1;
	Ibis.myIbis.lock();
	try {
	    if (msgSize == 0) {
		msg.nextFragment();
	    }
	    x = lockedRead();
	    msgSize -= SIZEOF_BYTE;
	    msgCount += SIZEOF_BYTE;
	} finally {
	    Ibis.myIbis.unlock();
	}
	return x;
    }

    private native long nSkip(long n);
    private native int nAvailable();

    public long skip(long n) {
	Ibis.myIbis.lock();
	try {
	    return nSkip(n);
	} finally {
	    Ibis.myIbis.unlock();
	}
    }

    public int available() {
	Ibis.myIbis.lock();
	try {
	    return nAvailable();
	} finally {
	    Ibis.myIbis.unlock();
	}
    }

    native static void resetMsg(int msgHandle);

    private native int readBooleanArray(boolean[] array, int off, int len, int handle);
    private native int readByteArray(byte[] array, int off, int len, int handle);
    private native int readCharArray(char[] array, int off, int len, int handle);
    private native int readShortArray(short[] array, int off, int len, int handle);
    private native int readIntArray(int[] array, int off, int len, int handle);
    private native int readLongArray(long[] array, int off, int len, int handle);
    private native int readFloatArray(float[] array, int off, int len, int handle);
    private native int readDoubleArray(double[] array, int off, int len, int handle);

    public void readArray(boolean b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readBooleanArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_BOOLEAN;
		msgCount += rd * SIZEOF_BOOLEAN;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(boolean[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(byte b[], int off, int len) throws IOException {
	while (len > 0) {
	    int rd = read(b, off, len);
	    len -= rd;
	    off += rd;
	}
    }


    public void readArray(char b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readCharArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_CHAR;
		msgCount += rd * SIZEOF_CHAR;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(char[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(short b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readShortArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_SHORT;
		msgCount += rd * SIZEOF_SHORT;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(short[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(int b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readIntArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_INT;
		msgCount += rd * SIZEOF_INT;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(int[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(long b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readLongArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_LONG;
		msgCount += rd * SIZEOF_LONG;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(long[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(float b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readFloatArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_FLOAT;
		msgCount += rd * SIZEOF_FLOAT;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(float[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }


    public void readArray(double b[], int off, int len) throws IOException {
	Ibis.myIbis.lock();
	try {
	    while (len > 0) {
		if (msgSize == 0) {
		    msg.nextFragment();
		}
		int rd = readDoubleArray(b, off, len, msgHandle);
		msgSize -= rd * SIZEOF_DOUBLE;
		msgCount += rd * SIZEOF_DOUBLE;
		if (Ibis.DEBUG && msgSize < 0) {
		    throw new ArrayIndexOutOfBoundsException("readArray(double[]): insufficient data");
		}
		len -= rd;
		off += rd;
	    }
	} finally {
	    Ibis.myIbis.unlock();
	}
    }



    public void close() {
	// Ibis.myIbis.checkLockNotOwned();
    }


    public synchronized void mark(int readlimit) throws IOException {
	throw new IOException("mark/reset not supported");
    }


    public synchronized void reset() throws IOException {
	throw new IOException("mark/reset not supported");
    }


    public boolean markSupported() {
	return false;
    }

    public int getCount() {
	return msgCount;
    }

    public void resetCount() {
	msgCount = 0;
    }

}
