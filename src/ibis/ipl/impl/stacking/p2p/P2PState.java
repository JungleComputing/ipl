package ibis.ipl.impl.stacking.p2p;

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
	private int neighborhoodSize, leftLeafSize, rightLeafSize;
	private P2PNode myID;
	transient private Ibis baseIbis;

	public P2PState(P2PNode myID, Ibis baseIbis) {
		// initialize leaf set / neighborhood set
		leafSet = new P2PNode[P2PConfig.LEAF_SIZE];
		neighborhoodSet = new P2PNode[P2PConfig.NEIGHBOORHOOD_SIZE];

		routingTable = new P2PNode[P2PConfig.MAX_PREFIX][P2PConfig.MAX_DIGITS];

		this.myID = myID;
		minLeaf = maxLeaf = minNeighbor = maxNeighbor = myID;
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
	private boolean insertElement(P2PNode[] set, P2PNode node,
			int start, int end) throws IOException {
		int i;
		if (node == null) {
			return false;
		}
		if (node.equals(myID)) {
			return false;
		}
		for (i = start; i < end && set[i] != null; i++)
			if (set[i].equals(node))
				return false;
		if (i < end) {
			set[i] = node;
			node.connect(baseIbis.createSendPort(P2PConfig.portType));
			node.setDistance(myID);
			return true;
		}
		return false;
	}

	/**
	 * replace an element in either neighborhood set or leaf set
	 * 
	 * @param set
	 * @param node
	 * @param comparator
	 * @param start
	 * @param end
	 * @throws IOException
	 */
	private void replaceElement(P2PNode[] set, P2PNode node,
			Comparator<P2PNode> comparator, int start, int end)
			throws IOException {
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
			node.connect(baseIbis.createSendPort(P2PConfig.portType));
			node.setDistance(myID);
		}
	}

	/**
	 * add new leaf node
	 * 
	 * @param node
	 * @throws IOException
	 */
	public synchronized void addLeafNode(P2PNode node) throws IOException {
		if (node == null) {
			return;
		}

		if (myID.compareTo(node) < 0) {
			// ID is greater than myID, insert at right
			if (rightLeafSize < P2PConfig.LEAF_SIZE / 2) {
				// the set is not full, insert and update max
				if (node.compareTo(maxLeaf) > 0) {
					maxLeaf = node;
				}
				if (insertElement(leafSet, node, P2PConfig.LEAF_SIZE / 2,
						P2PConfig.LEAF_SIZE)) {
					rightLeafSize++;
				}
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
				if (insertElement(leafSet, node, 0, P2PConfig.LEAF_SIZE / 2)) {
					leftLeafSize++;
				}
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
	public synchronized void addNeighborNode(P2PNode node) throws IOException {
		P2PDistanceComparator distComp = new P2PDistanceComparator();

		if (node == null) {
			return;
		}

		if (neighborhoodSize < P2PConfig.NEIGHBOORHOOD_SIZE) {
			if (insertElement(neighborhoodSet, node, 0,
					P2PConfig.NEIGHBOORHOOD_SIZE)) {
				neighborhoodSize++;
			}

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
	public synchronized void addRoutingTableNode(P2PNode node)
			throws IOException {
		if (node == null) {
			return;
		}

		int i = myID.prefixLength(node);
		if (i >= P2PConfig.MAX_PREFIX) {
			return;
		}

		int j = node.digit(i);
		if (j >= P2PConfig.MAX_DIGITS) {
			return;
		}

		if (routingTable[i][j] != null) {
			double oldDistance = myID.vivaldiDistance(routingTable[i][j]);
			double newDistance = myID.vivaldiDistance(node);

			if (newDistance >= oldDistance)
				return;
		}

		// update routing table and connect
		routingTable[i][j] = node;
		node.connect(baseIbis.createSendPort(P2PConfig.portType));
		node.setDistance(myID);
	}

	public synchronized P2PNode getEntryAt(int i, int j) {
		return routingTable[i][j];
	}

	private synchronized P2PNode findNode(P2PNode[] set, P2PNode minNode, P2PNode maxNode,
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

	private synchronized P2PNode findNodeWithPrefix(P2PNode[] set, P2PNode node, int start,
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

	public synchronized P2PNode findRoutingNode(P2PNode node, int prefix) {
		P2PNode nextDest = myID;
		BigInteger minDiff = node.idDistance(myID);

		for (int i = 0; i < P2PConfig.MAX_DIGITS; i++) {
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

	public synchronized P2PNode[] getRoutingTableRow(int row) {
		if (row < P2PConfig.MAX_PREFIX) {
			return routingTable[row];
		}
		return null;
	}

	public synchronized P2PNode[] getNeighborhoodSet() {
		return neighborhoodSet;
	}

	public synchronized P2PNode[] getLeafSet() {
		return leafSet;
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

		int pathSize = path.size();
		P2PNode leafNode = path.elementAt(pathSize - 1);
		P2PNode nearbyNode = path.elementAt(1);

		// update routing table
		for (int i = 0; i < routingTables.size(); i++) {
			// read state info
			P2PRoutingInfo info = routingTables.elementAt(i);
			P2PNode[] row = info.getRoutingRow();

			if (row != null) {
				// update routing table row
				for (int j = 0; j < row.length; j++) {
					addRoutingTableNode(row[j]);
				}
			}
		}

		for (int i = 0; i < P2PConfig.LEAF_SIZE; i++) {
			addLeafNode(leafSet[i]);
		}

		for (int i = 0; i < P2PConfig.NEIGHBOORHOOD_SIZE; i++) {
			addNeighborNode(neighborhoodSet[i]);
			addRoutingTableNode(neighborhoodSet[i]);
		}

		// parse path
		for (int i = 1; i < pathSize; i++) {
			addRoutingTableNode(path.elementAt(i));
		}

		// add neighbor node the nearby node
		addNeighborNode(nearbyNode);

		// add leaf node the last node
		addLeafNode(leafNode);

		P2PMessage msg = new P2PMessage(null, P2PMessage.STATE_REQUEST);
		P2PStateInfo myStateInfo = new P2PStateInfo(myID, this.routingTable, this.leafSet,
				this.neighborhoodSet, true); 
		// send a copy of my state to all nodes from routing table
		for (int i = 0; i < P2PConfig.MAX_PREFIX; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (routingTable[i][j] != null) {
					routingTable[i][j].sendObjects(msg, myStateInfo);
				}
			}
		}

		for (int i = 0; i < P2PConfig.LEAF_SIZE; i++) {
			if (this.leafSet[i] != null) {
				this.leafSet[i].sendObjects(msg, myStateInfo);
			}
		}

		for (int i = 0; i < P2PConfig.NEIGHBOORHOOD_SIZE; i++) {
			if (this.neighborhoodSet[i] != null) {
				this.neighborhoodSet[i].sendObjects(msg, myStateInfo);
			}
		}

		// printSets();
	}

	public void updateState(P2PStateInfo stateInfo)
			throws IOException {
		P2PNode source = stateInfo.getSource();
		P2PNode[][] routingTable = stateInfo.getRoutingTable();
		P2PNode[] leafSet = stateInfo.getLeafSet();
		P2PNode[] neighborhoodSet = stateInfo.getNeighborhoodSet();
		
		int prefix = myID.prefixLength(source);

		prefix = prefix < P2PConfig.MAX_PREFIX ? prefix
				: P2PConfig.MAX_PREFIX - 1;

		// printSets();
		addRoutingTableNode(source);
		addNeighborNode(source);
		addLeafNode(source);

		// update routing table
		for (int i = 0; i <= prefix; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				addRoutingTableNode(routingTable[i][j]);
			}
		}

		for (int i = 0; i < P2PConfig.LEAF_SIZE; i++) {
			addRoutingTableNode(leafSet[i]);
			addLeafNode(leafSet[i]);
		}

		for (int i = 0; i < P2PConfig.NEIGHBOORHOOD_SIZE; i++) {
			addRoutingTableNode(neighborhoodSet[i]);
			addNeighborNode(neighborhoodSet[i]);
		}

		if (stateInfo.isSendBack()) {
			P2PMessage msg = new P2PMessage(null, P2PMessage.STATE_RESPONSE);
			P2PStateInfo myStateInfo = new P2PStateInfo(myID, this.routingTable, this.leafSet,
					this.neighborhoodSet, false);
			// send my state
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(msg, myStateInfo);
		}
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
	 * 
	 * @param set
	 * @param size
	 * @throws IOException
	 */
	private void cleanupSet(P2PNode[] set, int size) throws IOException {
		for (int i = 0; i < size; i++) {
			if (set[i] != null) {
				set[i].close();
			}
		}
	}

	/**
	 * cleanup before exit - close all sendports from all sets
	 */
	public void end() {
		/*
		 * try {
		 * 
		 * // cleanup routing tables for (int i = 0; i < P2PConfig.MAX_PREFIX;
		 * i++) { cleanupSet(routingTable[i], P2PConfig.MAX_DIGITS); }
		 * 
		 * // cleanup leaf and neighborhood sets cleanupSet(leafSet,
		 * P2PConfig.LEAF_SIZE); cleanupSet(neighborhoodSet,
		 * P2PConfig.NEIGHBOORHOOD_SIZE);
		 * 
		 * } catch (IOException ex) { // TODO: possibly ignore this?
		 * ex.printStackTrace(); }
		 */
	}
}
