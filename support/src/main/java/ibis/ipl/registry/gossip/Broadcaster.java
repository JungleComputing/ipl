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
package ibis.ipl.registry.gossip;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.util.ThreadPool;

/**
 * Broadcasts the leave of an ibis
 */
final class Broadcaster implements Runnable {

    private static final int THREADS = 10;

    private static final Logger logger = LoggerFactory.getLogger(Broadcaster.class);

    private final CommunicationHandler commHandler;

    private List<VirtualSocketAddress> q;

    private int count;

    Broadcaster(CommunicationHandler commHandler, VirtualSocketAddress[] addresses) {
        this.commHandler = commHandler;

        // Arrays.asList list does not support remove, so do this "trick"
        q = new LinkedList<>();
        q.addAll(Arrays.asList(addresses));

        // number of jobs remaining
        count = this.q.size();

        int threads = Math.min(THREADS, count);
        for (int i = 0; i < threads; i++) {
            ThreadPool.createNew(this, "broadcaster");
        }
    }

    synchronized VirtualSocketAddress next() {
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
        if (logger.isDebugEnabled()) {
            logger.debug("waiting until done, " + count + " remaining");
        }
        while (count > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("done!");
        }
    }

    @Override
    public void run() {
        while (true) {
            VirtualSocketAddress address = next();

            if (address == null) {
                // done pushing
                return;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("sending leave to " + address);
            }

            commHandler.sendLeave(address);

            doneJob();
        }
    }
}
