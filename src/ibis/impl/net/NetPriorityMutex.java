package ibis.ipl.impl.net;

/**
 * Provide a special kind of mutex with a 2-level priority support.
 */
public final class NetPriorityMutex {

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
         * @exception NetIbisInterruptedException if the calling thread is
         * interrupted while the method is blocked waiting for the
         * mutex.
         */
	public synchronized void lock(boolean priority) throws NetIbisInterruptedException{
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        synchronized(this) {
                                                priorityvalue--;
                                                notifyAll();
                                        }
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
                        priorityvalue--;
                } else {
                        while (priorityvalue > 0 || lockvalue <= 0) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
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
         * @exception InterruptedException if the calling thread is
         * interrupted while the method is blocked waiting for the
         * mutex.
         */
	public synchronized void ilock(boolean priority) throws InterruptedException {
                if (priority) {
                        priorityvalue++;
                        while (lockvalue <= 0) {
                                try {
                                        wait();
                                } finally {
                                        synchronized(this) {
                                                priorityvalue--;
                                                notifyAll();
                                        }
                                }
                        }
                        priorityvalue--;
                } else {
                        while (priorityvalue > 0 || lockvalue <= 0) {
                                wait();
                        }
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

                lockvalue--;
                return true;
	}

        /**
         * Unlock the mutex.
         */
	public synchronized void unlock() {
		lockvalue++;
		notifyAll();
	}
}

