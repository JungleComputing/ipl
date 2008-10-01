package ibis.ipl.impl.registry.central.client;

import ibis.ipl.impl.registry.central.Member;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public void run() {
        while (!pool.isStopped()) {
            Member member = pool.getRandomMember();

            if (member != null) {
                logger.debug("gossiping with " + member);

                try {
                    commHandler.gossip(member.getIbis());
                } catch (IOException e) {
                    logger.warn("could not gossip with " + member);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Event time at "
                            + commHandler.getIdentifier().getID() + " now "
                            + pool.getTime());
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
