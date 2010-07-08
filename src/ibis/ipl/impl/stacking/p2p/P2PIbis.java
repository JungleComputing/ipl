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
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.impl.stacking.p2p.endtoend.P2PMessage;
import ibis.ipl.impl.stacking.p2p.join.P2PJoinThread;
import ibis.ipl.impl.stacking.p2p.tracker.P2PTrackerClient;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PIdentifier;
import ibis.ipl.impl.stacking.p2p.util.P2PMessageHeader;
import ibis.ipl.impl.stacking.p2p.util.P2PNode;
import ibis.ipl.impl.stacking.p2p.util.P2PState;
import ibis.ipl.impl.stacking.p2p.util.P2PStateInfo;
import ibis.ipl.impl.stacking.p2p.util.P2PStateRepairThread;
import ibis.ipl.impl.stacking.p2p.util.P2PStateUpdateThread;
import ibis.ipl.impl.stacking.p2p.viz.P2PViewClient;
import ibis.ipl.support.vivaldi.VivaldiClient;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
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

	/** The P2PIbis tracker. */
	private P2PTrackerClient tracker;

	// external threads
	private P2PViewClient p2pVisualizer;
	private P2PStateUpdateThread stateUpdaterThread;
	private P2PStateRepairThread stateRepairThread;
	private P2PJoinThread joinThread;
	
	private IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	/** Counter for allocating names for anonymous send ports. */
	private static int send_counter = 0;

	/** Counter for allocating names for anonymous receive ports. */
	private static int receive_counter = 0;

	private int nearbyResponses = 0;

	/** The receive ports running on this Ibis instance. */
	private Map<String, P2PReceivePort> receivePorts = Collections
			.synchronizedMap(new HashMap<String, P2PReceivePort>());

	/** The send ports running on this Ibis instance. */
	private Map<String, P2PSendPort> sendPorts = Collections
			.synchronizedMap(new HashMap<String, P2PSendPort>());

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

		setBaseIbis(factory.createIbis(null, ibisCapabilities, userProperties,
				credentials, applicationTag, ports, specifiedSubImplementation));

		try {
			vivaldiClient = new VivaldiClient(userProperties,
					(ibis.ipl.registry.Registry) getBaseIbis().registry());

			setMyID(new P2PNode());
			getMyID().setIbisID(getBaseIbis().identifier());
			getMyID().setP2pID(new P2PIdentifier(getBaseIbis().identifier()));
			getMyID().setCoords(vivaldiClient.getCoordinates());

			setState(new P2PState(getMyID(), getBaseIbis()));
			nearbyNode = new P2PNode();

			receiver = getBaseIbis().createReceivePort(P2PConfig.portType, "p2p",
					this);
			receiver.enableConnections();
			receiver.enableMessageUpcalls();

			// initialize tracker
			tracker = new P2PTrackerClient(getBaseIbis());

			joined = false;
			foundNearbyNode = false;
			connected = false;

			p2pVisualizer = new P2PViewClient(getBaseIbis(), getMyID().getP2pID());
			new Thread(p2pVisualizer).start();

			stateUpdaterThread = new P2PStateUpdateThread(stateUpdates, getState());
			stateUpdaterThread.start();

			//stateRepairThread = new P2PStateRepairThread(getState());
			//stateRepairThread.start();

			joinThread = new P2PJoinThread(this);
			joinThread.start();
			
			logger
					.debug(getMyID().getIbisID().name() + " " + getMyID()
							+ " was created.");

			IbisIdentifier firstP2PNode = getBaseIbis().registry().elect(
					P2PConfig.ELECTION_JOIN);
			if (!getMyID().getIbisID().equals(firstP2PNode)) {
				join();
			} else {
				joined = true;
			}

			// send join information to tracker;
			tracker.sendNodeInfo();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException e) {
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

		P2PReceivePort receivePort =  new P2PReceivePort(portType, this, receivePortName,
				messageUpcall, receivePortConnectUpcall, properties);
		
		new Thread(receivePort).start();
		
		return receivePort;
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
		logger.debug("Ibis is ending...");
		//stateRepairThread.interrupt();
		stateUpdaterThread.interrupt();
		p2pVisualizer.sendNodeInfo(P2PMessageHeader.NODE_DEPARTURE);
		p2pVisualizer.end();
		getBaseIbis().end();
		
		logger.debug("Ibis is ended.");
	}

	@Override
	public String getVersion() {
		return "Stacking P2P Ibis over " + getBaseIbis().getVersion();
	}

	@Override
	public IbisIdentifier identifier() {
		return getMyID().getIbisID();
	}

	@Override
	public void poll() throws IOException {
		// TODO: implement poll

	}

	@Override
	public Properties properties() {
		return getBaseIbis().properties();
	}

	@Override
	public ibis.ipl.Registry registry() {
		return getBaseIbis().registry();
	}

	@Override
	public String getManagementProperty(String key)
			throws NoSuchPropertyException {
		return getBaseIbis().getManagementProperty(key);
	}

	@Override
	public Map<String, String> managementProperties() {
		return getBaseIbis().managementProperties();
	}

	@Override
	public void printManagementProperties(PrintStream stream) {
		getBaseIbis().printManagementProperties(stream);
	}

	@Override
	public void setManagementProperties(Map<String, String> properties)
			throws NoSuchPropertyException {
		getBaseIbis().setManagementProperties(properties);
	}

	@Override
	public void setManagementProperty(String key, String value)
			throws NoSuchPropertyException {
		getBaseIbis().setManagementProperty(key, value);
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
		if (dest.equals(getMyID())) {
			logger.debug("[myself]Next dest for " + dest.getIbisID().name() + " is: "
					+ dest.getIbisID().name());
			return dest;
		}

		// search within leaf set
		nextDest = getState().findLeafNode(dest);
		if (nextDest != null && !nextDest.isFailed()) {
			logger.debug("[leafset]Next dest for " + dest.getIbisID().name() + " "
					+ dest + " is: " + nextDest.getIbisID().name() + " "
					+ nextDest);

			return nextDest;
		} else {
			int prefix = getMyID().prefixLength(dest);
			int digit = dest.digit(prefix);

			// get routing table entry
			nextDest = getState().getEntryAt(prefix, digit);

			// find node rare case if entry is empty or node is failed
			if (nextDest == null || nextDest.isFailed()) {
				logger.debug("Finding node rare case for: "
						+ dest.getIbisID().name() + " " + dest);

				nextDest = getState().findNodeRareCase(dest, prefix);

				logger.debug("Rare case node for: " + dest.getIbisID().name()
						+ " " + dest + " is: " + nextDest.getIbisID().name()
						+ " " + nextDest);
			}
		}

		logger.debug("Next dest for " + dest.getIbisID().name() + " " + dest
				+ " is: " + nextDest.getIbisID().name() + " " + nextDest);

		return nextDest;
	}

	/*
	private void reverseJoinDirection(ArrayList<P2PNode> path) throws IOException {
		int myPosition = path.indexOf(getMyID());
		P2PNode nextDest = path.get(myPosition - 1);

		// get node that issued the join request
		P2PNode newNode = path.get(P2PJoinThread.NEW_NODE);

		// compute prefix length
		int prefix = newNode.prefixLength(getMyID());

		// create new P2PMessage
		P2PMessageHeader msg = new P2PMessageHeader(null,
				P2PMessageHeader.JOIN_RESPONSE);

		// create vector with routing table
		Vector<P2PRoutingInfo> routingTables = new Vector<P2PRoutingInfo>();
		P2PRoutingInfo myRoutingInfo = new P2PRoutingInfo(getMyID(), getState()
				.getRoutingTableRow(prefix), prefix);
		routingTables.add(myRoutingInfo);

		// get leaf set
		ArrayList<P2PNode> leafSet = getState().getLeafSet();

		// forward state
		nextDest.connect(getBaseIbis().createSendPort(P2PConfig.portType));

		if (myPosition == 1) {
			// I am the nearby node, send the neighborhood set
			nextDest.sendObjects(msg, path, routingTables, leafSet, getState()
					.getNeighborhoodSet());
		} else {
			nextDest.sendObjects(msg, path, routingTables, leafSet);
		}
	}
*/
	public HashMap<P2PNode, ArrayList<P2PNode>> route(ArrayList<P2PNode> dests) {
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = new HashMap<P2PNode, ArrayList<P2PNode>>();
		// find next hop for each destination
		for (int i = 0; i < dests.size(); i++) {
			P2PNode nextDest = route(dests.get(i));
			if (destinations.containsKey(nextDest)) {
				// append dest[i] for this next hop
				ArrayList<P2PNode> currDests = destinations.get(nextDest);
				currDests.add(dests.get(i));
			} else {
				// create new entry in hashmap
				Vector<P2PNode> currDests = new Vector<P2PNode>();
				currDests.add(dests.get(i));
				destinations.put(nextDest, dests);
			}
		}
		return destinations;
	}

	@Override
	public void upcall(ReadMessage readMessage) throws IOException,
			ClassNotFoundException{
		P2PMessageHeader msg = (P2PMessageHeader) readMessage.readObject();

		switch (msg.getType()) {
		// join related messages
		case P2PMessageHeader.NEARBY_REQUEST:
			handleNearbyRequest(readMessage);
			break;
		case P2PMessageHeader.NEARBY_RESPONSE:
			handleNearbyResponse(readMessage, true);
			break;
		case P2PMessageHeader.NEARBY_NOT_JOINED:
			handleNearbyResponse(readMessage, false);
			break;
		case P2PMessageHeader.JOIN_REQUEST:
			joinThread.putJoinRequest(readMessage);
			break;
		case P2PMessageHeader.JOIN_RESPONSE:
			joinThread.handleJoinResponse(readMessage);
			break;
		case P2PMessageHeader.ALREADY_JOINED:
			joinThread.setProcessNext(readMessage);
			break;
		case P2PMessageHeader.STATE_REQUEST:
			handleStateUpdate(readMessage, true);
			break;
		case P2PMessageHeader.STATE_RESPONSE:
			handleStateUpdate(readMessage, false);
			break;
		// external messages
		case P2PMessageHeader.REGULAR:
			handleRegularMessage(readMessage, msg.getDest());
			break;
		case P2PMessageHeader.REGULAR_ACK:
			handleRegularAck(readMessage, msg.getDest());
			break;
		case P2PMessageHeader.CONNECTION_REQUEST:
			handleConnectionRequest(readMessage, msg.getDest());
			break;
		case P2PMessageHeader.CONNECTION_RESPONSE:
			handleConnectionResponse(readMessage, msg.getDest());
			break;
		// state repair related messages
		case P2PMessageHeader.PING_REQUEST:
			handlePingRequest(readMessage);
			break;
		case P2PMessageHeader.PING_RESPONSE:
			handlePingResponse(readMessage);
			break;
		case P2PMessageHeader.NEIGHBOR_REQUEST:
			handleNeighBorRequest(readMessage);
			break;
		case P2PMessageHeader.NEIGHBOR_RESPONSE:
			handleNeighborResponse(readMessage);
			break;
		case P2PMessageHeader.LEAF_REQUEST:
			handleLeafRequest(readMessage);
			break;
		case P2PMessageHeader.LEAF_RESPONSE:
			handleLeafResponse(readMessage);
			break;
		case P2PMessageHeader.ROUTE_REQUEST:
			handleRouteRequest(readMessage);
			break;
		case P2PMessageHeader.ROUTE_RESPONSE:
			handleRouteResponse(readMessage);
			break;
		}
	}

	private void handleRegularAck(ReadMessage readMessage, ArrayList<P2PNode> dests)
			throws IOException, ClassNotFoundException {
		logger.debug("Processing regular ack... ");

		try {
			SendPortIdentifier sid = (SendPortIdentifier) readMessage
					.readObject();
			ReceivePortIdentifier rid = (ReceivePortIdentifier) readMessage
					.readObject();
			Integer ack = (Integer) readMessage.readObject();
			readMessage.finish();

			logger.debug("Received message ack: " + ack);

			forwardMessageAck(dests, sid, rid, ack);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	private void handleRouteResponse(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		P2PNode replacement = (P2PNode) readMessage.readObject();
		Integer prefix = (Integer) readMessage.readObject();
		Integer digit = (Integer) readMessage.readObject();
		readMessage.finish();

		getState().repairRoutingEntry(replacement, prefix, digit);
	}

	private void handleRouteRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		Integer prefix = (Integer) readMessage.readObject();
		Integer digit = (Integer) readMessage.readObject();
		readMessage.finish();

		// get replacement
		P2PNode replacement = getState().getEntryAt(prefix, digit);

		// send replacement only if is valid
		if (replacement != null) {
			P2PMessageHeader msg = new P2PMessageHeader(null,
					P2PMessageHeader.ROUTE_RESPONSE);
			// route source, maybe source is already in the sets of this node
			P2PNode node = route(source);
			if (!node.equals(source)) {
				// if source is not in the sets, connect and send result
				source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
				source.sendObjects(msg, replacement, prefix, digit);
			} else {
				node.sendObjects(msg, replacement, prefix, digit);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleLeafResponse(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		ArrayList<P2PNode> leafList = (ArrayList<P2PNode>) readMessage
				.readObject();
		Integer position = (Integer) readMessage.readObject();
		readMessage.finish();

		P2PNode[] leafSet = (P2PNode[]) leafList.toArray();
		getState().repairLeafSet(leafSet, position);
	}

	private void handleLeafRequest(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		Integer side = (Integer) readMessage.readObject();
		Integer position = (Integer) readMessage.readObject();
		readMessage.finish();

		ArrayList<P2PNode> leafSet = getState().getLeafSet(side);

		P2PMessageHeader msg = new P2PMessageHeader(null,
				P2PMessageHeader.LEAF_RESPONSE);
		P2PNode node = route(source);
		if (!node.isConnected()) {
			source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
			source.sendObjects(msg, leafSet, position);
			return;
		}
		node.sendObjects(msg, leafSet, position);
	}

	@SuppressWarnings("unchecked")
	private void handleNeighborResponse(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		ArrayList<P2PNode> neighborhoodList = (ArrayList<P2PNode>) readMessage
				.readObject();
		Integer position = (Integer) readMessage.readObject();
		readMessage.finish();

		P2PNode[] neighborhoodSet = (P2PNode[]) neighborhoodList.toArray();
		getState().repairNeighborhoodSet(neighborhoodSet, position);
	}

	private void handleNeighBorRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		Integer position = (Integer) readMessage.readObject();
		readMessage.finish();

		ArrayList<P2PNode> neighborhoodSet = getState().getNeighborhoodSet();

		P2PMessageHeader msg = new P2PMessageHeader(null,
				P2PMessageHeader.LEAF_RESPONSE);
		P2PNode node = route(source);
		if (!node.equals(source)) {
			source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
			source.sendObjects(msg, neighborhoodSet, position);
			return;
		}
		node.sendObjects(msg, neighborhoodSet, position);

	}

	private void handlePingResponse(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		Integer type = (Integer) readMessage.readObject();
		Integer i = (Integer) readMessage.readObject();
		Integer j = (Integer) readMessage.readObject();
		readMessage.finish();

		//logger.debug("Ping response of type " + type + " for " + i + " " + j);

		getState().handlePing(type, i, j);

	}

	private void handlePingRequest(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		Integer type = (Integer) readMessage.readObject();
		Integer i = (Integer) readMessage.readObject();
		Integer j = (Integer) readMessage.readObject();
		readMessage.finish();

		//logger.debug("Ping request received from " + source + "of type " + type
				//+ " for " + i + " " + j);

		P2PMessageHeader msg = new P2PMessageHeader(null,
				P2PMessageHeader.PING_RESPONSE);
		P2PNode node = route(source);

		if (!node.isConnected()) {
			source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
			source.sendObjects(msg, type, i, j);
		} else {
			node.sendObjects(msg, type, i, j);
		}
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

	private synchronized int getNearby() {
		return nearbyResponses;
	}

	private synchronized void incrementNearby() {
		nearbyResponses++;

	}

	private void handleNearbyRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {

		P2PNode source = (P2PNode) readMessage.readObject();
		readMessage.finish();

		if (!getJoined()) {
			// send back result, not joined, cannot serve as a nearby node
			P2PMessageHeader message = new P2PMessageHeader(null,
					P2PMessageHeader.NEARBY_NOT_JOINED);
			source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
			source.sendObject(message);
		} else {
			// find nearby Node
			P2PNode nearbyNode = getState().findNearbyNode(source);

			// send back result
			P2PMessageHeader message = new P2PMessageHeader(null,
					P2PMessageHeader.NEARBY_RESPONSE);
			source.connect(getBaseIbis().createSendPort(P2PConfig.portType));
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
			joinThread.setProcessNext(stateInfo.getSource(), true);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * wait until join response message received
	 */
	public synchronized void setJoined() {
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

	private void findNearbyKNodes() throws IOException, ClassNotFoundException {

		ArrayList<IbisIdentifier> joinedIbises = tracker.getJoinedIbises();
		nearbyRequests = joinedIbises.size();

		for (IbisIdentifier joinedIbis : joinedIbises) {
			P2PNode p2pNearbyNode = new P2PNode(joinedIbis);
			P2PMessageHeader msg = new P2PMessageHeader(null,
					P2PMessageHeader.NEARBY_REQUEST);

			// send nearby request
			p2pNearbyNode.connect(getBaseIbis().createSendPort(P2PConfig.portType));
			p2pNearbyNode.sendObjects(msg, getMyID());

			// logger.debug("Sent nearby request to " + joinedIbis.name() +
			// " .");
		}

		// logger.debug("Finished selecting nearby nodes!");

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

		double minDist = getMyID().vivaldiDistance(nearbyNodes.elementAt(0));
		nearbyNode = nearbyNodes.elementAt(0);
		for (P2PNode node : nearbyNodes) {
			double dist = getMyID().vivaldiDistance(node);
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
	 * @throws ClassNotFoundException
	 * @throws NodeNotFoundException
	 */
	public void join() throws IOException, InterruptedException,
			ClassNotFoundException {
		// find a nearby node
		findNearbyKNodes();

		logger.debug("Found nearby node:" + nearbyNode.getIbisID().name());

		// if I am not the only node in the network
		if (nearbyNode.equals(getMyID()) == false) {

			// set message key
			ArrayList<P2PNode> dest = new ArrayList<P2PNode>();
			dest.add(getMyID());

			// set message path
			ArrayList<P2PNode> path = new ArrayList<P2PNode>();
			path.add(getMyID());

			// route a join message
			P2PMessageHeader p2pMsg = new P2PMessageHeader(dest,
					P2PMessageHeader.JOIN_REQUEST);
			nearbyNode.connect(getBaseIbis().createSendPort(P2PConfig.portType));
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
			p2pVisualizer.sendNodeInfo(P2PMessageHeader.NODE_JOIN);
			logger.debug("Join completed.");
		}
	}

	/**
	 * deliver regular message object[0] - source send port object[1] -
	 * receivePortNames object[2] - data
	 * 
	 * @param objects
	 */
	private void deliverRegularMessage(
			Vector<ReceivePortIdentifier> receivePortIDs, P2PMessage message) {

		// logger.debug("Regular message received from: "
		// + message.getSid().name());

		// deliver message to each receivePort based on receivePortName
		for (ReceivePortIdentifier rid : receivePortIDs) {
			P2PReceivePort receivePort = receivePorts.get(rid.name());
			receivePort.processMessage(message);
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
			ArrayList<P2PNode> dests) throws IOException, ClassNotFoundException {
		P2PMessage message = (P2PMessage) readMessage.readObject();
		readMessage.finish();

		forwardMessage(dests, P2PMessageHeader.REGULAR, message);
	}

	private void forwardMessage(ArrayList<P2PNode> dests, int regular,
			P2PMessage message) {

		// for each destination, find next hop
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();
			ArrayList<P2PNode> nextHopDests = destinations.get(nextHop);

			p2pVisualizer.sendAddMessage(nextHop.getP2pID().getP2pID());

			if (nextHop.equals(getMyID()) == false) {
				P2PMessageHeader header = new P2PMessageHeader(nextHopDests,
						P2PMessageHeader.REGULAR);

				// forward message to next hop
				nextHop.sendObjects(header, message);

				p2pVisualizer.sendDeleteMessage(nextHop.getP2pID().getP2pID());
			} else {
				// I am the destination, deliver message

				Vector<ReceivePortIdentifier> receivePortNames = nextHopDests
						.get(0).getReceivePortNames();
				deliverRegularMessage(receivePortNames, message);
			}
		}
	}

	public void send(P2PMessage message) {
		P2PNode node = new P2PNode(message.getRid());

		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();
		dests.add(node);

		getState().printSets();

		forwardMessage(dests, P2PMessageHeader.REGULAR, message);

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
	public void send(P2PMessage message,
			Map<IbisIdentifier, ReceivePortIdentifier[]> connections)
			throws IOException, ClassNotFoundException {
		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();

		// construct destinations
		// group based on receive port identifier
		// TODO: what happens if sender not connected to any receiver?
		Collection<ReceivePortIdentifier[]> c = connections.values();
		Iterator<ReceivePortIdentifier[]> itr = c.iterator();
		while (itr.hasNext()) {
			ReceivePortIdentifier[] temp = itr.next();
			IbisIdentifier ibisID = temp[0].ibisIdentifier();
			P2PNode dest = new P2PNode(ibisID);

			for (int i = 0; i < temp.length; i++) {
				ibis.ipl.impl.stacking.p2p.ReceivePortIdentifier id = (ibis.ipl.impl.stacking.p2p.ReceivePortIdentifier) temp[i];
				dest.addReceivePortName(id);
			}

			dests.add(dest);
		}

		forwardMessage(dests, P2PMessageHeader.REGULAR, message);
	}

	public void forwardMessageAck(ArrayList<P2PNode> dests,
			SendPortIdentifier source, ReceivePortIdentifier rid, Integer ack) {
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();
			ArrayList<P2PNode> nextHopDests = destinations.get(nextHop);
			if (nextHop.equals(getMyID()) == false) {
				P2PMessageHeader header = new P2PMessageHeader(nextHopDests,
						P2PMessageHeader.REGULAR_ACK);
				logger.debug("Forwarding message ack: " + ack + " for "
						+ source + " to " + nextHop.getIbisID());
				nextHop.sendObjects(header, source, rid, ack);
			} else {
				deliverRegularAck(source, rid, ack);
			}
		}
	}

	private void deliverRegularAck(SendPortIdentifier source,
			ReceivePortIdentifier rid, Integer ack) {
		P2PSendPort sendPort = sendPorts.get(source.name());
		sendPort.processAck(rid, ack);
	}

	private void forwardConnectionRequest(ArrayList<P2PNode> dests,
			SendPortIdentifier source, String receivePortName, String[] portType) {

		// for each destination, find next hop
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();
			ArrayList<P2PNode> nextHopDests = destinations.get(nextHop);
			if (nextHop.equals(getMyID()) == false) {
				P2PMessageHeader msg = new P2PMessageHeader(nextHopDests,
						P2PMessageHeader.CONNECTION_REQUEST);

				// forward message to next hop
				nextHop.sendObjects(msg, source, receivePortName, portType);

			} else {
				// I am the destination, deliver message
				deliverConnectionRequest(source, receivePortName, portType);
			}
		}
	}

	private void forwardConnectionResponse(ArrayList<P2PNode> dests, Byte response) {
		// for each destination, find next hop
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = route(dests);
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();

			if (nextHop.equals(getMyID()) == false) {
				P2PMessageHeader msg = new P2PMessageHeader(destinations
						.get(nextHop), P2PMessageHeader.CONNECTION_RESPONSE);

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

		// prepare response for receiver
		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();
		dests.add(new P2PNode(source.ibisIdentifier()));

		Byte response;
		// logger.debug("Connection request:" + source.ibisIdentifier().name());

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
			ArrayList<P2PNode> dests) throws IOException, ClassNotFoundException {

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
			ArrayList<P2PNode> dests) throws IOException, ClassNotFoundException {
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
		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();
		dests.add(new P2PNode(receiver));

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

	public void register(P2PSendPort sid) throws IOException {
		if (sendPorts.get(sid.name()) != null) {
			throw new IOException("Multiple instances of receiveport named "
					+ sid.name());
		}

		sendPorts.put(sid.name(), sid);
	}

	public void setMyID(P2PNode myID) {
		this.myID = myID;
	}

	public P2PNode getMyID() {
		return myID;
	}

	public void setBaseIbis(Ibis baseIbis) {
		this.baseIbis = baseIbis;
	}

	public Ibis getBaseIbis() {
		return baseIbis;
	}

	public void setState(P2PState state) {
		this.state = state;
	}

	public P2PState getState() {
		return state;
	}
}
