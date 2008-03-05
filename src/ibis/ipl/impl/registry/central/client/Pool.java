package ibis.ipl.impl.registry.central.client;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.central.Election;
import ibis.ipl.impl.registry.central.ElectionSet;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.EventList;
import ibis.ipl.impl.registry.central.ListMemberSet;
import ibis.ipl.impl.registry.central.Member;
import ibis.ipl.impl.registry.central.MemberSet;
import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.ipl.impl.registry.central.TreeMemberSet;
import ibis.ipl.impl.registry.statistics.Statistics;
import ibis.util.TypedProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

final class Pool {

    private static final Logger logger = Logger.getLogger(Pool.class);

    private final String poolName;

    private final boolean closedWorld;

    private final int size;

    private final MemberSet members;

    private final ElectionSet elections;

    private final EventList eventList;

    private final Registry registry;

    private final Statistics statistics;

    private boolean initialized;

    private boolean closed;

    private boolean stopped;

    private int time;

    Pool(IbisCapabilities capabilities, TypedProperties properties,
            Registry registry, Statistics statistics) {
        this.registry = registry;

        this.statistics = statistics;
        if (statistics != null) {
            statistics.newPoolSize(0);
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

    synchronized int getNextRequiredEvent() {
        return eventList.getNextRequiredEvent();
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

    synchronized boolean isMember(IbisIdentifier ibis) {
        return members.contains(ibis);
    }

    // new incoming events
    void newEventsReceived(Event[] events) {
        synchronized (this) {
            eventList.add(events);
        }
        handleEvents();
    }

    synchronized void purgeHistoryUpto(int time) {
        if (this.time != -1 && this.time < time) {
            logger.error("EEP! we are asked to purge the history of events we still need. Our time =  "
                    + this.time + " purge time = " + time);
            return;
        }

        eventList.setMinimum(time);
    }

    void init(DataInputStream stream) throws IOException {
        long start = System.currentTimeMillis();
        // copy over data first so we are not blocked while reading data
        byte[] bytes = new byte[stream.readInt()];
        stream.readFully(bytes);

        DataInputStream in =
            new DataInputStream(new ByteArrayInputStream(bytes));

        long read = System.currentTimeMillis();

        // we have all the data in the array now, read from that...
        synchronized (this) {
            long locked = System.currentTimeMillis();

            if (initialized) {
                //already initialized, ignore
                return;
            }

            logger.debug("reading bootstrap state");

            time = in.readInt();

            members.init(in);

            long membersDone = System.currentTimeMillis();

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

            // Create list of "old" events

            SortedSet<Event> events = new TreeSet<Event>();
            events.addAll(members.getJoinEvents());
            events.addAll(elections.getEvents());
            events.addAll(signals);

            long used = System.currentTimeMillis();

            // pass old events to the registry
            // CALLS REGISTRY WHILE POOL IS LOCKED!
            for (Event event : events) {
                registry.handleEvent(event);
            }

            long handled = System.currentTimeMillis();

            initialized = true;
            notifyAll();

            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            long statted = System.currentTimeMillis();

            logger.info("pool init, read = " + (read - start) + ", locked = "
                    + (locked - read) + ", membersDone = "
                    + (membersDone - locked) + ", used = "
                    + (used - membersDone) + ", handled = " + (handled - used)
                    + ", statted = " + (statted - handled));
        }

        handleEvents();
        logger.debug("bootstrap complete");

    }

    void writeState(DataOutputStream out, int joinTime) throws IOException {
        ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(arrayOut);

        synchronized (this) {
            if (!initialized) {
                throw new IOException("state not initialized yet");
            }

            dataOut.writeInt(time);

            members.writeTo(dataOut);
            elections.writeTo(dataOut);

            Event[] signals = eventList.getSignalEvents(joinTime, time);
            dataOut.writeInt(signals.length);
            for (Event event : signals) {
                event.writeTo(dataOut);
            }

            dataOut.writeBoolean(closed);
        }

        dataOut.flush();
        dataOut.close();
        byte[] bytes = arrayOut.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
        logger.debug("pool state size = " + bytes.length);
    }

    synchronized IbisIdentifier getElectionResult(String election, long timeout)
            throws IOException {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        }

        Election result = elections.get(election);

        while (result == null) {
            long timeRemaining = deadline - System.currentTimeMillis();

            if (timeRemaining <= 0) {
                logger.debug("getElectionResullt deadline expired");
                return null;
            }

            try {
                if (timeRemaining > 1000) {
                    timeRemaining = 1000;
                }
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
        logger.debug("handling event: " + event);

        switch (event.getType()) {
        case Event.JOIN:
            members.add(new Member(event.getFirstIbis(), event));
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            break;
        case Event.LEAVE:
            members.remove(event.getFirstIbis());
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            break;
        case Event.DIED:
            IbisIdentifier died = event.getFirstIbis();
            members.remove(died);
            if (statistics != null) {
                statistics.newPoolSize(members.size());
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
                statistics.electionEvent();
            }
            break;
        case Event.UN_ELECT:
            elections.remove(event.getDescription());
            if (statistics != null) {
                statistics.electionEvent();
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

        if (logger.isDebugEnabled()) {
            logger.debug("member list now: " + members);
        }
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

    synchronized void waitForEventTime(int time, long timeout) {
        long deadline = System.currentTimeMillis() + timeout;

        if (timeout == 0) {
            deadline = Long.MAX_VALUE;
        }

        while (getTime() < time) {
            if (stopped) {
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

    synchronized void stop() {
        stopped = true;
        notifyAll();

        if (statistics != null) {
            statistics.newPoolSize(0);
            statistics.write();
        }
    }

    private synchronized Event getEvent() {
        return eventList.get(time);
    }

    /**
     * Handles incoming events, passes events to the registry
     */
    private void handleEvents() {
        logger.info("handling events");
        if (!isInitialized()) {
            logger.info("handle events: not initialized yet");
            return;
        }

        while (true) {
            // Modified the code below to do getEvent and time++ within
            // one synchronized block, otherwise race condition.

            Event event;

            synchronized (this) {
                event = getEvent();

                if (event == null) {
                    logger.info("done handling events, event time now: "
                            + time);
                    return;
                }
                handleEvent(event);
                time++;
                notifyAll();
                // Niels: does having this outside the synchronized block not
                // allow for a change in the order in which the user sees the
                // events? (Ceriel)
                // TODO: check this!!!
                registry.handleEvent(event);
            }
        }
    }

    public synchronized Member[] getChildren() {
        return members.getChildren(registry.getIbisIdentifier());
    }

}
