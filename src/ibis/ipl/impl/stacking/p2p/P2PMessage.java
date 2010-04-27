package ibis.ipl.impl.stacking.p2p;

import java.io.Serializable;
import java.util.Vector;

public class P2PMessage implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private Vector<P2PNode> dest;
	private int type;
	
	public final static int JOIN_REQUEST = 0;
	public final static int JOIN_RESPONSE = 1;
	public final static int REGULAR = 2;
	public final static int LEAF_UPDATE = 3;
	public final static int NEIGHBOR_UPDATE = 4;
	public final static int ROUTE_UPDATE = 5;
	
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
