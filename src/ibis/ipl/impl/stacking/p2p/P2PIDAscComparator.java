package ibis.ipl.impl.stacking.p2p;

import java.util.Comparator;

public class P2PIDAscComparator implements Comparator<P2PNode> {

	@Override
	public int compare(P2PNode node1, P2PNode node2) { 
		return node1.compareTo(node2);
	}

}
