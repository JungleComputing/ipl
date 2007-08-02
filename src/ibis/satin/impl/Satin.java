/* $Id: Satin.java 3687 2006-04-26 09:39:58Z rob $ */

package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;
import ibis.satin.impl.aborts.AbortException;
import ibis.satin.impl.aborts.Aborts;
import ibis.satin.impl.communication.Communication;
import ibis.satin.impl.communication.Protocol;
import ibis.satin.impl.faultTolerance.FaultTolerance;
import ibis.satin.impl.loadBalancing.ClusterAwareRandomWorkStealing;
import ibis.satin.impl.loadBalancing.LoadBalancing;
import ibis.satin.impl.loadBalancing.LoadBalancingAlgorithm;
import ibis.satin.impl.loadBalancing.MasterWorker;
import ibis.satin.impl.loadBalancing.RandomWorkStealing;
import ibis.satin.impl.loadBalancing.VictimTable;
import ibis.satin.impl.sharedObjects.SOInvocationRecord;
import ibis.satin.impl.sharedObjects.SharedObjects;
import ibis.satin.impl.spawnSync.DoubleEndedQueue;
import ibis.satin.impl.spawnSync.IRStack;
import ibis.satin.impl.spawnSync.IRVector;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.ReturnRecord;
import ibis.satin.impl.spawnSync.SpawnCounter;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.util.Log;

import java.util.Vector;

public final class Satin implements Config {

    private static final int SUGGESTED_QUEUE_SIZE = 1000;

    public static final boolean GLOBAL_PAUSE_RESUME = true;
    
    private static Satin thisSatin;

    public final Communication comm;

    public final LoadBalancing lb;

    public final FaultTolerance ft;

    public final SharedObjects so;

    public final Aborts aborts;

    public final IbisIdentifier ident; // this ibis

    /** Am I the root (the one running main)? */
    private boolean master;

    /** The ibis identifier for the master (the one running main). */
    private IbisIdentifier masterIdent;

    /** Am I the cluster coordinator? */
    public boolean clusterCoordinator;

    /** My scheduling algorithm. */
    public LoadBalancingAlgorithm algorithm;

    /** Set to true if we need to exit for some reason. */
    public volatile boolean exiting;

    /** The work queue. Contains jobs that were spawned, but not yet executed. */
    public final DoubleEndedQueue q;

    /**
     * This vector contains all jobs that were stolen from me. 
     * Used to locate the invocation record corresponding to the result of a
     * remote job.
     */
    public final IRVector outstandingJobs;

    /** The jobs that are currently being executed, they are on the Java stack. */
    public final IRStack onStack;

    public Statistics totalStats;

    public Statistics stats = new Statistics();

    /** The invocation record that is the parent of the current job. */
    public InvocationRecord parent;

    public volatile boolean currentVictimCrashed;

    /** All victims, myself NOT included. The elements are Victims. */
    public final VictimTable victims;

    /**
     * Used for fault tolerance. All ibises that once took part in the
     * computation, but then crashed. Assumption: ibis identifiers are uniqe in
     * time; the same ibis cannot crash and join the computation again.
     */
    public final Vector<IbisIdentifier> deadIbises = new Vector<IbisIdentifier>();

    static {
        properties.checkProperties(PROPERTY_PREFIX, sysprops, null, true);
    }

    /**
     * Creates a Satin instance and also an Ibis instance to run Satin on. This
     * constructor gets called by the rewritten main() from the application, and
     * the argument array from main is passed to this constructor. Which ibis is
     * chosen depends, a.o., on these arguments.
     */
    public Satin() {
        if (thisSatin != null) {
            throw new Error(
                "multiple satin instances are currently not supported");
        }
        thisSatin = this;
        
        Log.initLog4J("ibis.satin");

        q = new DoubleEndedQueue(this);

        outstandingJobs = new IRVector(this);
        onStack = new IRStack(this);

        ft = new FaultTolerance(this); // this must be first, it handles registry upcalls 
        comm = new Communication(this); // creates ibis
        ident = comm.ibis.identifier();
        ft.electClusterCoordinator(); // need ibis for this
        victims = new VictimTable(this); // need ibis for this

        lb = new LoadBalancing(this);
        so = new SharedObjects(this);
        aborts = new Aborts(this);
        
        // elect the master
        setMaster(comm.electMaster());

        createLoadBalancingAlgorithm();
        
        if (DUMP) {
            DumpThread dumpThread = new DumpThread(this);
            Runtime.getRuntime().addShutdownHook(dumpThread);
        }

        stats.totalTimer.start();

        comm.enableConnections();
        
        // this opens the world, other ibises might join from this point
        // we need the master to be set before this call
        ft.init(); 
    }

    /**
     * Called at the end of the rewritten "main", to do a synchronized exit.
     */
    public void exit() {
        exit(0);
    }

    private void exit(int status) {

        stats.totalTimer.stop();

        if (STATS && DETAILED_STATS) {
            stats.printDetailedStats(ident);
            comm.ibis.printStatistics(System.out);
        }

        // Do not accept new connections and joins.
        comm.disableUpcallsForExit();

        if (master) {
            synchronized (this) {
                exiting = true;
                notifyAll();
            }
            comm.bcastMessage(Protocol.EXIT);
            comm.waitForExitReplies();

            // OK, we have got the ack from everybody, now we know that there will be no 
            // further communication between nodes. Broadcast this again.
            comm.bcastMessage(Protocol.EXIT_STAGE2);
        } else {
            comm.sendExitAck();
            comm.waitForExitStageTwo();
        }

        // OK, we have got the ack from everybody, 
        // now we know that there will be no further communication between nodes.

        algorithm.exit(); // give the algorithm time to clean up

        int size;
        synchronized (this) {
            size = victims.size() + 1;
        }

        if (master && STATS) {
            // add my own stats
            stats.fillInStats();
            totalStats.add(stats);

            totalStats.printStats(size, stats.totalTimer.totalTimeVal());
        }

        so.exit();
        comm.closeSendPorts();
        comm.closeReceivePort();

        comm.end();

        if (commLogger.isDebugEnabled()) {
            commLogger.debug("SATIN '" + ident + "': exited");
        }

        // Do a gc, and run the finalizers. Useful for printing statistics in
        // Satin applications.
        // The app should register a shutdownhook. --Rob
        System.gc();
        System.runFinalization();

        System.exit(status); // Needed for IBM jit.
    }

    /**
     * Called at the end of the rewritten main in case the original main
     * threw an exception.
     */
    public void exit(Throwable e) {
        System.err.println("Exception in main: " + e);
        e.printStackTrace(System.err);
        exit(1);
    }

    /**
     * Spawns the method invocation as described by the specified invocation
     * record. The invocation record is added to the job queue maintained by
     * this Satin.
     * 
     * @param r the invocation record specifying the spawned invocation.
     */
    public void spawn(InvocationRecord r) {
        stats.spawns++;

        // If my parent is aborted, so am I.
        if (parent != null && parent.aborted) return;

        // Maybe this job is already in the global result table.
        // If so, we don't have to do it again.
        if (ft.checkForDuplicateWork(parent, r)) return;

        r.spawn(ident, parent);
        q.addToHead(r);
        algorithm.jobAdded();
    }

    /**
     * Waits for the jobs as specified by the spawncounter given, but meanwhile
     * execute jobs from the end of the jobqueue (or rather, the head of the job
     * queue, where new jobs are added).
     * 
     * @param s the spawncounter.
     */
    public void sync(SpawnCounter s) {
        stats.syncs++;

        if (s.getValue() == 0) { // A sync without spawns is a poll.
            handleDelayedMessages();
            return;
        }

        while (s.getValue() > 0) {
            InvocationRecord r = q.getFromHead(); // Try the local queue
            if (r != null) {
                callSatinFunction(r);
            } else {
                noWorkInQueue();
            }
        }
    }

    private void noWorkInQueue() {
        InvocationRecord r = algorithm.clientIteration();
        if (r != null && so.executeGuard(r)) {
            callSatinFunction(r);
        } else {
            handleDelayedMessages();
        }
    }

    /**
     * Implements the main client loop: steal jobs and execute them.
     */
    public void client() {
        if (spawnLogger.isDebugEnabled()) {
            spawnLogger.debug("SATIN '" + ident + "': starting client!");
        }

        while (!exiting) {
            // steal and run jobs
            noWorkInQueue();

            // Maybe the master crashed, and we were elected the new master.
            if (master) return;
        }
    }

    public void handleDelayedMessages() {
        // Handle messages received in upcalls.
        aborts.handleDelayedMessages();
        lb.handleDelayedMessages();
        ft.handleDelayedMessages();
        so.handleDelayedMessages();
    }

    /**
     * Aborts the spawns that are the result of the specified invocation record.
     * The invocation record of the invocation actually throwing the exception
     * is also specified, but it is valid only for clones with inlets.
     * 
     * @param outstandingSpawns
     *            parent of spawns that need to be aborted.
     * @param exceptionThrower
     *            invocation throwing the exception.
     */
    public synchronized void abort(InvocationRecord outstandingSpawns,
        InvocationRecord exceptionThrower) {
        // We do not need to set outstanding Jobs in the parent frame to null,
        // it is just used for assigning results.
        // get the lock, so no-one can steal jobs now, and no-one can change my
        // tables.
        stats.abortsDone++;

        if (abortLogger.isDebugEnabled()) {
            abortLogger.debug("ABORT called: outstandingSpanws = "
                    + outstandingSpawns
                    + ", exceptionThrower = " + exceptionThrower);
        }

        if (exceptionThrower != null) { // can be null if root does an abort.
            // kill all children of the parent of the thrower.
            aborts.killChildrenOf(exceptionThrower.getParentStamp());
        }

        // now kill mine
        if (outstandingSpawns != null) {
            Stamp stamp;
            InvocationRecord parent = outstandingSpawns.getParent();
            if (parent == null) {
                stamp = null;
            } else {
                stamp = parent.getStamp();
            }
            aborts.killChildrenOf(stamp);
        }
    }

    /**
     * Pause Satin operation. This method can optionally be called before a
     * large sequential part in a program. This will temporarily pause Satin's
     * internal load distribution strategies to avoid communication overhead
     * during sequential code.
     */
    public static void pause() {
        if (thisSatin == null) {
            return;
        }
        
        if(GLOBAL_PAUSE_RESUME) {
            thisSatin.comm.pause();
        } else {
            thisSatin.comm.receivePort.disableMessageUpcalls();
        }
    }

    /**
     * Resume Satin operation. This method can optionally be called after a
     * large sequential part in a program.
     */
    public static void resume() {
        if (thisSatin == null) {
            return;
        }

        if(GLOBAL_PAUSE_RESUME) {
            thisSatin.comm.resume();
        } else {
            thisSatin.comm.receivePort.enableMessageUpcalls();    
        }
    }

    /**
     * Returns whether it might be useful to spawn more methods. If there is
     * enough work in the system to keep all processors busy, this method
     * returns false.
     */
    public static boolean needMoreJobs() {
        // This can happen in sequential programs.
        if (thisSatin == null) {
            return false;
        }
        synchronized (thisSatin) {
            int size = thisSatin.victims.size();
            if (size == 0 && CLOSED) {
                // No need to spawn work on one machine.
                return false;
            }

            if (thisSatin.q.size() / (size + 1) > SUGGESTED_QUEUE_SIZE) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns whether the current method was generated by the machine it is
     * running on. Methods can be distributed to remote machines by the Satin
     * runtime system, in which case this method returns false.
     */
    public static boolean localJob() {
        if (thisSatin == null) {
            return true; // sequential run
        }

        if (thisSatin.parent == null) {
            return true; // root job
        }

        return thisSatin.parent.getOwner().equals(thisSatin.ident);
    }

    /**
     * Returns true if this is the instance that is running main().
     * 
     * @return <code>true</code> if this is the instance running main().
     */
    public boolean isMaster() {
    	if(ASSERTS && masterIdent == null) {
    		throw new Error("asked for master before he was elected");
    	}
        return master;
    }

    public void setMaster(IbisIdentifier newMaster) {
    	masterIdent = newMaster;
    	
		if (masterIdent.equals(ident)) {
			/* I an the master. */
			commLogger.info(
                                "SATIN '" + ident
                                + "': init ibis: I am the master");
			master = true;
		} else {
			commLogger.info("SATIN '" + ident
                                + "': init ibis I am slave");
		}

		if (STATS && master) {
            totalStats = new Statistics();
        }
    }

    // called from generated code
    public void broadcastSOInvocation(SOInvocationRecord r) {
        so.broadcastSOInvocation(r);
    }

    public static void assertLocked(Object o) {
        if (!ASSERTS) return;

        if (!trylock(o)) {
            System.err.println("AssertLocked failed!: ");
            new Exception().printStackTrace();
            System.exit(1); // Failed assertion
        }
    }

    public IbisIdentifier getMasterIdent() {
    	if(ASSERTS && masterIdent == null) {
    		throw new Error("asked for master before he was elected");
    	}
        return masterIdent;
    }

    private void callSatinFunction(InvocationRecord r) {
        if (ASSERTS) callSatinFunctionPreAsserts(r);

        if (r.getParent() != null && r.getParent().aborted) {
            r.decrSpawnCounter();
            return;
        }

        InvocationRecord oldParent = parent;
        onStack.push(r);
        parent = r;

        // We MUST make sure that steals don't overtake aborts.
        // Therefore, we must poll for messages here.
        handleDelayedMessages();

        if (r.getOwner().equals(ident)) {
            callSatinLocalFunction(r);
        } else { // we are running a job that I stole from another machine
            callSatinRemoteFunction(r);
        }

        // restore this, there may be more spawns afterwards...
        parent = oldParent;
        onStack.pop();
    }

    private void callSatinFunctionPreAsserts(InvocationRecord r) {
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

        if (r.getOwner() == null) {
            spawnLogger.fatal("SATIN '" + ident
                + ": EEK, r.owner = null in callSatinFunc, r = " + r,
                new Throwable());
            System.exit(1); // Failed assertion
        }
    }

    private void callSatinLocalFunction(InvocationRecord r) {
        stats.jobsExecuted++;
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
            if (!(t instanceof AbortException)) {
                r.eek = t;
                aborts.handleInlet(r);
            } else if (abortLogger.isDebugEnabled()) {
                abortLogger.debug("Caught abort exception " + t, t);
            }
        }

        r.decrSpawnCounter();

        if (!FT_NAIVE) r.jobFinished();
    }

    private void callSatinRemoteFunction(InvocationRecord r) {
        if (stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + ident
                    + "': RUNNING REMOTE CODE, STAMP = " + r.getStamp() + "!");
        }
        ReturnRecord rr = null;
        stats.jobsExecuted++;
        rr = r.runRemote();
        rr.setEek(r.eek);

        if (r.eek != null && stealLogger.isInfoEnabled()) {
            stealLogger.info("SATIN '" + ident
                + "': RUNNING REMOTE CODE GAVE EXCEPTION: " + r.eek, r.eek);
        } else {
            stealLogger
                .info("SATIN '" + ident + "': RUNNING REMOTE CODE DONE!");
        }

        // send wrapper back to the owner
        if (!r.aborted) {
            lb.sendResult(r, rr);
            if (stealLogger.isInfoEnabled()) {
                stealLogger.info("SATIN '" + ident
                        + "': REMOTE CODE SEND RESULT DONE!");
            }
        } else {
            if (stealLogger.isInfoEnabled()) {
                stealLogger.info("SATIN '" + ident
                        + "': REMOTE CODE WAS ABORTED!");
            }
        }
    }

    private void createLoadBalancingAlgorithm() {
        String alg = SUPPLIED_ALG;

        if (SUPPLIED_ALG == null) {
            alg = "CRS";
        }

        if (alg.equals("RS")) {
            algorithm = new RandomWorkStealing(this);
        } else if (alg.equals("CRS")) {
            algorithm = new ClusterAwareRandomWorkStealing(this);
        } else if (alg.equals("MW")) {
            algorithm = new MasterWorker(this);
        } else {
            System.err.println("SATIN '" + "- " + "': satin_algorithm '" + alg
                + "' unknown");
            algorithm = null;
            System.exit(1); // Wrong option
        }

        commLogger.info("SATIN '" + "- " + "': using algorithm '" + alg);
    }

    private static boolean trylock(Object o) {
        try {
            o.notifyAll();
        } catch (IllegalMonitorStateException e) {
            return false;
        }

        return true;
    }

    /**
     * @return Returns the current Satin instance.
     */
    public final static Satin getSatin() {
        return thisSatin;
    }

    /** Returns the parent of the current job, used in generated code. */
    public InvocationRecord getParent() {
        return parent;
    }

}
