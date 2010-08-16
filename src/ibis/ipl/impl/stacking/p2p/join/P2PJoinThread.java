package ibis.ipl.impl.stacking.p2p.join;

import ibis.ipl.Ibis;
import ibis.ipl.ReadMessage;
import ibis.ipl.impl.stacking.p2p.P2PIbis;
import ibis.ipl.impl.stacking.p2p.util.P2PConfig;
import ibis.ipl.impl.stacking.p2p.util.P2PMessageHeader;
import ibis.ipl.impl.stacking.p2p.util.P2PNode;
import ibis.ipl.impl.stacking.p2p.util.P2PRoutingInfo;
import ibis.ipl.impl.stacking.p2p.util.P2PState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P2PJoinThread extends Thread {
	private PriorityBlockingQueue<P2PJoinRequest> queue;
	private boolean processNext, allowInterrupt;
	private P2PIbis myIbis;
	private P2PNode myID;
	private ArrayList<P2PNode> currentRequest;
	private Ibis baseIbis;
	private P2PState state;

	private static final Logger logger = LoggerFactory
			.getLogger(P2PJoinThread.class);

	/** path position constants **/
	public final static int NEW_NODE = 0;
	public final static int NEARBY_NODE = 1;

	public P2PJoinThread(P2PIbis myIbis) {
		processNext = false;
		allowInterrupt = false;
		queue = new PriorityBlockingQueue<P2PJoinRequest>();
		this.myIbis = myIbis;
		this.myID = myIbis.getMyID();
		this.baseIbis = myIbis.getBaseIbis();
		this.state = myIbis.getState();
	}

	@Override
	public void run() {
		while (true) {
			consume();
		}
	}

	public void consume() {
		try {
			P2PJoinRequest head = queue.take();
			processJoinRequest(head);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void processJoinRequest(P2PJoinRequest request)
			throws IOException, InterruptedException {

		allowInterrupt = false;
		
		ArrayList<P2PNode> path = request.getPath();

		// update current request
		setCurrentRequest(path);

		// append my ID
		path.add(myIbis.getMyID());

		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();
		dests.add(path.get(NEW_NODE));

		// for each destination, find next hop
		HashMap<P2PNode, ArrayList<P2PNode>> destinations = myIbis.route(dests);

		// for each next hop destination, construct a message and forward join
		// request
		Set<P2PNode> keys = destinations.keySet();
		Iterator<P2PNode> iter = keys.iterator();
		while (iter.hasNext()) {
			P2PNode nextHop = iter.next();
			if (nextHop.equals(myIbis.getMyID()) == false) {
				P2PMessageHeader msg = new P2PMessageHeader(destinations
						.get(nextHop), P2PMessageHeader.JOIN_REQUEST);
				// forward message to next hop
				nextHop.connect(baseIbis.createSendPort(P2PConfig.portType));
				nextHop.sendObjects(msg, path);

			} else {
				// process message, reverse message direction and append state
				reverseJoinDirection(path);
			}

		}

		// allow to interrupt job only when arrives here
		allowInterrupt = true;
		notifyAll();

		while (!processNext) {
			wait();
		}
		processNext = false;
	}

	private void reverseJoinDirection(ArrayList<P2PNode> path)
			throws IOException {
		int myPosition = path.indexOf(myID);
		P2PNode nextDest = path.get(myPosition - 1);

		// get node that issued the join request
		P2PNode newNode = path.get(NEW_NODE);

		// compute prefix length
		int prefix = newNode.prefixLength(myID);

		// create new P2PMessage
		P2PMessageHeader msg = new P2PMessageHeader(null,
				P2PMessageHeader.JOIN_RESPONSE);

		// create vector with routing table
		ArrayList<P2PRoutingInfo> routingTables = new ArrayList<P2PRoutingInfo>();
		P2PRoutingInfo myRoutingInfo = new P2PRoutingInfo(myID, state
				.getRoutingTableRow(prefix), prefix);
		routingTables.add(myRoutingInfo);

		// get leaf set
		ArrayList<P2PNode> leafSet = state.getLeafSet();

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

	@SuppressWarnings("unchecked")
	public void handleJoinResponse(ReadMessage readMessage) throws IOException,
			ClassNotFoundException {
		// read path
		ArrayList<P2PNode> path = (ArrayList<P2PNode>) readMessage.readObject();

		// read routing table
		ArrayList<P2PRoutingInfo> routingTables = (ArrayList<P2PRoutingInfo>) readMessage
				.readObject();

		// read leafSet
		ArrayList<P2PNode> leafSet = (ArrayList<P2PNode>) readMessage
				.readObject();

		int myPosition = -1;
		for (int i = 0; i < path.size(); i++) {
			P2PNode node = path.get(i);
			if (node.equals(myID)) {
				myPosition = i;
				break;
			}
		}

		// get newly joined node ID
		if (myPosition == 0) {
			// I am the new node
			// parse routing table, leaf set, neighborhood set
			ArrayList<P2PNode> neighborhoodSet = (ArrayList<P2PNode>) readMessage
					.readObject();
			readMessage.finish();

			state.parseSets(path, routingTables, leafSet, neighborhoodSet);

			// received join response, wake main thread
			myIbis.setJoined();

		} else {
			// append my routing table and forward message
			readMessage.finish();

			// get next destination
			P2PNode nextDest = path.get(myPosition - 1);

			// node that issued the request has position 0 in the path
			P2PNode newNode = path.get(NEW_NODE);

			// compute prefix length between my ID and new node ID
			int prefix = newNode.prefixLength(myID);
			P2PRoutingInfo myRoutingInfo = new P2PRoutingInfo(myID, state
					.getRoutingTableRow(prefix), prefix);
			routingTables.add(myRoutingInfo);

			nextDest.connect(baseIbis.createSendPort(P2PConfig.portType));

			// create new P2PMessage with type JOIN_RESPONSE
			P2PMessageHeader msg = new P2PMessageHeader(null,
					P2PMessageHeader.JOIN_RESPONSE);
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

	public void processDuplicate(P2PJoinRequest request) throws IOException {
		ArrayList<P2PNode> path = request.getPath();
		
		P2PMessageHeader header = new P2PMessageHeader(null, P2PMessageHeader.ALREADY_JOINED);
		for (P2PNode item: path) {
			item.connect(baseIbis.createSendPort(P2PConfig.portType));
			item.sendObjects(header, myID);
		}
	}
	
	@SuppressWarnings("unchecked")
	public synchronized void putJoinRequest(ReadMessage readMessage)
			throws IOException, ClassNotFoundException {

		// read current path
		ArrayList<P2PNode> newRequest = (ArrayList<P2PNode>) readMessage
				.readObject();
		readMessage.finish();

		P2PNode newRequestSource = newRequest.get(NEW_NODE);
		P2PJoinRequest request = new P2PJoinRequest(newRequestSource,
				newRequest);

		// check if queue already contains request
		if (newRequestSource.equals(myID)) {
			processDuplicate(request);
			return;
		}
		
		for (P2PJoinRequest item: queue) {
			if (item.getSource().equals(newRequestSource)) {
				return;
			}
		}

		// if new request is smaller than current request
		// interrupt current request and process new request
		if (currentRequest != null) {
			P2PNode currentRequestSource = currentRequest.get(NEW_NODE);
			if (currentRequestSource != null
					&& newRequestSource.compareTo(currentRequestSource) < 0) {

				try {
					while (!allowInterrupt) {
						wait();
					}
				} catch (InterruptedException ex) {
					// ignore
				}
				
				P2PJoinRequest oldRequest = new P2PJoinRequest(
						currentRequestSource, currentRequest);
				queue.put(oldRequest);
				setProcessNext(true);

			}
		}
		queue.put(request);
	}

	public synchronized void setProcessNext(P2PNode source, boolean processNext) {

		if (getCurrentRequest() != null) {
			P2PNode currentRequestSource = currentRequest.get(NEW_NODE);
			if (currentRequestSource.equals(source)) {
				this.processNext = processNext;
				notifyAll();
			}
		}
	}

	public synchronized void setProcessNext(ReadMessage readMessage) throws IOException, ClassNotFoundException {
		P2PNode source = (P2PNode) readMessage.readObject();
		readMessage.finish();
		
		setProcessNext(source, true);
	}
	public synchronized void setProcessNext(boolean processNext) {
		this.processNext = processNext;
		notifyAll();
	}

	public synchronized void resetProcessNext() {
		this.processNext = false;
	}

	public synchronized void setCurrentRequest(ArrayList<P2PNode> currentRequest) {
		this.currentRequest = currentRequest;
	}

	public synchronized ArrayList<P2PNode> getCurrentRequest() {
		return currentRequest;
	}
}
