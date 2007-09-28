package ibis.ipl.impl.registry.central.client;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

public class Gossiper implements Runnable {

    private static final Logger logger = Logger.getLogger(Gossiper.class);

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
            IbisIdentifier ibis = null;
            try {
                ibis = pool.getRandomMember().getIbis();

                if (ibis != null) {
                    logger.debug("gossiping with " + ibis);

                    commHandler.gossip(ibis);
                }

            } catch (IOException e) {
                logger.error("could not gossip with " + ibis + ": " + e);

            }

            logger.debug("Event time at " + commHandler.getIdentifier().getID()
                    + " now " + pool.getTime());
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
