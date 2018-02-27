/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ibis.ipl.registry.central.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.Location;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.registry.ControlPolicy;
import ibis.ipl.registry.central.Member;
import ibis.ipl.registry.central.Protocol;
import ibis.ipl.registry.central.RegistryProperties;
import ibis.ipl.server.RegistryServiceInterface;
import ibis.ipl.server.ServerProperties;
import ibis.ipl.server.Service;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

/**
 * Server for the centralized registry implementation.
 *
 */
public final class CentralRegistryService extends Thread
        implements Service, RegistryServiceInterface {

    // public static final int VIRTUAL_PORT = 302;

    private static final Logger logger = LoggerFactory
            .getLogger(CentralRegistryService.class);

    // do we remove old, terminated pools or not?
    // we shouldn't, but the ipl used to, so we keep this for backwards
    // compatibility for now.
    public static final boolean REMOVE_ENDED_POOLS = true;

    private static final long POOL_CLEANUP_TIMEOUT = 60 * 1000;

    private final VirtualSocketFactory socketFactory;

    private final SortedMap<String, Pool> pools;

    private final boolean printStats;

    private final boolean printEvents;

    private final boolean printErrors;

    private final int connectTimeout;

    private ServerConnectionHandler handler;

    private boolean stopped = false;

    /**
     * Constructor to create a registry server which is part of a IbisServer
     *
     * @param properties
     *            the properties
     * @param socketFactory
     *            the socket factory to use
     * @param policy
     *            the policy
     * @throws IOException
     *             when an IO error occurs
     */
    public CentralRegistryService(TypedProperties properties,
            VirtualSocketFactory socketFactory, ControlPolicy policy)
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

        connectTimeout = typedProperties.getIntProperty(
                RegistryProperties.SERVER_CONNECT_TIMEOUT) * 1000;

        pools = new TreeMap<String, Pool>();

        // start handling connections
        handler = new ServerConnectionHandler(this, socketFactory, policy);

        ThreadPool.createNew(this, "Central Registry Service");

        if (logger.isDebugEnabled()) {
            logger.debug("Started Central Registry service on virtual port "
                    + Protocol.VIRTUAL_PORT);
        }
    }

    synchronized Pool getPool(String poolName) {
        return pools.get(poolName);
    }

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.registry.central.server.RegistryService#getServiceName()
     */
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

        if (result == null || (result.hasEnded() && REMOVE_ENDED_POOLS)) {

            result = new Pool(poolName, socketFactory, peerBootstrap,
                    heartbeatInterval, eventPushInterval, gossip,
                    gossipInterval, adaptGossipInterval, tree, closedWorld,
                    poolSize, keepStatistics, statisticsInterval,
                    connectTimeout, implementationVersion, printEvents,
                    printErrors, purgeHistory);
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

    @Override
    public String toString() {
        return "Central Registry service on virtual port "
                + Protocol.VIRTUAL_PORT;
    }

    // pool cleanup thread
    @Override
    public synchronized void run() {

        while (!stopped) {
            if (pools.size() > 0) {
                String message = "list of pools:\n"
                        + "     CURRENT_SIZE JOINS LEAVES DIEDS ELECTIONS SIGNALS FIXED_SIZE CLOSED TERMINATED ENDED\n";

                // copy values to new array so we can do "remove" on original
                for (Pool pool : pools.values().toArray(new Pool[0])) {
                    if (printStats) {
                        message = message + pool.getStatsString() + "\n";
                    }

                    if (pool.hasEnded()) {
                        pool.saveStatistics();
                        if (REMOVE_ENDED_POOLS) {
                            pools.remove(pool.getName());
                        }
                        if (pools.size() == 0) {
                            notifyAll();
                        }
                    }

                }

                if (printStats) {
                    if (logger.isInfoEnabled()) {
                        logger.info(message);
                    } else {
                        System.err.println(message);
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

    /*
     * (non-Javadoc)
     *
     * @see ibis.ipl.registry.central.server.RegistryService#getStats()
     */
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

    public synchronized String[] getPools() {
        return pools.keySet().toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * ibis.ipl.registry.central.server.RegistryService#getMembers(java.lang
     * .String)
     */
    public IbisIdentifier[] getMembers(String poolName) {
        Pool pool = getPool(poolName);

        if (pool == null) {
            return new IbisIdentifier[0];
        }

        Member[] members = pool.getMembers();

        IbisIdentifier[] result = new IbisIdentifier[members.length];

        for (int i = 0; i < members.length; i++) {
            result[i] = members[i].getIbis();
        }

        return result;
    }

    // 1.5 Does not allow @Override for interfaces
    // @Override
    public String[] getLocations(String poolName) throws IOException {
        Pool pool = getPool(poolName);

        if (pool == null) {
            return new String[0];
        }

        Location[] locations = pool.getLocations();

        String[] result = new String[locations.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = locations[i].toString();
        }

        return result;
    }

    // 1.5 Does not allow @Override for interfaces
    // @Override
    public synchronized Map<String, Integer> getPoolSizes() throws IOException {
        Map<String, Integer> result = new HashMap<String, Integer>();

        for (Pool pool : pools.values()) {
            result.put(pool.getName(), pool.getSize());
        }
        return result;
    }

}
