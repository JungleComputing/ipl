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
package ibis.ipl.registry.central.client;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.registry.central.Member;
import ibis.util.ThreadPool;

public class Gossiper implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Gossiper.class);

    private final CommunicationHandler commHandler;
    private final Pool pool;

    private final long gossipInterval;

    Gossiper(CommunicationHandler commHandler, Pool pool, long gossipInterval) {
        this.commHandler = commHandler;
        this.pool = pool;
        this.gossipInterval = gossipInterval;

        ThreadPool.createNew(this, "gossiper");
    }

    @Override
    public void run() {
        while (!pool.isStopped()) {
            Member member = pool.getRandomMember();

            if (member != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("gossiping with " + member);
                }

                try {
                    commHandler.gossip(member.getIbis());
                } catch (IOException e) {
                    logger.warn("could not gossip with " + member);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Event time at " + commHandler.getIdentifier().getID() + " now " + pool.getTime());
                }
            }

            synchronized (this) {
                try {
                    wait((int) (Math.random() * gossipInterval * 2));
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }

    }
}
