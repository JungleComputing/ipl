package ibis.util;

class PoolThread extends Thread {
	Queue q;

	static int id = 0;

	int myId;

	PoolThread(Queue q) {
		this.q = q;
		synchronized(q) {
			if (ThreadPool.DEBUG) {
				System.out.println("Create new poolthread, numthreads = " +
						   ThreadPool.numthreads);
			}
			ThreadPool.numthreads++;
			myId = id++;
		}
	}

	public void run() {
		while(true) {
			//set name here in case a target changes it
			setName("Pool thread " + myId);

			Runnable target = null;
			synchronized(q) {
				ThreadPool.ready++;
				if (q.size() == 0) {
					if (2 * ThreadPool.ready > ThreadPool.numthreads) {
						if (ThreadPool.DEBUG) {
							System.out.println("Poolthread exits, numthreads = " + ThreadPool.numthreads);
						}
						ThreadPool.numthreads--;
						ThreadPool.ready--;
						return;
					}
				}
				target = (Runnable) q.dequeue();
				ThreadPool.ready--;
			}

			// catch and print all user exceptions.
			// I've seen it happen that a thread was throwing an exception that is not caught here
			// Next, the counters in the queue are not correct anymore -> havoc --Rob 
			try {
				target.run();
			} catch (Throwable t) {
				System.err.println("got exception in pool thread (continuing): " + t);
				t.printStackTrace();
			}

		}
	}
}

/**
 * The <code>ThreadPool</code> class maintains a pool of daemon threads that
 * can be used to run any {@link Runnable}.
 */
public final class ThreadPool {
	static final boolean DEBUG = false;

	static int numthreads = 0;
	static int ready = 0;

	static Queue q = new Queue();

	/**
	 * To prevent creation of a threadpool object.
	 */
	private ThreadPool() {
	}

	/**
	 * Associates a thread from the <code>ThreadPool</code> with the
	 * specified {@link Runnable}. If no thread is available, a new one
	 * is created. When the {@link Runnable} is finished, the thread is
	 * added to the pool of available threads, or, if enough threads are
	 * available, it is discarded.
	 * @param r the <code>Runnable</code> to be executed.
	 */
	public static void createNew(Runnable r) {
		synchronized(q) {
			q.enqueue(r);
			if (ready == 0) {
				PoolThread p = new PoolThread(q);
				p.setDaemon(true);
				p.start();
			}
		}
	}
}
