package ibis.ipl.impl.generic;

public class ConditionVariable {

    private Monitor lock;
    private final boolean interruptible;

    static long waits;
    static long timed_waits;
    static long signals;
    static long bcasts;


    ConditionVariable(Monitor lock, boolean interruptible) {
	this.lock = lock;
	this.interruptible = interruptible;
    }


    ConditionVariable(Monitor lock) {
	this(lock, false);
    }


    final public void cv_wait() throws InterruptedException {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    waits++;
	}

	try {
	    synchronized (this) {
		lock.unlock();
		if (interruptible) {
		    wait();
		} else {
		    try {
			wait();
		    } catch (InterruptedException e) {
			// Ignore
		    }
		}
	    }
	} finally {
	    lock.lock();
	}
    }


    final public boolean cv_wait(long timeout) throws InterruptedException {
	lock.checkImOwner();
	if (Monitor.DEBUG) {
	    timed_waits++;
	}

	boolean timedOut = false;

	try {
	    synchronized (this) {
		long now = System.currentTimeMillis();
		lock.unlock();
		if (interruptible) {
		    wait(timeout);
		} else {
		    try {
			wait(timeout);
		    } catch (InterruptedException e) {
			// Ignore
		    }
		}
		timedOut = (System.currentTimeMillis() - now >= timeout);
	    }
	} finally {
	    lock.lock();
	}

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
