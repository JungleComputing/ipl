package ibis.ipl.registry.central.client;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.registry.central.Election;
import ibis.ipl.registry.central.ElectionSet;
import ibis.ipl.registry.central.Event;
import ibis.ipl.registry.central.EventList;
import ibis.ipl.registry.central.ListMemberSet;
import ibis.ipl.registry.central.Member;
import ibis.ipl.registry.central.MemberSet;
import ibis.ipl.registry.central.RegistryProperties;
import ibis.ipl.registry.central.TreeMemberSet;
import ibis.ipl.registry.statistics.Statistics;
import ibis.util.TypedProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Pool {

    private static final Logger logger = LoggerFactory.getLogger(Pool.class);

    private final String poolName;

    private final boolean closedWorld;

    private final int size;

    private final long heartbeatInterval;

    private final MemberSet members;

    private final ElectionSet elections;

    private final EventList eventList;

    private final Registry registry;

    private final Statistics statistics;

    private boolean initialized;

    private boolean closed;

    private boolean stopped;
    
    private boolean terminated;
    
    private Event closeEvent;
    
    private Event terminateEvent;

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
        closeEvent = null;

        terminated = false;
        terminateEvent = null;
        
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

        heartbeatInterval = properties
                .getIntProperty(RegistryProperties.HEARTBEAT_INTERVAL) * 1000;

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

    public synchronized boolean mustReportMaybeDead(
            ibis.ipl.IbisIdentifier ibisIdentifier) {
        Member member = members.get(ibisIdentifier.name());

        if (member == null) {
            if (logger.isDebugEnabled()) {
        	logger.debug("user reporting ibis " + ibisIdentifier
        		+ "  which is not in pool, not reporting");
            }
            return false;
        }

        if (member.getTime() > (System.currentTimeMillis() - heartbeatInterval)) {
            if (logger.isDebugEnabled()) {
        	logger.debug("user reporting member " + ibisIdentifier
        		+ "  recently reported already, skipping this time");
            }
            return false;
        }

        member.updateTime();
        return true;
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
            logger
                    .error("EEP! we are asked to purge the history of events we still need. Our time =  "
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

        DataInputStream in = new DataInputStream(
                new ByteArrayInputStream(bytes));

        long read = System.currentTimeMillis();

        // we have all the data in the array now, read from that...
        synchronized (this) {
            long locked = System.currentTimeMillis();

            if (initialized) {
                // already initialized, ignore
                return;
            }

            if (logger.isDebugEnabled()) {
        	logger.debug("reading bootstrap state");
            }

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
            if (closed) {
            	closeEvent = new Event(in);
            }
            
            terminated = in.readBoolean();
            if (terminated) {
                terminateEvent = new Event(in);
            }

            // Create list of "old" events

            SortedSet<Event> events = new TreeSet<Event>();
            events.addAll(members.getJoinEvents());
            events.addAll(elections.getEvents());
            events.addAll(signals);
            if (closed) {
            	events.add(closeEvent);
            }
            if (terminated) {
            	events.add(terminateEvent);
            }

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

            if (logger.isDebugEnabled()) {
        	logger.debug("pool init, read = " + (read - start) + ", locked = "
        		+ (locked - read) + ", membersDone = "
        		+ (membersDone - locked) + ", used = "
        		+ (used - membersDone) + ", handled = " + (handled - used)
        		+ ", statted = " + (statted - handled));
            }
        }

        handleEvents();
        if (logger.isDebugEnabled()) {
            logger.debug("bootstrap complete");
        }

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
            if (closed) {
            	closeEvent.writeTo(out);
            }
            dataOut.writeBoolean(terminated);
            if (terminated) {
            	terminateEvent.writeTo(out);
            }
        }

        dataOut.flush();
        dataOut.close();
        byte[] bytes = arrayOut.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
        if (logger.isDebugEnabled()) {
            logger.debug("pool state size = " + bytes.length);
        }
    }
    
    synchronized String[] wonElections(IbisIdentifier id) {
        ArrayList<String> result = new ArrayList<String>();
        for (Election e : elections) {
            if (e.getWinner().equals(id)) {
                result.add(e.getName());
            }
        }
        return result.toArray(new String[result.size()]);
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
        	if (logger.isDebugEnabled()) {
        	    logger.debug("getElectionResult deadline expired");
        	}
                return null;
            }

            try {
                if (timeRemaining > 1000) {
                    timeRemaining = 1000;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("waiting " + timeRemaining + " for election");
                }
                wait(timeRemaining);
                if (logger.isDebugEnabled()) {
                    logger.debug("DONE waiting " + timeRemaining + " for election");
                }
            } catch (final InterruptedException e) {
                // IGNORE
            }
            result = elections.get(election);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("getElection result = " + result);
        }
        return result.getWinner();
    }

    private synchronized void handleEvent(Event event) {
	if (logger.isDebugEnabled()) {
	    logger.debug("handling event: " + event);
	}

        switch (event.getType()) {
        case Event.JOIN:
            members.add(new Member(event.getIbis(), event));
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            break;
        case Event.LEAVE:
            members.remove(event.getIbis());
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            break;
        case Event.DIED:
            IbisIdentifier died = event.getIbis();
            members.remove(died);
            if (statistics != null) {
                statistics.newPoolSize(members.size());
            }
            if (died.equals(registry.getIbisIdentifier())) {
        	if (logger.isDebugEnabled()) {
        	    logger.debug("we were declared dead");
        	}
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
            closeEvent = event;
            break;
        case Event.POOL_TERMINATED:
            terminated = true;
            terminateEvent = event;
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

    synchronized boolean isClosed() {
        if (!closedWorld) {
            throw new IbisConfigurationException("isClosed() called but not "
                    + "closed world");
        }

        return closed;
    }

    synchronized void waitUntilPoolClosed() {
        if (!closedWorld) {
            throw new IbisConfigurationException(
                    "waitUntilPoolClosed() called but not " + "closed world");
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
	if (logger.isDebugEnabled()) {
	    logger.debug("handling events");
	}
        if (!isInitialized()) {
            if (logger.isDebugEnabled()) {
        	logger.debug("handle events: not initialized yet");
            }
            return;
        }

        while (true) {
            // Modified the code below to do getEvent and time++ within
            // one synchronized block, otherwise race condition.

            Event event;

            synchronized (this) {
                event = getEvent();

                if (event == null) {
                    if (logger.isDebugEnabled()) {
                	logger.debug("done handling events, event time now: "
                                    + time);
                    }
                    return;
                }
                handleEvent(event);
                time++;
                notifyAll();
                registry.handleEvent(event);
            }
        }
    }

    public synchronized Member[] getChildren() {
        return members.getChildren(registry.getIbisIdentifier());
    }

    synchronized boolean hasTerminated() {
        return terminated;
    }

    synchronized ibis.ipl.IbisIdentifier waitUntilTerminated() {
        while (!(terminated || stopped)) {
            try {
                wait();
            } catch (final InterruptedException e) {
                // IGNORE
            }
        }
        
        return terminateEvent.getIbis();
    }

}
