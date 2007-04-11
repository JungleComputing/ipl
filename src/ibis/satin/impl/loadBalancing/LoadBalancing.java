/*
 * Created on Jun 20, 2006 by rob
 */
package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.spawnSync.IRVector;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;
import ibis.satin.impl.spawnSync.Stamp;

import java.io.IOException;
import java.util.ArrayList;

public class LoadBalancing implements Config {
    
    static final class StealRequest {
        int opcode;
        SendPortIdentifier sp;
    }

    final class StealRequestHandler extends Thread {
    	static final boolean CONTINUOUS_STATS = true;
    	static final long CONTINUOUS_STATS_INTERVAL = 60 * 1000;
    	
        public StealRequestHandler() {
            setDaemon(true);
            setName("Satin StealRequestHandler");
        }
        
        public void run() {
        	long lastPrintTime = System.currentTimeMillis();
        	
            while(true) {
            	if(CONTINUOUS_STATS && System.currentTimeMillis() - lastPrintTime > CONTINUOUS_STATS_INTERVAL) {
            		lastPrintTime = System.currentTimeMillis();
            		s.stats.printDetailedStats(s.ident);
                    s.comm.ibis.printStatistics(System.out);
            	}
            	
                StealRequest sr = null;
                synchronized (s) {
                    if(stealQueue.size() > 0) {
                        sr = stealQueue.remove(0);
                    } else {
                        try {
                            s.wait();
                        } catch (Exception e) {
                            // ignore
                        }
                        continue;
                    }
                }
                
                lbComm.handleStealRequest(sr.sp, sr.opcode);
            }
        }
    }
    
    private LBCommunication lbComm;

    private Satin s;

    private volatile boolean receivedResults = false;

    /** Used to store reply messages. */
    private volatile boolean gotStealReply = false;

    private InvocationRecord stolenJob = null;

    private final IRVector resultList;

    private final ArrayList<StealRequest> stealQueue;
    
    /**
     * Used for fault tolerance, we must know who the current victim is,
     * in case it crashes.
     */
    private IbisIdentifier currentVictim = null;

    public LoadBalancing(Satin s) {
        this.s = s;
     
        if(QUEUE_STEALS) {
            stealQueue = new ArrayList<StealRequest>();
            new StealRequestHandler().start();
        } else {
            stealQueue = null;
        }
        
        
        resultList = new IRVector(s);
        lbComm = new LBCommunication(s, this);
    }

    public void gotJobResult(InvocationRecord ir, IbisIdentifier sender) {
        synchronized (s) {
        	// This might be a job that came in after a STEAL_WAIT_TIMEOUT.
        	// If this is the case, this job has to be added to the queue,
        	// it is not the result of the current steal request.
        	if(!sender.equals(currentVictim)) {
        		ftLogger.warn("SATIN '" + s.ident
                        + "': received a job from a node that caused a timeout before.");
        		if(ir != null) {
        			s.q.addToTail(ir);
        		}
        		return;
        	}
        	
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
            throw new Error(
                "EEEK, trying to steal while an unhandled stolen job is available.");
        }

        if (s.exiting) return null;

        s.stats.stealTimer.start();
        s.stats.stealAttempts++;

        try {
            lbComm.sendStealRequest(v, true, blockOnServer);
            return waitForStealReply();
        } catch (IOException e) {
            ftLogger.info("SATIN '" + s.ident
                    + "': got exception during steal request", e);
            return null;
        } finally {
            s.stats.stealTimer.stop();
        }
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

    private void waitForStealReplyMessage() {
    	long start = System.currentTimeMillis();
    	while (true) {
            synchronized (s) {
            	boolean gotTimeout = System.currentTimeMillis() - start >= STEAL_WAIT_TIMEOUT;
            	if(gotTimeout && !gotStealReply) {
            		ftLogger.warn("SATIN '" + s.ident
                            + "': a timeout occurred while waiting for a steal reply");
            	}
            	
                if (gotStealReply || gotTimeout) {
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
                        s.wait(500);
                    } catch (InterruptedException e) {
                        // ignore
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
                result.setStealer(ident.ibisIdentifier());

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

            Victim v = s.victims.getVictim(ident.ibisIdentifier());
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

    public void queueStealRequest(SendPortIdentifier ident, int opcode) {
        StealRequest r = new StealRequest();
        r.opcode = opcode;
        r.sp = ident;
        synchronized (s) {
            stealQueue.add(r);
            s.notifyAll();
        }
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
