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
        if (COMM_DEBUG) {
            out.print("SATIN '" + ident.name() + ": handling crashes");
        }

        gotCrashes = false;
        IbisIdentifier id = null;
        while (crashedIbises.size() > 0) {
            id = (IbisIdentifier) crashedIbises.remove(0);

            // Let the Ibis registry know ...
            try {
                r.dead(id);
            } catch (IOException e) {
                System.err
                        .println("SATIN '"
                                + ident.name()
                                + "' :exception while notifying registry about crash of "
                                + id.name() + ": " + e.getMessage());
            }

            if (COMM_DEBUG) {
                out.println("SATIN '" + ident.name() + ": handling crash of "
                        + id.name());
            }

            if (algorithm instanceof ClusterAwareRandomWorkStealing) {
                ((ClusterAwareRandomWorkStealing) algorithm)
                        .checkAsyncVictimCrash(id);
            }

            if (!FT_NAIVE) {
                globalResultTable.removeReplica(id);
            }

            /*if (killTime > 0) {
             System.err.println("SATIN '" + ident.name() + "': " + id +  " HAS CRASHED!!!");
             }*/

            if (id.equals(masterIdent) && /*quick hack*/!(killTime > 0)) {
                //master has crashed, let's elect a new one
                System.err.println("SATIN '" + ident.name() + "': MASTER ("
                        + masterIdent + ") HAS CRASHED!!!");
                try {
                    masterIdent = r.elect("satin master");
                    if (masterIdent.equals(ident)) {
                        master = true;
                    }
                    //barrier ports
                    if (master) {
                        barrierReceivePort = barrierPortType
                                .createReceivePort("satin barrier receive port on "
                                        + ident.name());
                        barrierReceivePort.enableConnections();
                    } else {
                        barrierSendPort.close();
                        barrierSendPort = barrierPortType
                                .createSendPort("satin barrier send port on "
                                        + ident.name());
                        ReceivePortIdentifier barrierIdent = lookup("satin barrier receive port on "
                                + masterIdent.name());
                        connect(barrierSendPort, barrierIdent);
                    }

                    //statistics
                    if (stats && master) {
                        totalStats = new StatsMessage();
                    }

                } catch (IOException e) {
                    System.err.println("SATIN '" + ident.name()
                            + "' :exception while electing a new master "
                            + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err.println("SATIN '" + ident.name()
                            + "' :exception while electing a new master "
                            + e.getMessage());
                }
                restarted = true;
            }

            if (id.equals(clusterCoordinatorIdent)
                    && /*quick hack*/!(killTime > 0)) {
                try {
                    clusterCoordinatorIdent = r.elect("satin "
                            + ident.cluster() + " cluster coordinator");
                    if (clusterCoordinatorIdent.equals(ident)) {
                        clusterCoordinator = true;
                    }
                } catch (IOException e) {
                    System.err
                            .println("SATIN '"
                                    + ident.name()
                                    + "' :exception while electing a new cluster coordinator "
                                    + e.getMessage());
                } catch (ClassNotFoundException e) {
                    System.err
                            .println("SATIN '"
                                    + ident.name()
                                    + "' :exception while electing a new cluster coordinator "
                                    + e.getMessage());
                }
            }

            if (master) {
                System.err.println(id.name() + " has crashed");
            }

            if (!FT_NAIVE) {
                if (FT_WITHOUT_ABORTS) {
                    //store orphans in the table
                    onStack.storeOrphansOf(id);
                } else {
                    //abort all jobs stolen from id or descendants of jobs stolen from
                    // id
                    killAndStoreSubtreeOf(id);
                }
            }

            //if using CRS, remove the asynchronously stolen job if it is owned
            // by a
            //crashed machine
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

        if (COMM_DEBUG) {
            out.println("SATIN '" + ident.name() + ": numCrashes handled: "
                    + numCrashesHandled);
        }

        notifyAll();

    }

    // Used for fault tolerance
    void sendAbortAndStoreMessage(InvocationRecord r) {
        if (ASSERTS) {
            assertLocked(this);
        }

        if (ABORT_DEBUG) {
            out.println("SATIN '" + ident.name()
                    + ": sending abort and store message to: " + r.stealer
                    + " for job " + r.stamp);
        }

        if (deadIbises.contains(r.stealer)) {
            /* don't send abort and store messages to crashed ibises */
            return;
        }

        try {
            SendPort s = getReplyPortNoWait(r.stealer);
            if (s == null)
                return;

            WriteMessage writeMessage = s.newMessage();
            writeMessage.writeByte(Protocol.ABORT_AND_STORE);
            writeMessage.writeInt(r.parentStamp);
            writeMessage.writeObject(r.parentOwner);
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
            System.err.println("SATIN '" + ident.name()
                    + "': Got Exception while sending abort message: " + e);
            // This should not be a real problem, it is just inefficient.
            // Let's continue...
            // System.exit(1);
        }
    }

    void killAndStoreChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
        if (ASSERTS) {
            assertLocked(this);
        }

        //try work queue, outstanding jobs and jobs on the stack
        //but try stack first, many jobs in q are children of stack jobs
        onStack.killAndStoreChildrenOf(targetStamp, targetOwner);
        q.killChildrenOf(targetStamp, targetOwner);
        outstandingJobs.killAndStoreChildrenOf(targetStamp, targetOwner);
    }

    void killAndStoreSubtreeOf(IbisIdentifier targetOwner) {
        onStack.killAndStoreSubtreeOf(targetOwner);
        q.killSubtreeOf(targetOwner);
        outstandingJobs.killAndStoreSubtreeOf(targetOwner);
    }

    /**
     * Attach a child to its parent's finished children list
     */
    void attachToParentFinished(InvocationRecord r) {
        if (r.parent != null) {
            r.finishedSibling = r.parent.finishedChild;
            r.parent.finishedChild = r;
        } else if (r.owner.equals(ident)) {
            if (ASSERTS && !r.owner.equals(ident)) {
                System.err.println("SATIN '" + ident.name()
                        + "': parent of a restarted job on another machine!");
                System.exit(1);
            }
            r.finishedSibling = rootFinishedChild;
            rootFinishedChild = r;
        }
        //remove the job's children list
        r.finishedChild = null;
    }

    /**
     * Attach a child to its parent's list of children which need to be restarted
     */
    void attachToParentToBeRestarted(InvocationRecord r) {
        if (r.parent != null) {
            r.toBeRestartedSibling = r.parent.toBeRestartedChild;
            r.parent.toBeRestartedChild = r;
        } else if (r.owner.equals(ident)) {
            if (ASSERTS && !r.owner.equals(ident)) {
                System.err.println("SATIN '" + ident.name()
                        + "': parent of a restarted job on another machine!");
                System.exit(1);
            }
            r.toBeRestartedSibling = rootToBeRestartedChild;
            rootToBeRestartedChild = r;
        }
        //remove the job's children list
        r.toBeRestartedChild = null;
    }

    //connect upcall functions
    public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant) {
        //	    System.err.println("SATIN '" + ident.name() + "': got gotConnection
        // upcall");
        return true;
    }

    synchronized void handleLostConnection(IbisIdentifier dead) {
        if (!deadIbises.contains(dead)) {
            crashedIbises.add(dead);
            deadIbises.add(dead);
            if (dead.equals(currentVictim)) {
                currentVictimCrashed = true;
            }
            gotCrashes = true;
            Victim v = victims.remove(dead);
            notifyAll();
            if (v != null && v.s != null) {
                try {
                    v.s.close();
                } catch (IOException e) {
                    System.err.println("port.free() throws exception "
                            + e.getMessage());
                }
            }

        }
    }

    public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe,
            Exception reason) {
        if (COMM_DEBUG) {
            System.err.println("SATIN '" + ident.name()
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
        if (COMM_DEBUG) {
            System.err.println("SATIN '" + ident.name()
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
            //			redoTimer.start();
        }

        GlobalResultTable.Key key = new GlobalResultTable.Key(r);
        GlobalResultTable.Value value = null;
        synchronized (this) {
            value = globalResultTable.lookup(key, true);
        }

        if (value == null) {
            if (TABLE_CHECK_TIMING) {
                //					redoTimer.stop();
            }
            return false;
        }

        if (GLOBAL_RESULT_TABLE_REPLICATED) {

            if (ASSERTS && value.type != GlobalResultTable.Value.TYPE_RESULT) {
                out
                        .println("SATIN '"
                                + ident.name()
                                + "': EEK using replicated table, but got a non-result value!");
                System.exit(1);
            }
            ReturnRecord rr = value.result;
            rr.assignTo(r);
            if (SPAWN_DEBUG) {
                r.spawnCounter.decr(r);
            } else
                r.spawnCounter.value--;
            if (TABLE_CHECK_TIMING) {
                //					redoTimer.stop();
            }
            return true;

        } else {
            //distributed table
            if (value.type == GlobalResultTable.Value.TYPE_POINTER) {
                //remote result

                SendPort s = null;
                synchronized (this) {
                    if (deadIbises.contains(value.owner)) {
                        //the one who's got the result has crashed
                        if (TABLE_CHECK_TIMING) {
                            //								redoTimer.stop();
                        }
                        return false;
                    }

                    //						System.err.println("SATIN '" + ident.name() + "': sending a result request of " + key + " to " + value.owner.name());
                    s = getReplyPortNoWait(value.owner);
                }

                if (s == null) {
                    if (TABLE_CHECK_TIMING) {
                        //							redoTimer.stop();
                    }
                    return false;
                }
                //put the job in the stolen jobs list					
                synchronized (this) {
                    r.stealer = value.owner;
                    addToOutstandingJobList(r);
                }
                //send a request to the remote node
                try {
                    WriteMessage m = s.newMessage();
                    m.writeByte(Protocol.RESULT_REQUEST);
                    m.writeObject(key);
                    m.writeInt(r.stamp); //stamp and owner are not
                    // neccessary when using
                    m.writeObject(r.owner);//globally unique stamps, but
                    // let's not make things too
                    // complicated..
                    m.finish();
                } catch (IOException e) {
                    System.err
                            .println("SATIN '"
                                    + ident.name()
                                    + "': trying to send RESULT_REQUEST but got exception: "
                                    + e.getMessage());
                    synchronized (this) {
                        outstandingJobs.remove(r);
                    }
                    return false;
                }
                if (TABLE_CHECK_TIMING) {
                    //						redoTimer.stop();
                }
                return true;
            }

            if (FT_WITHOUT_ABORTS && ASSERTS) {
                if (value.type == GlobalResultTable.Value.TYPE_LOCK) {
                    //i don't think that should happen
                    System.err.println("SATIN '" + ident.name()
                            + "': found local unfinished job in the table!! "
                            + key);
                    System.exit(1);
                }
            }

            if (value.type == GlobalResultTable.Value.TYPE_RESULT) {
                //local result, handle normally
                ReturnRecord rr = value.result;
                rr.assignTo(r);
                if (SPAWN_DEBUG) {
                    r.spawnCounter.decr(r);
                } else
                    r.spawnCounter.value--;
                if (TABLE_CHECK_TIMING) {
                    //						redoTimer.stop();
                }
                return true;
            }
        }

        return false;

    }

    // Used for fault tolerance
    void addToAbortAndStoreList(int stamp, IbisIdentifier owner) {
        if (ASSERTS) {
            assertLocked(this);
        }
        if (ABORT_DEBUG) {
            out.println("SATIN '" + ident.name() + ": got abort message");
        }
        abortAndStoreList.add(stamp, owner);
        gotAbortsAndStores = true;
    }

    // Used for fault tolerance
    synchronized void handleAbortsAndStores() {
        int stamp;
        IbisIdentifier owner;

        while (true) {
            if (abortAndStoreList.count > 0) {
                stamp = abortAndStoreList.stamps[0];
                owner = abortAndStoreList.owners[0];
                abortAndStoreList.removeIndex(0);
            } else {
                gotAbortsAndStores = false;
                return;
            }

            killAndStoreChildrenOf(stamp, owner);

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

        System.err.println("SATIN '" + ident.name() + "': delete cluster "
                + clusterName);

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

        System.err.println("SATIN '" + ident.name()
                + "': handle delete cluster");

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