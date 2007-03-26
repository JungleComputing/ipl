package ibis.impl.registry.central;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.util.ThreadPool;

final class Pool implements Runnable {

    public static final int MAX_TRIES = 5;

    public static final int PING_INTERVAL = 5 * 60 * 1000;

    public static final int PING_THREADS = 5;

    public static final int GOSSIP_INTERVAL = 120 * 60 * 1000;

    public static final int GOSSIP_THREADS = 10;

    public static final int PUSH_THREADS = 10;

    public static final int CONNECT_TIMEOUT = 10 * 1000;

    private static final Logger logger = Logger.getLogger(Pool.class);

    // list of all joins, leaves, elections, etc.
    private final ArrayList<Event> events;

    // cache of election results we've seen. Optimization over searching all the
    // events each time an election result is requested.
    // map<election name, winner>
    private final Map<String, IbisIdentifier> elections;

    private final MemberSet members;

    // List of "suspect" ibisses we must ping
    private final List<IbisIdentifier> checkList;

    private final boolean keepNodeState;

    private final String name;

    private final ConnectionFactory connectionFactory;

    private int nextID;

    private long nextSequenceNr;

    private boolean ended = false;

    public Pool(String name, ConnectionFactory connectionFactory,
            boolean gossip, boolean keepNodeState) {
        this.name = name;
        this.connectionFactory = connectionFactory;
        this.keepNodeState = keepNodeState;

        nextID = 0;
        nextSequenceNr = 0;

        events = new ArrayList<Event>();
        elections = new HashMap<String, IbisIdentifier>();
        members = new MemberSet();
        checkList = new LinkedList<IbisIdentifier>();

        if (gossip) {
            // ping iteratively
            new PeriodicNodeContactor(this, false, false, PING_INTERVAL,
                    PING_THREADS);
            // gossip randomly
            new PeriodicNodeContactor(this, true, true, GOSSIP_INTERVAL,
                    GOSSIP_THREADS);

            logger.info("created new GOSSIPING pool " + name);

        } else { // central
            // ping iteratively
            new PeriodicNodeContactor(this, false, false, PING_INTERVAL,
                    PING_THREADS);

            new EventPusher(this, PUSH_THREADS);

            logger.info("created new CENTRALIZED pool " + name);
        }

        ThreadPool.createNew(this, "pool management thread");

    }

    public synchronized int getEventTime() {
        return events.size();
    }

    public synchronized void waitForEventTime(int time) {
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

    public synchronized int getSize() {
        return members.size();
    }

    public synchronized boolean ended() {
        return ended;
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized IbisIdentifier join(byte[] implementationData,
            byte[] clientAddress, Location location) throws Exception {
        if (ended()) {
            throw new Exception("Pool already ended");
        }

        String id = Integer.toString(nextID);
        nextID++;

        IbisIdentifier identifier = new IbisIdentifier(id, implementationData,
                clientAddress, location, name);

        members.add(new Member(identifier));

        logger.info(identifier + " joined pool \"" + name + "\" now "
                + members.size() + " members");

        events.add(new Event(events.size(), Event.JOIN, identifier, null));
        notifyAll();

        return identifier;
    }

    public synchronized void leave(IbisIdentifier identifier) throws Exception {
        if (!members.remove(identifier.myId)) {
            logger.error("unknown ibis " + identifier + " tried to leave");
            throw new Exception("ibis unknown: " + identifier);
        }
        logger.info(identifier + " left pool \"" + name + "\" now "
                + members.size() + " members");

        events.add(new Event(events.size(), Event.LEAVE, identifier, null));
        notifyAll();

        Iterator<Entry<String,IbisIdentifier>> iterator = elections.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String,IbisIdentifier> entry = iterator.next();
            if (entry.getValue().equals(identifier)) {
                iterator.remove();

                events.add(new Event(events.size(), Event.UN_ELECT, identifier,
                        entry.getKey()));
                notifyAll();

            }
        }

        if (members.size() == 0) {
            ended = true;
            logger.info("pool " + name + " ended");
            notifyAll();

        }
    }

    public synchronized void dead(IbisIdentifier identifier) {
        if (!members.remove(identifier.myId)) {
            logger.warn("unknown ibis " + identifier + " died");
        }
        logger.info(identifier + " left pool \"" + name + "\" now "
                + members.size() + " members");

        events.add(new Event(events.size(), Event.DIED, identifier, null));
        notifyAll();

        for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
            if (entry.getValue().equals(identifier)) {
                elections.remove(entry.getKey());

                events.add(new Event(events.size(), Event.UN_ELECT, identifier,
                        entry.getKey()));
                notifyAll();

            }
        }

        if (members.size() == 0) {
            ended = true;
            logger.info("pool " + name + " ended");
            notifyAll();
        }
    }

    public synchronized Event[] getEvents(int startTime, int maxSize) {
        // logger.debug("getting events, startTime = " + startTime
        // + ", maxSize = " + maxSize +
        // ", event time = " + events.size());

        int resultSize = events.size() - startTime;

        if (resultSize > maxSize) {
            resultSize = maxSize;
        }
        if (resultSize < 0) {
            return new Event[0];
        }

        return events.subList(startTime, startTime + resultSize).toArray(
                new Event[0]);
    }

    public synchronized IbisIdentifier elect(String election,
            IbisIdentifier candidate) {
        IbisIdentifier winner = elections.get(election);

        if (winner == null) {
            // Do the election now. The caller WINS! :)
            winner = candidate;
            elections.put(election, winner);

            logger.info(winner + " won election \"" + election
                    + "\" in pool \"" + name + "\"");

            events.add(new Event(events.size(), Event.ELECT, winner, election));
            notifyAll();

        }

        return winner;
    }

    public synchronized long getSequenceNumber() {
        return nextSequenceNr++;
    }

    public synchronized void maybeDead(IbisIdentifier identifier) {
        // add to todo list :)
        checkList.add(identifier);
        notifyAll();
    }

    public synchronized void signal(String signal, IbisIdentifier[] victims) {
        ArrayList<IbisIdentifier> result = new ArrayList<IbisIdentifier>();

        for (IbisIdentifier victim : victims) {
            if (members.contains(victim.myId)) {
                result.add(victim);
            }
        }
        events.add(new Event(events.size(), Event.SIGNAL, result
                .toArray(new IbisIdentifier[0]), signal));
        notifyAll();

    }

    void ping(IbisIdentifier ibis) {
        logger.debug("pinging " + ibis);
        for (int i = 0; i < MAX_TRIES; i++) {
            Connection connection = null;
            try {

                connection = connectionFactory.connect(ibis,
                        Protocol.OPCODE_PING, CONNECT_TIMEOUT);

                // get reply
                connection.getAndCheckReply();

                IbisIdentifier result = new IbisIdentifier(connection.in());

                connection.close();

                if (result.equals(ibis)) {
                    return;
                }
            } catch (Exception e) {
                logger.debug("error on pinging ibis", e);
                if (connection != null) {
                    connection.close();
                }
            }
        }

        logger.error("cannot reach " + ibis + ", removing from pool");

        dead(ibis);
    }

    void push(Member member) {
        logger.debug("pushing entries to " + member);

        Connection connection = null;
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            try {
                int localTime = getEventTime();

                int peerTime = 0;

                if (keepNodeState) {
                    peerTime = member.getCurrentTime();

                    if (peerTime >= localTime) {
                        logger.debug("NOT pushing entries to " + member
                                + ", nothing to do");
                        return;
                    }
                }

                connection =connectionFactory.connect(member.ibis(), 
                        Protocol.OPCODE_PUSH, CONNECT_TIMEOUT);

                connection.out().writeUTF(getName());

                if (!keepNodeState) {
                    logger.debug("waiting for peer time");
                    connection.out().flush();
                    peerTime = connection.in().readInt();
                }

                logger.debug("peer time = " + peerTime);

                int sendEntries = localTime - peerTime;

                logger.debug("sending " + sendEntries + " entries");

                connection.out().writeInt(sendEntries);

                Event[] events = getEvents(peerTime, peerTime + sendEntries);
                for (int i = 0; i < events.length; i++) {

                    events[i].writeTo(connection.out());
                }

                connection.getAndCheckReply();
                if (keepNodeState) {
                    member.setCurrentTime(localTime);
                }
                connection.close();
                return;
            } catch (IOException e) {
                logger.debug("error on pushing to ibis", e);
                if (connection != null) {
                    connection.close();
                }
            }
        }
        logger.error("cannot reach " + member + ", removing from pool");

        dead(member.ibis());
    }

    private synchronized IbisIdentifier getSuspectIbis() {
        while (checkList.size() == 0 && !ended()) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        if (ended()) {
            return null;
        }

        return checkList.remove(0);
    }

    /**
     * contacts any suspect nodes when asked
     */
    public void run() {
        while (!ended()) {
            IbisIdentifier suspect = getSuspectIbis();

            if (suspect != null) {
                ping(suspect);
            }
        }
    }

    public synchronized Member[] getRandomMembers(int size) {
        return members.getRandom(size);
    }

    public synchronized Member getRandomMember() {
        return members.getRandom();
    }

    public synchronized Member getMember(int index) {
        return members.get(index);
    }
}
