package ibis.impl.registry.central;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.impl.registry.RegistryProperties;
import ibis.ipl.IbisConfigurationException;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

/**
 * Gossiping registry.
 */
public final class Registry extends ibis.impl.Registry implements Runnable {

    public static final boolean DEFAULT_SMARTSOCKETS = true;

    public static final int MAX_GOSSIP_INTERVAL = 20 * 1000;

    public static final int GOSSIP_ATTEMPTS = 20;

    private static final int PEER_CONNECT_TIMEOUT = 60 * 1000;

    private static final Logger logger = Logger.getLogger(Registry.class);

    private final ConnectionFactory connectionFactory;

    private final Random random;

    // list of all joins, leaves, elections, etc.
    private final ArrayList<Event> events;

    // cache of election results we've seen. Optimization over searching all the
    // events each time an election result is requested.
    // map<election name, winner>
    private final Map<String, IbisIdentifier> elections;

    // Ibisses currently assumed to be alive
    private final ArrayList<IbisIdentifier> currentIbisses;

    private final IbisIdentifier identifier;

    private final String poolName;

    private final boolean keepClientState;

    private boolean stopped = false;

    private final Server server;

    private final int serverConnectTimeout;

    public Registry(ibis.impl.Ibis ibis, boolean needsUpcalls, byte[] data)
            throws IOException, IbisConfigurationException {

        events = new ArrayList<Event>();
        elections = new HashMap<String, IbisIdentifier>();
        currentIbisses = new ArrayList<IbisIdentifier>();

        random = new Random();

        TypedProperties properties = new TypedProperties(ibis.properties());
        String serverString = properties
                .getProperty(RegistryProperties.SERVER_ADDRESS);

        serverConnectTimeout = properties
                .getIntProperty(RegistryProperties.CENTRAL_SERVER_CONNECT_TIMEOUT) * 1000;

        boolean smart = properties.booleanProperty(
                RegistryProperties.CENTRAL_SMARTSOCKETS, DEFAULT_SMARTSOCKETS);

        int defaultServerPort = properties
                .getIntProperty(RegistryProperties.SERVER_PORT);

        connectionFactory = new ConnectionFactory(0, smart, serverString,
                defaultServerPort);

        Server server = null;
        if (connectionFactory.serverIsLocalHost()) {
            try {
                properties.setProperty(RegistryProperties.SERVER_PORT, Integer
                        .toString(connectionFactory.getServerPort()));
                properties
                        .setProperty(RegistryProperties.SERVER_SINGLE, "true");
                server = new Server(properties);
                server.setDaemon(true);
                server.start();
                logger.warn("Automagically created server on "
                        + server.getLocalAddress());
            } catch (Throwable t) {
                logger.debug("Could not create registry server", t);
            }
        }
        this.server = server;

        // Next, get the nameserver pool ....
        poolName = properties.getProperty(RegistryProperties.POOL);
        if (poolName == null) {
            throw new IbisConfigurationException("property "
                    + RegistryProperties.POOL + " is not specified");
        }

        boolean gossip = properties
                .booleanProperty(RegistryProperties.CENTRAL_GOSSIP);
        keepClientState = properties
                .booleanProperty(RegistryProperties.CENTRAL_KEEP_NODE_STATE);

        Location location = Location.defaultLocation();

        // join at server, also sets identifier and adds a number of ibisses
        // to the "current" ibis list
        identifier = join(connectionFactory.getLocalAddress(), location, data,
                gossip, keepClientState);

        if (gossip) {
            // start gossiping
            ThreadPool.createNew(this, "Registry Gossiper");
        }

        // start sending events to the ibis instance we belong to
        if (needsUpcalls) {
            new Upcaller(ibis, this);
        }

        // start handling incoming connections
        new PeerConnectionHandler(connectionFactory, this);

        logger.debug("registry for " + identifier + " initiated");
    }

    String getPoolName() {
        return poolName;
    }

    public boolean statefulServer() {
        return keepClientState;
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    synchronized int currentEventTime() {
        return events.size();
    }

    synchronized Event waitForEvent(int eventNr) {
        while (true) {
            if (stopped) {
                return null;
            }

            if (events.size() > eventNr) {
                return events.get(eventNr);
            }

            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    synchronized Event getEvent(int eventNr) throws IOException {
        if (eventNr >= events.size()) {
            throw new IOException("unknown event");
        }
        return events.get(eventNr);
    }

    synchronized boolean isStopped() {
        return stopped;
    }

    /**
     * connects to the nameserver, joins, and gets back a bootstrap list with
     * some peers
     * 
     * @throws IOException
     *             in case of trouble
     */
    private IbisIdentifier join(byte[] myAddress, Location location,
            byte[] implementationData, boolean gossip, boolean stateFullServer)
            throws IOException {
        logger.debug("joining to " + getPoolName());
        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_JOIN, serverConnectTimeout);

        try {
            connection.out().writeInt(myAddress.length);
            connection.out().write(myAddress);

            connection.out().writeUTF(getPoolName());
            connection.out().writeInt(implementationData.length);
            connection.out().write(implementationData);
            location.writeTo(connection.out());
            connection.out().writeBoolean(gossip);
            connection.out().writeBoolean(stateFullServer);

            connection.getAndCheckReply();

            IbisIdentifier result = new IbisIdentifier(connection.in());
            int listLength = connection.in().readInt();
            for (int i = 0; i < listLength; i++) {
                currentIbisses.add(new IbisIdentifier(connection.in()));
            }

            connection.close();

            logger.debug("join done");

            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    @Override
    public void leave() throws IOException {
        logger.debug("leaving pool");

        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_LEAVE, serverConnectTimeout);

        try {
            getIbisIdentifier().writeTo(connection.out());

            connection.getAndCheckReply();

            connection.close();

            synchronized (this) {
                stopped = true;
                notifyAll();
            }
            connectionFactory.end();
            logger.debug("left");
            if (server != null) {
                logger.info("waiting for server to stop");
                try {
                    server.join();
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        } catch (IOException e) {
            connection.close();
            synchronized (this) {
                stopped = true;
                notifyAll();
            }
            throw e;
        }
    }

    private void gossip(IbisIdentifier ibis) throws IOException {
        if (ibis.equals(getIbisIdentifier())) {
            logger.debug("not gossiping with self");
            return;
        }

        logger.debug("gossiping with " + ibis);

        Connection connection = connectionFactory.connect(ibis,
                Protocol.OPCODE_GOSSIP, PEER_CONNECT_TIMEOUT);

        try {
            connection.out().writeUTF(getPoolName());
            int localTime = currentEventTime();
            connection.out().writeInt(currentEventTime());

            connection.getAndCheckReply();

            int peerTime = connection.in().readInt();

            Event[] newEvents;
            if (peerTime > localTime) {
                newEvents = new Event[connection.in().readInt()];
                for (int i = 0; i < newEvents.length; i++) {
                    newEvents[i] = new Event(connection.in());
                }

                connection.close();

                handleNewEvents(newEvents);
            } else if (peerTime < localTime) {
                newEvents = new Event[0];

                int sendEntries = localTime - peerTime;

                connection.out().writeInt(sendEntries);
                for (int i = 0; i < sendEntries; i++) {
                    Event event = getEvent(peerTime + i);

                    event.writeTo(connection.out());
                }

            } else {
                // nothing to send either way
            }
            logger.debug("gossiping with " + ibis + " done, time now: "
                    + currentEventTime());
        } catch (IOException e) {
            connection.close();
            throw e;
        }
        connection.close();
    }

    public IbisIdentifier elect(String election) throws IOException {
        logger.debug("running election: \"" + election + "\"");

        synchronized (this) {
            if (elections.containsKey(election)) {
                IbisIdentifier winner = elections.get(election);
                logger.debug("election: \"" + election + "\" result = "
                        + winner);

                return winner;
            }
        }

        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_ELECT, serverConnectTimeout);

        try {

            getIbisIdentifier().writeTo(connection.out());
            connection.out().writeUTF(election);

            connection.getAndCheckReply();

            IbisIdentifier winner = new IbisIdentifier(connection.in());

            connection.close();

            // put result in our table too
            synchronized (this) {
                if (!elections.containsKey(election)) {
                    elections.put(election, winner);
                }
            }

            logger.debug("election: \"" + election + "\" result = " + winner);
            return winner;

        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public synchronized IbisIdentifier getElectionResult(String election)
            throws IOException {
        logger.debug("getting election result for: \"" + election + "\"");

        IbisIdentifier winner = elections.get(election);

        while (winner == null) {
            logger.debug("waiting for election: " + election);

            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
            winner = elections.get(election);
        }
        logger.debug("election: " + election + "result = " + winner);
        return winner;
    }

    @Override
    public long getSeqno(String name) throws IOException {
        logger.debug("getting sequence number");
        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_SEQUENCE_NR, serverConnectTimeout);

        try {
            connection.out().writeUTF(getPoolName());
            connection.out().flush();

            connection.getAndCheckReply();

            long result = connection.in().readLong();

            connection.close();

            logger.debug("sequence number = " + result);
            return result;
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void dead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        logger.debug("declaring " + ibis + " to be dead");

        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_DEAD, serverConnectTimeout);

        try {
            ((IbisIdentifier) ibis).writeTo(connection.out());

            connection.getAndCheckReply();

            connection.close();

            logger.debug("done declaring " + ibis + " dead ");
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        logger.debug("reporting " + ibis + " to possibly be dead");

        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_MAYBE_DEAD, serverConnectTimeout);

        try {

            ((IbisIdentifier) ibis).writeTo(connection.out());

            connection.getAndCheckReply();
            connection.close();

            logger.debug("done reporting " + ibis + " to possibly be dead");
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public void mustLeave(ibis.ipl.IbisIdentifier[] ibisses) throws IOException {
        logger.debug("telling " + ibisses.length + " ibisses to leave");
        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_MUST_LEAVE, serverConnectTimeout);

        try {
            connection.out().writeUTF(getPoolName());
            connection.out().writeInt(ibisses.length);
            for (int i = 0; i < ibisses.length; i++) {
                ((IbisIdentifier) ibisses[i]).writeTo(connection.out());
            }

            connection.getAndCheckReply();
            connection.close();

            logger
                    .debug("done telling " + ibisses.length
                            + " ibisses to leave");
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    private synchronized void addIbis(IbisIdentifier newIbis) {
        for (IbisIdentifier ibis : currentIbisses) {
            if (ibis.equals(newIbis)) {
                return;
            }
        }
        currentIbisses.add(newIbis);
    }

    private synchronized void removeIbis(IbisIdentifier ibis) {
        for (int i = 0; i < currentIbisses.size(); i++) {
            if (currentIbisses.get(i).equals(ibis)) {
                currentIbisses.remove(i);
                return;
            }
        }
    }

    synchronized void handleNewEvents(Event[] newEvents) {
        logger.debug(" handling" + newEvents.length + " new events");

        if (newEvents.length == 0) {
            return;
        }

        for (int i = 0; i < newEvents.length; i++) {

            if (newEvents[i].getTime() != events.size()) {
                logger.error("we need event " + events.size()
                        + " but we received " + newEvents[i].getTime());
                continue;
            }

            // may overwrite events, but the are immutable anyway
            events.add(newEvents[i].getTime(), newEvents[i]);

            switch (newEvents[i].getType()) {
            case Event.JOIN:
                for (IbisIdentifier ibis : newEvents[i].getIbisses()) {
                    addIbis(ibis);
                }
                break;
            case Event.LEAVE:
                for (IbisIdentifier ibis : newEvents[i].getIbisses()) {
                    removeIbis(ibis);
                }
                break;
            case Event.DIED:
                for (IbisIdentifier ibis : newEvents[i].getIbisses()) {
                    removeIbis(ibis);
                }
                break;
            case Event.MUST_LEAVE:
                // Only handled in upcaller
                break;
            case Event.ELECT:
                elections.put(newEvents[i].getElectionName(), newEvents[i]
                        .getFirstIbis());
                break;
            case Event.UN_ELECT:
                elections.remove(newEvents[i].getElectionName());
                break;
            default:
                logger.error("unknown event type: " + newEvents[i].getType());
            }

        }
        // notify threads waiting for elections or events
        notifyAll();
    }

    private synchronized IbisIdentifier getVictim() throws IOException {
        int rank = random.nextInt(currentIbisses.size());

        return currentIbisses.get(rank);
    }

    public void run() {
        while (!isStopped()) {
            boolean success = false;
            int triesLeft = GOSSIP_ATTEMPTS;

            while (!success && triesLeft > 0) {
                try {
                    IbisIdentifier ibis = getVictim();

                    gossip(ibis);
                    success = true;
                } catch (IOException e) {
                    triesLeft--;
                    if (triesLeft == 0) {
                        logger.error("could not gossip with " + GOSSIP_ATTEMPTS
                                + " peers, giving up for now", e);

                    }
                }
            }

            logger.debug("Event time at " + identifier.myId + " now "
                    + currentEventTime());
            synchronized (this) {
                try {
                    wait((int) (Math.random() * MAX_GOSSIP_INTERVAL));
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }

    }

}
