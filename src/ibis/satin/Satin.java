package ibis.satin;

import ibis.ipl.*;
import ibis.util.*;
import java.util.Random;
import java.util.Properties;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Vector;
import java.util.Enumeration;

/* 
   One important invariant: there is only one thread per machine that spawns work.
   Another: there is only one lock: the global satin object.
   invariant: all jobs in the queue and outstandingJobsList have me as owner.
   invariant: all invocations records in use are in one of these lists:
     - onStack (being worked on)
     - OutstandingJobs (stolen)
     - q (spawned but not yet running)

   invariant: When running java code, the parentStamp, parentOwner and 
   parent contain the spawner of the work. (parent may be null when running the root node,
   or when the spawner is a remote machine).

   Satin.spawn gets the satin wrapper.
   This can be serialized, and run may be called.

   When a job is spawned, the RTS put a stamp on it.
   When a job is stolen the RTS puts an entry in a table.
   The runRemote method creates a return wrapper containing the return value.
   The runtime system sends the return value back, together with the
   original stamp. The victim can do a lookup to find the entry (containing the spawn counter and 
   result pointer) that corresponds with the job.
*/

public final class Satin implements Config, Protocol, ResizeHandler {
	private Ibis ibis;
	IbisIdentifier ident; // used in messageHandler

	/* Options. */
	private boolean closed = false;
	private boolean stats = false;
	private boolean panda = false;
	private boolean mantaSerialization = false;

	/* Am I the root (the one running main)? */
	public boolean master = false; // used in generated code
	public String[] mainArgs; // used in generated code
	private String name;
	private IbisIdentifier masterIdent;

	/* My scheduling algorithm. */
	private Algorithm a;
	private String alg;

	DEQueueDijkstra q = new DEQueueDijkstra(this);
//	DEQueue q = new DEQueue(this);
	private PortType portType;
	private ReceivePort receivePort;
	private ReceivePort barrierReceivePort; /* Only for the master. */
	private SendPort barrierSendPort; /* Only for the clients. */

	volatile boolean exiting = false; // used in messageHandler
	Random random = new Random(); // used in victimTable
	private MessageHandler messageHandler;

	private static SpawnCounter spawnCounterCache = null;

	/* Used to locate the invocation record corresponing to the result of a remote job. */
	private IRVector outstandingJobs = new IRVector();
	private int stampCounter = 0;

	private IRStack onStack = new IRStack(this);

	private IRVector exceptionList = new IRVector();

	/* abort messages are queued until the sync. */
	private StampVector abortList = new StampVector();

	/* used to store reply messages */
	boolean gotStealReply = false; // used in messageHandler
	boolean gotBarrierReply = false; // used in messageHandler
	ReadMessage m; // used in messageHandler

	/* Variables that contain statistics. */
	private long spawns = 0;
	private long syncs = 0;
	private long aborts = 0;
	public long abortedJobs = 0; // used in dequeue
	private long abortMessages = 0;
	private long stealAttempts = 0;
	private long stealSuccess = 0;
	long stolenJobs = 0; // used in messageHandler
	long stealRequests = 0; // used in messageHandler

	private int parentStamp = -1;
	private IbisIdentifier parentOwner = null;
	public InvocationRecord parent = null; // used in generated code

	/* use these to avoid locking */
	private volatile boolean gotExceptions = false;
	private volatile boolean gotAborts = false;

	VictimTable victims; /* All victims, myself NOT included. The elements are Victims. */


	public Satin(String[] args) {
		init(args);
	}

//	private native void DebugMe(Object o, Object o2);

	private void init(String[] args) {
		Properties p = System.getProperties();
		String hostName = null;
		int poolSize = 0; /* Only used with closed world. */
		victims = new VictimTable(this);

		try {
			InetAddress address = InetAddress.getLocalHost();
			hostName = address.getHostName();

		} catch (UnknownHostException e) {
			System.err.println("SATIN:init: Cannot get ip of local host: " + e);
			System.exit(1);
		}

		/* Parse commandline parameters. Remove everything that starts with satin. */
		Vector tempArgs = new Vector();
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-satin-closed")) { /* Closed world assumption. */
				closed = true;
			} else if(args[i].equals("-satin-panda")) {
				panda = true;
			} else if(args[i].equals("-satin-stats")) {
				stats = true;
			} else if(args[i].equals("-satin-manta")) {
				mantaSerialization = true;
			} else if(args[i].equals("-satin-alg")) {
				i++;
				alg = args[i];
			} else {
				tempArgs.add(args[i]);
			}
		}
		mainArgs = new String[tempArgs.size()];
		for(int i=0; i<tempArgs.size(); i++) {
			mainArgs[i] = (String) tempArgs.get(i);
		}

		if(closed) {
			String pool = p.getProperty("pool_total_hosts");
			if(pool == null) {
				System.out.println("property 'pool_total_hosts' not set, and running with closed world.");
				System.exit(1);
			}

			poolSize = Integer.parseInt(pool);
		}

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + hostName + "': init ibis" );
		}

		for(int i=0; (i<10 && ibis == null); i++) {
			try {
				name = "ibis@" + hostName + "_" + Math.abs(random.nextInt());
				if(panda) {
					ibis = Ibis.createIbis(name, "ibis.ipl.impl.messagePassing.panda.PandaIbis", this);
				} else {
					ibis = Ibis.createIbis(name, "ibis.ipl.impl.tcp.TcpIbis", this);
				}
			} catch (IbisException e) {
				System.err.println("SATIN '" + hostName + "': Could not start ibis with name '" + name + "': " + e);
				e.printStackTrace();
			}
		}
		if(ibis == null) {
			System.err.println("SATIN: giving up");
			System.exit(1);
		}

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + hostName + "': init ibis DONE" );
		}

		ident = ibis.identifier();
		parentOwner = ident;

		try {
			Registry r = ibis.registry();
			
			StaticProperties s = new StaticProperties();
			if(mantaSerialization) {
				s.add("Serialization", "manta");
			}

			portType = ibis.createPortType("satin porttype", s);

			messageHandler = new MessageHandler(this);
			receivePort = portType.createReceivePort("satin port on " + 
								 ident.name(), messageHandler);

			masterIdent = (IbisIdentifier) r.elect("satin master", ident);

			if(masterIdent.equals(ident)) {
				/* I an the master. */
				if(COMM_DEBUG) {
					System.out.println("SATIN '" + hostName + "': init ibis: I am the master");
				}
				master = true;
			} else {
				if(COMM_DEBUG) {
					System.out.println("SATIN '" + hostName + "': init ibis I an slave" );
				}
			}

			if(master) {
				barrierReceivePort = portType.createReceivePort("satin barrier receive port");
				barrierReceivePort.enableConnections();
			} else {
				barrierSendPort = portType.createSendPort("satin barrier send port on " + 
										ident.name());
				ReceivePortIdentifier barrierIdent = lookup("satin barrier receive port");
				connect(barrierSendPort, barrierIdent);
			}
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + hostName + "': Could not start ibis: " + e);
			System.exit(1);
		} catch (IbisException e) {
			System.err.println("SATIN '" + hostName + "': Could not start ibis: " + e);
			System.exit(1);
		}

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
					   "': init ibis DONE2");
		}

		if(master) {
			if(closed) {
				System.out.println("SATIN '" + hostName + "': running with closed world, " + poolSize + " host(s)");
			} else {
				System.out.println("SATIN '" + hostName + "': running with open world");
			}
		}

		if(alg == null) {
			if(master) {
				System.out.println("SATIN '" + hostName + "': satin_algorithm property not specified, using RS");
			}
			alg = "RS";
		}

		if(alg.equals("RS")) {
			a = new RandomWorkStealing(this);
		} else {
			System.out.println("SATIN '" + hostName + "': satin_algorithm '" + alg + "' unknown");
			System.exit(1);
		}

		receivePort.enableUpcalls();
		receivePort.enableConnections();
		ibis.openWorld();

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + hostName + "': pre barrier" );
		}

		if(closed) {
			synchronized(this) {
				while(victims.size() != poolSize - 1) {
					try {
						wait();
					} catch (InterruptedException e) {
						// Ignore.
					}
				}
				ibis.closeWorld();
			}

			barrier();
		}

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + hostName + "': post barrier" );
		}
	}

	public void exit() {
                /* send exit messages to all others */
		int size;

		if(!closed) {
			ibis.closeWorld();
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
							System.out.println("SATIN '" + ident.name() + 
									   "': sending exit message to " + victims.getIdent(i));
						}

						writeMessage = victims.getPort(i).newMessage();
					}
					writeMessage.writeByte(EXIT);
					writeMessage.send();
					writeMessage.finish();
					
				} catch (IbisIOException e) {
					synchronized(this) {
						System.err.println("SATIN: Could not send exit message to " + victims.getIdent(i));
					}
				}
			}
		}

		barrier(); /* Wait until everybody agrees to exit. */

		// If not closed, free ports. Otherwise, ports will be freed in leave calls.
		while(true) {
		    try {
			SendPort s;
			
			synchronized(this) {
				if(victims.size() == 0) break;
				
				s = victims.getPort(0);
				
				if(COMM_DEBUG) {
					System.out.print("SATIN '" + ident.name() + 
							 "': freeing sendport to " + victims.getIdent(0));
				}
				victims.remove(0);
			}
			
			if(s != null) {
				s.free();
			}
			
			if(COMM_DEBUG) {
				System.out.println(" DONE");
			}
		    } catch (IbisIOException e) {
			System.err.println("port.free() throws " + e);
		    }
		}
		
		receivePort.free();

		try {
			if(master) {
				barrierReceivePort.free();
			} else {
				barrierSendPort.free();
			}
		} catch (IbisIOException e) {
		    System.err.println("port.free() throws " + e);
		}
		ibis.end();

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
					   "': exited");
		}

		if(SPAWN_STATS && stats) {
			System.out.println("SATIN '" + ident.name() + 
					   "': SPAWN_STATS: spawns = " + spawns + " syncs = " + syncs);
			System.out.println("SATIN '" + ident.name() + 
					   "': ABORT_STATS: aborts = " + aborts + " abort msgs = " + abortMessages +
					   " aborted jobs = " + abortedJobs);
		}
		if(STEAL_STATS && stats) {
			System.out.println("SATIN '" + ident.name() + 
					   "': STEAL_STATS 1: attempts = " + stealAttempts + " success = " + stealSuccess + 
					   " (" + (((double) stealSuccess / stealAttempts) * 100.0) + " %)");

			System.out.println("SATIN '" + ident.name() + 
					   "': STEAL_STATS 2: requests = " + stealRequests + " jobs stolen = " + stolenJobs);
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
			System.out.println("SATIN '" + ident.name() + 
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
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
					   "': error in barrier");
			System.exit(1);
		}

		if(!closed) {
			ibis.openWorld();
		}

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
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
	protected InvocationRecord getStolenInvocationRecord(int stamp, SendPortIdentifier sender, IbisIdentifier owner) {
		if(ASSERTS) {
			assertLocked(this);
			if(owner == null) { 
				System.err.println("SATIN '" + ident.name() + 
						   "': owner is null in getStolenInvocationRecord");
				System.exit(1);
			}
			if(!owner.equals(ident)) {
				System.out.println("SATIN '" + ident.name() + 
						   "': Removing wrong stamp!");
				System.exit(1);
			}
		}
		return outstandingJobs.remove(stamp, owner);
	}

	protected synchronized void sendResult(InvocationRecord r, ReturnRecord rr) {
		if(ASSERTS && r.owner == null) {
			System.err.println("SATIN '" + ident.name() + 
					   "': owner is null in sendResult");
			System.exit(1);
		}

		if(STEAL_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
					   "': sending job result to " +
					   r.owner.name());
		}

		try {
			SendPort s = getReplyPort(r.owner);
			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(JOB_RESULT);
			writeMessage.writeObject(r.owner); 
			writeMessage.writeObject(rr);
			writeMessage.send();
			writeMessage.finish();
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while sending steal request: " + e);
			System.exit(1);
		}
	}

	protected boolean stealJob(Victim v) {
		if(STEAL_STATS) {
			stealAttempts++;
		}
		if(STEAL_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
					   "': sending steal message to " +
					   v.ident.name());
		}

		try {
			SendPort s = v.s;
			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(STEAL_REQUEST);
			writeMessage.send();
			writeMessage.finish();
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while sending steal request: " + e);
			System.exit(1);
		}

		synchronized(this) {
			while(!gotStealReply) {
				try {
					wait();
				} catch (InterruptedException e) {
				// Ignore.
				}
			}
			/* Imediately reset gotStealReply, we know that a reply has arrived. */
			gotStealReply = false;
		}

		if(STEAL_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
					   "': got steal reply from " +
					   v.ident.name() + ", " + (m == null ? "FAILED" : "SUCCESS"));
		}

		/* If successfull, we now have a message in m. */
		if (m == null) {
			return false;
		}

		if(STEAL_STATS) {
			stealSuccess++;
		}

		/* I love it when a plan comes together! */
		InvocationRecord r = null;
		try {
			r = (InvocationRecord) m.readObject();
			m.finish();
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while reading steal reply: " + e);
			System.exit(1);
		} catch (ClassNotFoundException e1) {
			System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while reading steal reply: " + e1);
			System.exit(1);
		}

		callSatinFunction(r);
		return true;
	}

	public void join(IbisIdentifier joiner) {
		if(joiner.equals(ident)) return;

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
						   "': '" + joiner.name() + "' is trying to join");
		}
		try {
			ReceivePortIdentifier r = null;
			SendPort s = portType.createSendPort("satin sendport");
			Registry reg = ibis.registry();

			r = lookup("satin port on " + joiner.name());
			connect(s, r);

			synchronized (this) {
				victims.add(joiner, s);
				notifyAll();
			}
			if(COMM_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': " + joiner.name() + " JOINED");
			}
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
						   "': got an exception in Satin.join: " + e);
			System.exit(1);
		}
	}

	public void leave(IbisIdentifier leaver) {
		if(leaver.equals(this.ident)) return;

		if(COMM_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
						   "': " + leaver.name() + " left");
		}

		Victim v;

		synchronized (this) {
			v = victims.remove(leaver);
			notifyAll();

			if (v != null && v.s != null) {
			    try {
				v.s.free();
			    } catch (IbisIOException e) {
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
			} catch (IbisIOException e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e2) {
					// ignore
				}
			}
		} while (!success);
	}

	public ReceivePortIdentifier lookup(String name) throws IbisIOException { 
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
			System.out.println("got remote exception!");
		}
	}

	void addToAbortList(int stamp, IbisIdentifier owner) {
		if(ASSERTS) {
			assertLocked(this);
		}
		if(ABORT_DEBUG) {
			System.out.println("SATIN '" + ident.name() + ": got abort message");
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
					System.out.println("SATIN '" + ident.name() + 
							   "': could not get reply port, retrying");
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
	private void sendAbortMessage(InvocationRecord r) {
		if(ABORT_DEBUG) {
			System.out.println("SATIN '" + ident.name() + ": sending abort message to: " + 
					   r.stealer + " for job " + r.stamp);
		}
		try {
			SendPort s = getReplyPort(r.stealer);
			WriteMessage writeMessage = s.newMessage();
			writeMessage.writeByte(ABORT);
			writeMessage.writeInt(r.parentStamp);
			writeMessage.writeObject(r.parentOwner);
			writeMessage.send();
			writeMessage.finish();
		} catch (IbisIOException e) {
			System.err.println("SATIN '" + ident.name() + 
						   "': Got Exception while sending abort message: " + e);
			// This should not be a real problem, it is just inefficient.
			// Let's continue...
			// System.exit(1);
		}
	}

	/* This does not need to be synchronized, only one thread spawns. */
	static public SpawnCounter newSpawnCounter() {
		if(spawnCounterCache == null) {
			return new SpawnCounter();
		}

		SpawnCounter res = spawnCounterCache;
		spawnCounterCache = res.next;
		res.value = 0;

		return res;
	}

	/* This does not need to be synchronized, only one thread spawns. */
	static public void deleteSpawnCounter(SpawnCounter s) {
		if(ASSERTS && s.value < 0) {
			System.out.println("deleteSpawnCounter: spawncouner < 0, val =" + s.value);
			new Exception().printStackTrace();
			System.exit(1);
		}

		s.next = spawnCounterCache;
		spawnCounterCache = s;
	}

	private void callSatinFunction(InvocationRecord r) {
		InvocationRecord oldParent;
		int oldParentStamp;
		IbisIdentifier oldParentOwner;

		if(ABORTS) {
			oldParent = parent;
			oldParentStamp = parentStamp;
			oldParentOwner = parentOwner;
		}

		if(ASSERTS) {
			if(r == null) {
				System.out.println("SATIN '" + ident.name() + ": EEK, r = null in callSatinFunc");
				System.exit(1);
			}
			if(r.aborted) {
				System.out.println("SATIN '" + ident.name() + ": spawning aborted job!");
				System.exit(1);
			}
		
			if(r.owner.equals(ident)) {
				if(r.spawnCounter.value < 0) {
					System.out.println("SATIN '" + ident.name() + 
							   ": spawncounter < 0 in callSatinFunc");
					System.exit(1);
				}

				if(ASSERTS && r.parent == null && parentOwner.equals(ident) && r.parentStamp != -1) { 
					System.out.println("SATIN '" + ident.name() +
							   ": parent is null for non-root, should not happen here! job = " + r);
					System.exit(1);
				}
			}
		}
		
		if(ABORTS && r.parent != null && r.parent.aborted) {
			if(ABORT_DEBUG) { 
				System.out.print("SATIN '" + ident.name());
				System.out.print(": spawning job, parent was aborted! job = " + r);
				System.out.println(", parent = " + r.parent + "\n");
//				System.exit(1);
			}
			r.spawnCounter.value--;
			if(ASSERTS) {
				if(r.spawnCounter.value < 0) {
					System.out.println("SATIN '" + ident.name() + 
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

		if(r.owner.equals(ident)) {
			if(SPAWN_DEBUG) {
				System.out.println("SATIN '" + ident.name() + ": callSatinFunc: stamp = " + r.stamp + 
						   ", parentStamp = " + r.parentStamp + 
						   ", parentOwner = " + r.parentOwner + 
						   " spawn counter = " + r.spawnCounter.value);
			}
			if(ABORTS) {
				try {
					r.runLocal();
				} catch (Throwable t) { // this can only happen if an inlet has thrown an exception.
					if(r.parentStamp == -1) { // root job
						System.err.println("SATIN '" + ident.name() + ": Unexpected exception: " + t);
						t.printStackTrace();
						System.exit(1);
					}
					
					System.out.println("SATIN '" + ident.name() + ": Got exception from an inlet!: " + t + ": " + t.getMessage());
					t.printStackTrace();

					if(SPAWN_STATS) {
						aborts++;
					}
					synchronized(this) {
						r.parent.eek = t;
						killChildrenOf(r.parent.stamp, r.parent.owner);
						
						// also kill the parent itself. it is either on the stack or on a remote machine.
						r.parent.aborted = true;
					}
				}
			} else {
				r.runLocal();
			}

			r.spawnCounter.value--;
			if(ASSERTS && r.spawnCounter.value < 0) {
				System.out.println("SATIN '" + ident.name() + ": Just made spawncounter < 0");
				new Exception().printStackTrace();
				System.exit(1);
			}

			if(SPAWN_DEBUG) {
				System.out.print("SATIN '" + ident.name() + ": callSatinFunc: stamp = " + r.stamp + 
						 ", parentStamp = " + r.parentStamp + 
						 ", parentOwner = " + r.parentOwner + " spawn counter = " + r.spawnCounter.value);
				if(r.eek == null) {
					System.out.println(" DONE");
				} else {
					System.out.println(" DONE with exception");
				}
			}
		} else {
			if(STEAL_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': RUNNING REMOTE CODE!");
			}
			ReturnRecord rr = null;
			if(ABORTS) {
				try {
					rr = r.runRemote();
				} catch (Throwable t) { // @@@ handle this
					System.out.println("SATIN '" + ident.name() + ": OOOhh dear, got exception in runremote: " + t);
					t.printStackTrace();
					System.exit(1);
				}
			} else {
				rr = r.runRemote();
			}
			if(STEAL_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': RUNNING REMOTE CODE DONE!");
			}

			if(STEAL_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': REMOTE CODE SEND RESULT!");
			}
			// send wrapper back to the owner
			sendResult(r, rr);

			if(STEAL_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
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
			System.out.println("Job on the stack was aborted: " + r.stamp);
		}
	}

	public void client() {
		InvocationRecord r;
		SendPort s;

		while(!exiting) {
			// steal and run jobs

			r = q.getFromHead();
			if(r != null) {
				callSatinFunction(r);
			} else {
				/* We are idle. There is no work in the queue, and we are not running Java code.
				   Call appropriate handler. */
				a.syncHandler();
			}
		}
	}

	public void spawn(InvocationRecord r) {
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
		}

		q.addToHead(r);

		if(SPAWN_DEBUG) {
			System.out.println("SATIN '" + ident.name() + 
			   "': Spawn, counter = " + r.spawnCounter.value + ", stamp = " + r.stamp +
			   ", parentStamp = " + r.parentStamp + ", owner = " + r.owner + ", parentOwner = " + r.parentOwner);
		}

		if(ABORTS && gotAborts) handleAborts();
		if(ABORTS && gotExceptions) handleExceptions();
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
				System.out.println("SATIN '" + ident.name() + ": handling abort message: stamp = " + 
						   stamp + ", owner = " + owner);
			}
			
			killChildrenOf(stamp, owner);

			if(ABORT_DEBUG) {
				System.out.println("SATIN '" + ident.name() + ": handling abort message: stamp = " + 
						   stamp + ", owner = " + owner + " DONE");
			}
		}
	}

	void handleExceptions() {
		if(!ABORTS) {
			System.out.println("cannot handle inlets, set ABORTS to true in Config");
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
				System.out.println("SATIN '" + ident.name() + ": handling remote exception: " + r.eek + ", inv = " + r);
			}

			InvocationRecord oldParent;
			int oldParentStamp;
			IbisIdentifier oldParentOwner;

			onStack.push(r);
			oldParent = parent;
			oldParentStamp = parentStamp;
			oldParentOwner = parentOwner;
			parentStamp = r.stamp;
			parentOwner = r.owner;
			parent = r;

			try {
				r.parentLocals.handleException(r.spawnId, r.eek, r);
			} catch (Throwable t) {
				System.err.println("EEEK, got an exception from exception handler!");
				System.err.println("parent = " + r.parent);
				if(r.parent == null) {
					System.err.println("EEEK, root job?");
					t.printStackTrace();
					System.exit(1);
				}
				synchronized(this) {
					r.parent.aborted = true;
					r.parent.eek = t;
					killChildrenOf(r.parent.stamp, r.parent.owner);
				}
			}

			// restore these, there may be more spawns afterwards...
			parentStamp = oldParentStamp;
			parentOwner = oldParentOwner;
			parent = oldParent;
			onStack.pop();
			
			r.spawnCounter.value--;
			if(ASSERTS && r.spawnCounter.value < 0) {
				System.out.println("Just made spawncounter < 0");
				new Exception().printStackTrace();
				System.exit(1);
			}
			if(INLET_DEBUG) {
				System.out.println("SATIN '" + ident.name() + ": handling remote exception DONE");
			}
		}
	}

	public void sync(SpawnCounter s) {
		InvocationRecord r;
		
		if(SPAWN_STATS) {
			syncs++;
		}
		
		while(s.value > 0) {
			if(SPAWN_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': Sync, counter = " + s.value);
			}

			if(ABORTS && gotAborts) handleAborts();
			if(ABORTS && gotExceptions) handleExceptions();

			r = q.getFromHead();
			if(r != null) {
				callSatinFunction(r);
			} else {
				/* We are idle. There is no work in the queue, and we are not running Java code.
				   Call appropriate handler. */
				a.syncHandler();
			}
		}
	}

	// the second parameter is valid only for clones with inlets
	// We do not need to set outstanding Jobs in the parent frame to null,
	// it is just used for assigning results.
	// get the lock, so no-one can steal jobs now, and no-one can change my tables.
	public synchronized void abort(InvocationRecord outstandingSpawns, InvocationRecord exceptionThrower) {
		try {
			if(ABORT_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': Abort, outstanding = " + outstandingSpawns + 
						   ", thrower = " + exceptionThrower);
			}
			InvocationRecord curr;

			if(SPAWN_STATS) {
				aborts++;
			}

			if(ASSERTS && exceptionThrower == null) {
				System.out.println("eek, exceptionThrower is null in abort");
				System.exit(1);
			}

			// kill all children of the parent of the thrower.
			if(ABORT_DEBUG) {
				System.out.println("killing children of " + exceptionThrower.parentStamp);
			}
			killChildrenOf(exceptionThrower.parentStamp, exceptionThrower.parentOwner);

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
					System.out.println("killing children of my own: " + stamp);
				}
				killChildrenOf(stamp, ident);
			}

			if(ABORT_DEBUG) {
				System.out.println("SATIN '" + ident.name() + 
						   "': Abort DONE");
			}
		} catch (Exception e) {
			System.out.println("GOT EXCEPTION IN RTS!: " + e);
			e.printStackTrace();
		}
	}

	private void killChildrenOf(int targetStamp, IbisIdentifier targetOwner) {
		if(ASSERTS) {
			assertLocked(this);
		}

		// try work queue, outstanding jobs and jobs on the stack
		q.killChildrenOf(targetStamp, targetOwner);

		InvocationRecord curr;
		for(int i=0; i<outstandingJobs.count; i++) {
			curr = outstandingJobs.l[i];
			if(isDescendentOf(curr, targetStamp, targetOwner)) {
				if(ABORT_DEBUG) {
					System.out.println("found stolen child: " + curr.stamp + ", it depends on " + targetStamp);
				}
				curr.spawnCounter.value--;
				if(ASSERTS && curr.spawnCounter.value < 0) {
					System.out.println("Just made spawncounter < 0");
					new Exception().printStackTrace();
					System.exit(1);
				}
				if(ABORT_STATS) {
					abortedJobs++;
				}
				if(STEAL_STATS) {
					abortMessages++;
				}
				outstandingJobs.removeIndex(i);
				i--;
				sendAbortMessage(curr);
			}
		}

		onStack.killChildrenOf(targetStamp, targetOwner);
	}

	static boolean isDescendentOf(InvocationRecord child, int targetStamp, IbisIdentifier targetOwner) {
		if(child.parentStamp == targetStamp && child.parentOwner.equals(targetOwner)) {
			return true;
		}
		if(child.parent == null || child.parentStamp < 0) return false;

		return isDescendentOf(child.parent, targetStamp, targetOwner);
	}

	public final static boolean trylock(Object o) {
		try {
			o.notify();
		} catch (IllegalMonitorStateException e) {
			return false;
		}

		return true;
	}

	public final static void assertLocked(Object o) {
		if(!trylock(o)) {
			System.err.println("AssertLocked failed!: ");
			new Exception().printStackTrace();
			System.exit(1);
		}
	}
}
