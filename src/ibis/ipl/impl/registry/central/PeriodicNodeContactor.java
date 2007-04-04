package ibis.ipl.impl.registry.central;

import ibis.util.ThreadPool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

class PeriodicNodeContactor implements Runnable {

    private static final long MIN_TIMEOUT = 100;

    private static final long MAX_TIMEOUT = 10 * 1000;

    private static final long DELAY = 1000;

    private static final Logger logger = Logger
            .getLogger(PeriodicNodeContactor.class);

    private final Pool pool;

    private int next;

    // randomly select nodes to contact. If false, will iterate over entire list
    private final boolean randomNodeSelection;

    private final boolean sendEvents;

    private static class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            Thread result = new Thread(r);
            result.setDaemon(true);
            return result;
        }
    }

    private static class Scheduler implements Runnable {

        private final long interval;

        private final Runnable runnable;

        private final ThreadPoolExecutor executor;

        private final Pool pool;

        Scheduler(Runnable runnable, int threads, long interval, Pool pool) {
            this.interval = interval;
            this.runnable = runnable;
            this.pool = pool;

            BlockingQueue<Runnable> workQ = new ArrayBlockingQueue<Runnable>(
                    threads * 2);
            executor = new ThreadPoolExecutor(threads, threads, 60,
                    TimeUnit.SECONDS, workQ, new SimpleThreadFactory());

            ThreadPool.createNew(this, "scheduler thread");
        }

        public void run() {
            while (!pool.ended()) {
                logger.debug("scheduling node contactor");
                long timeout;
                try {
                    executor.execute(runnable);

                    logger.debug("scheduled node contactor");

                    int poolSize = pool.getSize();

                    if (poolSize == 0) {
                        // wait one second (pool is probably just starting)
                        timeout = DELAY;
                    } else {
                        timeout = interval / poolSize;
                    }
                    if (timeout < MIN_TIMEOUT) {
                        timeout = MIN_TIMEOUT;
                    }
                    if (timeout > MAX_TIMEOUT) {
                        timeout = MAX_TIMEOUT;
                    }
                } catch (RejectedExecutionException e) {
                    logger
                            .warn("could not start another node contactor, q full");
                    timeout = DELAY;
                }
                logger.debug("waiting " + timeout);
                synchronized (this) {
                    try {

                        wait(timeout);
                    } catch (InterruptedException e) {
                        // IGNORE
                    }
                }

            }
            executor.shutdownNow();
        }
    }

    /**
     * Contacts ibisses in a pool to check if they are still alive and
     * optionally send them new events
     */
    PeriodicNodeContactor(Pool pool, boolean sendEvents,
            boolean randomNodeSelection, long interval, int threads) {
        this.pool = pool;
        this.sendEvents = sendEvents;
        this.randomNodeSelection = randomNodeSelection;

        // schedules calls to our run function
        new Scheduler(this, threads, interval, pool);
    }

    private synchronized void setNext(int next) {
        this.next = next;
    }

    private synchronized void incNext() {
        next++;
    }

    private synchronized int getNext() {
        return next;
    }

    public void run() {
        Member member;

        if (randomNodeSelection) {
            member = pool.getRandomMember();
        } else {
            member = pool.getMember(getNext());
            if (member == null) {
                setNext(0);
                member = pool.getMember(0);
            }
            incNext();

        }

        if (member == null) {
            logger.debug("no member to contact");
            return;
        }

        if (sendEvents) {
            pool.push(member);
        } else {
            pool.ping(member.ibis());
        }

    }
}
