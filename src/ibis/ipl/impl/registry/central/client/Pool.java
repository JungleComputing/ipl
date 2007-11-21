package ibis.ipl.impl.registry.central.client;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.PoolStatistics;
import ibis.ipl.impl.registry.central.Election;
import ibis.ipl.impl.registry.central.ElectionSet;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.EventList;
import ibis.ipl.impl.registry.central.ListMemberSet;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.MemberSet;
import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.ipl.impl.registry.central.TreeMemberSet;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

final class Pool implements Runnable {

    private static final Logger logger = Logger.getLogger(Pool.class);

    private final String poolName;

    private final boolean closedWorld;

    private final int size;

    private final MemberSet members;

    private final ElectionSet elections;

    private final EventList eventList;

    private final Registry registry;

    private final PoolStatistics statistics;

    private boolean initialized;

    private boolean closed;

    private boolean stopped;

    private int time;

    Pool(IbisCapabilities capabilities, TypedProperties properties,
            Registry registry) {
        this.registry = registry;

        if (properties.getBooleanProperty(RegistryProperties.STATISTICS)) {
            statistics = new PoolStatistics();
        } else {
            statistics = null;
        }

        if (properties.getBooleanProperty(RegistryProperties.TREE)) {
            members = new TreeMemberSet();
        } else {
            members = new ListMemberSet();
        }

        elections = new ElectionSet();
        eventList = new EventList();

        time = -1;
        initialized = false;
        closed = false;
        stopped = false;

        // get the pool ....
        poolName = properties.getProperty(IbisProperties.POOL_NAME);
        if (poolName == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.POOL_NAME + " is not specified");
        }

        closedWorld = capabilities.hasCapability(IbisCapabilities.CLOSED_WORLD);

        if (closedWorld) {
            try {
                size = properties.getIntProperty(IbisProperties.POOL_SIZE);
            } catch (final NumberFormatException e) {
                throw new IbisConfigurationException(
                        "could not start registry for a closed world ibis, "
                                + "required property: "
                                + IbisProperties.POOL_SIZE + " undefined", e);
            }
        } else {
            size = -1;
        }

    }

    synchronized Event[] getEventsFrom(int start) {
        return eventList.getList(start);
    }

    String getName() {
        return poolName;
    }

    boolean isClosedWorld() {
        return closedWorld;
    }

    int getSize() {
        return size;
    }

    synchronized Member getRandomMember() {
        return members.getRandom();
    }

    synchronized int getTime() {
        return time;
    }

    synchronized void setTime(int time) {
        this.time = time;
    }

    synchronized boolean isInitialized() {
        return initialized;
    }

    boolean isStopped() {
        return stopped;
    }

    synchronized boolean isMember(IbisIdentifier ibis) {
        return members.contains(ibis);
    }

    // new incoming events
    synchronized void newEventsReceived(Event[] events) {
        eventList.add(events);
        notifyAll();
    }

    synchronized void purgeHistoryUpto(int time) {
        eventList.purgeUpto(time);
    }

    public PoolStatistics getStatistics() {
        return statistics;
    }

    synchronized void init(DataInputStream in) throws IOException {
        if (initialized) {
            logger.error("Tried to initialize registry state twice");
            return;
        }

        logger.debug("reading bootstrap state");

        members.init(in);
        elections.init(in);
        int nrOfSignals = in.readInt();
        if (nrOfSignals < 0) {
            throw new IOException("negative number of signals");
        }

        ArrayList<Event> signals = new ArrayList<Event>();
        for (int i = 0; i < nrOfSignals; i++) {
            signals.add(new Event(in));
        }

        closed = in.readBoolean();
        time = in.readInt();

        // Create list of "old" events

        SortedSet<Event> events = new TreeSet<Event>();
        events.addAll(members.getJoinEvents());
        events.addAll(elections.getEvents());
        events.addAll(signals);

        // pass old events to the registry
        for (Event event : events) {
            registry.handleEvent(event);
        }

        initialized = true;
        notifyAll();

        logger.debug("bootstrap complete");

        ThreadPool.createNew(this, "pool event generator");
    }

    synchronized void writeState(DataOutputStream out, int joinTime)
            throws IOException {
        if (!initialized) {
            throw new IOException("state not initialized yet");
        }

        members.writeTo(out);
        elections.writeTo(out);

        Event[] signals = eventList.getSignalEvents(joinTime, time);
        out.writeInt(signals.length);
        for (Event event : signals) {
            event.writeTo(out);
        }

        out.writeBoolean(closed);
        out.writeInt(time);

    }

    synchronized IbisIdentifier getElectionResult(String election, long timeout)
            throws IOException {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        }

        Election result = elections.get(election);

        while (result == null) {
            final long timeRemaining = deadline - System.currentTimeMillis();

            if (timeRemaining <= 0) {
                logger.debug("getElectionResullt deadline expired");
                return null;
            }

            try {
                logger.debug("waiting " + timeRemaining + " for election");
                wait(timeRemaining);
                logger.debug("DONE waiting " + timeRemaining + " for election");
            } catch (final InterruptedException e) {
                // IGNORE
            }
            result = elections.get(election);
        }
        logger.debug("getElection result = " + result);
        return result.getWinner();
    }

    private synchronized void handleEvent(Event event) {
        switch (event.getType()) {
        case Event.JOIN:
            members.add(new Member(event.getFirstIbis(), event));
            if (statistics != null) {
            statistics.ibisJoined();
            }
            break;
        case Event.LEAVE:
            members.remove(event.getFirstIbis());
            if (statistics != null) {
            statistics.ibisLeft();
            }
            break;
        case Event.DIED:
            IbisIdentifier died = event.getFirstIbis();
            members.remove(died);
            if (statistics != null) {
            statistics.ibisDied();
            }
            if (died.equals(registry.getIbisIdentifier())) {
                logger.debug("we were declared dead");
                stop();
            }
            break;
        case Event.SIGNAL:
            // Not handled here
            break;
        case Event.ELECT:
            elections.put(new Election(event));
            if (statistics != null) {
            statistics.newElection();
            }
            break;
        case Event.UN_ELECT:
            elections.remove(event.getDescription());
            if (statistics != null) {
            statistics.unElect();
            }
            break;
        case Event.POOL_CLOSED:
            closed = true;
            break;
        default:
            logger.error("unknown event type: " + event.getType());
        }

        // wake up threads waiting for events
        notifyAll();
    }

    synchronized void waitUntilPoolClosed() {
        if (!closedWorld) {
            throw new IbisConfigurationException("waitForAll() called but not "
                    + "closed world");
        }

        while (!(closed || stopped)) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // IGNORE
            }
        }
    }

    public synchronized void waitForEventTime(int time) {
        while (!(getTime() >= time || stopped)) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // IGNORE
            }
        }
    }

    synchronized void stop() {
        stopped = true;
        notifyAll();
    }

    /**
     * Handles incoming events, passes events to the registry
     */
    public synchronized void run() {
        while (!(initialized || stopped)) {
            try {
                wait();
            } catch (InterruptedException e) {
                // IGNORE
            }
        }

        while (true) {
            Event event = eventList.get(time);

            while (event == null && !stopped) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // IGNORE
                }
                event = eventList.get(time);
            }

            if (stopped) {
                return;
            }

            if (event == null) {
                logger.error("could not get event!");
                continue;
            }

            handleEvent(event);
            registry.handleEvent(event);
            time++;
        }
    }

    public synchronized Member[] getChildren() {
        return members.getChildren(registry.getIbisIdentifier());
    }

}
