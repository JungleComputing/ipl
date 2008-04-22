package ibis.ipl.impl.registry.gossip;

import java.io.IOException;
import java.util.HashSet;

import org.apache.log4j.Logger;

import ibis.ipl.impl.registry.Connection;
import ibis.ipl.impl.registry.statistics.Statistics;
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

    private static final int CONNECT_TIMEOUT = 30000;

    private static final int SERVER_CONNECT_TIMEOUT = 120000;
    
    private static final int CACHE_SIZE = 100;

    private static final int GOSSIP_SIZE = 30;

    private static final long DEAD_TIMEOUT = 5 * 60 * 1000;

    private final VirtualSocketFactory socketFactory;

    private final VirtualSocketAddress bootstrapAddress;

    private final String poolName;

    private final Statistics statistics;

    private final ARRGCacheEntry self;

    private final ARRGCache cache;

    private final ARRGCache fallbackCache;

    private boolean ended;

    private long lastGossip;

    ARRG(VirtualSocketAddress address, boolean arrgOnly,
            VirtualSocketAddress[] bootstrapList,
            VirtualSocketAddress bootstrapAddress, String poolName,
            VirtualSocketFactory socketFactory, Statistics statistics) {
        this.socketFactory = socketFactory;
        this.poolName = poolName;
        this.bootstrapAddress = bootstrapAddress;

        if (statistics == null) {
            statistics = new Statistics(Protocol.OPCODE_NAMES);
            statistics.setID("server", poolName);
            statistics.startWriting(60000);
        }

        this.statistics = statistics;

        self = new ARRGCacheEntry(address, arrgOnly);

        cache = new ARRGCache(CACHE_SIZE);
        fallbackCache = new ARRGCache(CACHE_SIZE);

        // add all bootstrap addresses to both caches
        for (VirtualSocketAddress bootstrapEntry : bootstrapList) {
            ARRGCacheEntry entry = new ARRGCacheEntry(bootstrapEntry, true);
            cache.add(entry);
            fallbackCache.add(entry);
        }

        lastGossip = System.currentTimeMillis();

    }

    /*
     * (non-Javadoc)
     * 
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
        ARRGCacheEntry peerEntry = new ARRGCacheEntry(connection.in());

        int receiveCount = connection.in().readInt();

        ARRGCacheEntry[] receivedEntries = new ARRGCacheEntry[receiveCount];

        for (int i = 0; i < receiveCount; i++) {
            receivedEntries[i] = new ARRGCacheEntry(connection.in());
        }

        connection.sendOKReply();

        self.writeTo(connection.out());

        ARRGCacheEntry[] sendEntries =
            cache.getRandomEntries(GOSSIP_SIZE, true);
        connection.out().writeInt(sendEntries.length);
        for (ARRGCacheEntry entry : sendEntries) {
            entry.writeTo(connection.out());
        }
        
        connection.out().flush();

        connection.close();

        cache.add(peerEntry);
        cache.add(receivedEntries);

        resetLastGossip();

        logger.debug("bootstrap service for " + poolName
                + " received request from " + peerEntry);
    }

    private void gossip(VirtualSocketAddress victim, int timeout, boolean fillTimeout) throws IOException {
        long start = System.currentTimeMillis();

        if (victim == null) {
            logger.debug("no victim specified");
            return;
        }

        if (victim.equals(self.getAddress())) {
            logger.debug("not gossiping with outselves");
            return;
        }

        logger.debug("gossiping with " + victim);

        ARRGCacheEntry[] sendEntries =
            cache.getRandomEntries(GOSSIP_SIZE, true);

        Connection connection = null;
        try {

            connection =
                new Connection(victim, timeout, fillTimeout, socketFactory);

            // header

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_ARRG_GOSSIP);
            connection.out().writeUTF(poolName);

            // data

            self.writeTo(connection.out());

            connection.out().writeInt(sendEntries.length);
            for (ARRGCacheEntry entry : sendEntries) {
                entry.writeTo(connection.out());
            }

            connection.getAndCheckReply();

            ARRGCacheEntry peerEntry = new ARRGCacheEntry(connection.in());

            int receiveCount = connection.in().readInt();

            ARRGCacheEntry[] receivedEntries = new ARRGCacheEntry[receiveCount];

            for (int i = 0; i < receiveCount; i++) {
                receivedEntries[i] = new ARRGCacheEntry(connection.in());
            }

            connection.close();

            cache.add(peerEntry);
            cache.add(receivedEntries);

            resetLastGossip();

            if (statistics != null) {
                statistics.add(Protocol.OPCODE_ARRG_GOSSIP,
                    System.currentTimeMillis() - start, connection.read(),
                    connection.written(), false);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    VirtualSocketAddress getRandomMember() {
        ARRGCacheEntry result = cache.getRandomEntry(false);

        if (result == null) {
            return null;
        }

        return result.getAddress();
    }
    
    VirtualSocketAddress[] getRandomMembers(int size) {
        // use a set to get rid of duplicates
        HashSet<VirtualSocketAddress> result =
            new HashSet<VirtualSocketAddress>();

        ARRGCacheEntry[] entries = cache.getRandomEntries(size, false);
        
        for (ARRGCacheEntry entry : entries) {
            if (!entry.isArrgOnly()) {
                result.add(entry.getAddress());
            }
        }

        return result.toArray(new VirtualSocketAddress[0]);
    }


    VirtualSocketAddress[] getMembers() {
        // use a set to get rid of duplicates
        HashSet<VirtualSocketAddress> result =
            new HashSet<VirtualSocketAddress>();

        ARRGCacheEntry[] entries = cache.getEntries(false);

        for (ARRGCacheEntry entry : entries) {
            if (!entry.isArrgOnly()) {
                result.add(entry.getAddress());
            }
        }

        entries = fallbackCache.getEntries(false);

        for (ARRGCacheEntry entry : entries) {
            if (!entry.isArrgOnly()) {
                result.add(entry.getAddress());
            }
        }

        return result.toArray(new VirtualSocketAddress[0]);
    }

    public void run() {
        while (!ended()) {
            ARRGCacheEntry victim = cache.getRandomEntry(true);

            boolean success = false;

            // first try normal cache
            if (victim != null) {
                try {
                    gossip(victim.getAddress(), CONNECT_TIMEOUT, false);
                    fallbackCache.add(victim);
                    success = true;
                } catch (IOException e) {
                    logger.debug("could not gossip with " + victim, e);
                }

            }

            // then try fallback cache
            if (success == false) {
                victim = fallbackCache.getRandomEntry(true);
                if (victim != null) {
                    try {
                        gossip(victim.getAddress(), CONNECT_TIMEOUT, false);
                        success = true;
                    } catch (IOException e) {
                        logger.debug("could not gossip with fallback entry: "
                                + victim, e);
                    }
                }
            }

            // lastly, use bootstrap service (also wait longer on connecting)
            if (success == false) {
                if (bootstrapAddress != null) {
                    try {
                        gossip(bootstrapAddress, SERVER_CONNECT_TIMEOUT, true);
                        success = true;
                    } catch (IOException e) {
                        logger.error(
                            "could not gossip with bootstrap server at "
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

    public Statistics getStatistics() {
        return statistics;
    }

}
