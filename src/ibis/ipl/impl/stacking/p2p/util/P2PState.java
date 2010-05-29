package ibis.ipl.impl.stacking.p2p.util;

import ibis.ipl.Ibis;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PState {
	private P2PNode[] leafSet;
	private P2PNode[] neighborhoodSet;
	private P2PNode[][] routingTable;
	private P2PNode minLeaf, maxLeaf, minNeighbor, maxNeighbor;
	private int neighborhoodSize, leftLeafSize, rightLeafSize;
	private P2PNode myID;

	transient private Ibis baseIbis;

	// repair state related members
	private HashMap<Pair, ArrayList<P2PNode>> recvRoutingTable = new HashMap<Pair, ArrayList<P2PNode>>();

	private static final Logger logger = LoggerFactory
			.getLogger(P2PState.class);

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
	private synchronized boolean insertElement(P2PNode[] set, P2PNode node,
			int start, int end) throws IOException {
		int i;
		if (node == null) {
			return false;
		}
		if (myID.equals(node)) {
			return false;
		}
		for (i = start; i < end && set[i] != null; i++)
			if (set[i].equals(node))
				return false;
		if (i < end) {
			set[i] = new P2PNode(node);
			set[i].connect(baseIbis.createSendPort(P2PConfig.portType));
			set[i].setDistance(myID);

			return true;
		}
		return false;
	}

	/**
	 * replace an element in either neighborhood set or leaf set sort the
	 * elements based either on vivaldi or prefix distance
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
		if (myID.equals(node)) {
			return;
		}

		Arrays.sort(set, start, end, comparator);
		int position = Arrays.binarySearch(set, start, end, node, comparator);

		// node is not already in the set
		if (position < 0) {
			int insertPoint = -1 - position;
			if (insertPoint >= end) {
				insertPoint = end - 1;
			}
			for (int i = end - 1; i > insertPoint; i--) {
				set[i].copyObject(set[i - 1]);
			}
			set[insertPoint].copyObject(node, baseIbis
					.createSendPort(P2PConfig.portType));
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

		if (node.equals(myID)) {
			return;
		}

		int i = myID.prefixLength(node);
		int j = node.digit(i);

		if (routingTable[i][j] != null) {
			double oldDistance = myID.vivaldiDistance(routingTable[i][j]);
			double newDistance = myID.vivaldiDistance(node);

			// if new distance is larger than current distance, do not replace
			// entry
			if (newDistance >= oldDistance || routingTable[i][j].equals(node)) {
				return;
			}

			// TODO: use clone method
			routingTable[i][j].copyObject(node, baseIbis
					.createSendPort(P2PConfig.portType));
			routingTable[i][j].setDistance(myID);
		} else {
			routingTable[i][j] = new P2PNode(node);
			routingTable[i][j].connect(baseIbis
					.createSendPort(P2PConfig.portType));
			routingTable[i][j].setDistance(myID);
		}
	}

	public synchronized P2PNode getEntryAt(int i, int j) {
		return routingTable[i][j];
	}

	private synchronized P2PNode findNode(P2PNode[] set, P2PNode minNode,
			P2PNode maxNode, P2PNode node, int start, int end) {
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

	/**
	 * find node which is closest to myID and shares at leas a prefix of size
	 * prefix with myID
	 * 
	 * @param set
	 * @param node
	 * @param start
	 * @param end
	 * @param prefix
	 * @return
	 */
	private synchronized P2PNode findNodeWithPrefix(P2PNode[] set,
			P2PNode node, int start, int end, int prefix) {
		P2PNode nextDest = myID;
		BigInteger minDiff = myID.idDistance(node).abs();

		for (int i = start; i < end && set[i] != null; i++) {
			BigInteger newDiff = node.idDistance(set[i]).abs();
			int currPrefix = node.prefixLength(set[i]);
			if (newDiff.compareTo(minDiff) < 0 && currPrefix >= prefix
					&& !set[i].isFailed()) {
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

	/**
	 * find node in routing table which shares at least a prefix of length
	 * prefix compared to myID
	 * 
	 * @param node
	 * @param prefix
	 * @return
	 */
	public synchronized P2PNode findRoutingNode(P2PNode node, int prefix) {
		P2PNode nextDest = myID;
		BigInteger minDiff = P2PConfig.MAX;

		for (int i = 0; i < P2PConfig.MAX_DIGITS; i++) {
			if (routingTable[prefix][i] != null) {
				BigInteger newDiff = node.idDistance(routingTable[prefix][i]);
				int newPrefix = node.prefixLength(routingTable[prefix][i]);

				if (newDiff.compareTo(minDiff) < 0 && newPrefix >= prefix && !routingTable[prefix][i].isFailed()) {
					minDiff = newDiff;
					nextDest = routingTable[prefix][i];
				}
			}
		}

		return nextDest;
	}

	public synchronized P2PNode findNodeRareCase(P2PNode node, int prefix) {
		P2PNode[] nextNodes = new P2PNode[4];
		nextNodes[0] = findNodeWithPrefix(leafSet, node, 0,
				P2PConfig.LEAF_SIZE / 2, prefix);
		nextNodes[1] = findNodeWithPrefix(leafSet, node,
				P2PConfig.LEAF_SIZE / 2, P2PConfig.LEAF_SIZE, prefix);
		nextNodes[2] = findNodeWithPrefix(neighborhoodSet, node, 0,
				P2PConfig.NEIGHBOORHOOD_SIZE, prefix);
		nextNodes[3] = findRoutingNode(node, prefix);

		P2PNode nextDest = myID;

		logger.debug("Finding rare case node for:" + node);

		// FIXME: not ok, shouldn't forward to myself if I am not the
		// destination...
		// change find node rare case for normal messages
		// add flag to indicate normal or internal p2p message
		BigInteger minDiff = myID.idDistance(node).abs();

		for (int i = 0; i < nextNodes.length; i++) {
			BigInteger newDiff = node.idDistance(nextNodes[i]).abs();
			if (newDiff.compareTo(minDiff) < 0) {
				minDiff = newDiff;
				nextDest = nextNodes[i];
			}
		}

		return nextDest;
	}

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
	public synchronized void parseSets(Vector<P2PNode> path,
			Vector<P2PRoutingInfo> routingTables, P2PNode[] leafSet,
			P2PNode[] neighborhoodSet) throws IOException {

		int pathSize = path.size();
		// leaf node is the last element in the path array
		P2PNode leafNode = path.elementAt(pathSize - 1);

		// nearby node is the
		P2PNode nearbyNode = path.elementAt(1);

		logger.debug("Leaf node:" + leafNode.getIbisID().name());
		logger.debug("Nearby node:" + nearbyNode.getIbisID().name());

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
			logger.debug("Path element " + i + ":"
					+ path.elementAt(i).getIbisID().name());
		}

		// add neighbor node the nearby node
		addNeighborNode(nearbyNode);

		// add leaf node the last node
		addLeafNode(leafNode);

		P2PMessage msg = new P2PMessage(null, P2PMessage.STATE_REQUEST);
		P2PStateInfo myStateInfo = new P2PStateInfo(myID, getRoutingTable(),
				getLeafSet(), getNeighborhoodSet(), true);

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

	public synchronized void updateState(P2PStateInfo stateInfo)
			throws IOException {
		P2PNode source = stateInfo.getSource();
		P2PNode[][] routingTable = stateInfo.getRoutingTable();
		P2PNode[] leafSet = stateInfo.getLeafSet();
		P2PNode[] neighborhoodSet = stateInfo.getNeighborhoodSet();

		logger.debug("State update received from " + source
				+ source.getIbisID().name());

		int prefix = myID.prefixLength(source);

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
			P2PStateInfo myStateInfo = new P2PStateInfo(myID,
					getRoutingTable(), getLeafSet(), getNeighborhoodSet(),
					false);
			// send my state
			source.connect(baseIbis.createSendPort(P2PConfig.portType));
			source.sendObjects(msg, myStateInfo);
		}

		printSets();
	}

	public synchronized P2PNode[][] getRoutingTable() {
		return this.routingTable;
	}

	public void setBaseIbis(Ibis baseIbis) {
		this.baseIbis = baseIbis;
	}

	public Ibis getBaseIbis() {
		return baseIbis;
	}

	public synchronized void printSets() {
		logger.debug(myID.toString());
		for (int i = 0; i < leafSet.length; i++) {
			if (leafSet[i] != null) {
				logger.debug(myID + " leafset " + i + " " + leafSet[i] + " "
						+ leafSet[i].getIbisID().name());

			}
		}

		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				logger.debug(myID + " neighbor " + i + " " + neighborhoodSet[i]
						+ " " + neighborhoodSet[i].getIbisID().name());

			}
		}

		System.out.println();
		for (int i = 0; i < 5; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (routingTable[i][j] != null) {
					logger.debug(myID + " routing " + i + " " + j + " "
							+ routingTable[i][j] + " "
							+ routingTable[i][j].getIbisID().name());
				}
			}
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

	public P2PNode findNearbyNode(P2PNode source) {
		P2PNode nearbyNode = myID;
		double minDistance = myID.vivaldiDistance(source);

		// search in leaf set
		for (int i = 0; i < leafSet.length; i++) {
			if (leafSet[i] != null) {
				double distance = source.vivaldiDistance(leafSet[i]);
				if (distance < minDistance) {
					nearbyNode = leafSet[i];
				}
			}
		}

		// search in neighborhood set
		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				double distance = source.vivaldiDistance(neighborhoodSet[i]);
				if (distance < minDistance) {
					nearbyNode = neighborhoodSet[i];
				}
			}
		}

		// search in routing table
		for (int i = 0; i < P2PConfig.MAX_PREFIX; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (routingTable[i][j] != null) {
					double distance = source
							.vivaldiDistance(routingTable[i][j]);
					if (distance < minDistance) {
						nearbyNode = routingTable[i][j];
					}
				}
			}
		}

		return nearbyNode;
	}

	public synchronized void pingNodes() throws IOException {
		P2PMessage msg = new P2PMessage(null, P2PMessage.STATE_REQUEST);
		for (int i = 0; i < leafSet.length; i++) {
			if (leafSet[i] != null) {
				leafSet[i].sendObjects(msg, myID, P2PStateRepairThread.LEAF, i,
						0);
			}
		}

		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				neighborhoodSet[i].sendObjects(msg, myID,
						P2PStateRepairThread.NEIGHBOR, i, 0);
			}
		}

		for (int i = 0; i < P2PConfig.MAX_PREFIX; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (routingTable[i][j] != null) {
					routingTable[i][j].sendObjects(msg, myID,
							P2PStateRepairThread.ROUTING, i, j, 0);
				}
			}
		}
	}

	public synchronized void handlePing(int type, int i, int j) {
		switch (type) {
		case P2PStateRepairThread.LEAF:
			leafSet[i].setAckTime();
			break;
		case P2PStateRepairThread.NEIGHBOR:
			neighborhoodSet[i].setAckTime();
			break;
		case P2PStateRepairThread.ROUTING:
			routingTable[i][j].setAckTime();
			break;
		}
	}

	/**
	 * check sets from time to time for last ack time, if timeout exceeds
	 * threshold, replace entry
	 */
	public void checkNodes() {
		// TODO: maybe create a new thread which performs the replacement?
		for (int i = 0; i < leafSet.length; i++) {
			if (leafSet[i] != null) {
				if (leafSet[i].getAckTimeout() > P2PConfig.ACK_THRESHOLD) {
					// replace leaf entry
					replaceLeafEntry(i);
				}
			}
		}

		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				if (neighborhoodSet[i].getAckTimeout() > P2PConfig.ACK_THRESHOLD) {
					// repair neighbor entry
					replaceNeighBorEntry(i);
				}
			}
		}

		for (int i = 0; i < P2PConfig.MAX_PREFIX; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (routingTable[i][j].getAckTimeout() > P2PConfig.ACK_THRESHOLD) {
					// repair routing entry
					replaceRoutingEntry(i, j);
				}
			}
		}

	}

	/**
	 * replace failed routing table entry at position i, j
	 * 
	 * @param i
	 * @param j
	 */
	private synchronized void replaceRoutingEntry(int prefix, int digit) {
		// contact nodes R(i, k), k not equal with j for a replacement (give, id
		// + prefix + digit)
		// wait for a reply, if no response
		// contact R(i + 1, k) k not equal with j for a replacement ...
		// wait for a reply, if no response repeat procedure for rows i+2, i+3,
		// ..

		P2PNode oldEntry = new P2PNode(routingTable[prefix][digit]);
		P2PMessage msg = new P2PMessage(null, P2PMessage.ROUTE_REQUEST);
		for (int i = prefix; i < P2PConfig.MAX_PREFIX; i++) {
			for (int j = 0; j < P2PConfig.MAX_DIGITS; j++) {
				if (j != digit && routingTable[i][j] != null) {
					routingTable[i][j].sendObjects(msg, myID, prefix, digit);
				}
			}

			try {
				wait(P2PConfig.REPAIR_TIMEOUT);
			} catch (InterruptedException e) {
				// ignore
			}

			// if the entry has been replaced, return
			if (!oldEntry.equals(routingTable[prefix][digit])) {
				return;
			}
		}
	}

	/**
	 * replace failed neighbor entry at position i
	 * 
	 * @param i
	 */
	private synchronized void replaceNeighBorEntry(int position) {
		// contact nearby nodes, wait some timeout until response received
		P2PMessage msg = new P2PMessage(null, P2PMessage.NEIGHBOR_REQUEST);
		for (int i = 0; i < neighborhoodSet.length; i++) {
			if (neighborhoodSet[i] != null) {
				neighborhoodSet[i].sendObjects(msg, myID, position);
			}
		}
	}

	/**
	 * replace leaf entry at position i
	 * 
	 * @param i
	 */
	private synchronized void replaceLeafEntry(int position) {
		int start, end, side;
		if (position < P2PConfig.LEAF_SIZE / 2) {
			start = 0;
			end = P2PConfig.LEAF_SIZE / 2;
			side = P2PStateRepairThread.LEAF_LEFT;
		} else {
			start = P2PConfig.LEAF_SIZE / 2;
			end = P2PConfig.LEAF_SIZE;
			side = P2PStateRepairThread.LEAF_RIGHT;
		}

		P2PMessage msg = new P2PMessage(null, P2PMessage.LEAF_REQUEST);
		for (int i = start; i < end; i++) {
			if (leafSet[i] != null) {
				leafSet[i].sendObjects(msg, myID, side, position);
			}
		}
	}

	public synchronized ArrayList<P2PNode> getLeafSet(int type) {
		P2PNode[] repairLeafSet = new P2PNode[P2PConfig.LEAF_SIZE / 2];

		if (type == P2PStateRepairThread.LEAF_LEFT) {
			// failed node is on the left side, send right part of the leaf set
			System.arraycopy(leafSet, P2PConfig.LEAF_SIZE / 2, repairLeafSet,
					0, P2PConfig.LEAF_SIZE / 2);
		} else {
			// failed node is on the right side, send left part of the leaf set
			System.arraycopy(leafSet, P2PConfig.LEAF_SIZE / 2, repairLeafSet,
					0, P2PConfig.LEAF_SIZE / 2);
		}

		return new ArrayList<P2PNode>(Arrays.asList(repairLeafSet));
	}

	public synchronized void repairNeighborhoodSet(P2PNode[] neighborhoodSet,
			int position) {
		double minDiff = Double.MAX_VALUE;
		P2PNode replacement = null;
		for (P2PNode neighbor : neighborhoodSet) {
			if (neighbor != null && !neighbor.equals(myID)
					&& !neighbor.equals(neighborhoodSet[position])) {
				double newDiff = myID.vivaldiDistance(neighbor);
				if (newDiff < minDiff) {
					replacement = neighbor;
				}
			}
		}

		this.neighborhoodSet[position] = replacement;
		minDiff = myID.vivaldiDistance(minNeighbor);
		double maxDiff = myID.vivaldiDistance(maxNeighbor);
		double newDiff = myID.vivaldiDistance(replacement);

		// update min and max neighbor
		if (newDiff < minDiff) {
			minNeighbor = replacement;
		}
		if (newDiff > maxDiff) {
			maxNeighbor = replacement;
		}
	}

	public synchronized void repairLeafSet(P2PNode[] leafSet, int position) {
		P2PNode replacement = null;
		BigInteger minDiff = P2PConfig.MAX;
		for (P2PNode leaf : leafSet) {
			if (leaf != null && !leaf.equals(myID)
					&& !leaf.equals(leafSet[position])) {
				BigInteger newDiff = myID.idDistance(leaf);
				if (newDiff.compareTo(minDiff) < 0) {
					minDiff = newDiff;
					replacement = leaf;
				}
			}
		}

		leafSet[position] = replacement;

		if (replacement.compareTo(minLeaf) < 0) {
			minLeaf = replacement;
		}
		if (replacement.compareTo(maxLeaf) > 0) {
			maxLeaf = replacement;
		}
	}

	public synchronized void repairRoutingEntry(P2PNode entry, int prefix,
			int digit) {
		double oldDiff = myID.vivaldiDistance(routingTable[prefix][digit]);
		double newDiff = myID.vivaldiDistance(entry);

		if (!entry.equals(routingTable[prefix][digit]) && newDiff < oldDiff) {
			routingTable[prefix][digit] = entry;
		}
	}
}
