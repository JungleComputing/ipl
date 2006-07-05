/*
 * Created on Jun 20, 2006 by rob
 */
package ibis.satin.impl.loadBalancing;

import java.io.IOException;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.IRVector;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;
import ibis.satin.impl.spawnSync.Stamp;

public class LoadBalancing implements Config {
    private LBCommunication lbComm;

    private Satin s;

    private volatile boolean receivedResults = false;

    /** Used to store reply messages. */
    private volatile boolean gotStealReply = false;

    private InvocationRecord stolenJob = null;

    private final IRVector resultList;

    /**
     * Used for fault tolerance, we must know who the current victim is,
     * in case it crashes.
     */
    private IbisIdentifier currentVictim = null;

    public LoadBalancing(Satin s) {
        this.s = s;
        resultList = new IRVector(s);
        lbComm = new LBCommunication(s, this);
    }

    public void gotJobResult(InvocationRecord ir) {
        synchronized (s) {
            gotStealReply = true;
            stolenJob = ir;
            currentVictim = null;
            s.notifyAll();
        }
    }

    public void addToOutstandingJobList(InvocationRecord r) {
        Satin.assertLocked(s);
        s.outstandingJobs.add(r);
    }

    /**
     * does a synchronous steal. If blockOnServer is true, it blocks on server
     * side until work is available, or we must exit. This is used in
     * MasterWorker algorithms.
     */
    public InvocationRecord stealJob(Victim v, boolean blockOnServer) {
        if (ASSERTS && stolenJob != null) {
            throw new IbisError(
                "EEEK, trying to steal while an unhandled stolen job is available.");
        }

        if (s.exiting) return null;

        s.stats.stealTimer.start();
        s.stats.stealAttempts++;

        try {
            lbComm.sendStealRequest(v, true, blockOnServer);
        } catch (IOException e) {
            return null;
        }

        InvocationRecord res = waitForStealReply();
        s.stats.stealTimer.stop();
        return res;
    }

    public void handleDelayedMessages() {
        if (!receivedResults) return;

        synchronized (s) {
            while (true) {
                InvocationRecord r = resultList.removeIndex(0);
                if (r == null) {
                    break;
                }

                if (r.eek != null) {
                    s.aborts.handleInlet(r);
                }

                r.decrSpawnCounter();

                if (!FT_NAIVE) {
                    r.jobFinished();
                }
            }

            receivedResults = false;
        }
    }

    // returns false in case we have to stop because of a crash, or maybe we have to exit 
    private void waitForStealReplyMessage() {
        while (true) {
            synchronized (s) {
                if (gotStealReply) {
                    // Immediately reset gotStealReply, a reply has arrived.
                    gotStealReply = false;
                    s.currentVictimCrashed = false;
                    return;
                }

                if (s.currentVictimCrashed) {
                    s.currentVictimCrashed = false;
                    ftLogger.debug("SATIN '" + s.ident
                        + "': current victim crashed");
                    return;
                }

                if (s.exiting) {
                    return;
                }

                if (!HANDLE_MESSAGES_IN_LATENCY) { // a normal blocking steal 
                    try {
                        s.wait();
                    } catch (InterruptedException e) {
                        throw new IbisError(e);
                    }
                }
            }

            if (HANDLE_MESSAGES_IN_LATENCY) {
                s.handleDelayedMessages();
                // Thread.yield();
            }
        }
    }

    private InvocationRecord waitForStealReply() {
        waitForStealReplyMessage();

        /* If successfull, we now have a job in stolenJob. */
        if (stolenJob == null) {
            return null;
        }

        /* I love it when a plan comes together! We stole a job. */
        s.stats.stealSuccess++;
        InvocationRecord myJob = stolenJob;
        stolenJob = null;

        return myJob;
    }

    private void addToJobResultList(InvocationRecord r) {
        Satin.assertLocked(s);
        resultList.add(r);
    }

    private InvocationRecord getStolenInvocationRecord(Stamp stamp) {
        Satin.assertLocked(s);
        return s.outstandingJobs.remove(stamp);
    }

    protected void addJobResult(ReturnRecord rr, Throwable eek, Stamp stamp) {
        synchronized (s) {
            receivedResults = true;
            InvocationRecord r = null;

            if (rr != null) {
                r = getStolenInvocationRecord(rr.getStamp());
            } else {
                r = getStolenInvocationRecord(stamp);
            }

            if (r != null) {
                if (rr != null) {
                    rr.assignTo(r);
                } else {
                    r.eek = eek;
                }
                if (r.eek != null) {
                    // we have an exception, add it to the list.
                    // the list will be read during the sync
                    s.aborts.addToExceptionList(r);
                } else {
                    addToJobResultList(r);
                }
            } else {
                abortLogger.debug("SATIN '" + s.ident
                    + "': got result for aborted job, ignoring.");
            }
        }
    }

    // throws an IO exception when the ibis that tried to steal the job dies
    protected InvocationRecord stealJobFromLocalQueue(SendPortIdentifier ident,
        boolean blocking) throws IOException {
        InvocationRecord result = null;

        while (true) {
            result = s.q.getFromTail();
            if (result != null) {
                result.setStealer(ident.ibis());

                // store the job in the outstanding list
                addToOutstandingJobList(result);
                return result;
            }

            if (!blocking || s.exiting) {
                return null; // the steal request failed
            }

            try {
                s.wait();
            } catch (Exception e) {
                // Ignore.
            }

            Victim v = s.victims.getVictim(ident.ibis());
            if (v == null) {
                throw new IOException("the stealing ibis died");
            }
        }
    }

    public void sendResult(InvocationRecord r, ReturnRecord rr) {
        lbComm.sendResult(r, rr);
    }

    public void handleStealRequest(SendPortIdentifier ident, int opcode) {
        lbComm.handleStealRequest(ident, opcode);
    }

    public void handleReply(ReadMessage m, int opcode) {
        lbComm.handleReply(m, opcode);
    }

    public void handleJobResult(ReadMessage m, int opcode) {
        lbComm.handleJobResult(m, opcode);
    }

    public void sendStealRequest(Victim v, boolean synchronous, boolean blocking)
        throws IOException {
        lbComm.sendStealRequest(v, synchronous, blocking);
    }

    /**
     * Used for fault tolerance, we must know who the current victim is,
     * in case it crashes.
     */
    public IbisIdentifier getCurrentVictim() {
        return currentVictim;
    }

    public void setCurrentVictim(IbisIdentifier ident) {
        currentVictim = ident;
    }
}
