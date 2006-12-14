/*
 * Created on May 2, 2006 by rob
 */
package ibis.satin.impl.faultTolerance;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.ResizeHandler;
import ibis.ipl.StaticProperties;
import ibis.satin.impl.Config;
import ibis.satin.impl.Satin;
import ibis.satin.impl.loadBalancing.Victim;
import ibis.satin.impl.spawnSync.InvocationRecord;
import ibis.satin.impl.spawnSync.Stamp;
import ibis.satin.impl.spawnSync.StampVector;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class FaultTolerance implements Config {
    private Satin s;

    /** True if the master crashed and the whole work was restarted. */
    private boolean restarted = false;

    /* use these to avoid locking */
    protected volatile boolean gotCrashes = false;

    protected volatile boolean gotAbortsAndStores = false;

    protected volatile boolean gotDelete = false;

    protected volatile boolean gotDeleteCluster = false;

    protected volatile boolean updatesToSend = false;

    protected volatile boolean masterHasCrashed = false;

    protected volatile boolean clusterCoordinatorHasCrashed = false;

    private StampVector abortAndStoreList;

    protected IbisIdentifier clusterCoordinatorIdent;

    /** Historical name; it's the global job table used in fault tolerance. */
    protected GlobalResultTable globalResultTable;

    /** True if the node needs to download the contents of the global result
     *  table; protected by the Satin lock. */
    public boolean getTable = true;

    /**
     * Used for fault tolerance Ibises that crashed recently, and whose crashes
     * still need to be handled.
     */
    protected ArrayList<IbisIdentifier> crashedIbises = new ArrayList<IbisIdentifier>();

    protected FTCommunication ftComm;

    public FaultTolerance(Satin s) {
        this.s = s;

        ftComm = new FTCommunication(s); // must be created first, it handles resize upcalls.

        /* the threads below are used for debugging */
        if (KILL_TIME > 0) {
            (new KillerThread(KILL_TIME)).start();
        }
        if (DELETE_TIME > 0) {
            (new DeleteThread(DELETE_TIME)).start();
        }
        if (DELETE_CLUSTER_TIME > 0) {
            (new DeleteClusterThread(DELETE_CLUSTER_TIME)).start();
        }
    }

    public void electClusterCoordinator() {
        ftComm.electClusterCoordinator();    	
    }
    
    public void init(StaticProperties requestedProperties) {

        if(!FT_NAIVE) {
            globalResultTable = new GlobalResultTable(s, requestedProperties);
        }
        abortAndStoreList = new StampVector();

        if (FT_NAIVE) {
            ftLogger.info("naive FT on");
        } else {
            ftLogger.info("FT on, with GRT enabled");
        }

        if (s.isMaster()) {
            getTable = false;
        }

        s.comm.ibis.enableResizeUpcalls();

        if (CLOSED) {
            s.comm.waitForAllNodes();
        }
    }

    // The core of the fault tolerance mechanism, the crash recovery procedure
    public void handleCrashes() {
        ftLogger.debug("SATIN '" + s.ident + ": handling crashes");
        s.stats.crashTimer.start();

        ArrayList<IbisIdentifier> crashesToHandle;

        synchronized (s) {
            crashesToHandle = (ArrayList<IbisIdentifier>) crashedIbises.clone();
            crashedIbises.clear();
            gotCrashes = false;
        }

        // Let the Ibis registry know, but only if this is the master or
        // a cluster coordinator, otherwise everything gets terribly slow.
        // Don't hold the lock while doing this.
        for (int i = 0; i < crashesToHandle.size(); i++) {
            IbisIdentifier id = crashesToHandle.get(0);
            if (id.equals(s.getMasterIdent()) || id.equals(clusterCoordinatorIdent)) {
                try {
                    s.comm.ibis.registry().maybeDead(id);
                } catch (IOException e) {
                    // ignore exception
                    ftLogger.info("SATIN '" + s.ident
                        + "' :exception while notifying registry about "
                        + "crash of " + id + ": " + e, e);
                }
            }
        }

        synchronized (s) {
            while (crashesToHandle.size() > 0) {
                IbisIdentifier id = crashesToHandle.remove(0);
                ftLogger.debug("SATIN '" + s.ident + ": handling crash of "
                    + id);

                // give the load-balancing algorith a chance to clean up
                s.algorithm.handleCrash(id);

                if (!FT_NAIVE) {
                    // abort all jobs stolen from id or descendants of jobs
                    // stolen from id
                    killAndStoreSubtreeOf(id);
                }

                s.outstandingJobs.redoStolenBy(id);
                s.stats.numCrashesHandled++;
                
                s.so.handleCrash(id);
            }

            s.notifyAll();
        }
        s.stats.crashTimer.stop();
    }

    public void handleMasterCrash() {
        ftLogger.info("SATIN '" + s.ident + "': MASTER (" + s.getMasterIdent()
            + ") HAS CRASHED");

        // master has crashed, let's elect a new one
        IbisIdentifier newMaster = null;
        try {
            newMaster = s.comm.ibis.registry().elect("satin master");
        } catch (Exception e) {
            ftLogger.fatal("SATIN '" + s.ident
                + "' :exception while electing a new master " + e, e);
            System.exit(1);
        }

        synchronized (s) {
            masterHasCrashed = false;
            s.setMaster(newMaster);
            if (s.getMasterIdent().equals(s.ident)) {
                ftLogger.info("SATIN '" + s.ident + "': I am the new master");
            } else {
                ftLogger.info("SATIN '" + s.ident + "': " + s.getMasterIdent()
                    + "is the new master");
            }
            restarted = true;
        }
    }

    public void handleClusterCoordinatorCrash() {
        clusterCoordinatorHasCrashed = false;
        try {
            ftComm.electClusterCoordinator();
        } catch (Exception e) {
            ftLogger.warn("SATIN '" + s.ident
                + "' :exception while electing a new cluster coordinator " + e,
                e);
        }
    }

    public void killAndStoreChildrenOf(Stamp targetStamp) {
        Satin.assertLocked(s);
        // try work queue, outstanding jobs and jobs on the stack
        // but try stack first, many jobs in q are children of stack jobs
        ArrayList<InvocationRecord> toStore = s.onStack.killChildrenOf(targetStamp, true);

        //update the global result table

        for (int i = 0; i < toStore.size(); i++) {
            storeFinishedChildrenOf(toStore.get(i));
        }

        s.q.killChildrenOf(targetStamp);
        s.outstandingJobs.killChildrenOf(targetStamp, true);
    }

    private void storeFinishedChildrenOf(InvocationRecord r) {
        InvocationRecord child = r.getFinishedChild();
        while (child != null) {
            s.ft.storeResult(child);
            child = child.getFinishedSibling();
        }
    }

    public void killAndStoreSubtreeOf(IbisIdentifier targetOwner) {
        ArrayList<InvocationRecord> toStore = s.onStack.killSubtreesOf(targetOwner);

        // update the global result table
        for (int i = 0; i < toStore.size(); i++) {
            storeFinishedChildrenOf(toStore.get(i));
        }

        s.q.killSubtreeOf(targetOwner);
        s.outstandingJobs.killAndStoreSubtreeOf(targetOwner);
    }

    public void addToAbortAndStoreList(Stamp stamp) {
        Satin.assertLocked(s);
        abortLogger.debug("SATIN '" + s.ident + ": got abort message");
        abortAndStoreList.add(stamp);
        gotAbortsAndStores = true;
    }

    public void handleAbortsAndStores() {
        synchronized (s) {
            Stamp stamp;

            while (true) {
                if (abortAndStoreList.getCount() > 0) {
                    stamp = abortAndStoreList.getStamps()[0];
                    abortAndStoreList.removeIndex(0);
                } else {
                    gotAbortsAndStores = false;
                    return;
                }

                killAndStoreChildrenOf(stamp);
            }
        }
    }

    public void deleteCluster(String clusterName) {
        ftLogger.info("SATIN '" + s.ident + "': delete cluster " + clusterName);

        if (s.ident.cluster().equals(clusterName)) {
            gotDeleteCluster = true;
        }
    }

    public void handleDelete() {
        Victim victim = s.victims.getRandomLocalVictim();
        pushJobs(victim);
        System.exit(0);
    }

    public void handleDeleteCluster() {
        Victim victim = s.victims.getRandomRemoteVictim();
        pushJobs(victim);
        System.exit(0);
    }

    private void pushJobs(Victim v) {
        Map<Stamp, GlobalResultTableValue> toPush = new HashMap<Stamp, GlobalResultTableValue>();
        synchronized (s) {
            ArrayList<InvocationRecord> tmp = s.onStack.getAllFinishedChildren(v);

            for (int i = 0; i < tmp.size(); i++) {
                InvocationRecord curr = tmp.get(i);
                Stamp key = curr.getStamp();
                GlobalResultTableValue value = new GlobalResultTableValue(
                    GlobalResultTableValue.TYPE_RESULT, curr);
                toPush.put(key, value);
            }

            s.stats.killedOrphans += s.onStack.size();
            s.stats.killedOrphans += s.q.size();
        }

        ftComm.pushResults(v, toPush);
    }

    public void handleDelayedMessages() {
        if (gotCrashes) {
            s.ft.handleCrashes();
        }
        if (gotAbortsAndStores) {
            s.ft.handleAbortsAndStores();
        }
        if (gotDelete) {
            s.ft.handleDelete();
        }
        if (gotDeleteCluster) {
            s.ft.handleDeleteCluster();
        }
        if (masterHasCrashed) {
            s.ft.handleMasterCrash();
        }
        if (clusterCoordinatorHasCrashed) {
            s.ft.handleClusterCoordinatorCrash();
        }
        if (updatesToSend) {
            globalResultTable.sendUpdates();
        }
    }

    public boolean checkForDuplicateWork(InvocationRecord parent,
        InvocationRecord r) {
        if (FT_NAIVE) return false;

        if (parent != null && parent.isReDone() || parent == null && restarted) {
            r.setReDone(true);
        }

        if (r.isReDone()) {
            if (ftComm.askForJobResult(r)) {
                return true;
            }
        }

        return false;
    }

    public void storeResult(InvocationRecord r) {
        globalResultTable.storeResult(r);
    }

    public void print(PrintStream out) {
        globalResultTable.print(out);
    }

    public IbisIdentifier lookupOwner(InvocationRecord r) {
        return globalResultTable.lookup(r.getStamp()).sendTo;
    }

    public Map<Stamp, GlobalResultTableValue> getContents() {
        return globalResultTable.getContents();
    }

    public void addContents(Map<Stamp, GlobalResultTableValue> contents) {
        globalResultTable.addContents(contents);
    }

    public ResizeHandler getResizeHandler() {
        return ftComm;
    }

    public ReceivePortConnectUpcall getReceivePortConnectHandler() {
        return ftComm;
    }

    public void disableConnectionUpcalls() {
        ftComm.disableConnectionUpcalls();
    }

    public void handleAbortAndStore(ReadMessage m) {
        ftComm.handleAbortAndStore(m);
    }

    public void handleResultRequest(ReadMessage m) {
        ftComm.handleResultRequest(m);
    }

    public void handleResultPush(ReadMessage m) {
        ftComm.handleResultPush(m);
    }

    public void sendAbortAndStoreMessage(InvocationRecord r) {
        ftComm.sendAbortAndStoreMessage(r);
    }
    
    public void handleGRTUpdate(ReadMessage m) {
        globalResultTable.handleGRTUpdate(m);
    }
}
