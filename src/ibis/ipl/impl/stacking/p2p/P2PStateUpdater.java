package ibis.ipl.impl.stacking.p2p;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;

public class P2PStateUpdater extends Thread {
	private P2PState state;
	private final BlockingQueue<P2PStateInfo> queue;

	public P2PStateUpdater(BlockingQueue<P2PStateInfo> queue, P2PState state) {
		this.queue = queue;
		this.state = state;
	}

	@Override
	public void run() {
		try {
			while (!isInterrupted()) {
				P2PStateInfo info = queue.take();
				consume(info);
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			// ignore for now
		}

	}

	private synchronized void consume(P2PStateInfo stateInfo) throws IOException {
		state.updateState(stateInfo);
	}
}
