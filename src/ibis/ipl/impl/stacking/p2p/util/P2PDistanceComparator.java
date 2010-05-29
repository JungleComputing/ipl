package ibis.ipl.impl.stacking.p2p.util;


import java.util.Comparator;

public class P2PDistanceComparator implements Comparator<P2PNode>{

	@Override
	public int compare(P2PNode node1, P2PNode node2) {
		double distance1 = node1.getDistance();
		double distance2 = node2.getDistance();
		double diff = Math.pow(distance1 - distance2, 1000);
	
		return (int) diff;
		
	}

}
