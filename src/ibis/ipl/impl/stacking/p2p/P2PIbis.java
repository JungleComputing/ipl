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
import ibis.ipl.impl.stacking.p2p.util.P2PHashTools;
import ibis.ipl.impl.stacking.p2p.util.P2PMessage;
import ibis.ipl.impl.stacking.p2p.util.P2PRoutingInfo;
import ibis.ipl.support.vivaldi.VivaldiClient;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PIbis implements Ibis, MessageUpcall {

	private Ibis baseIbis;
	private ReceivePort receiver;
	private P2PNode myID, nearbyNode;
	private VivaldiClient vivaldiClient;
	private P2PState state;
	private boolean joined, foundNearbyNode, connected;
	private Byte connectionResponse;
	private PortType[] portTypes;
	private LinkedBlockingQueue<P2PStateInfo> stateUpdates = new LinkedBlockingQueue<P2PStateInfo>();
	private Vector<P2PNode> nearbyNodes = new Vector<P2PNode>();
	private int nearbyRequests;

	private P2PVisualizer p2pVisualizer;
	private P2PStateUpdateThread stateUpdaterThread;

	private IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	/** Counter for allocating names for anonymous send ports. */
	private static int send_counter = 0;

	/** Counter for allocating names for anonymous receive ports. */
	private static int receive_counter = 0;

	/** path position constants **/
	public final static int NEW_NODE = 0;
	public final static int NEARBY_NODE = 1;

	private int nearbyResponses = 0;

	/** The receive ports running on this Ibis instance. */
	private Map<String, P2PReceivePort> receivePorts = Collections
			.synchronizedMap(new HashMap<String, P2PReceivePort>());

	private static final Logger logger = LoggerFactory.getLogger(P2PIbis.class);

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

			joined = false;
			foundNearbyNode = false;
			connected = false;

			p2pVisualizer = new P2PVisualizer(myID.getIbisID(), myID.getP2pID());

			stateUpdaterThread = new P2PStateUpdateThread(stateUpdates, state);
			stateUpdaterThread.start();

			logger
					.debug(myID.getIbisID().name() + " " + myID
							+ " was created.");

			if (!myID.getIbisID().name().equals(("Ibis 0"))) {
				join();
			} else {
				joined = true;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
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
		stateUpdaterThread.interrupt();
		p2pVisualizer.sendNodeInfo(P2PMessage.NODE_DEPARTURE);
		p2pVisualizer.end();
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

	public void deRegister(ReceivePort receivePort) {
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

		logger.debug("Routing node with Ibis ID: " + dest.getIbisID().name()
				+ " and P2PID: " + dest);
		if (dest.equals(myID)) {
			logger.debug("Next dest for " + dest.getIbisID().name() + " is: "
					+ dest.getIbisID().name());
			return dest;
		}

		// search within leaf set
		nextDest = state.findLeafNode(dest);
		if (nextDest != null) {
			logger.debug("Next dest for " + dest.getIbisID().name() + " "
					+ dest + " is: " + nextDest.getIbisID().name() + " "
					+ nextDest);

			return nextDest;
		} else {
			int prefix = myID.prefixLength(dest);
			int digit = dest.digit(prefix);

			nextDest = state.getEntryAt(prefix, digit);

			if (nextDest == null) {
				logger.debug("Finding node rare case for: "
						+ dest.getIbisID().name() + " " + dest);
				nextDest = state.findNodeRareCase(dest, prefix);

				logger.debug("Rare case node for: " + dest.getIbisID().name()
						+ " " + dest + " is: " + nextDest.getIbisID().name()
						+ " " + nextDest);
			}
		}

		logger.debug("Next dest for " + dest.getIbisID().name() + " " + dest
				+ " is: " + nextDest.getIbisID().name() + " " + nextDest);

		return nextDest;
	}

	private void reverseJoinDirection(Vector<P2PNode> path) throws IOException {
		int myPosition = path.indexOf(myID);
		P2PNode nextDest = path.elementAt(myPosition - 1);

		// get node that issued the join request
		P2PNode newNode = path.elementAt(NEW_NODE);

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
			setJoined();

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
		}
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PMessage msg = (P2PMessage) readMessage.readObject();

		switch (msg.getType()) {
		// join related messages
		case P2PMessage.NEARBY_REQUEST:
			handleNearbyRequest(readMessage);
			break;
		case P2PMessage.NEARBY_RESPONSE:
			handleNearbyResponse(readMessage, true);
			break;
		case P2PMessage.NEARBY_NOT_JOINED:
			handleNearbyResponse(readMessage, false);
			break;
		case P2PMessage.JOIN_REQUEST:
			handleJoinRequest(readMessage, msg.getDest());
			break;
		case P2PMessage.JOIN_RESPONSE:
			handleJoinResponse(readMessage);
			break;
		case P2PMessage.STATE_REQUEST:
			handleStateUpdate(readMessage, true);
			break;
		case P2PMessage.STATE_RESPONSE:
			handleStateUpdate(readMessage, false);
			break;
		// external messages 
		case P2PMessage.REGULAR:
			handleRegularMessage(readMessage, msg.getDest(), msg.MESSAGE_ID);
			break;
		case P2PMessage.CONNECTION_REQUEST:
			handleConnectionRequest(readMessage, msg.getDest());
			break;
		case P2PMessage.CONNECTION_RESPONSE:
			handleConnectionResponse(readMessage, msg.getDest());
			break;
		// state updates sent to GUI
		case P2PMessage.LOGGER:
			handleVisualizerRequest(readMessage);
			break;	
		// state repair related messages
		case P2PMessage.PING_REQUEST:
			handlePingRequest(readMessage);
			break;
		case P2PMessage.PING_RESPONSE:
			handlePingResponse(readMessage);
			break;
		case P2PMessage.NEIGHBOR_REQUEST:
			handleNeighBorRequest(readMessage);
			break;
		case P2PMessage.NEIGHBOR_RESPONSE:
			handleNeighborResponse(readMessage);
			break;
		case P2PMessage.LEAF_REQUEST:
			handleLeafRequest(readMessage);
			break;
		case P2PMessage.LEAF_RESPONSE:
			handleLeafResponse(readMessage);
			break;
		case P2PMessage.ROUTE_REQUEST:
			handleRouteRequest(readMessage);
			break;
		case P2PMessage.ROUTE_RESPONSE:
			handleRouteResponse(readMessage);
			break;
		}
	}

	private void handleRouteResponse(ReadMessage readMessage) {
		// TODO: handle route response
		
	}

	private void handleRouteRequest(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		P2PNode source =(P2PNode) readMessage.readObject();
		int prefix = readMessage.readInt();
		int digit = readMessage.readInt();
		
		// get replacement
		P2PNode replacement = state.getEntryAt(prefix, digit);
		
		//send replacement only if is valid
		if (replacement != null) {
			P2PMessage msg = new P2PMessage(null, P2PMessage.ROUTE_RESPONSE);
			// route source, maybe source is already in the sets of this node
			P2PNode node = route(source);
			if (!node.equals(source)) {
				// if source is not in the sets, connect and send result
				source.connect(baseIbis.createSendPort(P2PConfig.portType));
				source.sendObjects(msg, replacement, prefix, digit);
			} else {
				node.sendObjects(msg, replacement, prefix, digit);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleLeafResponse(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		ArrayList<P2PNode> leafList = (ArrayList<P2PNode>) readMessage.readObject();
		int position = readMessage.readInt();
		readMessage.finish();
		
		P2PNode[] leafSet = (P2PNode[]) leafList.toArray();
		state.addRepairLeafSet(leafSet, position);
	}

	private void handleLeafRequest(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		int side = readMessage.readInt();
		int position = readMessage.readInt();
		readMessage.finish();
		
		ArrayList<P2PNode> leafSet = state.getLeafSet(side);
		
		P2PMessage msg = new P2PMessage(null, P2PMessage.LEAF_RESPONSE);
		P2PNode node = route(source);
		if (!node.equals(source)) {
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(msg, leafSet, position);
			return;
		}
		node.sendObjects(msg, leafSet, position);
	}

	@SuppressWarnings("unchecked")
	private void handleNeighborResponse(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		ArrayList<P2PNode> neighborhoodList = (ArrayList<P2PNode>) readMessage.readObject();
		int position = readMessage.readInt();
		readMessage.finish();
		
		P2PNode[] neighborhoodSet = (P2PNode[]) neighborhoodList.toArray();
		state.addRepairNeighborhoodSet(neighborhoodSet, position);
	}

	private void handleNeighBorRequest(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		int position = readMessage.readInt();
		readMessage.finish();
		
		ArrayList<P2PNode> neighborhoodSet = new ArrayList<P2PNode>(Arrays.asList(state.getNeighborhoodSet()));
		
		P2PMessage msg = new P2PMessage(null, P2PMessage.LEAF_RESPONSE);
		P2PNode node = route(source);
		if (!node.equals(source)) {
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(msg, neighborhoodSet, position);
			return;
		}
		node.sendObjects(msg, neighborhoodSet, position);
		
	}

	private void handlePingResponse(ReadMessage readMessage) throws IOException {
		int type = readMessage.readInt();
		int i = readMessage.readInt();
		int j = readMessage.readInt();
		readMessage.finish();
		
		state.handlePing(type, i, j);
		
	}

	private void handlePingRequest(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		
		int type = readMessage.readInt();
		int i = readMessage.readInt();
		int j = readMessage.readInt();

		readMessage.finish();

		P2PMessage msg = new P2PMessage(null, P2PMessage.PING_RESPONSE);
		P2PNode node = route(source);

		if (!node.equals(source)) {
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(msg, type, i, j);
		} else {
			node.sendObjects(msg, type, i, j);
		}
	}

	private void handleVisualizerRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		IbisIdentifier visualizerID = (IbisIdentifier) readMessage.readObject();
		readMessage.finish();

		p2pVisualizer.setVisualizerID(visualizerID);
		p2pVisualizer.setSendPort(baseIbis.createSendPort(P2PConfig.portType));
		p2pVisualizer.sendNodeInfo(P2PMessage.NODE_JOIN);

		// TODO: exception if p2pVisualizer is not null, only one visualization
		// at a time allowed
	}

	private void handleNearbyResponse(ReadMessage readMessage, boolean isNearby)
			throws IOException, ClassNotFoundException {
		if (isNearby) {
			P2PNode nearbyNode = (P2PNode) readMessage.readObject();
			readMessage.finish();
			nearbyNodes.add(nearbyNode);

		}
		incrementNearby();

		// if enough response received, notify main thread
		if (getNearby() == nearbyRequests) {
			logger.debug("queue has already " + nearbyRequests);
			setFoundNearbyNode();
		}
	}

	private int getNearby() {
		return nearbyResponses;
	}

	private synchronized void incrementNearby() {
		nearbyResponses++;

	}

	private void handleNearbyRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {

		P2PNode source = (P2PNode) readMessage.readObject();
		readMessage.finish();

		/*
		 * // wait until node has joined synchronized (this) { while (!joined) {
		 * try { wait(); } catch (Exception e) { // ignored } } }
		 */

		if (!getJoined()) {
			// send back result, not joined, cannot serve as a nearby node
			P2PMessage message = new P2PMessage(null,
					P2PMessage.NEARBY_NOT_JOINED);
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObject(message);
		} else {
			// find nearby Node
			P2PNode nearbyNode = state.findNearbyNode(source);

			// send back result
			P2PMessage message = new P2PMessage(null,
					P2PMessage.NEARBY_RESPONSE);
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(message, nearbyNode);
		}
	}

	private void handleStateUpdate(ReadMessage readMessage, boolean request)
			throws IOException, ClassNotFoundException {
		P2PStateInfo stateInfo = (P2PStateInfo) readMessage.readObject();
		stateInfo.setSendBack(request);
		readMessage.finish();

		try {
			stateUpdates.put(stateInfo);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * wait until join response message received
	 */
	private synchronized void setJoined() {
		joined = true;
		notifyAll();
	}

	private synchronized boolean getJoined() {
		return joined;
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
	private void findNearbyNode() throws IOException, InterruptedException {
		nearbyNode = new P2PNode(myID);
		P2PIdentifier p2pID = new P2PIdentifier("");

		while (!foundNearbyNode) {
			IbisIdentifier[] joinedIbises = baseIbis.registry().joinedIbises();
			logger.debug("Joined ibises:" + joinedIbises.length);
			for (int i = 0; i < joinedIbises.length; i++) {
				p2pID = new P2PIdentifier(P2PHashTools.MD5(joinedIbises[i]
						.name()));
				if (p2pID.prefixLength(myID.getP2pID()) == 0) {
					nearbyNode.setIbisID(joinedIbises[i]);
					nearbyNode.setP2pID(p2pID);
					if (nearbyNode.connect(baseIbis
							.createSendPort(P2PConfig.portType))) {
						setFoundNearbyNode();
					}
				}
				if (!foundNearbyNode) {
					Thread.sleep(P2PConfig.TIMEOUT);
				}
			}
		}
	}

	private void findNearbyKNodes() throws IOException {
		IbisIdentifier[] joinedIbises = baseIbis.registry().joinedIbises();
		int ibises = joinedIbises.length;

		// wait a certain amount of time until other ibises join
		// TODO: what if there are no other ibises....
		while (ibises == 1) {
			synchronized (this) {
				try {
					wait(P2PConfig.TIMEOUT);
				} catch (InterruptedException ex) {
					// ignore
				}
			}
			joinedIbises = baseIbis.registry().joinedIbises();
			ibises = joinedIbises.length;
		}

		nearbyRequests = Math.min(ibises - 1, P2PConfig.NEARBY_REQUESTS);
		int selected = 0;
		BitSet set = new BitSet();
		set.clear();

		logger.debug("Nearby requests: " + nearbyRequests);
		Random random = new Random();
		for (int i = 0; i < nearbyRequests; i++) {
			// generate random numbers between 0 and no. of ibises
			// until selected is not a not previously queried or my own id
			while (set.get(selected)
					|| joinedIbises[selected].equals(myID.getIbisID())) {
				selected = random.nextInt(ibises);
			}
			set.set(selected);

			logger.debug("Selected node: " + selected + " "
					+ joinedIbises[selected]);

			// send nearby node request
			P2PNode nearbyNode = new P2PNode(joinedIbises[selected]);
			P2PMessage msg = new P2PMessage(null, P2PMessage.NEARBY_REQUEST);

			nearbyNode.connect(baseIbis.createSendPort(P2PConfig.portType));
			nearbyNode.sendObjects(msg, myID);
		}

		logger.debug("Finished selecting nearby nodes!");

		// wait until nearbyNodes are found
		synchronized (this) {
			while (!foundNearbyNode) {
				try {
					wait(P2PConfig.NEARBY_TIMEOUT);
				} catch (Exception e) {
					// ignored
				}
			}
		}

		double minDist = myID.vivaldiDistance(nearbyNodes.elementAt(0));
		nearbyNode = nearbyNodes.elementAt(0);
		for (P2PNode node : nearbyNodes) {
			double dist = myID.vivaldiDistance(node);
			if (dist < minDist) {
				nearbyNode = node;
				minDist = dist;
			}
		}
	}

	/**
	 * implements join operation
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws NodeNotFoundException
	 */
	public void join() throws IOException, InterruptedException {
		// find a nearby node
		findNearbyKNodes();
		// findNearbyNode();

		logger.debug("Found nearby node:" + nearbyNode.getIbisID().name());

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
			nearbyNode.connect(baseIbis.createSendPort(P2PConfig.portType));
			nearbyNode.sendObjects(p2pMsg, path);

			// wait until join response message received
			// TODO: check if communication unreliable, wait with timeout
			synchronized (this) {
				while (!joined) {
					try {
						wait();
					} catch (Exception e) {
						// ignored
					}
				}
			}
			p2pVisualizer.sendNodeInfo(P2PMessage.NODE_JOIN);
			logger.debug("Join completed.");
		}
	}

	/**
	 * deliver regular message object[0] - source send port object[1] -
	 * receivePortNames object[2] - data
	 * 
	 * @param objects
	 */
	private void deliverRegularMessage(Vector<String> receivePortNames,
			SendPortIdentifier source, byte[] data) {

		logger.debug("Regular message received from: "
				+ source.ibisIdentifier().name());
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
			Vector<P2PNode> dests, String messageID) throws IOException,
			ClassNotFoundException {
		SendPortIdentifier source = (SendPortIdentifier) readMessage
				.readObject();
		byte[] data = (byte[]) readMessage.readObject();
		readMessage.finish();

		p2pVisualizer.sendAddMessage(messageID);
		forwardMessage(dests, P2PMessage.REGULAR, source, data);
		p2pVisualizer.sendDeleteMessage(messageID);
	}

	private void forwardMessage(Vector<P2PNode> dests, int regular,
			SendPortIdentifier source, byte[] data) {
		// for each destination, find next hop
		HashMap<P2PNode, Vector<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();
			Vector<P2PNode> nextHopDests = destinations.get(nextHop);

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(nextHopDests,
						P2PMessage.REGULAR);

				// forward message to next hop
				nextHop.sendObjects(msg, source, data);
			} else {
				// I am the destination, deliver message
				logger.debug("My dest: " + nextHopDests);

				Vector<String> receivePortNames = nextHopDests.elementAt(0)
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
	public void send(byte[] buffer, int length, SendPortIdentifier sid,
			Map<IbisIdentifier, ReceivePortIdentifier[]> connections)
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
				logger.debug(dest.getIbisID().name() + " Added recv port: "
						+ id.name());
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
			Vector<P2PNode> nextHopDests = destinations.get(nextHop);
			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(nextHopDests,
						P2PMessage.CONNECTION_REQUEST);

				logger.debug("Preparing to send connection request to: "
						+ nextHop.getIbisID().name());

				// forward message to next hop
				nextHop.sendObjects(msg, source, receivePortName, portType);

				logger.debug("sent connection request to: "
						+ nextHop.getIbisID().name());
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

				logger.debug("Forwarded connection response to:"
						+ nextHop.getIbisID().name());
				// forward message to next hop
				nextHop.sendObjects(msg, response);
			} else {
				// I am the destination, deliver message
				logger.debug("Connection response received.");
				setConnected(true, response);
				logger.debug("Connection response set.");
			}
		}
	}

	private void deliverConnectionRequest(SendPortIdentifier source,
			String receivePortName, String[] portType) {
		P2PReceivePort receivePort = findReceivePort(receivePortName);

		// prepare response for receiver
		Vector<P2PNode> dests = new Vector<P2PNode>();
		dests.add(new P2PNode(source.ibisIdentifier()));

		Byte response;
		logger.debug("Connection request:" + source.ibisIdentifier().name());

		if (receivePort != null) {
			response = receivePort.handleConnectionRequest(source,
					new PortType(portType));
		} else {
			response = P2PReceivePort.NOT_PRESENT;
		}

		logger.debug("Forwarding connection response to:"
				+ source.ibisIdentifier().name());

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
		String receivePortName = (String) readMessage.readObject();
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

		logger.debug("connection response received, processing...");
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
	public void connect(IbisIdentifier receiver, String receivePortName,
			SendPortIdentifier source, PortType senderType, long timeoutMillis,
			boolean fillTimeout) {
		Vector<P2PNode> dests = new Vector<P2PNode>();
		dests.add(new P2PNode(receiver));

		state.printSets();

		forwardConnectionRequest(dests, source, receivePortName, senderType
				.getCapabilities());

		if (senderType.hasCapability(PortType.COMMUNICATION_RELIABLE)) {
			// wait until connection request message is answered
			synchronized (this) {
				connected = false;
				while (!connected) {
					try {
						wait();
					} catch (Exception e) {
						// ignored
					}
				}
			}

			logger.debug("Connection complete.");
		}
	}

	public P2PReceivePort findReceivePort(String name) {
		return receivePorts.get(name);
	}

	private synchronized void setConnected(boolean connected, Byte response) {
		this.connected = connected;
		this.connectionResponse = response;
		notifyAll();
	}

	public synchronized Byte getConnectionResponse() {
		return connectionResponse;
	}
}
