package ibis.impl.net;

import ibis.util.ConditionVariable;
import ibis.util.IllegalLockStateException;
import ibis.util.Monitor;

/**
 * Provide a set of lock that can be changed and tested atomically.
 */
public final class NetLockArray {

    private static final boolean DEBUG = false; // true;

    private class Lock {
	/**
	 * Lock value. <code>v > 0</code>means lock is free.
	 */
	private int	v;

	/**
	 * Lock liveness mask.
	 */
	private boolean m;

	/**
	 * Maintain an interrupt count. On an interrupt, this is set to the
	 * global interrupt count, which is incremented for each interrupt.
	 * Around a cv_wait, we must check if the interrupt count has
	 * increased. If so, we suffered an interrupt, and must throw an
	 * {@link ibis.impl.net.InterruptedIOException InterruptedIOException}.
	 */
	private int	interrupts;

	private ConditionVariable cv;
	private WaitingOn front;
	private WaitingOn tail;

	Lock() {
	    cv = mon.createCV(true);
	}
    };

    private Lock[]	lock;

    private Monitor	mon;

    private int		interrupts;


    /**
     * Construct a lock array with a single lock entry and no live locks.
     */
    public NetLockArray() {
	this(new Monitor());
    }

    /**
     * Construct a lock array with a single lock entry and no live locks.
     *
     * @param mon The {@link ibis.util.Monitor Monitor} that governs access to the lock array.
     */
    public NetLockArray(Monitor mon) {
	this.mon = mon;
	lock = new Lock[1];
	lock[0] = new Lock();
    }

    /**
     * Initialize a new lock.
     *
     * @param id index of the lock into the lock array.
     * @param locked initial state of the new lock.
     */
    public void initLock(int id, boolean locked) {
	mon.checkImOwner();
	if (lock.length <= id) {
	    Lock[] _lock = new Lock[id + 1];

	    System.arraycopy(lock, 0, _lock, 0, lock.length);

	    for (int i = lock.length; i < _lock.length; i++) {
		_lock[i] = new Lock();
	    }

	    lock = _lock;
	}

	if (lock[id].m) {
	    throw new IllegalLockStateException("lock already initialized");
	}

	lock[id].v = locked ? 0 : 1;
	lock[id].m = true;
    }

    /**
     * Initialize a new lock.
     * The new lock is initially unlocked.
     *
     * @param id index of the lock into the lock array.
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

	if (DEBUG) {
	    System.err.print("Enqueue(" + wait_id + "); Wait queue[" + id
		    + "] := (");
	    for (w = q.front; w != null; w = w.next) {
		System.err.print(w.id + ",");
	    }
	    System.err.println(")");
	}
    }


    /**
     * Remove our wait request from the queue.
     * Apparently we have been signalled.
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
	if (DEBUG) {
	    System.err.print("Dequeue(" + wait_id + "); Wait queue[" + id
		    + "] was (");
	    for (WaitingOn w = q.front; w != null; w = w.next) {
		System.err.print(w.id + ",");
	    }
	    System.err.println(")");
	}

	if (scan == null) {
	    throw new Error("Multi-wait queue[" + id + "," + wait_id
		    + "] corrupt");
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
	if (DEBUG) {
	    System.err.print("Dequeue(" + wait_id + "); Wait queue[" + id
		    + "] := (");
	    for (WaitingOn w = q.front; w != null; w = w.next) {
		System.err.print(w.id + ",");
	    }
	    System.err.println(")");
	}
    }


    /**
     * Wait on condition variable that belongs to <code>lock[id]</code>.
     * If the condition variable has been interrupted, its interrupt variable
     * is incremented. In that case, throw an InterruptedIOException.
     */
    private void iwait(int id) throws InterruptedIOException {
	int interrupts = lock[id].interrupts;
	try {
	    lock[id].cv.cv_wait();
	} catch (InterruptedException e) {
	    if (DEBUG) {
		System.err.println(NetIbis.hostName() +
		       	" Thread.interrupt() -> interrupted Id "
		       	+ id + "; intpts " + lock[id].interrupts);
	    }
	    throw new InterruptedIOException(e);
	}
	if (lock[id].interrupts > interrupts) {
	    if (DEBUG) {
		System.err.println(NetIbis.hostName() + " interrupted Id "
		       	+ id + "; intpts " + lock[id].interrupts);
	    }
	    throw new InterruptedIOException();
	}
    }


    /**
     * Lock one of the array's locks.
     * No support for interrupt of the lock operation.
     *
     * @param id indicates the index of the lock.
     *
     * @exception IllegalLockStateException if the requested lock has not
     * been initialized or gets discarded while the method is blocked on a
     * {@link #wait}.
     */
    public void lock(int id) {

	mon.checkImOwner();
	if (DEBUG) {
	    System.err.println("enter lock(" + id +")");
	}
	if (!lock[id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}

	while (lock[id].v <= 0) {
	    registerWaitingOn(id, id);
	    try {
		iwait(id);
	    } catch (InterruptedIOException e) {
		// Don't care about interrupts. Retry grabbing the lock.
	    } finally {
		unregisterWaitingOn(id, id);
	    }

	    if (!lock[id].m) {
		throw new IllegalLockStateException("uninitialized lock");
	    }
	}

	lock[id].v--;
	wakeupFront(id);

	if (DEBUG) {
	    System.err.println("locked(" + id +")");
	}
    }


    /**
     * Lock one of the array's locks.
     * Support interrupt of the lock operation.
     *
     * @param id indicates the index of the lock.
     *
     * @exception IllegalLockStateException if the requested lock has not been
     * initialized or gets discarded while the method is blocked on a
     * {@link #wait}.
     * @exception InterruptedIOException if the thread is
     * {@link #interrupt}ed while waiting.
     */
    public void ilock(int id) throws InterruptedIOException {
	mon.checkImOwner();
	if (DEBUG) {
	    System.err.println("enter ilock(" + id +")");
	}

	if (!lock[id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}

	while (lock[id].v <= 0) {
	    registerWaitingOn(id, id);
	    try {
		iwait(id);
	    } finally {
		unregisterWaitingOn(id, id);
	    }
	    if (!lock[id].m) {
		throw new IllegalLockStateException("uninitialized lock");
	    }
	}
	lock[id].v--;
	wakeupFront(id);

	if (DEBUG) {
	    System.err.println("ilocked(" + id +")");
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
     * not been initialized or has been cleared.
     */
    public boolean trylock(int id) {
	mon.checkImOwner();
	if (!lock[id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}

	if (lock[id].v <= 0) {
	    return false;
	}

	if (DEBUG) {
	    System.err.println("trylocked(" + id +")");
	}
	lock[id].v--;

	return true;
    }


    /**
     * Lock the first free lock in the array indexed by <code>ids</code>.
     * No support for interrupt of the lock operation.
     * 
     * @param ids array of indexes into our lock array
     *
     * @return the index into <code>ids</code> of the lock that is taken
     *
     * @exception IllegalLockStateException if the requested lock has
     * not been initialized or has been cleared.
     */
    public int lockFirst(int[] ids) {
	int wait_id = ids[0];

	mon.checkImOwner();
	if (DEBUG) {
	    System.err.print("enter lockFirst(");
	    for (int i = 0; i < ids.length; i++) {
		System.err.print(ids[i] + ",");
	    }
	    System.err.println(")");
	}

	int result = -1;

outer:
	while (true) {
	    for (int i = 0; i < ids.length; i++) {
		if (!lock[ids[i]].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
	    }

	    for (int i = 0; i < ids.length; i++) {
		if (lock[ids[i]].v > 0) {
		    result = i;
		    break outer;
		}
	    }

	    for (int i = 0; i < ids.length; i++) {
		registerWaitingOn(ids[i], wait_id);
	    }
	    try {
		if (DEBUG) {
		    System.err.println("lockFirst wait(" + wait_id + ")");
		}
		iwait(wait_id);
	    } catch (InterruptedIOException e) {
		// Don't care about interrupts. Retry grabbing the lock.
	    } finally {
		for (int i = 0; i < ids.length; i++) {
		    unregisterWaitingOn(ids[i], wait_id);
		}
	    }
	}

	if (DEBUG) {
	    System.err.println("lockFirst(" + ids[result] + ")");
	}
	lock[ids[result]].v--;
	wakeupFront(ids);

	return result;
    }


    /**
     * Lock the first free lock in the array indexed by <code>ids</code>.
     * Support interrupt of the lock operation.
     * 
     * @param ids array of indexes into our lock array
     *
     * @return the index into <code>ids</code> of the lock that is taken
     *
     * @exception InterruptedIOException when some lock that we wait on
     * 		has been {@link #interrupt}ed.
     * @exception IllegalLockStateException if the requested lock has
     * not been initialized or has been cleared.
     */
    public int ilockFirst(int[] ids) throws InterruptedIOException {
	int wait_id = ids[0];

	mon.checkImOwner();
	if (DEBUG) {
	    System.err.print("enter ilockFirst(");
	    for (int i = 0; i < ids.length; i++) {
		System.err.print(ids[i] + ",");
	    }
	    System.err.println(")");
	}

	int result = -1;

outer:
	while (true) {

	    for (int i = 0; i < ids.length; i++) {
		if (!lock[ids[i]].m) {
		    throw new IllegalLockStateException("uninitialized lock");
		}
	    }

	    for (int i = 0; i < ids.length; i++) {
		if (lock[ids[i]].v > 0) {
		    result = i;
		    break outer;
		}
	    }

	    if (DEBUG) {
		System.err.println("ilockFirst wait(" + wait_id + ")");
	    }
	    for (int i = 0; i < ids.length; i++) {
		registerWaitingOn(ids[i], wait_id);
	    }
	    try {
		iwait(wait_id);
	    } finally {
		for (int i = 0; i < ids.length; i++) {
		    unregisterWaitingOn(ids[i], wait_id);
		}
	    }
	}

	if (DEBUG) {
	    System.err.println("ilockFirst(" + ids[result] + ")");
	}
	lock[ids[result]].v--;
	wakeupFront(ids);

	return result;
    }


    /**
     * Try to lock some free lock in the array indexed by <code>ids</code>,
     * with preference for lower indices.
     * 
     * @param ids array of indexes into our lock array
     *
     * @return the index into <code>ids</code> of the lock that is taken or -1
     * 		if none was free
     *
     * @exception IllegalLockStateException if the requested lock has
     * not been initialized or has been cleared.
     */
    public int trylockFirst(int[] ids) {
	mon.checkImOwner();

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
		if (DEBUG) {
		    System.err.print("L(" + i + ")");
		}
		break;
	    }
	}

	return result;
    }


    private void wakeupFront(int id) {
	if (lock[id].v == 0) {
	    // Is not free
	    return;
	}

	if (lock[id].front == null) {
	    // Nobody waiting for us
	    return;
	}

	int	signal_id = lock[id].front.id;

	if (!lock[signal_id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}
	lock[signal_id].cv.cv_signal();
    }


    private void wakeupFront(int[] ids) {
	for (int i = 0; i < ids.length; i++) {
	    wakeupFront(ids[i]);
	}
    }


    /**
     * Unlock the lock in our array indexed by <code>id</code>
     *
     * @param id index into the lock array
     *
     * <br>
     * <standout>NOTE</standout> This method is called from native code.
     * <br>
     * <standout>NOTE</standout> This method is also emulated from native
     * code, for the case where it is known that no notify is necessary.
     * The native code is completely aware of the fields of this and of
     * lock[id]. If anything is changed here, be sure to also check function
     * ni_gm_lock_unlock in gm/net_ibis_gm.c.
     */
    public void unlock(int id) {
	mon.checkImOwner();

	if (!lock[id].m) {
	    throw new IllegalLockStateException("uninitialized lock");
	}
	/* It seems NetIbis allows unlocking of a free lock????? *
	if (lock[id].v > 0) {
	    System.err.println("Unlock free lock");
	    Thread.dumpStack();
	    throw new IllegalLockStateException("Unlock free lock");
	}
	*/

	lock[id].v++;

	if (false && true) {
	    if (lock[id].front != null) {
		WaitingOn w = lock[id].front;
		while (w != null) {
		    if (DEBUG) {
			System.err.println("Signal(" + w.id + ") from unlock("
				+ id +")");
		    }
		    lock[w.id].cv.cv_signal();
		    w = w.next;
		}
	    }
	} else {
	    wakeupFront(id);
	}

	if (DEBUG) {
	    System.err.println("Unlocked(" + id +")");
	}
    }


    /**
     * Delete the lock in our array indexed by <code>id</code>
     *
     * @param id index into the lock array
     */
    public void deleteLock(int id) {
	mon.checkImOwner();
	lock[id].m = false;
	WaitingOn w = lock[id].front;
	while (w != null) {
	    lock[w.id].cv.cv_bcast();
	    w = w.next;
	}
    }


    private void interruptLocked(int id) {
	WaitingOn w = lock[id].front;
	while (w != null) {
	    Lock lck = lock[w.id];
	    if (w.id != 0 && lck.interrupts < interrupts) {
		if (DEBUG) {
		    System.err.println(NetIbis.hostName()
			    + ": interrupt dependent id " + w.id
			    + "; intpts " + interrupts);
		}
		lck.interrupts = interrupts;
		lck.cv.cv_bcast();
	    }
	    w = w.next;
	}
	Lock lck = lock[id];
	if (lck.front != null && lck.interrupts < interrupts && id != 0) {
	    if (DEBUG) {
		System.err.println(NetIbis.hostName()
			+ ": interrupt own id " + id
		       	+ "; intpts " + interrupts);
	    }
	    lck.interrupts = interrupts;
	    lck.cv.cv_bcast();
	}
    }


    /**
     * Interrupt all threads that wait on any of the lock indexed by
     * <code>id</code>. If the threads are in any of the routines that
     * support interrupts ({@link #ilock}, {@link #ilockFirst}), they return
     * from that routine with an {@link InterruptedIOException}.
     *
     * @param id index into the lock array
     */
    public void interrupt(int id) {
	mon.checkImOwner();
	interrupts++;
	interruptLocked(id);
    }


    /**
     * Interrupt all threads that wait on any of the locks indexed by
     * <code>lockIds</code>. If the threads are in any of the routines that
     * support interrupts ({@link #ilock}, {@link #ilockFirst}), they return
     * from that routine with an {@link InterruptedIOException}.
     *
     * @param lockIds indices into the lock array
     */
    public void interrupt(int[] lockIds) {
	mon.checkImOwner();
	interrupts++;
	for (int i = 0; i < lockIds.length; i++) {
	    interruptLocked(lockIds[i]);
	}
    }

}
