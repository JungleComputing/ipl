package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.ConditionVariable;

import java.io.IOException;

public abstract class ByteOutputStream
	extends java.io.OutputStream
	implements PollClient {

    SendPort sport;

    private ConditionVariable sendComplete = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
    private int outstandingSends;

    protected int msgHandle;

    protected abstract void init();

    protected ByteOutputStream(ibis.ipl.SendPort p) {
	sport = (SendPort)p;
	init();
    }

    protected abstract boolean msg_send(int cpu, int port, int my_port, int msgHandle, boolean is_final);

    public void send() throws IbisException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    int n = sport.splitter.length;
	    // long t = Ibis.currentTime();
	    for (int i = 0; i < n; i++) {
		ReceivePortIdentifier r = sport.splitter[i];
		if (msg_send(r.cpu,
			     r.port,
			     sport.ident.port,
			     msgHandle,
			     i == n - 1)) {
		    outstandingSends++;
		}
	    }
	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.tPandaSend += Ibis.currentTime() - t;
	}
    }

    /* Called from native */
    private void finished_upcall(boolean signal) {
	// Already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	outstandingSends--;
	if (signal && outstandingSends == 0) {
	    sendComplete.cv_signal();
	}
// System.err.println(Thread.currentThread() + "Signal finish msg for stream " + this + "; outstandingSends " + outstandingSends);
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
	return outstandingSends == 0;
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

    protected abstract void panda_msg_reset(int msgHandle);

    private void reset(boolean finish) throws IbisException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    if (outstandingSends > 0) {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.rcve_poll.poll();

		if (outstandingSends > 0) {
// System.err.println(Thread.currentThread() + "Start wait to finish msg for stream " + this);
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, true);
		}
	    }

// System.err.println(Thread.currentThread() + "Done  wait to finish msg for stream " + this);

	    panda_msg_reset(msgHandle);
	    if (finish) {
		sport.reset();
	    }
	}
    }

    public void reset() throws IbisException {
	reset(false);
    }

    public void finish() throws IbisException {
	reset(true);
    }

    public abstract void close();

    public abstract void flush();

    public void write(byte[] b) {
	write(b, 0, b.length);
    }

    protected abstract void write_bytes(byte[]b, int off, int len, int msgHandle);

    public void write(byte[]b, int off, int len) {
// System.err.println(Thread.currentThread() + "ByteOutputStream: write " + len + " bytes");
// Thread.dumpStack();
	write_bytes(b, off, len, msgHandle);
    }

    protected abstract void write_int(int b, int msgHandle);

    public void write(int b) {
	write_int(b, msgHandle);
    }

    public abstract void report();
}
