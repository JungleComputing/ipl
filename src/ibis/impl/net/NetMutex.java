package ibis.impl.net;

import ibis.util.TypedProperties;

/**
 * Provide a synchronization mutex.
 */
public final class NetMutex {

        /**
         * Activate the debugging code.
         */
	private static final boolean DEBUG = TypedProperties.booleanProperty(NetIbis.mutex_debug);

        /**
         * Store the current mutex owner when DEBUG is set to
         * <code>true</code>.
         */
	private Thread owner = null;
	private StackTraceElement[] lockerTrace = null;

	/**
	 * Store the mutex value, which cannot be negative.
	 */
	private int value = 1;

	private int waiters;

	/**
	 * Construct an initially unlocked mutex.
	 */
	public NetMutex() {
		this(false);
	}

	/**
	 * Construct a mutex.
	 *
	 * @param locked specifies whether the initial state of the mutex is
	 * locked (<code>true</code>) or unlocked (<code>false</code>).
	 */
	public NetMutex(boolean locked) {
		value = locked?0:1;
		if (DEBUG) {
		    if (locked) {
			owner = Thread.currentThread();
			lockerTrace = new Throwable().getStackTrace();
		    }
		}
	}

	/**
	 * Attempt to lock the mutex.
	 *
	 * If the mutex is already locked, the function blocks until
	 * the mutex is released or the corresponding thread is
	 * interrupted in which case the mutex is <strong>not</strong>
	 * acquired.
	 *
	 * @exception InterruptedIOException when the corresponding thread is
	 *            interrupted. The lock is not acquired in this case.
         */
	public synchronized void lock() throws InterruptedIOException {
		while (value <= 0) {
			waiters++;
			try {
				wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException(e);
			} finally {
				waiters--;
			}
		}
		value--;
		if (DEBUG) {
		    owner = Thread.currentThread();
		    lockerTrace = new Throwable().getStackTrace();
		}
	}

	/**
	 * Attempts to interruptibly lock the mutex.
	 *
	 * If the mutex is already locked, the function blocks until
	 * the mutex is released or the corresponding thread is
	 * interrupted in which case the mutex is <strong>not</strong>
	 * acquired.
	 *
	 * @exception InterruptedIOException when the corresponding thread is
	 *            interrupted. The lock is not acquired in this case.
	 */
	public synchronized void ilock() throws InterruptedIOException {
		while (value <= 0) {
			waiters++;
			try {
				wait();
			} catch (InterruptedException e) {
				throw new InterruptedIOException(e);
			} finally {
				waiters--;
			}
		}
		value--;
		if (DEBUG) {
		    owner = Thread.currentThread();
		    lockerTrace = new Throwable().getStackTrace();
		}
	}

	/**
	 * Attempt to lock the mutex without blocking.
	 *
	 * @return <code>true</code> if the lock was successfully acquired and
	 *         <code>false</code> otherwise.
	 */
	public synchronized boolean trylock() {
		if (value <= 0) {
			return false;
		} else {
			value--;
			if (DEBUG) {
			    owner = Thread.currentThread();
			    lockerTrace = new Throwable().getStackTrace();
			}
			return true;
		}
	}

	/**
	 * Unlock the mutex.
	 *
	 * Note: the <code>unlock</code> may be called multiple times in a row,
	 *       in which case enough successful
	 *       {@link #lock}/{@link #ilock}/{@link #trylock}
	 *       calls should be performed to actually own the mutex. This use is
	 *       however unsafe and <strong>discouraged</strong>.
	 *       Use a semaphore instead.
	 */
	public synchronized void unlock() {
		value++;
		if (waiters > 0) {
		    // notifyAll();
		    notify();
		}
	}
}

