package ibis.ipl.impl.stacking.p2p;

import java.util.Comparator;

public class P2PIdentifierComparator implements Comparator<P2PInternalNode> {

	@Override
	public int compare(P2PInternalNode node1, P2PInternalNode node2) { 
		return node1.compareTo(node2);
	}

}
