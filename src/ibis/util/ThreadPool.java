package ibis.ipl.impl.generic;

// also set size with property.
class PoolThread extends Thread {
	Object lock;
	Runnable target;
	boolean IamReady;
	
	PoolThread(Object lock) {
		this.lock = lock;
	}
	
	public void run() {
		while(true) {
			synchronized(lock) {
				if(ThreadPool.ready >= ThreadPool.MAX_CACHE_SIZE) {
					System.err.println("AHAHAHA");
						return;
				}

				if(target == null) {
					IamReady = true;
					ThreadPool.ready++;
				}
				while(target == null) {

//					System.err.println("poolthread: waiting");
					try {
						lock.wait();
					} catch (Exception e) {
						// Ignore.
					}
//					System.err.println("poolthread: wakeup");
				}
			}
			
			target.run();
			target = null;
		}
	}
}

public final class ThreadPool {
	static final int MAX_CACHE_SIZE = 5;
	static final boolean DEBUG = false;

	static PoolThread[] threads = new PoolThread[MAX_CACHE_SIZE];
	static int count = 0;
	static int ready = 0;
	static Object lock = new Object();

	public static void createNew(Runnable r) {
		synchronized(lock) {
			if(ready == 0) {
				if(count >= MAX_CACHE_SIZE) {
					if(DEBUG) {
						System.err.println("pool: no ready thread, cache full, started new one");
					}
					new Thread(r).start();
				} else {
					if(DEBUG) {
						System.err.println("pool: no ready thread, cache not full, started new poolthread");
					}
					threads[count] = new PoolThread(lock);
					threads[count].target = r;
					threads[count].start();
					count++;
				}
				return;
			}

			// there is at least one thread ready!
			for(int i=0; i<count; i++) {
				if(threads[i].IamReady) {
					threads[i].target = r;
					threads[i].IamReady = false;
					ready--;
					lock.notifyAll();
//					System.err.println("pool: notify ready thread");
					return;
				}
			}

			System.err.println("EEK, threadpool, this should not happen!");
			System.exit(1);
		}
	}
}
