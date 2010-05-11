package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.ConnectionFailedException;
import ibis.ipl.Ibis;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class P2PState {
	private P2PNode[] leafSet;
	private P2PNode[] neighborhoodSet;
	private P2PNode[][] routingTable;
	private P2PNode minLeaf, maxLeaf, minNeighbor, maxNeighbor;
	private int columnSize, rowSize;
	private int neighborhoodSize, leftLeafSize, rightLeafSize;
	private P2PNode myID;
	private Ibis baseIbis;

	public P2PState(P2PNode myID, Ibis baseIbis) {
		// initialize leaf set / neighborhood set
		leafSet = new P2PNode[P2PConfig.LEAF_SIZE];
		neighborhoodSet = new P2PNode[P2PConfig.NEIGHBOORHOOD_SIZE];

		// initialize routing table
		rowSize = P2PConfig.MAX_PREFIX;
		columnSize = P2PConfig.MAX_DIGITS;

		routingTable = new P2PNode[rowSize][columnSize];

		this.myID = myID;
		minLeaf = new P2PNode(myID);
		maxLeaf = new P2PNode(myID);
		minNeighbor = new P2PNode(myID);
		maxNeighbor = new P2PNode(myID);
		neighborhoodSize = rightLeafSize = leftLeafSize = 0;
		this.baseIbis = baseIbis;
	}

	/**
	 * insert an element in either neighborhood set or leaf set
	 * 
	 * @param set
	 * @param node
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	private void insertElement(P2PNode[] set, P2PNode node, int start, int end)
			throws IOException {
		int i;
		for (i = start; i < end && set[i] != null; i++)
			if (set[i].compareTo(node) == 0)
				return;
		if (i < end) {
			set[i] = node;

			// connect and set distance to myself
			node.setDistance(myID);
			node.connect(baseIbis.createSendPort(P2PConfig.portType));

		}
	}

	/**
	 * replace an element in either neighborhood set or leaf set
	 * 
	 * @param set
	 * @param node
	 * @param comparator
	 * @param start
	 * @param end
	 */
	private void replaceElement(P2PNode[] set, P2PNode node,
			Comparator<P2PNode> comparator, int start, int end) {
		Arrays.sort(set, start, end, comparator);
		int position = Arrays.binarySearch(set, start, end, node, comparator);

		if (position < 0) {
			int insertPoint = -1 - position;
			if (insertPoint < end) {
				for (int i = end - 1; i > insertPoint; i--) {
					set[i] = set[i - 1];
				}
				set[insertPoint] = node;
			} else {
				set[end - 1] = node;
			}
		}

		// connect and set distance to myself
		try {
			node.setDistance(myID);
			node.connect(baseIbis.createSendPort(P2PConfig.portType));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * add new leaf node
	 * 
	 * @param node
	 * @throws IOException
	 */
	public void addLeafNode(P2PNode node) throws IOException {

		if (myID.compareTo(node) < 0) {
			// ID is greater than myID, insert at right
			if (rightLeafSize < P2PConfig.LEAF_SIZE / 2) {
				// the set is not full, insert and update max
				if (node.compareTo(maxLeaf) > 0) {
					maxLeaf = node;
				}
				insertElement(leafSet, node, P2PConfig.LEAF_SIZE / 2,
						P2PConfig.LEAF_SIZE);
				rightLeafSize++;
			} else {
				if (node.compareTo(maxLeaf) < 0) {
					Comparator<P2PNode> leafComp = new P2PIDAscComparator();
					// the set is full, replace an entry with the new one
					replaceElement(leafSet, node, leafComp,
							P2PConfig.LEAF_SIZE / 2, P2PConfig.LEAF_SIZE);
					// update max leaf
					maxLeaf = leafSet[P2PConfig.LEAF_SIZE - 1];
				}
			}
		} else {
			if (leftLeafSize < P2PConfig.LEAF_SIZE / 2) {
				// ID is smaller than myID, insert at left
				if (node.compareTo(minLeaf) < 0) {
					minLeaf = node;
				}
				insertElement(leafSet, node, 0, P2PConfig.LEAF_SIZE / 2);
				leftLeafSize++;
			} else {
				// the set is full, replace an entry with the new one
				if (node.compareTo(minLeaf) > 0) {
					Comparator<P2PNode> leafComp = new P2PIDDescComparator();
					replaceElement(leafSet, node, leafComp, 0,
							P2PConfig.LEAF_SIZE / 2);
					minLeaf = leafSet[0];
				}
			}
		}
	}

	/**
	 * add new neighbor node
	 * 
	 * @param node
	 * @throws IOException
	 */
	public void addNeighborNode(P2PNode node) throws IOException {
		P2PDistanceComparator distComp = new P2PDistanceComparator();

		if (neighborhoodSize < P2PConfig.NEIGHBOORHOOD_SIZE) {
			insertElement(neighborhoodSet, node, 0,
					P2PConfig.NEIGHBOORHOOD_SIZE);
			neighborhoodSize++;

			// update min and max
			if (node.compareTo(minNeighbor) < 0) {
				minNeighbor = node;
			}

			if (node.compareTo(maxNeighbor) > 0) {
				maxNeighbor = node;
			}

		} else {
			// neighborhood set full, compare with min and max
			if (node.compareTo(minNeighbor) > 0
					&& node.compareTo(maxNeighbor) < 0) {
				replaceElement(neighborhoodSet, node, distComp, 0,
						P2PConfig.NEIGHBOORHOOD_SIZE);

				// update min and max neighbor
				minNeighbor = neighborhoodSet[0];
				maxNeighbor = neighborhoodSet[P2PConfig.NEIGHBOORHOOD_SIZE - 1];
			}
		}
	}

	/**
	 * add new node at entry (i, j) from the routing table only if the distance
	 * is better than the old one
	 * 
	 * @param node
	 * @param i
	 * @param j
	 * @throws IOException
	 */
	public void addRoutingTableNode(P2PNode node, int i, int j)
			throws IOException {
		if (routingTable[i][j] != null) {
			double oldDistance = myID.vivaldiDistance(routingTable[i][j]);
			double newDistance = myID.vivaldiDistance(node);

			if (newDistance >= oldDistance)
				return;

			// close existing connection
			routingTable[i][j].close();
		}

		// update routing table and connect
		routingTable[i][j] = node;
		node.setDistance(myID);
		node.connect(baseIbis.createSendPort(P2PConfig.portType));
	}

	public P2PNode getEntryAt(int i, int j) {
		return routingTable[i][j];
	}

	private P2PNode findNode(P2PNode[] set, P2PNode minNode, P2PNode maxNode,
			P2PNode node, int start, int end) {
		P2PNode nextDest = myID;

		// check if node within range
		if (node.compareTo(minNode) < 0 || node.compareTo(maxNode) > 0)
			return null;

		BigInteger minDist = node.idDistance(myID);
		// search for node with closest distance
		for (int i = start; i < end && set[i] != null; i++) {
			BigInteger newDist = node.idDistance(set[i]);
			if (newDist.compareTo(minDist) < 0) {
				minDist = newDist;
				nextDest = set[i];
			}
		}

		return nextDest;
	}

	private P2PNode findNodeWithPrefix(P2PNode[] set, P2PNode node, int start,
			int end, int prefix) {
		P2PNode nextDest = myID;
		BigInteger minDiff = node.idDistance(myID);
		for (int i = start; i < end && set[i] != null; i++) {
			BigInteger newDiff = set[i].idDistance(myID);
			int currPrefix = node.prefixLength(set[i]);
			if (newDiff.compareTo(minDiff) < 0 && currPrefix >= prefix) {
				minDiff = newDiff;
				nextDest = set[i];
			}
		}

		return nextDest;
	}

	/**
	 * find the closest node to node within the leaf set
	 * 
	 * @param node
	 * @return
	 */
	public P2PNode findLeafNode(P2PNode node) {
		P2PNode nextDest = null;
		P2PIdentifier nodeP2PID = node.getP2pID();

		if (nodeP2PID.compareTo(myID.getP2pID()) < 0) {
			nextDest = findNode(leafSet, minLeaf, maxLeaf, node, 0,
					P2PConfig.LEAF_SIZE / 2);
		} else {
			nextDest = findNode(leafSet, minLeaf, maxLeaf, node,
					P2PConfig.LEAF_SIZE / 2, P2PConfig.LEAF_SIZE);
		}

		return nextDest;
	}

	public P2PNode findNeighBorNode(P2PNode node) {
		return findNode(neighborhoodSet, minNeighbor, maxNeighbor, node, 0,
				P2PConfig.NEIGHBOORHOOD_SIZE);
	}

	public P2PNode findRoutingNode(P2PNode node, int prefix) {
		P2PNode nextDest = myID;
		BigInteger minDiff = node.idDistance(myID);

		for (int i = 0; i < columnSize; i++) {
			if (routingTable[prefix][i] != null) {
				BigInteger newDiff = node.idDistance(routingTable[prefix][i]);
				int newPrefix = node.prefixLength(routingTable[prefix][i]);

				if (newDiff.compareTo(minDiff) < 0 && newPrefix >= prefix) {
					minDiff = newDiff;
					nextDest = routingTable[prefix][i];
				}
			}
		}

		return nextDest;
	}

	public P2PNode findNodeRareCase(P2PNode node, int prefix) {
		P2PNode[] nextNodes = new P2PNode[4];
		nextNodes[0] = findNodeWithPrefix(leafSet, node, 0,
				P2PConfig.LEAF_SIZE / 2, prefix);
		nextNodes[1] = findNodeWithPrefix(leafSet, node,
				P2PConfig.LEAF_SIZE / 2, P2PConfig.LEAF_SIZE, prefix);
		nextNodes[2] = findNodeWithPrefix(neighborhoodSet, node, 0,
				P2PConfig.NEIGHBOORHOOD_SIZE, prefix);
		nextNodes[3] = findRoutingNode(node, prefix);

		P2PNode nextDest = myID;

		BigInteger minDiff = node.idDistance(myID).abs();
		for (int i = 0; i < nextNodes.length; i++) {
			BigInteger newDiff = node.idDistance(nextNodes[i]).abs();
			if (newDiff.compareTo(minDiff) < 0) {
				minDiff = newDiff;
				nextDest = nextNodes[i];
			}
		}

		return nextDest;
	}

	/**
	 * convert set from P2PInternalNode to P2PNode for sending
	 * 
	 * @param set
	 * @return
	 */

	public P2PNode[] getRoutingTableRow(int row) {
		if (row < rowSize) {
			return routingTable[row];
		}
		return null;
	}

	public P2PNode[] getNeighborhoodSet() {
		return neighborhoodSet;
	}

	public P2PNode[] getLeafSet() {
		return leafSet;
	}

	public boolean updateSet(P2PNode[] set, P2PNode[] receivedSet, P2PNode min,
			P2PNode max) {
		// update leaf set, source is the last node on the path
		boolean isEmpty = true;
		for (int i = 0; i < receivedSet.length; i++) {
			if (receivedSet[i] != null) {
				// update entry
				isEmpty = false;
				set[i] = receivedSet[i];

				// update distance to myself, needed for neighborhood set
				set[i].setDistance(myID);

				// update min
				if (receivedSet[i].getP2pID().compareTo(min.getP2pID()) < 0) {
					min = receivedSet[i];
				}

				// update max
				if (receivedSet[i].getP2pID().compareTo(max.getP2pID()) > 0) {
					max = receivedSet[i];
				}
			}
		}
		return isEmpty;
	}

	public void sendNotification(P2PNode source, int type)
			throws ConnectionFailedException, IOException {
		P2PMessage msg = new P2PMessage(null, type);
		source.connect(baseIbis.createSendPort(P2PConfig.portType));
		source.sendObjects(msg, myID);
	}

	/**
	 * parse received states
	 * 
	 * @param path
	 * @param routingTables
	 * @param leafSet
	 * @param neighborhoodSet
	 * @throws IOException
	 */
	public void parseSets(Vector<P2PNode> path,
			Vector<P2PRoutingInfo> routingTables, P2PNode[] leafSet,
			P2PNode[] neighborhoodSet) throws IOException {

		boolean isEmpty;
		int pathSize = path.size();
		P2PNode leafNode = path.elementAt(pathSize - 1);
		P2PNode nearbyNode = path.elementAt(1);

		// update routing table
		for (int i = 0; i < routingTables.size(); i++) {
			// read state info
			P2PRoutingInfo info = routingTables.elementAt(i);
			P2PNode[] row = info.getRoutingRow();
			P2PNode source = info.getSource();

			// update set
			int length = row.length;
			for (int j = 0; j < length; j++) {
				if (row[j] != null) {
					addRoutingTableNode(row[j], info.getPrefix(), j);
				}
			}

			int digit = myID.digit(i);
			double newDistance = 0;
			double oldDistance = 1;
			if (row[digit] != null) {
				newDistance = row[digit].vivaldiDistance(myID);
				oldDistance = row[digit].vivaldiDistance(source);
			}

			// send notification if distance is smaller or row[i] = null
			if (newDistance < oldDistance) {
				// send notification to source node
				sendNotification(source, P2PMessage.ROUTE_UPDATE);
			}
		}

		// update leaf set
		isEmpty = updateSet(this.leafSet, leafSet, minLeaf, maxLeaf);

		// if my ID is between min and max leaf, send notification
		if (isEmpty
				|| (minLeaf.compareTo(myID) < 0 && myID.compareTo(maxLeaf) < 0)) {
			// notify leaf node;
			sendNotification(leafNode, P2PMessage.LEAF_UPDATE);
		}

		// update neighborhood set
		isEmpty = updateSet(this.neighborhoodSet, neighborhoodSet, minNeighbor,
				maxNeighbor);

		// compute min and max distance
		double minDistance = 0, maxDistance = 0;
		double myDistance = myID.vivaldiDistance(nearbyNode);
		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				double distance = nearbyNode
						.vivaldiDistance(neighborhoodSet[i]);
				if (distance < minDistance) {
					minDistance = distance;
				}
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}

		// notify nearby if this is the case, has position two in the path
		// vector
		if (isEmpty || (minDistance < myDistance && myDistance < maxDistance)) {
			sendNotification(nearbyNode, P2PMessage.NEIGHBOR_UPDATE);
		}

		// parse path
		for (int i = 1; i < pathSize; i++) {
			P2PNode node = path.elementAt(i);
			// compute digit at i
			int digit = node.digit(i - 1);
			addRoutingTableNode(node, i - 1, digit);
		}

		// add neighbor node the nearby node
		addNeighborNode(nearbyNode);

		// add leaf node the last node
		addLeafNode(leafNode);

		// System.out.println("Distance: " + myID.getCoords().toString());
		for (int i = 0; i < path.size(); i++) {
			System.out.println(myID + " " + i + " " + path.elementAt(i));
		}
		printSets();
	}

	public void setBaseIbis(Ibis baseIbis) {
		this.baseIbis = baseIbis;
	}

	public Ibis getBaseIbis() {
		return baseIbis;
	}

	private void printSets() {
		System.out.println(myID);
		for (int i = 0; i < leafSet.length; i++) {
			System.out.println(myID + " leafset " + i + " " + leafSet[i] + " ");
		}

		System.out.println();

		for (int i = 0; i < neighborhoodSet.length; i++) {
			System.out.println(myID + " neighbor " + i + " "
					+ neighborhoodSet[i] + " ");
		}

		System.out.println();
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				System.out.println(myID + " routing " + i + " " + j + " "
						+ routingTable[i][j] + " ");
			}
			System.out.println();
		}
	}

	/**
	 * cleanup set, close all send ports
	 * @param set
	 * @param size
	 * @throws IOException
	 */
	private void cleanupSet(P2PNode[] set, int size) throws IOException {
		for (int i = 0; i<size; i++) {
			if (set[i] != null) {
				set[i].close();
			}
		}
	}
	
	/**
	 * cleanup before exit - close all sendports from all sets
	 */
	public void end() {
		try {
			// cleanup routing tables
			for (int i = 0; i < P2PConfig.MAX_PREFIX; i++) {
				cleanupSet(routingTable[i], P2PConfig.MAX_DIGITS);
			}
			
			// cleanup leaf and neighborhood sets 
			cleanupSet(leafSet, P2PConfig.LEAF_SIZE);
			cleanupSet(neighborhoodSet, P2PConfig.NEIGHBOORHOOD_SIZE);
		} catch (IOException ex) {
			// TODO: possibly ignore this?
			ex.printStackTrace();
		}
	}
}
