package ibis.ipl.impl.net;

/**
 * Provides a synchronization mutex.
 */
public class NetMutex {

	/**
	 * The mutex value, which cannot be negative.
	 */
	private int value = 1;

	/**
	 * Default constructor.
	 *
	 * The mutex initial state is unlocked.
	 */
	public NetMutex() {
		this(false);
	}

	/**
	 * Constructor.
	 *
	 * @param locked specifies whether the initial state of the mutex is
	 * locked (<code>true</code>) or unlocked (<code>false</code>).
	 */
	public NetMutex(boolean locked) {
		value = locked?0:1;
	}

	/**
	 * Attempts to <strong>uninterruptibly</strong> lock the mutex.
	 *
	 * If the mutex is already locked, the function blocks
	 * <strong>uninterruptibly</strong> until the mutex is
	 * released. This function should be used carefully because it
	 * cannot be interrupted (preferably use {@link #ilock} when
	 * possible).
	 */
	synchronized void lock() {
		while (value <= 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				//
			}
		}
		value--;
	}

	/**
	 * Attempts to interruptibly lock the mutex.
	 *
	 * If the mutex is already locked, the function blocks until the mutex is
	 * released or the corresponding thread is interrupted in which case the mutex
	 * is <strong>not</strong> acquired.
	 *
	 * @exception InterruptedException when the corresponding thread is 
	 *            interrupted. The lock is not acquired in this case.
	 */
	synchronized void ilock() throws InterruptedException {
		while (value <= 0) {
			wait();
		}
		value--;
	}

	/** 
	 * Attemps to lock the mutex without blocking.
	 *
	 * @return <code>true</code> if the lock was successfully acquired and
	 *         <code>false</code> otherwise.
	 */
	synchronized boolean trylock() {
		if (value <= 0) {
			return false;
		} else {
			value--;
			return true;
		}
	}

	/**
	 * Unlocks the mutex.
	 *
	 * Note: the <code>unlock</code> may be called multiple times in a row,
	 *       in which case enough successful 
	 *       {@link #lock}/{@link #ilock}/{@link #trylock}
	 *       calls should be performed to actually own the mutex. This use is
	 *       however unsafe and <strong>discouraged</strong>. 
	 *       Use a semaphore instead.
	 */
	synchronized void unlock() {
		value++;
		notifyAll();
	}
}

