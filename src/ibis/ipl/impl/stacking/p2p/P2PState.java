package ibis.ipl.impl.stacking.p2p;

public class P2PState {
	private P2PNode[] leafSet;
	private P2PNode[] neighborhoodSet;
	private P2PNode[][] routingTable;
	private P2PNode myID, minLeaf, maxLeaf, minNeighbor, maxNeighbor;
	private int columnSize, rowSize;

	public P2PState(P2PNode myID) {
		// initialize leaf set / neighborhood set
		leafSet = new P2PNode[P2PConfig.LEAF_SIZE];
		neighborhoodSet = new P2PNode[P2PConfig.NEIGHBOORHOOD_SIZE];

		// initialize routing table
		int prefixSize = (int) Math.ceil(Math.pow(2, P2PConfig.b));
		rowSize = (int) Math.ceil(Math.log(P2PConfig.N) / Math.log(prefixSize));
		columnSize = prefixSize;

		routingTable = new P2PNode[rowSize][columnSize];

		this.myID = myID;
		minLeaf = maxLeaf = minNeighbor = maxNeighbor = myID;
	}

	private void insertElement(P2PNode[] set, P2PNode node, int start, int end) {
		int i;
		for (i = start; i < end && set[i] != null; i++)
			;
		set[i] = node;
	}

	public void addLeafNode(P2PNode node) {
		P2PIdentifier myP2PID = myID.getP2pID();
		P2PIdentifier nodeP2PID = node.getP2pID();

		if (myP2PID.compareTo(nodeP2PID) < 0) {
			// ID is greater than myID, insert at right
			if (nodeP2PID.compareTo(maxLeaf.getP2pID()) > 0) {
				maxLeaf = node;
			}
			insertElement(leafSet, node, P2PConfig.LEAF_SIZE / 2,
					P2PConfig.LEAF_SIZE);
		} else {
			// ID is smaller than myID, insert at left
			if (nodeP2PID.compareTo(minLeaf.getP2pID()) < 0) {
				minLeaf = node;
			}
			insertElement(leafSet, node, 0, P2PConfig.LEAF_SIZE / 2);
		}
	}

	public void addNeighborNode(P2PNode node) {
		insertElement(neighborhoodSet, node, 0, P2PConfig.NEIGHBOORHOOD_SIZE);
	}

	public void addRoutingTableNode(P2PNode node, int i, int j) {
		routingTable[i][j] = node;
	}

	public P2PNode getEntryAt(int i, int j) {
		return routingTable[i][j];
	}

	private P2PNode findNode(P2PNode[] set, P2PNode minNode, P2PNode maxNode,
			P2PNode node, int start, int end) {
		P2PNode nextDest = null;
		P2PIdentifier nodeID = node.getP2pID();

		// check if node within range
		if (nodeID.compareTo(minNode.getP2pID()) < 0
				|| nodeID.compareTo(maxNode.getP2pID()) > 0)
			return null;

		int minDist = nodeID.prefixLength(myID.getP2pID());
		nextDest = myID;

		// search for node with closest distance
		for (int i = start; i < end && set[i] != null; i++) {
			P2PIdentifier leafID = set[i].getP2pID();
			int dist = nodeID.prefixLength(leafID);
			int newDiff = nodeID.digitDifference(leafID, dist + 1);
			int currDiff = nodeID
					.digitDifference(nextDest.getP2pID(), dist + 1);
			if (dist < minDist || (dist == minDist && newDiff < currDiff)) {
				nextDest = set[i];
			}
		}

		return nextDest;
	}

	private P2PNode findNodeWithPrefix(P2PNode[] set, P2PNode node, int start,
			int end, int prefix) {
		P2PNode nextDest = myID;
		int minPrefix = prefix;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = start; i < end && set[i] != null; i++) {
			int newPrefix = nodeID.prefixLength(set[i].getP2pID());
			int currDiff = nodeID.digitDifference(nextDest.getP2pID(),
					newPrefix + 1);
			int newDiff = nodeID.digitDifference(set[i].getP2pID(),
					newPrefix + 1);

			if (newPrefix > minPrefix
					|| (newPrefix == minPrefix && newDiff < currDiff)) {
				nextDest = set[i];
			}

		}

		return nextDest;
	}

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
		int minDiff = columnSize;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = 0; i < columnSize; i++) {
			if (routingTable[prefix][i] != null) {
				int newDiff = nodeID.digitDifference(routingTable[prefix][i]
						.getP2pID(), prefix);
				if (newDiff < minDiff) {
					nextDest = routingTable[prefix][i];
					minDiff = newDiff;
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
		int maxPrefix = prefix;
		int minDigitDiff = columnSize;
		P2PIdentifier nodeID = node.getP2pID();

		for (int i = 0; i < nextNodes.length; i++) {
			int newPrefix = nodeID.prefixLength(nextNodes[i].getP2pID());
			int newDigitDiff = nodeID.digitDifference(nextNodes[i].getP2pID(),
					newPrefix);
			if (newPrefix > maxPrefix
					|| (newPrefix == maxPrefix && newDigitDiff < minDigitDiff)) {
				maxPrefix = newPrefix;
				minDigitDiff = newDigitDiff;
				nextDest = nextNodes[i];
			}
		}

		return nextDest;
	}

}
