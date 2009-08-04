package ibis.ipl.registry.btcentral.server;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisProperties;
import ibis.ipl.registry.ControlPolicy;
import ibis.ipl.registry.btcentral.Member;
import ibis.ipl.registry.btcentral.RegistryProperties;
import ibis.ipl.server.ServerProperties;
import ibis.ipl.server.Service;
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
public final class CentralRegistryService extends Thread implements Service {

    private static final Logger logger = LoggerFactory
            .getLogger(CentralRegistryService.class);

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    //private final VirtualSocketFactory socketFactory;

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
    public CentralRegistryService(TypedProperties properties,
            /*VirtualSocketFactory socketFactory, */ControlPolicy policy)
            throws IOException {
        //this.socketFactory = socketFactory;

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
        handler = new ServerConnectionHandler(this, properties, policy);

        ThreadPool.createNew(this, "Central Registry Service");

        logger.debug("Started BT Central Registry service");
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
            long statisticsInterval, boolean purgeHistory,
            String implementationVersion) throws IOException {
        Pool result = getPool(poolName);

        if (result == null || result.hasEnded()) {
            result = new Pool(poolName, peerBootstrap,
                    heartbeatInterval, eventPushInterval, gossip,
                    gossipInterval, adaptGossipInterval, tree, closedWorld,
                    poolSize, keepStatistics, statisticsInterval,
                    implementationVersion, printEvents, printErrors,
                    purgeHistory);
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
        return "BT Central Registry service";
    }

    // pool cleanup thread
    public synchronized void run() {

        while (!stopped) {
            if (pools.size() > 0) {
                if (printStats) {
                    System.err.printf("%tT list of pools:\n", System
                            .currentTimeMillis());
                    System.err
                            .println("     CURRENT_SIZE JOINS LEAVES DIEDS ELECTIONS SIGNALS FIXED_SIZE CLOSED ENDED");
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
    
    public IbisIdentifier[] getMembers(String poolName) {
        Pool pool = getPool(poolName);
        
        if (pool == null) {
            return null;
        }
        
        Member[] members = pool.getMembers();
        
        IbisIdentifier[] result = new IbisIdentifier[members.length];
        
        for(int i = 0; i < members.length; i++) {
            result[i] = members[i].getIbis();
        }
        
        return result;
    }

    public static void main(String[] args) {
        TypedProperties properties = new TypedProperties();
        properties.putAll(IbisProperties.getHardcodedProperties());
        properties.putAll(System.getProperties());

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--events")) {
                properties.setProperty(ServerProperties.PRINT_EVENTS, "true");
            } else if (args[i].equalsIgnoreCase("--errors")) {
                properties.setProperty(ServerProperties.PRINT_ERRORS, "true");
            } else if (args[i].equalsIgnoreCase("--stats")) {
                properties.setProperty(ServerProperties.PRINT_STATS, "true");
            }
        }
		try {
			CentralRegistryService inst = new CentralRegistryService(properties, null);
	    	inst.run();    	
		} catch (IOException e) {
			System.err.println("Unable to start BT central registry server");
			e.printStackTrace();
		}

    }
}
