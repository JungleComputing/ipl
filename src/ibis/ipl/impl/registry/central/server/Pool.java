package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.Connection;
import ibis.ipl.impl.registry.central.Election;
import ibis.ipl.impl.registry.central.ElectionSet;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.EventList;
import ibis.ipl.impl.registry.central.ListMemberSet;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.MemberSet;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.ipl.impl.registry.central.TreeMemberSet;
import ibis.ipl.impl.registry.statistics.Statistics;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Pool implements Runnable {

    public static final int BOOTSTRAP_LIST_SIZE = 25;

    // 10 seconds connect timeout
    private static final int CONNECT_TIMEOUT = 10000;

    // minimum time between two "ping" attempts if maybeDead() was
    // called by the user
    private static final long RECENTLY_SEEN_THRESHOLD = 1000;

    private static final Logger logger = LoggerFactory.getLogger(Pool.class);

    private final VirtualSocketFactory socketFactory;

    // list of all joins, leaves, elections, etc.
    private final EventList events;

    private final boolean peerBootstrap;

    // private final boolean gossip;
    //
    // private final boolean tree;

    private final long heartbeatInterval;

    private int currentEventTime;

    private int minEventTime;

    private final ElectionSet elections;

    private final MemberSet members;

    private final OndemandEventPusher pusher;

    private final String name;

    private String ibisImplementationIdentifier;

    private final boolean closedWorld;

    // size of this pool (if closed world)
    private final int fixedSize;

    private final boolean printEvents;

    private final boolean printErrors;
    
    private final boolean purgeHistory;

    // statistics are only kept on the request of the user

    private final Statistics statistics;

    // simple statistics which are always kept,
    // so the server can print them if so requested
    private final int[] eventStats;

    private final Map<String, Integer> sequencers;

    private int nextID;

    private boolean ended = false;

    private boolean closed = false;

    private Event closeEvent = null;

    private boolean terminated = false;

    private Event terminateEvent = null;

    Pool(String name, VirtualSocketFactory socketFactory,
            boolean peerBootstrap, long heartbeatInterval,
            long eventPushInterval, boolean gossip, long gossipInterval,
            boolean adaptGossipInterval, boolean tree, boolean closedWorld,
            int poolSize, boolean keepStatistics, long statisticsInterval,
            String ibisImplementationIdentifier, boolean printEvents,
            boolean printErrors, boolean purgeHistory) {
        print("creating new pool: \"" + name + "\"");

        this.name = name;
        this.socketFactory = socketFactory;
        this.peerBootstrap = peerBootstrap;
        this.heartbeatInterval = heartbeatInterval;
        // this.gossip = gossip;
        // this.tree = tree;
        this.closedWorld = closedWorld;
        this.fixedSize = poolSize;
        this.ibisImplementationIdentifier = ibisImplementationIdentifier;
        this.printEvents = printEvents;
        this.printErrors = printErrors;
        this.purgeHistory = purgeHistory;

        if (keepStatistics) {
            statistics = new Statistics(Protocol.OPCODE_NAMES);
            statistics.setID("server", name);
            statistics.startWriting(statisticsInterval);
        } else {
            statistics = null;
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
            // also check for needed updates every second.
            new IterativeEventPusher(this, 1000, true, true);

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
        System.err.printf("%tT Central Registry: %s\n", System
                .currentTimeMillis(), message);
    }

    synchronized int getEventTime() {
        return currentEventTime;
    }

    synchronized int getMinEventTime() {
        return minEventTime;
    }

    synchronized Event addEvent(int type, String description,
            IbisIdentifier ibis, IbisIdentifier... ibisses) {
        Event event = new Event(currentEventTime, type, description, ibis,
                ibisses);
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

    synchronized ibis.ipl.Location[] getLocations() {
        HashSet<ibis.ipl.Location> locations = new HashSet<ibis.ipl.Location>();

        for (Member member : members.asArray()) {
            locations.add(member.getIbis().location());
        }

        return locations.toArray(new Location[0]);
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
        pusher.enqueue(null);
        if (statistics != null) {
            statistics.write();
            statistics.end();
        }
    }

    public void saveStatistics() {
        if (statistics != null) {
            statistics.write();
        }
    }

    String getName() {
        return name;
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
            throw new IOException("Pool already closed");
        }

        // On Android, the implementation identifier set to an empty string,
        // because it cannot be extracted from a jar file. So, we allow for that
        // as well.
        if (!ibisImplementationIdentifier.equals("")) {
            if (this.ibisImplementationIdentifier.equals("")) {
                this.ibisImplementationIdentifier = ibisImplementationIdentifier;
            } else {
                if (!ibisImplementationIdentifier
                        .equals(this.ibisImplementationIdentifier)) {
                    throw new IOException("Ibis implementation "
                            + ibisImplementationIdentifier
                            + " does not match pool's Ibis implementation: "
                            + this.ibisImplementationIdentifier);
                }
            }
        }

        logger.debug("ibis version: " + ibisImplementationIdentifier);

        String id = Integer.toString(nextID);
        nextID++;

        IbisIdentifier identifier = new IbisIdentifier(id, implementationData,
                clientAddress, location, name);

        Event event = addEvent(Event.JOIN, null, identifier);

        Member member = new Member(identifier, event);
        member.setCurrentTime(getMinEventTime());
        member.updateTime();

        members.add(member);

        if (logger.isDebugEnabled()) {
            logger.debug("members now: " + members);
        }

        if (statistics != null) {
            statistics.newPoolSize(members.size());
        }

        if (printEvents) {
            print(identifier + " joined pool \"" + name + "\" now "
                    + members.size() + " members");
        }

        if (closedWorld && nextID >= fixedSize) {
            close();
        }

        return member;
    }

    private synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeEvent = addEvent(Event.POOL_CLOSED, null, null,
                new IbisIdentifier[0]);
        if (printEvents) {
            print("pool \"" + name + "\" now closed");
        }
    }

    void writeBootstrapList(DataOutputStream out) throws IOException {
        if (!peerBootstrap) {
            // send a list containing 0 members. It is not used anyway
            out.writeInt(0);
            return;
        }

        Member[] peers = getRandomMembers(BOOTSTRAP_LIST_SIZE);

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
            dataOut.writeInt(currentEventTime);

            members.writeTo(dataOut);
            elections.writeTo(dataOut);

            Event[] signals = events
                    .getSignalEvents(joinTime, currentEventTime);
            dataOut.writeInt(signals.length);
            for (Event event : signals) {
                event.writeTo(dataOut);
            }

            dataOut.writeBoolean(closed);
            if (closed) {
                closeEvent.writeTo(dataOut);
            }
            dataOut.writeBoolean(terminated);
            if (terminated) {
                terminateEvent.writeTo(dataOut);
            }
        }

        dataOut.flush();
        byte[] bytes = arrayOut.toByteArray();

        out.writeInt(bytes.length);
        out.write(bytes);

        logger.debug("pool state size = " + bytes.length);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#leave(ibis.ipl.impl.IbisIdentifier)
     */
    synchronized void leave(IbisIdentifier identifier) throws Exception {
        if (members.remove(identifier) == null) {
            // May happen if it was declared dead before. So, no exception.
            // --Ceriel
            // logger.error("unknown ibis " + identifier + " tried to leave");
            // throw new Exception("ibis unknown: " + identifier);
            logger.debug("unknown ibis " + identifier + " tried to leave");
            return;
        }
        if (printEvents) {
            print(identifier + " left pool \"" + name + "\" now "
                    + members.size() + " members");
        }

        addEvent(Event.LEAVE, null, identifier);

        if (statistics != null) {
            statistics.newPoolSize(members.size());
        }

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());
            if (statistics != null) {
                statistics.electionEvent();
            }
        }

        if (members.size() == 0) {
            end();
            print("pool \"" + name + "\" ended");
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
                exception.printStackTrace(System.err);
            } else {
                print(identifier + " died in pool \"" + name + "\" now "
                        + members.size() + " members");
            }
        }

        addEvent(Event.DIED, null, identifier);

        if (statistics != null) {
            statistics.newPoolSize(members.size());
        }

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());
            if (statistics != null) {
                statistics.electionEvent();
            }

            elections.remove(election.getName());
        }

        if (members.size() == 0) {
            end();
            print("pool " + name + " ended");
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
            IbisIdentifier candidate) throws IOException {
        Election election = elections.get(electionName);

        if (election == null) {
            if (!members.contains(candidate)) {
                throw new IOException(candidate + " tries to win election "
                        + electionName + ", but is not a member of the pool");
            }

            // Do the election now. The caller WINS! :)

            Event event = addEvent(Event.ELECT, electionName, candidate);
            if (statistics != null) {
                statistics.electionEvent();
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
            if (member.getTime() > (System.currentTimeMillis() - RECENTLY_SEEN_THRESHOLD)) {
                logger.debug("got maybeDead for member of pool " + identifier
                        + ", but recently contacted it, ignoring report");
            } else {
                logger.debug("got maybeDead for member of pool: " + identifier);

                member.clearTime();
                // wake up checker thread, this suspect now (among) the oldest
                notifyAll();
            }
        } else {
            logger.debug("got maybeDead for " + identifier
                    + " which is not in pool");
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#signal(java.lang.String,
     *      ibis.ipl.impl.IbisIdentifier[])
     */
    synchronized void signal(String signal, IbisIdentifier source,
            IbisIdentifier[] targets) {
        ArrayList<IbisIdentifier> result = new ArrayList<IbisIdentifier>();

        for (IbisIdentifier target : targets) {
            if (members.contains(target)) {
                result.add(target);
            }
        }
        addEvent(Event.SIGNAL, signal, source, result
                .toArray(new IbisIdentifier[result.size()]));
        notifyAll();

    }

    synchronized void terminate(IbisIdentifier source) {
        if (terminated) {
            return;
        }

        // close pool, preventing any new ibisses from joining
        close();

        terminated = true;
        terminateEvent = addEvent(Event.POOL_TERMINATED, null, source);
        if (printEvents) {
            print("pool \"" + name + "\" now terminated");
        }
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
            connection = new Connection(member.getIbis(), CONNECT_TIMEOUT,
                    true, socketFactory);
            logger.debug("connection created to " + member
                    + ", send opcode, checking for reply");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.VERSION);
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
            member.updateTime();
            if (statistics != null) {
                statistics
                        .add(Protocol.OPCODE_PING, System.currentTimeMillis()
                                - start, connection.read(), connection
                                .written(), false);
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
    void push(Member member, boolean force, boolean isBroadcast) {
        byte opcode;

        if (isBroadcast) {
            opcode = Protocol.OPCODE_BROADCAST;
        } else {
            opcode = Protocol.OPCODE_PUSH;
        }

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
            long connecting = System.currentTimeMillis();

            logger.debug("creating connection to push events to " + member);

            connection = new Connection(member.getIbis(), CONNECT_TIMEOUT,
                    true, socketFactory);

            long connected = System.currentTimeMillis();

            logger.debug("connection to " + member + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.VERSION);
            connection.out().writeByte(opcode);
            connection.out().writeUTF(getName());
            connection.out().flush();

            long writtenOpcode = System.currentTimeMillis();

            logger.debug("waiting for info of peer " + member);

            boolean requestBootstrap = connection.in().readBoolean();
            int joinTime = connection.in().readInt();
            int requestedEventTime = connection.in().readInt();

            long readInfo = System.currentTimeMillis();

            connection.sendOKReply();

            long sendOk = System.currentTimeMillis();

            if (requestBootstrap) {
                // peer requests bootstrap data
                writeState(connection.out(), joinTime);
            }
            long writtenState = System.currentTimeMillis();

            member.setCurrentTime(requestedEventTime);
            Event[] events = getEvents(requestedEventTime);

            long gotEvents = System.currentTimeMillis();

            logger.debug("sending " + events.length + " entries to " + member);

            connection.out().writeInt(events.length);

            for (int i = 0; i < events.length; i++) {
                events[i].writeTo(connection.out());
            }

            long writtenEvents = System.currentTimeMillis();

            connection.out().writeInt(getMinEventTime());

            connection.out().flush();

            long writtenAll = System.currentTimeMillis();

            connection.close();

            long closedConnection = System.currentTimeMillis();

            logger.debug("connection to " + member + " closed");
            member.updateTime();

            long done = System.currentTimeMillis();

            if (statistics != null) {
                long end = System.currentTimeMillis();

                statistics.add(opcode, end - start, connection.read(),
                        connection.written(), false);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("connecting = " + (connecting - start)
                        + ", connected = " + (connected - connecting)
                        + ", writtenOpcode = " + (writtenOpcode - connected)
                        + ", readInfo = " + (readInfo - writtenOpcode)
                        + ", sendOk = " + (sendOk - readInfo)
                        + ", writtenState (" + requestBootstrap + ") = "
                        + (writtenState - sendOk) + "\n\t\t\t" +

                        "gotEvents = " + (gotEvents - writtenState)
                        + ", writtenEvents = " + (writtenEvents - gotEvents)
                        + ", writtenAll = " + (writtenAll - writtenEvents)
                        + ", closedConnection = "
                        + (closedConnection - writtenAll) + ", done = "
                        + (done - closedConnection));

            }

        } catch (IOException e) {
            if (isMember(member)) {
                if (printErrors) {
                    print("cannot reach " + member + " to push events to");
                    // e.printStackTrace(System.err);
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
                timeout = (oldest.getTime() + heartbeatInterval) - currentTime;
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
            member.updateTime();
        }
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

    public synchronized String getStatsString() {
        StringBuilder message = new StringBuilder();

        Formatter formatter = new Formatter(message);

        if (isClosedWorld()) {
            formatter.format("%s\n     %12d %5d %6d %5d %9d %7d %10d %6b %5b",
                    getName(), getSize(), eventStats[Event.JOIN],
                    eventStats[Event.LEAVE], eventStats[Event.DIED],
                    eventStats[Event.ELECT], eventStats[Event.SIGNAL],
                    getFixedSize(), isClosed(), ended);
        } else {
            formatter.format("%s\n     %12d %5d %6d %5d %9d %7d %10s %6b %5b",
                    getName(), getSize(), eventStats[Event.JOIN],
                    eventStats[Event.LEAVE], eventStats[Event.DIED],
                    eventStats[Event.ELECT], eventStats[Event.SIGNAL], "N.A.",
                    isClosed(), ended);
        }

        return message.toString();
    }

    public synchronized Map<String, String> getStatsMap() {
        Map<String, String> result = new HashMap<String, String>();

        result.put(name + ".size", "" + getSize());
        result.put(name + ".joins", "" + eventStats[Event.JOIN]);
        result.put(name + ".leaves", "" + eventStats[Event.LEAVE]);
        result.put(name + ".dieds", "" + eventStats[Event.DIED]);
        result.put(name + ".elections", "" + eventStats[Event.ELECT]);
        result.put(name + ".signals", "" + eventStats[Event.SIGNAL]);
        result.put(name + ".fixed.size", "" + getFixedSize());
        result.put(name + ".closed", "" + "" + isClosed());
        result.put(name + ".ended", "" + "" + ended);

        return result;
    }

    /**
     * Remove events from the event history to make space.
     * 
     */
    synchronized void purgeHistory() {
        if (!purgeHistory) {
            //do nothing, history purge disabled
            return;
        }
        
        int newMinimum = members.getMinimumTime();

        if (newMinimum == -1) {
            // pool is empty, clear out all events
            newMinimum = getEventTime();
        }

        if (newMinimum < minEventTime) {
            logger.error("tried to set minimum event time backwards");
            return;
        }

        events.setMinimum(newMinimum);

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
            suspect.updateTime();
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

    Statistics getStatistics() {
        return statistics;
    }

}
