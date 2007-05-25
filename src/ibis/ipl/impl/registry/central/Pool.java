package ibis.ipl.impl.registry.central;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
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

    static final int MAX_TRIES = 5;

    static final int PING_THREADS = 5;

    static final long GOSSIP_INTERVAL = 120 * 60 * 1000;

    static final int GOSSIP_THREADS = 10;

    static final int PUSH_THREADS = 10;

    private static final Logger logger = Logger.getLogger(Pool.class);

    // list of all joins, leaves, elections, etc.
    private final ArrayList<Event> events;

    // cache of election results we've seen. Optimization over searching all the
    // events each time an election result is requested.
    // map<election name, winner>
    private final Map<String, IbisIdentifier> elections;

    private final MemberSet members;

    // List of "suspect" ibisses we must ping
    private final Set<IbisIdentifier> checkList;

    private final boolean keepNodeState;

    private final String name;

    private final ConnectionFactory connectionFactory;

    private final boolean printEvents;

    private int nextID;

    private long nextSequenceNr;

    private boolean ended = false;

    Pool(String name, ConnectionFactory connectionFactory, boolean gossip,
            boolean keepNodeState, long pingInterval, boolean printEvents) {
        this.name = name;
        this.connectionFactory = connectionFactory;
        this.keepNodeState = keepNodeState;
        this.printEvents = printEvents;

        nextID = 0;
        nextSequenceNr = 0;

        events = new ArrayList<Event>();
        elections = new HashMap<String, IbisIdentifier>();
        members = new MemberSet();
        checkList = new LinkedHashSet<IbisIdentifier>();

        if (gossip) {
            // ping iteratively
            new PeriodicNodeContactor(this, false, false, pingInterval,
                    PING_THREADS);
            // gossip randomly
            new PeriodicNodeContactor(this, true, true, GOSSIP_INTERVAL,
                    GOSSIP_THREADS);
        } else { // central
            // ping iteratively
            new PeriodicNodeContactor(this, false, false, pingInterval,
                    PING_THREADS);

            new EventPusher(this, PUSH_THREADS);
        }

        ThreadPool.createNew(this, "pool management thread");

    }

    synchronized int getEventTime() {
        return events.size();
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

        IbisIdentifier identifier = new IbisIdentifier(id, implementationData,
                clientAddress, location, name);

        members.add(new Member(identifier));

        if (printEvents) {
            System.out.println("Central Registry: " + identifier
                    + " joined pool \"" + name + "\" now " + members.size()
                    + " members");
        }

        events.add(new Event(events.size(), Event.JOIN, identifier, null));
        notifyAll();

        return identifier;
    }

    void writeBootstrap(DataOutputStream out) throws IOException {

        Member[] peers = getRandomMembers(Protocol.BOOTSTRAP_LIST_SIZE);

        out.writeInt(peers.length);
        for (Member member : peers) {
            member.ibis().writeTo(out);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ibis.ipl.impl.registry.central.SuperPool#leave(ibis.ipl.impl.IbisIdentifier)
     */
    synchronized void leave(IbisIdentifier identifier) throws Exception {
        if (!members.remove(identifier.myId)) {
            logger.error("unknown ibis " + identifier + " tried to leave");
            throw new Exception("ibis unknown: " + identifier);
        }
        if (printEvents) {
            System.out.println("Central Registry: " + identifier
                    + " left pool \"" + name + "\" now " + members.size()
                    + " members");
        }

        events.add(new Event(events.size(), Event.LEAVE, identifier, null));
        notifyAll();

        Iterator<Entry<String, IbisIdentifier>> iterator = elections.entrySet()
                .iterator();
        while (iterator.hasNext()) {
            Entry<String, IbisIdentifier> entry = iterator.next();
            if (entry.getValue().equals(identifier)) {
                iterator.remove();

                events.add(new Event(events.size(), Event.UN_ELECT, identifier,
                        entry.getKey()));
                notifyAll();

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
    synchronized void dead(IbisIdentifier identifier) {
        if (!members.remove(identifier.myId)) {
            return;
        }
        if (printEvents) {
            System.out.println("Central Registry: " + identifier
                    + " died in pool \"" + name + "\" now " + members.size()
                    + " members");
        }

        events.add(new Event(events.size(), Event.DIED, identifier, null));
        notifyAll();

        Iterator<Map.Entry<String, IbisIdentifier>> iterator = elections
                .entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, IbisIdentifier> entry = iterator.next();
            if (entry.getValue().equals(identifier)) {
                iterator.remove();

                events.add(new Event(events.size(), Event.UN_ELECT, identifier,
                        entry.getKey()));
                notifyAll();

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
    }

    synchronized Event[] getEvents(int startTime, int maxSize) {
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

            events.add(new Event(events.size(), Event.ELECT, winner, election));
            notifyAll();

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
        // add to todo list :)
        checkList.add(identifier);
        notifyAll();
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
            if (members.contains(victim.myId)) {
                result.add(victim);
            }
        }
        events.add(new Event(events.size(), Event.SIGNAL, result
                .toArray(new IbisIdentifier[result.size()]), signal));
        notifyAll();

    }

    void ping(IbisIdentifier ibis) {
        logger.debug("pinging " + ibis);
        for (int i = 0; i < MAX_TRIES; i++) {
            Connection connection = null;
            try {

                logger.debug("creating connection");
                connection = connectionFactory.connect(ibis,
                        Protocol.OPCODE_PING, false);
                logger.debug("connection created, checking for reply");

                // get reply
                connection.getAndCheckReply();

                IbisIdentifier result = new IbisIdentifier(connection.in());

                connection.close();
                
                if (result.equals(ibis)) {
                    logger.debug("ping succeeded");
                    return;
                } else {
                    logger.debug("ping ended up at wrong ibis");
                }
            } catch (Exception e) {
                logger.debug("error on pinging ibis", e);
                if (connection != null) {
                    connection.close();
                }
            }
        }

        logger.debug("cannot reach " + ibis + ", removing from pool");

        dead(ibis);
    }

    void push(Member member) {
        if (ended()) {
            return;
        }
        logger.debug("pushing entries to " + member);

        Connection connection = null;
        for (int tries = 0; tries < MAX_TRIES; tries++) {
            try {

                int peerTime = 0;

                if (keepNodeState) {
                    peerTime = member.getCurrentTime();

                    int localTime = getEventTime();

                    if (peerTime >= localTime) {
                        logger.debug("NOT pushing entries to " + member
                                + ", nothing to do");
                        return;
                    }
                }

                logger.debug("creating connection to push events to " + member.ibis());
                
                connection = connectionFactory.connect(member.ibis(),
                        Protocol.OPCODE_PUSH, false);
                
                logger.debug("connection created");

                connection.out().writeUTF(getName());
                connection.out().flush();

                if (!keepNodeState) {
                    logger.debug("waiting for peer time");
                    peerTime = connection.in().readInt();
                }

                int localTime = getEventTime();

                logger.debug("peer time = " + peerTime + ", localtime = "
                        + localTime);

                int sendEntries = localTime - peerTime;
                
                if (sendEntries < 0) {
                    logger.debug("sendEntries " + sendEntries + " is negative, not sending events"); 
                    sendEntries = 0;
                }

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
        logger.debug("cannot reach " + member + ", removing from pool");

        dead(member.ibis());
    }

    private synchronized IbisIdentifier getSuspectMember() {
        while (true) {

            // wait for the list to become non-empty
            while (checkList.size() == 0 && !ended()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }

            // return if this pool ended anyway
            if (ended()) {
                return null;
            }

            IbisIdentifier result = checkList.iterator().next();
            checkList.remove(result);

            // return if still in pool
            if (members.contains(result.myId)) {
                return result;
            }
        }
    }

    /**
     * contacts any suspect nodes when asked
     */
    public void run() {
        while (!ended()) {
            IbisIdentifier suspect = getSuspectMember();

            if (suspect != null) {
                ping(suspect);
            }
        }
    }

    synchronized Member[] getRandomMembers(int size) {
        return members.getRandom(size);
    }

    synchronized Member getRandomMember() {
        return members.getRandom();
    }

    synchronized Member getMember(int index) {
        return members.get(index);
    }

    public String toString() {
        return "Pool " + name + ": size = " + getSize() + ", event time = "
                + getEventTime();
    }

}
