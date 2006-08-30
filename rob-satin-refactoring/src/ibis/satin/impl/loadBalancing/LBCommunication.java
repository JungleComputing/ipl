/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Communication;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.faultTolerance.GlobalResultTableValue;
import ibis.satin.impl.spawnSync.IRVector;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.util.Timer;

import java.io.IOException;
import java.util.Map;

public final class LBCommunication implements Config, Protocol {
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
    public IbisIdentifier currentVictim = null;

    public LBCommunication(Satin s, StaticProperties requestedProperties) {
        this.s = s;
        resultList = new IRVector(s);
    }

    public void sendStealRequest(Victim v, boolean synchronous, boolean blocking)
        throws IOException {
        stealLogger.debug("SATIN '" + s.ident + "': sending"
            + (synchronous ? "SYNC" : "ASYNC") + "steal message to "
            + v.getIdent());

        WriteMessage writeMessage = v.newMessage();
        byte opcode = -1;

        if (synchronous) {
            if (blocking) {
                opcode = Protocol.BLOCKING_STEAL_REQUEST;
            } else {
                if (!FT_NAIVE) {
                    synchronized (s) {
                        if (s.ft.getTable) {
                            opcode = Protocol.STEAL_AND_TABLE_REQUEST;
                        } else {
                            opcode = Protocol.STEAL_REQUEST;
                        }
                    }
                } else {
                    opcode = Protocol.STEAL_REQUEST;
                }
            }
        } else {
            if (!FT_NAIVE) {
                synchronized (s) {
                    if (s.clusterCoordinator && s.ft.getTable) {
                        opcode = Protocol.ASYNC_STEAL_AND_TABLE_REQUEST;
                    } else {
                        if (s.ft.getTable) {
                            grtLogger.info("SATIN '" + s.ident
                                + ": EEEK sending async steal message "
                                + "while waiting for table!!");
                        }
                        opcode = Protocol.ASYNC_STEAL_REQUEST;
                    }
                }
            } else {
                opcode = Protocol.ASYNC_STEAL_REQUEST;
            }
        }

        writeMessage.writeByte(opcode);
        long cnt = writeMessage.finish();
        if (s.comm.inDifferentCluster(v.getIdent())) {
            s.stats.interClusterMessages++;
            s.stats.interClusterBytes += cnt;
        } else {
            s.stats.intraClusterMessages++;
            s.stats.intraClusterBytes += cnt;
        }
    }

    /*
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
            s.lbComm.sendStealRequest(v, true, blockOnServer);
        } catch (IOException e) {
            return null;
        }

        InvocationRecord res = waitForStealReply();
        s.stats.stealTimer.stop();
        return res;
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

    public void handleJobResult(ReadMessage m, int opcode) {
        ReturnRecord rr = null;
        Stamp stamp = null;
        Throwable eek = null;
        Timer returnRecordReadTimer = null;
        boolean gotException = false;

        stealLogger.info("SATIN '" + s.ident
            + "': got job result message from " + m.origin().ibis());

        // This upcall may run in parallel with other upcalls.
        // Therefore, we cannot directly use the timer in Satin.
        // Use our own local timer, and add the result to the global timer
        // later.

        returnRecordReadTimer = Timer.createTimer();
        returnRecordReadTimer.start();
        try {
            if (opcode == JOB_RESULT_NORMAL) {
                rr = (ReturnRecord) m.readObject();
                stamp = rr.getStamp();
                eek = rr.getEek();
            } else {
                eek = (Throwable) m.readObject();
                stamp = (Stamp) m.readObject();
            }
            // m.finish();
        } catch (Exception e) {
            spawnLogger
                .error("SATIN '" + s.ident
                    + "': got exception while reading job result: " + e
                    + opcode, e);
            gotException = true;
        }
        returnRecordReadTimer.stop();
        s.stats.returnRecordReadTimer.add(returnRecordReadTimer);

        if (gotException) {
            return;
        }

        if (eek != null) {
            stealLogger.info("SATIN '" + s.ident
                + "': handleJobResult: exception result: " + eek, eek);
        } else {
            stealLogger.info("SATIN '" + s.ident
                + "': handleJobResult: normal result");
        }

        addJobResult(rr, eek, stamp);
    }

    public void sendResult(InvocationRecord r, ReturnRecord rr) {
        if (/* exiting || */r.alreadySentExceptionResult()) {
            return;
        }

        stealLogger.info("SATIN '" + s.ident + "': sending job result to "
            + r.getOwner() + ", exception = "
            + (r.eek == null ? "null" : ("" + r.eek)));

        Victim v = null;

        synchronized (s) {
            if (!FT_NAIVE && r.isOrphan()) {
                GlobalResultTableValue value = s.ft.lookup(r);
                if (ASSERTS && value == null) {
                    grtLogger.fatal("SATIN '" + s.ident
                        + "': orphan not locked in the table");
                    System.exit(1); // Failed assertion
                }
                r.setOwner(value.sendTo);
                grtLogger.info("SATIN '" + s.ident + "': storing an orphan");
                s.ft.storeResult(r);
            }
            v = s.victims.getVictim(r.getOwner());
        }

        if (v == null) {
            //probably crashed..
            if (!FT_NAIVE && !r.isOrphan()) {
                synchronized (s) {
                    grtLogger.info("SATIN '" + s.ident
                        + "': a job became an orphan??");
                    s.ft.storeResult(r);
                }
            }
            return;
        }

        try {
            s.stats.returnRecordWriteTimer.start();

            WriteMessage writeMessage = v.newMessage();
            if (r.eek == null) {
                writeMessage.writeByte(Protocol.JOB_RESULT_NORMAL);
                writeMessage.writeObject(rr);
            } else {
                if (rr == null) {
                    r.setAlreadySentExceptionResult(true);
                }
                writeMessage.writeByte(Protocol.JOB_RESULT_EXCEPTION);
                writeMessage.writeObject(r.eek);
                writeMessage.writeObject(r.getStamp());
            }

            long cnt = writeMessage.finish();
            s.stats.returnRecordWriteTimer.stop();
            s.stats.returnRecordBytes += cnt;
            if (s.comm.inDifferentCluster(r.getOwner())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch (IOException e) {
            ftLogger.info("SATIN '" + s.ident
                + "': Got Exception while sending result of stolen job", e);
        }
    }

    public void handleStealRequest(SendPortIdentifier ident, int opcode) {
        Map table = null;
        Victim v = null;

        // This upcall may run in parallel with other upcalls.
        // Therefore, we cannot directly use the handleSteal timer in Satin.
        // Use our own local timer, and add the result to the global timer
        // later.

        Timer handleStealTimer = Timer.createTimer();
        handleStealTimer.start();
        s.stats.stealRequests++;

        stealLogger.debug("SATIN '" + s.ident + "': got steal request from "
            + ident.ibis() + " opcode = "
            + Communication.opcodeToString(opcode));

        InvocationRecord result = null;

        synchronized (s) {
            if (s.deadIbises.contains(ident.ibis())) {
                //this message arrived after the crash of its sender was
                // detected. Is this actually possible?
                stealLogger.warn("SATIN '" + s.ident
                    + "': EEK!! got steal request from a dead ibis: "
                    + ident.ibis());
                handleStealTimer.stop();
                s.stats.handleStealTimer.add(handleStealTimer);
                return;
            }

            try {
                result = stealJobFromLocalQueue(ident, opcode);
            } catch (IOException e) {
                handleStealTimer.stop();
                s.stats.handleStealTimer.add(handleStealTimer);
                return; // the stealing ibis died
            }

            if (!FT_NAIVE
                && (opcode == STEAL_AND_TABLE_REQUEST || opcode == ASYNC_STEAL_AND_TABLE_REQUEST)) {
                if (!s.ft.getTable) {
                    table = s.ft.getContents();
                }
            }

            synchronized (s) {
                v = s.victims.getVictim(ident.ibis());
            }

            if (v == null) return; // node might have crashed
        }

        if (result == null) {
            sendStealFailedMessage(ident, opcode, v, table);
            handleStealTimer.stop();
            s.stats.handleStealTimer.add(handleStealTimer);
            return;
        }

        // we stole a job
        sendStolenJobMessage(ident, opcode, v, result, table);
        handleStealTimer.stop();
        s.stats.handleStealTimer.add(handleStealTimer);
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

    // Here, the timing code is OK, the upcall cannot run in parallel
    // (readmessage is not finished).
    public void handleReply(ReadMessage m, int opcode) {
        SendPortIdentifier ident = m.origin();
        InvocationRecord tmp = null;
        Map table = null;

        stealLogger.debug("SATIN '" + s.ident
            + "': got steal reply message from " + ident.ibis() + ": "
            + Communication.opcodeToString(opcode));

        switch (opcode) {
        case STEAL_REPLY_SUCCESS_TABLE:
        case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
            try {
                table = (Map) m.readObject();
            } catch (Exception e) {
                stealLogger.error("SATIN '" + s.ident
                    + "': Got Exception while reading steal " + "reply from "
                    + ident + ", opcode:" + +opcode + ", exception: " + e, e);
            }
            if (table != null) {
                synchronized (s) {
                    s.ft.getTable = false;
                    s.ft.addContents(table);
                }
            }
            s.stats.addReplicaTimer.stop();
        //fall through
        case STEAL_REPLY_SUCCESS:
        case ASYNC_STEAL_REPLY_SUCCESS:
            try {
                s.stats.invocationRecordReadTimer.start();
                tmp = (InvocationRecord) m.readObject();
                s.stats.invocationRecordReadTimer.stop();

                if (ASSERTS && tmp.aborted) {
                    stealLogger.warn("SATIN '" + s.ident
                        + ": stole aborted job!");
                }
            } catch (Exception e) {
                stealLogger.error("SATIN '" + s.ident
                    + "': Got Exception while reading steal " + "reply from "
                    + ident + ", opcode:" + +opcode + ", exception: " + e, e);
            }

            synchronized (s) {
                if (s.deadIbises.contains(ident)) {
                    //this message arrived after the crash of its sender
                    // was detected, is it anyhow possible?
                    stealLogger.error("SATIN '" + s.ident
                        + "': got reply from dead ibis??? Ignored");
                    break;
                }
            }

            s.algorithm.stealReplyHandler(tmp, opcode);
            break;

        case STEAL_REPLY_FAILED_TABLE:
        case ASYNC_STEAL_REPLY_FAILED_TABLE:
            try {
                table = (Map) m.readObject();
            } catch (Exception e) {
                stealLogger.error("SATIN '" + s.ident
                    + "': Got Exception while reading steal " + "reply from "
                    + ident + ", opcode:" + +opcode + ", exception: " + e, e);
            }
            if (table != null) {
                synchronized (s) {
                    s.ft.getTable = false;
                    s.ft.addContents(table);
                }
            }
            s.stats.addReplicaTimer.stop();
        //fall through
        case STEAL_REPLY_FAILED:
        case ASYNC_STEAL_REPLY_FAILED:
            s.algorithm.stealReplyHandler(null, opcode);
            break;
        default:
            stealLogger.error("INTERNAL ERROR, opcode = " + opcode);
            break;
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

                if (UPCALLS && !HANDLE_MESSAGES_IN_LATENCY) { // a normal blocking steal 
                    try {
                        s.wait();
                    } catch (InterruptedException e) {
                        throw new IbisError(e);
                    }
                }
            }

            if (!UPCALLS || HANDLE_MESSAGES_IN_LATENCY) {
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

    private void addJobResult(ReturnRecord rr, Throwable eek, Stamp stamp) {
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
    private InvocationRecord stealJobFromLocalQueue(SendPortIdentifier ident,
        int opcode) throws IOException {
        InvocationRecord result = null;

        while (true) {
            result = s.q.getFromTail();
            if (result != null) {
                result.setStealer(ident.ibis());

                // store the job in the outstanding list
                addToOutstandingJobList(result);
                return result;
            }

            if (opcode != BLOCKING_STEAL_REQUEST || s.exiting) {
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

    private void sendStealFailedMessage(SendPortIdentifier ident, int opcode,
        Victim v, Map table) {
        if (opcode == ASYNC_STEAL_REQUEST) {
            stealLogger.debug("SATIN '" + s.ident
                + "': sending FAILED back to " + ident.ibis());
        }
        if (opcode == ASYNC_STEAL_AND_TABLE_REQUEST) {
            stealLogger.debug("SATIN '" + s.ident
                + "': sending FAILED_TABLE back to " + ident.ibis());
        }

        try {
            WriteMessage m = v.newMessage();
            if (opcode == STEAL_REQUEST || opcode == BLOCKING_STEAL_REQUEST) {
                m.writeByte(STEAL_REPLY_FAILED);
            } else if (opcode == ASYNC_STEAL_REQUEST) {
                m.writeByte(ASYNC_STEAL_REPLY_FAILED);
            } else if (opcode == STEAL_AND_TABLE_REQUEST) {
                if (table != null) {
                    m.writeByte(STEAL_REPLY_FAILED_TABLE);
                    m.writeObject(table);
                } else {
                    m.writeByte(STEAL_REPLY_FAILED);
                }
            } else if (opcode == ASYNC_STEAL_AND_TABLE_REQUEST) {
                if (table != null) {
                    m.writeByte(ASYNC_STEAL_REPLY_FAILED_TABLE);
                    m.writeObject(table);
                } else {
                    m.writeByte(ASYNC_STEAL_REPLY_FAILED);
                }
            } else {
                stealLogger.error("UNHANDLED opcode " + opcode
                    + " in handleStealRequest");
            }

            long cnt = m.finish();
            if (s.comm.inDifferentCluster(ident.ibis())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }

            stealLogger.debug("SATIN '" + s.ident
                + "': sending FAILED back to " + ident.ibis() + " DONE");
        } catch (IOException e) {
            stealLogger.warn("SATIN '" + s.ident
                + "': trying to send FAILURE back, but got exception: " + e, e);
        }

    }

    private void sendStolenJobMessage(SendPortIdentifier ident, int opcode,
        Victim v, InvocationRecord result, Map table) {
        if (ASSERTS && result.aborted) {
            stealLogger.warn("SATIN '" + s.ident
                + ": trying to send aborted job!");
        }

        s.stats.stolenJobs++;

        stealLogger.info("SATIN '" + s.ident + "': sending SUCCESS and job #"
            + result.getStamp() + " back to " + ident.ibis());

        try {
            WriteMessage m = v.newMessage();
            if (opcode == STEAL_REQUEST || opcode == BLOCKING_STEAL_REQUEST) {
                m.writeByte(STEAL_REPLY_SUCCESS);
            } else if (opcode == ASYNC_STEAL_REQUEST) {
                m.writeByte(ASYNC_STEAL_REPLY_SUCCESS);
            } else if (opcode == STEAL_AND_TABLE_REQUEST) {
                if (table != null) {
                    m.writeByte(STEAL_REPLY_SUCCESS_TABLE);
                    m.writeObject(table);
                } else {
                    stealLogger.warn("SATIN '" + s.ident
                        + "': EEK!! sending a job but not a table !?");
                }
            } else if (opcode == ASYNC_STEAL_AND_TABLE_REQUEST) {
                if (table != null) {
                    m.writeByte(ASYNC_STEAL_REPLY_SUCCESS_TABLE);
                    m.writeObject(table);
                } else {
                    stealLogger.warn("SATIN '" + s.ident
                        + "': EEK!! sending a job but not a table !?");
                }
            } else {
                stealLogger.error("UNHANDLED opcode " + opcode
                    + " in handleStealRequest");
                // System.exit(1);
            }

            Timer invocationRecordWriteTimer = Timer.createTimer();
            invocationRecordWriteTimer.start();
            m.writeObject(result);
            invocationRecordWriteTimer.stop();
            long cnt = m.finish();
            s.stats.invocationRecordWriteTimer.add(invocationRecordWriteTimer);
            if (s.comm.inDifferentCluster(ident.ibis())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch (IOException e) {
            stealLogger.warn("SATIN '" + s.ident
                + "': trying to send a job back, but got exception: " + e, e);
        }

        /* If we don't use fault tolerance with the global result table, 
         * we can set the object parameters to null,
         * so the GC can clean them up --Rob */
        if (FT_NAIVE) {
            result.clearParams();
        }
    }
}
