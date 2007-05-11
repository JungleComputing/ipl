package ibis.ipl.impl.registry.central;

import ibis.server.ServerProperties;
import ibis.server.Service;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.Log;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Server for the centralized registry implementation.
 * 
 */
public final class Server implements Service, Runnable {

    public static final int VIRTUAL_PORT = 300;

    private static final Logger logger = Logger.getLogger(Server.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final ConnectionFactory connectionFactory;

    private final HashMap<String, Pool> pools;

    private final boolean printStats;

    private final boolean printEvents;

    private ServerConnectionHandler handler;

    private boolean stopped = false;

    /**
     * Constructor to create a registry server which is part of a IbisServer
     * 
     * @param properties
     * @param factory
     * @throws IOException
     */
    public Server(TypedProperties properties, VirtualSocketFactory factory)
            throws IOException {
        TypedProperties typedProperties =
                RegistryProperties.getHardcodedProperties();
        typedProperties.addProperties(properties);

        // Init logger
        Logger logger = Logger.getLogger("ibis.ipl.impl.registry.central");
        Level level =
                Level.toLevel(typedProperties
                        .getProperty(ServerProperties.LOG_LEVEL));
        Log.initLog4J(logger, level);

        int timeout =
                typedProperties
                        .getIntProperty(RegistryProperties.CONNECT_TIMEOUT) * 1000;

        connectionFactory =
                new ConnectionFactory(factory, VIRTUAL_PORT, timeout);

        printStats =
                typedProperties
                        .getBooleanProperty(RegistryProperties.SERVER_PRINT_STATS)
                        || typedProperties
                                .getBooleanProperty(ServerProperties.PRINT_STATS);

        printEvents =
                typedProperties
                        .getBooleanProperty(RegistryProperties.SERVER_PRINT_EVENTS)
                        || typedProperties
                                .getBooleanProperty(ServerProperties.PRINT_EVENTS);

        pools = new HashMap<String, Pool>();

        ThreadPool.createNew(this, "server");

        logger.debug("Started Central Registry service on virtual port "
                + VIRTUAL_PORT);
    }

    /**
     * Creates a stand-alone registry server. Uses plain tcp.
     * 
     * @param properties
     *            settings for this server.
     * @throws IOException
     */
    public Server(Properties properties) throws IOException {
        TypedProperties typedProperties =
                RegistryProperties.getHardcodedProperties();
        typedProperties.addProperties(properties);

        int port =
                typedProperties.getIntProperty(RegistryProperties.SERVER_PORT);

        if (port <= 0) {
            throw new IOException(
                    "can only start registry server on a positive port");
        }

        int timeout =
                typedProperties
                        .getIntProperty(RegistryProperties.CONNECT_TIMEOUT) * 1000;

        connectionFactory = new ConnectionFactory(port, timeout);

        printStats =
                typedProperties
                        .getBooleanProperty(RegistryProperties.SERVER_PRINT_STATS);

        printEvents =
                typedProperties
                        .getBooleanProperty(RegistryProperties.SERVER_PRINT_EVENTS);

        pools = new HashMap<String, Pool>();

        ThreadPool.createNew(this, "server");
    }

    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    // atomic get/create pool
    synchronized Pool getAndCreatePool(String poolName, boolean gossip,
            boolean keepNodeState, long pingInterval) throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.ended()) {
            if (printEvents) {
                //print to standard out
                System.out.println("Central Registry: creating new pool: \""
                        + poolName + "\"");
            } else {
                // print to the logger
                logger.info("Central Registry: creating new pool: \""
                        + poolName + "\"");
            }

            result =
                    new Pool(poolName, connectionFactory, gossip,
                            keepNodeState, pingInterval, printEvents);
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
        connectionFactory.end();
        if (handler != null && printStats) {
            System.out.println(handler.getStats(false));
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
        // start handling connections
        handler = new ServerConnectionHandler(this, connectionFactory);

        while (!stopped) {
            if (printStats) {
                System.out.println(handler.getStats(false));
            }

            Pool[] poolArray = pools.values().toArray(new Pool[0]);

            if (poolArray.length > 0) {

                StringBuilder message = new StringBuilder();

                Formatter formatter = new Formatter(message);
                formatter.format("list of pools:\n");
                formatter
                        .format("POOL_NAME           POOL_SIZE   EVENT_TIME\n");

                for (int i = 0; i < poolArray.length; i++) {
                    formatter.format("%-18s %10d   %10d\n", poolArray[i]
                            .getName(), poolArray[i].getSize(), poolArray[i]
                            .getEventTime());

                    if (poolArray[i].ended()) {
                        pools.remove(poolArray[i].getName());
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
