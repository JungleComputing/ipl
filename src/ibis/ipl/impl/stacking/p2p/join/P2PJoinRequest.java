package ibis.ipl.impl.stacking.p2p.join;

import ibis.ipl.impl.stacking.p2p.util.P2PNode;

import java.util.ArrayList;

public class P2PJoinRequest implements Comparable<P2PJoinRequest>{
	private P2PNode source;
	private ArrayList<P2PNode> path;
	
	public P2PJoinRequest(P2PNode source, ArrayList<P2PNode> path) {
		this.setSource(source);
		this.setPath(path);
	}
	
	public int compareTo(P2PJoinRequest other) {
		return source.compareTo(other.getSource());
	}

	public void setSource(P2PNode source) {
		this.source = source;
	}

	public P2PNode getSource() {
		return source;
	}

	public void setPath(ArrayList<P2PNode> path) {
		this.path = path;
	}

	public ArrayList<P2PNode> getPath() {
		return path;
	}
}
