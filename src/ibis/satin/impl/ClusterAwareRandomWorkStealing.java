package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;

class ClusterAwareRandomWorkStealing extends Algorithm implements Protocol,
        Config {

    private boolean gotAsyncStealReply = false;

    private InvocationRecord asyncStolenJob = null;

    private IbisIdentifier asyncCurrentVictim = null;

    private long asyncStealAttempts = 0;

    private long asyncStealSuccess = 0;

    /**
     * This means we have sent an ASYNC request, and are waiting for the reply.
     * These are/should only (be) used in clientIteration.
     */
    private boolean asyncStealInProgress = false;

    ClusterAwareRandomWorkStealing(Satin s) {
        super(s);
    }

    public InvocationRecord clientIteration() {
        Victim localVictim;
        Victim remoteVictim = null;
        boolean canDoAsync = true;
        InvocationRecord remoteJob = null;

        // First look if there was an oustanding WAN steal request that resulted
        // in a job.
        //check asyncStealInProgress, taking a lock is quite expensive..
        if (asyncStealInProgress) {
            synchronized (satin) {
                if (gotAsyncStealReply) {
                    gotAsyncStealReply = false;
                    asyncStealInProgress = false;
                    asyncCurrentVictim = null;
                    remoteJob = asyncStolenJob;
                    asyncStolenJob = null;
                }
            }

            if (remoteJob != null) { //try a saved async job
                if (STEAL_STATS) {
                    asyncStealSuccess++;
                }
                return remoteJob;
            }
        }

        // else .. we are idle, try to steal a job.
        synchronized (satin) {
            localVictim = satin.victims.getRandomLocalVictim();
            if (localVictim != null) {
                satin.currentVictim = localVictim.ident;
            }
            if (!asyncStealInProgress) {
                remoteVictim = satin.victims.getRandomRemoteVictim();
                if (remoteVictim != null) {
                    asyncCurrentVictim = remoteVictim.ident;
                }
            }
            if (FAULT_TOLERANCE) {
                //until we download the table, only the cluster coordinator can
                // issue wide-area steal requests
                if (satin.getTable && !satin.clusterCoordinator) {
                    canDoAsync = false;
                }
            }
        }

        // Send an asynchronous wide-area steal request,
        // if not is outstanding
        // remoteVictim can be null on a single cluster run.
        if (remoteVictim != null && !asyncStealInProgress) {
            if (!FAULT_TOLERANCE || FT_NAIVE || canDoAsync) {
                asyncStealInProgress = true;
                if (STEAL_STATS)
                    asyncStealAttempts++;
                satin.sendStealRequest(remoteVictim, false, false);
            }
        }

        // do a local steal, if possible (we might be the only node in this
        // cluster)
        if (localVictim != null) {
            return satin.stealJob(localVictim, false);
        }

        return null;
    }

    public void stealReplyHandler(InvocationRecord ir, int opcode) {
        switch (opcode) {
        case STEAL_REPLY_SUCCESS:
        case STEAL_REPLY_FAILED:
        case STEAL_REPLY_SUCCESS_TABLE:
        case STEAL_REPLY_FAILED_TABLE:
            satin.gotJobResult(ir);
            break;
        case ASYNC_STEAL_REPLY_SUCCESS:
        case ASYNC_STEAL_REPLY_FAILED:
        case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
        case ASYNC_STEAL_REPLY_FAILED_TABLE:
            synchronized (satin) {
                gotAsyncStealReply = true;
                asyncStolenJob = ir;
                satin.notifyAll();
            }
            break;
        }
    }

    public void exit() {
        //wait for a pending async steal reply
        if (asyncStealInProgress) {
            System.err.println("waiting for a pending async steal reply from "
                    + asyncCurrentVictim);
            synchronized (satin) {
                while (!gotAsyncStealReply) {
                    try {
                        satin.wait();
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }
            if (ASSERTS && asyncStolenJob != null) {
                System.err.println("Satin: CRS: EEK, stole async job "
                        + "after exiting!");
            }
        }
    }

    public void printStats(java.io.PrintStream out) {
        out.println("SATIN '" + satin.ident.name()
                + "': ASYNC STEAL_STATS: attempts = " + asyncStealAttempts
                + " success = " + asyncStealSuccess + " ("
                + (((double) asyncStealSuccess / asyncStealAttempts) * 100.0)
                + " %)");

    }

    /**
     * Used in fault tolerance; if the owner of the asynchronously stolen job
     * crashed, abort the job
     */
    public void killOwnedBy(IbisIdentifier owner) {
        if (asyncStolenJob != null) {
            if (asyncStolenJob.owner.equals(owner)) {
                asyncStolenJob = null;
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
            SatinBase.assertLocked(satin);
        }
        if (crashedIbis.equals(asyncCurrentVictim)) {
            /*
             * current async victim crashed, reset the flag, remove the stolen
             * job
             */
            asyncStealInProgress = false;
            asyncStolenJob = null;
            asyncCurrentVictim = null;
            gotAsyncStealReply = false;
        }
    }
}