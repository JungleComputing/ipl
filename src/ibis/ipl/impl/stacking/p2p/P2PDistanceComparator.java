package ibis.ipl.impl.stacking.p2p;

import java.util.Comparator;

public class P2PDistanceComparator implements Comparator<P2PInternalNode>{

	@Override
	public int compare(P2PInternalNode node1, P2PInternalNode node2) {
		double distance1 = node1.getNode().getDistance();
		double distance2 = node2.getNode().getDistance();
		double diff = Math.pow(distance1 - distance2, 1000);
	
		return (int) diff;
		
	}

}
