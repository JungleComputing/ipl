package ibis.ipl.impl.stacking.p2p.util;

import java.io.Serializable;
import java.util.ArrayList;

public class P2PRoutingInfo implements Serializable{
	private static final long serialVersionUID = 1L;
	private P2PNode source;
	private ArrayList<P2PNode> routingRow;
	private int prefix;
	
	public P2PRoutingInfo(P2PNode source, ArrayList<P2PNode> routingRow, int prefix) {
		this.source = source;
		this.routingRow = routingRow;
		this.prefix = prefix;
	}
	
	public synchronized P2PNode getSource() {
		return source;
	}
	
	public synchronized  ArrayList<P2PNode> getRoutingRow() {
		return routingRow;
	}
	
	public synchronized int getPrefix() {
		return prefix;
	}
}
