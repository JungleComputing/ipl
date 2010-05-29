package ibis.ipl.impl.stacking.p2p.util;


import java.util.Comparator;

public class P2PIDDescComparator implements Comparator<P2PNode>{
	
	@Override
	public int compare(P2PNode node1, P2PNode node2) { 
		return node2.compareTo(node1);
	}
}
