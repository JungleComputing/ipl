package ibis.ipl.impl.stacking.p2p.util;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Vector;

public class P2PMessageHeader implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private ArrayList<P2PNode> dest;
	private int type;
	
	// internal peer to peer specific messages
	public final static int JOIN_REQUEST = 0;
	public final static int JOIN_RESPONSE = 1;
	public final static int REGULAR = 2;
	public final static int REGULAR_ACK = 3;
	public final static int STATE_REQUEST = 4;
	public final static int STATE_RESPONSE = 5;
	public final static int CONNECTION_REQUEST = 6;
	public final static int CONNECTION_RESPONSE = 7;
	public final static int NEARBY_REQUEST = 8;
	public final static int NEARBY_RESPONSE = 9;
	public final static int NEARBY_NOT_JOINED = 10;
	public final static int ALREADY_JOINED = 27;
	
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
	
	// tracker specific messages
	public final static int GET_IBISES = 25;
	public final static int REGISTER_IBIS = 26;
	
	// unique message identifier 
	public final String MESSAGE_ID = UUID.randomUUID().toString();
	
	public P2PMessageHeader(ArrayList<P2PNode> dest, int type) {
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

	public void setDest(ArrayList<P2PNode> dest) {
		this.dest = dest;
	}

	public ArrayList<P2PNode> getDest() {
		return dest;
	}
}
