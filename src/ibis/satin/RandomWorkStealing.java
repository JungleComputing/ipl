package ibis.satin;

/** The random work-stealing distributed computing algorithm. */

final class RandomWorkStealing extends Algorithm implements Config {
    Satin satin;

    RandomWorkStealing(Satin s) {
	this.satin = s;
    }

    public void clientIteration() {
	Victim v;

		synchronized(satin) {
			v = satin.victims.getRandomVictim();
			/* Used for fault tolerance
			   we must know who the current victim is in case it crashes..
			*/
			if (FAULT_TOLERANCE) {
			    if (v != null) {
				satin.currentVictim = v.ident;
			    }
			}
		}
		if(v == null) return; //can happen with open world if nobody joined.
		satin.stealJob(v);
    }

    public void stealReplyHandler(InvocationRecord ir, int opcode) {
	synchronized(satin) {
	    satin.gotStealReply = true;
	    satin.stolenJob = ir;
	    satin.currentVictim = null;
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
