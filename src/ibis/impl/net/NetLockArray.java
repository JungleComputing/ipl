package ibis.ipl.impl.net;


public class NetLockArray {

        /**
         * Array of lock values.
         */
        private volatile int []     v = null;

        /**
         * Lock array mask.
         */
        private volatile boolean [] m = null;
        
        public NetLockArray() {
                v = new int[1];
                m = new boolean[1];
                m[0] = false;
        }
        

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

        public synchronized void initLock(int id) {
                initLock(id, false);
        }
        
	public synchronized void lock(int id) {
                // System.err.println("lock: "+id+"-->");
                if (!m[id]) {
                        throw new Error("uninitialized lock");
                }

                // System.err.println("lock: "+id+" = "+v[id]);
		while (v[id] <= 0) {
			try {
				wait();
			} catch (InterruptedException e) {
				//
			}
                        // System.err.println("lock: "+id+" = "+v[id]);
		}
		v[id]--;
                // System.err.println("lock: "+id+" -> "+v[id]);
                // System.err.println("lock: "+id+"<--");
	}

	public synchronized void ilock(int id) throws InterruptedException {
                if (!m[id]) {
                        throw new Error("uninitialized lock");
                }

		while (v[id] <= 0) {
			wait();
		}
		v[id]--;
	}

	public synchronized boolean trylock(int id) {
                // System.err.println("trylock: "+id+"-->");
                if (!m[id]) {
                        throw new Error("uninitialized lock");
                }

                // System.err.println("trylock: "+id+" = "+v[id]);
		if (v[id] <= 0) {
                        // System.err.println("trylock: "+id+"<--");
			return false;
		} else {
			v[id]--;
                        // System.err.println("trylock: "+id+"<-- -> "+v[id]);
			return true;
		}
	}


	public synchronized void lockAll(int [] ids) {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
                        }

                        state = state && (v[ids[i]] > 0);
                }
                
		while (!state) {
			try {
				wait();
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

	public synchronized void ilockAll(int [] ids) throws InterruptedException {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
                        }

                        state = state && (v[ids[i]] > 0);
                }
                
		while (!state) {
                        wait();

                        state = true;
                        for (int i = 0; i < ids.length; i++) {
                                state = state && (v[ids[i]] > 0);
                        }
		}

                for (int i = 0; i < ids.length; i++) {
                        v[ids[i]]--;
                }
	}

	public synchronized boolean trylockAll(int [] ids) {
                boolean state = true;

                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
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

        
	public synchronized int lockFirst(int [] ids) {
                // System.err.println("lockFirst: -->");
                int result = -1;
                
                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
                        }
                }
                
                boolean state = false;

                for (int i = 0; i < ids.length; i++) {
                        // System.err.println("lockFirst: "+ids[i]+" = "+v[ids[i]]);
                        if (v[ids[i]] > 0) {
                                result = i;
                                state  = true;
                                break;
                        }
                }
                
		while (!state) {
			try {
				wait();
			} catch (InterruptedException e) {
				//
			}
                        // System.err.println("lockFirst: ---");

                        state = false;
                        for (int i = 0; i < ids.length; i++) {
                                // System.err.println("lockFirst: "+ids[i]+" = "+v[ids[i]]);
                                if (v[ids[i]] > 0) {
                                        result = i;
                                        state  = true;
                                        break;
                                }
                        }                
		}
                
                v[ids[result]]--;
                // System.err.println("lockFirst: "+ids[result]+" -> "+v[ids[result]]);

                // System.err.println("lockFirst: <-- = "+result);
                return result;
	}

	public synchronized int ilockFirst(int [] ids) throws InterruptedException {
                int result = -1;
                
                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
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

	public synchronized int trylockFirst(int [] ids) {
                // System.err.println("trylockFirst: -->");
                int result = -1;
                
                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
                        }
                }

                for (int i = 0; i < ids.length; i++) {
                        // System.err.println("trylockFirst: "+ids[i]+" = "+v[ids[i]]);

                        if (v[ids[i]] > 0) {
                                result = i;
                                v[ids[result]]--;
                                // System.err.println("trylockFirst: "+ids[i]+" -> "+v[ids[i]]);
                                break;
                        }
                }

                // System.err.println("trylockFirst: <-- -> "+result);
                return result;
	}
        
	public synchronized void unlock(int id) {
                // System.err.println("unlock: "+id+"-->");
                if (!m[id]) {
                        throw new Error("uninitialized lock");
                }

                // System.err.println("unlock: "+id+" = "+v[id]);
		v[id]++;
                // System.err.println("unlock: "+id+" -> "+v[id]);
                // System.err.println("unlock: "+id+"<--");
		notifyAll();
	}

	public synchronized void unlockAll(int [] ids) {
                for (int i = 0; i < ids.length; i++) {
                        if (!m[ids[i]]) {
                                throw new Error("uninitialized lock");
                        }

                        v[ids[i]]++;
                }
                
		notifyAll();
	}
}
