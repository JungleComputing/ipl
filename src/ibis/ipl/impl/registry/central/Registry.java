package ibis.ipl.impl.registry.central;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.RegistryProperties;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * Central registry.
 */
public final class Registry extends ibis.ipl.impl.Registry implements Runnable {

    static final boolean DEFAULT_SMARTSOCKETS = true;

    static final int MAX_GOSSIP_INTERVAL = 20 * 1000;

    static final int GOSSIP_ATTEMPTS = 20;

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

    private final ArrayList<ibis.ipl.IbisIdentifier> joinedIbises;

    private final ArrayList<ibis.ipl.IbisIdentifier> leftIbises;
    
    private final ArrayList<ibis.ipl.IbisIdentifier> diedIbises;

    private final ArrayList<String> signals;
    
    // A user-supplied registry handler, with join/leave upcalls.
    private final RegistryEventHandler registryHandler;
    
    // A thread that forwards the events to the user event handler 
    private final Upcaller upcaller;
    
    private final IbisIdentifier identifier;

    private final String poolName;
    
    private final boolean closedWorld;

    private final int numInstances;
    
    private final boolean keepClientState;

    private boolean stopped = false;

    private final Server server;

    private final int serverConnectTimeout;
    
    private final IbisCapabilities capabilities;

    /**
     * Creates a Central Registry.
     * 
     * @param handler
     *            registry handler to pass events to.
     * @param props
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public Registry(IbisCapabilities caps, RegistryEventHandler handler, 
            Properties props, byte[] data)
            throws IOException, IbisConfigurationException {

        events = new ArrayList<Event>();
        elections = new HashMap<String, IbisIdentifier>();
        currentIbisses = new ArrayList<IbisIdentifier>();
        capabilities = caps;

        closedWorld = caps.hasCapability(IbisCapabilities.CLOSEDWORLD);
        
        if (closedWorld) {
            try {
                TypedProperties tmp = new TypedProperties(props);
                numInstances = tmp.getIntProperty("ibis.pool.total_hosts");
            } catch(NumberFormatException e) {
                throw new IbisConfigurationException("Could not get number of "
                        + "instances", e);
            }
        } else {
            numInstances = -1;
        }
        
        if (caps.hasCapability(IbisCapabilities.MEMBERSHIP)) {
            joinedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            leftIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            diedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            signals = new ArrayList<String>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
            signals = null;
        }
        
        random = new Random();

        TypedProperties properties = new TypedProperties(props);
        String serverString = properties.getProperty(
                RegistryProperties.SERVER_ADDRESS);

        serverConnectTimeout = properties.getIntProperty(
                RegistryProperties.CENTRAL_SERVER_CONNECT_TIMEOUT) * 1000;

        boolean smart = properties.booleanProperty(
                RegistryProperties.CENTRAL_SMARTSOCKETS, DEFAULT_SMARTSOCKETS);

        int defaultServerPort = properties.getIntProperty(
                RegistryProperties.SERVER_PORT);

        connectionFactory = new ConnectionFactory(smart, serverString,
                defaultServerPort);

        Server server = null;
        
        if (connectionFactory.serverIsLocalHost()) {
            try {
                properties.setProperty(
                        RegistryProperties.SERVER_PORT, 
                        Integer.toString(connectionFactory.getServerPort()));
                
                properties.setProperty(
                        RegistryProperties.SERVER_SINGLE, "true");
                
                server = new Server(properties);
                server.setDaemon(true);
                server.start();
                logger.warn("Automagically created " + server.toString());
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
        long pingInterval = properties.getLongProperty(RegistryProperties.CENTRAL_PING_INTERVAL);

        Location location = Location.defaultLocation(props);

        // join at server, also sets identifier and adds a number of ibisses
        // to the "current" ibis list
        identifier = join(connectionFactory.getLocalAddress(), location, data,
                gossip, keepClientState, pingInterval);

        if (gossip) {
            // start gossiping
            ThreadPool.createNew(this, "Registry Gossiper");
        }

        registryHandler = handler;
        
        // start sending events to the ibis instance we belong to
        if (registryHandler != null) {
            upcaller = new Upcaller(registryHandler, identifier, this);
        } else { 
            upcaller = null;
        }

        // start handling incoming connections
        new PeerConnectionHandler(connectionFactory, this);

        logger.debug("registry for " + identifier + " initiated");
    }

    String getPoolName() {
        return poolName;
    }

    boolean statefulServer() {
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
            byte[] implementationData, boolean gossip, boolean stateFullServer, long pingInterval)
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
            connection.out().writeLong(pingInterval);

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

            // print a warning about waiting for the server after a few seconds
            if (server != null) {
                logger.debug("Waiting for central server to finish");
                try {
                    server.join(5000);
                } catch (InterruptedException e) {
                    // IGNORE
                }
                if (server.isAlive()) {
                    logger.warn("Waiting for central server to finish");
                    try {
                        server.join();
                    } catch (InterruptedException e) {
                        // IGNORE
                    }
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
        
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
            throw new IbisConfigurationException("No election support requested");
        }

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

        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
            throw new IbisConfigurationException("No election support requested");
        }
        
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

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
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

    public void signal(String signal, ibis.ipl.IbisIdentifier... ibisses)
            throws IOException {
        logger.debug("telling " + ibisses.length + " ibisses a signal: "
                + signal);
        
        if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            throw new IbisConfigurationException("No signal support requested");
        }
        
        Connection connection = connectionFactory.connectToServer(
                Protocol.OPCODE_SIGNAL, serverConnectTimeout);

        try {
            connection.out().writeUTF(getPoolName());
            connection.out().writeUTF(signal);
            connection.out().writeInt(ibisses.length);
            for (int i = 0; i < ibisses.length; i++) {
                ((IbisIdentifier) ibisses[i]).writeTo(connection.out());
            }

            connection.getAndCheckReply();
            connection.close();

            logger.debug("done telling " + ibisses.length
                    + " ibisses a signal: " + signal);
        } catch (IOException e) {
            connection.close();
            throw e;
        }
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = joinedIbises.toArray(
                new ibis.ipl.IbisIdentifier[joinedIbises.size()]);
        joinedIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = leftIbises.toArray(
                new ibis.ipl.IbisIdentifier[leftIbises.size()]);
        leftIbises.clear();
        return retval;
    }
    
    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = diedIbises.toArray(
                new ibis.ipl.IbisIdentifier[diedIbises.size()]);
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
    
    public int getPoolSize() {
        if (! closedWorld) {
            throw new IbisConfigurationException(
                "totalNrOfIbisesInPool called but open world run");
        }
        return numInstances;
    }
    
    public synchronized void waitForAll() {
        
        if (! closedWorld) {
            throw new IbisConfigurationException("waitForAll() called but not "
                    + "closed world");
        }
        
        /*
        if (registryHandler != null && ! registryUpcallerEnabled) {
            throw new IbisConfigurationException("waitForAll() called but "
                    + "registry events not enabled yet");
        }
        */
        
        while (currentIbisses.size() < numInstances) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
        }
    }
    
    public void enableEvents() {
        if (upcaller == null) { 
            throw new IbisConfigurationException("Registry not configured to " +
                    "produce events");
        }
    
        upcaller.enableEvents();
    }
    
    public void disableEvents() {
        if (upcaller == null) { 
            throw new IbisConfigurationException("Registry not configured to " +
                    "produce events");
        }
    
        upcaller.disableEvents();    
    }
    
    private synchronized void addIbis(IbisIdentifier newIbis) {
        if (joinedIbises != null) { 
            joinedIbises.add(newIbis);
        }
        
        for (IbisIdentifier ibis : currentIbisses) {
            if (ibis.equals(newIbis)) {
                return;
            }
        }
        currentIbisses.add(newIbis);
    }

    private synchronized void ibisLeft(IbisIdentifier ibis) {
        if (leftIbises != null) { 
            leftIbises.add(ibis);
        }
        
        for (int i = 0; i < currentIbisses.size(); i++) {
            if (currentIbisses.get(i).equals(ibis)) {
                currentIbisses.remove(i);       
                return;
            }
        }
    }

    private synchronized void ibisDied(IbisIdentifier ibis) {
        if (diedIbises != null) { 
            diedIbises.add(ibis);
        }

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
                    ibisLeft(ibis);
                }
                break;
            case Event.DIED:
                for (IbisIdentifier ibis : newEvents[i].getIbisses()) {
                    ibisDied(ibis);
                }
                break;
            case Event.SIGNAL:
                if (signals != null) { 
                    for (IbisIdentifier identifier : newEvents[i].getIbisses()) {
                        if (identifier.equals(identifier)) {
                            synchronized (this) {
                                signals.add(newEvents[i].getDescription());
                            }
                        }
                    }
                }
                break;
            case Event.ELECT:
                elections.put(newEvents[i].getDescription(), newEvents[i]
                        .getFirstIbis());
                break;
            case Event.UN_ELECT:
                elections.remove(newEvents[i].getDescription());
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
