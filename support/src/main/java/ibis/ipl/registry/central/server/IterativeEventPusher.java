/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.registry.central.server;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.registry.central.Member;
import ibis.util.ThreadPool;

/**
 * Sends events to clients from the server.
 */
final class IterativeEventPusher implements Runnable {

    private static final int THREADS = 25;

    private class WorkQ {
        private List<Member> q;

        private int count;

        WorkQ(Member[] work) {
            // Arrays.asList list does not support remove, so do this "trick"
            q = new LinkedList<>();
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

        @Override
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

                pool.push(work, false, useTree);
                workQ.doneJob();
                if (logger.isDebugEnabled()) {
                    logger.debug("done pushing to " + work);
                }
            }
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(IterativeEventPusher.class);

    private final Pool pool;

    private final long timeout;

    private final boolean eventTriggersPush;

    private final boolean useTree;

    IterativeEventPusher(Pool pool, long timeout, boolean eventTriggersPush, boolean useTree) {
        this.pool = pool;
        this.timeout = timeout;
        this.eventTriggersPush = eventTriggersPush;
        this.useTree = useTree;

        ThreadPool.createNew(this, "event pusher scheduler thread");
    }

    @Override
    public void run() {
        while (!pool.hasEnded()) {
            int eventTime = pool.getEventTime();

            Member[] children;

            if (useTree) {
                children = pool.getChildren();
                if (logger.isDebugEnabled()) {
                    String message = "broadcasting to " + children.length + " children:";
                    for (Member member : children) {
                        message += "\n" + member;
                    }
                    logger.debug(message);
                }
            } else {
                children = pool.getMembers();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("updating " + children.length + " nodes in pool (pool size = " + pool.getSize() + ") to event-time " + eventTime
                        + " using tree: " + useTree);
            }

            WorkQ workQ = new WorkQ(children);

            int threads = Math.min(THREADS, children.length);
            for (int i = 0; i < threads; i++) {
                new EventPusherThread(workQ);
            }

            workQ.waitUntilDone();

            if (logger.isDebugEnabled()) {
                logger.debug("DONE updating nodes in pool to event-time " + eventTime);
            }

            pool.purgeHistory();

            // try {
            // Thread.sleep(100);
            // } catch (InterruptedException e) {
            // //IGNORE
            // }

            if (eventTriggersPush) {
                pool.waitForEventTime(eventTime + 1, timeout);
            } else {
                // wait for the timeout, or until the pool ends
                pool.waitForEventTime(Integer.MAX_VALUE, timeout);
            }
        }
    }

}
