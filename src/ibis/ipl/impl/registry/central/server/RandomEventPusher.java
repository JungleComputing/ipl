package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.registry.central.Member;
import ibis.util.ThreadPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RandomEventPusher implements Runnable {
    
    private static final int THREADS = 25;

    private static final Logger logger = LoggerFactory.getLogger(RandomEventPusher.class);

    private final Pool pool;

    private final long interval;

    /**
     * If true, the interval of the event pushing is adapted to the pool value
     */
    private final boolean adaptInterval;

    private int currentThreads;

    // inner scheduling class
    private class Scheduler implements Runnable {

        Scheduler() {
            ThreadPool.createNew(this, "scheduler thread");
        }

        public void run() {
            while (!pool.hasEnded()) {
                long timeout = interval;

                createNewThread();

                if (adaptInterval) {
                    int poolSize = pool.getSize();
                    if (poolSize > 1) {
                        // divide by log2(poolSize)
                        timeout = (long) (timeout / ((Math.log(poolSize) / Math
                                .log(2))));
                    }

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
        }
    }

    /**
     * Randomly push events to registries in the pool
     */
    RandomEventPusher(Pool pool, long interval, boolean adaptInterval
            ) {
        this.pool = pool;
        this.interval = interval;
        this.adaptInterval = adaptInterval;

        currentThreads = 0;

        // schedules calls to our run function
        new Scheduler();
    }

    private synchronized void createNewThread() {
        if (currentThreads >= THREADS) {
            logger.debug("not creating thread, maximum reached");
            return;
        }
        ThreadPool.createNew(this, "node contactor");
        currentThreads++;
    }

    private synchronized void threadDone() {
        currentThreads--;
    }

    public void run() {
        Member member = pool.getRandomMember();

        if (member == null) {
            logger.debug("no member to contact");
        } else {
            logger.debug("gossiping/pushing to " + member);
            pool.push(member, false, false);
        }
        threadDone();
    }
}
