package ibis.ipl.impl.net;

/**
 * Provide a set of lock that can be changed and tested atomically.
 */
public final class NetLockArray {

        /**
         * Store the lock values.
         */
        private volatile int []     v = null;

        /**
         * Store the lock mask.
         *
         * Only the lock entries for whom the mask is
         * <code>true</code> are alive.
         */
        private volatile boolean [] m = null;

        /**
         * Construct a lock array with a single lock entry and no alive locks.
         */
        public NetLockArray() {
                v = new int[1];
                m = new boolean[1];
                m[0] = false;
        }

        /**
         * Initialize a new lock.
         *
         * @param id indicates the index of the lock in the lock array.
         * @param locked indicates the initial state of the new lock.
         */
        public synchronized void initLock(int id, boolean locked) {
                if (v.length <= id) {
                        int []     _v = new int[id+1];
                        boolean [] _m = new boolean[id+1];

                        System.arraycopy(v, 0, _v, 0, v.length);
                        System.arraycopy(m, 0, _m, 0, m.length);

                        for (int i = m.length; i < _m.length; i++) {
                                _m[i] = false;
                        }

                        v = _v;
                        m = _m;
                }

                if (m[id]) {
                        throw new Error("lock already initialized");
                }

                v[id] = locked?0:1;
                m[id] = true;
        }

        /**
         * Initialize a new lock.
         *
         * The new lock is initially unlocked.
         *
         * @param id indicates the index of the lock in the lock array.
         */
        public synchronized void initLock(int id) {
                initLock(id, false);
        }

        /**
         * Lock one of the array's locks.
         *
         * @param id indicates the index of the lock.
         *
         * @exception NetIbisClosedException if the requested lock has not been initialized or gets discarded while the method is blocked on a {@link #wait}.
         */
	public synchronized void lock(int id) throws NetIbisException {
                if (!m[id]) {
                        throw new NetIbisClosedException("uninitialized lock");
                }

		while (v[id] <= 0) {
			try {
				wait();
                                if (!m[id]) {
                                        throw new NetIbisClosedException("uninitialized lock");
                                }
			} catch (InterruptedException e) {
				//
			}

		}
		v[id]--;
	}

        /**
         * Interruptibly lock one of the array's locks.
         *
         * @param id indicates the index of the lock.
         *
         * @exception NetIbisClosedException if the requested lock has not been initialized or gets discarded while the method is blocked on a {@link #wait}.
         * @exception InterruptedException if the thread is
         * interrupted while waiting.
         */
	public synchronized void ilock(int id) throws InterruptedException, NetIbisException {
                if (!m[id]) {
                        throw new NetIbisClosedException("uninitialized lock");
                }

		while (v[id] <= 0) {
			wait();
                        if (!m[id]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }
		}
		v[id]--;
	}

        /**
         * Attempt to lock one of the array's locks.
         *
         * @param id indicates the index of the lock.
         *
         * @return true if the lock was successfully acquired, false otherwise.
         *
         * @exception NetIbisClosedException if the requested lock has
         * not been initialized
         */
	public synchronized boolean trylock(int id) throws NetIbisException {
                if (!m[id]) {
                        throw new NetIbisClosedException("uninitialized lock");
                }

		if (v[id] <= 0) {
			return false;
		} else {
			v[id]--;
			return true;
		}
	}

        /**
         * Atomically acquire a whole set of lock entries.
         *
         * @param ids stores the set of entry indexes.
         *
         * @exception NetIbisClosedException if one of the requested locks has
         * not been initialized or or got discarded while the method is
         * blocked on a {@link #wait}.
         */
	public synchronized void lockAll(int [] ids) throws NetIbisException {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }

                        state = state && (v[ids[i]] > 0);
                }

		while (!state) {
			try {
				wait();
                                for (int i = 0; i < ids.length; i++) {
                                        if (!m[ids[i]]) {
                                                throw new NetIbisClosedException("uninitialized lock");
                                        }
                                }
			} catch (InterruptedException e) {
				//
			}

                        state = true;
                        for (int i = 0; i < ids.length; i++) {
                                state = state && (v[ids[i]] > 0);
                        }
		}

                for (int i = 0; i < ids.length; i++) {
                        v[ids[i]]--;
                }
	}

        /**
         * Atomically --- but interruptibly --- acquire a whole set of lock entries.
         *
         * @param ids stores the set of entry indexes.
         *
         * @exception NetIbisClosedException if one of the requested locks has
         * not been initialized or or got discarded while the method is
         * blocked on a {@link #wait}.
         */
	public synchronized void ilockAll(int [] ids) throws InterruptedException, NetIbisException {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }

                        state = state && (v[ids[i]] > 0);
                }

		while (!state) {
                        wait();
                        for (int i = 0; i < ids.length; i++) {
                                if (!m[ids[i]]) {
                                        throw new NetIbisClosedException("uninitialized lock");
                                }
                        }

                        state = true;
                        for (int i = 0; i < ids.length; i++) {
                                state = state && (v[ids[i]] > 0);
                        }
		}

                for (int i = 0; i < ids.length; i++) {
                        v[ids[i]]--;
                }
	}

	public synchronized boolean trylockAll(int [] ids) throws NetIbisException {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }

                        state = state && (v[ids[i]] > 0);
                }

                if (state) {
                        for (int i = 0; i < ids.length; i++) {
                                v[ids[i]]--;
                        }
                }

                return state;
	}


	public synchronized int lockFirst(int [] ids) throws NetIbisException {
                int result = -1;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }
                }

                boolean state = false;

                for (int i = 0; i < ids.length; i++) {
                        if (v[ids[i]] > 0) {
                                result = i;
                                state  = true;
                                break;
                        }
                }

		while (!state) {
			try {
				wait();
                                for (int i = 0; i < ids.length; i++) {
                                        if (!m[ids[i]]) {
                                                throw new NetIbisClosedException("uninitialized lock");
                                        }
                                }
			} catch (InterruptedException e) {
				//
			}

                        state = false;
                        for (int i = 0; i < ids.length; i++) {
                                if (v[ids[i]] > 0) {
                                        result = i;
                                        state  = true;
                                        break;
                                }
                        }
		}

                v[ids[result]]--;

                return result;
	}

	public synchronized int ilockFirst(int [] ids) throws InterruptedException, NetIbisException {
                int result = -1;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }
                }

                boolean state = false;

                for (int i = 0; i < ids.length; i++) {
                        if (v[ids[i]] > 0) {
                                result = i;
                                state  = true;
                                break;
                        }
                }

		while (!state) {
                        wait();
                        for (int i = 0; i < ids.length; i++) {
                                if (!m[ids[i]]) {
                                        throw new NetIbisClosedException("uninitialized lock");
                                }
                        }

                        state = false;
                        for (int i = 0; i < ids.length; i++) {
                                if (v[ids[i]] > 0) {
                                        result = i;
                                        state  = true;
                                        break;
                                }
                        }
		}

                v[ids[result]]--;

                return result;
	}

	public synchronized int trylockFirst(int [] ids) throws NetIbisException {
                int result = -1;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }
                }

                for (int i = 0; i < ids.length; i++) {

                        if (v[ids[i]] > 0) {
                                result = i;
                                v[ids[result]]--;
                                break;
                        }
                }

                return result;
	}

	public synchronized void unlock(int id) throws NetIbisException {
                if (!m[id]) {
                        throw new NetIbisClosedException("uninitialized lock");
                }

		v[id]++;
		notifyAll();
	}

	public synchronized void unlockAll(int [] ids) throws NetIbisException {
                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new NetIbisClosedException("uninitialized lock");
                        }

                        v[ids[i]]++;
                }

		notifyAll();
	}

        public synchronized void deleteLock(int id) {
                m[id] = false;
		notifyAll();
        }

}
