package ibis.ipl.impl.stacking.p2p.util;

import java.io.IOException;

/**
 * from time to time contact the nodes from all the sets and send a ping message
 * if one is not reachable, replace it according with the Pastry state repair
 * procedure
 * 
 * @author delia
 * 
 */
public class P2PStateRepairThread extends Thread {
	private P2PState state;

	public final static int LEAF = 0;
	public final static int NEIGHBOR = 1;
	public final static int ROUTING = 2;
	public final static int LEAF_LEFT = 3;
	public final static int LEAF_RIGHT = 4;
	
	public P2PStateRepairThread(P2PState state) {
		this.state = state;
	}
	
	public void run() {
		try {
			state.pingNodes();
			state.checkNodes();
			
			Thread.sleep(60000);
		} catch (IOException ex) {
			// ignore
		} catch (InterruptedException e) {
			// ignore
		}

	}
}
