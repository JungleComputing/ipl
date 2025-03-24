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
 * Condition variable synchronization construct.
 *
 * Condition variables are part of thread synchronization primitives of the
 * {@link Monitor} construct. Threads can wait on a condition variable, and
 * condition variables can be signalled by other threads to wake up one waiting
 * thread. A bcast call wakes up all waiting threads on a ConditionVariable.
 *
 * A thread that calls <code>cv_wait</code>, <code>cv_signal</code> or
 * <code>cv_bcast</code> must have locked the {@link Monitor} that owns this
 * ConditionVariable.
 *
 * Condition Variables can be <code>interruptible</code>. For interruptible
 * Condition Variables, <code>Thread.interrupt</code>ing the {@link Thread} that
 * is waiting on this Condition Variable causes the waiting thread to return
 * with an {@link InterruptedException}. Non-interruptible Condition Variables
 * ignore <code>Thread.interrupt</code>.
 *
 * A Condition variable is created by means of the {@link Monitor#createCV()} or
 * the {@link Monitor#createCV(boolean)} method.
 */
final public class ConditionVariable {

    private Monitor lock;

    private final boolean INTERRUPTIBLE;

    static long waits;

    static long timed_waits;

    static long signals;

    static long bcasts;

    ConditionVariable(Monitor lock, boolean interruptible) {
        this.lock = lock;
        INTERRUPTIBLE = interruptible;
    }

    ConditionVariable(Monitor lock) {
        this(lock, false);
    }

    /**
     * Waits until the thread is signalled (by means of {@link #cv_signal()} or
     * {@link #cv_bcast}).
     *
     * @exception InterruptedException is thrown when the condition variable was
     *                                 created with interrupts enabled, and
     *                                 {@link Thread#interrupt()} was invoked on the
     *                                 current thread.
     */
    final public void cv_wait() throws InterruptedException {
        lock.checkImOwner();
        if (Monitor.STATISTICS) {
            waits++;
        }

        try {
            synchronized (this) {
                lock.unlock();
                if (INTERRUPTIBLE) {
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

    /**
     * Waits until the thread is signalled (by means of {@link #cv_signal()} or
     * {@link #cv_bcast}), or the specified timeout expires.
     *
     * @param timeout the specified timeout.
     * @exception InterruptedException is thrown when the condition variable was
     *                                 created with interrupts enabled, and
     *                                 {@link Thread#interrupt()} was invoked on the
     *                                 current thread.
     * @return <code>true</code> when this method returns because the timeout
     *         expired.
     */
    final public boolean cv_wait(long timeout) throws InterruptedException {
        lock.checkImOwner();
        if (Monitor.STATISTICS) {
            timed_waits++;
        }

        boolean timedOut = false;

        try {
            synchronized (this) {
                long now = System.currentTimeMillis();
                lock.unlock();
                if (INTERRUPTIBLE) {
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

    /**
     * Signals a single thread that is waiting on this condition variable.
     */
    final public void cv_signal() {
        lock.checkImOwner();
        if (Monitor.STATISTICS) {
            signals++;
        }

        synchronized (this) {
            notify();
        }
    }

    /**
     * Signals all threads that are waiting on this condition variable.
     */
    final public void cv_bcast() {
        lock.checkImOwner();
        if (Monitor.STATISTICS) {
            bcasts++;
        }

        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * When statistics are enabled, prints them on the specified stream.
     *
     * @param out the stream to print on.
     */
    static public void report(java.io.PrintStream out) {
        if (Monitor.STATISTICS) {
            out.println("Condition variables: wait " + waits + " timed wait " + timed_waits + " signal " + signals + " bcast " + bcasts);
        }
    }
}
