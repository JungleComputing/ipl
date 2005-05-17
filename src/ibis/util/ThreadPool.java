package ibis.util;

import ibis.ipl.IbisError;

import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * @author Niels Drost
 * 
 * Threadpool which uses timeouts to determine the number of threads...
 * There is no maximum number of threads in this pool, to prevent deadlocks.
 *
 */
public final class ThreadPool {

    static final Logger logger = GetLogger.getLogger(ThreadPool.class);

    private static final class PoolThread extends Thread {

        private static final int TIMEOUT = 30 * 1000; //30 seconds 

        Runnable work = null;

        String name = null;

        boolean expired = false;

        private static int nrOfThreads = 0;

        private static synchronized void newThread() {
            nrOfThreads++;
            logger.debug("new thread. Threadcount: " + nrOfThreads);
        }

        private static synchronized void threadGone() {
            nrOfThreads--;
            logger.debug("thread gone. Threadcount: " + nrOfThreads);
        }

        private PoolThread() {
            //DO NOT USE
        }

        PoolThread(Runnable runnable, String name) {
            this.work = runnable;
            this.name = name;

             if (logger.isDebugEnabled()) {
                 newThread();
             }
        }

        synchronized boolean issue(Runnable newWork, String newName) {
            if (expired) {
                logger.debug("issue(): thread has expired");
                return false;
            }

            if (this.work != null) {
                throw new IbisError("tried to issue work to already running"
                        + " poolthread");
            }

            work = newWork;
            name = newName;

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
                            if (logger.isDebugEnabled()) {
                                threadGone();
                            }
                            return;
                        }
                    }
                    if (this.work == null) {
                        //still no work, exit
                        expired = true;
                        if (logger.isDebugEnabled()) {
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
                    String errorString = "caught exception in pool thread "
                        + currentName + ": " + t + "\n Stacktrace: \n";
                        
                    StackTraceElement e[] = t.getStackTrace();
                    for(int i = 0; i < e.length; i++) {
                        errorString = errorString + "\t\t" + e.toString() + "\n";
                    }
                    
                    logger.error(errorString);
                }
                synchronized (this) {
                    this.work = null;
                    this.name = null;
                }
            }
        }
    }

    //list of waiting Poolthreads
    private static final LinkedList threadPool = new LinkedList();

    /**
     * Prevent creation of a threadpool object.
     */
    private ThreadPool() {
        //DO NOT USE
    }

    static synchronized void waiting(PoolThread thread) {
        threadPool.add(thread);
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
