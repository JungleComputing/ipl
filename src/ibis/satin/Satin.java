package ibis.satin;

import ibis.ipl.Ibis;
import ibis.ipl.IbisError;
import ibis.ipl.IbisException;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.ResizeHandler;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.WriteMessage;

import ibis.util.Timer;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.ArrayList;

/* 
   One important invariant: there is only one thread per machine that spawns
   work.
   Another: there is only one lock: the global satin object.
   invariant: all jobs in the queue and outstandingJobsList have me as owner.
   invariant: all invocations records in use are in one of these lists:
     - onStack (being worked on)
     - OutstandingJobs (stolen)
     - q (spawned but not yet running)

   invariant: When running java code, the parentStamp, parentOwner and 
   parent contain the spawner of the work. (parent may be null when running
   the root node, or when the spawner is a remote machine).

   Satin.spawn gets the satin wrapper.
   This can be serialized, and run may be called.

   When a job is spawned, the RTS put a stamp on it.
   When a job is stolen the RTS puts an entry in a table.
   The runRemote method creates a return wrapper containing the return value.
   The runtime system sends the return value back, together with the
   original stamp. The victim can do a lookup to find the entry (containing
   the spawn counter and result pointer) that corresponds with the job.
*/

public final class Satin implements Config, Protocol, ResizeHandler {

	public static Satin this_satin = null;

	private Ibis ibis;
	IbisIdentifier ident; // used in messageHandler

	/* Options. */
	private boolean closed = false;
	private boolean stats = false;
	private boolean panda = false;
	private boolean mpi = false;
	private boolean net = false;
	private boolean ibisSerialization = false;
	private boolean upcallPolling = false;

	/* Am I the root (the one running main)? */
	public boolean master = false; // used in generated code
	public String[] mainArgs; // used in generated code
	private String name;
	protected IbisIdentifier masterIdent;

	/* My scheduling algorithm. */
	protected final Algorithm algorithm;

	volatile int exitReplies = 0;

	// WARNING: dijkstra does not work in combination with aborts.
	public DEQueue q = ABORTS ? ((DEQueue) new DEQueueNormal(this)) : 
		((DEQueue) new DEQueueDijkstra(this));
	private PortType portType;
	private ReceivePort receivePort;
	private ReceivePort barrierReceivePort; /* Only for the master. */
	private SendPort barrierSendPort; /* Only for the clients. */
	private SendPort tuplePort; /* used to bcast tuples */

	volatile boolean exiting = false; // used in messageHandler
	Random random = new Random(); // used in victimTable
	private MessageHandler messageHandler;

	private static SpawnCounter spawnCounterCache = null;

	/* Used to locate the invocation record corresponding to the
	   result of a remote job. */
	private IRVector outstandingJobs = new IRVector(this);
	private IRVector resultList = new IRVector(this);
	private ArrayList activeTupleKeyList = new ArrayList();
	private ArrayList activeTupleDataList = new ArrayList();
	private volatile boolean receivedResults = false;
	private int stampCounter = 0;


	private IRStack onStack = new IRStack(this);

	private IRVector exceptionList = new IRVector(this);

	/* abort messages are queued until the sync. */
	private StampVector abortList = new StampVector();

	/* used to store reply messages */
	volatile boolean gotStealReply = false; // used in messageHandler
	volatile boolean gotBarrierReply = false; // used in messageHandler
	volatile boolean gotActiveTuples = false; // used in messageHandler

	InvocationRecord stolenJob = null;

	/* Variables that contain statistics. */
	private long spawns = 0;
	private long syncs = 0;
	private long aborts = 0;
	private long jobsExecuted = 0;
	public long abortedJobs = 0; // used in dequeue
	long abortMessages = 0;
	private long stealAttempts = 0;
	private long stealSuccess = 0;
	private long tupleMsgs = 0;
	private long tupleBytes = 0;

	long stolenJobs = 0; // used in messageHandler
	long stealRequests = 0; // used in messageHandler
	protected final boolean upcalls;

	long interClusterMessages = 0;
	long intraClusterMessages = 0;
	long interClusterBytes = 0;
	long intraClusterBytes = 0;

	private int parentStamp = -1;
	private IbisIdentifier parentOwner = null;
	public InvocationRecord parent = null; // used in generated code

	/* use these to avoid locking */
	private volatile boolean gotExceptions = false;
	private volatile boolean gotAborts = false;

	/* All victims, myself NOT included. The elements are Victims. */
	VictimTable victims;

	Timer stealTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	Timer handleStealTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	Timer abortTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	Timer idleTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	Timer pollTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	Timer tupleTimer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");
	private long prevPoll = 0;
	//	float MHz = Timer.getMHz();

	java.io.PrintStream out = System.err;


	public Satin(String[] args) {

		if(this_satin != null) {
			throw new IbisError("multiple satin instances are currently not supported");
		}
		this_satin = this;

		if(stealTimer == null) {
			System.err.println("Native timers not found, using (less accurate) java timers.");
		}

		if(stealTimer == null) stealTimer = new Timer();
		if(handleStealTimer == null) handleStealTimer = new Timer();
		if(abortTimer == null) abortTimer = new Timer();
		if(idleTimer == null) idleTimer = new Timer();
		if(pollTimer == null) pollTimer = new Timer();
		if(tupleTimer == null) tupleTimer = new Timer();

		Properties p = System.getProperties();
		String hostName = null;
		String alg = null;
		int poolSize = 0; /* Only used with closed world. */

		try {
			InetAddress address = InetAddress.getLocalHost();
			hostName = address.getHostName();

		} catch (UnknownHostException e) {
			System.err.println("SATIN:init: Cannot get ip of local host: " + e);
			System.exit(1);
		}

		boolean doUpcalls = true;

		/* Parse commandline parameters. Remove everything that starts
		   with satin. */
		Vector tempArgs = new Vector();
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-satin-closed")) {/* Closed world assumption. */
				closed = true;
			} else if(args[i].equals("-satin-panda")) {
				panda = true;
			} else if(args[i].equals("-satin-mpi")) {
				mpi = true;
			} else if(args[i].equals("-satin-net")) {
				net = true;
			} else if(args[i].equals("-satin-tcp")) {
			} else if(args[i].equals("-satin-stats")) {
				stats = true;
			} else if(args[i].equals("-satin-ibis")) {
				ibisSerialization = true;
			} else if(args[i].equals("-satin-no-upcalls")) {
				doUpcalls = false;
			} else if(args[i].equals("-satin-upcall-polling")) {
				upcallPolling = true;
			} else if(args[i].equals("-satin-alg")) {
				i++;
				alg = args[i];
			} else {
				tempArgs.add(args[i]);
			}
		}

		upcalls = doUpcalls; // upcalls is final for performance reasons :-)

		mainArgs = new String[tempArgs.size()];
		for(int i=0; i<tempArgs.size(); i++) {
			mainArgs[i] = (String) tempArgs.get(i);
		}

		if(closed) {
			String pool = p.getProperty("ibis.pool.total_hosts");
			if(pool == null) {
				out.println("property 'ibis.pool.total_hosts' not set," +
					    " and running with closed world.");
				System.exit(1);
			}

			poolSize = Integer.parseInt(pool);
		}

		if(COMM_DEBUG) {
			out.println("SATIN '" + hostName + "': init ibis" );
		}

		StaticProperties s = new StaticProperties();

		for(int i=0; (i<10 && ibis == null); i++) {
			try {
				name = "ibis@" + hostName + "_" + System.currentTimeMillis();
				if(panda) {
					ibis = Ibis.createIbis(name,
							       "ibis.impl.messagePassing.PandaIbis", this);
				} else if (mpi) {
					ibis = Ibis.createIbis(name,
							       "ibis.impl.messagePassing.MPIIbis", this);
				} else if (net) {
					ibis = Ibis.createIbis(name,
							       "ibis.impl.net.NetIbis", this);

					Properties sysP = System.getProperties();
					String ibisName = sysP.getProperty("ibis.name");
					s.add("IbisName", ibisName);

				} else {
					ibis = Ibis.createIbis(name,
							       "ibis.impl.tcp.TcpIbis", this);
				}
			} catch (ConnectionRefusedException e) {
				if(COMM_DEBUG) {
					System.err.println("SATIN '" + hostName + 
							   "': WARNING Could not start ibis with name '" +
							   name + "': " + e + ", retrying.");
				}
			} catch (IbisException e) {
				System.err.println("SATIN '" + hostName + 
						   "': Could not start ibis with name '" +
						   name + "': " + e);
				e.printStackTrace();
				break;
			}
		}
		if(ibis == null) {
			System.err.println("SATIN: giving up");
			System.exit(1);
		}

		ident = ibis.identifier();

		parentOwner = ident;

		victims = new VictimTable(this); //victimTable accesses ident..

		if(COMM_DEBUG) {
			out.println("SATIN '" + hostName + "': init ibis DONE, " +
				    "my cluster is '" + ident.cluster() + "'");
		}

		try {
			Registry r = ibis.registry();

			if(ibisSerialization) {
				s.add("Serialization", "ibis");
				System.err.println("satin: using Ibis serialization");
			}

			portType = ibis.createPortType("satin porttype", s);

			messageHandler = new MessageHandler(this);

			if(upcalls) {
				receivePort = portType.createReceivePort("satin port on " + 
									 ident.name(), messageHandler);
			} else {
				System.err.println("using blocking receive");
				receivePort = portType.createReceivePort("satin port on " + 
									 ident.name());
			}

			masterIdent = (IbisIdentifier) r.elect("satin master", ident);

			if(masterIdent.equals(ident)) {
				/* I an the master. */
				if(COMM_DEBUG) {
					out.println("SATIN '" + hostName +
						    "': init ibis: I am the master");
				}
				master = true;
			} else {
				if(COMM_DEBUG) {
					out.println("SATIN '" + hostName +
						    "': init ibis I am slave" );
				}
			}

			if(master) {
				barrierReceivePort =
					portType.createReceivePort("satin barrier receive port");
				barrierReceivePort.enableConnections();
			} else {
				barrierSendPort =
					portType.createSendPort("satin barrier send port on " + 
								ident.name());
				ReceivePortIdentifier barrierIdent =
					lookup("satin barrier receive port");
				connect(barrierSendPort, barrierIdent);
			}

			// Create a multicast port to bcast tuples.
			// Connections are established in the join upcall.
			if(SUPPORT_TUPLE_MULTICAST) {
				tuplePort = 
					portType.createSendPort("satin tuple port on " +
								ident.name());
			}
		} catch (Exception e) {
			System.err.println("SATIN '" + hostName +
					   "': Could not start ibis: " + e);
			e.printStackTrace();
			System.exit(1);
		}

		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': init ibis DONE2");
		}

		if(master) {
			if(closed) {
				System.err.println("SATIN '" + hostName +
						   "': running with closed world, "
						   + poolSize + " host(s)");
			} else {
				System.err.println("SATIN '" + hostName +
						   "': running with open world");
			}
		}

		if(alg == null) {
			if(master) {
				System.err.println("SATIN '" + hostName +
						   "': satin_algorithm property not specified, using RS");
			}
			alg = "RS";
		}

		if(alg.equals("RS")) {
			algorithm = new RandomWorkStealing(this);
		} else if(alg.equals("CRS")) {
			algorithm = new ClusterAwareRandomWorkStealing(this);
		} else if(alg.equals("MW")) {
			algorithm = new MasterWorker(this);
		} else {
			System.err.println("SATIN '" + hostName + "': satin_algorithm '"
					   + alg + "' unknown");
			algorithm = null;
			System.exit(1);
		}

		if(upcalls) receivePort.enableUpcalls();
		receivePort.enableConnections();
		ibis.openWorld();

		if(COMM_DEBUG) {
			out.println("SATIN '" + hostName + "': pre barrier" );
		}

		if(closed) {
			synchronized(this) {
				while(victims.size() != poolSize - 1) {
					try {
						wait();
					} catch (InterruptedException e) {
						System.err.println("eek: " + e);
						// Ignore.
					}
				}
				if(COMM_DEBUG) {
					out.println("SATIN '" + hostName +
						    "': barrier, everybody has joined" );
				}

				ibis.closeWorld();
			}

			barrier();
		}

		if(COMM_DEBUG) {
			out.println("SATIN '" + hostName + "': post barrier" );
		}
	}

	public boolean inDifferentCluster(IbisIdentifier other) {
		if (ASSERTS) {
			if (ident.cluster() == null || other.cluster() == null) {
				System.err.println("WARNING: Found NULL cluster!");

				/* this isn't severe enough to exit, so return something */
				return true;
			}
		}

		return !ident.cluster().equals(other.cluster());
	}

	public void exit() {
		/* send exit messages to all others */
		int size;
		java.text.NumberFormat nf = null;

		if(!closed) {
			ibis.closeWorld();
		}

		if (stats) {
			nf = java.text.NumberFormat.getInstance();
		}

		if(SPAWN_STATS && stats) {
			out.println("SATIN '" + ident.name() + 
				    "': SPAWN_STATS: spawns = " + spawns +
				    " executed = " + jobsExecuted + 
				    " syncs = " + syncs);
			if(ABORTS) {
				out.println("SATIN '" + ident.name() + 
					    "': ABORT_STATS 1: aborts = " + aborts +
					    " abort msgs = " + abortMessages +
					    " aborted jobs = " + abortedJobs);
			}
		}
		if(TUPLE_STATS && stats) {
			out.println("SATIN '" + ident.name() + 
				    "': TUPLE_STATS 1: tuple bcast msgs: " + tupleMsgs +
				    ", bytes = " + nf.format(tupleBytes));
		}
		if(STEAL_STATS && stats) {
			out.println("SATIN '" + ident.name() + 
				    "': INTRA_STATS: messages = " + intraClusterMessages +
				    ", bytes = " + nf.format(intraClusterBytes));

			out.println("SATIN '" + ident.name() + 
				    "': INTER_STATS: messages = " + interClusterMessages +
				    ", bytes = " + nf.format(interClusterBytes));

			out.println("SATIN '" + ident.name() + 
				    "': STEAL_STATS 1: attempts = " + stealAttempts +
				    " success = " + stealSuccess + " (" +
				    (((double) stealSuccess / stealAttempts) * 100.0) +
				    " %)");

			out.println("SATIN '" + ident.name() + 
				    "': STEAL_STATS 2: requests = " + stealRequests +
				    " jobs stolen = " + stolenJobs);

			if(STEAL_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': STEAL_STATS 3: attempts = " +
					    stealTimer.nrTimes() + " total time = " +
					    stealTimer.totalTime() + " avg time = " +
					    stealTimer.averageTime());

				out.println("SATIN '" + ident.name() + 
					    "': STEAL_STATS 4: handleSteals = " +
					    handleStealTimer.nrTimes() + 
					    " total time = " + handleStealTimer.totalTime() +
					    " avg time = " + handleStealTimer.averageTime());
			}

			if(ABORTS && ABORT_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': ABORT_STATS 2: aborts = " +
					    abortTimer.nrTimes() + 
					    " total time = " + abortTimer.totalTime() +
					    " avg time = " + abortTimer.averageTime());
			}

			if(IDLE_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': IDLE_STATS: idle count = " +
					    idleTimer.nrTimes() + " total time = " +
					    idleTimer.totalTime() + " avg time = " +
					    idleTimer.averageTime());
			}

			if(POLL_FREQ > 0 && POLL_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': POLL_STATS: poll count = " +
					    pollTimer.nrTimes() + " total time = " +
					    pollTimer.totalTime() + " avg time = " +
					    pollTimer.averageTime());
			}

			if(STEAL_TIMING && IDLE_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': COMM_STATS: software comm time = " +
					    pollTimer.format(stealTimer.totalTimeVal() +
							     handleStealTimer.totalTimeVal() -
							     idleTimer.totalTimeVal()));
			}

			if(TUPLE_TIMING) {
				out.println("SATIN '" + ident.name() + 
					    "': TUPLE_STATS 2: bcasts = " +
					    tupleTimer.nrTimes() + " total time = " +
					    tupleTimer.totalTime() + " avg time = " +
					    tupleTimer.averageTime());
			}
			algorithm.printStats(out);
		}

		synchronized(this) {
			size = victims.size();
		}

		if(master) {
			for (int i=0; i<size; i++) {
				try {
					WriteMessage writeMessage;
					synchronized(this) {
						if(COMM_DEBUG) {
							out.println("SATIN '" + ident.name() + 
								    "': sending exit message to " +
								    victims.getIdent(i));
						}
						
						writeMessage = victims.getPort(i).newMessage();
					}

					writeMessage.writeByte(EXIT);
					writeMessage.send();
					writeMessage.finish();
				} catch (IOException e) {
					synchronized(this) {
						System.err.println("SATIN: Could not send exit message to " + victims.getIdent(i));
					}
				}
			}
			
			while(exitReplies != size) {
				satinPoll();
			}
		} else { // send exit ack to master
			SendPort mp = null;

			synchronized(this) {
				mp = getReplyPort(masterIdent);
			}

			try {
				WriteMessage writeMessage;
				if(COMM_DEBUG) {
					out.println("SATIN '" + ident.name() + 
						    "': sending exit ACK message to " + masterIdent);
				}
				
				writeMessage = mp.newMessage();
				writeMessage.writeByte(EXIT_REPLY);
				writeMessage.send();
				writeMessage.finish();
			} catch (IOException e) {
				synchronized(this) {
					System.err.println("SATIN: Could not send exit message to " + masterIdent);
				}
			}
		}

		algorithm.exit(); //give the algorithm time to clean up

		barrier(); /* Wait until everybody agrees to exit. */

		try {
			if(SUPPORT_TUPLE_MULTICAST) {
				tuplePort.free();
			}
		} catch (Throwable e) {
			System.err.println("tuplePort.free() throws " + e);
		}

		// If not closed, free ports. Otherwise, ports will be freed in leave calls.
		while(true) {
			try {
				SendPort s;
			
				synchronized(this) {
					if(victims.size() == 0) break;

					s = victims.getPort(0);
					
					if(COMM_DEBUG) {
						out.println("SATIN '" + ident.name() + 
							    "': freeing sendport to " +
							    victims.getIdent(0));
					}
					victims.remove(0);
				}
			
				if(s != null) {
					s.free();
				}
			
				/*if(COMM_DEBUG) {
				  out.println(" DONE");
				  }*/
			} catch (Throwable e) {
				System.err.println("port.free() throws " + e);
			}
		}
		
		try {
			receivePort.free();

			if(master) {
				barrierReceivePort.free();
			} else {
				barrierSendPort.free();
			}

			ibis.end();
		} catch (Throwable e) {
			System.err.println("port.free() throws " + e);
		}

		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': exited");
		}

		// Do a gc, and run the finalizers. Useful for printing statistics in Satin applications.
		System.gc();
		System.runFinalization();
		System.runFinalizersOnExit(true);

		System.exit(0); /* Needed for IBM jit. */
	}

	/* Only allowed when not stealing. */
	public void barrier() {
		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': barrier start");
		}

		if(!closed) {
			ibis.closeWorld();
		}

		int size;
		synchronized(this) {
			size = victims.size();
		}

		try {
			if(master) {
				for(int i=0; i<size; i++) {
					ReadMessage r = barrierReceivePort.receive();
					r.finish();
				}

				for(int i=0; i<size; i++) {
					SendPort s;
					synchronized(this) {
						s = victims.getPort(i);
					}
					WriteMessage writeMessage = s.newMessage();
					writeMessage.writeByte(BARRIER_REPLY);
					writeMessage.send();
					writeMessage.finish();
				}
			} else {
				WriteMessage writeMessage = barrierSendPort.newMessage();
				writeMessage.send();
				writeMessage.finish();
				
				if (!upcalls) {
					while(!gotBarrierReply/* && !exiting */) {
						satinPoll();
					}
					/* Imediately reset gotBarrierReply, we know that a reply has arrived. */
					gotBarrierReply = false; 
				} else {
					synchronized(this) {
						while(!gotBarrierReply) {
							try {
								wait();
							} catch (InterruptedException e) {
				// Ignore.
							}
						}
						/* Imediately reset gotBarrierReply, we know that a reply has arrived. */
						gotBarrierReply = false; 
					}
				}
			}
		} catch (IOException e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': error in barrier");
			System.exit(1);
		}

		if(!closed) {
			ibis.openWorld();
		}

		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': barrier DONE");
		}
	}

	// hold the lock when calling this
	protected void addToOutstandingJobList(InvocationRecord r) {
		if(ASSERTS) {
			assertLocked(this);
		}
		outstandingJobs.add(r);
	}

	// hold the lock when calling this
	protected void addToActiveTupleList(String key, Serializable data) {
		if(ASSERTS) {
			assertLocked(this);
		}
		activeTupleKeyList.add(key);
		activeTupleDataList.add(data);
	}

	// hold the lock when calling this
	protected void addToJobResultList(InvocationRecord r) {
		if(ASSERTS) {
			assertLocked(this);
		}
		resultList.add(r);
	}

	// hold the lock when calling this
	protected InvocationRecord getStolenInvocationRecord(int stamp, SendPortIdentifier sender, IbisIdentifier owner) {
		if(ASSERTS) {
			assertLocked(this);
			if(owner == null) { 
				System.err.println("SATIN '" + ident.name() + 
						   "': owner is null in getStolenInvocationRecord");
				System.exit(1);
			}
			if(!owner.equals(ident)) {
				System.err.println("SATIN '" + ident.name() + 
						   "': Removing wrong stamp!");
				System.exit(1);
			}
		}
		return outstandingJobs.remove(stamp, owner);
	}

	protected synchronized void sendResult(InvocationRecord r, ReturnRecord rr) {
		if(exiting || r.alreadySentExceptionResult) return;

		if(ASSERTS && r.owner == null) {
			System.err.println("SATIN '" + ident.name() + 
					   "': owner is null in sendResult");
			System.exit(1);
		}

		if(STEAL_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': sending job result to " +
				    r.owner.name() + ", exception = " + (r.eek == null ? "null" : ("" + r.eek)));
		}

		try {
			SendPort s = getReplyPort(r.owner);
			WriteMessage writeMessage = s.newMessage();
			if(r.eek == null) {
				writeMessage.writeByte(JOB_RESULT_NORMAL);
				writeMessage.writeObject(r.owner);  // hmm, I don't think this is needed --Rob @@@
				writeMessage.writeObject(rr);
			} else {
				if (rr == null) r.alreadySentExceptionResult = true;
				writeMessage.writeByte(JOB_RESULT_EXCEPTION);
				writeMessage.writeObject(r.owner);  // hmm, I don't think this is needed --Rob @@@
				writeMessage.writeObject(r.eek);
				writeMessage.writeInt(r.stamp);
			}
			writeMessage.send();
			long cnt = writeMessage.finish();

			if(STEAL_STATS) {
				if(inDifferentCluster(r.owner)) {
					interClusterMessages++;
					interClusterBytes += cnt;
				} else {
					intraClusterMessages++;
					intraClusterBytes += cnt;
				}
			} 
		} catch (IOException e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': Got Exception while sending steal request: " + e);
			System.exit(1);
		}
	}

	/* does a synchronous steal */
	protected void stealJob(Victim v) {

		if(ASSERTS && stolenJob != null) {
			throw new IbisError("EEEK, trying to steal while an unhandled stolen job is available.");
		}
/*
  synchronized(this) {
  q.print(System.err);
  outstandingJobs.print(System.err);
  onStack.print(System.err);
  }
*/
		if(STEAL_TIMING) {
			stealTimer.start();
		}

		if(STEAL_STATS) {
			stealAttempts++;
		}

		sendStealRequest(v, true);
		waitForStealReply();
	}

	protected void sendStealRequest(Victim v, boolean synchronous) {
		if(exiting) return;

		if(STEAL_DEBUG && synchronous) {
			System.err.println("SATIN '" + ident.name() + 
					   "': sending steal message to " +
					   v.ident.name());
		}
		if(STEAL_DEBUG && !synchronous) {
			System.err.println("SATIN '" + ident.name() + 
					   "': sending ASYNC steal message to " +
					   v.ident.name());
		}

		try {
			SendPort s = v.s;
			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(synchronous ? STEAL_REQUEST :
					       ASYNC_STEAL_REQUEST);
			writeMessage.send();
			long cnt = writeMessage.finish();
			if(STEAL_STATS) {
				if(inDifferentCluster(v.ident)) {
					interClusterMessages++;
					interClusterBytes += cnt;
				} else {
					intraClusterMessages++;
					intraClusterBytes += cnt;
				}
			}
		} catch (IOException e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': Got Exception while sending " +
					   (synchronous ? "" : "a") + "synchronous" +
					   " steal request: " + e);
			System.exit(1);
		}
	}

	protected boolean waitForStealReply() {
		if(exiting) return false;

		if(IDLE_TIMING) {
			idleTimer.start();
		}

		// Replaced this wait call, do something useful instead:
		// handleExceptions and aborts.
		if(upcalls) {
			if(ABORTS && HANDLE_ABORTS_IN_LATENCY) {
				while(true) {
					if(ABORTS && gotAborts) handleAborts();
					if(ABORTS && gotExceptions) handleExceptions();
					synchronized(this) {
						if(gotStealReply) {
							/* Immediately reset gotStealReply, we know that
							   a reply has arrived. */
							gotStealReply = false;
							break;
						}
					}
					Thread.yield();
				}
			} else {
				synchronized(this) {
					while(!gotStealReply) {
						try {
							wait();
						} catch (InterruptedException e) {
							throw new IbisError(e);
						}
					}
					/* Immediately reset gotStealReply, we know that a
					   reply has arrived. */
					gotStealReply = false;
				}
			}
		} else { // poll for reply
			while(!gotStealReply) {
				satinPoll();
			}
			gotStealReply = false;
		}

		if(IDLE_TIMING) {
			idleTimer.stop();
		}

		if(STEAL_TIMING) {
			stealTimer.stop();
		}

		/*if(STEAL_DEBUG) {
		  out.println("SATIN '" + ident.name() + 
		  "': got synchronous steal reply: " +
		  (stolenJob == null ? "FAILED" : "SUCCESS"));
		  }*/

		/* If successfull, we now have a job in stolenJob. */
		if (stolenJob == null) {
			return false;
		}

		/* I love it when a plan comes together! */

		if(STEAL_STATS) {
			stealSuccess++;
		}

		InvocationRecord myJob = stolenJob;
		stolenJob = null;

		callSatinFunction(myJob);

		return true;
	}

	public void join(IbisIdentifier joiner) {
		if(joiner.equals(ident)) return;

		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': '" + joiner.name() + "' from cluster '" +
				    joiner.cluster() + "' is trying to join");
		}
		try {
			ReceivePortIdentifier r = null;
			SendPort s = portType.createSendPort("satin sendport");
			Registry reg = ibis.registry();

			r = lookup("satin port on " + joiner.name());
			connect(s, r);

			if(SUPPORT_TUPLE_MULTICAST) {
				connect(tuplePort, r);
			}

			synchronized (this) {
				victims.add(joiner, s);
				notifyAll();
			}
			if(COMM_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': " + joiner.name() + " JOINED");
			}
		} catch (Exception e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': got an exception in Satin.join: " + e);
			System.exit(1);
		}
	}

	public void leave(IbisIdentifier leaver) {
		if(leaver.equals(this.ident)) return;

		if(COMM_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': " + leaver.name() + " left");
		}

		Victim v;

		synchronized (this) {
			v = victims.remove(leaver);
			notifyAll();

			if (v != null && v.s != null) {
				try {
					v.s.free();
				} catch (IOException e) {
					System.err.println("port.free() throws " + e);
				}
			}
		}
	}

	public static void connect(SendPort s, ReceivePortIdentifier ident) {
		boolean success = false;
		do {
			try {
				s.connect(ident);
				success = true;
			} catch (IOException e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
					// ignore
				}
			}
		} while (!success);
	}

	public ReceivePortIdentifier lookup(String name) throws IOException { 
		ReceivePortIdentifier temp = null;
		do {
			temp = ibis.registry().lookup(name);

			if (temp == null) {
				try {
					//					System.err.print("."); System.err.flush();
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// ignore
				}
			}
			
		} while (temp == null);
				
		return temp;
	} 

	void addToExceptionList(InvocationRecord r) {
		if(ASSERTS) {
			assertLocked(this);
		}
		exceptionList.add(r);
		gotExceptions = true;
		if(INLET_DEBUG) {
			out.println("SATIN '" + ident.name() + ": got remote exception!");
		}
	}

	void addToAbortList(int stamp, IbisIdentifier owner) {
		if(ASSERTS) {
			assertLocked(this);
		}
		if(ABORT_DEBUG) {
			out.println("SATIN '" + ident.name() + ": got abort message");
		}
		abortList.add(stamp, owner);
		gotAborts = true;
	}

	SendPort getReplyPort(IbisIdentifier ident) {
		SendPort s;
		if(ASSERTS) {
			assertLocked(this);
		}
		do {
			s = victims.getReplyPort(ident);
			if(s == null) {
				if(COMM_DEBUG) {
					
					out.println("SATIN '" + this.ident.name() + 
						    "': could not get reply port to " +
						    ident.name() + ", retrying");
				}
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			}
		} while (s == null);

		return s;
	}


	/* message combining for abort messages does not work (I tried). It is very unlikely that
	   one node stole more than one job from me */
	void sendAbortMessage(InvocationRecord r) {
		if(ABORT_DEBUG) {
			out.println("SATIN '" + ident.name() + ": sending abort message to: " + 
				    r.stealer + " for job " + r.stamp);
		}
		try {
			SendPort s = getReplyPort(r.stealer);
			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(ABORT);
			writeMessage.writeInt(r.parentStamp);
			writeMessage.writeObject(r.parentOwner);
			writeMessage.send();
			long cnt = writeMessage.finish();
			if(STEAL_STATS) {
				if(inDifferentCluster(r.stealer)) {
					interClusterMessages++;
					interClusterBytes += cnt;
				} else {
					intraClusterMessages++;
					intraClusterBytes += cnt;
				}
			} 
		} catch (IOException e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': Got Exception while sending abort message: " + e);
			// This should not be a real problem, it is just inefficient.
			// Let's continue...
			// System.exit(1);
		}
	}

	boolean satinPoll() {
		if(POLL_FREQ == 0) {
			return false;
		} else {
			long curr = pollTimer.currentTimeNanos();
			if(curr - prevPoll < POLL_FREQ) {
				return false;
			}
			prevPoll = curr;
		}

		if(POLL_TIMING) pollTimer.start();

		ReadMessage m = null;
		if(POLL_RECEIVEPORT) {

			try {
				m = receivePort.poll();
			} catch (IOException e) {
				System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while polling: " + e);
			}

			if(m != null) {
				messageHandler.upcall(m);
				try {
					m.finish(); // Finish the message, the upcall does not need to do this.
				} catch (Exception e) {
					System.err.println("error in finish: " + e);
				}
			}
		} else {
			try {
				ibis.poll(); // does not return message, but triggers upcall.
			} catch (Exception e) {
				System.err.println("polling failed, continuing anyway");
			}
		}

		if(POLL_TIMING) pollTimer.stop();

		if(m == null) {
			return false;
		}

		return true;
	}

	// This does not need to be synchronized, only one thread spawns.
	static public SpawnCounter newSpawnCounter() {
		if(spawnCounterCache == null) {
			return new SpawnCounter();
		}

		SpawnCounter res = spawnCounterCache;
		spawnCounterCache = res.next;
		res.value = 0;

		return res;
	}

	// This does not need to be synchronized, only one thread spawns.
	static public void deleteSpawnCounter(SpawnCounter s) {
		if(ASSERTS && s.value < 0) {
			System.err.println("deleteSpawnCounter: spawncouner < 0, val =" + s.value);
			new Exception().printStackTrace();
			System.exit(1);
		}

		s.next = spawnCounterCache;
		spawnCounterCache = s;
	}


	synchronized void addJobResult(ReturnRecord rr, SendPortIdentifier sender, IbisIdentifier i, 
				       Throwable eek, int stamp) {
		receivedResults = true;
		InvocationRecord r = null;

		if (rr != null) {
			r = getStolenInvocationRecord(rr.stamp, sender, i);
		} else {
			r = getStolenInvocationRecord(stamp, sender, i);
		}

		if(r != null) {
			if(rr != null) {
				rr.assignTo(r);
			} else {
				r.eek = eek;
			}
			if(r.eek != null) { // we have an exception, add it to the list. the list will be read during the sync
				if(ABORTS) {
					addToExceptionList(r);
				} else {
					throw new IbisError("Got exception result", r.eek);
				}
			} else {
				addToJobResultList(r);
			}
		} else {
			if(ABORTS) {
				if (ABORT_DEBUG) {
					out.println("SATIN '" + ident.name() + 
						    "': got result for aborted job, ignoring.");
				}
			} else {
				out.println("SATIN '" + ident.name() + 
					    "': got result for unknown job!");
				System.exit(1);
			}
		}
	}

	private void handleInlet(InvocationRecord r) {
		InvocationRecord oldParent;
		int oldParentStamp;
		IbisIdentifier oldParentOwner;

		if(r.inletExecuted) return;

		onStack.push(r);
		oldParent = parent;
		oldParentStamp = parentStamp;
		oldParentOwner = parentOwner;
		parentStamp = r.stamp;
		parentOwner = r.owner;
		parent = r;

		try {
			if(INLET_DEBUG) {
				System.err.println("SATIN '" + ident.name() + ": calling inlet caused by remote exception");
			}

			r.parentLocals.handleException(r.spawnId, r.eek, r);
			r.inletExecuted = true;
		} catch (Throwable t) {
			if(INLET_DEBUG) {
				System.err.println("Got an exception from exception handler! " + t);
//						t.printStackTrace();
				System.err.println("r = " + r);
				System.err.println("parent = " + r.parent);
			}
			if(r.parent == null) {
				System.err.println("EEEK, root job?");
				t.printStackTrace();
				System.exit(1);
			}

			if(ABORT_STATS) {
				aborts++;
			}

			synchronized(this) {
				// also kill the parent itself.
				// It is either on the stack or on a remote machine.
				// Here, this is OK, the child threw an exception, 
				// the parent did not catch it, and must therefore die.
				r.parent.aborted = true;
				r.parent.eek = t; // rethrow exception
				killChildrenOf(r.parent.stamp, r.parent.owner);

				if(!r.parentOwner.equals(ident)) {
					if(INLET_DEBUG || STEAL_DEBUG) {
						System.err.println("SATIN '" + ident.name() + ": prematurely sending exception result");
					}
					sendResult(r.parent, null);
				}
			}
		}

		// restore these, there may be more spawns afterwards...
		parentStamp = oldParentStamp;
		parentOwner = oldParentOwner;
		parent = oldParent;
		onStack.pop();
	}

        // trace back from the exception, and execute inlets / empty imlets back to the root
        // during this, prematurely send result messages.
	private void handleEmptyInlet(InvocationRecord r) {
		// if r does not have parentLocals, this means
		// that the PARENT does not have a try catch block around the spawn.
		// there is thus no inlet to call in the parent.

		if(r.parentLocals != null || r.eek == null) return;

		if(INLET_DEBUG) {
			out.println("SATIN '" + ident.name() + ": Got exception, empty inlet: " + r.eek + 
				    ": " + r.eek.getMessage());
//				r.eek.printStackTrace();
		}

		InvocationRecord curr = r;

		synchronized(this) {
			while(curr.parentLocals == null && curr.parent != null) {
				if(INLET_DEBUG) {
					System.err.println("SATIN '" + ident.name() + ": unwind");
				}
				curr = curr.parent;
				
				if(SPAWN_STATS) {
					aborts++;
				}
			}
 
			if(INLET_DEBUG) {
				System.err.println("SATIN '" + ident.name() + ": unwind stopped, curr = " + curr);
			}

			// also kill the parent itself.
			// It is either on the stack or on a remote machine.
			// Here, this is OK, the child threw an exception, 
			// the parent did not catch it, and must therefore die.
			curr.aborted = true;
			curr.eek = r.eek; // rethrow exception
			killChildrenOf(curr.stamp, curr.owner);

			if(curr.parentLocals != null) { // parent has inlet
				handleInlet(curr);
			}

			if(!curr.parentOwner.equals(ident)) {
				if(INLET_DEBUG || STEAL_DEBUG) {
					System.err.println("SATIN '" + ident.name() + ": prematurely sending exception result");
				}
				sendResult(curr, null);
			}
		}
	}

	private synchronized void handleResults() {
		while (true) {
			InvocationRecord r = resultList.removeIndex(0);
			if(r == null) break;

			handleEmptyInlet(r);
			r.spawnCounter.value--;
		}

		receivedResults = false;
	}

	protected void callSatinFunction(InvocationRecord r) {
		InvocationRecord oldParent;
		int oldParentStamp;
		IbisIdentifier oldParentOwner;

		if(gotActiveTuples) handleActiveTuples();

		if(ABORTS) {
			if(gotAborts) handleAborts();
			if(gotExceptions) handleExceptions();

			oldParent = parent;
			oldParentStamp = parentStamp;
			oldParentOwner = parentOwner;
		}

		if(ASSERTS) {
			if(r == null) {
				out.println("SATIN '" + ident.name() +
					    ": EEK, r = null in callSatinFunc");
				System.exit(1);
			}
			if(r.aborted) {
				out.println("SATIN '" + ident.name() +
					    ": spawning aborted job!");
				System.exit(1);
			}

			if(r.owner == null) {
				out.println("SATIN '" + ident.name() +
					    ": EEK, r.owner = null in callSatinFunc, r = " + r);
				new Throwable().printStackTrace();
				System.exit(1);
			}

			if(r.owner.equals(ident)) {
				if(r.spawnCounter.value < 0) {
					out.println("SATIN '" + ident.name() + 
						    ": spawncounter < 0 in callSatinFunc");
					System.exit(1);
				}
		
				if(ABORTS && r.parent == null && parentOwner.equals(ident) &&
				   r.parentStamp != -1) { 
					out.println("SATIN '" + ident.name() +
						    ": parent is null for non-root, should not happen here! job = " + r);
					System.exit(1);
				}
			}
		}

		if(ABORTS && r.parent != null && r.parent.aborted) {
			if(ABORT_DEBUG) { 
				out.print("SATIN '" + ident.name());
				out.print(": spawning job, parent was aborted! job = " + r);
				out.println(", parent = " + r.parent + "\n");
			}
			r.spawnCounter.value--;
			if(ASSERTS) {
				if(r.spawnCounter.value < 0) {
					out.println("SATIN '" + ident.name() + 
						    ": Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
			}
			return;
		}
	
		if(ABORTS) {
			onStack.push(r);
			parent = r;
			parentStamp = r.stamp;
			parentOwner = r.owner;
		}

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() +
				    "': callSatinFunc: stamp = " + r.stamp +
				    ", owner = " +
				    (r.owner.equals(ident) ? "me" : r.owner.toString()) +
				    ", parentStamp = " + r.parentStamp +
				    ", parentOwner = " + r.parentOwner);
		}

		if(r.owner.equals(ident)) {
			if (SPAWN_DEBUG) {
				out.println("SATIN '" + ident.name() +
					    "': callSatinFunc: spawn counter = " +
					    r.spawnCounter.value);
			}
			if(ABORTS) {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				try {
					r.runLocal();
				} catch (Throwable t) { 
                                        // This can only happen if an inlet has thrown an exception.
					// The semantics of this: all work is aborted,
					// and the exception is passed on to the spawner.
					// The parent is aborted, it must handle the exception.
					if(r.parentStamp == -1) { // root job
						System.err.println("SATIN '" + ident.name() + ": callSatinFunction: Unexpected exception: " + t);
						t.printStackTrace();
						System.exit(1);
					}

					if(INLET_DEBUG) {
						out.println("SATIN '" + ident.name() + ": Got exception from an inlet!: " + t + ": " + t.getMessage());
//						t.printStackTrace();
					}

					if(SPAWN_STATS) {
						aborts++;
					}

					synchronized(this) {
						// also kill the parent itself. 
						// It is either on the stack or on a remote machine.
				                // Here, this is OK, the inlet threw an exception, 
				                // the parent did not catch it, and must therefore die.
						r.parent.aborted = true; 
						r.parent.eek = t; // rethrow exception
						killChildrenOf(r.parent.stamp, r.parent.owner);

						if(!r.parentOwner.equals(ident)) {
							System.err.println("SATIN '" + ident.name() + ": prematurely sending exception result");
							sendResult(r.parent, null);
						}

					}
				}

				handleEmptyInlet(r);
			} else { // NO aborts
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				r.runLocal();
			}

			r.spawnCounter.value--;

			if(ASSERTS && r.spawnCounter.value < 0) {
				out.println("SATIN '" + ident.name() + ": Just made spawncounter < 0");
				new Exception().printStackTrace();
				System.exit(1);
			}

			if(ASSERTS && !ABORTS && r.eek != null) {
				out.println("Got exception: " + r.eek);
				System.exit(1);
			}

			if(SPAWN_DEBUG) {
				out.print("SATIN '" + ident.name() + ": callSatinFunc: stamp = " + r.stamp + 
					  ", parentStamp = " + r.parentStamp + 
					  ", parentOwner = " + r.parentOwner + " spawn counter = " + r.spawnCounter.value);

				if(r.eek == null) {
					out.println(" DONE");
				} else {
					out.println(" DONE with exception: " + r.eek);
				}
			}
		} else {
			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': RUNNING REMOTE CODE!");
			}
			ReturnRecord rr = null;
			if(ABORTS) {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				try {
					rr = r.runRemote();
					// May be needed if the method did not throw an exception,
					// but its child did, and there is an empty inlet.
					rr.eek = r.eek; 
				} catch (Throwable t) {
					out.println("SATIN '" + ident.name() + ": OOOhh dear, got exception in runremote: " + t);
					t.printStackTrace();
					System.exit(1);
				}
			} else {
				if(SPAWN_STATS) {
					jobsExecuted++;
				}
				rr = r.runRemote();
			}
			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': RUNNING REMOTE CODE DONE!");
			}

			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': REMOTE CODE SEND RESULT!, exception = " + (r.eek == null ? "null" : ("" + r.eek)));
			}
			// send wrapper back to the owner
			sendResult(r, rr);

			if(STEAL_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': REMOTE CODE SEND RESULT DONE!");
			}
		}

		if (ABORTS) {
			// restore these, there may be more spawns afterwards...
			parentStamp = oldParentStamp;
			parentOwner = oldParentOwner;
			parent = oldParent;
			onStack.pop();
		}
		
		if(ABORT_DEBUG && r.aborted) {
			System.err.println("Job on the stack was aborted: " + r.stamp + " EEK = " + (r.eek == null ? "null" : ("" + r.eek)));
		}

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': call satin func done!");
		}
	}

	public void client() {
		InvocationRecord r;
		SendPort s;

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': starting client!");
		}

		while(!exiting) {
			// steal and run jobs

			if(!upcalls) {
				satinPoll();
			}

			algorithm.clientIteration();
		}
	}

	public void spawn(InvocationRecord r) {
		if(ASSERTS) {
			if(algorithm instanceof MasterWorker) {
				synchronized(this) {
					if(!ident.equals(masterIdent)) {
						System.err.println("with the master/worker algorithm, work can only be spawned on the master!");
						System.exit(1);
					}
				}
			}
		}

		if(SPAWN_STATS) {
			spawns++;
		}

		r.spawnCounter.value++; 
		r.stamp = stampCounter++;
		r.owner = ident;

		if(ABORTS) {
			r.parentStamp = parentStamp;
			r.parentOwner = parentOwner;
			r.parent = parent;

/*
			if(parent != null) {
				for(int i=0; i<parent.parentStamps.size(); i++) {
					r.parentStamps.add(parent.parentStamps.get(i));
					r.parentOwners.add(parent.parentOwners.get(i));
				}
			}

			r.parentStamps.add(new Integer(parentStamp));
			r.parentOwners.add(parentOwner);
*/
		}

		q.addToHead(r);

		if(SPAWN_DEBUG) {
			out.println("SATIN '" + ident.name() + 
				    "': Spawn, counter = " + r.spawnCounter.value +
				    ", stamp = " + r.stamp + ", parentStamp = " + r.parentStamp +
				    ", owner = " + r.owner + ", parentOwner = " + r.parentOwner);
		}

//		if(ABORTS && gotAborts) handleAborts();
//		if(ABORTS && gotExceptions) handleExceptions();
	}


	synchronized void handleActiveTuples() {
		while(true) {
			if(activeTupleKeyList.size() > 0) {
				// do upcall
				try {
					String key = (String) activeTupleKeyList.remove(0);
					ActiveTuple data = (ActiveTuple) activeTupleDataList.remove(0);
					if(TUPLE_DEBUG) {
						System.err.println("calling active tuple key = " + 
								   key + " data = " + data);
					}
					data.handleTuple(key);
				} catch (Throwable t) {
					System.err.println("WARNING: active tuple threw exception: " + t);
				}
			} else {
				gotActiveTuples = false;
				return;
			}
		}
	}

	synchronized void handleAborts() {
		int stamp;
		IbisIdentifier owner;

		while(true) {
			if(abortList.count > 0) {
				stamp = abortList.stamps[0];
				owner = abortList.owners[0];
				abortList.removeIndex(0);
			} else {
				gotAborts = false;
				return;
			}
			
			if(ABORT_DEBUG) {
				out.println("SATIN '" + ident.name() + ": handling abort message: stamp = " + 
					    stamp + ", owner = " + owner);
			}
			
			if(ABORT_STATS) {
				aborts++;
			}

			killChildrenOf(stamp, owner);

			if(ABORT_DEBUG) {
				out.println("SATIN '" + ident.name() + ": handling abort message: stamp = " + 
					    stamp + ", owner = " + owner + " DONE");
			}
		}
	}


        // both here and in handleEmpty inlets: sendResult NOW if parentOwner is on remote machine
	void handleExceptions() {
		if(ASSERTS && !ABORTS) {
			System.err.println("cannot handle inlets, set ABORTS to true in Config.java");
			System.exit(1);
		}

		InvocationRecord r;
		while(true) {
			synchronized(this) {
				r = exceptionList.removeIndex(0);
				if (r == null) {
					gotExceptions = false;
					return;
				}
			}

			if(INLET_DEBUG) {
				out.println("SATIN '" + ident.name() + ": handling remote exception: " + r.eek + ", inv = " + r);
			}

			//  If there is an inlet, call it.
			if(r.parentLocals != null) {
				handleInlet(r);
			} else {
				if(INLET_DEBUG) {
					out.println("SATIN '" + ident.name() + ": impty inlet caused by remote exception: " + r.eek + ", inv = " + r);
				}

				handleEmptyInlet(r);
			}

			r.spawnCounter.value--;
			if(ASSERTS && r.spawnCounter.value < 0) {
				out.println("Just made spawncounter < 0");
				new Exception().printStackTrace();
				System.exit(1);
			}
			if(INLET_DEBUG) {
				out.println("SATIN '" + ident.name() + ": handling remote exception DONE");
			}
		}
	}

	public void sync(SpawnCounter s) {
		InvocationRecord r;
		
		if(SPAWN_STATS) {
			syncs++;
		}

		//Waar is dit pollen voor nodig, gebeurt onderaan al ??? - Maik

		//pollAsyncResult(); // for CRS

		if(SUPPORT_UPCALL_POLLING && upcalls && upcallPolling) {
			satinPoll();
		}

		if (POLL_FREQ > 0 && s.value == 0) { // sync is poll
			if(POLL_FREQ > 0 && !upcalls) satinPoll();
		}

		if(s.value == 0) { // sync is poll
			if (ABORTS) { 
				if(gotAborts) handleAborts();
				if(gotExceptions) handleExceptions();
			}
			if(gotActiveTuples) handleActiveTuples();
		}

		while(s.value > 0) {
			//pollAsyncResult(); // for CRS

			if(SPAWN_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': Sync, counter = " + s.value);
			}

			if(POLL_FREQ > 0 && !upcalls) satinPoll();

			if(receivedResults) handleResults();
			if(gotActiveTuples) handleActiveTuples();

			if(ABORTS && gotAborts) handleAborts();
			if(ABORTS && gotExceptions) handleExceptions();

			r = q.getFromHead(); // Try the local queue
			if(r != null) {
				callSatinFunction(r);
			} else {
				algorithm.clientIteration();
			}
		}
	}

	// the second parameter is valid only for clones with inlets
	// We do not need to set outstanding Jobs in the parent frame to null,
	// it is just used for assigning results.
	// get the lock, so no-one can steal jobs now, and no-one can change my tables.
	public synchronized void abort(InvocationRecord outstandingSpawns, InvocationRecord exceptionThrower) {
		//		System.err.println("q " + q.size() + ", s " + onStack.size() + ", o " + outstandingJobs.size());
		try {
			if(ABORT_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': Abort, outstanding = " + outstandingSpawns + 
					    ", thrower = " + exceptionThrower);
			}
			InvocationRecord curr;

			if(SPAWN_STATS) {
				aborts++;
			}

			if(exceptionThrower != null) { // can be null if root does an abort.
				// kill all children of the parent of the thrower.
				if(ABORT_DEBUG) {
					out.println("killing children of " + exceptionThrower.parentStamp);
				}
				killChildrenOf(exceptionThrower.parentStamp, exceptionThrower.parentOwner);
			}
			
			// now kill mine
			if(outstandingSpawns != null) {
				int stamp;
				int me;
				if(outstandingSpawns.parent == null) {
					stamp = -1;
				} else {
					stamp = outstandingSpawns.parent.stamp;
				}

				if(ABORT_DEBUG) {
					out.println("killing children of my own: " + stamp);
				}
				killChildrenOf(stamp, ident);
			}

			if(ABORT_DEBUG) {
				out.println("SATIN '" + ident.name() + 
					    "': Abort DONE");
			}
		} catch (Exception e) {
			System.err.println("GOT EXCEPTION IN RTS!: " + e);
			e.printStackTrace();
		}
	}

	private void killChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		if(ABORT_TIMING) {
			abortTimer.start();
		}

		if(ASSERTS) {
			assertLocked(this);
		}
/*
  int iter = 0;
  while(true) {
  long abortCount = abortedJobs;

  System.err.println("killChildrenOf: iter = " + iter + " abort cnt = " + abortedJobs);
*/
		// try work queue, outstanding jobs and jobs on the stack
		// but try stack first, many jobs in q are children of stack jobs.
		onStack.killChildrenOf(targetStamp, targetOwner);
		q.killChildrenOf(targetStamp, targetOwner);
		outstandingJobs.killChildrenOf(targetStamp, targetOwner);
/*
  if(abortedJobs == abortCount) {
				// no more jobs were removed.
				break;
				}

				iter++;
				}
*/
		if(ABORT_TIMING) {
			abortTimer.stop();
		}
	}

	static boolean isDescendentOf(InvocationRecord child, int targetStamp, IbisIdentifier targetOwner) {
		if(child.parentStamp == targetStamp && child.parentOwner.equals(targetOwner)) {
			return true;
		}
		if(child.parent == null || child.parentStamp < 0) return false;

		return isDescendentOf(child.parent, targetStamp, targetOwner);
	}
/*
  static boolean isDescendentOf(InvocationRecord child, int targetStamp, IbisIdentifier targetOwner) {
  for(int i = 0; i< child.parentStamps.size(); i++) {
  int currStamp = ((Integer) child.parentStamps.get(i)).intValue();
  IbisIdentifier currOwner = (IbisIdentifier) child.parentOwners.get(i);

  if(currStamp == targetStamp && currOwner.equals(targetOwner)) {
  System.err.print("t");
  return true;
  }
  }
  return false;
  }
*/
	public static boolean trylock(Object o) {
		try {
			o.notify();
		} catch (IllegalMonitorStateException e) {
			return false;
		}

		return true;
	}

	public static void assertLocked(Object o) {
		if(!trylock(o)) {
			System.err.println("AssertLocked failed!: ");
			new Exception().printStackTrace();
			System.exit(1);
		}
	}


        /* ------------------- tuple space stuff ---------------------- */

	protected synchronized void broadcastTuple(String key, Serializable data) {
		long count = 0;

		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + ident.name() + 
					   "': bcasting tuple " + key);
		}

		if(victims.size() == 0) return; // don't multicast when there is no-one.

		if(TUPLE_TIMING) {
			tupleTimer.start();
		}

		if(SUPPORT_TUPLE_MULTICAST) {
			try {
				WriteMessage writeMessage = tuplePort.newMessage();
				writeMessage.writeByte(TUPLE_ADD);
				writeMessage.writeObject(key);
				writeMessage.writeObject(data);
				writeMessage.send();

				if(TUPLE_STATS) {
					tupleMsgs++;
					count = writeMessage.finish();
				}
				else {
					writeMessage.finish();
				}

			} catch (IOException e) {
				System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while sending tuple update: " + e);
				System.exit(1);
			}
		} else {
			for(int i=0; i<victims.size(); i++) {
				try {
					SendPort s = victims.getPort(i);
					WriteMessage writeMessage = s.newMessage();
					writeMessage.writeByte(TUPLE_ADD);
					writeMessage.writeObject(key);
					writeMessage.writeObject(data);
					writeMessage.send();

					if(TUPLE_STATS && i == 0) {
						tupleMsgs++;
						count = writeMessage.finish();
					}
					else {
						writeMessage.finish();
					}

				} catch (IOException e) {
					System.err.println("SATIN '" + ident.name() + 
							   "': Got Exception while sending tuple update: " + e);
					System.exit(1);
				}
			}
		}

		tupleBytes += count;

		if(TUPLE_TIMING) {
			tupleTimer.stop();
//			System.err.println("SATIN '" + ident.name() + ": bcast of " + count + " bytes took: " + tupleTimer.lastTime());
		}
	}

	protected synchronized void broadcastRemoveTuple(String key) {
		long count = 0;
		
		if(TUPLE_DEBUG) {
			System.err.println("SATIN '" + ident.name() + 
					   "': bcasting remove tuple" + key);
		}

		if(victims.size() == 0) return; // don't multicast when there is no-one.

		if(TUPLE_TIMING) {
			tupleTimer.start();
		}

		if(SUPPORT_TUPLE_MULTICAST) {
			try {
				WriteMessage writeMessage = tuplePort.newMessage();
				writeMessage.writeByte(TUPLE_DEL);
				writeMessage.writeObject(key);
				writeMessage.send();

				if(TUPLE_STATS) {
					tupleMsgs++;
					count += writeMessage.finish();
				}
				else {
					writeMessage.finish();
				}

			} catch (IOException e) {
				System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while sending tuple update: " + e);
				System.exit(1);
			}
		} else {
			for(int i=0; i<victims.size(); i++) {
				try {
					SendPort s = victims.getPort(i);
					WriteMessage writeMessage = s.newMessage();
					writeMessage.writeByte(TUPLE_DEL);
					writeMessage.writeObject(key);
					writeMessage.send();

					if(TUPLE_STATS && i == 0) {
						tupleMsgs++;
						count += writeMessage.finish();
					}
					else {
						writeMessage.finish();
					}

				} catch (IOException e) {
					System.err.println("SATIN '" + ident.name() + 
							   "': Got Exception while sending tuple update: " + e);
					System.exit(1);
				}
			}
		}

		tupleBytes += count;

		if(TUPLE_TIMING) {
			tupleTimer.stop();
//			System.err.println("SATIN '" + ident.name() + ": bcast of " + count + " bytes took: " + tupleTimer.lastTime());
		}
	}

        /* ------------------- pause/resume space stuff ---------------------- */

	/** Pause Satin operation. 
	    This method can optionally be called before a large sequential part in a program.
	    This will temporarily pause Satin's internal load distribution strategies to 
	    avoid communication overhead during sequential code.
	**/
	public static void pause() {
		if(this_satin == null || !this_satin.upcalls) return;
		this_satin.receivePort.disableUpcalls();
	}

	/** Resume Satin operation. 
	    This method can optionally be called after a large sequential part in a program.
	**/
	public static void resume() {
		if(this_satin == null || !this_satin.upcalls) return;
		this_satin.receivePort.enableUpcalls();
	}

	/** Returns whether it might be useful to spawn more methods.
	    If there is enough work in the system to keep all processors busy, this
	    method returns false.
	**/
	public static boolean needMoreJobs() {
		if(this_satin == null) return true;
		int size = 0;

		synchronized(this_satin) {
			size = this_satin.victims.size();
			if(this_satin.q.size() / (size+1) > 100) return false;
		}

		return true;
	}

	/** Returns whether the current method was generated by the machine it is running on.
	    methods can be distributed to remote machines by the Satin runtime system,
	    in which case this method returns false/
	 **/
	public static boolean localJob() {
		if(this_satin == null) return true;

		if(this_satin.parentOwner == null) return true;

		return this_satin.parentOwner.equals(this_satin.ident);
	}
}
