package ibis.ipl.impl.stacking.p2p.util;


import java.io.Serializable;
import java.util.ArrayList;

public class P2PStateInfo implements Serializable{
	private static final long serialVersionUID = 1L;
	private P2PNode[][] routingTable;
	private ArrayList<P2PNode> leafSet, neighborhoodSet;
	private P2PNode source;
	private boolean sendBack;
	
	public P2PStateInfo(P2PNode source, P2PNode[][] routingTable, ArrayList<P2PNode> leafSet, ArrayList<P2PNode> neighborhoodSet, boolean sendBack) {
		this.source = source;
		this.routingTable = routingTable;
		this.leafSet = leafSet;
		this.neighborhoodSet = neighborhoodSet;
	}
	
	public synchronized void setRoutingTable(P2PNode[][] routingTable) {
		this.routingTable = routingTable;
	}
	public synchronized P2PNode[][] getRoutingTable() {
		return routingTable;
	}
	public synchronized void setLeafSet(ArrayList<P2PNode> leafSet) {
		this.leafSet = leafSet;
	}
	public synchronized ArrayList<P2PNode> getLeafSet() {
		return leafSet;
	}
	public synchronized void setNeighborhoodSet(ArrayList<P2PNode> neighborhoodSet) {
		this.neighborhoodSet = neighborhoodSet;
	}
	public synchronized ArrayList<P2PNode> getNeighborhoodSet() {
		return neighborhoodSet;
	}
	public synchronized void setSource(P2PNode source) {
		this.source = source;
	}
	public synchronized P2PNode getSource() {
		return source;
	}

	public synchronized boolean isSendBack() {
		return sendBack;
	}
	
	public synchronized void setSendBack(boolean sendBack) {
		this.sendBack = sendBack;
	}
}
