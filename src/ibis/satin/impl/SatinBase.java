/* $Id$ */

package ibis.satin.impl;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPort;
import ibis.util.Timer;
import ibis.util.TypedProperties;
import ibis.util.messagecombining.MessageCombiner;

import java.util.Random;
import java.util.Vector;

public abstract class SatinBase implements Config {

    Ibis ibis; //used in GlobalResultTable

    static Satin this_satin = null;

    IbisIdentifier ident = null;        // this ibis

    /* Options. */
    protected boolean closed = false;

    boolean stats = true; // used in messageHandler

    protected boolean detailedStats = false;

    protected boolean ibisSerialization = false;

    protected boolean sunSerialization = false;

    protected boolean upcallPolling = false;

    protected int killTime = 0; //used in automatic ft tests

    protected int deleteTime = 0;

    protected int deleteClusterTime = 0;

    protected String killCluster = null;

    protected String deleteCluster = null;

    int soInvocationsDelay = 0;
    //if > 0, it is used for combining shared objects invocations

    int soMaxMessageSize = 64 * 1024;
    // The maximum message size for messagecombining for shared objects
    
    long soCurrTotalMessageSize = 0;
    // the current size of the accumulated so messages

    long soRealMessageCount = 0;
    
    long soInvocationsDelayTimer = -1;

    protected boolean dump = false;
    //true if the node should dump its datastructures during shutdown

    //done by registering DumpThread as a shutdown hook

    boolean getTable = true;
    //true if the node needs to download the contents of the global result
    //table of the global result table; protected by lock

    boolean initialNode = false;
    //true if the node takes part in the computation from the beginning

    /* Am I the root (the one running main)? */
    boolean master = false;

    protected String[] mainArgs;

    protected String name;

    protected IbisIdentifier masterIdent = null;

    protected long stealReplySeqNr;

    /* Am I the cluster coordinator? */
    boolean clusterCoordinator = false;

    IbisIdentifier clusterCoordinatorIdent;

    /* My scheduling algorithm. */
    protected Algorithm algorithm;

    volatile int exitReplies = 0;

    long expected_seqno = ReadMessage.INITIAL_SEQNO;

    // WARNING: dijkstra does not work in combination with aborts.
    DEQueue q;

    protected PortType portType;

    PortType globalResultTablePortType;

    protected ReceivePort receivePort;

    PortType soPortType;

    ReceivePort soReceivePort; /* used to receive shared object invocations */

    SendPort soSendPort; /* used to broadcast shared object invocations */

    MessageCombiner soMessageCombiner; /* used to do message combining on soSendPort */


    //ft
    /**
     * Timeout for connecting to other nodes (in joined()) who might be
     * crashed.
     */
    static final protected long connectTimeout
            = TypedProperties.intProperty(s_ft_connectTimeout, 120) * 1000L;

    volatile boolean exiting = false; // used in messageHandler

    volatile boolean exitStageTwo = false; // used in messageHandler
    
    Random random = new Random(); // used in victimTable

    protected MessageHandler messageHandler;

    protected volatile boolean receivedResults = false;

    /**
     * Used to locate the invocation record corresponding to the result of a
     * remote job.
     */
    protected IRVector outstandingJobs;

    protected IRVector resultList;

    protected IRStack onStack;

    protected IRVector exceptionList;

    /** Abort messages are queued until the sync. */
    protected StampVector abortList = new StampVector();

    /** List that stores requests for shared object transfers */
    protected SORequestList SORequestList = new SORequestList();
    
    /** Used for fault tolerance. */
    protected StampVector abortAndStoreList;    

    /** Used for storing pending shared object invocations (SOInvocationRecords)*/
    protected Vector soInvocationList = new Vector();

    /** Used to store reply messages. */
    volatile boolean gotStealReply = false; // used in messageHandler

    volatile int barrierRequests = 0; // used in messageHandler

    volatile boolean gotBarrierReply = false; // used in messageHandler

    volatile boolean gotSOInvocations = false;

    protected boolean upcalls = true;

    InvocationRecord stolenJob = null;

    //	IbisIdentifier stolenFrom = null;

    protected int suggestedQueueSize = 1000;

    /* Variables that contain statistics. */
    protected long spawns = 0;

    protected long syncs = 0;

    protected long aborts = 0;

    protected long jobsExecuted = 0;

    long abortedJobs = 0; // used in dequeue

    long abortMessages = 0;

    protected long stealAttempts = 0;

    protected long stealSuccess = 0;

    long asyncStealAttempts;

    long asyncStealSuccess;

    long killedOrphans = 0;

    long restartedJobs = 0;

    long stolenJobs = 0; // used in messageHandler

    long stealRequests = 0; // used in messageHandler

    long interClusterMessages = 0;

    long intraClusterMessages = 0;

    long interClusterBytes = 0;

    long intraClusterBytes = 0;

    long soInvocations = 0;

    long soInvocationsBytes = 0;
    
    long soTransfers = 0;

    long soTransfersBytes = 0;

    long soBcasts = 0;
    
    long soBcastBytes = 0;
    
    StatsMessage totalStats; // used in messageHandler

    /* Variables that contain data of the current job*/
    protected Stamp parentStamp = null;

    protected IbisIdentifier parentOwner = null;

    InvocationRecord parent = null;

    /* use these to avoid locking */
    protected volatile boolean gotExceptions = false;

    protected volatile boolean gotAborts = false;

    protected volatile boolean gotSORequests = false;

    protected volatile boolean gotCrashes = false;

    protected volatile boolean gotAbortsAndStores = false;

    protected volatile boolean gotDelete = false;

    protected volatile boolean gotDeleteCluster = false;

    protected volatile boolean updatesToSend = false;
    
    protected volatile boolean masterHasCrashed = false;
    
    protected volatile boolean clusterCoordinatorHasCrashed = false;

    /**
     * Used for fault tolerance, we must know who the current victim is,
     * in case it crashes.
     */
    IbisIdentifier currentVictim = null;

    protected volatile boolean currentVictimCrashed = false;

    // IbisIdentifier asyncCurrentVictim = null;

    // boolean asyncCurrentVictimCrashed = false;

    /* historical name.. it's the global job table used in fault tolerance */
    GlobalResultTable globalResultTable = null;

    //used in ft, true if the master crashed and the whole work was restarted
    boolean restarted = false;

    //list of finished children of the root node (which don't have an
    //invocation record)
    InvocationRecord rootFinishedChild = null;

    //list of children of the root node which need to be restarted
    InvocationRecord rootToBeRestartedChild = null;

    //for debugging ft
    boolean del = false;

    protected IbisIdentifier crashedIbis = null;

    Vector allIbises = new Vector();

    final static int NUM_CRASHES = 0;

    boolean connectionUpcallsDisabled = false;

    /* All victims, myself NOT included. The elements are Victims. */
    VictimTable victims;

    Timer totalTimer = Timer.createTimer();

    Timer stealTimer = Timer.createTimer();

    Timer handleStealTimer = Timer.createTimer();

    Timer abortTimer = Timer.createTimer();

    Timer idleTimer = Timer.createTimer();

    Timer pollTimer = Timer.createTimer();

    Timer invocationRecordWriteTimer = Timer.createTimer();

    Timer returnRecordWriteTimer = Timer.createTimer();

    Timer invocationRecordReadTimer = Timer.createTimer();

    Timer returnRecordReadTimer = Timer.createTimer();

    Timer lookupTimer = Timer.createTimer();

    Timer updateTimer = Timer.createTimer();

    Timer handleUpdateTimer = Timer.createTimer();

    Timer handleLookupTimer = Timer.createTimer();

    Timer tableSerializationTimer = Timer.createTimer();

    Timer tableDeserializationTimer = Timer.createTimer();

    Timer crashTimer = Timer.createTimer();

    Timer redoTimer = Timer.createTimer();

    Timer addReplicaTimer = Timer.createTimer();

    Timer handleSOInvocationsTimer = Timer.createTimer();

    Timer broadcastSOInvocationsTimer = Timer.createTimer();

    Timer soTransferTimer = Timer.createTimer();

    Timer soSerializationTimer = Timer.createTimer();

    Timer soDeserializationTimer = Timer.createTimer();

    Timer soBroadcastDeserializationTimer = Timer.createTimer();

    Timer soBroadcastSerializationTimer = Timer.createTimer();

    Timer soBroadcastTransferTimer = Timer.createTimer();

    long returnRecordBytes = 0;

    long prevPoll = 0;

    java.io.PrintStream out = System.out;

    /*
     * Used for fault tolerance Ibises that crashed recently and whose crashes
     * still need to be handled
     */
    Vector crashedIbises = new Vector();

    /*
     * Used for fault tolerance All ibises that once took part in the
     * computation, but then crashed Assumption: ibis identifiers are uniqe in
     * time; the same ibis cannot crash and join the computation again
     */
    Vector deadIbises = new Vector();

    static {
        TypedProperties.checkProperties(PROPERTY_PREFIX, sysprops, null);
    }

    static boolean trylock(Object o) {
        try {
            o.notifyAll();
        } catch (IllegalMonitorStateException e) {
            return false;
        }

        return true;
    }

    static void assertLocked(Object o) {
        if (!trylock(o)) {
            System.err.println("AssertLocked failed!: ");
            new Exception().printStackTrace();
            System.exit(1);     // Failed assertion
        }
    }

    boolean inDifferentCluster(IbisIdentifier other) {
        if (ASSERTS) {
            if (ident.cluster() == null || other.cluster() == null) {
                System.err.println("WARNING: Found NULL cluster!");

                /* this isn't severe enough to exit, so return something */
                return true;
            }
        }

        return !ident.cluster().equals(other.cluster());
    }

    abstract boolean satinPoll();

    abstract void sendResult(InvocationRecord r, ReturnRecord rr);

    abstract void handleInlet(InvocationRecord r);

    abstract void handleAborts();

    abstract void handleExceptions();

    abstract void handleResults();

    abstract void handleSOInvocations();
    
    abstract void handleSORequests();
        
    abstract void handleCrashes();

    abstract void handleAbortsAndStores();

    abstract void handleDelete();

    abstract void handleDeleteCluster();
    
    abstract void handleMasterCrash();
    
    abstract void handleClusterCoordinatorCrash();

    abstract void handleDelayedMessages();

    abstract void callSatinFunction(InvocationRecord job);

    abstract boolean globalResultTableCheck(InvocationRecord r);

    abstract void addToOutstandingJobList(InvocationRecord r);

    abstract StatsMessage createStats();

    abstract void printStats();

    abstract void printDetailedStats();

    abstract void barrier();

    abstract Victim getVictimWait(IbisIdentifier id);

    abstract void addToExceptionList(InvocationRecord r);

    abstract void attachToParentFinished(InvocationRecord r);

    abstract void attachToParentToBeRestarted(InvocationRecord r);

    abstract void broadcastSharedObject(ibis.satin.SharedObject object);

    abstract void broadcastSOInvocation(SOInvocationRecord r);

    abstract void sendAccumulatedSOInvocations();
    
    abstract void executeGuard(InvocationRecord r) 
	throws SOReferenceSourceCrashedException;

    Timer createTimer() {
        return Timer.createTimer();
    }
}
