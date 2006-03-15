
/* $Id$ */


package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class WorkStealing extends Stats {

    protected void sendResult(InvocationRecord r, ReturnRecord rr) {
        if (/* exiting || */r.alreadySentExceptionResult) {
            return;
        }

        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + ident
                    + "': sending job result to " + r.owner
                    + ", exception = "
                    + (r.eek == null ? "null" : ("" + r.eek)));
        }

        Victim v = null;

        synchronized (this) {

            //for debugging
            //globalResultTable.storeResult(r);

            if (FAULT_TOLERANCE && !FT_NAIVE && r.orphan) {
                GlobalResultTable.Value value = globalResultTable.lookup(r,
                        false);
                if (ASSERTS && value == null) {
                    grtLogger.fatal("SATIN '" + ident
                            + "': orphan not locked in the table");
                    System.exit(1);     // Failed assertion
                }
                r.owner = value.sendTo;
                if (grtLogger.isInfoEnabled()) {
                    grtLogger.info("SATIN '" + ident
                            + "': storing an orphan");
                }
                globalResultTable.storeResult(r);
            }
            v = getVictimNoWait(r.owner);
        }

        if (v == null) {
            //probably crashed..
            if (FAULT_TOLERANCE && !FT_NAIVE && !r.orphan) {
                synchronized (this) {
                    if (grtLogger.isInfoEnabled()) {
                        grtLogger.info("SATIN '" + ident
                                + "': a job became an orphan??");
                    }
                    globalResultTable.storeResult(r);
                }
            }
            return;
        }

        try {
            if (STEAL_TIMING) {
                returnRecordWriteTimer.start();
            }
            
            WriteMessage writeMessage = v.newMessage();
            if (r.eek == null) {
                writeMessage.writeByte(Protocol.JOB_RESULT_NORMAL);
                writeMessage.writeObject(rr);		
            } else {
                if (rr == null) {
                    r.alreadySentExceptionResult = true;
                }
                writeMessage.writeByte(Protocol.JOB_RESULT_EXCEPTION);
                writeMessage.writeObject(r.eek);
		writeMessage.writeObject(r.stamp);
		//writeMessage.writeInt(r.updateCounter.value);
	    }

            long cnt = writeMessage.finish();
            if (STEAL_TIMING) {
                returnRecordWriteTimer.stop();
            }
	    returnRecordBytes += cnt;

            if (STEAL_STATS) {
                if (inDifferentCluster(r.owner)) {
                    interClusterMessages++;
                    interClusterBytes += cnt;
                } else {
                    intraClusterMessages++;
                    intraClusterBytes += cnt;
                }
            }
        } catch (IOException e) {
            if (! FAULT_TOLERANCE) {
                stealLogger.warn("SATIN '" + ident
                        + "': Got Exception while sending result of stolen job", e);
            } else {
                ftLogger.info("SATIN '" + ident
                        + "': Got Exception while sending result of stolen job", e);
            }

        }
    }

    /*
     * does a synchronous steal. If blockOnServer is true, it blocks on server
     * side until work is available, or we must exit. This is used in
     * MasterWorker algorithms.
     */
    protected InvocationRecord stealJob(Victim v, boolean blockOnServer) {

        if (ASSERTS && stolenJob != null) {
            throw new IbisError(
                    "EEEK, trying to steal while an unhandled stolen job is available.");
        }

        if (!exiting) {
            if (STEAL_TIMING) {
                stealTimer.start();
            }

            if (STEAL_STATS) {
                stealAttempts++;
            }

            try {
                sendStealRequest(v, true, blockOnServer);
            } catch(IOException e) {
                return null;
            }
            return waitForStealReply();
        }
        return null;
    }

    protected void sendStealRequest(Victim v, boolean synchronous,
            boolean blocking) throws IOException {

        if (stealLogger.isDebugEnabled()) {
            if (synchronous) {
                stealLogger.debug("SATIN '" + ident
                        + "': sending steal message to " + v.ident);
            } else {
                stealLogger.debug("SATIN '" + ident
                        + "': sending ASYNC steal message to "
                        + v.ident);
            }
        }

        WriteMessage writeMessage = v.newMessage();
        byte opcode = -1;

        if (synchronous) {
            if (blocking) {
                opcode = Protocol.BLOCKING_STEAL_REQUEST;
            } else {
                if (FAULT_TOLERANCE && !FT_NAIVE) {
                    synchronized (this) {
                        if (getTable) {
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
            if (FAULT_TOLERANCE && !FT_NAIVE) {
                synchronized (this) {
                    if (clusterCoordinator && getTable) {
                        opcode = Protocol.ASYNC_STEAL_AND_TABLE_REQUEST;
                    } else {
                        if (grtLogger.isInfoEnabled() && getTable) {
                            grtLogger.info("SATIN '" + ident
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
        if (STEAL_STATS) {
            if (inDifferentCluster(v.ident)) {
                interClusterMessages++;
                interClusterBytes += cnt;
            } else {
                intraClusterMessages++;
                intraClusterBytes += cnt;
            }
        }
    }

    protected InvocationRecord waitForStealReply() {
        // if (exiting) {
        //     return false;
        // }

        // Replaced this wait call, do something useful instead:
        // handleExceptions and aborts.
        if (upcalls) {
            if (HANDLE_MESSAGES_IN_LATENCY) {
                while (true) {
                    satinPoll();

                    if (ABORTS || FAULT_TOLERANCE) {
                        handleDelayedMessages();
                    }

                    synchronized (this) {

                        if (gotStealReply) {
                            /*
                             * Immediately reset gotStealReply, we know that a
                             * reply has arrived.
                             */
                            gotStealReply = false;
                            currentVictimCrashed = false;
                            break;
                        }

                        if (FAULT_TOLERANCE) {
                            if (currentVictimCrashed) {
                                currentVictimCrashed = false;
                                if (gotStealReply == false) {
                                    if (STEAL_TIMING) {
                                        stealTimer.stop();
                                    }

                                    return null;
                                }
                                break;
                            }
                        }
                    }
                    // Thread.yield();
                }
            } else {
                synchronized (this) {
                    while (!gotStealReply) {

                        if (FAULT_TOLERANCE) {
                            if (currentVictimCrashed) {
                                currentVictimCrashed = false;
                                if (ftLogger.isDebugEnabled()) {
                                    ftLogger.debug("SATIN '" + ident
                                             + "': current victim crashed");
                                }
                                if (gotStealReply == false) {
                                    if (STEAL_TIMING) {
                                        stealTimer.stop();
                                    }
                                    return null;
                                }
                                break;
                            }
                        }

                        try {
                            wait();
                        } catch (InterruptedException e) {
                            throw new IbisError(e);
                        }
                        if (exiting) {
                            if (STEAL_TIMING) {
                                stealTimer.stop();
                            }
                            return null;
                        }

                    }
                    /*
                     * Immediately reset gotStealReply, we know that a reply has
                     * arrived.
                     */
                    gotStealReply = false;
                }
            }
        } else { // poll for reply
            while (!gotStealReply) {
                satinPoll();
                if (FAULT_TOLERANCE) {
                    if (currentVictimCrashed) {
                        currentVictimCrashed = false;
                        if (gotStealReply == false) {
                            if (STEAL_TIMING) {
                                stealTimer.stop();
                            }
                            return null;
                        }
                        break;
                    }
                }
                if (exiting) {
                    if (STEAL_TIMING) {
                        stealTimer.stop();
                    }
                    return null;
                }

            }
            gotStealReply = false;
        }

        if (STEAL_TIMING) {
            stealTimer.stop();
        }

        /*
         * stealLogger.debug("SATIN '" + ident
         *         + "': got synchronous steal reply: "
         *         + (stolenJob == null ? "FAILED" : "SUCCESS"));
         */

        /* If successfull, we now have a job in stolenJob. */
        if (stolenJob == null) {
            return null;
        }

        /* I love it when a plan comes together! */

        if (IDLE_TIMING && idleStarted) {
            idleStarted = false;
            if (idleLogger.isDebugEnabled()) {
                idleLogger.debug("SATIN '" + ident + "': idle stop");
            }
            idleTimer.stop();
        }

        if (STEAL_STATS) {
            stealSuccess++;
        }

        InvocationRecord myJob = stolenJob;
        stolenJob = null;

        // stolenFrom = myJob.owner;

        return myJob;
    }

    // hold the lock when calling this
    protected void addToJobResultList(InvocationRecord r) {
        if (ASSERTS) {
            assertLocked(this);
        }
        resultList.add(r);
    }

    // hold the lock when calling this
    protected InvocationRecord getStolenInvocationRecord(Stamp stamp) {
        if (ASSERTS) {
            assertLocked(this);
        }
        return outstandingJobs.remove(stamp);
    }

    synchronized void addJobResult(ReturnRecord rr, Throwable eek,
            Stamp stamp) {
        receivedResults = true;
        InvocationRecord r = null;

        if (rr != null) {
            r = getStolenInvocationRecord(rr.stamp);
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
                if (ABORTS) {
                    addToExceptionList(r);
                } else {
                    throw new IbisError("Got exception result", r.eek);
                }
            } else {
                addToJobResultList(r);
            }
        } else {
            if (ABORTS || FAULT_TOLERANCE) {
                if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("SATIN '" + ident
                            + "': got result for aborted job, ignoring.");
                }
            } else {
                stealLogger.warn("SATIN '" + ident
                        + "': got result for unknown job!");
            }
        }
    }

    synchronized void handleResults() {
        while (true) {
            InvocationRecord r = resultList.removeIndex(0);
            if (r == null) {
                break;
            }

	    if (r.eek != null) {
		handleInlet(r);
	    }
	    
	    if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
		r.spawnCounter.decr(r);
                spawnLogger.debug("SATIN '" + ident
                        + "': got result for stolen job, "
                        + "spawncounter = " + r.spawnCounter
                        + "(" + r.spawnCounter.value + ")"
                        );
	    } else {
		r.spawnCounter.value--;
	    }
	    
	    if (FAULT_TOLERANCE && !FT_WITHOUT_ABORTS && !FT_NAIVE) {
		attachToParentFinished(r);
	    }


            if (ASSERTS && r.spawnCounter != null && r.spawnCounter.value < 0) {
                spawnLogger.fatal("Just made spawncounter < 0",
                        new Throwable());
                System.exit(1);         // Failed assertion
            }
        }

        receivedResults = false;
    }

    // hold the lock when calling this
    protected void addToOutstandingJobList(InvocationRecord r) {
        if (ASSERTS) {
            assertLocked(this);
        }
        outstandingJobs.add(r);
    }

    protected synchronized void gotJobResult(InvocationRecord ir) {
        gotStealReply = true;
        stolenJob = ir;
        currentVictim = null;
        notifyAll();
    }
}
