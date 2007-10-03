package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.registry.central.Protocol;
import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.server.ServerProperties;
import ibis.server.Service;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Server for the centralized registry implementation.
 * 
 */
public final class Server extends Thread implements Service {

    public static final int VIRTUAL_PORT = 302;

    private static final Logger logger = Logger.getLogger(Server.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final VirtualSocketFactory socketFactory;

    private final HashMap<String, Pool> pools;

    private final Stats stats;

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

        pools = new HashMap<String, Pool>();

        stats = new Stats(Protocol.NR_OF_OPCODES);

        // start handling connections
        handler = new ServerConnectionHandler(this, socketFactory);

        ThreadPool.createNew(this, "Central Registry Service");

        logger.debug("Started Central Registry service on virtual port "
                + VIRTUAL_PORT);
    }

    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    Stats getStats() {
        return stats;
    }

    // atomic get/create pool
    synchronized Pool getAndCreatePool(String poolName, long heartbeatInterval,
            long eventPushInterval, boolean gossip, long gossipInterval,
            boolean adaptGossipInterval, boolean tree, boolean closedWorld,
            int poolSize, String ibisImplementationIdentifier) throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.hasEnded()) {
            // print message
            System.out.println("Central Registry: creating new pool: \""
                    + poolName + "\"");

            result = new Pool(poolName, socketFactory, heartbeatInterval,
                    eventPushInterval, gossip, gossipInterval,
                    adaptGossipInterval, tree, closedWorld, poolSize,ibisImplementationIdentifier,
                    printEvents, printErrors, stats);
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
    public synchronized void end(boolean waitUntilIdle) {
        if (stopped) {
            return;
        }
        while (waitUntilIdle && pools.size() > 0) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        stopped = true;
        notifyAll();
        handler.end();
        if (printStats && !stats.empty()) {
            System.out.println(stats.getStats(false));
        }
    }

    // force the server to check the pools _now_
    synchronized void nudge() {
        notifyAll();
    }

    public String toString() {
        return "Central Registry service on virtual port " + VIRTUAL_PORT;
    }

    // pool cleanup thread
    public synchronized void run() {

        while (!stopped) {
            if (printStats && !stats.empty()) {
                System.out.println(stats.getStats(false));
            }

            if (pools.size() > 0) {

                StringBuilder message = new StringBuilder();

                Formatter formatter = new Formatter(message);
                formatter.format("list of pools:\n");
                formatter
                        .format("NAME               CLOSED  FIXED_SIZE  CURRENT_SIZE  EVENT_TIME\n");

                //copy values to new array so we can do "remove" on original
                for (Pool pool : pools.values().toArray(new Pool[0])) {
                    if (pool.isClosedWorld()) {
                    formatter.format("%-17s  %6b  %10d  %12d  %10d\n", pool
                            .getName(), pool.isClosed(), pool.getFixedSize(),
                            pool.getSize(), pool.getEventTime());
                    } else {
                        formatter.format("%-17s  %6s  %10s  %12d  %10d\n", pool
                                .getName(), "N.A.", "N.A.",
                                pool.getSize(), pool.getEventTime());
                    }

                    if (pool.stale()) {
                        pools.remove(pool.getName());
                        if (pools.size() == 0) {
                            notifyAll();
                        }
                    }

                }
                if (printStats) {
                    System.out.println(message);
                }

            }

            try {
                wait(POOL_CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                // IGNORE
            }

        }

    }
}
