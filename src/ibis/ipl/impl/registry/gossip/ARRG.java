package ibis.ipl.impl.registry.gossip;

import java.io.IOException;

import org.apache.log4j.Logger;

import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

/**
 * Implementation of the ARRG algorithm
 * 
 * @author ndrost
 * 
 */
class ARRG extends Thread {

    private static final Logger logger = Logger.getLogger(ARRG.class);

    private static final int GOSSIP_TIMEOUT = 1000;

    private static final int CONNECT_TIMEOUT = 5000;

    private static final int CACHE_SIZE = 100;

    private static final int GOSSIP_SIZE = 30;

    private static final long DEAD_TIMEOUT = 5 * 60 * 1000;

    private final VirtualSocketFactory socketFactory;

    private final VirtualSocketAddress bootstrapAddress;

    private final String poolName;

    private final CacheEntry self;

    private final Cache cache;

    private final Cache fallbackCache;

    private boolean ended;

    private long lastGossip;

    ARRG(VirtualSocketAddress address, boolean arrgOnly,
            VirtualSocketAddress[] bootstrapList,
            VirtualSocketAddress bootstrapAddress, String poolName,
            VirtualSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
        this.poolName = poolName;
        this.bootstrapAddress = bootstrapAddress;

        self = new CacheEntry(address, arrgOnly);

        cache = new Cache(CACHE_SIZE);
        fallbackCache = new Cache(CACHE_SIZE);

        // add all bootstrap addresses to both caches
        for (VirtualSocketAddress bootstrapEntry : bootstrapList) {
            CacheEntry entry = new CacheEntry(bootstrapEntry, true);
            cache.add(entry);
            fallbackCache.add(entry);
        }

        lastGossip = System.currentTimeMillis();

    }

    /* (non-Javadoc)
     * @see java.lang.Thread#start()
     */
    @Override
    public synchronized void start() {
        super.setDaemon(true);
        super.start();
    }



    public String getPoolName() {
        return poolName;
    }

    synchronized void end() {
        ended = true;

        notifyAll();
    }

    synchronized boolean ended() {
        return ended;
    }

    private synchronized void resetLastGossip() {
        lastGossip = System.currentTimeMillis();
    }

    /**
     * If we have not had any sucessfull gossip for DEAD_TIMEOUT, declare this
     * pool dead
     * 
     * @return
     */
    synchronized boolean isDead() {
        return System.currentTimeMillis() > lastGossip + DEAD_TIMEOUT;
    }

    void handleGossip(Connection connection) throws IOException {
        CacheEntry peerEntry = new CacheEntry(connection.in());

        int receiveCount = connection.in().readInt();

        CacheEntry[] receivedEntries = new CacheEntry[receiveCount];

        for (int i = 0; i < receiveCount; i++) {
            receivedEntries[i] = new CacheEntry(connection.in());
        }

        connection.sendOKReply();

        self.writeTo(connection.out());

        CacheEntry[] sendEntries = cache.getRandomEntries(GOSSIP_SIZE);
        connection.out().writeInt(sendEntries.length);
        for (CacheEntry entry : sendEntries) {
            entry.writeTo(connection.out());
        }

        connection.close();

        cache.add(peerEntry);
        cache.add(receivedEntries);

        resetLastGossip();
    }

    private void gossip(VirtualSocketAddress victim) throws IOException {
        if (victim == null) {
            logger.debug("no victim specified");
            return;
        }
        
        if (victim.equals(self.getAddress())) {
            logger.debug("not gossiping with outselves");
            return;
        }

        logger.debug("gossiping with " + victim);

        CacheEntry[] sendEntries = cache.getRandomEntries(GOSSIP_SIZE);

        Connection connection =
                new Connection(victim, CONNECT_TIMEOUT, false, socketFactory);

        // header

        connection.out().writeByte(Protocol.MAGIC_BYTE);
        connection.out().writeByte(Protocol.OPCODE_ARRG_GOSSIP);
        connection.out().writeUTF(poolName);

        // data

        self.writeTo(connection.out());

        connection.out().writeInt(sendEntries.length);
        for (CacheEntry entry : sendEntries) {
            entry.writeTo(connection.out());
        }

        connection.getAndCheckReply();

        CacheEntry peerEntry = new CacheEntry(connection.in());

        int receiveCount = connection.in().readInt();

        CacheEntry[] receivedEntries = new CacheEntry[receiveCount];

        for (int i = 0; i < receiveCount; i++) {
            receivedEntries[i] = new CacheEntry(connection.in());
        }

        connection.close();

        cache.add(peerEntry);
        cache.add(receivedEntries);

        resetLastGossip();
    }

    public void run() {
        while (!ended()) {
            CacheEntry victim = cache.getRandomEntry();

            boolean success = false;

            //first try normal cache
            if (victim != null) {
                try {
                    gossip(victim.getAddress());
                    fallbackCache.add(victim);
                    success = true;
                } catch (IOException e) {
                    logger.error("could not gossip with " + victim, e);
                }

            }

            //then try fallback cache
            if (success == false) {
                victim = fallbackCache.getRandomEntry();
                if (victim != null) {
                    try {
                        gossip(victim.getAddress());
                        success = true;
                    } catch (IOException e) {
                        logger.error("could not gossip with fallback entry: "
                                + victim, e);
                    }
                }
            }

            //lastly, use bootstrap service
            if (success == false) {
                if (bootstrapAddress != null) {
                    try {
                        gossip(bootstrapAddress);
                        success = true;
                    } catch (IOException e) {
                        logger.error("could not gossip with bootstrap server at "
                                + bootstrapAddress, e);
                    }
                }
            }

            synchronized (this) {
                long timeout = (long) (Math.random() * GOSSIP_TIMEOUT) + 1;

                try {
                    logger.debug("waiting " + timeout + " ms");
                    wait(timeout);
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }
    }

}
