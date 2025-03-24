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
package ibis.ipl.registry.central.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.io.Conversion;
import ibis.ipl.Credentials;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.registry.central.Event;
import ibis.ipl.registry.central.Protocol;
import ibis.ipl.registry.central.RegistryProperties;
import ibis.ipl.registry.statistics.Statistics;
import ibis.ipl.server.ServerProperties;
import ibis.ipl.support.Client;
import ibis.ipl.support.Connection;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

final class CommunicationHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 10;

    private static final int MAX_THREADS = 10;

    private static final Logger logger = LoggerFactory.getLogger(CommunicationHandler.class);

    // private final byte[] version;

    private final Heartbeat heartbeat;

    private final Client client;

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket serverSocket;

    private final VirtualSocketAddress serverAddress;

    private final Pool pool;

    private final TypedProperties properties;

    private final int timeout;

    private final Statistics statistics;

    // communication settings

    private final boolean peerBootstrap;

    private final boolean gossip;

    private final boolean tree;

    // bootstrap data

    private IbisIdentifier identifier;

    private IbisIdentifier[] bootstrapList;

    private int joinTime;

    private int currentNrOfThreads = 0;

    private int maxNrOfThreads = 0;

    CommunicationHandler(TypedProperties properties, Pool pool, Statistics statistics) throws IOException, IbisConfigurationException {
        this.properties = properties;
        this.pool = pool;
        this.statistics = statistics;

        if (properties.getProperty(IbisProperties.SERVER_ADDRESS) == null) {
            throw new IbisConfigurationException("cannot initialize registry, property " + IbisProperties.SERVER_ADDRESS + " is not specified");
        }

        gossip = properties.getBooleanProperty(RegistryProperties.GOSSIP);
        tree = properties.getBooleanProperty(RegistryProperties.TREE);

        if (gossip && tree) {
            throw new IbisConfigurationException("enabling both gossip and tree communication not allowed");
        }

        if (gossip) {
            peerBootstrap = true;
        } else if (tree) {
            peerBootstrap = false;
            if (properties.getBooleanProperty(RegistryProperties.PEER_BOOTSTRAP)) {
                throw new IbisConfigurationException("peer bootstrap not possible in combination with tree");
            }
        } else {
            peerBootstrap = properties.getBooleanProperty(RegistryProperties.PEER_BOOTSTRAP);
        }

        timeout = properties.getIntProperty(RegistryProperties.CLIENT_CONNECT_TIMEOUT) * 1000;

        String clientID = this.properties.getProperty(Ibis.ID_PROPERTY);
        client = Client.getOrCreateClient(clientID, properties, 0);
        virtualSocketFactory = client.getFactory();

        serverSocket = virtualSocketFactory.createServerSocket(Protocol.VIRTUAL_PORT, CONNECTION_BACKLOG, null);

        serverAddress = client.getServiceAddress(Protocol.VIRTUAL_PORT);

        if (serverAddress == null) {
            throw new IOException("could not get address of server");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("local address = " + serverSocket.getLocalSocketAddress());
            logger.debug("server address = " + serverAddress);
        }

        // init heartbeat

        long heartbeatInterval = properties.getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;

        boolean exitOnServerFailure = properties.getBooleanProperty(RegistryProperties.EXIT_ON_SERVER_FAILURE);

        heartbeat = new Heartbeat(this, pool, heartbeatInterval, exitOnServerFailure);

        // init gossiper (if needed)
        if (gossip) {
            long gossipInterval = properties.getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;

            new Gossiper(this, pool, gossipInterval);
        }

        // init broadcaster (if needed)
        if (tree) {
            Thread eventPusher = new IterativeEventPusher(pool, this);
            eventPusher.setDaemon(true);
            eventPusher.start();
        }

    }

    synchronized IbisIdentifier getIdentifier() {
        return identifier;
    }

    /**
     * connects to the registry server, joins, and gets back the identifier of this
     * Ibis and some bootstrap information
     *
     * @param applicationTag A tag for this ibis provided by the application
     *
     * @throws IOException in case of trouble
     */
    IbisIdentifier join(byte[] implementationData, String implementationVersion, Credentials credentials, byte[] tag) throws IOException {
        long start = System.currentTimeMillis();

        long heartbeatInterval = properties.getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;
        long eventPushInterval = properties.getIntProperty(RegistryProperties.EVENT_PUSH_INTERVAL) * 1000;
        long gossipInterval = properties.getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;
        boolean adaptGossipInterval = properties.getBooleanProperty(RegistryProperties.ADAPT_GOSSIP_INTERVAL);
        boolean keepStatistics = properties.getBooleanProperty(RegistryProperties.STATISTICS);
        long statisticsInterval = properties.getIntProperty(RegistryProperties.STATISTICS_INTERVAL) * 1000;
        boolean purgeHistory = properties.getBooleanProperty(RegistryProperties.PURGE_HISTORY);

        VirtualSocketAddress address = serverSocket.getLocalSocketAddress();

        // We try to generate an array of global IP addresses here. We use these
        // in an attempt to get a more reasonable location.
        InetAddress[] preferred = null;

        if (address.machine().hasPublicAddress()) {
            InetSocketAddress[] sa = address.machine().getPublicAddresses();
            preferred = new InetAddress[sa.length];

            for (int i = 0; i < sa.length; i++) {
                preferred[i] = sa[i].getAddress();
            }
        }

        Location location = Location.defaultLocation(properties, preferred);

        byte[] myAddress = address.toBytes();

        if (logger.isDebugEnabled()) {
            logger.debug("joining to " + pool.getName() + ", connecting to server");
        }
        Connection connection;
        try {
            connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);
        } catch (IOException e) {
            throw new IbisConfigurationException("Cannot connect to server at " + serverAddress + ", please check if it has been started", e);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("sending join info to server");
        }

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_JOIN);
            connection.out().writeUTF(ServerProperties.implementationVersion);

            connection.out().writeInt(myAddress.length);
            connection.out().write(myAddress);

            connection.out().writeUTF(pool.getName());
            connection.out().writeInt(implementationData.length);
            connection.out().write(implementationData);

            connection.out().writeUTF(implementationVersion);

            location.writeTo(connection.out());
            connection.out().writeBoolean(peerBootstrap);
            connection.out().writeLong(heartbeatInterval);
            connection.out().writeLong(eventPushInterval);
            connection.out().writeBoolean(gossip);
            connection.out().writeLong(gossipInterval);
            connection.out().writeBoolean(adaptGossipInterval);
            connection.out().writeBoolean(tree);
            connection.out().writeBoolean(pool.isClosedWorld());
            connection.out().writeInt(pool.getSize());
            connection.out().writeBoolean(keepStatistics);
            connection.out().writeLong(statisticsInterval);
            connection.out().writeBoolean(purgeHistory);

            byte[] credentialBytes = Conversion.object2byte(credentials);
            connection.out().writeInt(credentialBytes.length);
            connection.out().write(credentialBytes);

            if (tag == null) {
                connection.out().writeInt(-1);
            } else {
                connection.out().writeInt(tag.length);
                connection.out().write(tag);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("reading join result info from server");
            }

            connection.getAndCheckReply();

            IbisIdentifier identifier = new IbisIdentifier(connection.in());
            int joinTime = connection.in().readInt();
            int startOfEventListTime = connection.in().readInt();

            int listLength = connection.in().readInt();
            IbisIdentifier[] bootstrapList = new IbisIdentifier[listLength];
            for (int i = 0; i < listLength; i++) {
                bootstrapList[i] = new IbisIdentifier(connection.in());
            }
            connection.close();
            heartbeat.resetDeadlines();

            synchronized (this) {
                this.identifier = identifier;
                this.bootstrapList = bootstrapList;
                this.joinTime = joinTime;
            }

            // start saving event from the time the server expects
            pool.purgeHistoryUpto(startOfEventListTime);

            if (logger.isDebugEnabled()) {
                logger.debug("join done, identifier = " + identifier);
            }

            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_JOIN, end - start, connection.read(), connection.written(), false);
            }
            return identifier;
        } catch (IOException e) {
            // join failed
            connection.close();

            throw e;
        } finally {
            // start handling connections
            createThread();
        }

    }

    Client getClient() {
        return client;
    }

    void bootstrap() throws IOException {
        if (!peerBootstrap) {
            // we will receive bootstrap data from a push/forward/broadcast
            return;
        }

        long start = System.currentTimeMillis();

        IbisIdentifier identifier;
        IbisIdentifier[] bootstrapList;
        int joinTime;

        synchronized (this) {
            identifier = this.identifier;
            bootstrapList = this.bootstrapList;
            joinTime = this.joinTime;
        }

        for (IbisIdentifier ibis : bootstrapList) {
            if (!ibis.equals(identifier)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("trying to bootstrap with data from " + ibis);
                }
                Connection connection = null;
                try {
                    connection = new Connection(ibis, timeout, false, virtualSocketFactory, Protocol.VIRTUAL_PORT);

                    connection.out().writeByte(Protocol.MAGIC_BYTE);
                    connection.out().writeByte(Protocol.OPCODE_GET_STATE);

                    identifier.writeTo(connection.out());
                    connection.out().writeInt(joinTime);
                    connection.out().flush();

                    connection.getAndCheckReply();

                    pool.init(connection.in());
                    long end = System.currentTimeMillis();
                    if (statistics != null) {
                        statistics.add(Protocol.OPCODE_GET_STATE, end - start, connection.read(), connection.written(), false);
                    }
                    return;
                } catch (Exception e) {
                    if (logger.isInfoEnabled()) {
                        logger.info("bootstrap with " + ibis + " failed, trying next one", e);
                    }
                } finally {
                    if (connection != null) {
                        connection.close();
                    }
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("could not bootstrap registry with any peer, trying server");
        }

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);
        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_STATE);
            identifier.writeTo(connection.out());
            connection.out().writeInt(joinTime);
            connection.out().flush();

            connection.getAndCheckReply();

            pool.init(connection.in());
            connection.close();

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_GET_STATE, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void signal(String signal, ibis.ipl.IbisIdentifier... ibisses) throws IOException {
        long start = System.currentTimeMillis();

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SIGNAL);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(signal);
            connection.out().writeInt(ibisses.length);
            for (ibis.ipl.IbisIdentifier element : ibisses) {
                ((IbisIdentifier) element).writeTo(connection.out());
            }
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("done telling " + ibisses.length + " ibisses a string: " + signal);
            }

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_SIGNAL, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void terminate() throws IOException {
        long start = System.currentTimeMillis();

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_TERMINATE);
            getIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("done terminating");
            }

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_TERMINATE, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }

    }

    public long getSeqno(String name) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException("cannot get sequence number, registry already stopped");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getting sequence number");
        }
        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SEQUENCE_NR);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(name);
            connection.out().flush();

            connection.getAndCheckReply();

            long result = connection.in().readLong();

            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("sequence number = " + result);
            }

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_SEQUENCE_NR, end - start, connection.read(), connection.written(), false);
            }

            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException("cannot do assumeDead, registry already stopped");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("declaring " + ibis + " to be dead");
        }

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_DEAD);
            getIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("done declaring " + ibis + " dead ");
            }

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_DEAD, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException("cannot do maybeDead, registry already stopped");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("reporting " + ibis + " to possibly be dead");
        }

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_MAYBE_DEAD);
            getIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("done reporting " + ibis + " to possibly be dead");
            }

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_MAYBE_DEAD, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    /**
     * Contact server, to get new events, and to let the server know we are still
     * alive
     *
     * @return true if sending the heartbeat succeeded, or was unnecessary, or false
     *         if sending the heartbeat failed
     *
     */
    boolean sendHeartBeat() {
        long start = System.currentTimeMillis();

        if ((getIdentifier() == null) || pool.isStopped()) {
            // pool already stopped
            return true;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("sending heartbeat to server");
        }

        Connection connection = null;
        try {
            connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_HEARTBEAT);
            getIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_HEARTBEAT, end - start, connection.read(), connection.written(), false);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("sent heartbeat");
            }
            return true;
        } catch (Exception e) {
            if (connection != null) {
                connection.close();
            }
            if (logger.isInfoEnabled()) {
                logger.info(identifier + ": could not send heartbeat to server", e);
            }
            return false;
        }
    }

    public void leave() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("leaving pool");
        }

        long start = System.currentTimeMillis();

        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_LEAVE);
            getIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            heartbeat.resetDeadlines();

            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_LEAVE, end - start, connection.read(), connection.written(), false);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("left");
            }

        } finally {
            connection.close();
            pool.stop();
            end();
            heartbeat.nudge();
        }
    }

    public IbisIdentifier elect(String election, long timeout) throws IOException {
        long start = System.currentTimeMillis();

        if (timeout > Integer.MAX_VALUE) {
            timeout = Integer.MAX_VALUE;
        }
        if (timeout == 0) {
            // we are allowed to wait forever, but no need to wait more than
            // until we are sure we should have connected with the server
            timeout = this.timeout;
        }

        Connection connection = new Connection(serverAddress, (int) timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_ELECT);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(election);
            connection.out().flush();

            connection.getAndCheckReply();

            IbisIdentifier winner = new IbisIdentifier(connection.in());

            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("election : \"" + election + "\" done, result = " + winner);
            }

            heartbeat.resetDeadlines();

            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_ELECT, end - start, connection.read(), connection.written(), false);
            }

            return winner;

        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    void gossip(IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (ibis.equals(getIdentifier())) {
            if (logger.isDebugEnabled()) {
                logger.debug("not gossiping with self");
            }
            return;
        }

        if (pool.isStopped()) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("gossiping with " + ibis);
        }

        Connection connection = new Connection(ibis, timeout, false, virtualSocketFactory, Protocol.VIRTUAL_PORT);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GOSSIP);
            getIdentifier().writeTo(connection.out());
            int localTime = pool.getTime();
            connection.out().writeInt(localTime);
            connection.out().flush();

            connection.getAndCheckReply();

            int peerTime = connection.in().readInt();

            Event[] newEvents = null;
            if (peerTime > localTime) {
                if (logger.isDebugEnabled()) {
                    logger.debug("localtime = " + localTime + ", peerTime = " + peerTime + ", receiving events");
                }

                int nrOfEvents = connection.in().readInt();
                if (nrOfEvents > 0) {
                    newEvents = new Event[nrOfEvents];
                    for (int i = 0; i < newEvents.length; i++) {
                        newEvents[i] = new Event(connection.in());
                    }
                }
            } else if (peerTime < localTime) {
                if (logger.isDebugEnabled()) {
                    logger.debug("localtime = " + localTime + ", peerTime = " + peerTime + ", pushing events");
                }

                Event[] sendEvents = pool.getEventsFrom(peerTime);

                connection.out().writeInt(sendEvents.length);
                for (Event event : sendEvents) {
                    event.writeTo(connection.out());
                }

            } else {
                // nothing to send either way
            }
            if (logger.isDebugEnabled()) {
                logger.debug("gossiping with " + ibis + " done, time now: " + pool.getTime());
            }
            connection.close();

            if (newEvents != null) {
                pool.newEventsReceived(newEvents);
            }
            long end = System.currentTimeMillis();
            if (statistics != null) {

                statistics.add(Protocol.OPCODE_GOSSIP, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    void forward(IbisIdentifier ibis) {
        byte opcode = Protocol.OPCODE_FORWARD;
        long start = System.currentTimeMillis();

        if (ibis.equals(getIdentifier())) {
            logger.debug("not forwarding events to self");
            return;
        }

        if (pool.isStopped()) {
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(identifier + ": forwarding to: " + ibis);
        }

        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("creating connection to push events to " + ibis);
            }

            connection = new Connection(ibis, timeout, false, virtualSocketFactory, Protocol.VIRTUAL_PORT);

            if (logger.isDebugEnabled()) {
                logger.debug("connection to " + ibis + " created");
            }

            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(opcode);
            connection.out().writeUTF(pool.getName());
            connection.out().flush();

            if (logger.isDebugEnabled()) {
                logger.debug("waiting for peer time of peer " + ibis);
            }
            boolean requestBootstrap = connection.in().readBoolean();
            int peerJoinTime = connection.in().readInt();
            int requestedEventTime = connection.in().readInt();

            if (logger.isDebugEnabled()) {
                logger.debug("request bootstrap = " + requestBootstrap + ", peerJoinTime = " + peerJoinTime + ", requested event time = "
                        + requestedEventTime);
            }

            connection.sendOKReply();

            // send bootstrap (if needed)
            if (requestBootstrap) {
                if (logger.isDebugEnabled()) {
                    logger.debug("sending state");
                }
                pool.writeState(connection.out(), peerJoinTime);

            }

            Event[] events = pool.getEventsFrom(requestedEventTime);

            if (logger.isDebugEnabled()) {
                logger.debug("sending " + events.length + " entries to " + ibis);
            }

            connection.out().writeInt(events.length);
            for (Event event : events) {
                event.writeTo(connection.out());
            }

            // no updated of minimum time
            connection.out().writeInt(-1);

            connection.close();

            if (logger.isDebugEnabled()) {
                logger.debug("connection to " + ibis + " closed");
            }
            if (statistics != null) {
                statistics.add(opcode, System.currentTimeMillis() - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            if (pool.isMember(ibis)) {
                logger.error("cannot reach " + ibis + " to push events to", e);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }

    private void handleGossip(Connection connection) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("got a gossip request");
        }

        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String poolName = identifier.poolName();
        int peerTime = connection.in().readInt();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of " + pool.getName());
            connection.closeWithError("wrong pool: " + poolName + " instead of " + pool.getName());
            return;
        }

        int localTime = pool.getTime();

        connection.sendOKReply();
        connection.out().writeInt(localTime);
        connection.out().flush();

        if (localTime > peerTime) {
            Event[] sendEvents = pool.getEventsFrom(peerTime);

            connection.out().writeInt(sendEvents.length);
            for (Event event : sendEvents) {
                event.writeTo(connection.out());
            }
            connection.out().flush();

        } else if (localTime < peerTime) {
            int nrOfEvents = connection.in().readInt();

            if (nrOfEvents > 0) {
                Event[] newEvents = new Event[nrOfEvents];
                for (int i = 0; i < newEvents.length; i++) {
                    newEvents[i] = new Event(connection.in());
                }

                connection.close();

                pool.newEventsReceived(newEvents);
            }
        }
        connection.close();
    }

    private void handlePush(Connection connection, byte opcode) throws IOException {
        Event[] newEvents = null;
        if (logger.isDebugEnabled()) {
            logger.debug("got a push/forward/broadcast");
        }

        long start = System.currentTimeMillis();

        String poolName = connection.in().readUTF();

        long readPoolName = System.currentTimeMillis();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of " + pool.getName());
            connection.closeWithError("wrong pool: " + poolName + " instead of " + pool.getName());
        }

        boolean requestBootstrap = !peerBootstrap && !pool.isInitialized();
        int nextRequiredEvent = pool.getNextRequiredEvent();
        int joinTime;

        long gatheredPoolData = System.currentTimeMillis();

        synchronized (this) {
            joinTime = this.joinTime;
        }

        long gatheredData = System.currentTimeMillis();

        connection.out().writeBoolean(requestBootstrap);
        connection.out().writeInt(joinTime);
        connection.out().writeInt(nextRequiredEvent);

        connection.out().flush();

        long sendData = System.currentTimeMillis();

        connection.getAndCheckReply();

        long gotReply = System.currentTimeMillis();

        if (requestBootstrap) {
            if (logger.isDebugEnabled()) {
                logger.debug("recieving bootstrap in push");
            }
            // we should receive the bootstrap data next
            pool.init(connection.in());
        }

        long readBootstrap = System.currentTimeMillis();

        int events = connection.in().readInt();

        if (logger.isDebugEnabled()) {
            logger.debug("receiving " + events + " events");
        }

        if (events < 0) {
            connection.closeWithError("negative event value");
            return;
        }

        newEvents = new Event[events];
        for (int i = 0; i < newEvents.length; i++) {
            newEvents[i] = new Event(connection.in());
            if (logger.isDebugEnabled()) {
                logger.debug("received event " + newEvents[i]);
            }
        }

        int minEventTime = connection.in().readInt();

        long readEvents = System.currentTimeMillis();

        connection.close();

        long closedConnection = System.currentTimeMillis();

        if (newEvents != null) {
            pool.newEventsReceived(newEvents);
        }

        if (minEventTime != -1) {
            pool.purgeHistoryUpto(minEventTime);
        }

        long done = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            if (opcode == Protocol.OPCODE_BROADCAST) {
                logger.debug("readPoolName = " + (readPoolName - start) + ", gatheredPoolData = " + (gatheredPoolData - readPoolName)
                        + ", gatheredData = " + (gatheredData - gatheredPoolData) + ", sendData = " + (sendData - gatheredData) + ", gotReply = "
                        + (gotReply - sendData) + ", readBootstrap = " + (readBootstrap - gotReply) + ", readEvents = " + (readEvents - readBootstrap)
                        + ", closedConnection = " + (closedConnection - readEvents) + ", done = " + (done - closedConnection));
            }
            logger.debug("push handled");
        }
    }

    private void handlePing(Connection connection) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("got a ping request");
        }
        IbisIdentifier identifier = getIdentifier();
        if (identifier == null) {
            connection.closeWithError("ibis identifier not initialized yet");
            return;
        }
        connection.sendOKReply();
        getIdentifier().writeTo(connection.out());
        connection.out().flush();
        connection.close();
    }

    private void handleGetState(Connection connection) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("got a state request");
        }

        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        int joinTime = connection.in().readInt();

        String poolName = identifier.poolName();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of " + pool.getName());
            connection.closeWithError("wrong pool: " + poolName + " instead of " + pool.getName());
            return;
        }

        if (!pool.isInitialized()) {
            connection.closeWithError("state not available");
            return;
        }

        connection.sendOKReply();

        pool.writeState(connection.out(), joinTime);

        connection.out().flush();
        connection.close();
    }

    private synchronized void createThread() {
        while (currentNrOfThreads >= MAX_THREADS) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        // create new thread for next connection
        ThreadPool.createNew(this, "client connection handler");
        currentNrOfThreads++;

        if (currentNrOfThreads > maxNrOfThreads) {
            maxNrOfThreads = currentNrOfThreads;
        }
    }

    private synchronized void threadEnded() {
        currentNrOfThreads--;

        notifyAll();
    }

    @Override
    public void run() {
        Connection connection = null;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("accepting connection");
            }
            connection = new Connection(serverSocket);
            if (logger.isDebugEnabled()) {
                logger.debug("connection accepted");
            }
        } catch (IOException e) {
            if (pool.isStopped()) {
                threadEnded();
                return;
            }
            logger.error("Accept failed, waiting a second, will retry", e);

            // wait a bit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // IGNORE
            }
        }

        // create new thread for next connection
        createThread();

        if (connection == null) {
            threadEnded();
            return;
        }

        try {
            long start = System.currentTimeMillis();

            byte magic = connection.in().readByte();

            if (magic != Protocol.MAGIC_BYTE) {
                throw new IOException("Invalid header byte in accepting connection");
            }

            byte opcode = connection.in().readByte();

            if (logger.isDebugEnabled() && opcode < Protocol.NR_OF_OPCODES) {
                logger.debug("received request: " + Protocol.OPCODE_NAMES[opcode]);
            }

            switch (opcode) {
            case Protocol.OPCODE_GOSSIP:
                handleGossip(connection);
                break;
            case Protocol.OPCODE_PUSH:
            case Protocol.OPCODE_BROADCAST:
            case Protocol.OPCODE_FORWARD:
                handlePush(connection, opcode);
                break;
            case Protocol.OPCODE_PING:
                handlePing(connection);
                break;
            case Protocol.OPCODE_GET_STATE:
                handleGetState(connection);
                break;
            default:
                logger.error("unknown opcode in request: " + opcode);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("done handling request");
            }

            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(opcode, end - start, connection.read(), connection.written(), true);
            }
        } catch (Throwable e) {
            logger.error("error on handling request", e);
        } finally {
            connection.close();
        }
        threadEnded();
    }

    void end() {
        try {
            serverSocket.close();
        } catch (Exception e) {
            // IGNORE
        }

        try {
            virtualSocketFactory.end();
        } catch (Exception e) {
            // IGNORE
        }
    }

    public void addTokens(String name, int count) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException("cannot add tokens, registry already stopped");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("adding tokens");
        }
        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_ADD_TOKENS);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(name);
            connection.out().writeInt(count);
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_ADD_TOKENS, end - start, connection.read(), connection.written(), false);
            }
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public String getToken(String name) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException("cannot get tokens, registry already stopped");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("getting token");
        }
        Connection connection = new Connection(serverAddress, timeout, true, virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_TOKEN);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(name);
            connection.out().flush();

            connection.getAndCheckReply();
            int reply = connection.in().readInt();
            connection.close();

            heartbeat.resetDeadlines();
            long end = System.currentTimeMillis();
            if (statistics != null) {
                statistics.add(Protocol.OPCODE_GET_TOKEN, end - start, connection.read(), connection.written(), false);
            }
            return reply == 0 ? null : name;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

}
