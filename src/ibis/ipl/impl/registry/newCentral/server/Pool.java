package ibis.ipl.impl.registry.newCentral.server;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.impl.registry.newCentral.Connection;
import ibis.ipl.impl.registry.newCentral.ConnectionFactory;
import ibis.ipl.impl.registry.newCentral.Event;
import ibis.ipl.impl.registry.newCentral.Protocol;
import ibis.util.ThreadPool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

final class Pool implements Runnable {

    // time until the pool data is removed after the pool has ended (2 minutes)
    private static final int STALE_TIMEOUT = 120000;

    private static final int PUSH_THREADS = 10;

    // 10 seconds connect timeout
    private static final int CONNECT_TIMEOUT = 10000;

    private static final Logger logger = Logger.getLogger(Pool.class);

    // list of all joins, leaves, elections, etc.
    private final ArrayList<Event> events;

    private final long heartbeatInterval;

    private int currentEventTime;

    private int minEventTime;

    // cache of election results we've seen. Optimization over searching all the
    // events each time an election result is requested.
    // map<election name, winner>
    private final Map<String, IbisIdentifier> elections;

    private final MemberSet members;

    private final OndemandEventPusher pusher;

    private final String name;

    private final ConnectionFactory connectionFactory;

    private final boolean printEvents;

    private int nextID;

    private long nextSequenceNr;

    private boolean ended = false;

    private long staleTime;

    Pool(String name, ConnectionFactory connectionFactory,
            long heartbeatInterval, boolean gossip, long gossipInterval,
            boolean adaptGossipInterval, boolean tree, boolean printEvents) {
        this.name = name;
        this.connectionFactory = connectionFactory;
        this.heartbeatInterval = heartbeatInterval;
        this.printEvents = printEvents;

        currentEventTime = 0;
        minEventTime = 0;
        nextID = 0;
        nextSequenceNr = 0;

        events = new ArrayList<Event>();
        elections = new HashMap<String, IbisIdentifier>();
        members = new MemberSet();

        if (gossip) {
            // TODO: do something :)
            throw new Error("gossip not working");
        } else if (tree) {
            throw new Error("tree not implemented");
        } else { // central
            new IterativeEventPusher(this, PUSH_THREADS);
        }

        pusher = new OndemandEventPusher(this);

        ThreadPool.createNew(this, "pool management thread");

    }

    synchronized int getEventTime() {
        return currentEventTime;
    }

    synchronized int getMinEventTime() {
        return minEventTime;
    }

    synchronized void addEvent(int type, String description,
            IbisIdentifier... ibisses) {
        if (!events.isEmpty()
                && events.get(events.size() - 1).getTime() != (currentEventTime - 1)) {
            throw new Error("event list inconsistent. Last event = "
                    + events.get(events.size() - 1) + " current time = "
                    + getEventTime());
        }

        Event event = new Event(currentEventTime, type, description, ibisses);
        events.add(event);
        currentEventTime++;
        notifyAll();
    }

    synchronized void waitForEventTime(int time) {
        while (getEventTime() < time) {
            if (ended()) {
                return;
            }

            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }
    }

    synchronized int getSize() {
        return members.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#ended()
     */
    synchronized boolean ended() {
        return ended;
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
    synchronized IbisIdentifier join(byte[] implementationData,
            byte[] clientAddress, Location location) throws Exception {
        if (ended()) {
            throw new Exception("Pool already ended");
        }

        String id = Integer.toString(nextID);
        nextID++;

        IbisIdentifier identifier =
                new IbisIdentifier(id, implementationData, clientAddress,
                        location, name);

        members.add(new Member(identifier));

        if (printEvents) {
            System.out.println("Central Registry: " + identifier
                    + " joined pool \"" + name + "\" now " + members.size()
                    + " members");
        }

        addEvent(Event.JOIN, null, identifier);

        return identifier;
    }

    void writeBootstrapList(DataOutputStream out) throws IOException {

        Member[] peers = getRandomMembers(Protocol.BOOTSTRAP_LIST_SIZE);

        out.writeInt(peers.length);
        for (Member member : peers) {
            member.getIbis().writeTo(out);
        }

    }

    public void writeState(DataOutputStream out) throws IOException {
        int time;
        Member[] memberArray;
        ArrayList<String> electionKeys = new ArrayList<String>();
        ArrayList<IbisIdentifier> electionValues =
                new ArrayList<IbisIdentifier>();

        // copy state
        synchronized (this) {
            time = getEventTime();
            memberArray = members.asArray();

            for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
                electionKeys.add(entry.getKey());
                electionValues.add(entry.getValue());
            }
        }

        // write state

        out.writeInt(memberArray.length);
        for (Member member : memberArray) {
            member.getIbis().writeTo(out);
        }

        out.writeInt(electionKeys.size());
        for (int i = 0; i < electionKeys.size(); i++) {
            electionKeys.get(0);
            electionValues.get(0);
        }

        out.writeInt(time);

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
            System.out.println("Central Registry: " + identifier
                    + " left pool \"" + name + "\" now " + members.size()
                    + " members");
        }

        addEvent(Event.LEAVE, null, identifier);

        Iterator<Entry<String, IbisIdentifier>> iterator =
                elections.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, IbisIdentifier> entry = iterator.next();
            if (entry.getValue().equals(identifier)) {
                iterator.remove();

                addEvent(Event.UN_ELECT, entry.getKey(), identifier);

            }
        }

        if (members.size() == 0) {
            ended = true;
            if (printEvents) {
                System.err.println("Central Registry: " + "pool \"" + name
                        + "\" ended");
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
            System.out.println("Central Registry: " + identifier
                    + " died in pool \"" + name + "\" now " + members.size()
                    + " members, caused by:");
            exception.printStackTrace(System.out);
        }

        addEvent(Event.DIED, null, identifier);

        Iterator<Map.Entry<String, IbisIdentifier>> iterator =
                elections.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, IbisIdentifier> entry = iterator.next();
            if (entry.getValue().equals(identifier)) {
                iterator.remove();

                addEvent(Event.UN_ELECT, entry.getKey(), identifier);
            }
        }

        if (members.size() == 0) {
            ended = true;
            if (printEvents) {
                System.out.println("Central Registry: " + "pool " + name
                        + " ended");
            } else {
                logger.info("Central Registry: " + "pool \"" + name
                        + "\" ended");
            }
            notifyAll();
        }

        pusher.enqueue(member);
    }

    synchronized Event[] getEvents(int startTime) throws IOException {
        logger.debug("getting events, startTime = " + startTime
                + ", event time = " + getEventTime() + " min event time = "
                + getMinEventTime());

        if (startTime == -1) {
            logger.debug("uninitialized peer, send everything!");
            return events.toArray(new Event[0]);
        }

        if (startTime < minEventTime) {
            logger
                    .error("client needs events we already removed from the history!");
            return null;
        }

        if (events.isEmpty()) {
            return new Event[0];
        }

        int eventHistoryOffset = events.get(0).getTime();

        int startIndex = startTime - eventHistoryOffset;

        if (startIndex < 0) {
            logger
                    .error("trying to get events from befor the start of the event list");
            return null;
        }

        // return the requested portion of the list
        return events.subList(startIndex, events.size()).toArray(new Event[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#elect(java.lang.String,
     *      ibis.ipl.impl.IbisIdentifier)
     */
    synchronized IbisIdentifier elect(String election, IbisIdentifier candidate) {
        IbisIdentifier winner = elections.get(election);

        if (winner == null) {
            // Do the election now. The caller WINS! :)
            winner = candidate;
            elections.put(election, winner);

            if (printEvents) {
                System.out.println("Central Registry: " + winner
                        + " won election \"" + election + "\" in pool \""
                        + name + "\"");
            }

            addEvent(Event.ELECT, election, winner);
        }

        return winner;
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
        if (ended()) {
            return;
        }
        if (!isMember(member)) {
            return;
        }
        logger.debug("pinging " + member);
        Connection connection = null;
        try {

            logger.debug("creating connection to " + member);
            connection = connectionFactory.connect(member.getIbis(), false);
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
        if (ended()) {
            if (!force) {
                return;
            }
        }
        if (!isMember(member)) {
            if (!force) {
                return;
            }
        }
        logger.debug("pushing entries to " + member);

        Connection connection = null;
        try {

            int peerTime = 0;

            logger.debug("creating connection to push events to " + member);

            connection =
                    connectionFactory.connect(member.getIbis(),
                            CONNECT_TIMEOUT, true);

            logger.debug("connection to " + member + " created");

            connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
            connection.out().writeByte(Protocol.OPCODE_PUSH);
            connection.out().writeUTF(getName());
            connection.out().flush();

            logger.debug("waiting for peer time of peer " + member);
            peerTime = connection.in().readInt();

            Event[] events = getEvents(peerTime);

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
        } catch (IOException e) {
            if (isMember(member)) {
                logger.warn("cannot reach " + member + " to push events to", e);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private synchronized Member getSuspectMember() {
        while (!ended()) {

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

        logger.debug("got hearbeart for " + member);

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

    public String toString() {
        return "Pool " + name + ": size = " + getSize() + ", event time = "
                + getEventTime();
    }

    /**
     * Remove events up to this event from the list to make space. Also tells
     * clients to do this.
     * 
     * @param eventTime
     *            the first event not to purge
     */
    synchronized void purgeUpto(int eventTime) {
        if (eventTime < minEventTime) {
            logger.warn("tried to set minimum event time backwards from "
                    + minEventTime + " to " + eventTime);
            return;
        }

        if (eventTime >= getEventTime()) {
            logger
                    .warn("tried to set minimum event time to after current time. Time = "
                            + getEventTime() + " new minimum = " + eventTime);
            return;
        }

        logger.debug("setting minimum event time to " + eventTime);

        minEventTime = eventTime;

        while (!events.isEmpty() && events.get(0).getTime() < minEventTime) {
            if (logger.isDebugEnabled()) {
                logger.debug("removing event: " + events.get(0));
            }
            events.remove(0);
        }
    }

    /**
     * contacts any suspect nodes when asked
     */
    public void run() {
        while (!ended()) {

            Member suspect = getSuspectMember();

            if (suspect != null) {
                ping(suspect);
            }
        }
    }

}
