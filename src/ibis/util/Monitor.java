package ibis.util;

import ibis.ipl.IllegalLockStateException;

public class Monitor {

    final static boolean DEBUG = false;
    private boolean in_use = false;
    private int waiters = 0;

    private Thread owner;
    private static int lock_occupied;
    private static int unlock_waiting;
    private static int unlock_waiters;

    static {
	if (DEBUG) {
	    System.err.println("Turn on Monitor.DEBUG");
	}
    }


    final public synchronized void lock() {
	if (DEBUG && owner == Thread.currentThread()) {
	    // manta.runtime.RuntimeSystem.DebugMe(1, this);
	    Thread.dumpStack();
	    throw new IllegalLockStateException("Already own monitor");
	}

	while (in_use) {
	    if (DEBUG) {
		lock_occupied++;
	    }
	    waiters++;
	    try {
		wait();
	    } catch (InterruptedException e) {
		// Ignore
	    }
	    waiters--;
	}
	in_use = true;

	if (DEBUG) {
	    owner = Thread.currentThread();
	}
    }


    final public synchronized void unlock() {
	if (DEBUG && owner != Thread.currentThread()) {
	    // manta.runtime.RuntimeSystem.DebugMe(2, this);
	    Thread.dumpStack();
	    throw new IllegalLockStateException("Don't own monitor");
	}

	in_use = false;
	if (waiters > 0) {
	    if (DEBUG) {
		unlock_waiting++;
		unlock_waiters += waiters;
		if (false && unlock_waiting > 100000) {
		    // manta.runtime.RuntimeSystem.DebugMe(0x3, this);
		}
	    }
	    notify();
	}

	if (DEBUG) {
	    owner = null;
	}
    }


    public ConditionVariable createCV() {
	return new ConditionVariable(this);
    }


    public ConditionVariable createCV(boolean interruptible) {
	return new ConditionVariable(this, interruptible);
    }


    final public void checkImOwner() {
	if (DEBUG) {
	    synchronized (this) {
		if (owner != Thread.currentThread()) {
		    throw new IllegalLockStateException("Don't own monitor");
		}
	    }
	}
    }


    final public void checkImNotOwner() {
	if (DEBUG) {
	    synchronized (this) {
		if (owner == Thread.currentThread()) {
		    throw new IllegalLockStateException("Already own monitor");
		}
	    }
	}
    }


    static public void report(java.io.PrintStream out) {
	if (Monitor.DEBUG) {
	    out.println("Monitor: lock occupied " + lock_occupied + " unlock for waiter " + unlock_waiting + " <waiters> " + ((double)unlock_waiters) / unlock_waiting);
	}
    }

}
