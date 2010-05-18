package ibis.ipl.impl.stacking.p2p;

import ibis.ipl.IbisIdentifier;

import java.io.Serializable;

/**
 * identifier in overlay network
 */
public class P2PIdentifier implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String p2pID;

	public P2PIdentifier(IbisIdentifier ibisID) {
		p2pID = P2PHashTools.MD5(ibisID.name());
	}
	
	public P2PIdentifier(String p2pID) {
		this.p2pID = p2pID;
	}
	
	public synchronized void setP2pID(String p2pID) {
		this.p2pID = p2pID;
	}

	public synchronized String getP2pID() {
		return p2pID;
	}
	
	/**
	 * compute longest common prefix of two p2p identifiers
	 * @param p2pNode
	 * @return
	 */
	public synchronized int prefixLength(P2PIdentifier p2pNode) 
	{
		int i = 0;
		String p2pOtherID = p2pNode.getP2pID();
		
		//System.out.println("MyID: " + p2pID + " OtherID: " + p2pOtherID);
		for (i = 0; i < p2pOtherID.length() && i < p2pID.length(); i++) {
			if (p2pOtherID.charAt(i) != p2pID.charAt(i)) {
				break;
			}
		}
		
		return i;
	}
	
	public synchronized int digitDifference(P2PIdentifier p2pNode, int i) {
		String otherP2PID = p2pNode.getP2pID();
		if (i >= p2pID.length()) {
			return i;
		}
		
		return Math.abs(otherP2PID.charAt(i) - p2pID.charAt(i));
	}
	
	public synchronized char charAt(int i) {
		return p2pID.charAt(i);
	}
	
	public synchronized int compareTo(P2PIdentifier p2pNode) {
		String p2pOtherID = p2pNode.getP2pID();
		return p2pID.compareTo(p2pOtherID);
	}
}
