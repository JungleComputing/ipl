package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.Member;
import ibis.util.ThreadPool;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Sends events to clients from the server.
 */
final class EventBroadcaster implements Runnable {

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

        private final WorkQ workQ;
        private final Event[] events;

        EventPusherThread(WorkQ workQ, Event[] events) {
            this.workQ = workQ;
            this.events = events;

            ThreadPool.createNew(this, "event pusher thread");
        }

        public void run() {
            while (true) {
                Member member = workQ.next();

                if (member == null) {
                    // done pushing
                    return;
                }

                logger.debug("broadcasting to " + member);

                pool.forward(member, events);
                workQ.doneJob();
            }
        }
    }

    private static final Logger logger = Logger
            .getLogger(EventBroadcaster.class);

    private final Pool pool;

    EventBroadcaster(Pool pool) {
        this.pool = pool;

        ThreadPool.createNew(this, "event pusher scheduler thread");
    }

    public void run() {
        int currentTime = 0;
        
        while (!pool.hasEnded()) {
            //wait until there is some event to send
            pool.waitForEventTime(currentTime + 1, 0);

            Event[] events = pool.getEvents(currentTime);
            
            if (pool.hasEnded()) {
                return;
            }
            
            if (events.length == 0) {
                logger.error("pool did not return anything");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //IGNORE
                }
                continue;
            }
            
            //update current time to after last event given
            currentTime = events[events.length - 1].getTime() + 1;
            logger.debug("current time now : " + currentTime);

            Member[] children = pool.getChildren();
            
            logger.debug("broadcasting events up to "
                    + currentTime);

            WorkQ workQ = new WorkQ(children);

            int threads = Math.min(THREADS, children.length);
            for (int i = 0; i < threads; i++) {
                new EventPusherThread(workQ, events);
            }

            workQ.waitUntilDone();

            logger.debug("DONE broadcasting events up to "
                    + currentTime);
        }
    }

}
