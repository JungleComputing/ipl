package ibis.satin;

class ClusterAwareRandomWorkStealing extends Algorithm implements Protocol, Config {
    Satin satin;

    boolean gotAsyncStealReply = false;
    InvocationRecord asyncStolenJob = null;

    private long asyncStealAttempts = 0;
    private long asyncStealSuccess = 0;

    ClusterAwareRandomWorkStealing(Satin s) {
	this.satin = s;
    }

    /**
     * This means we have sent an ASYNC request, and are waiting for
     * the reply. These are/should only (be) used in clientIteration.
     */
    private boolean asyncStealInProgress = false;
    private InvocationRecord asyncQ = null;

    public void clientIteration() {
	InvocationRecord todo;
	Victim lv;
	Victim rv = null;

	//check asyncStealInProgress, taking a lock is quite expensive..
	if (asyncStealInProgress) {
	    synchronized(this) {
		if (gotAsyncStealReply) {
		    processAsyncStolenJob();
		}
	    }
	}
		
	//		todo = satin.q.getFromHead(); //try the local queue
	//		if (todo != null) {
	//			satin.callSatinFunction(todo);
	//			return;
	//		} else 
	if (asyncQ != null) { //try a saved async job

	    /**
	     * When we select a job to be run, it must be removed from any
	     * queue before it is given to callSatinFunction (above),
	     * because in the recursion we may get here before
	     * callSatinFunction returns and then we see the job is still in
	     * the queue :-|
	     * q.getFromHead does this automatically, but asyncQ has to be made
	     * null explicitly!
	     */
	    todo = asyncQ;
	    asyncQ = null;
	    satin.callSatinFunction(todo);
	    return;
	}

	/* else .. we are idle. There is no work in any queue, and we are
	   not running Java code. Try to steal a job. */

	synchronized(satin) {
	    lv = satin.victims.getRandomLocalVictim();
	    if(!asyncStealInProgress) {
		rv = satin.victims.getRandomRemoteVictim();
	    }
	}
		
	if(rv != null) {
	    asyncStealInProgress = true;
	    if(STEAL_STATS) asyncStealAttempts++;
	    satin.sendStealRequest(rv, false, false);
	}

	if (lv != null) {
	    satin.stealJob(lv);
	} else {
	    if (satin.upcalls) { //only wait when there are upcalls!

		if (ASSERTS && !asyncStealInProgress) {
		    System.err.println("CRS: EEK! I'm idle and no steal reqs" +
				       " have been sent");
		}
				
		synchronized(this) {
		    while (!gotAsyncStealReply) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    //ignore
			}
		    }
		    // process the received job here, it could be delayed until
		    // the next iteration but then we'd have to lock again
		    processAsyncStolenJob();
		}
	    }
	}
    }

    //recurring piece of code in clientIteration:
    final private void processAsyncStolenJob() {
	gotAsyncStealReply = false;
	asyncStealInProgress = false;
		
	if (STEAL_STATS && asyncStolenJob != null) {
	    asyncStealSuccess++;
	}
	asyncQ = asyncStolenJob;
    } 

    public void stealReplyHandler(InvocationRecord ir, int opcode) {
	switch(opcode) {
	case STEAL_REPLY_SUCCESS:
	case STEAL_REPLY_FAILED:
	    synchronized(satin) {
		satin.gotStealReply = true;
		satin.stolenJob = ir;
		satin.notifyAll();
	    }
	    break;
	case ASYNC_STEAL_REPLY_SUCCESS:
	case ASYNC_STEAL_REPLY_FAILED:
	    synchronized(this) {
		gotAsyncStealReply = true;
		asyncStolenJob = ir;
		notifyAll();
	    }
	    break;
	}
    }

    public void exit() {
	//wait for a pending async steal reply
	if (asyncStealInProgress) {
	    System.err.println("waiting for a pending async steal reply...");
	    synchronized(this) {
		while (!gotAsyncStealReply) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			//ignore
		    }
		}
	    }
	    if (ASSERTS && asyncStolenJob != null) {
		System.err.println("Satin: CRS: EEK, stole async job " +
				   "after exiting!");
	    }
	}
    }

    public void printStats(java.io.PrintStream out) {
	out.println("SATIN '" + satin.ident.name() + 
		    "': ASYNC STEAL_STATS: attempts = " +asyncStealAttempts +
		    " success = " +	asyncStealSuccess + " (" +
		    (((double) asyncStealSuccess / asyncStealAttempts) * 100.0)
		    + " %)");

    }		
}
