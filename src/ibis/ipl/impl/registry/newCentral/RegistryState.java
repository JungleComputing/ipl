package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.impl.IbisIdentifier;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

class RegistryState {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(RegistryState.class);

	private final Registry registry;

	private final ArrayList<IbisIdentifier> ibises;

	private final Map<String, IbisIdentifier> elections;

	private final SortedSet<Event> pendingEvents;

	private final ArrayList<Event> eventHistory;

	// date structures that the user can poll

	private final ArrayList<ibis.ipl.IbisIdentifier> joinedIbises;

	private final ArrayList<ibis.ipl.IbisIdentifier> leftIbises;

	private final ArrayList<ibis.ipl.IbisIdentifier> diedIbises;

	private final ArrayList<String> signals;

	private boolean initialized;

	private int time;

	private final Random random;

	RegistryState(Registry registry, boolean membership, boolean supportSignals) {
		this.registry = registry;

		ibises = new ArrayList<IbisIdentifier>();
		elections = new HashMap<String, IbisIdentifier>();
		pendingEvents = new TreeSet<Event>();
		eventHistory = new ArrayList<Event>();

		random = new Random();

		time = -1;
		initialized = false;

		if (membership) {
			joinedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
			leftIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
			diedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
		} else {
			joinedIbises = null;
			leftIbises = null;
			diedIbises = null;
		}

		if (supportSignals) {
			signals = new ArrayList<String>();
		} else {
			signals = null;
		}

	}

	synchronized void writeTo(DataOutput out) throws IOException {
		if (!initialized) {
			throw new IOException("state not initialized yet");
		}

		out.writeInt(ibises.size());
		for (IbisIdentifier ibis : ibises) {
			ibis.writeTo(out);
		}

		out.writeInt(elections.size());
		for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
			out.writeUTF(entry.getKey());
			entry.getValue().writeTo(out);
		}

		out.writeInt(time);
	}

	synchronized void readFrom(DataInput in) throws IOException {
		if (initialized) {
			logger.error("Tried to initialize registry state twice");
			return;
		}

		logger.debug("reading bootstrap state");

		int nrOfIbises = in.readInt();
		SortedSet<IbisIdentifier> sortedIbises = new TreeSet<IbisIdentifier>();

		for (int i = 0; i < nrOfIbises; i++) {
			sortedIbises.add(new IbisIdentifier(in));
		}

		int nrOfElections = in.readInt();

		Map<String, IbisIdentifier> elections = new HashMap<String, IbisIdentifier>();
		for (int i = 0; i < nrOfElections; i++) {
			elections.put(in.readUTF(), new IbisIdentifier(in));
		}

		time = in.readInt();

		logger.debug("read bootstrap state of time " + time);

		logger.debug("generating events for already joined Ibises (in order)");
		for (IbisIdentifier ibis : sortedIbises) {
			handleEvent(new Event(-1, Event.JOIN, null, ibis));
		}

		logger.debug("generating events for elections");
		for (Map.Entry<String, IbisIdentifier> election : this.elections
				.entrySet()) {
			handleEvent(new Event(-1, Event.ELECT, election.getKey(),
					election.getValue()));
		}

		initialized = true;

		logger.debug("bootstrap complete");

		handlePendingEvents();
	}

	// new incoming events
	synchronized void handleEvents(Event[] events) {
		// add events to the list
		pendingEvents.addAll(Arrays.asList(events));

		handlePendingEvents();
	}

	private synchronized void addIbis(IbisIdentifier newIbis) {
		logger.debug(newIbis + " joined our pool");

		for (IbisIdentifier ibis : ibises) {
			if (ibis.equals(newIbis)) {
				return;
			}
		}
		ibises.add(newIbis);

		if (joinedIbises != null) {
			joinedIbises.add(newIbis);
		}
	}

	private synchronized void ibisLeft(IbisIdentifier ibis) {
		logger.debug(ibis + " left our pool");

		for (int i = 0; i < ibises.size(); i++) {
			if (ibises.get(i).equals(ibis)) {
				ibises.remove(i);
				return;
			}
		}

		if (leftIbises != null) {
			leftIbises.add(ibis);
		}

	}

	private synchronized void ibisDied(IbisIdentifier ibis) {
		logger.debug(ibis + " died");

		for (int i = 0; i < ibises.size(); i++) {
			if (ibises.get(i).equals(ibis)) {
				ibises.remove(i);
				return;
			}
		}

		if (diedIbises != null) {
			diedIbises.add(ibis);
		}

	}

	private synchronized void newElectionResult(String name, IbisIdentifier ibis) {
		logger.debug("received winner for election \"" + name + "\" : " + ibis);

		elections.put(name, ibis);
	}

	private synchronized void unElect(String name) {
		logger.debug("unelect for election \"" + name + "\"");

		elections.remove(name);
	}

	private synchronized void handlePendingEvents() {
		if (!initialized) {
			return;
		}

		// remove head of list until we have gotten rid of all "old" events
		while (!pendingEvents.isEmpty()
				&& pendingEvents.first().getTime() < time) {
			pendingEvents.remove(pendingEvents.first());
		}

		while (!pendingEvents.isEmpty()
				&& pendingEvents.first().getTime() == time) {
			// remove from TO-DO list :)
			Event event = pendingEvents.first();
			pendingEvents.remove(event);

			logger.debug("handling event " + event);

			time++;

			// add to history
			eventHistory.add(event);

			handleEvent(event);
		}

		if (logger.isDebugEnabled()) {
			if (pendingEvents.isEmpty()) {
				logger.debug("time now: " + time
						+ ", first event not handled: null");
			} else {
				logger
						.debug("time now: " + time
								+ ", first event not handled: "
								+ pendingEvents.first());
			}
		}

		// wake up any threads waiting for (results of) events
		notifyAll();

		// assert consistency of event history
		checkConsistency();
	}

	private synchronized void handleEvent(Event event) {

		switch (event.getType()) {
		case Event.JOIN:
			for (IbisIdentifier ibis : event.getIbises()) {
				addIbis(ibis);
			}
			break;
		case Event.LEAVE:
			for (IbisIdentifier ibis : event.getIbises()) {
				ibisLeft(ibis);
			}
			break;
		case Event.DIED:
			for (IbisIdentifier ibis : event.getIbises()) {
				ibisDied(ibis);
			}
			break;
		case Event.SIGNAL:
			for (IbisIdentifier destination : event.getIbises()) {
				if (destination.equals(registry.getIbisIdentifier())) {
					logger.debug("received signal: \"" + event.getDescription()
							+ "\"");
					signals.add(event.getDescription());
				}
			}
			break;
		case Event.ELECT:
			newElectionResult(event.getDescription(), event.getFirstIbis());
		case Event.UN_ELECT:
			unElect(event.getDescription());
			break;
		default:
			logger.error("unknown event type: " + event.getType());
		}

		// also push event to user
		registry.doUpcall(event);

	}

	private synchronized void checkConsistency() {
		if (eventHistory.isEmpty()) {
			return;
		}

		int offset = eventHistory.get(0).getTime();
		for (int i = 0; i < eventHistory.size(); i++) {
			if (eventHistory.get(i).getTime() != (offset + i)) {
				logger
						.error("EEP! Registry event history not consistent! Offset = "
								+ offset
								+ " element number "
								+ i
								+ " in list has event time "
								+ eventHistory.get(i).getTime()
								+ " should be "
								+ (offset + i));
			}
		}
	}

	synchronized int getTime() {
		return time;
	}

	synchronized Event[] getEventsFrom(int fromTime) {
		if (eventHistory.isEmpty()) {
			return new Event[0];
		}

		int eventHistoryOffset = eventHistory.get(0).getTime();

		int startIndex = fromTime - eventHistoryOffset;

		if (startIndex < 0) {
			return new Event[0];
		}

		// return the requested portion of the list
		return eventHistory.subList(startIndex, eventHistory.size()).toArray(
				new Event[0]);
	}

	synchronized IbisIdentifier getElectionResult(String election, long timeout) {
		long deadline = System.currentTimeMillis() + timeout;

		IbisIdentifier result = elections.get(election);

		while (result == null) {
			long timeRemaining = deadline - System.currentTimeMillis();

			if (timeRemaining <= 0) {
				logger.debug("getElectionResullt deadline expired");
				return null;
			}

			try {
				logger.debug("waiting " + timeRemaining);
				wait(timeRemaining);
				logger.debug("DONE waiting " + timeRemaining);
			} catch (InterruptedException e) {
				// IGNORE
			}
			result = elections.get(election);
		}
		logger.debug("getElection result = " + result);
		return result;
	}

	synchronized int nrOfIbisses() {
		return ibises.size();
	}

	synchronized void waitForNrOfIbisses(int numInstances) {
		while (nrOfIbisses() < numInstances) {
			try {
				wait();
			} catch (InterruptedException e) {
				// IGNORE
			}
		}

	}

	synchronized IbisIdentifier getRandomMember() {
		if (ibises.isEmpty()) {
			return null;
		}

		return ibises.get(random.nextInt(ibises.size()));
	}

	synchronized void purgeHistoryUpto(int time) {
		logger.debug("purging history upto " + time);
		while (!eventHistory.isEmpty() && eventHistory.get(0).getTime() < time) {
			if (logger.isDebugEnabled()) {
				logger.debug("removing event: " + eventHistory.get(0));
			}
			eventHistory.remove(0);
		}
	}

	synchronized boolean isInitialized() {
		return initialized;
	}

	public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
		if (joinedIbises == null) {
			throw new IbisConfigurationException(
					"Resize downcalls not configured");
		}
		ibis.ipl.IbisIdentifier[] retval = joinedIbises
				.toArray(new ibis.ipl.IbisIdentifier[joinedIbises.size()]);
		joinedIbises.clear();
		return retval;
	}

	public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
		if (leftIbises == null) {
			throw new IbisConfigurationException(
					"Resize downcalls not configured");
		}
		ibis.ipl.IbisIdentifier[] retval = leftIbises
				.toArray(new ibis.ipl.IbisIdentifier[leftIbises.size()]);
		leftIbises.clear();
		return retval;
	}

	public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
		if (diedIbises == null) {
			throw new IbisConfigurationException(
					"Resize downcalls not configured");
		}
		ibis.ipl.IbisIdentifier[] retval = diedIbises
				.toArray(new ibis.ipl.IbisIdentifier[diedIbises.size()]);
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
}
