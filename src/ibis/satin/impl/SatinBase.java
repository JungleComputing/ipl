package ibis.satin.impl;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPort;
import ibis.util.Timer;

import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

public abstract class SatinBase implements Config {

	Ibis ibis; //used in GlobalResultTable

	static Satin this_satin = null;

	IbisIdentifier ident; // used in messageHandler

	/* Options. */
	boolean closed = false; // used in TupleSpace

	boolean stats = true; // used in messageHandler

	protected boolean detailedStats = false;

	protected boolean ibisSerialization = true;

	protected boolean upcallPolling = false;

	protected int killTime = 0; //used in automatic ft tests

	protected int deleteTime = 0;

	int branchingFactor = 0; //if > 0, it is used for generating globally unique stamps
	
	protected boolean dump = false; //true if the node should dump its datastructures during shutdown
				       //done by registering DumpThread as a shutdown hook

	boolean getTable = true; //true if the node needs to download the contents

	// of the global result table; protected by lock

	/* Am I the root (the one running main)? */
	boolean master = false;

	boolean tuple_message_sent = false;

	protected String[] mainArgs;

	protected String name;

	protected IbisIdentifier masterIdent;

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
	PortType tuplePortType;
	protected PortType barrierPortType;

	protected ReceivePort receivePort;
	ReceivePort tupleReceivePort;

	protected ReceivePort barrierReceivePort; /* Only for the master. */

	protected SendPort barrierSendPort; /* Only for the clients. */

	SendPort tuplePort; /* used to bcast tuples */

	//ft
	protected long connectTimeout = 3000; /*
										   * Timeout for connecting to other
										   * nodes (in join()) who might be
										   * crashed
										   */

	volatile boolean exiting = false; // used in messageHandler

	Random random = new Random(); // used in victimTable

	protected MessageHandler messageHandler;

	protected static SpawnCounter spawnCounterCache = null;

	protected ArrayList activeTupleKeyList = new ArrayList();

	protected ArrayList activeTupleDataList = new ArrayList();

	protected volatile boolean receivedResults = false;

	protected int stampCounter = 0;

	/*
	 * Used to locate the invocation record corresponding to the result of a
	 * remote job.
	 */
	protected IRVector outstandingJobs;

	protected IRVector resultList;

	protected IRStack onStack;

	protected IRVector exceptionList;

	/* abort messages are queued until the sync. */
	protected StampVector abortList = new StampVector();

	/* used for fault tolerance */
	protected StampVector abortAndStoreList;

	/* used to store reply messages */
	volatile boolean gotStealReply = false; // used in messageHandler

	volatile boolean gotBarrierReply = false; // used in messageHandler

	volatile boolean gotActiveTuples = false; // used in messageHandler

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

	protected long tupleMsgs = 0;

	protected long tupleBytes = 0;

	long killedOrphans = 0;
	
	long restartedJobs = 0;

	long stolenJobs = 0; // used in messageHandler

	long stealRequests = 0; // used in messageHandler

	long interClusterMessages = 0;

	long intraClusterMessages = 0;

	long interClusterBytes = 0;

	long intraClusterBytes = 0;

	StatsMessage totalStats; // used in messageHandler

	/* Variables that contain data of the current job*/
	protected int parentStamp = -1;

	protected IbisIdentifier parentOwner = null;

	InvocationRecord parent = null;

	/* use these to avoid locking */
	protected volatile boolean gotExceptions = false;

	protected volatile boolean gotAborts = false;

	protected volatile boolean gotCrashes = false;

	protected volatile boolean gotAbortsAndStores = false;

	protected volatile boolean gotDelete = false;

	/*
	 * used for fault tolerance we must know who the current victim is, in case
	 * it crashes
	 */
	IbisIdentifier currentVictim = null;

	boolean currentVictimCrashed = false;

	IbisIdentifier asyncCurrentVictim = null;

	boolean asyncCurrentVictimCrashed = false;

	/* historical name.. it's the global job table used in fault tolerance */
	GlobalResultTable globalResultTable = null;

	//used in ft, true if the master crashed and the whole work was restarted
	boolean restarted = false;

	//used for generating globally unique stamps, number of jobs spawned by
	// root
	//no node can execute the root job twice, so this counter does not have to
	// be set to 0 after root crash
	int rootNumSpawned = 0;
	//list of children of the root node (which doesn't have and invocation record)
	InvocationRecord rootChild;

	//for debugging ft
	boolean del = false;

	protected IbisIdentifier crashedIbis = null;

	Vector allIbises = new Vector();

	final static int NUM_CRASHES = 0;

	boolean connectionUpcallsDisabled = false;

	/* All victims, myself NOT included. The elements are Victims. */
	VictimTable victims;

	Timer totalTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer stealTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer handleStealTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer abortTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer idleTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer pollTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer tupleTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer invocationRecordWriteTimer = Timer
			.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer invocationRecordReadTimer = Timer
			.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer tupleOrderingWaitTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer lookupTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer updateTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer handleUpdateTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer handleLookupTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	
	Timer tableSerializationTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	
	Timer tableDeserializationTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer crashTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer redoTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	Timer addReplicaTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	long prevPoll = 0;

	//	float MHz = Timer.getMHz();

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
			System.exit(1);
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

	abstract void handleActiveTuples();

	abstract void handleCrashes();

	abstract void handleAbortsAndStores();

	abstract void handleDelete();

	abstract void handleDelayedMessages();

	abstract void callSatinFunction(InvocationRecord job);

	abstract boolean globalResultTableCheck(InvocationRecord r);

	abstract void addToOutstandingJobList(InvocationRecord r);

	abstract StatsMessage createStats();

	abstract void printStats();

	abstract void printDetailedStats();

	abstract void barrier();

	abstract SendPort getReplyPortWait(IbisIdentifier ident);

	abstract void addToExceptionList(InvocationRecord r);
	
	abstract void attachToParent(InvocationRecord r);
}
