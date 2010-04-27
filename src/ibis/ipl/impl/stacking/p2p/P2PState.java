package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.Ibis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

public class P2PState {
	private P2PInternalNode[] leafSet;
	private P2PInternalNode[] neighborhoodSet;
	private P2PInternalNode[][] routingTable;
	private P2PInternalNode minLeaf, maxLeaf, minNeighbor, maxNeighbor;
	private int columnSize, rowSize;
	private int neighborhoodSize, leftLeafSize, rightLeafSize;
	private P2PNode myID;
	private Ibis baseIbis;

	public P2PState(P2PNode myID, Ibis baseIbis) {
		// initialize leaf set / neighborhood set
		leafSet = new P2PInternalNode[P2PConfig.LEAF_SIZE];
		neighborhoodSet = new P2PInternalNode[P2PConfig.NEIGHBOORHOOD_SIZE];

		// initialize routing table
		int prefixSize = (int) Math.ceil(Math.pow(2, P2PConfig.b));
		rowSize = (int) Math.ceil(Math.log(P2PConfig.N) / Math.log(prefixSize));
		columnSize = prefixSize;

		routingTable = new P2PInternalNode[rowSize][columnSize];

		this.myID = myID;
		minLeaf = new P2PInternalNode(myID);
		maxLeaf = new P2PInternalNode(myID);
		minNeighbor = new P2PInternalNode(myID);
		maxNeighbor = new P2PInternalNode(myID);
		neighborhoodSize = rightLeafSize = leftLeafSize = 0;
		this.baseIbis = baseIbis;
	}

	private void insertElement(P2PInternalNode[] set, P2PInternalNode node,
			int start, int end, int size) throws IOException {
		int i;
		for (i = start; i < end && set[i] != null; i++)
			;
		if (i < end) {
			set[i] = node;
			//TODO: add connect
			//set[i].connect(baseIbis.createSendPort(P2PConfig.portType));
			size++;
		}
	}

	private void replaceElement(P2PInternalNode[] set, P2PInternalNode node, Comparator<P2PInternalNode> comparator, int start, int end) {
		Arrays.sort(set, start, end, comparator);
		int position = Arrays.binarySearch(set, start, end, node, comparator);

		if (position < 0) {
			int insertPoint = -1 - position;
			if (insertPoint < end) {
				set[insertPoint] = node;
			} else {
				set[end - 1] = node;
			}
		}
		
		//TODO: add connect, distance to myself
	}
	
	public void addLeafNode(P2PNode node) throws IOException {
		P2PIdentifier myP2PID = myID.getP2pID();
		P2PIdentifier nodeP2PID = node.getP2pID();
		P2PInternalNode newLeaf = new P2PInternalNode(node);
		Comparator<P2PInternalNode> leafComp = new P2PIdentifierComparator();
		
		if (myP2PID.compareTo(nodeP2PID) < 0) {
			// ID is greater than myID, insert at right
			if (rightLeafSize < P2PConfig.LEAF_SIZE / 2) {
				// the set is not full, insert and update max
				if (nodeP2PID.compareTo(maxLeaf.getNode().getP2pID()) > 0) {
					maxLeaf = newLeaf;
				}
				insertElement(leafSet, newLeaf, P2PConfig.LEAF_SIZE / 2,
						P2PConfig.LEAF_SIZE, rightLeafSize);
			} else {
				// the set is full, replace an entry with the new one
				replaceElement(leafSet, newLeaf, leafComp, P2PConfig.LEAF_SIZE / 2,
						P2PConfig.LEAF_SIZE);
				// update max leaf
				maxLeaf = leafSet[P2PConfig.LEAF_SIZE - 1];
			}
		} else {
			if (leftLeafSize < P2PConfig.LEAF_SIZE / 2) {
				// ID is smaller than myID, insert at left
				if (nodeP2PID.compareTo(minLeaf.getNode().getP2pID()) < 0) {
					minLeaf = newLeaf;
				}
				insertElement(leafSet, newLeaf, 0, P2PConfig.LEAF_SIZE / 2,
						leftLeafSize);
			} else {
				// the set is full, replace an entry with the new one
				replaceElement(leafSet, newLeaf, leafComp, 0, P2PConfig.LEAF_SIZE / 2);
				minLeaf = leafSet[0];
			}
		}
	}

	public void addNeighborNode(P2PNode node) throws IOException {
		P2PInternalNode newNeighbor = new P2PInternalNode(node);
		P2PDistanceComparator distComp = new P2PDistanceComparator();
		
		if (neighborhoodSize < P2PConfig.NEIGHBOORHOOD_SIZE) {
			insertElement(neighborhoodSet, newNeighbor, 0,
					P2PConfig.NEIGHBOORHOOD_SIZE, neighborhoodSize);

			// update min and max
			if (node.compareTo(minNeighbor.getNode()) < 0) {
				minNeighbor = newNeighbor;
			}

			if (node.compareTo(maxNeighbor.getNode()) > 0) {
				maxNeighbor = newNeighbor;
			}

		} else {
			// neighborhood set full, compare with min and max
			if (node.compareTo(minNeighbor.getNode()) > 0
					&& node.compareTo(maxNeighbor.getNode()) < 0) {
				replaceElement(neighborhoodSet, newNeighbor, distComp, 0, P2PConfig.NEIGHBOORHOOD_SIZE);
				
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
	 */
	public void addRoutingTableNode(P2PNode node, int i, int j) {
		if (routingTable[i][j] != null) {
			double oldDistance = myID.getCoords().distance(
					routingTable[i][j].getNode().getCoords());
			double newDistance = myID.getCoords().distance(node.getCoords());

			if (newDistance >= oldDistance)
				return;
		}

		P2PInternalNode newRoute = new P2PInternalNode(node);
		routingTable[i][j] = newRoute;
		
		//TODO: set distance to myself
		//TODO: connect
	}

	public P2PInternalNode getEntryAt(int i, int j) {
		return routingTable[i][j];
	}

	private P2PInternalNode findNode(P2PInternalNode[] set,
			P2PInternalNode minNode, P2PInternalNode maxNode, P2PNode node,
			int start, int end) {
		P2PInternalNode nextDest = null;
		P2PIdentifier nodeID = node.getP2pID();

		// check if node within range
		if (nodeID.compareTo(minNode.getNode().getP2pID()) < 0
				|| nodeID.compareTo(maxNode.getNode().getP2pID()) > 0)
			return null;

		int minDist = nodeID.prefixLength(myID.getP2pID());
		nextDest = new P2PInternalNode(myID);

		// search for node with closest distance
		for (int i = start; i < end && set[i] != null; i++) {
			P2PIdentifier leafID = set[i].getNode().getP2pID();
			int dist = nodeID.prefixLength(leafID);
			int newDiff = nodeID.digitDifference(leafID, dist + 1);
			int currDiff = nodeID.digitDifference(
					nextDest.getNode().getP2pID(), dist + 1);
			if (dist < minDist || (dist == minDist && newDiff < currDiff)) {
				nextDest = set[i];
			}
		}

		return nextDest;
	}

	private P2PInternalNode findNodeWithPrefix(P2PInternalNode[] set,
			P2PNode node, int start, int end, int prefix) {
		P2PInternalNode nextDest = new P2PInternalNode(myID);
		int minPrefix = prefix;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = start; i < end && set[i] != null; i++) {
			int newPrefix = nodeID.prefixLength(set[i].getNode().getP2pID());
			int currDiff = nodeID.digitDifference(
					nextDest.getNode().getP2pID(), newPrefix + 1);
			int newDiff = nodeID.digitDifference(set[i].getNode().getP2pID(),
					newPrefix + 1);

			if (newPrefix > minPrefix
					|| (newPrefix == minPrefix && newDiff < currDiff)) {
				nextDest = set[i];
			}

		}

		return nextDest;
	}

	public P2PInternalNode findLeafNode(P2PNode node) {
		P2PInternalNode nextDest = null;
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

	public P2PInternalNode findNeighBorNode(P2PNode node) {
		return findNode(neighborhoodSet, minNeighbor, maxNeighbor, node, 0,
				P2PConfig.NEIGHBOORHOOD_SIZE);
	}

	public P2PInternalNode findRoutingNode(P2PNode node, int prefix) {
		P2PInternalNode nextDest = new P2PInternalNode(myID);
		int minDiff = columnSize;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = 0; i < columnSize; i++) {
			if (routingTable[prefix][i] != null) {
				int newDiff = nodeID.digitDifference(routingTable[prefix][i]
						.getNode().getP2pID(), prefix);
				if (newDiff < minDiff) {
					nextDest = routingTable[prefix][i];
					minDiff = newDiff;
				}
			}
		}

		return nextDest;
	}

	public P2PInternalNode findNodeRareCase(P2PNode node, int prefix) {
		P2PInternalNode[] nextNodes = new P2PInternalNode[4];
		nextNodes[0] = findNodeWithPrefix(leafSet, node, 0,
				P2PConfig.LEAF_SIZE / 2, prefix);
		nextNodes[1] = findNodeWithPrefix(leafSet, node,
				P2PConfig.LEAF_SIZE / 2, P2PConfig.LEAF_SIZE, prefix);
		nextNodes[2] = findNodeWithPrefix(neighborhoodSet, node, 0,
				P2PConfig.NEIGHBOORHOOD_SIZE, prefix);
		nextNodes[3] = findRoutingNode(node, prefix);

		P2PInternalNode nextDest = new P2PInternalNode(myID);
		int maxPrefix = prefix;
		int minDigitDiff = columnSize;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = 0; i < nextNodes.length; i++) {
			int newPrefix = nodeID.prefixLength(nextNodes[i].getNode()
					.getP2pID());
			int newDigitDiff = nodeID.digitDifference(nextNodes[i].getNode()
					.getP2pID(), newPrefix);
			if (newPrefix > maxPrefix
					|| (newPrefix == maxPrefix && newDigitDiff < minDigitDiff)) {
				maxPrefix = newPrefix;
				minDigitDiff = newDigitDiff;
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
	private P2PNode[] convertSet(P2PInternalNode[] set) {
		P2PNode[] newSet = new P2PNode[set.length];
		for (int i = 0; i < set.length; i++) {
			if (set[i] != null) {
				newSet[i] = set[i].getNode();
			} else {
				newSet[i] = null;
			}
		}
		return newSet;
	}

	public P2PNode[] getRoutingTableRow(int row) {
		if (row < rowSize) {
			return convertSet(routingTable[row]);
		}
		return null;
	}

	public P2PNode[] getNeighborhoodSet() {
		return convertSet(neighborhoodSet);
	}

	public P2PNode[] getLeafSet() {
		return convertSet(leafSet);
	}

	public boolean updateSet(P2PInternalNode[] set, P2PNode[] receivedSet,
			P2PInternalNode min, P2PInternalNode max) {
		// update leaf set, source is the last node on the path
		boolean isEmpty = true;
		for (int i = 0; i < receivedSet.length; i++) {
			if (receivedSet[i] != null) {
				isEmpty = false;
				set[i] = new P2PInternalNode(receivedSet[i]);

				// update min
				if (receivedSet[i].getP2pID().compareTo(
						min.getNode().getP2pID()) < 0) {
					min = new P2PInternalNode(receivedSet[i]);
				}

				// update max
				if (receivedSet[i].getP2pID().compareTo(
						max.getNode().getP2pID()) > 0) {
					max = new P2PInternalNode(receivedSet[i]);
				}
			}
		}
		return isEmpty;
	}

	/*
	public void updateNeighborSet(P2PInternalNode[] set, P2PNode[] receivedSet,
			P2PInternalNode min, P2PInternalNode max) {
		// update neighborhood set, source is the last node on the path
		for (int i = 0; i < receivedSet.length; i++) {
			if (receivedSet[i] != null) {
				set[i] = new P2PInternalNode(receivedSet[i]);
				double distance = myID.getCoords().distance(receivedSet[i].getCoords());
				set[i].getNode().setDistance(distance);
				//TODO: connect
			}
		}
	} */

	public void parseSets(Vector<P2PNode> path,
			Vector<P2PNode[]> routingTables, P2PNode[] leafSet,
			P2PNode[] neighborhoodSet) throws IOException {

		boolean isEmpty;
		int maxPrefix = routingTables.size();
		P2PNode leafNode = path.elementAt(maxPrefix - 1);
		P2PNode nearbyNode = path.elementAt(1);

		// update routing table
		for (int i = 0; i < maxPrefix; i++) {
			// append is made in reverse order!
			P2PNode source = path.elementAt(i);
			P2PNode[] row = routingTables.elementAt(maxPrefix - i - 1);

			// update set
			int length = row.length;
			for (int j = 0; j < length; j++) {
				if (row[j] != null) {
					addRoutingTableNode(row[j], i, j);
				}
			}

			int digit = myID.getP2pID().charAt(i);
			double newDistance = row[digit].getCoords().distance(
					myID.getCoords());
			double oldDistance = row[digit].getCoords().distance(
					source.getCoords());

			if (newDistance < oldDistance) {
				// TODO: send notification to node
			}
		}

		// update leaf set
		isEmpty = updateSet(this.leafSet, leafSet, minLeaf, maxLeaf);

		// if my ID is between min and max leaf, send notification
		if (isEmpty
				|| (minLeaf.getNode().getP2pID().compareTo(myID.getP2pID()) < 0 && myID
						.getP2pID().compareTo(maxLeaf.getNode().getP2pID()) < 0)) {
			// notify leaf node;
			// TODO: send notification to leaf!
			P2PInternalNode source = new P2PInternalNode(leafNode);
			
			//source.connect(baseIbis.createSendPort(P2PConfig.portType));

		}

		// update neighborhood set
		isEmpty = updateSet(this.neighborhoodSet, neighborhoodSet, minNeighbor,
				maxNeighbor);

		// compute min and max distance
		double minDistance = 0, maxDistance = 0;
		double myDistance = myID.getCoords().distance(nearbyNode.getCoords());
		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet != null) {
				double distance = nearbyNode.getCoords().distance(
						neighborhoodSet[i].getCoords());
				if (distance < minDistance) {
					minDistance = distance;
				}
				if (distance > maxDistance) {
					maxDistance = distance;
				}
			}
		}

		// notify source if this is the case, source is the second node on the path
		if (isEmpty || (minDistance < myDistance && myDistance < maxDistance)) {
			// TODO: notify nearby node
			P2PInternalNode source = new P2PInternalNode(nearbyNode);
		}

		// parse path
		for (int i = 0; i < maxPrefix; i++) {
			P2PNode node = path.elementAt(i);
			// compute digit at
			int digit = node.getP2pID().charAt(i);
			addRoutingTableNode(node, maxPrefix, digit);
		}

		// TODO: add neighbor node the nearby node
		addNeighborNode(nearbyNode);

		// TODO: add leaf node the last node
		addLeafNode(leafNode);
	}

	public void setBaseIbis(Ibis baseIbis) {
		this.baseIbis = baseIbis;
	}

	public Ibis getBaseIbis() {
		return baseIbis;
	}
}
