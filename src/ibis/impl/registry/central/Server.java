package ibis.impl.registry.central;

import ibis.impl.registry.RegistryProperties;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

import org.apache.log4j.Logger;

public final class Server extends ibis.impl.registry.Server {

    private static final Logger logger = Logger.getLogger(Registry.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final ConnectionFactory connectionFactory;

    private final HashMap<String, Pool> pools;
    
    //if non-null, this server will ONLY serve this one pool, and stop after
    //the pool has ended
    private final boolean single;
    
    private ServerConnectionHandler handler;

    private boolean stopped = false;
    
    public Server(Properties properties) throws IOException {
        TypedProperties typedProperties = new TypedProperties(properties);        
        
        int port = typedProperties.getIntProperty(RegistryProperties.SERVER_PORT, Protocol.DEFAULT_PORT);
        
        if (port <= 0) {
            throw new IOException("can only start registry server on a positive port");
        }

        boolean smart = typedProperties.booleanProperty(RegistryProperties.SMARTSOCKETS, true);

        connectionFactory = new ConnectionFactory(port, smart, null, properties);
        
        single = typedProperties.booleanProperty(RegistryProperties.SERVER_SINGLE);

        pools = new HashMap<String, Pool>();
    }


    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    // atomic get/create pool
    synchronized Pool getAndCreatePool(String poolName, boolean gossip, boolean keepNodeState) throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.ended()) {
            logger.info("creating new pool: " + poolName);
            result = new Pool(poolName, connectionFactory, gossip, keepNodeState);
            pools.put(poolName, result);
        }

        return result;
    }

    synchronized boolean isStopped() {
        return stopped;
    }
    
    
    public synchronized void stopServer() {
        stopped = true;
        notifyAll();
        connectionFactory.end();
        handler.printStats(false);
    }

    // pool cleanup thread
    public synchronized void run() {
        // start handling connections
        handler = new ServerConnectionHandler(this, connectionFactory);

        while (!stopped) {
            Pool[] poolArray = pools.values().toArray(new Pool[0]);

            for (int i = 0; i < poolArray.length; i++) {
                if (poolArray[i].ended()) {
                    pools.remove(poolArray[i].getName());
                    if (single && pools.size() == 0) {
                        logger.info("server exiting");
                        stopServer();
                        return;
                    }
                }
                logger.info("event time for pool " + poolArray[i].getName() + " with " + poolArray[i].getSize()
                        + " members now " + poolArray[i].getEventTime());
            }

            try {
                wait(POOL_CLEANUP_TIMEOUT);
            } catch (InterruptedException e) {
                // IGNORE
            }
            
            handler.printStats(false);
        }

    }

    @Override
    public String getLocalAddress() {
        return connectionFactory.getAddressString();
    }
}
