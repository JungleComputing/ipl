package ibis.util;

/**
 * The <code>ThreadPool</code> class maintains a pool of daemon threads that
 * can be used to run any {@link Runnable}.
 *
 * @author Rob van Nieuwpoort
 * @author Ceriel Jacobs did debugging I think
 * @author Rutger Hofman new implementation without Queue but with an array.
 * 	   This makes adding Thread.name easy: the Queue would have to
 * 	   manipulate tuples => even more GC overhead.
 * 	   Also make PoolThread a static inner class.
 */
public final class ThreadPool {

    private static final boolean DEBUG = false;

    // static int		numthreads = 0;
    // static int		ready = 0;

    // index of next available pool thread
    private static int poolPtr = 0;

    private static final int minPoolSize = 4;

    private static int poolSize;

    // dequeue from N - 1 downwards
    // Do lazy creation of pool threads, but bound fixed at poolSize.
    private static PoolThread[] pool;

    // Might also synchronize on pool but if we ever want to make a flexy-size
    // pool we are going to need a separate lock
    private static final Object lock = new Object();

    static {
        int size;
        try {
            PoolInfo info = PoolInfo.createPoolInfo();
            size = info.size() + minPoolSize;
            info = null;
        } catch (RuntimeException e) {
            // No pool, just guess
            size = 32;
        }
        poolSize = size;
        pool = new PoolThread[poolSize];
        poolPtr = 0;
    }

    /**
     * Double the pool size. Currently unused, but if we ever want to make a
     * flexy-size pool..
     */
    private static void grow() {
        synchronized (lock) {
            int newPoolSize = 2 * poolSize;
            PoolThread[] newPool = new PoolThread[newPoolSize];
            System.arraycopy(pool, 0, newPool, 0, poolSize);
            poolSize = newPoolSize;
            pool = newPool;
        }
    }

    /**
     * Halve the pool size. Currently unused, but if we ever want to make a
     * flexy-size pool..
     */
    private static void shrink() {
        synchronized (lock) {
            poolSize = poolSize / 2;
        }
    }

    private static class PoolThread extends Thread {

        private Runnable runnable;

        PoolThread() {
            // numthreads++;
        }

        public void run() {
            while (true) {
                Runnable runnable;

                synchronized (this) {
                    while (this.runnable == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    }

                    runnable = this.runnable;
                    this.runnable = null;
                }

                try {
                    // Perform the task we were assigned
                    runnable.run();
                } catch (Throwable t) {
                    /* Is this still true with the pool[] implementation?
                     * 						RFHH */
                    // catch and print all user exceptions.
                    // I've seen it happen that a thread was throwing an
                    // exception that is not caught here. Next, the counters
                    // in the queue are not correct anymore -> havoc
                    // --Rob 
                    System.err.println(
                            "exception in pool thread (continuing): " + t);
                    t.printStackTrace();
                }

                synchronized (lock) {
                    // ready++;
                    if (
                    // This is Rob's heuristics. I am afraid I don't
                    // understand. What if ready=1 and numthreads=1?
                    // q.size() == 0 && 2 * ready > numthreads
                    poolPtr == poolSize - 1) {
                        // ready--;
                        // numthreads--;
                        if (DEBUG) {
                            System.out.println("Poolthread exits");
                        }

                        /* No afterlife for me, too many ready threads */
                        return;
                    }

                    /* Enqueue us in the pool for the next spawn */
                    pool[poolPtr] = this;
                    poolPtr++;
                    setName("Sleeping pool thread");
                }
            }
        }

        synchronized void spawn(Runnable r, String name) {
            setName(name);
            runnable = r;
            notify();
        }

    }

    /**
     * Prevent creation of a threadpool object.
     */
    private ThreadPool() {
        /* do nothing */
    }

    /**
     * Associates a thread from the <code>ThreadPool</code> with the
     * specified {@link Runnable}. If no thread is available, a new one
     * is created. When the {@link Runnable} is finished, the thread is
     * added to the pool of available threads, or, if enough threads are
     * available, it is discarded.
     *
     * @param r the <code>Runnable</code> to be executed.
     */
    public static void createNew(Runnable r) {
        createNew(r, "Anonymous pool thread");
    }

    /**
     * Associates a thread from the <code>ThreadPool</code> with the
     * specified {@link Runnable}. If no thread is available, a new one
     * is created. When the {@link Runnable} is finished, the thread is
     * added to the pool of available threads, or, if enough threads are
     * available, it is discarded.
     *
     * @param r the <code>Runnable</code> to be executed.
     * @param name set the thread name for the duration of this run
     */
    public static void createNew(Runnable r, String name) {
        PoolThread t;
        synchronized (lock) {
            if (poolPtr == 0 || pool[poolPtr] == null) {
                t = new PoolThread();
                t.setDaemon(true);
                t.start();
            } else {
                --poolPtr;
                t = pool[poolPtr];
            }
        }
        t.spawn(r, name);
    }

}
