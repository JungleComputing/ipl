/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.util;

/**
 * Monitor synchronization construct.
 *
 * The Monitor can be entered ({@link #lock()}) and exited ({@link #unlock()}).
 * {@link ConditionVariable}s that are part of this monitor can be obtained by
 * {@link #createCV()}.
 *
 * The Monitor has optional support for priority locking. If the Monitor is
 * unlocked and some thread has locked it with <code>priority = true</code>,
 * that thread has preference in waking up above nonpriority lockers.
 */
public final class Monitor {

    final private static String PROPERTY_PREFIX = "ibis.util.monitor.";

    final private static String asserts = PROPERTY_PREFIX + "assert";

    final private static String stats = PROPERTY_PREFIX + "stats";

    final private static String[] props = { asserts, stats };

    final private static UtilProperties myprops = new UtilProperties(
            System.getProperties(), PROPERTY_PREFIX, props);

    final static boolean ASSERTS = myprops.getBooleanProperty(asserts);

    final static boolean STATISTICS = myprops.getBooleanProperty(stats);

    final boolean PRIORITY;

    private boolean locked = false;

    private int waiters = 0;

    // if (PRIORITY)
    private int prio_waiters;

    // if (ASSERTS)
    private Thread owner;

    // if (STATISTICS)
    private static int lock_occupied;

    private static int unlock_waiting;

    private static int unlock_waiters;

    private static int unlock_bcast;

    static {
        if (ASSERTS) {
            System.err.println("Turn on Monitor.ASSERTS");
        }
        if (STATISTICS) {
            Runtime.getRuntime()
                    .addShutdownHook(new Thread("Ibis Monitor ShutdownHook") {
                        @Override
                        public void run() {
                            Monitor.report(System.err);
                            ConditionVariable.report(System.err);
                        }
                    });
        }
    }

    /**
     * Constructs a <code>Monitor</code>. The parameter indicates wether it must
     * have support for priority locking.
     * 
     * @param priority
     *            when <code>true</code>, priority locking will be supported.
     */
    public Monitor(boolean priority) {
        PRIORITY = priority;
    }

    /**
     * Constructs a <code>Monitor</code>, without support for priority locking.
     */
    public Monitor() {
        this(false);
    }

    /**
     * Enters the Monitor, without priority over other threads.
     */
    public synchronized void lock() {
        lock(false);
    }

    /**
     * Enters the Monitor. The parameter indicates wether this thread has
     * priority over nonpriority lockers. This means that when the lock is
     * released, this thread will get the lock before nonpriority lockers.
     * 
     * @param priority
     *            when <code>true</code>, this thread has priority over
     *            nonpriority lockers.
     */
    public synchronized void lock(boolean priority) {
        if (!PRIORITY && priority) {
            throw new Error("Lock with priority=true for non-PRIORITY Monitor");
        }

        if (ASSERTS && owner == Thread.currentThread()) {
            throw new IllegalLockStateException("Already own monitor");
        }

        while (locked || (PRIORITY && !priority && prio_waiters > 0)) {
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

        if (ASSERTS) {
            owner = Thread.currentThread();
        }
    }

    /**
     * Tries to enter the Monitor, without priority over other threads. If the
     * Monitor is locked by someone else, <code>false</code> is returned.
     * Otherwise, the lock is taken and <code>true</code> is returned.
     * 
     * @return <code>true</code> if the attempt was succesful,
     *         <code>false</code> otherwise.
     */
    public synchronized boolean tryLock() {
        if (locked || (PRIORITY && prio_waiters > 0)) {
            return false;
        }

        lock();

        return true;
    }

    /**
     * Leaves the Monitor, making it available for other threads.
     */
    public synchronized void unlock() {
        if (ASSERTS && owner != Thread.currentThread()) {
            Thread.dumpStack();
            throw new IllegalLockStateException("Don't own monitor");
        }

        locked = false;
        if (waiters > 0) {
            if (STATISTICS) {
                unlock_waiting++;
                unlock_waiters += waiters;
            }

            if (!PRIORITY || prio_waiters == waiters) {
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

        if (ASSERTS) {
            owner = null;
        }
    }

    /**
     * Creates a {@link ConditionVariable} associated with this Monitor.
     * 
     * @return the ConditionVariable created.
     */
    public ConditionVariable createCV() {
        return new ConditionVariable(this);
    }

    /**
     * Creates a {@link ConditionVariable} associated with this Monitor.
     * 
     * @param interruptible
     *            when <code>true</code>, {@link Thread#interrupt()}ing the
     *            {@link Thread} that is waiting on this Condition Variable
     *            causes the waiting thread to return with an
     *            {@link InterruptedException}. Non-interruptible Condition
     *            Variables ignore {@link Thread#interrupt()}.
     * @return the condition variable.
     */
    public ConditionVariable createCV(boolean interruptible) {
        return new ConditionVariable(this, interruptible);
    }

    /**
     * When debugging is enabled, throws an exception when the current thread
     * does not own the Monitor.
     * 
     * @exception IllegalLockStateException
     *                is thrown when the current thread does not own the
     *                Monitor.
     */
    final public void checkImOwner() {
        if (ASSERTS) {
            synchronized (this) {
                if (owner != Thread.currentThread()) {
                    throw new IllegalLockStateException("Don't own monitor");
                }
            }
        }
    }

    /**
     * When debugging is enabled, throws an exception when the current thread
     * owns the Monitor.
     * 
     * @exception IllegalLockStateException
     *                is thrown when the current thread owns the Monitor.
     */
    final public void checkImNotOwner() {
        if (ASSERTS) {
            synchronized (this) {
                if (owner == Thread.currentThread()) {
                    throw new IllegalLockStateException("Already own monitor");
                }
            }
        }
    }

    /**
     * When statistics are enabled, this method prints some on the stream given.
     * 
     * @param out
     *            the stream to print on.
     */
    static public void report(java.io.PrintStream out) {
        if (Monitor.STATISTICS) {
            out.println("Monitor: lock occupied " + lock_occupied
                    + " unlock for waiter " + unlock_waiting + " prio-bcast "
                    + unlock_bcast + " <waiters> "
                    + ((double) unlock_waiters) / unlock_waiting);
        }
    }

    /**
     * Returns the thread that owns this Monitor.
     * 
     * @return the owner thread.
     */
    public Thread getOwner() {
        return owner;
    }

}
