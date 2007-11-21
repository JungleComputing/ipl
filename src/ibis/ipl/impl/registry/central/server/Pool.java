package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.Connection;
import ibis.ipl.impl.registry.CommunicationStatistics;
import ibis.ipl.impl.registry.PoolStatistics;
import ibis.ipl.impl.registry.StatisticsWriter;
import ibis.ipl.impl.registry.central.Election;
import ibis.ipl.impl.registry.central.ElectionSet;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.EventList;
import ibis.ipl.impl.registry.central.ListMemberSet;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.MemberSet;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.ipl.impl.registry.central.TreeMemberSet;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;

final class Pool implements Runnable {

    // time until the pool data is removed after the pool has ended (2 minutes)
    private static final int STALE_TIMEOUT = 120000;

    // 10 seconds connect timeout
    private static final int CONNECT_TIMEOUT = 10000;

    private static final Logger logger = Logger.getLogger(Pool.class);

    private final VirtualSocketFactory socketFactory;

    // list of all joins, leaves, elections, etc.
    private final EventList events;

    private final long heartbeatInterval;

    private int currentEventTime;

    private int minEventTime;

    private final ElectionSet elections;

    private final MemberSet members;

    private final OndemandEventPusher pusher;

    private final String name;

    private final String ibisImplementationIdentifier;

    private final boolean closedWorld;

    // value of this pool (if closed world)
    private final int fixedSize;

    private final boolean printEvents;

    private final boolean printErrors;

    // statistics which are only kept on the request of the user

    private final CommunicationStatistics commStatistics;

    private final PoolStatistics poolStatistics;

    private final StatisticsWriter statisticsWriter;
    
    // simple statistics which are always kept, so the server can print them
    // if so requested
    private final int[] eventStats;

    private final Map<String, Integer> sequencers;

    private int nextID;

    private boolean ended = false;

    private boolean closed = false;

    private long staleTime;

    Pool(String name, VirtualSocketFactory socketFactory,
            long heartbeatInterval, long eventPushInterval, boolean gossip,
            long gossipInterval, boolean adaptGossipInterval, boolean tree,
            boolean closedWorld, int poolSize, boolean keepStatistics, long statisticsInterval,
            String ibisImplementationIdentifier, boolean printEvents,
            boolean printErrors) {
        this.name = name;
        this.socketFactory = socketFactory;
        this.heartbeatInterval = heartbeatInterval;
        this.closedWorld = closedWorld;
        this.fixedSize = poolSize;
        this.ibisImplementationIdentifier = ibisImplementationIdentifier;
        this.printEvents = printEvents;
        this.printErrors = printErrors;

        if (keepStatistics) {
            commStatistics =
                new CommunicationStatistics(Protocol.NR_OF_OPCODES);
            poolStatistics = new PoolStatistics();
            statisticsWriter = new StatisticsWriter(name, statisticsInterval, commStatistics, poolStatistics, Protocol.OPCODE_NAMES);
            statisticsWriter.setDaemon(true);
            statisticsWriter.start();
        } else {
            commStatistics = null;
            poolStatistics = null;
            statisticsWriter = null;
        }

        currentEventTime = 0;
        minEventTime = 0;
        nextID = 0;
        sequencers = new HashMap<String, Integer>();

        events = new EventList();
        eventStats = new int[Event.NR_OF_TYPES];
        elections = new ElectionSet();

        if (gossip) {
            members = new ListMemberSet();
            new IterativeEventPusher(this, eventPushInterval, false, false);
            new RandomEventPusher(this, gossipInterval, adaptGossipInterval);
        } else if (tree) {
            members = new TreeMemberSet();
            // on new event send to children in tree
            new IterativeEventPusher(this, 0, true, true);

            // once in a while forward to everyone
            new IterativeEventPusher(this, eventPushInterval, false, false);
        } else { // central
            members = new ListMemberSet();
            new IterativeEventPusher(this, eventPushInterval, true, false);
        }

        pusher = new OndemandEventPusher(this);

        ThreadPool.createNew(this, "pool pinger thread");

    }

    private static void print(String message) {
        DateFormat format =
            DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.FRANCE);

        System.out.println(format.format(new Date(System.currentTimeMillis()))
                + " Central Registry: " + message);

    }

    synchronized int getEventTime() {
        return currentEventTime;
    }

    synchronized int getMinEventTime() {
        return minEventTime;
    }

    synchronized Event addEvent(int type, String description,
            IbisIdentifier... ibisses) {
        Event event = new Event(currentEventTime, type, description, ibisses);
        logger.debug("adding new event: " + event);
        events.add(event);
        eventStats[type]++;

        currentEventTime++;
        notifyAll();

        return event;
    }

    synchronized void waitForEventTime(int time, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        }

        while (getEventTime() < time) {
            if (hasEnded()) {
                return;
            }

            long currentTime = System.currentTimeMillis();

            if (currentTime >= deadline) {
                return;
            }

            try {
                wait(deadline - currentTime);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    synchronized int getSize() {
        return members.size();
    }

    int getFixedSize() {
        return fixedSize;
    }

    public boolean isClosedWorld() {
        return closedWorld;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#ended()
     */
    synchronized boolean hasEnded() {
        return ended;
    }

    synchronized boolean isClosed() {
        return closed;
    }

    synchronized void end() {
        ended = true;
        staleTime = System.currentTimeMillis() + STALE_TIMEOUT;
        pusher.enqueue(null);
        statisticsWriter.end();
    }

    public synchronized boolean stale() {
        logger.debug("pool stale at " + staleTime + " now " + System.currentTimeMillis() + " ended = " + ended);
        
        return ended && (System.currentTimeMillis() > staleTime);
    }

    String getName() {
        return name;
    }

    public CommunicationStatistics getCommStats() {
        return commStatistics;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#join(byte[], byte[],
     *      ibis.ipl.impl.Location)
     */
    synchronized Member join(byte[] implementationData, byte[] clientAddress,
            Location location, String ibisImplementationIdentifier)
            throws IOException {
        if (hasEnded()) {
            throw new IOException("Pool already ended");
        }

        if (isClosed()) {
            throw new IOException("Closed-World Pool already closed");
        }

        if (!ibisImplementationIdentifier.equals(this.ibisImplementationIdentifier)) {
            throw new IOException("Ibis implementation "
                    + ibisImplementationIdentifier
                    + " does not match pool's Ibis implementation: "
                    + this.ibisImplementationIdentifier);
        }
        logger.debug("ibis version: " + ibisImplementationIdentifier);

        String id = Integer.toString(nextID);
        nextID++;

        IbisIdentifier identifier =
            new IbisIdentifier(id, implementationData, clientAddress, location,
                    name);

        Event event = addEvent(Event.JOIN, null, identifier);
        if (poolStatistics != null) {
            poolStatistics.ibisJoined();
        }

        Member member = new Member(identifier, event);
        member.setCurrentTime(getMinEventTime());
        member.updateLastSeenTime();

        members.add(member);

        if (printEvents) {
            print(identifier + " joined pool \"" + name + "\" now "
                    + members.size() + " members");
        }

        if (closedWorld && nextID >= fixedSize) {
            closed = true;
            if (printEvents) {
                print("pool \"" + name + "\" now closed");
            }
            addEvent(Event.POOL_CLOSED, null, new IbisIdentifier[0]);
        }

        return member;
    }

    void writeBootstrapList(DataOutputStream out) throws IOException {
        Member[] peers = getRandomMembers(Protocol.BOOTSTRAP_LIST_SIZE);

        out.writeInt(peers.length);
        for (Member member : peers) {
            member.getIbis().writeTo(out);
        }

    }

    public void writeState(DataOutputStream out, int joinTime)
            throws IOException {

        ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(arrayOut);

        // create byte array of data
        synchronized (this) {
            members.writeTo(dataOut);
            elections.writeTo(dataOut);

            Event[] signals =
                events.getSignalEvents(joinTime, currentEventTime);
            dataOut.writeInt(signals.length);
            for (Event event : signals) {
                event.writeTo(dataOut);
            }

            dataOut.writeBoolean(closed);
            dataOut.writeInt(currentEventTime);

        }

        dataOut.flush();
        arrayOut.writeTo(out);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#leave(ibis.ipl.impl.IbisIdentifier)
     */
    synchronized void leave(IbisIdentifier identifier) throws Exception {
        if (members.remove(identifier) == null) {
            logger.error("unknown ibis " + identifier + " tried to leave");
            throw new Exception("ibis unknown: " + identifier);
        }
        if (printEvents) {
            print(identifier + " left pool \"" + name + "\" now "
                    + members.size() + " members");
        }

        addEvent(Event.LEAVE, null, identifier);
        if (poolStatistics != null) {
            poolStatistics.ibisLeft();
        }

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());
            if (poolStatistics != null) {
                poolStatistics.unElect();
            }
        }

        if (members.size() == 0) {
            end();
            if (printEvents) {
                print("pool \"" + name + "\" ended");
            } else {
                logger.info("Central Registry: " + "pool \"" + name
                        + "\" ended");
            }
            notifyAll();

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#dead(ibis.ipl.impl.IbisIdentifier)
     */
    synchronized void dead(IbisIdentifier identifier, Exception exception) {
        Member member = members.remove(identifier);
        if (member == null) {
            // member removed already
            return;
        }

        if (printEvents) {
            if (printErrors) {
                print(identifier + " died in pool \"" + name + "\" now "
                        + members.size() + " members, caused by:");
                exception.printStackTrace(System.out);
            } else {
                print(identifier + " died in pool \"" + name + "\" now "
                        + members.size() + " members");
            }
        }

        addEvent(Event.DIED, null, identifier);
        if (poolStatistics != null) {
            poolStatistics.ibisDied();
        }

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());
            if (poolStatistics != null) {
                poolStatistics.unElect();
            }

            elections.remove(election.getName());
        }

        if (members.size() == 0) {
            end();
            if (printEvents) {
                print("pool " + name + " ended");
            } else {
                logger.info("Central Registry: " + "pool \"" + name
                        + "\" ended");
            }
            notifyAll();
        }

        pusher.enqueue(member);
    }

    synchronized Event[] getEvents(int startTime) {
        return events.getList(startTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#elect(java.lang.String,
     *      ibis.ipl.impl.IbisIdentifier)
     */
    synchronized IbisIdentifier elect(String electionName,
            IbisIdentifier candidate) {
        Election election = elections.get(electionName);

        if (election == null) {
            // Do the election now. The caller WINS! :)

            Event event = addEvent(Event.ELECT, electionName, candidate);
            if (poolStatistics != null) {
                poolStatistics.newElection();
            }

            election = new Election(event);

            elections.put(election);

            if (printEvents) {
                print(candidate + " won election \"" + electionName
                        + "\" in pool \"" + name + "\"");
            }

        }

        return election.getWinner();
    }

    synchronized long getSequenceNumber(String name) {
        Integer currentValue = sequencers.get(name);

        if (currentValue == null) {
            currentValue = new Integer(0);
        }

        int result = currentValue;

        sequencers.put(name, currentValue + 1);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#maybeDead(ibis.ipl.impl.IbisIdentifier)
     */
    synchronized void maybeDead(IbisIdentifier identifier) {
        Member member = members.get(identifier);

        if (member != null) {
            member.clearLastSeenTime();
            // wake up checker thread, this suspect now (among) the oldest
            notifyAll();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#signal(java.lang.String,
     *      ibis.ipl.impl.IbisIdentifier[])
     */
    synchronized void signal(String signal, IbisIdentifier[] victims) {
        ArrayList<IbisIdentifier> result = new ArrayList<IbisIdentifier>();

        for (IbisIdentifier victim : victims) {
            if (members.contains(victim)) {
                result.add(victim);
            }
        }
        addEvent(Event.SIGNAL, signal,
            result.toArray(new IbisIdentifier[result.size()]));
        notifyAll();

    }

    void ping(Member member) {
        long start = System.currentTimeMillis();

        if (hasEnded()) {
            return;
        }
        if (!isMember(member)) {
            return;
        }
        logger.debug("pinging " + member);
        Connection connection = null;
        try {

            logger.debug("creating connection to " + member);
            connection =
                new Connection(member.getIbis(), CONNECT_TIMEOUT, false,
                        socketFactory);
            logger.debug("connection created to " + member
                    + ", send opcode, checking for reply");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_PING);
            connection.out().flush();
            // get reply
            connection.getAndCheckReply();

            IbisIdentifier result = new IbisIdentifier(connection.in());

            connection.close();

            if (!result.equals(member.getIbis())) {
                throw new Exception("ping ended up at wrong ibis");
            }
            logger.debug("ping to " + member + " successful");
            member.updateLastSeenTime();
            if (commStatistics != null) {
                commStatistics.add(Protocol.OPCODE_PING,
                    System.currentTimeMillis() - start, connection.read(),
                    connection.written(), false);
            }
        } catch (Exception e) {
            logger.debug("error on pinging ibis " + member, e);

            if (connection != null) {
                connection.close();
            }
            dead(member.getIbis(), e);
        }
    }

    /**
     * Push events to the given member. Checks if the pool has not ended, and
     * the peer is still a current member of this pool.
     * 
     * @param member
     *            The member to push events to
     * @param force
     *            if true, events are always pushed, even if the pool has ended
     *            or the peer is no longer a member.
     */
    void push(Member member, boolean force) {
        long start = System.currentTimeMillis();
        if (hasEnded()) {
            if (!force) {
                return;
            }
        }
        if (!isMember(member)) {
            if (!force) {
                return;
            }
        }
        if (force) {
            logger.debug("forced pushing entries to " + member);
        } else {
            logger.debug("pushing entries to " + member);
        }

        Connection connection = null;
        try {

            int peerTime = 0;

            logger.debug("creating connection to push events to " + member);

            connection =
                new Connection(member.getIbis(), CONNECT_TIMEOUT, true,
                        socketFactory);

            logger.debug("connection to " + member + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_PUSH);
            connection.out().writeUTF(getName());
            connection.out().flush();

            logger.debug("waiting for peer time of peer " + member);
            peerTime = connection.in().readInt();

            Event[] events;
            if (peerTime == -1) {
                // peer not finished join yet.
                events = new Event[0];

            } else {
                member.setCurrentTime(peerTime);
                events = getEvents(peerTime);
            }

            if (events == null) {
                connection.closeWithError("could not get events");
                return;
            }
            connection.sendOKReply();

            logger.debug("sending " + events.length + " entries to " + member);

            connection.out().writeInt(events.length);

            for (int i = 0; i < events.length; i++) {

                events[i].writeTo(connection.out());
            }

            connection.out().writeInt(getMinEventTime());

            connection.getAndCheckReply();
            connection.close();

            logger.debug("connection to " + member + " closed");
            member.updateLastSeenTime();
            if (commStatistics != null) {
                commStatistics.add(Protocol.OPCODE_PUSH,
                    System.currentTimeMillis() - start, connection.read(),
                    connection.written(), false);
            }
        } catch (IOException e) {
            if (isMember(member)) {
                if (printErrors) {
                    print("cannot reach " + member + " to push events to");
                    e.printStackTrace(System.out);
                }
            }

        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private synchronized Member getSuspectMember() {
        while (!hasEnded()) {

            Member oldest = members.getLeastRecentlySeen();

            logger.debug("oldest = " + oldest);

            long currentTime = System.currentTimeMillis();

            long timeout;

            if (oldest == null) {
                timeout = 1000;
            } else {
                timeout =
                    (oldest.getLastSeen() + heartbeatInterval) - currentTime;
            }

            if (timeout <= 0) {
                logger.debug(oldest + " now a suspect");
                return oldest;
            }

            // wait a while, get oldest again (might have changed)
            try {
                logger.debug(timeout + " milliseconds until " + oldest
                        + " needs checking");
                wait(timeout);
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
        return null;
    }

    synchronized void gotHeartbeat(IbisIdentifier identifier) {
        Member member = members.get(identifier);

        logger.debug("updating last seen time for " + member);

        if (member != null) {
            member.updateLastSeenTime();
        }
    }

    public void gotStatistics(IbisIdentifier identifier,
            CommunicationStatistics commStats, PoolStatistics poolStats, long timeOffset) {
        statisticsWriter.addStatistics(commStats, poolStats, identifier, timeOffset);
    }

    synchronized Member[] getRandomMembers(int size) {
        return members.getRandom(size);
    }

    synchronized Member getRandomMember() {
        return members.getRandom();
    }

    synchronized boolean isMember(Member member) {
        return members.contains(member);
    }

    synchronized Member[] getMembers() {
        return members.asArray();
    }

    /**
     * Returns the children of the root node
     */
    synchronized Member[] getChildren() {
        return members.getRootChildren();
    }

    public String toString() {
        return "Pool " + name + ": value = " + getSize() + ", event time = "
                + getEventTime();
    }

    public synchronized String getStats() {
        StringBuilder message = new StringBuilder();

        Formatter formatter = new Formatter(message);

        if (isClosedWorld()) {
            formatter.format(
                "%-18s %12d %10d %5d %6d %5d %9d %7d %10d %6b %5b\n",
                getName(), getSize(), getEventTime(), eventStats[Event.JOIN],
                eventStats[Event.LEAVE], eventStats[Event.DIED],
                eventStats[Event.ELECT], eventStats[Event.SIGNAL],
                getFixedSize(), isClosed(), ended);
        } else {
            formatter.format(
                "%-18s %12d %10d %5d %6d %5d %9d %7d %10s %6b %5b\n",
                getName(), getSize(), getEventTime(), eventStats[Event.JOIN],
                eventStats[Event.LEAVE], eventStats[Event.DIED],
                eventStats[Event.ELECT], eventStats[Event.SIGNAL], "N.A.",
                isClosed(), ended);
        }

        return message.toString();
    }

    /**
     * Remove events from the event history to make space.
     * 
     */
    synchronized void purgeHistory() {
        int newMinimum = members.getMinimumTime();

        if (newMinimum == -1) {
            // pool is empty, clear out all events
            newMinimum = getEventTime();
        }

        if (newMinimum < minEventTime) {
            logger.error("tried to set minimum event time backwards");
            return;
        }

        events.purgeUpto(newMinimum);

        minEventTime = newMinimum;
    }

    /**
     * contacts any suspect nodes when asked
     */
    public void run() {
        logger.debug("new pinger thread started");
        Member suspect = getSuspectMember();
        // fake we saw this member so noone else tries to ping it too
        if (suspect != null) {
            suspect.updateLastSeenTime();
        }

        if (hasEnded()) {
            return;
        }

        // start a new thread for pining another suspect
        ThreadPool.createNew(this, "pool pinger thread");

        if (suspect != null) {
            ping(suspect);
        }

    }

}
