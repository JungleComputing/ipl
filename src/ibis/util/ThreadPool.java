package ibis.ipl.impl.generic;

class PoolThread extends Thread {
	Queue q;

	static int id = 0;

	PoolThread(Queue q) {
		int myId;
		this.q = q;
		synchronized(q) {
			if (ThreadPool.DEBUG) {
				System.out.println("Create new poolthread, numthreads = " +
						   ThreadPool.numthreads);
			}
			ThreadPool.numthreads++;
			myId = id++;
		}
		setName("Pool thread " + myId);
	}

	public void run() {
		while(true) {
			Runnable target = null;
			synchronized(q) {
				if (q.size() == 0) {
					ThreadPool.ready++;
					if (2 * ThreadPool.ready > ThreadPool.numthreads) {
						if (ThreadPool.DEBUG) {
							System.out.println("Poolthread exits, numthreads = " + ThreadPool.numthreads);
						}
						ThreadPool.numthreads--;
						ThreadPool.ready--;
						return;
					}
					target = (Runnable) q.dequeue();
					ThreadPool.ready--;
				} else {
					target = (Runnable) q.dequeue();
				}
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

public final class ThreadPool {
	static final boolean DEBUG = false;

	static int numthreads = 0;
	static int ready = 0;

	static Queue q = new Queue();

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
