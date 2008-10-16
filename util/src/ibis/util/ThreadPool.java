/* $Id$ */

package ibis.util;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threadpool which uses timeouts to determine the number of threads.
 * There is no maximum number of threads in this pool, to prevent deadlocks.
 *
 * @author Niels Drost.
 */
public final class ThreadPool {

    static final Logger logger = LoggerFactory.getLogger(ThreadPool.class);
    
    private static final class PoolThread extends Thread {

        static {
            Runtime.getRuntime().addShutdownHook(new ThreadPoolShutdown());
        }

        private static final class ThreadPoolShutdown extends Thread {
            public void run() {
                Logger logger = LoggerFactory.getLogger(ThreadPool.class);
                logger.info("maximum number of simultaneous threads was: " + maxSimultaneousThreads);
            }
        }
        
        private static final int TIMEOUT = 30 * 1000; //30 seconds 

        Runnable work = null;

        String name = null;

        boolean expired = false;

        private static int nrOfThreads = 0;

        private static int maxSimultaneousThreads = 0;
        
        private static synchronized void newThread(String name) {
            nrOfThreads++;
            if(nrOfThreads > maxSimultaneousThreads) {
                maxSimultaneousThreads = nrOfThreads;
            }
            logger.debug("New Thread \"" + name + "\" createded, number of threads now: "  + nrOfThreads);
        }

        private static synchronized void threadGone() {
            nrOfThreads--;
            logger.debug("Thread removed from pool. Now " + nrOfThreads + " threads");
        }

        @SuppressWarnings("unused")
        private PoolThread() {
            //DO NOT USE
        }

        PoolThread(Runnable runnable, String name) {
            this.work = runnable;
            this.name = name;

             if (logger.isInfoEnabled()) {
                 newThread(name);
             }
        }

        private synchronized boolean issue(Runnable newWork, String newName) {
            if (expired) {
                logger.debug("issue(): thread has expired");
                return false;
            }

            if (this.work != null) {
                throw new Error("tried to issue work to already running"
                        + " poolthread");
            }

            work = newWork;
            name = newName;
            logger.debug("issue(): reusing thread");
            
            notifyAll();
            return true;
        }

        public void run() {
            while (true) {
                Runnable currentWork;
                String currentName;

                synchronized (this) {
                    if (this.work == null) {
                        waiting(this);
                        try {
                            wait(TIMEOUT);
                        } catch (InterruptedException e) {
                            expired = true;
                            if (logger.isInfoEnabled()) {
                                threadGone();
                            }
                            return;
                        }
                    }
                    if (this.work == null) {
                        //still no work, exit
                        expired = true;
                        if (logger.isInfoEnabled()) {
                            threadGone();
                        }
                        return;
                    }
                    currentWork = this.work;
                    currentName = this.name;
                }
                try {
                    setName(currentName);
                    currentWork.run();
                } catch (Throwable t) {
                    logger.error("caught exception in pool thread " + currentName, t);
                    // Exit, rather than continue. A thread died unexpectedly,
                    // after all. If you dont want this, catch all throwables
                    // yourself.
                    // Throwing an exception here makes no sense: it will only
                    // kill the thread.
                    System.exit(1);
                }
                synchronized (this) {
                    this.work = null;
                    this.name = null;
                }
            }
        }
    }

    //list of waiting Poolthreads
    private static final LinkedList<PoolThread> threadPool
            = new LinkedList<PoolThread>();

    /**
     * Prevent creation of a threadpool object.
     */
    private ThreadPool() {
        //DO NOT USE
    }

    private static synchronized void waiting(PoolThread thread) {
        threadPool.add(thread);
    }

    /**
     * Associates a thread from the <code>ThreadPool</code> with the
     * specified {@link Runnable}. If no thread is available, a new one
     * is created. When the {@link Runnable} is finished, the thread is
     * added to the pool of available threads.
     *
     * @param runnable the <code>Runnable</code> to be executed.
     * @param name set the thread name for the duration of this run
     */
    public static synchronized void createNew(Runnable runnable, String name) {
        PoolThread poolThread;

        if (!threadPool.isEmpty()) {
            poolThread = (PoolThread) threadPool.removeLast();
            if (poolThread.issue(runnable, name)) {
                //issue of work succeeded, return
                return;
            }
            //shortest waiting poolThread in list timed out, 
            //assume all threads timed out
            if (logger.isDebugEnabled()) {
                logger.debug("clearing thread pool of size "
                        + threadPool.size());
            }
            threadPool.clear();
        }

        //no usable thread found, create a new thread
        poolThread = new PoolThread(runnable, name);
        poolThread.setDaemon(true);
        poolThread.start();
    }
}
