package ibis.ipl.impl.messagePassing;

import java.io.IOException;


public abstract class ByteInputStream extends java.io.InputStream {

    protected int panda_msg;


    void setPandaMessage(int panda_msg) {
// System.err.println(Thread.currentThread() + "ByteInputStream.panda_msg := " + panda_msg);
	this.panda_msg = panda_msg;
    }


    public ByteInputStream() {
    }


    public int read(byte b[]) {
	return read(b, 0, b.length);
    }
    

    protected abstract int msg_read(int msg);

    protected abstract int msg_read_bytes(int msg, byte b[], int off, int len);

    protected abstract long msg_skip(int msg, long n);

    protected abstract int msg_available(int msg);

    protected abstract void msg_clear(int msg);


    public int read() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    return msg_read(panda_msg);
	}
    }

    public int read(byte b[], int off, int len) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
// System.err.println(Thread.currentThread() + "ByteInputStream: read " + len + " bytes");
// Thread.dumpStack();
	if (panda_msg == 0) {
	    System.err.println("Uh oh -- wanna read but msg = 0");
	    Thread.dumpStack();
	}
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    return msg_read_bytes(panda_msg, b, off, len);
	}
    }

    public long skip(long n) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    return msg_skip(panda_msg, n);
	}
    }

    public int available() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    return msg_available(panda_msg);
	}
    }

    void clearPandaMessage() {
	// Already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "Now clear pandaMessage " + panda_msg + " in ByteInputStream " + this);
	msg_clear(panda_msg);
    }

    public void close() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
    }


    public synchronized void mark(int readlimit) {
    }


    public synchronized void reset() throws IOException {
	throw new IOException("mark/reset not supported");
    }


    public boolean markSupported() {
	return false;
    }

}
