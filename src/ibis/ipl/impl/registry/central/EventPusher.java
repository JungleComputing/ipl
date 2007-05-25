package ibis.ipl.impl.registry.central;

import java.util.List;

import ibis.util.ThreadPool;

import org.apache.log4j.Logger;

final class EventPusher implements Runnable {

    private static final long DELAY = 1000;
    
    private class WorkQ {
        private List<Member> q;
        
        WorkQ(List<Member> work) {
            this.q = work;
        }
        
        synchronized Member next() {
            if (q.isEmpty()) {
                return null;
            }
            
            return q.remove(0);
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

                logger.debug("pushing to " + work);

                pool.push(work);
            }
        }
    }

    private static final Logger logger = Logger.getLogger(EventPusher.class);

    private Pool pool;

    private int threads;

    EventPusher(Pool pool, int threads) {
        this.pool = pool;
        this.threads = threads;

        ThreadPool.createNew(this, "event pusher scheduler thread");
    }
    

    public void run() {
        while (!pool.ended()) {
            int time = pool.getEventTime();

            List<Member> members = pool.getMemberList();

            logger.debug("updating nodes in pool (pool size = " + members.size() + "  to event-time " + time);
            
            WorkQ workQ = new WorkQ(members);
            
            int threads = Math.min(this.threads, members.size());
            for (int i = 0; i < threads; i++) {
                new EventPusherThread(workQ);
            }

            synchronized(this) {
                while(!members.isEmpty()) {
                    try {
                        wait(DELAY);
                    } catch (InterruptedException e) {
                        //IGNORE
                    }
                } 
            }

            logger.debug("DONE updating nodes in pool to event-time "
                    + time);

            // wait until some event has happened
            // (which it might already have)
            pool.waitForEventTime(time + 1);
        }
    }


}
