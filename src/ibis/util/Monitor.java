package ibis.util;

/**
 * Monitor synchronization construct.
 *
 * The Monitor can be entered (<code>lock</code>) and exited
 * (<code>unlock</code>). {@link ConditionVariable}s that are part of this
 * monitor can be obtained by <code>createCV</code>.
 *
 * The Monitor has optional support for priority locking. If the Monitor is
 * unlocked and some thread has locked it with <code>priority = true</code>,
 * that thread has preference in waking up above nonpriority lockers.
 */
public final class Monitor {

    final static boolean DEBUG;

    final static boolean STATISTICS;

    final boolean	PRIORITY;

    private boolean	locked = false;
    private int		waiters = 0;

    // if (PRIORITY)
    private int		prio_waiters;

    // if (DEBUG)
    private Thread	owner;

    // if (STATISTICS)
    private static int	lock_occupied;
    private static int	unlock_waiting;
    private static int	unlock_waiters;
    private static int	unlock_bcast;

    static {
	DEBUG = false; // TypedProperties.booleanProperty("ibis.monitor.debug");
	if (DEBUG) {
	    System.err.println("Turn on Monitor.DEBUG");
	}
	STATISTICS = false; // TypedProperties.booleanProperty("ibis.monitor.stats");
	if (STATISTICS) {
	    Runtime.getRuntime().addShutdownHook(new Thread() {
		public void run() {
		    Monitor.report(System.err);
		    ConditionVariable.report(System.err);
		}
	    });
	}
    }


    public Monitor(boolean priority) {
	PRIORITY = priority;
    }


    public Monitor() {
	this(false);
    }


    public synchronized void lock() {
	lock(false);
    }


    public synchronized void lock(boolean priority) {
	if (! PRIORITY && priority) {
	    throw new Error("Lock with priority=true for non-PRIORITY Monitor");
	}

	if (DEBUG && owner == Thread.currentThread()) {
	    throw new IllegalLockStateException("Already own monitor");
	}

	while (locked
		|| (PRIORITY && ! priority && prio_waiters > 0)) {
	    if (STATISTICS) {
		lock_occupied++;
	    }
	    if (PRIORITY && priority) {
		prio_waiters++;
	    }
	    waiters++;
	    try {
		wait();
	    } catch (InterruptedException e) {
		// Ignore
	    }
	    waiters--;
	    if (PRIORITY) {
	       	if (priority) {
		    prio_waiters--;
		} else if (prio_waiters > 0) {
		    // If I am not priority and there is some prio waiter, this
		    // is not for me, so wake up all and go back to wait
		    notifyAll();
		}
	    }
	}
	locked = true;

	if (DEBUG) {
	    owner = Thread.currentThread();
	}
    }


    public synchronized void unlock() {
	if (DEBUG && owner != Thread.currentThread()) {
	    Thread.dumpStack();
	    throw new IllegalLockStateException("Don't own monitor");
	}

	locked = false;
	if (waiters > 0) {
	    if (STATISTICS) {
		unlock_waiting++;
		unlock_waiters += waiters;
	    }

	    if (! PRIORITY || prio_waiters == waiters) {
		if (STATISTICS) {
		    unlock_waiting++;
		}
		// either no prio -> wake up anybody
		// or prio and only prio waiters -> wake up a prio waiter
		notify();
	    } else {
		// Sorry, there are prio and nonprio waiters. To be sure a
		// prio waiter comes alive, we have no choice but to wake all.
		if (STATISTICS) {
		    unlock_bcast++;
		}
		notifyAll();
	    }
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
	if (Monitor.STATISTICS) {
	    out.println("Monitor: lock occupied " + lock_occupied + " unlock for waiter " + unlock_waiting + " prio-bcast " + unlock_bcast + " <waiters> " + ((double)unlock_waiters) / unlock_waiting);
	}
    }

}
