package ibis.ipl.impl.registry.newCentral;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Central registry.
 */
public final class Registry extends ibis.ipl.impl.Registry implements Runnable {

	private static final Logger logger = Logger.getLogger(Registry.class);

	private final ConnectionFactory connectionFactory;

	private IbisIdentifier[] bootstrapList;

	private final RegistryState state;

	private final ArrayList<ibis.ipl.IbisIdentifier> joinedIbises;

	private final ArrayList<ibis.ipl.IbisIdentifier> leftIbises;

	private final ArrayList<ibis.ipl.IbisIdentifier> diedIbises;

	private final ArrayList<String> signals;

	// A user-supplied registry handler, with join/leave upcalls.
	private final RegistryEventHandler registryHandler;

	// A thread that forwards the events to the user event handler
	private final Upcaller upcaller;

	private final IbisIdentifier identifier;

	private final String poolName;

	private final boolean closedWorld;

	private final boolean gossip;
	private final long gossipInterval;

	private final int numInstances;

	private boolean stopped = false;

	private final Server server;

	private final IbisCapabilities capabilities;

	/**
	 * Creates a Central Registry.
	 * 
	 * @param handler
	 *            registry handler to pass events to.
	 * @param userProperties
	 *            properties of this registry.
	 * @param data
	 *            Ibis implementation data to attach to the IbisIdentifier.
	 * @throws IOException
	 *             in case of trouble.
	 * @throws IbisConfigurationException
	 *             In case invalid properties were given.
	 */
	public Registry(IbisCapabilities capabilities,
			RegistryEventHandler handler, Properties userProperties, byte[] data)
			throws IbisConfigurationException, IOException,
			IbisConfigurationException {
		logger.debug("creating central registry");

		this.capabilities = capabilities;

		TypedProperties properties = RegistryProperties
				.getHardcodedProperties();
		properties.addProperties(userProperties);

		// get the pool ....
		poolName = properties.getProperty(IbisProperties.POOL_NAME);
		if (poolName == null) {
			throw new IbisConfigurationException(
					"cannot initialize registry, property "
							+ IbisProperties.POOL_NAME + " is not specified");
		}

		if (capabilities.hasCapability(IbisCapabilities.MEMBERSHIP)) {
			joinedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
			leftIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
			diedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
			signals = new ArrayList<String>();
		} else {
			joinedIbises = null;
			leftIbises = null;
			diedIbises = null;
			signals = null;
		}

		state = new RegistryState(this);

		closedWorld = capabilities.hasCapability(IbisCapabilities.CLOSEDWORLD);

		if (closedWorld) {
			try {
				numInstances = properties
						.getIntProperty(IbisProperties.POOL_SIZE);
			} catch (NumberFormatException e) {
				throw new IbisConfigurationException(
						"could not start registry for a closed world ibis, "
								+ "required property: "
								+ IbisProperties.POOL_SIZE + " undefined", e);
			}
		} else {
			numInstances = -1;
		}

		connectionFactory = new ConnectionFactory(properties);

		Server server = null;

		if (properties.getBooleanProperty(RegistryProperties.SERVER_STANDALONE)
				&& connectionFactory.serverIsLocalHost()) {
			logger.debug("automagiscally creating server");
			try {
				properties.setProperty(RegistryProperties.SERVER_PORT, Integer
						.toString(connectionFactory.getServerPort()));

				server = new Server(properties);
				logger.warn("Automagically created " + server.toString());
			} catch (Throwable t) {
				logger.debug("Could not create registry server", t);
			}
		}

		this.server = server;

		long checkerInterval = properties
				.getIntProperty(RegistryProperties.CHECKUP_INTERVAL) * 1000;
		gossip = properties.getBooleanProperty(RegistryProperties.GOSSIP);
		gossipInterval = properties
				.getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;
		boolean adaptGossipInterval = properties
				.getBooleanProperty(RegistryProperties.ADAPT_GOSSIP_INTERVAL);
		boolean tree = properties.getBooleanProperty(RegistryProperties.TREE);

		Location location = Location.defaultLocation(userProperties);

		// join at server, also creates the "bootstrap" list
		identifier = join(connectionFactory.getLocalAddress(), location, data,
				checkerInterval, gossip, gossipInterval, adaptGossipInterval,
				tree);

		// bootstrap and gossip thread
		ThreadPool.createNew(this, "Registry");

		registryHandler = handler;

		// start sending events to the ibis instance we belong to
		if (registryHandler != null) {
			upcaller = new Upcaller(registryHandler, identifier);
		} else {
			upcaller = null;
		}

		// start handling incoming connections
		new ClientConnectionHandler(connectionFactory, this, state);

		logger.debug("registry for " + identifier + " initiated");
	}

	String getPoolName() {
		return poolName;
	}

	@Override
	public IbisIdentifier getIbisIdentifier() {
		return identifier;
	}

	synchronized boolean isStopped() {
		return stopped;
	}

	/**
	 * connects to the nameserver, joins, and gets back a bootstrap list with
	 * some peers
	 * 
	 * @throws IOException
	 *             in case of trouble
	 */
	private IbisIdentifier join(byte[] myAddress, Location location,
			byte[] implementationData, long checkupInterval, boolean gossip,
			long gossipInterval, boolean adaptGossipInterval, boolean tree)
			throws IOException {

		logger.debug("joining to " + getPoolName() + ", connecting to server");
		Connection connection = connectionFactory.connectToServer(true);

		logger.debug("sending join info to server");

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_JOIN);
			connection.out().writeInt(myAddress.length);
			connection.out().write(myAddress);

			connection.out().writeUTF(getPoolName());
			connection.out().writeInt(implementationData.length);
			connection.out().write(implementationData);
			location.writeTo(connection.out());
			connection.out().writeLong(checkupInterval);
			connection.out().writeBoolean(gossip);
			connection.out().writeLong(gossipInterval);
			connection.out().writeBoolean(adaptGossipInterval);
			connection.out().writeBoolean(tree);
			connection.out().flush();

			logger.debug("reading join result info from server");

			connection.getAndCheckReply();

			IbisIdentifier result = new IbisIdentifier(connection.in());
			int listLength = connection.in().readInt();
			bootstrapList = new IbisIdentifier[listLength];
			for (int i = 0; i < listLength; i++) {
				bootstrapList[i] = new IbisIdentifier(connection.in());
			}

			connection.close();

			logger.debug("join done");

			return result;
		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	@Override
	public void leave() throws IOException {
		logger.debug("leaving pool");

		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_LEAVE);
			getIbisIdentifier().writeTo(connection.out());
			connection.out().flush();

			connection.getAndCheckReply();

			connection.close();

			synchronized (this) {
				stopped = true;
				upcaller.stop();
				notifyAll();
			}
			connectionFactory.end();
			logger.debug("left");

			if (server != null) {
				logger
						.info("Central Registry: Waiting for central server to finish");
				server.end(true);
			}
		} catch (IOException e) {
			connection.close();
			synchronized (this) {
				stopped = true;
				upcaller.stop();
				notifyAll();
			}
			throw e;
		}
	}

	private void gossip(IbisIdentifier ibis) throws IOException {
		if (ibis.equals(getIbisIdentifier())) {
			logger.debug("not gossiping with self");
			return;
		}

		logger.debug("gossiping with " + ibis);

		Connection connection = connectionFactory.connect(ibis, false);

		try {
			connection.out().writeByte(Protocol.CLIENT_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_GOSSIP);
			connection.out().writeUTF(getPoolName());
			int localTime = state.getTime();
			connection.out().writeInt(localTime);
			connection.out().flush();

			connection.getAndCheckReply();

			int peerTime = connection.in().readInt();

			Event[] newEvents;
			if (peerTime > localTime) {

				int nrOfEvents = connection.in().readInt();
				if (nrOfEvents > 0) {
					newEvents = new Event[connection.in().readInt()];
					for (int i = 0; i < newEvents.length; i++) {
						newEvents[i] = new Event(connection.in());
					}
					state.handleEvents(newEvents);
				}
				connection.close();
			} else if (peerTime < localTime) {
				Event[] sendEvents = state.getEventsFrom(peerTime);

				connection.out().writeInt(sendEvents.length);
				for (Event event : sendEvents) {
					event.writeTo(connection.out());
				}

			} else {
				// nothing to send either way
			}
			logger.debug("gossiping with " + ibis + " done, time now: "
					+ state.getTime());
		} catch (IOException e) {
			connection.close();
			throw e;
		}
		connection.close();
	}

	public IbisIdentifier elect(String election) throws IOException {
		logger.debug("running election: \"" + election + "\"");

		if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
			throw new IbisConfigurationException(
					"No election support requested");
		}

		IbisIdentifier winner = state.getElectionResult(election, 0);
		if (winner != null) {
			logger.debug("election: \"" + election + "\" result = " + winner);

			return winner;
		}

		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_ELECT);
			getIbisIdentifier().writeTo(connection.out());
			connection.out().writeUTF(election);
			connection.out().flush();

			connection.getAndCheckReply();

			winner = new IbisIdentifier(connection.in());

			connection.close();

			logger.debug("election : \"" + election + "\" done, result = "
					+ winner);
			return winner;

		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	public IbisIdentifier getElectionResult(String election) throws IOException {
		return getElectionResult(election, 0);
	}

	public synchronized IbisIdentifier getElectionResult(String election,
			long timeoutMillis) throws IOException {
		logger.debug("getting election result for: \"" + election + "\"");

		if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS)) {
			throw new IbisConfigurationException(
					"No election support requested");
		}

		return state.getElectionResult(election, timeoutMillis);
	}

	@Override
	public long getSeqno(String name) throws IOException {
		logger.debug("getting sequence number");
		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_SEQUENCE_NR);
			connection.out().writeUTF(getPoolName());
			connection.out().flush();

			connection.getAndCheckReply();

			long result = connection.in().readLong();

			connection.close();

			logger.debug("sequence number = " + result);
			return result;
		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
		logger.debug("declaring " + ibis + " to be dead");

		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_DEAD);
			((IbisIdentifier) ibis).writeTo(connection.out());
			connection.out().flush();

			connection.getAndCheckReply();

			connection.close();

			logger.debug("done declaring " + ibis + " dead ");
		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
		logger.debug("reporting " + ibis + " to possibly be dead");

		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_MAYBE_DEAD);
			((IbisIdentifier) ibis).writeTo(connection.out());
			connection.out().flush();

			connection.getAndCheckReply();
			connection.close();

			logger.debug("done reporting " + ibis + " to possibly be dead");
		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	public void signal(String signal, ibis.ipl.IbisIdentifier... ibisses)
			throws IOException {
		logger.debug("telling " + ibisses.length + " ibisses a signal: "
				+ signal);

		if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
			throw new IbisConfigurationException("No signal support requested");
		}

		Connection connection = connectionFactory.connectToServer(true);

		try {
			connection.out().writeByte(Protocol.SERVER_MAGIC_BYTE);
			connection.out().writeByte(Protocol.OPCODE_SIGNAL);
			connection.out().writeUTF(getPoolName());
			connection.out().writeUTF(signal);
			connection.out().writeInt(ibisses.length);
			for (int i = 0; i < ibisses.length; i++) {
				((IbisIdentifier) ibisses[i]).writeTo(connection.out());
			}
			connection.out().flush();

			connection.getAndCheckReply();
			connection.close();

			logger.debug("done telling " + ibisses.length
					+ " ibisses a signal: " + signal);
		} catch (IOException e) {
			connection.close();
			throw e;
		}
	}

	public int getPoolSize() {
		if (!closedWorld) {
			throw new IbisConfigurationException(
					"totalNrOfIbisesInPool called but open world run");
		}
		return numInstances;
	}

	public void waitForAll() {

		if (!closedWorld) {
			throw new IbisConfigurationException("waitForAll() called but not "
					+ "closed world");
		}

		/*
		 * if (registryHandler != null && ! registryUpcallerEnabled) { throw new
		 * IbisConfigurationException("waitForAll() called but " + "registry
		 * events not enabled yet"); }
		 */

		state.waitForNrOfIbisses(numInstances);
	}

	public void enableEvents() {
		if (upcaller == null) {
			throw new IbisConfigurationException("Registry not configured to "
					+ "produce events");
		}

		upcaller.enableEvents();
	}

	public void disableEvents() {
		if (upcaller == null) {
			throw new IbisConfigurationException("Registry not configured to "
					+ "produce events");
		}

		upcaller.disableEvents();
	}

	private void bootstrap() {

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

	/**
	 * Called by the "RegistryState" object to tell the registry 
	 * a new event has arrived
	 * 
	 * @param event The new event
	 */
	synchronized void handleEvent(Event event) {
		//give event to upcaller too
		upcaller.newEvent(event);
		
		switch (event.getType()) {
		case Event.JOIN:
			if (joinedIbises != null) {
				for (IbisIdentifier ibis : event.getIbises()) {
					joinedIbises.add(ibis);
				}
			}
			break;
		case Event.LEAVE:
			if (leftIbises != null) {

				for (IbisIdentifier ibis : event.getIbises()) {
					leftIbises.add(ibis);
				}
			}
			break;
		case Event.DIED:
			if (diedIbises != null) {
				for (IbisIdentifier ibis : event.getIbises()) {
					diedIbises.add(ibis);
				}
			}
			break;
		case Event.SIGNAL:
			for (IbisIdentifier destination : event.getIbises()) {
				if (destination.equals(identifier)) {
					logger.debug("received signal: \"" + event.getDescription()
							+ "\"");
					signals.add(event.getDescription());
				}
			}
			break;
		case Event.ELECT:
		case Event.UN_ELECT:
			// NOT HANDLED HERE
			break;
		default:
			logger.error("unknown event type: " + event.getType());
		}

	}
	
	public void run() {
		bootstrap();

		if (!gossip) {
			return;
		}

		while (!isStopped()) {
			IbisIdentifier ibis = null;
			try {
				ibis = state.getRandomMember();

				gossip(ibis);
			} catch (IOException e) {
				logger.error("could not gossip with " + ibis + ": " + e);

			}

			logger.debug("Event time at " + identifier.getID() + " now "
					+ state.getTime());
			synchronized (this) {
				try {
					wait((int) (Math.random() * gossipInterval * 2));
				} catch (InterruptedException e) {
					// IGNORE
				}
			}
		}

	}

}
