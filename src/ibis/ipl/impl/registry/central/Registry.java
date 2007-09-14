package ibis.ipl.impl.registry.central;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.central.server.Server;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * Central registry.
 */
public final class Registry extends ibis.ipl.impl.Registry implements Runnable {

    private static final Logger logger = Logger.getLogger(Registry.class);

    private final ConnectionFactory connectionFactory;

    // A user-supplied registry handler, with join/leave upcalls.
    private final RegistryEventHandler registryHandler;

    // A thread that forwards the events to the user event handler
    private final Upcaller upcaller;

    private final IbisIdentifier identifier;

    private final String poolName;

    private final boolean closedWorld;

    private final boolean gossip;
    
    private final boolean peerBootstrap;

    private final long gossipInterval;

    private final long heartbeatInterval;

    private final int poolSize;

    private boolean stopped = false;

    private final Server server;

    private final IbisCapabilities capabilities;

    private final ArrayList<IbisIdentifier> ibises;

    private final Map<String, IbisIdentifier> elections;

    private final SortedSet<Event> pendingEvents;

    private final ArrayList<Event> eventHistory;

    private final Random random;
    
    // data structures that the user can poll

    private final ArrayList<ibis.ipl.IbisIdentifier> joinedIbises;

    private final ArrayList<ibis.ipl.IbisIdentifier> leftIbises;

    private final ArrayList<ibis.ipl.IbisIdentifier> diedIbises;

    private final ArrayList<String> signals;

    private boolean initialized;

    private int time;

    private int nrOfIbissesJoined;

    private long heartbeatDeadline;

    /**
     * Creates a Central Registry.
     * 
     * @param handler
     *                registry handler to pass events to.
     * @param userProperties
     *                properties of this registry.
     * @param data
     *                Ibis implementation data to attach to the IbisIdentifier.
     * @throws IOException
     *                 in case of trouble.
     * @throws IbisConfigurationException
     *                 In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities,
            RegistryEventHandler handler, Properties userProperties, byte[] data)
            throws IbisConfigurationException, IOException,
            IbisConfigurationException {
        logger.debug("creating central registry");

        this.capabilities = capabilities;

        TypedProperties properties = RegistryProperties
                .getHardcodedProperties();
        properties.addProperties(userProperties);

        // get the pool ....
        poolName = properties.getProperty(IbisProperties.POOL_NAME);
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.POOL_NAME + " is not specified");
        }

        ibises = new ArrayList<IbisIdentifier>();
        elections = new HashMap<String, IbisIdentifier>();
        pendingEvents = new TreeSet<Event>();
        eventHistory = new ArrayList<Event>();

        random = new Random();

        time = -1;
        nrOfIbissesJoined = 0;
        initialized = false;

        if (capabilities.hasCapability(IbisCapabilities.MEMBERSHIP)) {
            joinedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            leftIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            diedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
        }

        if (capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            signals = new ArrayList<String>();
        } else {
            signals = null;
        }

        closedWorld = capabilities.hasCapability(IbisCapabilities.CLOSEDWORLD);

        if (closedWorld) {
            try {
                poolSize = properties
                        .getIntProperty(IbisProperties.POOL_SIZE);
            } catch (NumberFormatException e) {
                throw new IbisConfigurationException(
                        "could not start registry for a closed world ibis, "
                                + "required property: "
                                + IbisProperties.POOL_SIZE + " undefined", e);
            }
        } else {
            poolSize = -1;
        }

        connectionFactory = new ConnectionFactory(properties);

        Server server = null;

        if (properties.getBooleanProperty(RegistryProperties.SERVER_STANDALONE)
                && connectionFactory.serverIsLocalHost()) {
            logger.debug("automagically creating server");
            try {
                properties.setProperty(RegistryProperties.SERVER_PORT, Integer
                        .toString(connectionFactory.getServerPort()));

                server = new Server(properties);
                logger.warn("Automagically created " + server.toString());
            } catch (Throwable t) {
                logger.debug("Could not create registry server", t);
            }
        }

        this.server = server;

        heartbeatInterval = properties
                .getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;
        long eventPushInterval = properties
                .getIntProperty(RegistryProperties.EVENT_PUSH_INTERVAL) * 1000;
        gossip = properties.getBooleanProperty(RegistryProperties.GOSSIP);
        peerBootstrap = properties.getBooleanProperty(RegistryProperties.PEER_BOOTSTRAP);
        gossipInterval = properties
                .getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;
        boolean adaptGossipInterval = properties
                .getBooleanProperty(RegistryProperties.ADAPT_GOSSIP_INTERVAL);
        boolean tree = properties.getBooleanProperty(RegistryProperties.TREE);

        Location location = Location.defaultLocation(userProperties);

        ArrayList<IbisIdentifier> bootstrapList = new ArrayList<IbisIdentifier>();

        // join at server
        identifier = join(connectionFactory.getLocalAddress(), location, data,
                heartbeatInterval, eventPushInterval, gossip, gossipInterval,
                adaptGossipInterval, tree, closedWorld, poolSize, bootstrapList);

        registryHandler = handler;

        if (registryHandler != null) {
            upcaller = new Upcaller(registryHandler, identifier);
        } else {
            upcaller = null;
        }

        // start handling incoming connections
        new ClientConnectionHandler(connectionFactory, this);

        bootstrap(bootstrapList);

        if (gossip) {
            new Gossiper(this, gossipInterval);
        }

        ThreadPool.createNew(this, "heartbeat thread");

        logger.debug("registry for " + identifier + " initiated");
    }

    synchronized void updateHeartbeatDeadline() {
        heartbeatDeadline = System.currentTimeMillis()
                + (long) (heartbeatInterval * 0.9 * Math.random());

        logger.debug("heartbeat deadline updated");

        // no need to wake up heartbeat thread, deadline will only be later
    }

    synchronized void waitForHeartbeatDeadline() {
        while (true) {
            int timeout = (int) (heartbeatDeadline - System.currentTimeMillis());

            if (timeout <= 0) {
                return;
            }

            try {
                logger.debug("waiting " + timeout + " for heartbeat");
                wait(timeout);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    synchronized int getTime() {
        return time;
    }

    synchronized boolean isInitialized() {
        return initialized;
    }

    synchronized boolean isStopped() {
        return stopped;
    }

    private synchronized void stop() {
        stopped = true;
        if (upcaller != null) {
            upcaller.stop();
        }
        if (connectionFactory != null) {
            connectionFactory.end();
        }
        notifyAll();
    }

    synchronized void writeState(DataOutput out) throws IOException {
        if (!initialized) {
            throw new IOException("state not initialized yet");
        }

        out.writeInt(ibises.size());
        for (IbisIdentifier ibis : ibises) {
            ibis.writeTo(out);
        }

        out.writeInt(elections.size());
        for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
            out.writeUTF(entry.getKey());
        }
        for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
            entry.getValue().writeTo(out);
        }
        
        out.writeInt(time);
    }

    private synchronized void readState(DataInput in) throws IOException {
        if (initialized) {
            logger.error("Tried to initialize registry state twice");
            return;
        }

        logger.debug("reading bootstrap state");

        int nrOfIbises = in.readInt();
        SortedSet<IbisIdentifier> sortedIbises = new TreeSet<IbisIdentifier>(
                new IbisComparator());

        for (int i = 0; i < nrOfIbises; i++) {
            sortedIbises.add(new IbisIdentifier(in));
        }

        int nrOfElections = in.readInt();
        String[] electionNames = new String[nrOfElections];
        for (int i = 0; i < electionNames.length; i++) {
            electionNames[i] = in.readUTF();
        }
        IbisIdentifier[] electionResults = new IbisIdentifier[nrOfElections];
        for (int i = 0; i < electionResults.length; i++) {
            electionResults[i] = new IbisIdentifier(in);
        }
        

        Map<String, IbisIdentifier> elections = new HashMap<String, IbisIdentifier>();
        for (int i = 0; i < nrOfElections; i++) {
            elections.put(electionNames[i], electionResults[i]);
        }

        time = in.readInt();

        logger.debug("read bootstrap state of time " + time);

        logger.debug("generating events for already joined Ibises (in order)");
        for (IbisIdentifier ibis : sortedIbises) {
            handleEvent(new Event(-1, Event.JOIN, null, ibis));
        }

        logger.debug("generating events for elections");
        for (Map.Entry<String, IbisIdentifier> election : elections.entrySet()) {
            handleEvent(new Event(-1, Event.ELECT, election.getKey(), election
                    .getValue()));
        }

        initialized = true;

        logger.debug("bootstrap complete");

        handlePendingEvents();
    }

    private void bootstrap(ArrayList<IbisIdentifier> bootstrapList)
            throws IOException {
        if (peerBootstrap) {
            for (IbisIdentifier ibis : bootstrapList) {
                if (!ibis.equals(identifier)) {
                    logger.debug("trying to bootstrap with data from " + ibis);
                    Connection connection = null;
                    try {
                        connection = connectionFactory.connect(ibis, false);

                        connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
                        connection.out().writeByte(Protocol.OPCODE_GET_STATE);

                        getIbisIdentifier().writeTo(connection.out());
                        connection.out().writeInt(getTime());
                        connection.out().flush();

                        connection.getAndCheckReply();

                        readState(connection.in());
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
        logger.debug("bootstrapping wit server");
        Connection connection = connectionFactory.connectToServer(true);
        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GET_STATE);
            getIbisIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            readState(connection.in());
            connection.close();

            updateHeartbeatDeadline();
        } catch (IOException e) {
            connection.close();
            stop();
            throw e;
        }
    }

    String getPoolName() {
        return poolName;
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    /**
     * connects to the nameserver, joins, and gets back a bootstrap list with
     * some peers
     * 
     * @throws IOException
     *                 in case of trouble
     */
    private IbisIdentifier join(byte[] myAddress, Location location,
            byte[] implementationData, long heartbeatInterval,
            long eventPushInterval, boolean gossip, long gossipInterval,
            boolean adaptGossipInterval, boolean tree, boolean closedWorld, int poolSize,
            ArrayList<IbisIdentifier> bootstrapList) throws IOException {

        logger.debug("joining to " + getPoolName() + ", connecting to server");
        Connection connection = connectionFactory.connectToServer(true);

        logger.debug("sending join info to server");

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_JOIN);
            connection.out().writeInt(myAddress.length);
            connection.out().write(myAddress);

            connection.out().writeUTF(getPoolName());
            connection.out().writeInt(implementationData.length);
            connection.out().write(implementationData);
            location.writeTo(connection.out());
            connection.out().writeLong(heartbeatInterval);
            connection.out().writeLong(eventPushInterval);
            connection.out().writeBoolean(gossip);
            connection.out().writeLong(gossipInterval);
            connection.out().writeBoolean(adaptGossipInterval);
            connection.out().writeBoolean(tree);
            connection.out().writeBoolean(closedWorld);
            connection.out().writeInt(poolSize);
            connection.out().flush();

            logger.debug("reading join result info from server");

            connection.getAndCheckReply();

            IbisIdentifier result = new IbisIdentifier(connection.in());

            // mimimum event time we need as a bootstrap
            int time = connection.in().readInt();

            int listLength = connection.in().readInt();
            for (int i = 0; i < listLength; i++) {
                bootstrapList.add(new IbisIdentifier(connection.in()));
            }

            synchronized (this) {
                this.time = time;
            }

            logger.debug("join done");

            connection.close();

            updateHeartbeatDeadline();

            return result;
        } catch (IOException e) {
            // join failed
            connection.close();
            stop();
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
    private void sendHeartBeat() {
        logger.debug("sending heartbeat to server");

        if (isStopped()) {
            return;
        }

        Connection connection = null;
        try {
            connection = connectionFactory.connectToServer(true);

            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_HEARTBEAT);
            getIbisIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            logger.debug("send heartbeat");

            updateHeartbeatDeadline();

        } catch (Exception e) {
            if (connection != null) {
                connection.close();
            }
            logger.error("could not send heartbeat", e);
        }
    }

    @Override
    public void leave() throws IOException {
        logger.debug("leaving pool");

        if (isStopped()) {
            throw new IOException("cannot leave, registry already stopped");
        }

        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_LEAVE);
            getIbisIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            logger.debug("left");

            updateHeartbeatDeadline();

            if (server != null) {
                logger
                        .info("Central Registry: Waiting for central server to finish");
                server.end(true);
            }
        } finally {
            connection.close();
            stop();
        }
    }

    void gossip(IbisIdentifier ibis) throws IOException {
        if (ibis.equals(getIbisIdentifier())) {
            logger.debug("not gossiping with self");
            return;
        }

        logger.debug("gossiping with " + ibis);

        Connection connection = connectionFactory.connect(ibis, false);

        try {
            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_GOSSIP);
            getIbisIdentifier().writeTo(connection.out());
            int localTime = getTime();
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
                    newEventsReceived(newEvents);
                }
                connection.close();
            } else if (peerTime < localTime) {
                logger.debug("localtime = " + localTime + ", peerTime = "
                        + peerTime + ", pushing events");

                Event[] sendEvents = getEventsFrom(peerTime);

                connection.out().writeInt(sendEvents.length);
                for (Event event : sendEvents) {
                    event.writeTo(connection.out());
                }

            } else {
                // nothing to send either way
            }
            logger.debug("gossiping with " + ibis + " done, time now: "
                    + getTime());
        } catch (IOException e) {
            connection.close();
            throw e;
        }
        connection.close();
    }

    public IbisIdentifier elect(String election) throws IOException {
        if (isStopped()) {
            throw new IOException("cannot do elect, registry already stopped");
        }

        logger.debug("running election: \"" + election + "\"");

        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        IbisIdentifier winner = getElectionResult(election, -1);
        if (winner != null) {
            logger.debug("election: \"" + election + "\" result = " + winner);

            return winner;
        }

        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_ELECT);
            getIbisIdentifier().writeTo(connection.out());
            connection.out().writeUTF(election);
            connection.out().flush();

            connection.getAndCheckReply();

            winner = new IbisIdentifier(connection.in());

            connection.close();

            logger.debug("election : \"" + election + "\" done, result = "
                    + winner);

            updateHeartbeatDeadline();

            return winner;

        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public IbisIdentifier getElectionResult(String election) throws IOException {
        return getElectionResult(election, 0);
    }

    public synchronized IbisIdentifier getElectionResult(String election,
            long timeout) throws IOException {
        if (isStopped()) {
            throw new IOException(
                    "cannot do getElectionResult, registry already stopped");
        }

        logger.debug("getting election result for: \"" + election + "\"");

        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        }

        IbisIdentifier result = elections.get(election);

        while (result == null) {
            long timeRemaining = deadline - System.currentTimeMillis();

            if (timeRemaining <= 0) {
                logger.debug("getElectionResullt deadline expired");
                return null;
            }

            try {
                logger.debug("waiting " + timeRemaining + " for election");
                wait(timeRemaining);
                logger.debug("DONE waiting " + timeRemaining + " for election");
            } catch (InterruptedException e) {
                // IGNORE
            }
            result = elections.get(election);
        }
        logger.debug("getElection result = " + result);
        return result;
    }

    @Override
    public long getSeqno(String name) throws IOException {
        if (isStopped()) {
            throw new IOException(
                    "cannot get sequence number, registry already stopped");
        }

        logger.debug("getting sequence number");
        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SEQUENCE_NR);
            getIbisIdentifier().writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            long result = connection.in().readLong();

            connection.close();

            logger.debug("sequence number = " + result);

            updateHeartbeatDeadline();

            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        if (isStopped()) {
            throw new IOException(
                    "cannot do assumeDead, registry already stopped");
        }

        logger.debug("declaring " + ibis + " to be dead");

        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_DEAD);
            getIbisIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();

            connection.close();

            logger.debug("done declaring " + ibis + " dead ");

            updateHeartbeatDeadline();
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        if (isStopped()) {
            throw new IOException(
                    "cannot do maybeDead, registry already stopped");
        }

        logger.debug("reporting " + ibis + " to possibly be dead");

        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_MAYBE_DEAD);
            getIbisIdentifier().writeTo(connection.out());
            ((IbisIdentifier) ibis).writeTo(connection.out());
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            logger.debug("done reporting " + ibis + " to possibly be dead");

            updateHeartbeatDeadline();
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void signal(String signal, ibis.ipl.IbisIdentifier... ibisses)
            throws IOException {
        if (isStopped()) {
            throw new IOException(
                    "cannot send signals, registry already stopped");
        }

        logger.debug("telling " + ibisses.length + " ibisses a signal: "
                + signal);

        if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            throw new IbisConfigurationException("No signal support requested");
        }

        Connection connection = connectionFactory.connectToServer(true);

        try {
            connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_SIGNAL);
            getIbisIdentifier().writeTo(connection.out());
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

            updateHeartbeatDeadline();
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public int getPoolSize() {
        if (!closedWorld) {
            throw new IbisConfigurationException(
                    "totalNrOfIbisesInPool called but open world run");
        }
        return poolSize;
    }

    public synchronized void waitForAll() {

        if (!closedWorld) {
            throw new IbisConfigurationException("waitForAll() called but not "
                    + "closed world");
        }

        while (nrOfIbissesJoined < poolSize) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    public void enableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.enableEvents();
    }

    public void disableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.disableEvents();
    }

    // new incoming events
    synchronized void newEventsReceived(Event[] events) {
        // add events to the list
        pendingEvents.addAll(Arrays.asList(events));

        handlePendingEvents();
    }

    private synchronized void handlePendingEvents() {
        if (!initialized) {
            return;
        }

        // remove head of list until we have gotten rid of all "old" events
        while (!pendingEvents.isEmpty()
                && pendingEvents.first().getTime() < time) {
            pendingEvents.remove(pendingEvents.first());
        }

        while (!pendingEvents.isEmpty()
                && pendingEvents.first().getTime() == time) {
            // remove from TO-DO list :)
            Event event = pendingEvents.first();
            pendingEvents.remove(event);

            logger.debug("handling event " + event);

            time++;

            // add to history
            eventHistory.add(event);

            handleEvent(event);
        }

        if (logger.isDebugEnabled()) {
            if (pendingEvents.isEmpty()) {
                logger.debug("time now: " + time + ", no pending events");
            } else {
                logger
                        .debug("time now: " + time
                                + ", first event not handled: "
                                + pendingEvents.first());
            }
        }

        // wake up any threads waiting for (results of) events
        notifyAll();

        // assert consistency of event history
        checkConsistency();
    }

    private synchronized void handleJoinEvent(Event event) {
        for (IbisIdentifier newIbis : event.getIbises()) {

            logger.debug(newIbis + " joined our pool");

            ibises.add(newIbis);
            
            nrOfIbissesJoined++;
            //wake up waitForAll() function...
            notifyAll();

            if (joinedIbises != null) {
                joinedIbises.add(newIbis);
            }
        }
    }

    private synchronized void handleLeaveEvent(Event event) {
        for (IbisIdentifier ibis : event.getIbises()) {

            logger.debug(ibis + " left our pool");

            for (int i = 0; i < ibises.size(); i++) {
                if (ibises.get(i).equals(ibis)) {
                    ibises.remove(i);
                    return;
                }
            }

            if (leftIbises != null) {
                leftIbises.add(ibis);
            }
        }

    }

    private synchronized void handleDiedEvent(Event event) {
        for (IbisIdentifier ibis : event.getIbises()) {
            if (ibis.equals(identifier)) {
                logger.error("we were declared dead!");

                stop();
            }

            logger.debug(ibis + " died");

            for (int i = 0; i < ibises.size(); i++) {
                if (ibises.get(i).equals(ibis)) {
                    ibises.remove(i);
                    return;
                }

            }

            if (diedIbises != null) {
                diedIbises.add(ibis);
            }

        }

    }

    private synchronized void handleSignalEvent(Event event) {
        for (IbisIdentifier destination : event.getIbises()) {
            if (destination.equals(identifier)) {
                logger.debug("received signal: \"" + event.getDescription()
                        + "\"");
                signals.add(event.getDescription());
            }
        }
    }

    private synchronized void handleElectionEvent(Event event) {
        String name = event.getDescription();
        IbisIdentifier ibis = event.getFirstIbis();

        logger.debug("received winner for election \"" + name + "\" : " + ibis);

        elections.put(name, ibis);
        // wake up any waiting threads
        notifyAll();
    }

    private synchronized void handleUnElectionEvent(Event event) {
        String name = event.getDescription();

        logger.debug("unelect for election \"" + name + "\"");

        elections.remove(name);
    }

    private synchronized void handleEvent(Event event) {

        switch (event.getType()) {
        case Event.JOIN:
            handleJoinEvent(event);
            break;
        case Event.LEAVE:
            handleLeaveEvent(event);
            break;
        case Event.DIED:
            handleDiedEvent(event);
            break;
        case Event.SIGNAL:
            handleSignalEvent(event);
            break;
        case Event.ELECT:
            handleElectionEvent(event);
            break;
        case Event.UN_ELECT:
            handleUnElectionEvent(event);
            break;
        default:
            logger.error("unknown event type: " + event.getType());
        }

        // also push event to user
        if (upcaller != null) {
            upcaller.newEvent(event);
        }
    }

    private synchronized void checkConsistency() {
        if (eventHistory.isEmpty()) {
            return;
        }

        int offset = eventHistory.get(0).getTime();
        for (int i = 0; i < eventHistory.size(); i++) {
            if (eventHistory.get(i).getTime() != (offset + i)) {
                logger
                        .error("EEP! Registry event history not consistent! Offset = "
                                + offset
                                + " element number "
                                + i
                                + " in list has event time "
                                + eventHistory.get(i).getTime()
                                + " should be "
                                + (offset + i));
            }
        }
    }

    synchronized Event[] getEventsFrom(int fromTime) {
        if (eventHistory.isEmpty()) {
            return new Event[0];
        }

        int eventHistoryOffset = eventHistory.get(0).getTime();

        int startIndex = fromTime - eventHistoryOffset;

        if (startIndex < 0) {
            return new Event[0];
        }

        // return the requested portion of the list
        return eventHistory.subList(startIndex, eventHistory.size()).toArray(
                new Event[0]);
    }

    synchronized IbisIdentifier getRandomMember() {
        if (ibises.isEmpty()) {
            return null;
        }

        return ibises.get(random.nextInt(ibises.size()));
    }

    synchronized void purgeHistoryUpto(int time) {
        logger.debug("purging history upto " + time);
        while (!eventHistory.isEmpty() && eventHistory.get(0).getTime() < time) {
            if (logger.isDebugEnabled()) {
                logger.debug("removing event: " + eventHistory.get(0));
            }
            eventHistory.remove(0);
        }
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = joinedIbises
                .toArray(new ibis.ipl.IbisIdentifier[joinedIbises.size()]);
        joinedIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = leftIbises
                .toArray(new ibis.ipl.IbisIdentifier[leftIbises.size()]);
        leftIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = diedIbises
                .toArray(new ibis.ipl.IbisIdentifier[diedIbises.size()]);
        diedIbises.clear();
        return retval;
    }

    public synchronized String[] receivedSignals() {
        if (signals == null) {
            throw new IbisConfigurationException(
                    "Registry downcalls not configured");
        }
        String[] retval = signals.toArray(new String[signals.size()]);
        signals.clear();
        return retval;
    }

    public void run() {
        while (!isStopped()) {
            waitForHeartbeatDeadline();

            sendHeartBeat();
        }
    }

}
