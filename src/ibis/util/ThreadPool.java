package ibis.ipl.impl.generic;

class PoolThread extends Thread {
    Queue q;
	
    PoolThread(Queue q) {
	this.q = q;
	synchronized(q) {
	    if (ThreadPool.DEBUG) {
		System.out.println("Create new poolthread, numthreads = " + ThreadPool.numthreads);
	    }
	    ThreadPool.numthreads++;
	}
	setName("Pool thread");
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
		}
		else {
		    target = (Runnable) q.dequeue();
		}
	    }

	    target.run();
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
