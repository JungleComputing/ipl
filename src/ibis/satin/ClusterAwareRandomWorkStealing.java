package ibis.satin;

import ibis.ipl.IbisIdentifier;

class ClusterAwareRandomWorkStealing extends Algorithm implements Protocol, Config {
    Satin satin;

    boolean gotAsyncStealReply = false;
    InvocationRecord asyncStolenJob = null;
    
    IbisIdentifier asyncCurrentVictim = null;
    boolean asyncCurrentVictimCrashed = false;

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
	boolean canDoAsync = true;

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
	    if (lv != null) {
		satin.currentVictim = lv.ident;
	    }
	    if(!asyncStealInProgress) {
		rv = satin.victims.getRandomRemoteVictim();
		if (rv != null) {
		    asyncCurrentVictim = rv.ident;
		}
	    } 		
	    if (FAULT_TOLERANCE) {
		//until we download the table, only the cluster coordinator can issue wide-area steal requests
		if (satin.getTable && !satin.clusterCoordinator) {
		    canDoAsync = false;
		}
	    } 
	}
		
	if(rv != null) {
	    if (!FAULT_TOLERANCE || canDoAsync) {		
		asyncStealInProgress = true;
		if(STEAL_STATS) asyncStealAttempts++;	    
		satin.sendStealRequest(rv, false, false);
	    }
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
	asyncCurrentVictim = null;
		
	if (STEAL_STATS && asyncStolenJob != null) {
	    asyncStealSuccess++;
	}
	asyncQ = asyncStolenJob;
    } 

    public void stealReplyHandler(InvocationRecord ir, int opcode) {
	switch(opcode) {
	case STEAL_REPLY_SUCCESS:
	case STEAL_REPLY_FAILED:
	case STEAL_REPLY_SUCCESS_TABLE:
	case STEAL_REPLY_FAILED_TABLE:
	    synchronized(satin) {
		satin.gotStealReply = true;
		satin.stolenJob = ir;
		satin.currentVictim = null;
		satin.notifyAll();
	    }
	    break;
	case ASYNC_STEAL_REPLY_SUCCESS:
	case ASYNC_STEAL_REPLY_FAILED:
	case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
	case ASYNC_STEAL_REPLY_FAILED_TABLE:
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
    
    /**
     * Used in fault tolerance; if the owner of the asynchronously stolen job crashed,
     * abort the job
     */
    public void killOwnedBy(IbisIdentifier owner) {
	if (asyncQ != null) {
	    if (asyncQ.owner.equals(owner)) {
		asyncQ = null;
	    }
	}
    }
    
    /**
     * Used in fault tolerance; check if the asynchronous steal victim crashed;
     * if so, cancel the steal request; if the job already arrived, remove it
     * (it should be aborted anyway, since it was stolen from a crashed machine)
     */
    public void checkAsyncVictimCrash(IbisIdentifier crashedIbis) {
	if (ASSERTS) {
	    Satin.assertLocked(satin);
	}
	if (crashedIbis.equals(asyncCurrentVictim)) {
	    /* current async victim crashed, reset the flag, remove the stolen job */
	    asyncStealInProgress = false;
	    asyncQ = null;
	}
    }
}
