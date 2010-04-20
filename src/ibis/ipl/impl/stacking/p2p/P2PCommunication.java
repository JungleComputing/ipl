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

	PortType portType = new PortType(PortType.COMMUNICATION_RELIABLE,
			PortType.SERIALIZATION_OBJECT_SUN, PortType.RECEIVE_AUTO_UPCALLS,
			PortType.CONNECTION_MANY_TO_ONE);

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
	private P2PNode route(P2PNode dest) {
		P2PNode nextDest;
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

	private void reverseJoinDirection(Vector<P2PNode> path) {
		path.remove(myID);
		for (int i = 0; i<path.size(); i++) {
			System.out.println(myID.getIbisID().name() + " " + path.elementAt(i).getIbisID().name());
		}
		
		
	}
	
	private void handleJoinRequest(ReadMessage readMessage,
			Vector<P2PNode> dests) throws IOException, ClassNotFoundException {

		// read current path and append my ID
		Vector<P2PNode> path = (Vector<P2PNode>) readMessage.readObject();
		path.add(myID);

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

		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			// for each next hop destination, construct a message
			P2PNode nextHop = iter.next();

			if (nextHop.equals(myID) == false) {
				P2PMessage msg = new P2PMessage(destinations.get(nextHop),
						P2PMessage.JOIN_REQUEST);
				// forward message to next hop
				nextHop.connect(myID.getIbisID());
				nextHop.sendObject(msg);
				nextHop.sendObject(path);
			} else {
				// process message, reverse message direction and append state
				reverseJoinDirection(path);
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
		}
	}

	public void run(IbisFactory factory,
			RegistryEventHandler registryEventHandler,
			Properties userProperties, IbisCapabilities capabilities,
			Credentials credentials, byte[] applicationTag,
			PortType[] portTypes, String specifiedSubImplementation,
			P2PIbisStarter p2pIbisStarter) throws IbisCreationFailedException,
			IOException {

		PortType[] ports = new PortType[1];
		ports[0] = portType;

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

		state = new P2PState(myID);

		receiver = baseIbis.createReceivePort(portType, "p2p", this);
		receiver.enableConnections();
		receiver.enableMessageUpcalls();
	}

	private P2PNode findNearbyNode() throws NodeNotFoundException, IOException {
		int i;
		P2PNode nearbyNode = myID;
		IbisIdentifier[] joinedIbises = baseIbis.registry().joinedIbises();
		P2PIdentifier p2pID = new P2PIdentifier("");

		for (i = 0; i < joinedIbises.length; i++) {
			p2pID = new P2PIdentifier(P2PHashTools.MD5(joinedIbises[i].name()));
			if (p2pID.prefixLength(myID.getP2pID()) == 0) {
				nearbyNode = new P2PNode();
				nearbyNode.setIbisID(joinedIbises[i]);
				nearbyNode.setP2pID(p2pID);
				nearbyNode.setSendPort(baseIbis.createSendPort(portType));
				nearbyNode.connect(myID.getIbisID());
				break;
			}
		}

		return nearbyNode;
	}

	/**
	 * implement join operation
	 * 
	 * @throws IOException
	 * @throws NodeNotFoundException
	 */
	public void join() throws IOException, NodeNotFoundException {
		// get a nearby node
		P2PNode nearbyNode = findNearbyNode();

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

			System.out.println("Am trimis!");
		}
	}
}
