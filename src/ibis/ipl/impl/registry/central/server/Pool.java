package ibis.ipl.impl.registry.central.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.central.Connection;
import ibis.ipl.impl.registry.central.Election;
import ibis.ipl.impl.registry.central.ElectionSet;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.EventList;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.MemberSet;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.smartsockets.virtual.VirtualSocketFactory;
import ibis.util.ThreadPool;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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

    private final boolean closedWorld;

    // size of this pool (if closed world)
    private final int fixedSize;

    private final boolean printEvents;

    private final boolean printErrors;

    // statistics for the server
    private final Stats serverStats;

    private int nextID;

    private long nextSequenceNr;

    private boolean ended = false;

    private boolean closed = false;

    private long staleTime;

    Pool(String name, VirtualSocketFactory socketFactory,
            long heartbeatInterval, long eventPushInterval, boolean gossip,
            long gossipInterval, boolean adaptGossipInterval, boolean tree,
            boolean closedWorld, int poolSize, boolean printEvents,
            boolean printErrors, Stats serverStats) {
        this.name = name;
        this.socketFactory = socketFactory;
        this.heartbeatInterval = heartbeatInterval;
        this.closedWorld = closedWorld;
        this.fixedSize = poolSize;
        this.printEvents = printEvents;
        this.printErrors = printErrors;
        this.serverStats = serverStats;

        currentEventTime = 0;
        minEventTime = 0;
        nextID = 0;
        nextSequenceNr = 0;

        events = new EventList();
        elections = new ElectionSet();
        members = new MemberSet();

        if (gossip) {
            new IterativeEventPusher(this, eventPushInterval, false);
            new RandomEventPusher(this, gossipInterval, adaptGossipInterval);
        } else if (tree) {
            new IterativeEventPusher(this, eventPushInterval, false);
            new EventBroadcaster(this);
        } else { // central
            new IterativeEventPusher(this, eventPushInterval, true);
        }

        pusher = new OndemandEventPusher(this);

        ThreadPool.createNew(this, "pool pinger thread");

    }

    private static void print(String message) {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.MEDIUM,
                Locale.FRANCE);

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
    }

    public synchronized boolean stale() {
        return ended && (System.currentTimeMillis() > staleTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#getName()
     */
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
            Location location) throws Exception {
        if (hasEnded()) {
            throw new Exception("Pool already ended");
        }

        if (isClosed()) {
            throw new Exception("Closed-World Pool already closed");
        }

        String id = Integer.toString(nextID);
        nextID++;

        IbisIdentifier identifier = new IbisIdentifier(id, implementationData,
                clientAddress, location, name);

        Event event = addEvent(Event.JOIN, null, identifier);

        Member member = new Member(identifier, event);
        member.setCurrentTime(getMinEventTime());
        member.updateLastSeenTime();

        members.add(member);

        if (printEvents) {
            print(identifier + " joined pool \"" + name + "\" now "
                    + members.size() + " members");
            if (closedWorld && nextID >= fixedSize) {
                print("pool \"" + name + "\" now closed");
            }
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

            Event[] signals = events
                    .getSignalEvents(joinTime, currentEventTime);
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

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());

        }

        if (members.size() == 0) {
            ended = true;
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

        Election[] deadElections = elections.getElectionsWonBy(identifier);

        for (Election election : deadElections) {
            addEvent(Event.UN_ELECT, election.getName(), election.getWinner());

        }

        if (members.size() == 0) {
            ended = true;
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
    synchronized IbisIdentifier elect(String electionName, IbisIdentifier candidate) {
        Election election = elections.get(electionName);

        if (election == null) {
            // Do the election now. The caller WINS! :)
            
            Event event = addEvent(Event.ELECT, electionName, candidate);
            
            election = new Election(event);

            elections.put(election);

            if (printEvents) {
                print(candidate + " won election \"" + electionName + "\" in pool \""
                        + name + "\"");
            }

        }

        return election.getWinner();
    }

    synchronized long getSequenceNumber() {
        return nextSequenceNr++;
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
        addEvent(Event.SIGNAL, signal, result.toArray(new IbisIdentifier[result
                .size()]));
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
                    false, socketFactory);
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
            serverStats.add(Protocol.OPCODE_PING, System.currentTimeMillis()
                    - start, false);
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
     *                The member to push events to
     * @param force
     *                if true, events are always pushed, even if the pool has
     *                ended or the peer is no longer a member.
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

            connection = new Connection(member.getIbis(), CONNECT_TIMEOUT,
                    true, socketFactory);

            logger.debug("connection to " + member + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_PUSH);
            connection.out().writeUTF(getName());
            connection.out().flush();

            logger.debug("waiting for peer time of peer " + member);
            peerTime = connection.in().readInt();

            Event[] events;
            if (peerTime == -1) {
                //peer not finished join yet.
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
            serverStats.add(Protocol.OPCODE_PUSH, System.currentTimeMillis()
                    - start, false);
        } catch (IOException e) {
            if (isMember(member)) {
                if (printErrors) {
                    logger.error("cannot reach " + member
                            + " to push events to", e);
                }
            }

        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    /**
     * Push events to the given member. Checks if the pool has not ended, and
     * the peer is still a current member of this pool.
     * 
     * @param member
     *                The member to broadcast events to
     */
    void forward(Member member, Event[] events) {
        long start = System.currentTimeMillis();
        if (hasEnded()) {
            return;
        }
        if (!isMember(member)) {
            return;
        }
        logger.debug("forwarding entries to " + member);

        Connection connection = null;
        try {
            logger.debug("creating connection to forward events to " + member);

            connection = new Connection(member.getIbis(), CONNECT_TIMEOUT,
                    true, socketFactory);

            logger.debug("connection to " + member + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_BROADCAST);
            connection.out().writeInt(events.length);
            for (Event event : events) {
                event.writeTo(connection.out());
            }
            connection.out().flush();

            connection.getAndCheckReply();
            connection.close();

            logger.debug("connection to " + member + " closed");
            member.updateLastSeenTime();
            serverStats.add(Protocol.OPCODE_BROADCAST, System
                    .currentTimeMillis()
                    - start, false);
        } catch (IOException e) {
            if (isMember(member)) {
                if (printErrors) {
                    logger.error("cannot reach " + member
                            + " to push events to", e);
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
                timeout = (oldest.getLastSeen() + heartbeatInterval)
                        - currentTime;
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
     * Returns the children of the root node, in a binomial tree. The indexes of
     * the children are 1, 2, 4, 8, etc...
     */
    synchronized Member[] getChildren() {
        ArrayList<Member> result = new ArrayList<Member>();
        int next = 1;

        while (next <= members.size()) {
            result.add(0, members.get(next - 1));

            next = next * 2;
        }
        return result.toArray(new Member[0]);
    }

    public String toString() {
        return "Pool " + name + ": size = " + getSize() + ", event time = "
                + getEventTime();
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
