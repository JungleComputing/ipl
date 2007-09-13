package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.IbisIdentifier;
import ibis.util.ThreadPool;

import java.io.IOException;

import org.apache.log4j.Logger;

public class Gossiper implements Runnable {

    private static final Logger logger = Logger.getLogger(Gossiper.class);

    private final Registry registry;

    private final long gossipInterval;

    Gossiper(Registry registry, long gossipInterval) {
        this.registry = registry;
        this.gossipInterval = gossipInterval;

        ThreadPool.createNew(this, "gossiper");
    }

    public void run() {
        while (!registry.isStopped()) {
            IbisIdentifier ibis = null;
            try {
                ibis = registry.getRandomMember();

                logger.debug("gossiping with " + ibis);

                registry.gossip(ibis);
                
            } catch (IOException e) {
                logger.error("could not gossip with " + ibis + ": " + e);

            }

            logger.debug("Event time at "
                    + registry.getIbisIdentifier().getID() + " now "
                    + registry.getTime());
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
