package ibis.impl.net;

import ibis.util.IllegalLockStateException;
import ibis.util.ConditionVariable;
import ibis.util.Monitor;

/**
 * Provide a set of lock that can be changed and tested atomically.
 */
public final class NetLockArray {

    private static class Lock {
	/**
	 * Store the lock values.
	 */
	private int	v;

	/**
	 * Store the lock mask.
	 *
	 * Only the lock entries for whom the mask is
	 * <code>true</code> are alive.
	 */
	private boolean m;

	private ConditionVariable cv;
	private WaitingOn front;
	private WaitingOn tail;

	Lock(Monitor mon) {
	    cv = mon.createCV(true);
	}
    };

    private Lock[]	lock;

    private Monitor	mon;


    /**
     * Construct a lock array with a single lock entry and no alive locks.
     */
    public NetLockArray() {
	mon = new Monitor();
	lock = new Lock[1];
	lock[0] = new Lock(mon);
    }

    /**
     * Initialize a new lock.
     *
     * @param id indicates the index of the lock in the lock array.
     * @param locked indicates the initial state of the new lock.
     */
    public void initLock(int id, boolean locked) {
	mon.lock();
	try {
	    if (lock.length <= id) {
		Lock[] _lock = new Lock[id + 1];

		System.arraycopy(lock, 0, _lock, 0, lock.length);

		for (int i = lock.length; i < _lock.length; i++) {
		    _lock[i] = new Lock(mon);
		}

		lock = _lock;
	    }

	    if (lock[id].m) {
		throw new IllegalLockStateException("lock already initialized");
	    }

	    lock[id].v = locked?0:1;
	    lock[id].m = true;
// if (id == 2) {
// System.err.println("Initialize lock(" + id + ") as " + (locked ? "locked" : "unlocked"));
// }
	} finally {
	    mon.unlock();
	}
    }

    /**
     * Initialize a new lock.
     *
     * The new lock is initially unlocked.
     *
     * @param id indicates the index of the lock in the lock array.
     */
    public void initLock(int id) {
	initLock(id, false);
    }


    /**
     * Because of the (i)lockFirst functionality, we cannot just use
     * cv_wait() and cv_signal() -- we must atomically wait on multiple
     * condition variables.
     * Use a datastructure to register the CVs we are waiting for.
     * If one of these CVs is signalled, it must inspect the datastructure
     * and possibly signal a different CV.
     */

    private static class WaitingOn {
	WaitingOn	next;
	int		id;
    }


    private WaitingOn	waitingOnFreelist = null;


    private WaitingOn getWaitingOn(int id) {
	WaitingOn w = waitingOnFreelist;
	if (w == null) {
	    w = new WaitingOn();
	} else {
	    waitingOnFreelist = w.next;
	}
	w.id = id;
	return w;
    }


    private void putWaitingOn(WaitingOn w) {
	w.next = waitingOnFreelist;
	waitingOnFreelist = w;
    }

    
    /**
     * Append our wait request to the queue that might be signalled
     */
    private void registerWaitingOn(int id, int wait_id) {
	WaitingOn w = getWaitingOn(wait_id);

	w.next = null;
	Lock q = lock[id];
	if (q.front == null) {
	    q.front = w;
	} else {
	    q.tail.next = w;
	}
	q.tail = w;
// System.err.print("Enqueue(" + wait_id + "); Wait queue[" + id + "] := (");
// for (w = q.front; w != null; w = w.next) {
    // System.err.print(w.id + ",");
// }
// System.err.println(")");
    }


    /**
     * Apparently we have been signalled. Remove our wait request
     * from the queue.
     */
    private void unregisterWaitingOn(int id, int wait_id) {
	WaitingOn scan;
	WaitingOn prev = null;

	Lock q = lock[id];
	for (scan = q.front; scan != null; scan = scan.next) {
	    if (scan.id == wait_id) {
		break;
	    }
	    prev = scan;
	}
// System.err.print("Dequeue(" + wait_id + "); Wait queue[" + id + "] was (");
// for (WaitingOn w = q.front; w != null; w = w.next) {
    // System.err.print(w.id + ",");
// }
// System.err.println(")");

	if (scan == null) {
	    throw new Error("Multi-wait queue[" + id + "," + wait_id + "] corrupt");
	}

	if (prev == null) {
	    q.front = scan.next;
	} else {
	    prev.next = scan.next;
	}
	if (scan == q.tail) {
	    if (prev == null) {
		q.tail = q.front;
	    } else {
		q.tail = prev;
	    }
	}

	putWaitingOn(scan);
// System.err.print("Dequeue(" + wait_id + "); Wait queue[" + id + "] := (");
// for (WaitingOn w = q.front; w != null; w = w.next) {
    // System.err.print(w.id + ",");
// }
// System.err.println(")");
    }


    /**
     * Lock one of the array's locks.
     *
     * @param id indicates the index of the lock.
     *
     * @exception IllegalLockStateException if the requested lock has not been initialized or gets discarded while the method is blocked on a {@link #wait}.
     * @exception InterruptedIOException if the thread is
     * interrupted while waiting. 
     */
    public void lock(int id) throws InterruptedIOException {

	mon.lock();
// System.err.println("enter lock(" + id +")");
	try {
	    if (!lock[id].m) {
		throw new IllegalLockStateException("uninitialized lock");
	    }

	    while (lock[id].v <= 0) {
		registerWaitingOn(id, id);
		try {
		    lock[id].cv.cv_wait();
		} catch (InterruptedException e) {
		    throw new InterruptedIOException(e);
		} finally {
		    unregisterWaitingOn(id, id);
		}

		if (!lock[id].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
	    }

// if (id == 2) {
// System.err.println("lock(" + id + ")");
// Thread.dumpStack();
// }
	    lock[id].v--;
// System.err.println("locked(" + id +")");

	} finally {
	    mon.unlock();
	}
    }

    /**
     * Lock one of the array's locks.
     *
     * @param id indicates the index of the lock.
     *
     * @exception IllegalLockStateException if the requested lock has not been initialized or gets discarded while the method is blocked on a {@link #wait}.
     * @exception InterruptedIOException if the thread is
     * interrupted while waiting.
     */
    public void ilock(int id) throws InterruptedIOException {
	mon.lock();
// System.err.println("enter ilock(" + id +")");

	try {

	    if (!lock[id].m) {
		throw new IllegalLockStateException("uninitialized lock");
	    }

	    while (lock[id].v <= 0) {
		registerWaitingOn(id, id);
		try {
		    lock[id].cv.cv_wait();
		} catch (InterruptedException e) {
		    throw new InterruptedIOException(e);
		} finally {
		    unregisterWaitingOn(id, id);
		}
		if (!lock[id].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
	    }
	    lock[id].v--;
// if (id == 2) {
// System.err.println("ilock(" + id + ")");
// Thread.dumpStack();
// }
// System.err.println("ilocked(" + id +")");

	} finally {
	    mon.unlock();
	}
    }


    /**
     * Attempt to lock one of the array's locks.
     *
     * @param id indicates the index of the lock.
     *
     * @return true if the lock was successfully acquired, false otherwise.
     *
     * @exception IllegalLockStateException if the requested lock has
     * not been initialized
     */
    public boolean trylock(int id) {
	mon.lock();
	try {
	    if (!lock[id].m) {
		throw new IllegalLockStateException("uninitialized lock");
	    }

	    if (lock[id].v <= 0) {
		return false;
	    }

// System.err.println("trylocked(" + id +")");
// if (id == 2) {
// System.err.println("trylock(" + id + ")");
// Thread.dumpStack();
// }
	    lock[id].v--;

	    return true;

	} finally {
	    mon.unlock();
	}
    }


    /*
     * Atomically --- but interruptibly ---  acquire a whole set of lock entries.
     *
     * @param ids stores the set of entry indexes.
     *
     * @exception IllegalLockStateException if one of the requested locks has
     * not been initialized or or got discarded while the method is
     * blocked on a {@link #wait}.
     * @exception InterruptedIOException if the thread is
     * interrupted while waiting.
     */
    public void lockAll(int[] ids) throws InterruptedIOException {
	throw new RuntimeException("lockAll is not implemented");
    }


    /**
     * Atomically --- but interruptibly --- acquire a whole set of lock entries.
     *
     * @param ids stores the set of entry indexes.
     *
     * @exception IllegalLockStateException if one of the requested locks has
     * not been initialized or or got discarded while the method is
     * blocked on a {@link #wait}.
     * @exception InterruptedIOException if the thread is
     * interrupted while waiting.
     */
    public void ilockAll(int[] ids) throws InterruptedIOException {
	throw new RuntimeException("ilockAll is not implemented");
    }


    public boolean trylockAll(int[] ids) {
	throw new RuntimeException("trylockAll is not implemented");
    }


    public synchronized int lockFirst(int[] ids) throws InterruptedIOException {
	int wait_id = ids[0];

	mon.lock();
// System.err.print("enter lockFirst(");
// for (int i = 0; i < ids.length; i++) {
    // System.err.print(ids[i] + ",");
// }
// System.err.println(")");
	try {
	    int result = -1;

	    boolean state = false;

	    while (true) {
		for (int i = 0; i < ids.length; i++) {
		    if (!lock[ids[i]].m) {
			throw new IllegalLockStateException("uninitialized lock");
		    }
		}

		for (int i = 0; i < ids.length; i++) {
		    if (lock[ids[i]].v > 0) {
			result = i;
			state  = true;
			break;
		    }
		}

		if (state) {
		    break;
		}

		for (int i = 0; i < ids.length; i++) {
		    registerWaitingOn(ids[i], wait_id);
		}
		try {
// System.err.println("lockFirst wait(" + wait_id + ")");
		    lock[wait_id].cv.cv_wait();
		} catch (InterruptedException e) {
		    throw new InterruptedIOException(e);
		} finally {
		    for (int i = 0; i < ids.length; i++) {
			unregisterWaitingOn(ids[i], wait_id);
		    }
		}
	    }

// if (ids[result] == 2) {
// System.err.println("lockFirst(" + ids[result] + ")");
// Thread.dumpStack();
// }
// System.err.println("lockFirst(" + ids[result] + ")");
	    lock[ids[result]].v--;

	    return result;
	} finally {
	    mon.unlock();
	}
    }

    public int ilockFirst(int[] ids) throws InterruptedIOException {
	int wait_id = ids[0];

	mon.lock();
// System.err.print("enter ilockFirst(");
// for (int i = 0; i < ids.length; i++) {
    // System.err.print(ids[i] + ",");
// }
// System.err.println(")");
	try {
	    int result = -1;

	    boolean state = false;

	    while (true) {

		for (int i = 0; i < ids.length; i++) {
		    if (!lock[ids[i]].m) {
			throw new IllegalLockStateException("uninitialized lock");
		    }
		}

		for (int i = 0; i < ids.length; i++) {
		    if (lock[ids[i]].v > 0) {
			result = i;
			state  = true;
			break;
		    }
		}

		if (state) {
		    break;
		}

// System.err.println("ilockFirst wait(" + wait_id + ")");
		for (int i = 0; i < ids.length; i++) {
		    registerWaitingOn(ids[i], wait_id);
		}
		try {
		    lock[wait_id].cv.cv_wait();
		} catch (InterruptedException e) {
		    throw new InterruptedIOException(e);
		} finally {
		    for (int i = 0; i < ids.length; i++) {
			unregisterWaitingOn(ids[i], wait_id);
		    }
		}
	    }

// if (ids[result] == 2) {
// System.err.println("ilockFirst(" + ids[result] + ")");
// Thread.dumpStack();
// }
// System.err.println("ilockFirst(" + ids[result] + ")");
	    lock[ids[result]].v--;

	    return result;
	} finally {
	    mon.unlock();
	}
    }


    public int trylockFirst(int[] ids) {
	mon.lock();
	try {
	    int		result = -1;

	    for (int i = 0; i < ids.length; i++) {
		if (!lock[ids[i]].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
	    }

	    for (int i = 0; i < ids.length; i++) {

		if (lock[ids[i]].v > 0) {
		    result = i;
		    lock[ids[result]].v--;
// System.err.print("L(" + i + ")");
// if (ids[result] == 2) {
// System.err.println("trylockFirst(" + ids[result] + ")");
// Thread.dumpStack();
// }
		    break;
		}
	    }

	    return result;
	} finally {
	    mon.unlock();
	}
    }


    private void unlockLocked(int id) {
	if (!lock[id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}
	/** It seems NetIbis allows unlocking of a free lock?????
	if (lock[id].v > 0) {
System.err.println("Unlock free lock");
Thread.dumpStack();
	    throw new IllegalLockStateException("Unlock free lock");
	}
	*/

	if (lock[id].front != null) {
	    if (false && true) {
		WaitingOn w = lock[id].front;
		while (w != null) {
// System.err.println("Signal(" + w.id + ") from unlock(" + id +")");
		    lock[w.id].cv.cv_signal();
		    w = w.next;
		}
	    } else {
		int	signal_id = lock[id].front.id;

		if (!lock[signal_id].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
		lock[signal_id].cv.cv_signal();
	    }
	}

// System.err.println("Unlocked(" + id +")");
// if (id == 2) {
// System.err.println("unlock(" + id + ")");
// Thread.dumpStack();
// }
	lock[id].v++;
    }


    public void unlock(int id) {
	mon.lock();
	try {
	    unlockLocked(id);
	} finally {
	    mon.unlock();
	}
    }


    public void unlockAll(int [] ids) {
	mon.lock();
	try {
	    for (int i = 0; i < ids.length; i++) {
		unlockLocked(ids[i]);
	    }
	} finally {
	    mon.unlock();
	}
    }


    public void deleteLock(int id) {
	mon.lock();
	try {
	    lock[id].m = false;
	    WaitingOn w = lock[id].front;
	    while (w != null) {
		lock[w.id].cv.cv_bcast();
		w = w.next;
	    }
	} finally {
	    mon.unlock();
	}
    }

}
