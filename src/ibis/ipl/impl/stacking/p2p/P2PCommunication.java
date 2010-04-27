package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.Credentials;
import ibis.ipl.Ibis;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.MessageUpcall;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.support.vivaldi.VivaldiClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

public class P2PCommunication implements MessageUpcall {

	private Ibis baseIbis;
	private ReceivePort receiver;
	private P2PNode myID;
	private VivaldiClient vivaldiClient;
	private P2PState state;
	private boolean finished;
	
	IbisCapabilities ibisCapabilities = new IbisCapabilities(
			IbisCapabilities.ELECTIONS_STRICT,
			IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED);

	/**
	 * find next hop using the peer to peer routing scheme
	 * 
	 * @param dest
	 *            - final destination
	 * @return
	 */
	private P2PInternalNode route(P2PNode dest) {
		P2PInternalNode nextDest;
		P2PIdentifier myP2PID = myID.getP2pID();
		P2PIdentifier destP2PID = dest.getP2pID();

		// search within leaf set
		nextDest = state.findLeafNode(dest);
		if (nextDest != null) {
			return nextDest;
		} else {
			int prefix = myP2PID.prefixLength(dest.getP2pID());
			int digit = destP2PID.charAt(prefix);
			nextDest = state.getEntryAt(prefix, digit);

			if (nextDest == null) {
				nextDest = state.findNodeRareCase(dest, prefix);
			}
		}
		return nextDest;
	}

	private void reverseJoinDirection(Vector<P2PNode> path) throws IOException {
		int myPosition = path.indexOf(myID);
		P2PInternalNode nextDest = new P2PInternalNode(path.elementAt(myPosition - 1));
		
		// get node that issued the join request
		P2PIdentifier newNodeID = path.elementAt(0).getP2pID();
		
		System.out.println(myID.getIbisID().name() + " reverse dest:" + nextDest.getNode().getIbisID().name());
		
		// compute prefix length
		int prefix = newNodeID.prefixLength(myID.getP2pID()); 
		
		// create new P2PMessage
		P2PMessage msg = new P2PMessage(null, P2PMessage.JOIN_RESPONSE);
		
		// create vector with routing table
		Vector<P2PNode[]> routingTables = new Vector<P2PNode[]>();
		routingTables.add(state.getRoutingTableRow(prefix));
	
		// get leaf set
		P2PNode[] leafSet = state.getLeafSet();
		
		// forward state
		nextDest.connect(baseIbis.createSendPort(P2PConfig.portType));
		
		if (myPosition == 1) {
			// I am the nearby node, send the neighborhood set 
			nextDest.sendObjects(msg, path, routingTables, leafSet, state.getNeighborhoodSet());
		} else {
			nextDest.sendObjects(msg, path, routingTables, leafSet);
		}
		nextDest.close();
	}
	
	private void handleJoinRequest(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {

		// read current path and append my ID
		Vector<P2PNode> path = (Vector<P2PNode>) readMessage.readObject();
		path.add(myID);
		readMessage.finish();
		
		HashMap<P2PInternalNode, Vector<P2PNode>> destinations = new HashMap<P2PInternalNode, Vector<P2PNode>>();

		// find next hop for each destination
		for (int i = 0; i < dests.size(); i++) {
			P2PInternalNode nextDest = route(dests.elementAt(i));
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

		Set<P2PInternalNode> keys = destinations.keySet();
		Iterator<P2PInternalNode> iter = keys.iterator();
		while (iter.hasNext()) {
			// for each next hop destination, construct a message
			P2PInternalNode nextHop = iter.next();

			if (nextHop.getNode().equals(myID) == false) {
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

	private void handleJoinResponse(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		// read path
		Vector<P2PNode> path = (Vector<P2PNode>) readMessage.readObject();
		
		// read routing table
		Vector<P2PNode[]> routingTables = (Vector<P2PNode[]>) readMessage.readObject();
		
		// read leafSet
		P2PNode[] leafSet = (P2PNode[]) readMessage.readObject();
		
		int myPosition = -1;
		for (int i = 0; i<path.size(); i++) {
			P2PNode node = path.elementAt(i);
			if (node.getIbisID().name().equals(myID.getIbisID().name())) {
				myPosition = i;
				break;
			}
		}
		
		System.out.println(myID.getIbisID().name() + " " + myPosition);
		// get newly joined node ID
		if (myPosition == 0) {
			// I am the new node
			// parse routing table, leaf set, neighborhood set
			P2PNode[] neighborhoodSet = (P2PNode[]) readMessage.readObject();
			readMessage.finish();
			
			state.parseSets(path, routingTables, leafSet, neighborhoodSet);
			
			setFinished();
			
			System.out.println(myID.getIbisID() + " I am the new node!");
		} else {
			// append my routing table and forward message
			readMessage.finish();
			
			// get next destination 
			P2PInternalNode nextDest = new P2PInternalNode(path.elementAt(myPosition - 1));
			
			// node that issued the request has position 0 in the path
			P2PIdentifier newNodeID = path.elementAt(0).getP2pID();
			
			// compute prefix length between my ID and new node ID
			int prefix = newNodeID.prefixLength(myID.getP2pID());
			routingTables.add(state.getRoutingTableRow(prefix));
			
			nextDest.connect(baseIbis.createSendPort(P2PConfig.portType));
	
			// create new P2PMessage with type JOIN_RESPONSE
			P2PMessage msg = new P2PMessage(null, P2PMessage.JOIN_RESPONSE);
			if (myPosition == 1) {
				// I am the nearby node, send the neighborhood set 
				nextDest.sendObjects(msg, path, routingTables, leafSet, state.getNeighborhoodSet());
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
		case P2PMessage.JOIN_REQUEST:
			handleJoinRequest(readMessage, msg.getDest());
			break;
		case P2PMessage.JOIN_RESPONSE:
			handleJoinResponse(readMessage);
			break;
		}
	}

	// wait until join response message received
	private synchronized void setFinished() {
        finished = true;
        notifyAll();
    }
	
	public void run(IbisFactory factory,
			RegistryEventHandler registryEventHandler,
			Properties userProperties, IbisCapabilities capabilities,
			Credentials credentials, byte[] applicationTag,
			PortType[] portTypes, String specifiedSubImplementation,
			P2PIbisStarter p2pIbisStarter) throws IbisCreationFailedException,
			IOException {

		PortType[] ports = new PortType[1];
		ports[0] = P2PConfig.portType;

		baseIbis = factory.createIbis(null, ibisCapabilities, userProperties,
				credentials, applicationTag, ports, specifiedSubImplementation);

		try {
			vivaldiClient = new VivaldiClient(userProperties,
					(ibis.ipl.registry.Registry) baseIbis.registry());
		} catch (IOException e) {
			e.printStackTrace();
		}

		String p2pID = P2PHashTools.MD5(baseIbis.identifier().name());
		myID = new P2PNode();
		myID.setIbisID(baseIbis.identifier());
		myID.setP2pID(new P2PIdentifier(p2pID));
		myID.setCoords(vivaldiClient.getCoordinates());

		state = new P2PState(myID, baseIbis);
	
		receiver = baseIbis.createReceivePort(P2PConfig.portType, "p2p", this);
		receiver.enableConnections();
		receiver.enableMessageUpcalls();
	}

	private P2PInternalNode findNearbyNode() throws NodeNotFoundException, IOException {
		int i;
		P2PInternalNode internalNearbyNode = new P2PInternalNode(myID);
		
		IbisIdentifier[] joinedIbises = baseIbis.registry().joinedIbises();
		P2PIdentifier p2pID = new P2PIdentifier("");

		for (i = 0; i < joinedIbises.length; i++) {
			p2pID = new P2PIdentifier(P2PHashTools.MD5(joinedIbises[i].name()));
			if (p2pID.prefixLength(myID.getP2pID()) == 0) {
				P2PNode nearbyNode = new P2PNode();
				nearbyNode.setIbisID(joinedIbises[i]);
				nearbyNode.setP2pID(p2pID);
				
				internalNearbyNode.setNode(nearbyNode);
				break;
			}
		}

		return internalNearbyNode;
	}

	/**
	 * implement join operation
	 * 
	 * @throws IOException
	 * @throws NodeNotFoundException
	 */
	public void join() throws IOException, NodeNotFoundException {
		// get a nearby node
		P2PInternalNode nearbyNode = findNearbyNode();
		nearbyNode.connect(baseIbis.createSendPort(P2PConfig.portType));

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
}
