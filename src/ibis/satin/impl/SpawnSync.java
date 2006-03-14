/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;

public abstract class SpawnSync extends Termination {

    private static SpawnCounter spawnCounterCache = null;

    boolean idleStarted = false;

    /**
     * Obtains a new spawn counter. This does not need to be synchronized, only
     * one thread spawns.
     * 
     * @return a new spawn counter.
     */
    static public final SpawnCounter newSpawnCounter() {
        if (spawnCounterCache == null) {
            return new SpawnCounter();
        }

        SpawnCounter res = spawnCounterCache;
        spawnCounterCache = res.next;

        return res;
    }

    /**
     * Makes a spawn counter available for recycling. This does not need to be
     * synchronized, only one thread spawns.
     * 
     * @param s
     *            the spawn counter made available.
     */
    static public final void deleteSpawnCounter(SpawnCounter s) {
        if (ASSERTS && s.value < 0) {
            spawnLogger.fatal("deleteSpawnCounter: spawncouner < 0, val ="
                    + s.value, new Throwable());
            System.exit(1); // Failed assertion
        }

        // Only put it in the cache if its value is 0.
        // If not, there may be references to it yet.
        if (s.value == 0) {
            s.next = spawnCounterCache;
            spawnCounterCache = s;
        }
    }

    private final void callSatinFunctionPreAsserts(InvocationRecord r) {
        if (r == null) {
            spawnLogger.fatal("SATIN '" + ident
                    + ": EEK, r = null in callSatinFunc", new Throwable());
            System.exit(1); // Failed assertion
        }

        if (r.aborted) {
            spawnLogger.fatal("SATIN '" + ident + ": spawning aborted job!",
                    new Throwable());
            System.exit(1); // Failed assertion
        }

        if (r.owner == null) {
            spawnLogger.fatal("SATIN '" + ident
                    + ": EEK, r.owner = null in callSatinFunc, r = " + r,
                    new Throwable());
            System.exit(1); // Failed assertion
        }

        if (r.owner.equals(ident)) {
            if (r.spawnCounter == null) {
                spawnLogger.fatal("SATIN '" + ident
                        + ": EEK, r.spawnCounter = null in callSatinFunc, "
                        + "r = " + r, new Throwable());
                System.exit(1); // Failed assertion
            }

            if (r.spawnCounter.value < 0) {
                spawnLogger.fatal("SATIN '" + ident
                        + ": spawncounter < 0 in callSatinFunc",
                        new Throwable());
                System.exit(1); // Failed assertion
            }

            if (ABORTS && r.parent == null && parentOwner.equals(ident)
                    && r.parentStamp != null) {
                spawnLogger.fatal("SATIN '" + ident
                        + ": parent is null for non-root, should not "
                        + "happen here! job = " + r, new Throwable());
                System.exit(1); // Failed assertion
            }
        }
    }

    private final void callSatinFunctionPostAsserts(InvocationRecord r) {
        if (r.spawnCounter.value < 0) {

            spawnLogger.fatal("SATIN '" + ident
                    + ": Just made spawncounter < 0", new Throwable());
            System.exit(1); // Failed assertion
        }

        if (!ABORTS && r.eek != null) {
            spawnLogger.fatal("SATIN '" + ident + ": Got exception: " + r.eek,
                    r.eek);
            System.exit(1); // Failed assertion
        }
    }

    private final void callSatinLocalFunction(InvocationRecord r) {
        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident
                    + "': callSatinFunc: spawn counter = "
                    + r.spawnCounter.value);
        }
        if (ABORTS) {
            if (SPAWN_STATS) {
                jobsExecuted++;
            }
            try {
                r.runLocal();
            } catch (Throwable t) {
                // This can only happen if an inlet has thrown an
                // exception, or if there was no try-catch block around
                // the spawn (i.e. no inlet).
                // The semantics of this: all work is aborted,
                // and the exception is passed on to the spawner.
                // The parent is aborted, it must handle the exception.
                // Note: this can now also happen on an abort. Check for
                // the AbortException!
                if (! (t instanceof AbortException)) {
                    r.eek = t;
                    handleInlet(r);
                } else if (abortLogger.isDebugEnabled()) {
                    abortLogger.debug("Caught abort exception " + t, t);
                }
            }
        } else { // NO aborts
            if (SPAWN_STATS) {
                jobsExecuted++;
            }
            try {
                r.runLocal();
            } catch (Throwable t) {
                throw new IbisError("Unexpected exception in runLocal", t);
            }
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            r.spawnCounter.decr(r);
        } else {
            r.spawnCounter.value--;
        }

        if (ASSERTS) {
            callSatinFunctionPostAsserts(r);
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + ": callSatinFunc: stamp = "
                    + r.stamp + ", parentStamp = " + r.parentStamp
                    + ", parentOwner = " + r.parentOwner + " spawn counter = "
                    + r.spawnCounter + "("
                    + r.spawnCounter.value + ") DONE");
            if (r.eek != null) {
                spawnLogger.debug("SATIN '" + ident + ": exception was "
                        + r.eek, r.eek);
            }
        }

        if (FAULT_TOLERANCE && !FT_WITHOUT_ABORTS && !FT_NAIVE) {
            //job is finished
            attachToParentFinished(r);
        }
    }

    private final void callSatinRemoteFunction(InvocationRecord r) {
        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + ident + "': RUNNING REMOTE CODE!");
        }
        ReturnRecord rr = null;
        if (SPAWN_STATS) {
            jobsExecuted++;
        }
        rr = r.runRemote();
        if (ABORTS) {
            rr.eek = r.eek;
        }

        if (stealLogger.isInfoEnabled()) {
            if (r.eek != null) {
                stealLogger.info("SATIN '" + ident
                        + "': RUNNING REMOTE CODE GAVE EXCEPTION: " + r.eek,
                        r.eek);
            } else {
                stealLogger.info("SATIN '" + ident
                        + "': RUNNING REMOTE CODE DONE!");
            }
        }

        // send wrapper back to the owner
        if (!r.aborted) {
            sendResult(r, rr);
        }

        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + ident
                    + "': REMOTE CODE SEND RESULT DONE!");
        }
    }

    protected final void callSatinFunction(InvocationRecord r) {
        InvocationRecord oldParent = null;
        Stamp oldParentStamp = null;
        IbisIdentifier oldParentOwner = null;

        if (IDLE_TIMING && idleStarted) {
            idleStarted = false;
            if (idleLogger.isDebugEnabled()) {
                idleLogger.debug("SATIN '" + ident + "': idle stop");
            }
            idleTimer.stop();
        }

        // not needed I think, already done in sync (Rob)
        // It IS needed. callSatinFunc is not only called from sync,
        // and we MUST make sure that steals don't overtake aborts. (Ceriel)
        handleDelayedMessages();

        if (ABORTS || FAULT_TOLERANCE) {
            oldParent = parent;
            oldParentStamp = parentStamp;
            oldParentOwner = parentOwner;
        }

        if (ASSERTS) {
            callSatinFunctionPreAsserts(r);
        }

        if ((ABORTS || FAULT_TOLERANCE) && r.parent != null && r.parent.aborted) {
            if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                r.spawnCounter.decr(r);
            } else {
                r.spawnCounter.value--;
            }
            if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("SATIN '" + ident
                        + "' spawnCounter = " + r.spawnCounter
                        + "(" + r.spawnCounter.value + ")"
                        + " spawning job, parent was aborted! job = " + r
                        + ", parent = " + r.parent);
            }
            if (ASSERTS) {
                if (r.spawnCounter.value < 0) {
                    spawnLogger.fatal("SATIN '" + ident
                            + ": Just made spawncounter < 0", new Throwable());
                    System.exit(1); // Failed assertion
                }
            }
            return;
        }

        if (ABORTS || FAULT_TOLERANCE) {
            onStack.push(r);
            parent = r;
            parentStamp = r.stamp;
            parentOwner = r.owner;
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': callSatinFunc: stamp = "
                    + r.stamp + ", owner = "
                    + (r.owner.equals(ident) ? "me" : r.owner.name())
                    + ", parentStamp = " + r.parentStamp + ", parentOwner = "
                    + r.parentOwner);
        }

        if (r.owner.equals(ident)) {
            callSatinLocalFunction(r);
        } else { // we are running a job that I stole from another machine
            callSatinRemoteFunction(r);
        }

        if (ABORTS || FAULT_TOLERANCE) {
            // restore these, there may be more spawns afterwards...
            parentStamp = oldParentStamp;
            parentOwner = oldParentOwner;
            parent = oldParent;
            onStack.pop();
        }

        if (ABORTS && abortLogger.isDebugEnabled()
                && r.aborted) {
            if (r.eek != null) {
                abortLogger.debug("Job on the stack was aborted: " + r.stamp
                        + " EEK = " + r.eek, r.eek);
            } else {
                abortLogger.debug("Job on the stack was aborted: " + r.stamp
                        + " EEK = null");
            }
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': call satin func done!");
        }
    }

    /**
     * Spawns the method invocation as described by the specified invocation
     * record. The invocation record is added to the job queue maintained by
     * this Satin.
     * 
     * @param r
     *            the invocation record specifying the spawned invocation.
     */
    public final void spawn(InvocationRecord r) {
        if (ASSERTS) {
            if (algorithm instanceof MasterWorker) {
                synchronized (this) {
                    if (!ident.equals(masterIdent)) {
                        spawnLogger.fatal("with the master/worker algorithm, "
                                + "work can only be spawned on the master!");
                        System.exit(1); // Failed assertion
                    }
                }
            }
        }

        if (ABORTS && parent != null && parent.aborted) {
            abortLogger.debug("parent " + parent.stamp
                    + " is aborted, spawn ignored");
            return;
        }

        if (SPAWN_STATS) {
            spawns++;
        }

        r.stamp = new Stamp(parentStamp);
        r.owner = ident;

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            r.spawnCounter.incr(r);
        } else {
            r.spawnCounter.value++;
        }

        if (ABORTS || FAULT_TOLERANCE) {
            r.parentStamp = parentStamp;
            r.parentOwner = parentOwner;
            r.parent = parent;

            /*
             * if(parent != null) { for (int i=0; i <parent.parentStamps.size();
             * i++) { r.parentStamps.add(parent.parentStamps.get(i));
             * r.parentOwners.add(parent.parentOwners.get(i)); } }
             * 
             * r.parentStamps.add(parentStamp);
             * r.parentOwners.add(parentOwner);
             */
        }

        if (FAULT_TOLERANCE && !FT_NAIVE) {
            if (parent != null && parent.reDone || parent == null && restarted) {
                r.reDone = true;
            }
        }

        if (FAULT_TOLERANCE && !FT_NAIVE) {
            if (r.reDone) {
                if (globalResultTableCheck(r)) {
                    return;
                }
            }
        }

        q.addToHead(r);

        algorithm.jobAdded();

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': Spawn, counter = "
                    + r.spawnCounter + "(" + r.spawnCounter.value + ")"
                    + ", stamp = " + r.stamp
                    + ", parentStamp = " + r.parentStamp + ", owner = "
                    + r.owner + ", parentOwner = " + r.parentOwner);
        }
    }

    private final void noWorkInQueue() {
        InvocationRecord r;

        if (FAULT_TOLERANCE && FT_WITHOUT_ABORTS) {
            //before you steal, check if kids need
            //to be restarted
            InvocationRecord curr = null;
            if (parent != null) {
                curr = parent.toBeRestartedChild;
                parent.toBeRestartedChild = null;
            } else {
                curr = rootToBeRestartedChild;
                rootToBeRestartedChild = null;
            }
            if (curr != null) {
                int i = 0;
                while (curr != null) {
                    //is it really necessary??
                    if (!globalResultTableCheck(curr)) {
                        q.addToTail(curr);
                    }
                    InvocationRecord tmp = curr;
                    curr = curr.toBeRestartedSibling;
                    tmp.toBeRestartedSibling = null;
                    i++;
                }
            } else {
                r = algorithm.clientIteration();
                if (r != null) {
                    if (SHARED_OBJECTS) {
                        //restore shared object references
                        try {
                            r.setSOReferences();
                            executeGuard(r);
                        } catch (SOReferenceSourceCrashedException e) {
                            //the source has crashed - abort the job
                            r = null;
                            return;
                        }

                    }
                    if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                        spawnLogger.debug("SATIN '" + ident
                                + "': Sync, start stolen job");
                    }
                    callSatinFunction(r);
                    if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                        spawnLogger.debug("SATIN '" + ident
                                + "': Sync, finish stolen job");
                    }
                } else if (IDLE_TIMING && !idleStarted) {
                    idleStarted = true;
                    if (idleLogger.isDebugEnabled()) {
                        idleLogger.debug("SATIN '" + ident
                                + "': sync idle start");
                    }
                    idleTimer.start();
                }
            }
        } else {
            r = algorithm.clientIteration();
            if (r != null) {
                if (SHARED_OBJECTS) {
                    //restore shared object references
                    try {
                        r.setSOReferences();
                        executeGuard(r);
                    } catch (SOReferenceSourceCrashedException e) {
                        //the source has crashed - abort the job
                        r = null;
                        return;
                    }
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': Sync, start stolen job");
                }
                callSatinFunction(r);
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': Sync, finish stolen job");
                }
            } else if (IDLE_TIMING && !idleStarted) {
                idleStarted = true;
                if (idleLogger.isDebugEnabled()) {
                    idleLogger.debug("SATIN '" + ident + "': sync idle start");
                }
                idleTimer.start();
            }
        }

    }

    /**
     * Waits for the jobs as specified by the spawncounter given, but meanwhile
     * execute jobs from the end of the jobqueue (or rather, the head of the job
     * queue, where new jobs are added).
     * 
     * @param s
     *            the spawncounter.
     */

    public final void sync(SpawnCounter s) {
        InvocationRecord r;

        if (SPAWN_STATS) {
            syncs++;
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': Sync, counter = "
                    + s + "(" + s.value + ")");
        }

        if (s.value == 0) { // sync is poll
            satinPoll();
            handleDelayedMessages();
            if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                spawnLogger.debug("SATIN '" + ident + "': Sync of counter " + s + " returns");
            }
            return;
        }

        while (s.value > 0) {
            satinPoll();
            handleDelayedMessages();

            r = q.getFromHead(); // Try the local queue
            if (r != null) {
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': Sync, start own job");
                }
                callSatinFunction(r);
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': Sync, finish own job");
                }
            } else {
                noWorkInQueue();
            }
        }

        if (idleStarted) {
            idleStarted = false;
            if (idleLogger.isDebugEnabled()) {
                idleLogger.debug("SATIN '" + ident
                        + "': sync returns; idle stop");
            }
            if (IDLE_TIMING) {
                idleTimer.stop();
            }
        }

        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': Sync of counter "
                    + s + "(" + s.value + ") returns");
        }
    }

    /**
     * Implements the main client loop: steal jobs and execute them.
     */
    public void client() {
        if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': starting client!");
        }

        while (!exiting) {
            // steal and run jobs

            satinPoll();
            handleDelayedMessages();

            InvocationRecord r = algorithm.clientIteration();
            if (r != null) {
                if (SHARED_OBJECTS) {
                    //restore shared object references
                    try {
                        r.setSOReferences();
                        executeGuard(r);
                    } catch (SOReferenceSourceCrashedException e) {
                        //the source has crashed - abort the job
                        r = null;
                        continue;
                    }
                }
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': client, start stolen job");
                }
                callSatinFunction(r);
                if (ENABLE_SPAWN_LOGGING && spawnLogger.isDebugEnabled()) {
                    spawnLogger.debug("SATIN '" + ident
                            + "': client, finish stolen job");
                }
            } else if (!idleStarted) {
                idleStarted = true;
                if (idleLogger.isDebugEnabled()) {
                    idleLogger
                            .debug("SATIN '" + ident + "': client idle start");
                }
                if (IDLE_TIMING) {
                    idleTimer.start();
                }
            }

            //for ft
            if (master) {
                return;
            }
        }

    }
}
