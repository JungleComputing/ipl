package ibis.ipl.impl.stacking.p2p.util;

import java.io.IOException;
import java.net.SocketException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private transient static final Logger logger = LoggerFactory.getLogger(P2PStateRepairThread.class);
	
	public P2PStateRepairThread(P2PState state) {
		this.state = state;
	}

	public void run() {
		try {
			while (!isInterrupted()) {
				//state.printSets();
				state.pingNodes();
				state.checkNodes();

				logger.debug("Checked nodes...");
				
				Thread.sleep(10000);
			}
		} catch (SocketException ex) {
			// ignore
		} catch (IOException ex) {
			// ignore
		} catch (InterruptedException e) {
			// ignore
		}
	}
}
