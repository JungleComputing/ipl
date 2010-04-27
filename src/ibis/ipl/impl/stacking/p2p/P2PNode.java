package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.support.vivaldi.Coordinates;

import java.io.Serializable;

/**
 * encapsulation of node identification in overlay network node ID - ID in
 * overlay network - compute SHA-1 of IP address basis node ID - ID in
 * underlying network distance according with proximity metric
 * 
 * @author delia
 * 
 */
public class P2PNode implements Serializable, Comparable<P2PNode>{
	private static final long serialVersionUID = 1L;
	private IbisIdentifier ibisID;
	private Coordinates coords;
	private P2PIdentifier p2pID;
	private double distance;
	
	public void setIbisID(IbisIdentifier ibisID) {
		this.ibisID = ibisID;
	}

	public IbisIdentifier getIbisID() {
		return ibisID;
	}

	public void setCoords(Coordinates coords) {
		this.coords = coords;
	}

	public Coordinates getCoords() {
		return coords;
	}

	public void setP2pID(P2PIdentifier p2pID) {
		this.p2pID = p2pID;
	}

	public P2PIdentifier getP2pID() {
		return p2pID;
	}

	@Override
	public int compareTo(P2PNode o) {
		return p2pID.compareTo(o.getP2pID());
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public double getDistance() {
		return distance;
	}
}
