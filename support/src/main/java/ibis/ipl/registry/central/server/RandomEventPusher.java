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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.registry.central.Member;
import ibis.util.ThreadPool;

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

        @Override
        public void run() {
            while (!pool.hasEnded()) {
                long timeout = interval;

                createNewThread();

                if (adaptInterval) {
                    int poolSize = pool.getSize();
                    if (poolSize > 1) {
                        // divide by log2(poolSize)
                        timeout = (long) (timeout / ((Math.log(poolSize) / Math.log(2))));
                    }

                }

                if (logger.isDebugEnabled()) {
                    logger.debug("waiting " + timeout);
                }
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
    RandomEventPusher(Pool pool, long interval, boolean adaptInterval) {
        this.pool = pool;
        this.interval = interval;
        this.adaptInterval = adaptInterval;

        currentThreads = 0;

        // schedules calls to our run function
        new Scheduler();
    }

    private synchronized void createNewThread() {
        if (currentThreads >= THREADS) {
            if (logger.isDebugEnabled()) {
                logger.debug("not creating thread, maximum reached");
            }
            return;
        }
        ThreadPool.createNew(this, "node contactor");
        currentThreads++;
    }

    private synchronized void threadDone() {
        currentThreads--;
    }

    @Override
    public void run() {
        Member member = pool.getRandomMember();

        if (member == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("no member to contact");
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("gossiping/pushing to " + member);
            }
            pool.push(member, false, false);
        }
        threadDone();
    }
}
