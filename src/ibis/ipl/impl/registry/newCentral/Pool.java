package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.util.ThreadPool;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

final class Pool implements Runnable {

	static final int PUSH_THREADS = 10;

	private static final Logger logger = Logger.getLogger(Pool.class);

	// list of all joins, leaves, elections, etc.
	private final ArrayList<Event> events;

	private int currentEventTime;

	private int minEventTime;

	// cache of election results we've seen. Optimization over searching all the
	// events each time an election result is requested.
	// map<election name, winner>
	private final Map<String, IbisIdentifier> elections;

	private final MemberSet members;

	// List of "suspect" ibisses we must ping
	private final Set<IbisIdentifier> checkList;

	private final String name;

	private final ConnectionFactory connectionFactory;
	
	private final long checkupInterval;

	private final boolean printEvents;

	private int nextID;

	private long nextSequenceNr;

	private boolean ended = false;

	
	Pool(String name, ConnectionFactory connectionFactory,
			long checkupInterval, boolean gossip, long gossipInterval,
			boolean adaptGossipInterval, boolean tree, boolean printEvents) {
		this.name = name;
		this.connectionFactory = connectionFactory;
		this.checkupInterval = checkupInterval;
		this.printEvents = printEvents;

		currentEventTime = 0;
		minEventTime = 0;
		nextID = 0;
		nextSequenceNr = 0;

		events = new ArrayList<Event>();
		elections = new HashMap<String, IbisIdentifier>();
		members = new MemberSet();
		checkList = new LinkedHashSet<IbisIdentifier>();

		if (gossip) {
			// TODO: do something :)
			throw new Error("gossip not working");
		} else if (tree) {
			throw new Error("tree not implemented");
		} else { // central
			new IterativeEventPusher(this, PUSH_THREADS, checkupInterval, true);
		}

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

	synchronized void waitForEventTime(int time, long deadline) {
		while (getEventTime() < time) {
			if (ended()) {
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

		members.add(identifier);

		if (printEvents) {
			System.out.println("Central Registry: " + identifier
					+ " joined pool \"" + name + "\" now " + members.size()
					+ " members");
		}

		addEvent(Event.JOIN, null, identifier);

		return identifier;
	}

	void writeBootstrapList(DataOutputStream out) throws IOException {

		IbisIdentifier[] peers = getRandomMembers(Protocol.BOOTSTRAP_LIST_SIZE);

		out.writeInt(peers.length);
		for (IbisIdentifier member : peers) {
			member.writeTo(out);
		}

	}

	public void writeState(DataOutputStream out) throws IOException {
		int time;
		List<IbisIdentifier> membersList;
		ArrayList<String> electionKeys = new ArrayList<String>();
		ArrayList<IbisIdentifier> electionValues = new ArrayList<IbisIdentifier>();

		// copy state
		synchronized (this) {
			time = getEventTime();
			membersList = members.asList();

			for (Map.Entry<String, IbisIdentifier> entry : elections.entrySet()) {
				electionKeys.add(entry.getKey());
				electionValues.add(entry.getValue());
			}
		}

		// write state

		out.writeInt(membersList.size());
		for (IbisIdentifier ibis : membersList) {
			ibis.writeTo(out);
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
		if (!members.remove(identifier.getID())) {
			logger.error("unknown ibis " + identifier + " tried to leave");
			throw new Exception("ibis unknown: " + identifier);
		}
		if (printEvents) {
			System.out.println("Central Registry: " + identifier
					+ " left pool \"" + name + "\" now " + members.size()
					+ " members");
		}

		addEvent(Event.LEAVE, null, identifier);

		Iterator<Entry<String, IbisIdentifier>> iterator = elections.entrySet()
				.iterator();
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
		if (!members.remove(identifier.getID())) {
			return;
		}
		if (printEvents) {
			System.out.println("Central Registry: " + identifier
					+ " died in pool \"" + name + "\" now " + members.size()
					+ " members, Error:");
			exception.printStackTrace(System.out);
		}

		addEvent(Event.DIED, null, identifier);

		Iterator<Map.Entry<String, IbisIdentifier>> iterator = elections
				.entrySet().iterator();
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
			logger.error(
					"client needs events we already removed from the history!");
			return null;
		}

		if (events.isEmpty()) {
			return new Event[0];
		}

		int eventHistoryOffset = events.get(0).getTime();

		int startIndex = startTime - eventHistoryOffset;

		if (startIndex < 0) {
			logger.error("trying to get events from befor the start of the event list");
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
			if (members.contains(victim.getID())) {
				result.add(victim);
			}
		}
		addEvent(Event.SIGNAL, signal, result.toArray(new IbisIdentifier[result
				.size()]));
		notifyAll();

	}

	void push(IbisIdentifier member) {
		if (ended()) {
			return;
		}
		if (!isMember(member)) {
			return;
		}
		logger.debug("pushing entries to " + member);

		long currentTime = System.currentTimeMillis();
		
		//don't wait any longer than the checkup interval
		long deadline = currentTime + checkupInterval;

		Exception exception = null;
		while (currentTime < deadline) {
			Connection connection = null;
			try {

				int peerTime = 0;

				logger.debug("creating connection to push events to " + member);

				int timeout = (int) (deadline - currentTime);
				
				if (timeout <= 0) {
					throw new IOException("timeout negative value");
				}
				logger.debug("timeout = " + timeout);
				
				connection = connectionFactory.connect(member,
						timeout , true);

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

				logger.debug("sending " + events.length + " entries to "
						+ member);

				connection.out().writeInt(events.length);

				for (int i = 0; i < events.length; i++) {

					events[i].writeTo(connection.out());
				}

				connection.out().writeInt(getMinEventTime());

				connection.getAndCheckReply();
				connection.close();

				logger.debug("connection to " + member + " closed");
				return;

			} catch (IOException e) {
				logger.debug("cannot reach " + member, e);
				exception = e;
			} finally {
				if (connection != null) {
					connection.close();
				}
			}
			currentTime = System.currentTimeMillis();
		}
		// deadline expired, declare member dead (with last exception)
		dead(member, exception);
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
			if (members.contains(result.getID())) {
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
				push(suspect);
			}
		}
	}

	synchronized IbisIdentifier[] getRandomMembers(int size) {
		return members.getRandom(size);
	}

	synchronized IbisIdentifier getRandomMember() {
		return members.getRandom();
	}

	synchronized boolean isMember(IbisIdentifier ibis) {
		return members.contains(ibis.getID());
	}

	synchronized IbisIdentifier getMember(int index) {
		return members.get(index);
	}

	synchronized List<IbisIdentifier> getMemberList() {
		return members.asList();
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

}
