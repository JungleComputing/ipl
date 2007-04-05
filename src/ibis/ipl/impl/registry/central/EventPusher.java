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
                
                start();

                waitUntilDone();
                
                logger.debug("DONE updating nodes in pool to event-time " + time);

                // wait until some event has happened
                // (which it might already have)
                pool.waitForEventTime(time + 1);
                
                try {
                    Thread.sleep(DELAY);
                } catch (InterruptedException e) {
                    //IGNORE
                }
            }
        }
    }

    private static final Logger logger = Logger.getLogger(EventPusher.class);

    private Pool pool;

    int next;

    EventPusher(Pool pool, int threads) {
        this.pool = pool;
        next = -1;

        for (int i = 0; i < threads; i++) {
            ThreadPool.createNew(this, "event pusher");
        }

        new Scheduler();
    }

    private synchronized void start() {
        logger.debug("starting");
        next = 0;
        notifyAll();
    }

    private synchronized Member getNext() {
        logger.debug("getting next");
        while (true) {
            if (pool.ended()) {
                return null;
            }
            if (next != -1) {
                Member result = pool.getMember(next++);
                logger.debug("next = " + next);
                if (result == null) {
                    next = -1;
                    notifyAll();
                } else {
                    return result;
                }
            }
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    private synchronized void waitUntilDone() {
        while (next != -1) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        logger.debug("done!");
    }

   

    public void run() {
        while (true) {
            Member next = getNext();

            if (next == null) {
                // pool ended
                return;
            }
            
            logger.debug("pushing to " + next);

            pool.push(next);
        }
    }
}
