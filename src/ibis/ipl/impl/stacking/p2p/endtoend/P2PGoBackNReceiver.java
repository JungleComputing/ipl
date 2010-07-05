package ibis.ipl.impl.stacking.p2p.endtoend;

import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.impl.stacking.p2p.P2PIbis;
import ibis.ipl.impl.stacking.p2p.P2PReceivePort;
import ibis.ipl.impl.stacking.p2p.util.P2PNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * remember per connection (a connection is identified by SendPort + ReceivePort)
 * last in order recv packet
 * send acks per connection
 * @author Delia
 *
 */
public class P2PGoBackNReceiver {
	
	private Map<SendPortIdentifier, P2PRecvConnectionHandler> connections;
	private P2PIbis ibis;
	private P2PReceivePort receivePort;
	
	private static final Logger logger = LoggerFactory.getLogger(P2PGoBackNReceiver.class);
	
	public P2PGoBackNReceiver(P2PIbis ibis, P2PReceivePort receivePort) {
		this.ibis = ibis;
		this.receivePort = receivePort;
		
		connections = new HashMap<SendPortIdentifier, P2PRecvConnectionHandler>();
	}
	
	public void addConnection(SendPortIdentifier sid) {
		connections.put(sid, new P2PRecvConnectionHandler());
		
		logger.debug("Connection from " + sid.ibisIdentifier() + " was added.");
	}
	
	public void processMessage(P2PMessage message) {
		// deliver message to upper layer only if sequence number in order
		P2PRecvConnectionHandler connection = connections.get(message.getSid());
		
		logger.debug("Processing message from " + message.getSid().ibisIdentifier());
		logger.debug("Connections: " + connections.size());
		
		if (connection.processSeqNum(message.getSeqNum())) {
			// deliver to upper layer
			receivePort.deliverMessage(message.getSid(), message.getContent());
		} 
		
		int expectedSeqNum = connection.getSeqNum();
		
		// send back expected seqNum;
		ArrayList<P2PNode> dests = new ArrayList<P2PNode>();
		dests.add(new P2PNode(message.getSid().ibisIdentifier()));
		
		ibis.forwardMessageAck(dests, message.getSid(), receivePort.identifier(), expectedSeqNum);
	}
}
