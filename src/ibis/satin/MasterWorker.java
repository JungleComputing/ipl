package ibis.satin;

final class MasterWorker implements Algorithm {
	Satin satin;

	MasterWorker(Satin s) {
		this.satin = s;
	}

	public void clientIteration() {
		InvocationRecord r;
		Victim v;

		if(satin.master) {
			Thread.yield();
			return;
		}

		synchronized(satin) {
			v = satin.victims.getVictim(satin.masterIdent);
		}

		satin.stealJob(v);
	}

	public void stealReplyHandler(InvocationRecord ir, int opcode) {
		synchronized(satin) {
			satin.gotStealReply = true;
			satin.stolenJob = ir;
			satin.notifyAll();
		}
	}	

	public void exit() {
		// Everything's synchronous, we don't have to wait for/do anything
	}

	public void printStats(java.io.PrintStream out) {
		// Satin already prints everything..
	}
}
