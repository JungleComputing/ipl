package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.support.vivaldi.VivaldiClient;

import java.beans.DesignMode;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class P2PIbis implements Ibis, MessageUpcall, RegistryEventHandler {

	private Ibis baseIbis;
	private ReceivePort receiver;
	private P2PNode myID, nearbyNode;
	private VivaldiClient vivaldiClient;
	private P2PState state;
	private boolean finished, foundNearbyNode, connected;
	private Byte connectionResponse;
	private PortType[] portTypes;

	private IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	/** Counter for allocating names for anonymous sendports. */
	private static int send_counter = 0;

	/** Counter for allocating names for anonymous receiveports. */
	private static int receive_counter = 0;

	/** path position constants **/
	public final static int NEW_NODE = 0;
	public final static int NEARBY_NODE = 1;

	/** The receiveports running on this Ibis instance. */
	private HashMap<String, P2PReceivePort> receivePorts = new HashMap<String, P2PReceivePort>();

	public P2PIbis(IbisFactory factory,
			RegistryEventHandler registryEventHandler,
			Properties userProperties, IbisCapabilities capabilities,
			Credentials credentials, byte[] applicationTag,
			PortType[] portTypes, String specifiedSubImplementation,
			P2PIbisStarter p2pIbisStarter) throws IbisCreationFailedException {

		// TODO: check porttype, at least one must be of P2PConfig.portType type
		this.portTypes = portTypes;

		// realloc ports arrays and add the p2pPortType
		int portLength = portTypes.length;
		PortType[] ports = Arrays.copyOf(portTypes, portLength + 1);
		ports[portLength] = P2PConfig.portType;

		baseIbis = factory.createIbis(null, ibisCapabilities, userProperties,
				credentials, applicationTag, ports, specifiedSubImplementation);

		try {
			vivaldiClient = new VivaldiClient(userProperties,
					(ibis.ipl.registry.Registry) baseIbis.registry());

			myID = new P2PNode();
			myID.setIbisID(baseIbis.identifier());
			myID.setP2pID(new P2PIdentifier(baseIbis.identifier()));
			myID.setCoords(vivaldiClient.getCoordinates());

			state = new P2PState(myID, baseIbis);
			nearbyNode = new P2PNode();

			receiver = baseIbis.createReceivePort(P2PConfig.portType, "p2p",
					this);
			receiver.enableConnections();
			receiver.enableMessageUpcalls();

			finished = false;
			foundNearbyNode = false;
			connected = false;

			join();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName) throws IOException {
		return createReceivePort(portType, receivePortName, null, null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName, MessageUpcall messageUpcall)
			throws IOException {

		return createReceivePort(portType, receivePortName, messageUpcall,
				null, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName,
			ReceivePortConnectUpcall receivePortConnectUpcall)
			throws IOException {

		return createReceivePort(portType, receivePortName, null,
				receivePortConnectUpcall, null);
	}

	@Override
	public ReceivePort createReceivePort(PortType portType,
			String receivePortName, MessageUpcall messageUpcall,
			ReceivePortConnectUpcall receivePortConnectUpcall,
			Properties properties) throws IOException {
		if (receivePortConnectUpcall != null) {
			if (!portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
				throw new IbisConfigurationException(
						"no connection upcalls requested for this port type");
			}
		}
		if (messageUpcall != null) {
			if (!portType.hasCapability(PortType.RECEIVE_AUTO_UPCALLS)
					&& !portType.hasCapability(PortType.RECEIVE_POLL_UPCALLS)) {
				throw new IbisConfigurationException(
						"no message upcalls requested for this port type");
			}
		} else {
			if (!portType.hasCapability(PortType.RECEIVE_EXPLICIT)) {
				throw new IbisConfigurationException(
						"no explicit receive requested for this port type");
			}
		}
		if (receivePortName == null) {
			synchronized (this.getClass()) {
				receivePortName = "anonymous receive port " + receive_counter++;
			}
		}
		matchPortType(portType);

		return new P2PReceivePort(portType, this, receivePortName,
				messageUpcall, receivePortConnectUpcall, properties);
	}

	private void matchPortType(PortType tp) {
		boolean matched = false;
		for (PortType p : portTypes) {
			if (tp.equals(p)) {
				matched = true;
			}
		}
		if (!matched) {
			throw new IbisConfigurationException("PortType \"" + tp
					+ "\" not specified when creating this Ibis instance");
		}
	}

	@Override
	public SendPort createSendPort(PortType portType) throws IOException {
		return createSendPort(portType, null, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String sendPortName)
			throws IOException {
		return createSendPort(portType, sendPortName, null, null);
	}

	@Override
	public SendPort createSendPort(PortType portType, String sendPortName,
			SendPortDisconnectUpcall sendPortDisconnectUpcall,
			Properties properties) throws IOException {

		if (sendPortDisconnectUpcall != null) {
			if (!portType.hasCapability(PortType.CONNECTION_UPCALLS)) {
				throw new IbisConfigurationException(
						"no connection upcalls requested for this port type");
			}
		}
		if (sendPortName == null) {
			synchronized (this.getClass()) {
				sendPortName = "anonymous send port " + send_counter++;
			}
		}
		// search if supplied port type exits in port types list
		matchPortType(portType);

		return new P2PSendPort(portType, this, sendPortName,
				sendPortDisconnectUpcall, properties);
	}

	@Override
	public void end() throws IOException {
		state.end();
		baseIbis.end();
	}

	@Override
	public String getVersion() {
		return "Stacking P2P Ibis over " + baseIbis.getVersion();
	}

	@Override
	public IbisIdentifier identifier() {
		return myID.getIbisID();
	}

	@Override
	public void poll() throws IOException {
		// TODO: implement poll

	}

	@Override
	public Properties properties() {
		return baseIbis.properties();
	}

	@Override
	public Registry registry() {
		return baseIbis.registry();
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return baseIbis.getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {
		return baseIbis.managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		baseIbis.printManagementProperties(stream);
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		baseIbis.setManagementProperties(properties);
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		baseIbis.setManagementProperty(key, value);
	}

	public void register(P2PReceivePort receivePort) throws IOException {
		if (receivePorts.get(receivePort.name()) != null) {
			throw new IOException("Multiple instances of receiveport named "
					+ receivePort.name());
		}
		receivePorts.put(receivePort.name(), receivePort);
	}

	synchronized void deRegister(ReceivePort receivePort) {
		if (receivePorts.remove(receivePort.name()) != null) {
			// TODO: add statistics for this receive port to "total" statistics
			// incomingMessageCount += receivePort.getMessageCount();
			// bytesReceived += receivePort.getBytesReceived();
			// bytesRead += receivePort.getBytesRead();
		}
	}

	public ReceivePortIdentifier createReceivePortIdentifier(String name,
			IbisIdentifier id) {

		return new ibis.ipl.impl.ReceivePortIdentifier(name,
				(ibis.ipl.impl.IbisIdentifier) id);
	}

	/**
	 * find next hop using the peer to peer routing scheme
	 * 
	 * @param dest
	 *            - final destination
	 * @return
	 */
	private P2PNode route(P2PNode dest) {
		P2PNode nextDest;

		// search within leaf set
		nextDest = state.findLeafNode(dest);
		if (nextDest != null) {
			return nextDest;
		} else {
			int prefix = myID.prefixLength(dest);
			int digit = dest.digit(prefix);
			nextDest = state.getEntryAt(prefix, digit);

			if (nextDest == null) {
				nextDest = state.findNodeRareCase(dest, prefix);
			}
		}
		return nextDest;
	}

	private void reverseJoinDirection(Vector<P2PNode> path) throws IOException {
		int myPosition = path.indexOf(myID);
		P2PNode nextDest = path.elementAt(myPosition - 1);

		// get node that issued the join request
		P2PNode newNode = path.elementAt(0);

		// compute prefix length
		int prefix = newNode.prefixLength(myID);

		// create new P2PMessage
		P2PMessage msg = new P2PMessage(null, P2PMessage.JOIN_RESPONSE);

		// create vector with routing table
		Vector<P2PRoutingInfo> routingTables = new Vector<P2PRoutingInfo>();
		P2PRoutingInfo myRoutingInfo = new P2PRoutingInfo(myID, state
				.getRoutingTableRow(prefix), prefix);
		routingTables.add(myRoutingInfo);

		// get leaf set
		P2PNode[] leafSet = state.getLeafSet();

		// forward state
		nextDest.connect(baseIbis.createSendPort(P2PConfig.portType));

		if (myPosition == 1) {
			// I am the nearby node, send the neighborhood set
			nextDest.sendObjects(msg, path, routingTables, leafSet, state
					.getNeighborhoodSet());
		} else {
			nextDest.sendObjects(msg, path, routingTables, leafSet);
		}
		nextDest.close();
	}

	private HashMap<P2PNode, Vector<P2PNode>> route(Vector<P2PNode> dests) {
		HashMap<P2PNode, Vector<P2PNode>> destinations = new HashMap<P2PNode, Vector<P2PNode>>();
		// find next hop for each destination
		for (int i = 0; i < dests.size(); i++) {
			P2PNode nextDest = route(dests.elementAt(i));
			if (destinations.containsKey(nextDest)) {
				// append dest[i] for this next hop
				Vector<P2PNode> currDests = destinations.get(nextDest);
				currDests.add(dests.elementAt(i));
			} else {
				// create new entry in hashmap
				Vector<P2PNode> currDests = new Vector<P2PNode>();
				currDests.add(dests.elementAt(i));
				destinations.put(nextDest, dests);
			}
		}
		return destinations;
	}

	@SuppressWarnings("unchecked")
	private void handleJoinRequest(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {

		// read current path and append my ID
		Vector<P2PNode> path = (Vector<P2PNode>) readMessage.readObject();
		path.add(myID);
		readMessage.finish();

		// for each destination, find next hop
		HashMap<P2PNode, Vector<P2PNode>> destinations = route(dests);

		// for each next hop destination, construct a message and forward join
		// request
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(destinations.get(nextHop),
						P2PMessage.JOIN_REQUEST);
				// forward message to next hop
				nextHop.connect(baseIbis.createSendPort(P2PConfig.portType));
				nextHop.sendObjects(msg, path);
				nextHop.close();
			} else {
				// process message, reverse message direction and append state
				reverseJoinDirection(path);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleJoinResponse(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		// read path
		Vector<P2PNode> path = (Vector<P2PNode>) readMessage.readObject();

		// read routing table
		Vector<P2PRoutingInfo> routingTables = (Vector<P2PRoutingInfo>) readMessage
				.readObject();

		// read leafSet
		P2PNode[] leafSet = (P2PNode[]) readMessage.readObject();

		int myPosition = -1;
		for (int i = 0; i < path.size(); i++) {
			P2PNode node = path.elementAt(i);
			if (node.getIbisID().name().equals(myID.getIbisID().name())) {
				myPosition = i;
				break;
			}
		}

		// get newly joined node ID
		if (myPosition == 0) {
			// I am the new node
			// parse routing table, leaf set, neighborhood set
			P2PNode[] neighborhoodSet = (P2PNode[]) readMessage.readObject();
			readMessage.finish();

			state.parseSets(path, routingTables, leafSet, neighborhoodSet);

			// received join response, wake main thread
			setFinished();

		} else {
			// append my routing table and forward message
			readMessage.finish();

			// get next destination
			P2PNode nextDest = path.elementAt(myPosition - 1);

			// node that issued the request has position 0 in the path
			P2PNode newNode = path.elementAt(NEW_NODE);

			// compute prefix length between my ID and new node ID
			int prefix = newNode.prefixLength(myID);
			P2PRoutingInfo myRoutingInfo = new P2PRoutingInfo(myID, state
					.getRoutingTableRow(prefix), prefix);
			routingTables.add(myRoutingInfo);

			nextDest.connect(baseIbis.createSendPort(P2PConfig.portType));

			// create new P2PMessage with type JOIN_RESPONSE
			P2PMessage msg = new P2PMessage(null, P2PMessage.JOIN_RESPONSE);
			if (myPosition == NEARBY_NODE) {
				// I am the nearby node, send the neighborhood set
				nextDest.sendObjects(msg, path, routingTables, leafSet, state
						.getNeighborhoodSet());
			} else {
				// forward routing table, leaf set
				nextDest.sendObjects(msg, path, routingTables, leafSet);
			}
			nextDest.close();
		}
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PMessage msg = (P2PMessage) readMessage.readObject();

		switch (msg.getType()) {
		case P2PMessage.JOIN_REQUEST:
			handleJoinRequest(readMessage, msg.getDest());
			break;
		case P2PMessage.JOIN_RESPONSE:
			handleJoinResponse(readMessage);
			break;
		case P2PMessage.NEIGHBOR_UPDATE:
			handleNeighborUpdate(readMessage);
			break;
		case P2PMessage.LEAF_UPDATE:
			handleLeafUpdate(readMessage);
			break;
		case P2PMessage.ROUTE_UPDATE:
			handleRouteUpdate(readMessage);
			break;
		case P2PMessage.REGULAR:
			handleRegularMessage(readMessage, msg.getDest());
			break;
		case P2PMessage.CONNECTION_REQUEST:
			handleConnectionRequest(readMessage, msg.getDest());
			break;
		case P2PMessage.CONNECTION_RESPONSE:
			handleConnectionResponse(readMessage, msg.getDest());
			break;
		}
	}

	private void handleRouteUpdate(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PNode newEntry = (P2PNode) readMessage.readObject();
		readMessage.finish();

		// compute prefix and digit, update routing table
		int prefix = myID.prefixLength(newEntry);
		int digit = newEntry.digit(prefix);
		state.addRoutingTableNode(newEntry, prefix, digit);
	}

	private void handleLeafUpdate(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PNode newLeaf = (P2PNode) readMessage.readObject();
		readMessage.finish();
		state.addLeafNode(newLeaf);
	}

	private void handleNeighborUpdate(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		P2PNode newNeighbor = (P2PNode) readMessage.readObject();
		readMessage.finish();
		state.addNeighborNode(newNeighbor);
	}

	/**
	 * wait until join response message received
	 */
	private synchronized void setFinished() {
		finished = true;
		notifyAll();
	}

	/**
	 * wait until nearby node found
	 */
	private synchronized void setFoundNearbyNode() {
		foundNearbyNode = true;
		notifyAll();
	}

	// TODO: problem 1: there is no other node that joined the pool before me
	// TODO: problem 2: the node that I am trying to connect to might not be
	// ready
	// TODO: problem 3: deadlock if all the nodes joined the all connects failed
	// TODO: change nearby node selection method - nodes should not know about
	// each other ready when accepting connections
	private void findNearbyNode() throws IOException {
		nearbyNode = new P2PNode(myID);
		IbisIdentifier[] joinedIbises = baseIbis.registry().joinedIbises();
		P2PIdentifier p2pID = new P2PIdentifier("");

		for (int i = 0; i < joinedIbises.length; i++) {
			p2pID = new P2PIdentifier(P2PHashTools.MD5(joinedIbises[i].name()));
			if (p2pID.prefixLength(myID.getP2pID()) == 0) {
				nearbyNode.setIbisID(joinedIbises[i]);
				nearbyNode.setP2pID(p2pID);
				if (nearbyNode.connect(baseIbis
						.createSendPort(P2PConfig.portType))) {
					setFoundNearbyNode();
				}
			}
		}

		// wait until nearby node found
		synchronized (this) {
			while (!foundNearbyNode) {
				try {
					wait();
				} catch (Exception e) {
					// ignored
				}
			}
		}
	}

	/**
	 * implements join operation
	 * 
	 * @throws IOException
	 * @throws NodeNotFoundException
	 */
	public void join() throws IOException {
		// find a nearby node
		findNearbyNode();

		// if I am not the only node in the network
		if (nearbyNode.equals(myID) == false) {

			// set message key
			Vector<P2PNode> dest = new Vector<P2PNode>();
			dest.add(myID);

			// set message path
			Vector<P2PNode> path = new Vector<P2PNode>();
			path.add(myID);

			// route a join message
			P2PMessage p2pMsg = new P2PMessage(dest, P2PMessage.JOIN_REQUEST);
			nearbyNode.sendObjects(p2pMsg, path);
			nearbyNode.close();

			// wait until join response message received
			synchronized (this) {
				while (!finished) {
					try {
						wait();
					} catch (Exception e) {
						// ignored
					}
				}
			}
		}
	}

	@Override
	public void died(IbisIdentifier corpse) {
	}

	@Override
	public void electionResult(String electionName, IbisIdentifier winner) {
	}

	@Override
	public void gotSignal(String signal, IbisIdentifier source) {
	}

	/**
	 * if there are not enough ibises in pool, wait until one joins and test if
	 * it is a candidate for the nearby node
	 */
	@Override
	public void joined(IbisIdentifier joinedIbis) {
		try {
			P2PIdentifier p2pID = new P2PIdentifier(joinedIbis);
			if (p2pID.prefixLength(myID.getP2pID()) == 0) {
				nearbyNode.setIbisID(joinedIbis);
				nearbyNode.setP2pID(p2pID);
				if (nearbyNode.connect(baseIbis
						.createSendPort(P2PConfig.portType))) {
					setFoundNearbyNode();
				}
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void left(IbisIdentifier leftIbis) {
	}

	@Override
	public void poolClosed() {
	}

	@Override
	public void poolTerminated(IbisIdentifier source) {
	}

	/**
	 * deliver regular message object[0] - source send port object[1] -
	 * receivePortNames object[2] - data
	 * 
	 * @param objects
	 */
	@SuppressWarnings("unchecked")
	private void deliverRegularMessage(Vector<String> receivePortNames,
			SendPortIdentifier source, byte[] data) {
		// deliver message to each receivePort based on receivePortName
		for (String receivePortName : receivePortNames) {
			P2PReceivePort receivePort = receivePorts.get(receivePortName);
			receivePort.deliverMessage(source, data);
		}
	}

	/**
	 * handle regular message received via upcall read source and data and
	 * forward it further
	 * 
	 * @param readMsg
	 * @param dests
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void handleRegularMessage(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {
		SendPortIdentifier source = (SendPortIdentifier) readMessage
				.readObject();
		byte[] data = (byte[]) readMessage.readObject();
		readMessage.finish();

		forwardMessage(dests, P2PMessage.REGULAR, source, data);
	}

	private void forwardMessage(Vector<P2PNode> dests, int regular,
			SendPortIdentifier source, byte[] data) {
		// for each destination, find next hop
		HashMap<P2PNode, Vector<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(destinations.get(nextHop),
						P2PMessage.REGULAR);

				// forward message to next hop
				nextHop.sendObjects(msg, source, data);
			} else {
				// I am the destination, deliver message
				Vector<P2PNode> myDests = destinations.get(myID);
				Vector<String> receivePortNames = myDests.elementAt(0)
						.getReceivePortNames();
				deliverRegularMessage(receivePortNames, source, data);
			}
		}

	}

	/**
	 * prepare message from sid to connections to be routed within the overlay
	 * network
	 * 
	 * @param buffer
	 * @param length
	 * @param sid
	 * @param connections
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public synchronized void send(byte[] buffer, int length,
			SendPortIdentifier sid,
			HashMap<IbisIdentifier, ReceivePortIdentifier[]> connections)
			throws IOException, ClassNotFoundException {
		Vector<P2PNode> dests = new Vector<P2PNode>();

		// construct destinations
		// group based on receive port identifier
		// TODO: what happens if sender not connected to any receiver?
		Collection<ReceivePortIdentifier[]> c = connections.values();
		Iterator<ReceivePortIdentifier[]> itr = c.iterator();
		while (itr.hasNext()) {
			ReceivePortIdentifier[] temp = itr.next();
			IbisIdentifier ibisID = temp[0].ibisIdentifier();
			P2PNode dest = new P2PNode(ibisID);

			for (ReceivePortIdentifier id : temp) {
				dest.addReceivePortName(id.name());
			}
			dests.add(dest);
		}

		forwardMessage(dests, P2PMessage.REGULAR, sid, buffer);
	}

	private void forwardConnectionRequest(Vector<P2PNode> dests,
			SendPortIdentifier source, String receivePortName, String[] portType) {

		// for each destination, find next hop
		HashMap<P2PNode, Vector<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(destinations.get(nextHop),
						P2PMessage.CONNECTION_REQUEST);

				System.out.println(myID.getIbisID().name()
						+ "Connection request la: "
						+ nextHop.getIbisID().name());

				// forward message to next hop
				nextHop.sendObjects(msg, source, receivePortName, portType);
			} else {
				// I am the destination, deliver message
				deliverConnectionRequest(source, receivePortName, portType);
			}
		}
	}

	private void forwardConnectionResponse(Vector<P2PNode> dests, Byte response) {
		// for each destination, find next hop
		HashMap<P2PNode, Vector<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(destinations.get(nextHop),
						P2PMessage.CONNECTION_RESPONSE);

				System.out.println(myID.getIbisID().name()
						+ "Connection response la: "
						+ nextHop.getIbisID().name());

				// forward message to next hop
				nextHop.sendObjects(msg, response);
			} else {
				// I am the destination, deliver message
				setConnected(true, response);
			}
		}
	}

	private void deliverConnectionRequest(SendPortIdentifier source,
			String receivePortName, String[] portType) {
		P2PReceivePort receivePort = findReceivePort(receivePortName);

		System.out.println(myID.getIbisID().name()
				+ "Connection request received from: "
				+ source.ibisIdentifier().name());

		// prepare response for receiver
		Vector<P2PNode> dests = new Vector<P2PNode>();
		dests.add(new P2PNode(source.ibisIdentifier()));

		Byte response;
		if (receivePort != null) {
			response = receivePort.handleConnectionRequest(source,
					new PortType(portType));
		} else {
			response = P2PReceivePort.NOT_PRESENT;
		}

		forwardConnectionResponse(dests, response);
	}

	/**
	 * handle join request, read object and forward message
	 * 
	 * @param readMessage
	 * @param dests
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void handleConnectionRequest(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {

		SendPortIdentifier source = (SendPortIdentifier) readMessage
				.readObject();
		Object obj = readMessage.readObject();

		System.out.println(obj.getClass().getName());

		String receivePortName = (String) obj;
		String[] portType = (String[]) readMessage.readObject();

		readMessage.finish();

		forwardConnectionRequest(dests, source, receivePortName, portType);
	}

	/**
	 * handle connection response
	 * 
	 * @param readMessage
	 * @param dest
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void handleConnectionResponse(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {
		Byte response = (Byte) readMessage.readObject();
		readMessage.finish();

		forwardConnectionResponse(dests, response);
	}

	/**
	 * perform connect to receiver on specified receivePortName send connection
	 * request message and wait until response is received TODO: what happens if
	 * timeout? - look in other implementations
	 * 
	 * @param receiver
	 * @param receivePortName
	 * @param source
	 * @param senderType
	 * @param timeoutMillis
	 * @param fillTimeout
	 * @return
	 */
	public Byte connect(IbisIdentifier receiver, String receivePortName,
			SendPortIdentifier source, PortType senderType, long timeoutMillis,
			boolean fillTimeout) {
		Vector<P2PNode> dests = new Vector<P2PNode>();
		dests.add(new P2PNode(receiver));

		System.out.println(myID.getIbisID().name() + " connect request la: "
				+ receiver.name());

		forwardConnectionRequest(dests, source, receivePortName, senderType
				.getCapabilities());

		// TODO: implement timeouts
		// wait until connection request message is answered
		synchronized (this) {
			while (!connected) {
				try {
					wait();
				} catch (Exception e) {
					// ignored
				}
			}
			connected = false;
		}

		// TODO: is it thread safe?
		return connectionResponse;
	}

	public synchronized P2PReceivePort findReceivePort(String name) {
		return receivePorts.get(name);
	}

	private synchronized void setConnected(boolean connected, Byte response) {
		this.connected = connected;
		this.connectionResponse = response;
		notifyAll();
	}
}
