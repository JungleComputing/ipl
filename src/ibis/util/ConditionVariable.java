package ibis.ipl.impl.generic;

public class ConditionVariable {

    private Monitor lock;

    static long waits;
    static long timed_waits;
    static long signals;
    static long bcasts;


    ConditionVariable(Monitor lock) {
	this.lock = lock;
    }


    final public void cv_wait() {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    waits++;
	}

	synchronized (this) {
	    lock.unlock();
	    try {
		wait();
	    } catch (InterruptedException e) {
		// Ignore
	    }
	}
	lock.lock();
    }


    final public boolean cv_wait(long timeout) {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    timed_waits++;
	}

	boolean timedOut = false;

	synchronized (this) {
	    long now = System.currentTimeMillis();
	    lock.unlock();
	    try {
		wait(timeout);
	    } catch (InterruptedException e) {
		// Ignore
	    }
	    timedOut = (System.currentTimeMillis() - now >= timeout);
	}
	lock.lock();

	return timedOut;
    }


    final public void cv_signal() {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    signals++;
	}

	synchronized (this) {
	    notify();
	}
    }


    final public void cv_bcast() {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    bcasts++;
	}

	synchronized (this) {
	    notifyAll();
	}
    }

    static public void report(java.io.PrintStream out) {
	if (Monitor.DEBUG) {
	    Monitor.report(out);
	    out.println("Condition variables: wait " + waits +
			" timed wait " + timed_waits +
			" signal " + signals +
			" bcast " + bcasts);
	}
    }

}
