package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.impl.IbisIdentifier;

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

	private boolean initialized;

	private int time;

	private final Random random;

	RegistryState(Registry registry) {
		this.registry = registry;

		ibises = new ArrayList<IbisIdentifier>();
		elections = new HashMap<String, IbisIdentifier>();
		pendingEvents = new TreeSet<Event>();
		eventHistory = new ArrayList<Event>();

		random = new Random();

		time = 0;
		initialized = false;
	}

	synchronized void bootstrap(ArrayList<IbisIdentifier> ibises,
			Map<String, IbisIdentifier> elections, int time) {
		if (initialized) {
			logger.error("Tried to initialize registry state twice");
			return;
		}

		// copy over state

		this.time = time;

		this.ibises.clear();
		this.ibises.addAll(ibises);

		this.elections.clear();
		this.elections.putAll(elections);

		// generate events for already joined Ibises (in order)
		SortedSet<IbisIdentifier> sortedIbises = new TreeSet<IbisIdentifier>();
		sortedIbises.addAll(this.ibises);

		for (IbisIdentifier ibis : sortedIbises) {
			registry.handleEvent(new Event(-1, Event.JOIN, null, ibis));
		}

		// generate events for elections
		for (Map.Entry<String, IbisIdentifier> election : this.elections
				.entrySet()) {
			registry.handleEvent(new Event(-1, Event.ELECT,election.getKey(),
					election.getValue()));
		}

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
	}

	private synchronized void ibisLeft(IbisIdentifier ibis) {
		logger.debug(ibis + " left our pool");

		for (int i = 0; i < ibises.size(); i++) {
			if (ibises.get(i).equals(ibis)) {
				ibises.remove(i);
				return;
			}
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
				// NOT HANDLED HERE
				break;
			case Event.ELECT:
				newElectionResult(event.getDescription(), event.getFirstIbis());
			case Event.UN_ELECT:
				unElect(event.getDescription());
				break;
			default:
				logger.error("unknown event type: " + event.getType());
			}

			// also push event to registry
			registry.handleEvent(event);
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
				// deadline expired
				return null;
			}

			try {
				wait(timeRemaining);
			} catch (InterruptedException e) {
				// IGNORE
			}
			result = elections.get(election);
		}
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
}
