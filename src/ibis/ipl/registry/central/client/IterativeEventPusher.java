package ibis.ipl.registry.central.client;

import ibis.ipl.registry.central.Member;
import ibis.util.ThreadPool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends events to clients from the server.
 */
final class IterativeEventPusher extends Thread {

    private static final int THREADS = 10;

    private class WorkQ {
        private List<Member> q;
        private int count;

        WorkQ(Member[] work) {
            // Arrays.asList list does not support remove, so do this "trick"
            q = new LinkedList<Member>();
            q.addAll(Arrays.asList(work));

            count = this.q.size();
        }

        synchronized Member next() {
            if (q.isEmpty()) {
                return null;
            }

            return q.remove(0);
        }

        synchronized void doneJob() {
            count--;

            if (count <= 0) {
                notifyAll();
            }
        }

        synchronized void waitUntilDone() {
            while (count > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }
    }

    private class EventPusherThread implements Runnable {

        WorkQ workQ;

        EventPusherThread(WorkQ workQ) {
            this.workQ = workQ;

            ThreadPool.createNew(this, "event pusher thread");
        }

        public void run() {
            while (true) {
                Member work = workQ.next();

                if (work == null) {
                    // done pushing
                    return;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("pushing to " + work);
                }

                commHandler.forward(work.getIbis());
                workQ.doneJob();
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(IterativeEventPusher.class);

    private final Pool pool;
    private final CommunicationHandler commHandler;

    IterativeEventPusher(Pool pool, CommunicationHandler commHandler) {
        this.pool = pool;
        this.commHandler = commHandler;
    }

    public void run() {
        while (!pool.isStopped()) {
            int eventTime = pool.getTime();

            Member[] children = pool.getChildren();

            logger.debug("updating " + children.length +
                    " children in pool to event-time " + eventTime);

            WorkQ workQ = new WorkQ(children);

            int threads = Math.min(THREADS, children.length);
            for (int i = 0; i < threads; i++) {
                new EventPusherThread(workQ);
            }

            workQ.waitUntilDone();

            logger.debug("DONE updating nodes in pool to event-time "
                    + eventTime);

            pool.waitForEventTime(eventTime + 1, 1000);
        }
    }
}
