package ibis.ipl.impl.registry.central;

import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

final class EventPusher implements Runnable {

    private class Scheduler implements Runnable {

        private static final long DELAY = 1000;

        Scheduler() {
            ThreadPool.createNew(this, "scheduler thread");
        }

        public void run() {
            while (!pool.ended()) {
                int time = pool.getEventTime();

                logger.debug("updating nodes in pool to event-time " + time);

                pushEvents();

                logger.debug("DONE updating nodes in pool to event-time "
                        + time);
                
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    // IGNORE
                }

                // wait until some event has happened
                // (which it might already have)
                pool.waitForEventTime(time + 1);
            }
        }
    }

    private static final Logger logger = Logger.getLogger(EventPusher.class);

    private Pool pool;

    private int next;

    private int threads;

    EventPusher(Pool pool, int threads) {
        this.pool = pool;
        this.threads = threads;

        next = -1;

        new Scheduler();
    }

    private synchronized void pushEvents() {
        next = 0;
        
        for (int i = 0; i < threads; i++) {
            ThreadPool.createNew(this, "event pusher");
        }
        
        while (next != -1) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        logger.debug("done!");
    }

    private synchronized Member getNext() {
        logger.debug("getting next");
        if (next == -1) {
            return null;
        }

        Member result = pool.getMember(next++);
        logger.debug("next = " + next);
        if (result == null) {
            next = -1;
            notifyAll();
        }

        return result;
    }


    public void run() {
        while (true) {
            Member next = getNext();

            if (next == null) {
                // done pushing
                return;
            }

            logger.debug("pushing to " + next);

            pool.push(next);
        }
    }
}
