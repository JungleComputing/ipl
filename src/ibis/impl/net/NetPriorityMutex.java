package ibis.impl.net;

import java.io.IOException;

import ibis.ipl.InterruptedIOException;
import ibis.ipl.Ibis;

/**
 * Provide a special kind of mutex with a 2-level priority support.
 */
public final class NetPriorityMutex {

	private final static boolean DEBUG = false;

	private Thread owner;

        /**
         * Store the mutex value, which cannot be negative.
         */
	private int lockvalue     = 1;

        /**
         * Store the number of pending <b>high-priority</b> lock requests.
         *
         * No low-priority pending lock requests can be honoured while
         * this value is greater than 0.
         */
	private int priorityvalue = 0;

	private int waiters;	// Maintain whether we need to do a notify()

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
		lockvalue = locked?0:1;
	}

        /**
         * Attempt to lock the mutex.
         *
         * If the mutex is already locked, the method will block.
         *
         * @param priority indicates whether the call is a
         * 'high-priority' request (<code>true</code>) or a
         * 'low-priority' request (<code>false</code>).
         * @exception InterruptedIOException if the calling thread is
         * interrupted while the method is blocked waiting for the
         * mutex.
         */
	public synchronized void lock(boolean priority) throws InterruptedIOException{
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
				if (DEBUG && owner == Thread.currentThread()) {
					throw new IllegalMonitorStateException("Cannot lock twice");
				}
                                try {
					waiters++;
                                        wait();
					waiters--;
                                } catch (InterruptedException e) {
                                        synchronized(this) {
                                                priorityvalue--;
						if (waiters > 0) {
							notify();
							// notifyAll();
						}
                                        }
                                        throw new InterruptedIOException(e);
                                }
                        }
                        priorityvalue--;
                } else {
                        while (priorityvalue > 0 || lockvalue <= 0) {
				if (DEBUG && lockvalue <= 0 && owner == Thread.currentThread()) {
					throw new IllegalMonitorStateException("Cannot lock twice");
				}
				waiters++;
				try {
					wait();
				} catch (InterruptedException e) {
					throw new InterruptedIOException(e);
				} finally {
					waiters--;
				}
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
         * 'high-priority' request (<code>true</code>) or a
         * 'low-priority' request (<code>false</code>).
         * @exception InterruptedIOException if the calling thread is
         * interrupted while the method is blocked waiting for the
         * mutex.
         */
	public synchronized void ilock(boolean priority) throws InterruptedIOException {
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
				if (DEBUG && owner == Thread.currentThread()) {
					throw new IllegalMonitorStateException("Cannot lock twice");
				}
                                try {
					waiters++;
                                        wait();
					waiters--;
				} catch (InterruptedException e) {
					throw new InterruptedIOException(e);
                                } finally {
                                        synchronized(this) {
                                                priorityvalue--;
						if (waiters > 0) {
							notify();
							// // notify();
							// notifyAll();
						}
                                        }
                                }
                        }
                        priorityvalue--;
                } else {
                        while (priorityvalue > 0 || lockvalue <= 0) {
				if (DEBUG && lockvalue <= 0 && owner == Thread.currentThread()) {
					throw new IllegalMonitorStateException("Cannot lock twice");
				}
				waiters++;
				try {
					wait();
				} catch (InterruptedException e) {
					throw new InterruptedIOException(e);
				} finally {
					waiters--;
				}
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
         * 'high-priority' request (<code>true</code>) or a
         * 'low-priority' request (<code>false</code>).
         *
         * @return a boolean value indicating whether the lock has
         * been successfully acquired or not.
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
			throw new IllegalMonitorStateException("Cannot unlock from not owner");
		}
		lockvalue++;
		if (waiters > 0) {
			notify();
			// notifyAll();
		}
	}
}

