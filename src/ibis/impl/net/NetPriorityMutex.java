/* $Id$ */

package ibis.impl.net;

/**
 * Provide a special kind of mutex with a 2-level priority support.
 *
 * ToDo: !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! RFHH
 *     remove the notifyAll and replace with a set of condition variables,
 *     one for low-priority lockers, one for high-priority lockers.
 *     Cannot we switch to 1.5?
 */
public final class NetPriorityMutex {

    private final static boolean DEBUG = false;

    private Thread owner;

    private Thread[] waitingThreads = new Thread[32];

    /**
     * Store the mutex value, which cannot be negative.
     */
    private int lockvalue = 1;

    /**
     * Store the number of pending <b>high-priority</b> lock requests.
     *
     * No low-priority pending lock requests can be honoured while
     * this value is greater than 0.
     */
    private int priorityvalue = 0;

    private int waiters; // Maintain whether we need to do a notify()

    /**
     * Construct an initially unlocked mutex.
     */
    public NetPriorityMutex() {
        this(false);
    }

    /**
     * Construct an initially optionally locked mutex.
     *
     * @param locked if set to true, the mutex is initally locked.
     */
    public NetPriorityMutex(boolean locked) {
        lockvalue = locked ? 0 : 1;
    }

    private void registerWait() {
        waiters++;
        if (DEBUG) {
            if (waiters < waitingThreads.length) {
                waitingThreads[waiters] = Thread.currentThread();
            }
        }
    }

    private void unregisterWait() {
        if (DEBUG) {
            Thread me = Thread.currentThread();
            for (int i = 0; i < waiters; i++) {
                if (waitingThreads[i] == me) {
                    waitingThreads[i] = waitingThreads[waiters - 1];
                    break;
                }
                if (i == waiters && waiters < waitingThreads.length) {
                    System.err.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"
                            + " inconsistent endWait()");
                    throw new Error("inconsistent endWait()");
                }
            }
        }
        waiters--;
    }

    private void wakeupHigherPriorityThreads(boolean priority) {
        if (priority || priorityvalue <= 0) {
            return;
        }

        /* I am not a priority thread, so I am sure to sleep on
         * because there are waiting priority threads. Now I must
         * wake (at least) one of them. */
        if (waiters == 0) {
            throw new Error("Cannot be that priorityvalue > 0 && waiters == 0");
        }
        if (waiters == priorityvalue) {
            // Sure we are going to wake up a prio waiter
            notify();
        } else {
            // Sorry, there are prio and nonprio waiters. To be sure a
            // prio waiter comes alive, we have no choice but to wake all.
            notifyAll();
        }
    }

    /* This method must be called in a loop that checks the condition
     * that is waited on. Hence, there is no loop around wait() here. */
    private void doWait(boolean priority) throws InterruptedIOException {
        registerWait();
        try {
            wait();
        } catch (InterruptedException e) {
            notifyAll();
            throw new InterruptedIOException(e);
        } finally {
            unregisterWait();
        }
        wakeupHigherPriorityThreads(priority);
    }

    /* This method must be called in a loop that checks the condition
     * that is waited on. Hence, there is no loop around wait() here. */
    private void idoWait(boolean priority) throws InterruptedException {
        registerWait();
        try {
            wait();
        } catch (InterruptedException e) {
            notifyAll();
            throw e;
        } finally {
            unregisterWait();
        }
        wakeupHigherPriorityThreads(priority);
    }

    /**
     * Attempt to lock the mutex.
     *
     * If the mutex is already locked, the method will block.
     *
     * @param priority indicates whether the call is a
     * 		'high-priority' request (<code>true</code>) or a
     * 		'low-priority' request (<code>false</code>).
     * @exception InterruptedIOException if the calling thread is
     * 		interrupted while the method is blocked waiting for the
     * 		mutex.
     */
    public synchronized void lock(boolean priority)
            throws InterruptedIOException {
        if (DEBUG && lockvalue <= 0 && owner == Thread.currentThread()) {
            throw new IllegalMonitorStateException("Cannot lock twice");
        }
        if (priority) {
            priorityvalue++;
            try {
                while (lockvalue <= 0) {
                    doWait(priority);
                }
            } finally {
                priorityvalue--;
            }
        } else {
            while (priorityvalue > 0 || lockvalue <= 0) {
                doWait(priority);
            }
        }
        if (DEBUG) {
            owner = Thread.currentThread();
        }
        lockvalue--;
    }

    /**
     * Attempt to lock the mutex.
     *
     * If the mutex is already locked, the method will block.
     *
     * Pending high-priority requests are honoured first.
     *
     * This method version is {@linkplain Thread#interrupt interruptible}.
     *
     * @param priority indicates whether the call is a
     * 		'high-priority' request (<code>true</code>) or a
     * 		'low-priority' request (<code>false</code>).
     * @exception InterruptedIOException if the calling thread is
     * 		interrupted while the method is blocked waiting for the
     * 		mutex.
     */
    public synchronized void ilock(boolean priority)
            throws InterruptedException {
        if (DEBUG && lockvalue <= 0 && owner == Thread.currentThread()) {
            throw new IllegalMonitorStateException("Cannot lock twice");
        }
        if (priority) {
            priorityvalue++;
            try {
                while (lockvalue <= 0) {
                    idoWait(priority);
                }
            } finally {
                priorityvalue--;
            }
        } else {
            while (priorityvalue > 0 || lockvalue <= 0) {
                idoWait(priority);
            }
        }
        if (DEBUG) {
            owner = Thread.currentThread();
        }
        lockvalue--;
    }

    /**
     * Attempt to unblockingly lock the mutex.
     *
     * If the mutex is already locked, the method return <code>false</code>.
     *
     * @param priority indicates whether the call is a
     * 		'high-priority' request (<code>true</code>) or a
     * 		'low-priority' request (<code>false</code>).
     *
     * @return a boolean value indicating whether the lock has
     * 		been successfully acquired or not.
     */
    public synchronized boolean trylock(boolean priority) {
        if (DEBUG && lockvalue <= 0 && owner == Thread.currentThread()) {
            throw new IllegalMonitorStateException("Cannot lock twice");
        }
        if (priority) {
            if (lockvalue <= 0) {
                return false;
            }
        } else {
            if (priorityvalue > 0) {
                return false;
            }

            if (lockvalue <= 0) {
                return false;
            }
        }

        if (DEBUG) {
            owner = Thread.currentThread();
        }
        lockvalue--;
        return true;
    }

    /**
     * Unlock the mutex.
     */
    public synchronized void unlock() {
        if (DEBUG && owner != Thread.currentThread()) {
            throw new IllegalMonitorStateException(
                    "Cannot unlock from not owner");
        }
        lockvalue++;
        if (waiters > 0) {
            notify();
            // notifyAll();
        }
    }
}

