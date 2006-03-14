/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.IOException;

public abstract class FaultTolerance extends Inlets {

    int numCrashesHandled = 0;

    // The core of the fault tolerance mechanism, the crash recovery procedure
    synchronized void handleCrashes() {

        Registry r = ibis.registry();

        if (CRASH_TIMING) {
            crashTimer.start();
        }
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + ": handling crashes");
        }

        gotCrashes = false;
        IbisIdentifier id = null;
        while (crashedIbises.size() > 0) {
            id = (IbisIdentifier) crashedIbises.remove(0);

            // Let the Ibis registry know ...
            // Only if this is the master, otherwise everything gets terribly slow
            if (id.equals(masterIdent) || id.equals(clusterCoordinatorIdent)) {
        	try {
            	    r.maybeDead(id);
        	} catch (IOException e) {
                    if (ftLogger.isInfoEnabled()) {
                        ftLogger.info("SATIN '" + ident
                                + "' :exception while notifying registry about "
                                + "crash of " + id + ": " + e, e);
                    }
        	}
	    }

            if (commLogger.isDebugEnabled()) {
                commLogger.debug("SATIN '" + ident
                        + ": handling crash of " + id);
            }

            if (algorithm instanceof ClusterAwareRandomWorkStealing) {
                ((ClusterAwareRandomWorkStealing) algorithm)
                        .checkAsyncVictimCrash(id);
            }

            if (!FT_NAIVE) {
                globalResultTable.removeReplica(id);
            }

            /*
             * if (killTime > 0) {
             *     if (ftLogger.isInfoEnabled()) {
             *         ftLogger.info("SATIN '" + ident + "': " + id
             *                 +  " HAS CRASHED!!!");
             *     }
             * }
             */

            if (ftLogger.isInfoEnabled() && master) {
                ftLogger.info(id + " has crashed");
            }

            if (!FT_NAIVE) {
                if (FT_WITHOUT_ABORTS) {
                    // store orphans in the table
                    onStack.storeOrphansOf(id);
                } else {
                    // abort all jobs stolen from id or descendants of jobs
                    // stolen from id
                    killAndStoreSubtreeOf(id);
                }
            }

            // if using CRS, remove the asynchronously stolen job if it is owned
            // by a crashed machine
            if (algorithm instanceof ClusterAwareRandomWorkStealing) {
                ((ClusterAwareRandomWorkStealing) algorithm).killOwnedBy(id);
            }

            if (FT_WITHOUT_ABORTS) {
                outstandingJobs.redoStolenByAttachToParents(id);
            } else {
                outstandingJobs.redoStolenByWorkQueue(id);
            }

            //for debugging
            crashedIbis = id;
            del = true;

            numCrashesHandled++;
        }

        if (CRASH_TIMING) {
            crashTimer.stop();
        }

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + ": numCrashes handled: "
                    + numCrashesHandled);
        }

        notifyAll();

    }
    
    synchronized void handleMasterCrash() {
        masterHasCrashed = false;
        Registry r = ibis.registry();	    
        //master has crashed, let's elect a new one
        if (ftLogger.isInfoEnabled()) {
            ftLogger.info("SATIN '" + ident + "': MASTER ("
                    + masterIdent + ") HAS CRASHED!!!");
        }
        try {
            masterIdent = r.elect("satin master");
            if (masterIdent.equals(ident)) {
                master = true;
            }

            //statistics
            if (stats && master) {
                totalStats = new StatsMessage();
            }
        } catch (IOException e) {
    	    ftLogger.warn("SATIN '" + ident
            	    + "' :exception while electing a new master " + e,
                    e);
        } catch (ClassNotFoundException e) {
    	    ftLogger.warn("SATIN '" + ident
            	    + "' :exception while electing a new master " + e,
                    e);
        }
        restarted = true;
    
    }
    
    void handleClusterCoordinatorCrash() {
        clusterCoordinatorHasCrashed = false;
        Registry r = ibis.registry();
        try {
            clusterCoordinatorIdent = r.elect("satin "
                    + ident.cluster() + " cluster coordinator");
            if (clusterCoordinatorIdent.equals(ident)) {
                clusterCoordinator = true;
            }
        } catch (IOException e) {
    	    ftLogger.warn("SATIN '" + ident
            	    + "' :exception while electing a new cluster coordinator "
                    + e, e);
        } catch (ClassNotFoundException e) {
    	    ftLogger.warn("SATIN '" + ident
            	    + "' :exception while electing a new cluster coordinator "
                    + e, e);
        }    
    }

    // Used for fault tolerance
    void sendAbortAndStoreMessage(InvocationRecord r) {
        if (ASSERTS) {
            assertLocked(this);
        }

        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("SATIN '" + ident
                    + ": sending abort and store message to: " + r.stealer
                    + " for job " + r.stamp);
        }

        if (deadIbises.contains(r.stealer)) {
            /* don't send abort and store messages to crashed ibises */
            return;
        }

        try {
            Victim v = getVictimNoWait(r.stealer);
            if (v == null) {
                return;
            }

            WriteMessage writeMessage = v.newMessage();
            writeMessage.writeByte(Protocol.ABORT_AND_STORE);
            writeMessage.writeObject(r.parentStamp);
            long cnt = writeMessage.finish();
            if (STEAL_STATS) {
                if (inDifferentCluster(r.stealer)) {
                    interClusterMessages++;
                    interClusterBytes += cnt;
                } else {
                    intraClusterMessages++;
                    intraClusterBytes += cnt;
                }
            }
        } catch (IOException e) {
            ftLogger.warn("SATIN '" + ident
                    + "': Got Exception while sending abort message: " + e);
            // This should not be a real problem, it is just inefficient.
            // Let's continue...
            // System.exit(1);
        }
    }

    void killAndStoreChildrenOf(Stamp targetStamp) {
        if (ASSERTS) {
            assertLocked(this);
        }

        // try work queue, outstanding jobs and jobs on the stack
        // but try stack first, many jobs in q are children of stack jobs
        onStack.killAndStoreChildrenOf(targetStamp);
        q.killChildrenOf(targetStamp);
        outstandingJobs.killAndStoreChildrenOf(targetStamp);
    }

    void killAndStoreSubtreeOf(IbisIdentifier targetOwner) {
        onStack.killAndStoreSubtreeOf(targetOwner);
        q.killSubtreeOf(targetOwner);
        outstandingJobs.killAndStoreSubtreeOf(targetOwner);
    }

    /**
     * Attach a child to its parent's finished children list.
     */
    void attachToParentFinished(InvocationRecord r) {
        if (r.parent != null) {
            r.finishedSibling = r.parent.finishedChild;
            r.parent.finishedChild = r;
        } else if (r.owner.equals(ident)) {
            r.finishedSibling = rootFinishedChild;
            rootFinishedChild = r;
        } else {
            if (ASSERTS) {
                ftLogger.fatal("SATIN '" + ident
                        + "': parent of a restarted job on another machine!");
                System.exit(1); // Failed assertion
            }
        }
        //remove the job's children list
        r.finishedChild = null;
    }

    /**
     * Attach a child to its parent's list of children which need to be
     * restarted.
     */
    void attachToParentToBeRestarted(InvocationRecord r) {
        if (r.parent != null) {
            r.toBeRestartedSibling = r.parent.toBeRestartedChild;
            r.parent.toBeRestartedChild = r;
        } else if (r.owner.equals(ident)) {
            r.toBeRestartedSibling = rootToBeRestartedChild;
            rootToBeRestartedChild = r;
        } else {
            if (ASSERTS) {
                ftLogger.fatal("SATIN '" + ident
                        + "': parent of a restarted job on another machine!");
                System.exit(1); // Failed assertion
            }
        }
        //remove the job's children list
        r.toBeRestartedChild = null;
    }

    //connect upcall functions
    public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
        // if (ftLogger.isDebugEnabled()) {
        //     ftLogger.debug("SATIN '" + ident
        //             + "': got gotConnection upcall");
        // }
        return true;
    }

    synchronized void handleLostConnection(IbisIdentifier dead) {
        if (!deadIbises.contains(dead)) {
            crashedIbises.add(dead);
            deadIbises.add(dead);
            if (dead.equals(currentVictim)) {
                currentVictimCrashed = true;
		currentVictim = null;
            notifyAll();
            }
            gotCrashes = true;
            Victim v = victims.remove(dead);
            notifyAll();
            if (v != null) {
                try {
                    v.close();
                } catch (IOException e) {
                    if (ftLogger.isInfoEnabled()) {
                        ftLogger.info("port.close() throws exception "
                                + e.getMessage(), e);
                    }
                }
            }
        }
    }

    public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe,
            Exception reason) {
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident
                    + "': got lostConnection upcall: " + johnDoe.ibis());
        }
        if (FAULT_TOLERANCE) {
            if (connectionUpcallsDisabled) {
                return;
            }
            handleLostConnection(johnDoe.ibis());
        }
    }

    public void lostConnection(SendPort me, ReceivePortIdentifier johnDoe,
            Exception reason) {
        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident
                    + "': got SENDPORT lostConnection upcall: "
                    + johnDoe.ibis());
        }
        if (FAULT_TOLERANCE) {
            if (connectionUpcallsDisabled) {
                return;
            }
            handleLostConnection(johnDoe.ibis());
        }
    }

    /**
     * Used in fault tolerant Satin If the job is being redone (redone flag is
     * set to true) perform a lookup in the global result table
     * 
     * @param r
     *            invocation record of the job
     * @return true if an entry was found, false otherwise
     */
    protected boolean globalResultTableCheck(InvocationRecord r) {
        if (TABLE_CHECK_TIMING) {
            // redoTimer.start();
        }

        GlobalResultTable.Key key = new GlobalResultTable.Key(r);
        GlobalResultTable.Value value = null;
        synchronized (this) {
            value = globalResultTable.lookup(key, true);
        }

        if (value == null) {
            if (TABLE_CHECK_TIMING) {
                // redoTimer.stop();
            }
            return false;
        }

        if (GLOBAL_RESULT_TABLE_REPLICATED) {

            if (ASSERTS && value.type != GlobalResultTable.Value.TYPE_RESULT) {
                grtLogger.fatal("SATIN '" + ident
                        + "': EEK using replicated table, but got a non-result "
                        + "value!");
                System.exit(1); // Failed assertion
            }
            ReturnRecord rr = value.result;
            rr.assignTo(r);
            if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                r.spawnCounter.decr(r);
            } else {
                r.spawnCounter.value--;
            }
            if (TABLE_CHECK_TIMING) {
                // redoTimer.stop();
            }
            return true;

        }
		//distributed table
		if (value.type == GlobalResultTable.Value.TYPE_POINTER) {
		    //remote result

		    Victim v = null;
		    synchronized (this) {
		        if (deadIbises.contains(value.owner)) {
		            //the one who's got the result has crashed
		            if (TABLE_CHECK_TIMING) {
		                // redoTimer.stop();
		            }
		            return false;
		        }

		        if (grtLogger.isDebugEnabled()) {
		            grtLogger.debug("SATIN '" + ident
		                     + "': sending a result request of " + key
		                     + " to " + value.owner);
		        }
		        v = getVictimNoWait(value.owner);
		    }

		    if (v == null) {
		        if (TABLE_CHECK_TIMING) {
		            // redoTimer.stop();
		        }
		        return false;
		    }
		    //put the job in the stolen jobs list.
		    synchronized (this) {
		        r.stealer = value.owner;
		        addToOutstandingJobList(r);
		    }
		    //send a request to the remote node
		    try {
		        WriteMessage m = v.newMessage();
		        m.writeByte(Protocol.RESULT_REQUEST);
		        m.writeObject(key);
                        m.writeObject(r.stamp);
		        m.finish();
		    } catch (IOException e) {
		        grtLogger.warn("SATIN '" + ident
		                + "': trying to send RESULT_REQUEST but got "
		                + "exception: " + e, e);
		        synchronized (this) {
		            outstandingJobs.remove(r);
		        }
		        return false;
		    }
		    if (TABLE_CHECK_TIMING) {
		        // redoTimer.stop();
		    }
		    return true;
		}

		if (FT_WITHOUT_ABORTS && ASSERTS) {
		    if (value.type == GlobalResultTable.Value.TYPE_LOCK) {
		        //i don't think that should happen
		        grtLogger.fatal("SATIN '" + ident
		                + "': found local unfinished job in the table!! "
		                + key);
		        System.exit(1);     // Failed assertion
		    }
		}

		if (value.type == GlobalResultTable.Value.TYPE_RESULT) {
		    //local result, handle normally
		    ReturnRecord rr = value.result;
		    rr.assignTo(r);
		    if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
		        r.spawnCounter.decr(r);
		    } else {
		        r.spawnCounter.value--;
		    }
		    if (TABLE_CHECK_TIMING) {
		        // redoTimer.stop();
		    }
		    return true;
		}

        return false;

    }

    // Used for fault tolerance
    void addToAbortAndStoreList(Stamp stamp) {
        if (ASSERTS) {
            assertLocked(this);
        }
        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("SATIN '" + ident + ": got abort message");
        }
        abortAndStoreList.add(stamp);
        gotAbortsAndStores = true;
    }

    // Used for fault tolerance
    synchronized void handleAbortsAndStores() {
        Stamp stamp;

        while (true) {
            if (abortAndStoreList.count > 0) {
                stamp = abortAndStoreList.stamps[0];
                abortAndStoreList.removeIndex(0);
            } else {
                gotAbortsAndStores = false;
                return;
            }

            killAndStoreChildrenOf(stamp);

        }
    }

    public void mustLeave(IbisIdentifier[] ids) {
        for (int i = 0; i < ids.length; i++) {
            if (ident.equals(ids[i])) {
                gotDelete = true;
                break;
            }
        }
    }

    public void deleteCluster(String clusterName) {

        if (ftLogger.isInfoEnabled()) {
            ftLogger.info("SATIN '" + ident + "': delete cluster "
                    + clusterName);
        }

        if (ident.cluster().equals(clusterName)) {
            gotDeleteCluster = true;
        }
    }

    synchronized void handleDelete() {

        gotDelete = false;

        if (FAULT_TOLERANCE) {
            if (GLOBAL_RESULT_TABLE_REPLICATED) {
                onStack.storeAll();
            } else {
                Victim victim = victims.getRandomLocalVictim();
                onStack.pushAll(victim);
            }
            killedOrphans += onStack.size();
            killedOrphans += q.size();
        }

        System.exit(0);
    }

    synchronized void handleDeleteCluster() {

        if (ftLogger.isInfoEnabled()) {
            ftLogger.info("SATIN '" + ident
                    + "': handle delete cluster");
        }

        gotDeleteCluster = false;

        if (FAULT_TOLERANCE) {
            if (GLOBAL_RESULT_TABLE_REPLICATED) {
                onStack.storeAll();
            } else {
                Victim victim = victims.getRandomRemoteVictim();
                onStack.pushAll(victim);
            }
            killedOrphans += onStack.size();
            killedOrphans += q.size();
        }

        System.exit(0);
    }

}
