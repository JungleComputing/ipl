package ibis.ipl.impl.registry.central.client;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.central.Connection;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.ipl.impl.registry.central.Stats;
import ibis.ipl.impl.registry.central.server.Server;
import ibis.server.Client;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

final class CommunicationHandler implements Runnable {

    private static final int CONNECTION_BACKLOG = 50;

    private static final Logger logger = Logger
            .getLogger(CommunicationHandler.class);

    private final Heartbeat heartbeat;

    private final VirtualSocketFactory virtualSocketFactory;

    private final VirtualServerSocket serverSocket;

    private final VirtualSocketAddress serverAddress;

    private final Pool pool;

    private final TypedProperties properties;

    private final int timeout;

    private final Stats stats;

    // bootstrap data

    private IbisIdentifier identifier;

    private int bootstrapTime;

    private IbisIdentifier[] bootstrapList;

    CommunicationHandler(TypedProperties properties, Pool pool)
            throws IOException {
        this.properties = properties;
        this.pool = pool;

        if (properties.getProperty(IbisProperties.SERVER_ADDRESS) == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.SERVER_ADDRESS
                            + " is not specified");
        }

        timeout = properties
                .getIntProperty(RegistryProperties.CLIENT_CONNECT_TIMEOUT) * 1000;

        stats = new Stats(Protocol.NR_OF_OPCODES);

        try {
            virtualSocketFactory = Client.getFactory(properties);
        } catch (InitializationException e) {
            throw new IOException("Could not create socket factory: " + e);
        }

        serverSocket = virtualSocketFactory.createServerSocket(0,
                CONNECTION_BACKLOG, null);

        serverAddress = Client.getServiceAddress(Server.VIRTUAL_PORT,
                properties);

        logger.debug("local address = " + serverSocket.getLocalSocketAddress());
        logger.debug("server address = " + serverAddress);

        // init heartbeat

        long heartbeatInterval = properties
                .getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;

        heartbeat = new Heartbeat(this, pool, heartbeatInterval);

        // init gossiper (if needed)
        if (properties.getBooleanProperty(RegistryProperties.GOSSIP)) {
            long gossipInterval = properties
                    .getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;

            new Gossiper(this, pool, gossipInterval);
        }

        // init broadcaster (if needed)
        if (properties.getBooleanProperty(RegistryProperties.TREE)) {
            new IterativeEventPusher(pool, this);
        }

        ThreadPool.createNew(this, "client connection handler");
    }

    synchronized IbisIdentifier getIdentifier() {
        return identifier;
    }

    public Map<String, String> getStats() {
        return stats.getMap();
    }

    /**
     * connects to the registry server, joins, and gets back the identifier of
     * this Ibis and some bootstrap information
     * 
     * @throws IOException
     *                 in case of trouble
     */
    IbisIdentifier join(byte[] implementationData,
            String ibisImplementationIdentifier) throws IOException {
        long start = System.currentTimeMillis();

        long heartbeatInterval = properties
                .getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;
        long eventPushInterval = properties
                .getIntProperty(RegistryProperties.EVENT_PUSH_INTERVAL) * 1000;
        boolean gossip = properties
                .getBooleanProperty(RegistryProperties.GOSSIP);
        long gossipInterval = properties
                .getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;
        boolean adaptGossipInterval = properties
                .getBooleanProperty(RegistryProperties.ADAPT_GOSSIP_INTERVAL);
        boolean tree = properties.getBooleanProperty(RegistryProperties.TREE);

        Location location = Location.defaultLocation(properties);

        byte[] myAddress = serverSocket.getLocalSocketAddress().toBytes();

        logger.debug("joining to " + pool.getName() + ", connecting to server");
        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        logger.debug("sending join info to server");

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_JOIN);
            connection.out().writeInt(myAddress.length);
            connection.out().write(myAddress);

            connection.out().writeUTF(pool.getName());
            connection.out().writeInt(implementationData.length);
            connection.out().write(implementationData);
            connection.out().writeUTF(ibisImplementationIdentifier);
            location.writeTo(connection.out());
            connection.out().writeLong(heartbeatInterval);
            connection.out().writeLong(eventPushInterval);
            connection.out().writeBoolean(gossip);
            connection.out().writeLong(gossipInterval);
            connection.out().writeBoolean(adaptGossipInterval);
            connection.out().writeBoolean(tree);
            connection.out().writeBoolean(pool.isClosedWorld());
            connection.out().writeInt(pool.getSize());
            connection.out().flush();

            logger.debug("reading join result info from server");

            connection.getAndCheckReply();

            IbisIdentifier identifier = new IbisIdentifier(connection.in());

            // mimimum event time we need as a bootstrap
            int time = connection.in().readInt();

            int listLength = connection.in().readInt();
            IbisIdentifier[] bootstrapList = new IbisIdentifier[listLength];
            for (int i = 0; i < listLength; i++) {
                bootstrapList[i] = new IbisIdentifier(connection.in());
            }
            connection.close();
            heartbeat.updateHeartbeatDeadline();

            synchronized (this) {
                this.identifier = identifier;
                this.bootstrapTime = time;
                this.bootstrapList = bootstrapList;
            }

            logger.debug("join done");

            // if anyone asks, report we have all events upto our join time
            pool.setTime(bootstrapTime);

            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_JOIN, end - start, connection.read(),
                    connection.written(), false);

            return identifier;
        } catch (IOException e) {
            // join failed
            connection.close();
            throw e;
        }
    }

    void bootstrap() throws IOException {
        long start = System.currentTimeMillis();

        IbisIdentifier identifier;
        IbisIdentifier[] bootstrapList;
        int bootstrapTime;

        synchronized (this) {
            identifier = this.identifier;
            bootstrapList = this.bootstrapList;
            bootstrapTime = this.bootstrapTime;
        }

        boolean peerBootstrap = properties
                .getBooleanProperty(RegistryProperties.PEER_BOOTSTRAP);

        if (peerBootstrap) {
            for (IbisIdentifier ibis : bootstrapList) {
                if (!ibis.equals(identifier)) {
                    logger.debug("trying to bootstrap with data from " + ibis);
                    Connection connection = null;
                    try {
                        connection = new Connection(ibis, timeout, false,
                                virtualSocketFactory);

                        connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
                        connection.out().writeByte(Protocol.OPCODE_GET_STATE);

                        identifier.writeTo(connection.out());
                        connection.out().writeInt(bootstrapTime);
                        connection.out().flush();

                        connection.getAndCheckReply();

                        pool.init(connection.in());
                        long end = System.currentTimeMillis();
                        stats.add(Protocol.OPCODE_GET_STATE, end - start,
                                connection.read(), connection.written(), false);
                        return;
                    } catch (Exception e) {
                        logger.info("bootstrap with " + ibis
                                + " failed, trying next one", e);
                    } finally {
                        if (connection != null) {
                            connection.close();
                        }
                    }
                }
            }
            logger
                    .debug("could not bootstrap registry with any peer, trying server");
        }
        logger.debug("bootstrapping with server");
        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);
        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_STATE);
            identifier.writeTo(connection.out());
            connection.out().writeInt(bootstrapTime);
            connection.out().flush();

            connection.getAndCheckReply();

            pool.init(connection.in());
            connection.close();

            heartbeat.updateHeartbeatDeadline();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_GET_STATE, end - start,
                    connection.read(), connection.written(), false);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void signal(String signal, ibis.ipl.IbisIdentifier... ibisses)
            throws IOException {
        long start = System.currentTimeMillis();

        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SIGNAL);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(signal);
            connection.out().writeInt(ibisses.length);
            for (int i = 0; i < ibisses.length; i++) {
                ((IbisIdentifier) ibisses[i]).writeTo(connection.out());
            }
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            logger.debug("done telling " + ibisses.length
                    + " ibisses a signal: " + signal);

            heartbeat.updateHeartbeatDeadline();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_SIGNAL, end - start, connection.read(),
                    connection.written(), false);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public long getSeqno(String name) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException(
                    "cannot get sequence number, registry already stopped");
        }

        logger.debug("getting sequence number");
        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SEQUENCE_NR);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(name);
            connection.out().flush();

            connection.getAndCheckReply();

            long result = connection.in().readLong();

            connection.close();

            logger.debug("sequence number = " + result);

            heartbeat.updateHeartbeatDeadline();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_SEQUENCE_NR, end - start, connection
                    .read(), connection.written(), false);

            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do assumeDead, registry already stopped");
        }

        logger.debug("declaring " + ibis + " to be dead");

        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_DEAD);
            getIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            logger.debug("done declaring " + ibis + " dead ");

            heartbeat.updateHeartbeatDeadline();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_DEAD, end - start, connection.read(),
                    connection.written(), false);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do maybeDead, registry already stopped");
        }

        logger.debug("reporting " + ibis + " to possibly be dead");

        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_MAYBE_DEAD);
            getIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            logger.debug("done reporting " + ibis + " to possibly be dead");

            heartbeat.updateHeartbeatDeadline();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_MAYBE_DEAD, end - start, connection
                    .read(), connection.written(), false);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    /**
     * Contact server, to get new events, and to let the server know we are
     * still alive
     * 
     * @throws IOException
     *                 in case of trouble
     */
    void sendHeartBeat() {
        long start = System.currentTimeMillis();

        if (getIdentifier() == null) {
            // not joined yet
            return;
        }

        logger.debug("sending heartbeat to server");

        Connection connection = null;
        try {
            connection = new Connection(serverAddress, timeout, true,
                    virtualSocketFactory);

            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_HEARTBEAT);
            getIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_HEARTBEAT, end - start,
                    connection.read(), connection.written(), false);
            logger.debug("send heartbeat");
        } catch (Exception e) {
            if (connection != null) {
                connection.close();
            }
            logger.error("could not send heartbeat", e);
        }
    }

    public void leave() throws IOException {
        logger.debug("leaving pool");

        long start = System.currentTimeMillis();

        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_LEAVE);
            getIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            heartbeat.updateHeartbeatDeadline();

            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_LEAVE, end - start, connection.read(),
                    connection.written(), false);

            logger.debug("left");
        } finally {
            connection.close();
            pool.stop();
            end();
        }
    }

    public IbisIdentifier elect(String election) throws IOException {
        long start = System.currentTimeMillis();
        Connection connection = new Connection(serverAddress, timeout, true,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_ELECT);
            getIdentifier().writeTo(connection.out());
            connection.out().writeUTF(election);
            connection.out().flush();

            connection.getAndCheckReply();

            IbisIdentifier winner = new IbisIdentifier(connection.in());

            connection.close();

            logger.debug("election : \"" + election + "\" done, result = "
                    + winner);

            heartbeat.updateHeartbeatDeadline();

            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_ELECT, end - start, connection.read(),
                    connection.written(), false);

            return winner;

        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    void gossip(IbisIdentifier ibis) throws IOException {
        long start = System.currentTimeMillis();

        if (ibis.equals(getIdentifier())) {
            logger.debug("not gossiping with self");
            return;
        }

        logger.debug("gossiping with " + ibis);

        Connection connection = new Connection(ibis, timeout, false,
                virtualSocketFactory);

        try {
            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GOSSIP);
            getIdentifier().writeTo(connection.out());
            int localTime = pool.getTime();
            connection.out().writeInt(localTime);
            connection.out().flush();

            connection.getAndCheckReply();

            int peerTime = connection.in().readInt();

            Event[] newEvents;
            if (peerTime > localTime) {
                logger.debug("localtime = " + localTime + ", peerTime = "
                        + peerTime + ", receiving events");

                int nrOfEvents = connection.in().readInt();
                if (nrOfEvents > 0) {
                    newEvents = new Event[nrOfEvents];
                    for (int i = 0; i < newEvents.length; i++) {
                        newEvents[i] = new Event(connection.in());
                    }
                    pool.newEventsReceived(newEvents);
                }
                connection.close();
            } else if (peerTime < localTime) {
                logger.debug("localtime = " + localTime + ", peerTime = "
                        + peerTime + ", pushing events");

                Event[] sendEvents = pool.getEventsFrom(peerTime);

                connection.out().writeInt(sendEvents.length);
                for (Event event : sendEvents) {
                    event.writeTo(connection.out());
                }

            } else {
                // nothing to send either way
            }
            logger.debug("gossiping with " + ibis + " done, time now: "
                    + pool.getTime());
            connection.close();
            long end = System.currentTimeMillis();
            stats.add(Protocol.OPCODE_GOSSIP, end - start, connection.read(),
                    connection.written(), false);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    void push(IbisIdentifier ibis) {
        // Connection connection = null;
        // try {
        // connection =
        // new Connection(ibis, timeout, false, virtualSocketFactory);
        //
        // connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
        // connection.out().writeByte(Protocol.OPCODE_PUSH);
        //            
        //            
        //            
        //
        // connection.out().writeInt(events.length);
        // for (Event event : events) {
        // event.writeTo(connection.out());
        // }
        //
        // connection.getAndCheckReply();
        // connection.close();
        // long end = System.currentTimeMillis();
        // stats.add(Protocol.OPCODE_PUSH, end - start, false);
        // } catch (IOException e) {
        // logger.error("could not forward events to " + ibis, e);
        // if (connection != null) {
        // connection.close();
        // }
        // }

        long start = System.currentTimeMillis();

        if (ibis.equals(getIdentifier())) {
            logger.debug("not forwarding events to self");
            return;
        }

        logger.debug(identifier + ": forwarding to: " + ibis);

        Connection connection = null;
        try {

            int peerTime = 0;

            logger.debug("creating connection to push events to " + ibis);

            connection = new Connection(ibis, timeout, false,
                    virtualSocketFactory);

            logger.debug("connection to " + ibis + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_PUSH);
            connection.out().writeUTF(pool.getName());
            connection.out().flush();

            logger.debug("waiting for peer time of peer " + ibis);
            peerTime = connection.in().readInt();

            Event[] events;
            if (peerTime == -1) {
                // peer not finished join yet.
                events = new Event[0];

            } else {
                events = pool.getEventsFrom(peerTime);
            }

            if (events == null) {
                connection.closeWithError("could not get events");
                return;
            }
            connection.sendOKReply();

            logger.debug("sending " + events.length + " entries to " + ibis);

            connection.out().writeInt(events.length);

            for (int i = 0; i < events.length; i++) {

                events[i].writeTo(connection.out());
            }

            connection.out().writeInt(-1);

            connection.getAndCheckReply();
            connection.close();

            logger.debug("connection to " + ibis + " closed");
            stats.add(Protocol.OPCODE_PUSH, System.currentTimeMillis() - start,
                    connection.read(), connection.written(), false);
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
        logger.debug("got a gossip request");

        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        String poolName = identifier.poolName();
        int peerTime = connection.in().readInt();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of "
                    + pool.getName());
            connection.closeWithError("wrong pool: " + poolName
                    + " instead of " + pool.getName());
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

    private void handlePush(Connection connection) throws IOException {
        logger.debug("got a push from the server");

        String poolName = connection.in().readUTF();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of "
                    + pool.getName());
            connection.closeWithError("wrong pool: " + poolName
                    + " instead of " + pool.getName());
        }

        connection.out().writeInt(pool.getTime());
        connection.out().flush();

        connection.getAndCheckReply();

        int events = connection.in().readInt();

        if (events < 0) {
            connection.closeWithError("negative event size");
            return;
        }

        Event[] newEvents = new Event[events];
        for (int i = 0; i < newEvents.length; i++) {
            newEvents[i] = new Event(connection.in());
        }

        int minEventTime = connection.in().readInt();

        connection.sendOKReply();

        connection.close();

        pool.newEventsReceived(newEvents);

        if (minEventTime != -1) {
            pool.purgeHistoryUpto(minEventTime);
        }
    }

    private void handlePing(Connection connection) throws IOException {
        logger.debug("got a ping request");
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
        logger.debug("got a state request");

        IbisIdentifier identifier = new IbisIdentifier(connection.in());
        int joinTime = connection.in().readInt();

        String poolName = identifier.poolName();

        if (!poolName.equals(pool.getName())) {
            logger.error("wrong pool: " + poolName + " instead of "
                    + pool.getName());
            connection.closeWithError("wrong pool: " + poolName
                    + " instead of " + pool.getName());
        }

        if (!pool.isInitialized()) {
            connection.closeWithError("state not available");
            return;
        }

        int time = pool.getTime();
        if (time < joinTime) {
            connection.closeWithError("minimum time requirement not met: "
                    + joinTime + " vs " + time);
            return;
        }

        connection.sendOKReply();

        pool.writeState(connection.out(), joinTime);

        connection.out().flush();
        connection.close();
    }

    public void run() {
        Connection connection = null;
        try {
            logger.debug("accepting connection");
            connection = new Connection(serverSocket);
            logger.debug("connection accepted");
        } catch (IOException e) {
            if (pool.isStopped()) {
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
        ThreadPool.createNew(this, "client connection handler");

        if (connection == null) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            byte magic = connection.in().readByte();

            if (magic != Protocol.CLIENT_MAGIC_BYTE) {
                throw new IOException(
                        "Invalid header byte in accepting connection");
            }

            byte opcode = connection.in().readByte();

            logger.debug("received request: " + Protocol.opcodeString(opcode));

            switch (opcode) {
            case Protocol.OPCODE_GOSSIP:
                handleGossip(connection);
                break;
            case Protocol.OPCODE_PUSH:
                handlePush(connection);
                break;
            case Protocol.OPCODE_PING:
                handlePing(connection);
                break;
            case Protocol.OPCODE_GET_STATE:
                handleGetState(connection);
                break;
            default:
                logger.error("unknown opcode in request: " + opcode + "("
                        + Protocol.opcodeString(opcode) + ")");
            }
            logger.debug("done handling request");
            long end = System.currentTimeMillis();
            stats.add(opcode, end - start, connection.read(), connection
                    .written(), true);
        } catch (IOException e) {
            logger.error("error on handling request", e);
        } finally {
            connection.close();
        }
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

}
