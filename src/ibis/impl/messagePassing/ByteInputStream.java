package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;


public abstract class ByteInputStream extends java.io.InputStream {

    protected int msgHandle;
    protected int msgSize;
    protected ibis.ipl.impl.messagePassing.ReadMessage msg;


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
    

    public abstract int read() throws IbisIOException;

    public abstract int read(byte b[], int off, int len) throws IbisIOException;

    protected abstract void resetMsg(int msgHandle);


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
