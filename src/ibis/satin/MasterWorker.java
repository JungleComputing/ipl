package ibis.satin;

/** The master-worker distribution algorithm. */

final class MasterWorker extends Algorithm {
	Satin satin;

	MasterWorker(Satin s) {
		this.satin = s;
	}

	// @@@ cache the master victim in a variable here --Rob
	public void clientIteration() {
		InvocationRecord r;
		Victim v;

		if(satin.master) {
//			Thread.yield();
			return;
		}

		synchronized(satin) {
			v = satin.victims.getVictim(satin.masterIdent);
		}

		satin.stealJobBlocking(v);
	}

	public void stealReplyHandler(InvocationRecord ir, int opcode) {
		synchronized(satin) {
			satin.gotStealReply = true;
			satin.stolenJob = ir;
			satin.notifyAll();
		}
	}	

	void jobAdded() {
		synchronized(satin) {
			satin.notifyAll();
		}
	}

	public void exit() {
		synchronized(satin) {
			satin.notifyAll();
		}
	}

	public void printStats(java.io.PrintStream out) {
		// Satin already prints everything..
	}
}
