/*
 * Created on Apr 26, 2006 by rob
 */
package ibis.satin.impl.loadBalancing;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.communication.Communication;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.faultTolerance.GlobalResultTableValue;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.util.Timer;

import java.io.IOException;
import java.util.Map;

final class LBCommunication implements Config, Protocol {
    private Satin s;

    private LoadBalancing lb;

    protected LBCommunication(Satin s, LoadBalancing lb) {
        this.s = s;
        this.lb = lb;
    }

    protected void sendStealRequest(Victim v, boolean synchronous,
        boolean blocking) throws IOException {
        if (stealLogger.isDebugEnabled()) {
            stealLogger.debug("SATIN '" + s.ident + "': sending"
                + (synchronous ? "SYNC" : "ASYNC") + "steal message to "
                + v.getIdent());
        }

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
                            if (grtLogger.isInfoEnabled()) {
                                grtLogger.info("SATIN '" + s.ident
                                    + ": EEEK sending async steal message "
                                    + "while waiting for table!!");
                            }
                        }
                        opcode = Protocol.ASYNC_STEAL_REQUEST;
                    }
                }
            } else {
                opcode = Protocol.ASYNC_STEAL_REQUEST;
            }
        }

        try {
            writeMessage.writeByte(opcode);
            // Finish the message but try to keep the connection. If the
            // steal attempt succeeds, we need to send an answer later on.
            long cnt = v.finishKeepConnection(writeMessage);
            if (s.comm.inDifferentCluster(v.getIdent())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch(IOException e) {
            writeMessage.finish(e);
            throw e;
        }
    }

    protected void handleJobResult(ReadMessage m, int opcode) {
        ReturnRecord rr = null;
        Stamp stamp = null;
        Throwable eek = null;
        Timer returnRecordReadTimer = null;
        boolean gotException = false;

        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + s.ident
                + "': got job result message from " 
                + m.origin().ibisIdentifier());
        }

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
        } finally {
            returnRecordReadTimer.stop();
        }
        s.stats.returnRecordReadTimer.add(returnRecordReadTimer);

        if (gotException) {
            return;
        }

        if (stealLogger.isInfoEnabled() && eek != null) {
            stealLogger.info("SATIN '" + s.ident
                + "': handleJobResult: exception result: " + eek
                + ", stamp = " + stamp, eek);
        } else {
            stealLogger.info("SATIN '" + s.ident
                + "': handleJobResult: normal result, stamp = " + stamp);
        }

        lb.addJobResult(rr, eek, stamp);
    }

    protected void sendResult(InvocationRecord r, ReturnRecord rr) {
        if (/* exiting || */r.alreadySentExceptionResult()) {
            return;
        }

        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + s.ident + "': sending job result to "
                + r.getOwner() + ", exception = "
                + (r.eek == null ? "null" : ("" + r.eek))
                + ", stamp = " + r.getStamp());
        }

        Victim v = null;

        synchronized (s) {
            if (!FT_NAIVE && r.isOrphan()) {
                IbisIdentifier owner = s.ft.lookupOwner(r);
                if (ASSERTS && owner == null) {
                    grtLogger.fatal("SATIN '" + s.ident
                        + "': orphan not locked in the table");
                    System.exit(1); // Failed assertion
                }
                r.setOwner(owner);
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("SATIN '" + s.ident
                            + "': storing an orphan");
                }
                s.ft.storeResult(r);
            }
            v = s.victims.getVictim(r.getOwner());
        }

        if (v == null) {
            //probably crashed..
            if (!FT_NAIVE && !r.isOrphan()) {
                synchronized (s) {
                    s.ft.storeResult(r);
                }
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("SATIN '" + s.ident
                        + "': a job became an orphan??");
                }
            }
            return;
        }

        s.stats.returnRecordWriteTimer.start();
        WriteMessage writeMessage = null;
        try {
            writeMessage = v.newMessage();
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

            long cnt = v.finish(writeMessage);
            s.stats.returnRecordBytes += cnt;
            if (s.comm.inDifferentCluster(r.getOwner())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch (IOException e) {
            if (writeMessage != null) {
                writeMessage.finish(e);
            }
            if (ftLogger.isInfoEnabled()) {
                ftLogger.info("SATIN '" + s.ident
                    + "': Got Exception while sending result of stolen job", e);
            }
        } finally {
            s.stats.returnRecordWriteTimer.stop();
        }
    }

    protected void handleStealRequest(SendPortIdentifier ident, int opcode) {

        // This upcall may run in parallel with other upcalls.
        // Therefore, we cannot directly use the handleSteal timer in Satin.
        // Use our own local timer, and add the result to the global timer
        // later.

        Timer handleStealTimer = Timer.createTimer();
        handleStealTimer.start();
        s.stats.stealRequests++;

        if (stealLogger.isDebugEnabled()) {
            stealLogger.debug("SATIN '" + s.ident + "': got steal request from "
                + ident.ibisIdentifier() + " opcode = "
                + Communication.opcodeToString(opcode));
        }

        InvocationRecord result = null;
        Victim v = null;
        Map<Stamp, GlobalResultTableValue> table = null;

        synchronized (s) {
            v = s.victims.getVictim(ident.ibisIdentifier());
            if (v == null || s.deadIbises.contains(ident.ibisIdentifier())) {
                //this message arrived after the crash of its sender was
                // detected. Is this actually possible?
                stealLogger.warn("SATIN '" + s.ident
                    + "': EEK!! got steal request from a dead ibis: "
                    + ident.ibisIdentifier());
                handleStealTimer.stop();
                s.stats.handleStealTimer.add(handleStealTimer);
                return;
            }

            try {
                result = lb.stealJobFromLocalQueue(ident,
                    opcode == BLOCKING_STEAL_REQUEST);
            } catch (IOException e) {
                stealLogger.warn("SATIN '" + s.ident
                + "': EEK!! got exception during steal request: "
                + ident.ibisIdentifier());
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

    // Here, the timing code is OK, the upcall cannot run in parallel
    // (readmessage is not finished).
    protected void handleReply(ReadMessage m, int opcode) {
        SendPortIdentifier ident = m.origin();
        InvocationRecord tmp = null;

        if (stealLogger.isDebugEnabled()) {
            stealLogger.debug("SATIN '" + s.ident
                + "': got steal reply message from " + ident.ibisIdentifier()
                + ": " + Communication.opcodeToString(opcode));
        }

        switch (opcode) {
        case STEAL_REPLY_SUCCESS_TABLE:
        case ASYNC_STEAL_REPLY_SUCCESS_TABLE:
            try {
                @SuppressWarnings("unchecked")
                Map<Stamp, GlobalResultTableValue> table
                        = (Map<Stamp, GlobalResultTableValue>) m.readObject();
                if (table != null) {
                    synchronized (s) {
                        s.ft.getTable = false;
                        s.ft.addContents(table);
                    }
                }
            } catch (Exception e) {
                stealLogger.error("SATIN '" + s.ident
                    + "': Got Exception while reading steal " + "reply from "
                    + ident + ", opcode:" + +opcode + ", exception: " + e, e);
            }
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

            s.algorithm.stealReplyHandler(tmp, ident.ibisIdentifier(), opcode);
            break;

        case STEAL_REPLY_FAILED_TABLE:
        case ASYNC_STEAL_REPLY_FAILED_TABLE:
            try {
                @SuppressWarnings("unchecked")
                Map<Stamp, GlobalResultTableValue> table
                    = (Map<Stamp, GlobalResultTableValue>) m.readObject();
                if (table != null) {
                    synchronized (s) {
                        s.ft.getTable = false;
                        s.ft.addContents(table);
                    }
                }
            } catch (Exception e) {
                stealLogger.error("SATIN '" + s.ident
                    + "': Got Exception while reading steal " + "reply from "
                    + ident + ", opcode:" + +opcode + ", exception: " + e, e);
            }
        //fall through
        case STEAL_REPLY_FAILED:
        case ASYNC_STEAL_REPLY_FAILED:
            if (CLOSE_CONNECTIONS) {
                // Drop the connection that we kept in case the steal
                // is succesful. It was'nt.
                synchronized(s) {
                    Victim v = s.victims.getVictimNonBlocking(ident.ibisIdentifier());
                    if (v != null) {
                        try {
                            v.loseConnection();
                        } catch(Exception e) {
                            // ignored
                        }
                    }
                }
            }
            s.algorithm.stealReplyHandler(null, ident.ibisIdentifier(), opcode);
            break;
        default:
            stealLogger.error("INTERNAL ERROR, opcode = " + opcode);
            break;
        }
    }

    private void sendStealFailedMessage(SendPortIdentifier ident, int opcode,
        Victim v, Map<Stamp, GlobalResultTableValue> table) {
        
        if (stealLogger.isDebugEnabled()) {
            if (opcode == ASYNC_STEAL_REQUEST) {
                stealLogger.debug("SATIN '" + s.ident
                    + "': sending FAILED back to " + ident.ibisIdentifier());
            }
            if (opcode == ASYNC_STEAL_AND_TABLE_REQUEST) {
                stealLogger.debug("SATIN '" + s.ident
                    + "': sending FAILED_TABLE back to "
                    + ident.ibisIdentifier());
            }
        }

        WriteMessage m = null;
        try {
            m = v.newMessage();
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

            long cnt = v.finish(m);
            if (s.comm.inDifferentCluster(ident.ibisIdentifier())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }

            if (stealLogger.isDebugEnabled()) {
                stealLogger.debug("SATIN '" + s.ident
                    + "': sending FAILED back to " + ident.ibisIdentifier()
                    + " DONE");
            }
        } catch (IOException e) {
            if (m != null) {
                m.finish(e);
            }
            stealLogger.warn("SATIN '" + s.ident
                + "': trying to send FAILURE back, but got exception: " + e, e);
        }
    }

    private void sendStolenJobMessage(SendPortIdentifier ident, int opcode,
        Victim v, InvocationRecord result, Map<Stamp, GlobalResultTableValue> table) {
        if (ASSERTS && result.aborted) {
            stealLogger.warn("SATIN '" + s.ident
                + ": trying to send aborted job!");
        }

        s.stats.stolenJobs++;

        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + s.ident
                    + "': sending SUCCESS and job #" + result.getStamp()
                    + " back to " + ident.ibisIdentifier());
        }

        WriteMessage m = null;
        try {
            m = v.newMessage();
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
            long cnt = v.finish(m);
            s.stats.invocationRecordWriteTimer.add(invocationRecordWriteTimer);
            if (s.comm.inDifferentCluster(ident.ibisIdentifier())) {
                s.stats.interClusterMessages++;
                s.stats.interClusterBytes += cnt;
            } else {
                s.stats.intraClusterMessages++;
                s.stats.intraClusterBytes += cnt;
            }
        } catch (IOException e) {
            if (m != null) {
                m.finish(e);
            }
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
