package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.server.ServerProperties;
import ibis.server.Service;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server for the centralized registry implementation.
 * 
 */
public final class Server extends Thread implements Service {

    public static final int VIRTUAL_PORT = 302;

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final VirtualSocketFactory socketFactory;

    private final SortedMap<String, Pool> pools;

    private final boolean printStats;

    private final boolean printEvents;

    private final boolean printErrors;

    private ServerConnectionHandler handler;

    private boolean stopped = false;

    /**
     * Constructor to create a registry server which is part of a IbisServer
     * 
     * @param properties
     * @param socketFactory
     * @throws IOException
     */
    public Server(TypedProperties properties, VirtualSocketFactory socketFactory)
            throws IOException {
        this.socketFactory = socketFactory;

        TypedProperties typedProperties = RegistryProperties
                .getHardcodedProperties();
        typedProperties.addProperties(properties);

        printStats = typedProperties
                .getBooleanProperty(ServerProperties.PRINT_STATS);

        printEvents = typedProperties
                .getBooleanProperty(ServerProperties.PRINT_EVENTS);

        printErrors = typedProperties
                .getBooleanProperty(ServerProperties.PRINT_ERRORS);

        pools = new TreeMap<String, Pool>();

        // start handling connections
        handler = new ServerConnectionHandler(this, socketFactory);

        ThreadPool.createNew(this, "Central Registry Service");

        logger.debug("Started Central Registry service on virtual port "
                + VIRTUAL_PORT);
    }

    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    public String getServiceName() {
        return "registry";
    }

    // atomic get/create pool
    synchronized Pool getOrCreatePool(String poolName, boolean peerBootstrap,
            long heartbeatInterval, long eventPushInterval, boolean gossip,
            long gossipInterval, boolean adaptGossipInterval, boolean tree,
            boolean closedWorld, int poolSize, boolean keepStatistics,
            long statisticsInterval, String ibisImplementationIdentifier)
            throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.hasEnded()) {
            result = new Pool(poolName, socketFactory, peerBootstrap,
                    heartbeatInterval, eventPushInterval, gossip,
                    gossipInterval, adaptGossipInterval, tree, closedWorld,
                    poolSize, keepStatistics, statisticsInterval,
                    ibisImplementationIdentifier, printEvents, printErrors);
            pools.put(poolName, result);
        }

        return result;
    }

    synchronized boolean isStopped() {
        return stopped;
    }

    /**
     * Stop this server.
     */
    public synchronized void end(long deadline) {
        if (stopped) {
            return;
        }
        long timeLeft = deadline - System.currentTimeMillis();

        while (timeLeft > 0 && pools.size() > 0) {
            try {
                // wake up cleanup thread, wait for a second
                notifyAll();
                wait(Math.min(1000, timeLeft));
            } catch (InterruptedException e) {
                // IGNORE
            }
            timeLeft = deadline - System.currentTimeMillis();
        }
        stopped = true;
        notifyAll();
        handler.end();
    }

    public String toString() {
        return "Central Registry service on virtual port " + VIRTUAL_PORT;
    }

    // pool cleanup thread
    public synchronized void run() {

        while (!stopped) {
            if (pools.size() > 0) {
                if (printStats) {
                    System.err.printf("%tT list of pools:\n", System.currentTimeMillis());
                    System.err.println("     CURRENT_SIZE JOINS LEAVES DIEDS ELECTIONS SIGNALS FIXED_SIZE CLOSED ENDED");
                }

                // copy values to new array so we can do "remove" on original
                for (Pool pool : pools.values().toArray(new Pool[0])) {
                    if (printStats) {
                        System.err.println(pool.getStatsString());
                    }

                    if (pool.hasEnded()) {
                        pool.saveStatistics();
                        pools.remove(pool.getName());
                        if (pools.size() == 0) {
                            notifyAll();
                        }
                    }

                }

            }

            try {
                wait(POOL_CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                // IGNORE
            }

        }

    }

    public synchronized Map<String, String> getStats() {
        Map<String, String> result = new HashMap<String, String>();

        String poolNames = null;

        for (Pool pool : pools.values()) {
            if (poolNames == null) {
                poolNames = pool.getName();
            } else {
                poolNames = poolNames + "," + pool.getName();
            }

            result.putAll(pool.getStatsMap());
        }

        result.put("pool.names", poolNames);

        return result;
    }

}
