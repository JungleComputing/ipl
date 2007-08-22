/* $Id$ */

package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.spawnSync.InvocationRecord;

import java.io.IOException;

public final class ClusterAwareRandomWorkStealing extends
        LoadBalancingAlgorithm implements Protocol, Config {

    private Satin s;

    private boolean gotAsyncStealReply = false;

    private InvocationRecord asyncStolenJob = null;

    private IbisIdentifier asyncCurrentVictim = null;

    private long asyncStealStart;
    
    /**
     * This means we have sent an ASYNC request, and are waiting for the reply.
     * These are/should only (be) used in clientIteration.
     */
    private boolean asyncStealInProgress = false;

    public ClusterAwareRandomWorkStealing(Satin s) {
        super(s);
        this.s = s;
    }

    public InvocationRecord clientIteration() {
        Victim localVictim;
        Victim remoteVictim = null;
        boolean canDoAsync = true;
        InvocationRecord remoteJob = null;

        // First look if there was an outstanding WAN steal request that resulted
        // in a job.
        // check asyncStealInProgress, taking a lock is quite expensive..
        if (asyncStealInProgress) {
            synchronized (satin) {
            	boolean gotTimeout = System.currentTimeMillis() - asyncStealStart >= STEAL_WAIT_TIMEOUT;
            	if(gotTimeout && !gotAsyncStealReply) {
            		ftLogger.warn("SATIN '" + s.ident
                            + "': a timeout occurred while waiting for a wide-area steal reply from " + asyncCurrentVictim);
            	}

            	if (gotAsyncStealReply || gotTimeout) {
                    gotAsyncStealReply = false;
                    asyncStealInProgress = false;
                    asyncCurrentVictim = null;
                    remoteJob = asyncStolenJob;
                    asyncStolenJob = null;
                    asyncStealStart = 0;
                }
            }

            if (remoteJob != null) { // try a saved async job
                s.stats.asyncStealSuccess++;
                return remoteJob;
            }
        }

        // Else .. we are idle, try to steal a job.
        synchronized (satin) {
            localVictim = satin.victims.getRandomLocalVictim();
            if (localVictim != null) {
                satin.lb.setCurrentVictim(localVictim.getIdent());
            }
            if (!asyncStealInProgress) {
                remoteVictim = satin.victims.getRandomRemoteVictim();
                if (remoteVictim != null) {
                    asyncCurrentVictim = remoteVictim.getIdent();
                }
            }
            // Until we download the table, only the cluster coordinator can
            // issue wide-area steal requests 
            // @@@ why? --Rob
            if (satin.ft.getTable && !satin.clusterCoordinator) {
                canDoAsync = false;
            }
        }

        // Send an asynchronous wide-area steal request,
        // if not is outstanding
        // remoteVictim can be null on a single cluster run.
        if (remoteVictim != null && !asyncStealInProgress) {
            if (FT_NAIVE || canDoAsync) {
                asyncStealInProgress = true;
                s.stats.asyncStealAttempts++;
                try {
                	asyncStealStart = System.currentTimeMillis();
                	satin.lb.sendStealRequest(remoteVictim, false, false);
                } catch (IOException e) {
                    satin.commLogger.warn("Got exception during wa steal request: " + e);
                    // Ignore this?
                }
            }
        }

        // do a local steal, if possible (we might be the only node in this
        // cluster)
        if (localVictim != null) {
            return satin.lb.stealJob(localVictim, false);
        }

        return null;
    }

    public void stealReplyHandler(InvocationRecord ir, IbisIdentifier sender, int opcode) {
        switch (opcode) {
        case STEAL_REPLY_SUCCESS:
        case STEAL_REPLY_FAILED:
        case STEAL_REPLY_SUCCESS_TABLE:
        case STEAL_REPLY_FAILED_TABLE:
            satin.lb.gotJobResult(ir, sender);
            break;
        case ASYNC_STEAL_REPLY_SUCCESS:
        case ASYNC_STEAL_REPLY_FAILED:
        case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
        case ASYNC_STEAL_REPLY_FAILED_TABLE:
            synchronized (satin) {
            	if(sender.equals(asyncCurrentVictim)) {
            		gotAsyncStealReply = true;
            		asyncStolenJob = ir;
            		satin.notifyAll();
            	} else {
            		ftLogger.warn("SATIN '" + s.ident
                            + "': received an async job from a node that caused a timeout before.");
            		if(ir != null) {
            			s.q.addToTail(ir);
            		}
            	}
            }
            break;
        default:
            System.err.println("illigal opcode in CRS stealReplyHandler");
            System.exit(1);
        }
    }

    public void exit() {
        //wait for a pending async steal reply
        if (asyncStealInProgress) {
            stealLogger.info("waiting for a pending async steal reply from "
                + asyncCurrentVictim);
            synchronized (satin) {
                while (!gotAsyncStealReply) {
                    try {
                        satin.handleDelayedMessages(); //TODO move outside lock --Rob
                        satin.wait(250);
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            }
            if (ASSERTS && asyncStolenJob != null) {
                stealLogger.warn("Satin: CRS: EEK, stole async job "
                    + "after exiting!");
            }
        }
    }

    /**
     * Used in fault tolerance; check if the asynchronous steal victim crashed;
     * if so, cancel the steal request; if the job already arrived, remove it
     * (it should be aborted anyway, since it was stolen from a crashed machine)
     * if the owner of the asynchronously stolen job
     * crashed, abort the job.
     */
    public void handleCrash(IbisIdentifier crashedIbis) {
        Satin.assertLocked(satin);
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

        if (asyncStolenJob != null) {
            if (asyncStolenJob.getOwner().equals(crashedIbis)) {
                asyncStolenJob = null;
            }
        }
    }
}
