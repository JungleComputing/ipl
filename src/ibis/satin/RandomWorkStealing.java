package ibis.satin;

/** The random work-stealing distributed computing algorithm. */

final class RandomWorkStealing extends Algorithm {
    Satin satin;

    RandomWorkStealing(Satin s) {
	this.satin = s;
    }

    public void clientIteration() {
//	InvocationRecord r;
	Victim v;

//	r = satin.q.getFromHead(); // Try the local queue
		
//	if(r != null) {
//		satin.callSatinFunction(r);
//	} else {
		/* We are idle. There is no work in the queue, and we are
		   not running Java code. Try to steal a job. */
		synchronized(satin) {
			v = satin.victims.getRandomVictim();
		}
		if(v == null) return; //can happen with open world if nobody joined.
		satin.stealJob(v);
//	}
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
