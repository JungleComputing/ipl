package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.registry.RegistryProperties;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Server for the centralized registry implementation.
 * 
 */
public final class Server extends ibis.ipl.impl.registry.Server {

    private static final Logger logger = Logger.getLogger(Server.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final ConnectionFactory connectionFactory;

    private final HashMap<String, Pool> pools;

    // if non-null, this server will ONLY serve this one pool, and stop after
    // the pool has ended
    private final boolean single;

    private ServerConnectionHandler handler;

    private boolean stopped = false;

    /**
     * Creates a registry server with the given properties
     * 
     * @param properties
     *            settings for this server.
     * @throws IOException
     */
    public Server(Properties properties) throws IOException {
        TypedProperties typedProperties = new TypedProperties(properties);

        int port = typedProperties
                .getIntProperty(RegistryProperties.SERVER_PORT);

        if (port <= 0) {
            throw new IOException(
                    "can only start registry server on a positive port");
        }

        boolean smart = typedProperties.booleanProperty(
                RegistryProperties.CENTRAL_SMARTSOCKETS, true);

        connectionFactory = new ConnectionFactory(port, smart, properties
                .getProperty(RegistryProperties.SERVER_HUB_ADDRESS));

        single = typedProperties
                .booleanProperty(RegistryProperties.SERVER_SINGLE);

        pools = new HashMap<String, Pool>();
    }

    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    // atomic get/create pool
    synchronized Pool getAndCreatePool(String poolName, boolean gossip,
            boolean keepNodeState) throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.ended()) {
            logger.debug("creating new pool: " + poolName);
            result = new Pool(poolName, connectionFactory, gossip,
                    keepNodeState);
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
    public synchronized void stopServer() {
        stopped = true;
        notifyAll();
        connectionFactory.end();
        logger.info(handler.getStats(false));
    }

    // force the server to check the pools _now_
    synchronized void nudge() {
        notifyAll();
    }

    // pool cleanup thread
    public synchronized void run() {
        // start handling connections
        handler = new ServerConnectionHandler(this, connectionFactory);

        while (!stopped) {
            StringBuilder message = new StringBuilder(handler.getStats(false));
            Formatter formatter = new Formatter(message);
            formatter.format("\nPOOL_NAME           POOL_SIZE   EVENT_TIME\n");

            Pool[] poolArray = pools.values().toArray(new Pool[0]);
            if (poolArray.length == 0) {
                formatter.format("%-18s %10d   %10d\n", "-", 0, 0);
            }

            for (int i = 0; i < poolArray.length; i++) {
                formatter.format("%-18s %10d   %10d\n", poolArray[i].getName(),
                        poolArray[i].getSize(), poolArray[i].getEventTime());

                if (poolArray[i].ended()) {
                    pools.remove(poolArray[i].getName());
                    if (single && pools.size() == 0) {
                        stopServer();
                        logger.info("server exiting");
                        return;
                    }
                }

            }
            logger.info(message);

            try {
                wait(POOL_CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                // IGNORE
            }

        }

    }

    @Override
    public String getLocalAddress() {
        return connectionFactory.getAddressString();
    }

    @Override
    public String toString() {
        return "central server on " + getLocalAddress();
    }
}
