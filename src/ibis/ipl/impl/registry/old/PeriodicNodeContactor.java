package ibis.ipl.impl.registry.old;

import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

final class PeriodicNodeContactor implements Runnable {

    private static final long MIN_TIMEOUT = 100;

    private static final long MAX_TIMEOUT = 30 * 1000;

    private static final Logger logger = Logger
            .getLogger(PeriodicNodeContactor.class);

    private final Pool pool;

    private int next;

    private final long interval;

    private final int maxThreads;

    private int currentThreads;

    // randomly select nodes to contact. If false, will iterate over entire list
    private final boolean randomNodeSelection;

    private final boolean sendEvents;

    // inner scheduling class
    private class Scheduler implements Runnable {

        Scheduler() {
            ThreadPool.createNew(this, "scheduler thread");
        }

        public void run() {
            while (!pool.ended()) {
                logger.debug("scheduling node contactor");
                long timeout;
                
                createNewThread();

                logger.debug("scheduled node contactor");

                int poolSize = pool.getSize();

                if (poolSize == 0) {
                    // divive by zero prevention :)
                    timeout = MAX_TIMEOUT;
                } else {
                    timeout = interval / poolSize;
                }
                if (timeout < MIN_TIMEOUT) {
                    timeout = MIN_TIMEOUT;
                }
                if (timeout > MAX_TIMEOUT) {
                    timeout = MAX_TIMEOUT;
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
     * Contacts ibisses in a pool to check if they are still alive and
     * optionally send them new events
     */
    PeriodicNodeContactor(Pool pool, boolean sendEvents,
            boolean randomNodeSelection, long interval, int maxThreads) {
        this.pool = pool;
        this.sendEvents = sendEvents;
        this.randomNodeSelection = randomNodeSelection;
        this.interval = interval;
        this.maxThreads = maxThreads;

        currentThreads = 0;
        next = 0;

        // schedules calls to our run function
        new Scheduler();
    }

    private synchronized void createNewThread() {
        if (currentThreads >= maxThreads) {
            logger.debug("not creating thread, maximum reached");
            return;
        }
        ThreadPool.createNew(this, "node contactor");
        currentThreads++;
    }

    private synchronized void threadDone() {
        currentThreads--;
    }

    private synchronized void resetNext() {
        this.next = 0;
    }

    private synchronized int incNext() {
        return next++;
    }

    public void run() {
        Member member;

        if (randomNodeSelection) {
            member = pool.getRandomMember();
        } else {
            member = pool.getMember(incNext());
            if (member == null) {
                resetNext();
                member = pool.getMember(incNext());
            }
        }

        if (member == null) {
            logger.debug("no member to contact");
        } else {
            if (sendEvents) {
                pool.push(member);
            } else {
                pool.ping(member.ibis());
            }
        }
        threadDone();
    }
}
