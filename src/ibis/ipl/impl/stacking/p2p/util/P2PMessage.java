package ibis.ipl.impl.stacking.p2p.util;

import ibis.ipl.impl.stacking.p2p.P2PNode;

import java.io.Serializable;
import java.util.UUID;
import java.util.Vector;

public class P2PMessage implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private Vector<P2PNode> dest;
	private int type;
	
	public final static int JOIN_REQUEST = 0;
	public final static int JOIN_RESPONSE = 1;
	public final static int REGULAR = 2;
	public final static int STATE_REQUEST = 3;
	public final static int STATE_RESPONSE = 4;
	public final static int CONNECTION_REQUEST = 6;
	public final static int CONNECTION_RESPONSE = 7;
	public final static int NEARBY_REQUEST = 8;
	public final static int NEARBY_RESPONSE = 9;
	public final static int NEARBY_NOT_JOINED = 10;
	
	// logger specific messages
	public final static int NODE_DEPARTURE = 11;
	public final static int NODE_JOIN = 12;
	public final static int MESSAGE_FORWARD = 13;
	public final static int MESSAGE_ADD = 14;
	public final static int MESSAGE_DELETE = 15;
	public final static int LOGGER = 16;
	
	// state update specific messages
	public final static int PING_REQUEST = 17;
	public final static int PING_RESPONSE = 18;
	public final static int NEIGHBOR_REQUEST = 19;
	public final static int NEIGHBOR_RESPONSE = 20;
	public final static int LEAF_REQUEST = 21;
	public final static int LEAF_RESPONSE = 22;
	public final static int ROUTE_REQUEST = 23;
	public final static int ROUTE_RESPONSE = 24;
	
	// unique message identifier 
	public final String MESSAGE_ID = UUID.randomUUID().toString();
	
	public P2PMessage(Vector<P2PNode> dest, int type) {
		this.setDest(dest);
		this.setType(type);
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public void addDestination(P2PNode newDest) {
		getDest().add(newDest);
	}

	public void setDest(Vector<P2PNode> dest) {
		this.dest = dest;
	}

	public Vector<P2PNode> getDest() {
		return dest;
	}
}
