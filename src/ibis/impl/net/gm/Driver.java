package ibis.impl.net.gm;

import ibis.impl.net.InterruptedIOException;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetLockArray;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.Ibis;
import ibis.util.Monitor;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Hashtable;

/**
 * The NetIbis GM driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	// Native functions
	//private static native void gm_init();
	//private static native void gm_exit();

	static final boolean	DEBUG = false; // true; // false;

	static final boolean	VERBOSE_INTPT = DEBUG; // true; // false;

	static final boolean	TIMINGS = false; // true;

        static Monitor		gmAccessLock  = null;
        static NetLockArray	gmLockArray   = null;

	static final boolean	PRIORITY = TypedProperties.booleanProperty("ibis.net.gm.prioritymutex", true);

	static final int        mtu = TypedProperties.intProperty("ibis.net.gm.mtu", 128 * 1024);

        static final int	packetMTU = 16384; // 4096;

	/**
	 * Reserve this amount of space for the byte buffer that is
	 * appended after each buffered message
	 */
	static final int	byteBufferSize = packetMTU / 2;

        private static final int speculativePolls = 16;

	private static int	interrupts = 0;	// Support poll interrupts

	private final static int POLLS_BEFORE_YIELD = TypedProperties.intProperty("ibis.net.gm.polls", 300);	    

	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws IOException;
	static native void nCloseDevice(long deviceHandler) throws IOException;

        private static native boolean nGmThread();
        private static native void nGmBlockingThread();

	private static native void nStatistics();

	// If (TIMINGS)
	static ibis.util.nativeCode.Rdtsc t_wait_reply = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_wait_service = new ibis.util.nativeCode.Rdtsc();

	static ibis.util.nativeCode.Rdtsc t_poll = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_lock = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_native = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_native_poll = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_native_flush = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_native_post = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_native_send = new ibis.util.nativeCode.Rdtsc();

	static private int	yields;
	static private int	pollers;
	static private int	yielders;


	static {
	    if (System.getProperty("ibis.net.gm.dynamic") != null) {
		Ibis.loadLibrary("gm");
	    }

	    Ibis.loadLibrary("net_ibis_gm");

	    gmAccessLock = new Monitor(PRIORITY);

	    gmLockArray = new NetLockArray(gmAccessLock);
	    gmAccessLock.lock();
	    gmLockArray.initLock(0, false);
	    gmAccessLock.unlock();

	    Runtime.getRuntime().addShutdownHook(new Thread("NetGm Driver ShutdownHook") {
		public void run() {
		    if (Driver.TIMINGS) {
			System.err.println("t_wait_reply   "
			    + t_wait_reply.nrTimes() + " "
			    + t_wait_reply.averageTime() + " "
			    + t_wait_reply.totalTime());
			System.err.println("t_wait_service "
			    + t_wait_service.nrTimes() + " "
			    + t_wait_service.averageTime() + " "
			    + t_wait_service.totalTime());
			System.err.println("t_lock         "
			    + t_lock.nrTimes() + " "
			    + t_lock.averageTime() + " "
			    + t_lock.totalTime());
			System.err.println("t_poll         "
			    + t_poll.nrTimes() + " "
			    + t_poll.averageTime() + " "
			    + t_poll.totalTime());
			System.err.println("t_native       "
			    + Driver.t_native.nrTimes() + " "
			    + Driver.t_native.averageTime() + " "
			    + Driver.t_native.totalTime());
			System.err.println("t_native_poll  "
			    + Driver.t_native_poll.nrTimes() + " "
			    + Driver.t_native_poll.averageTime() + " "
			    + Driver.t_native_poll.totalTime());
			System.err.println("t_native_flush "
			    + Driver.t_native_flush.nrTimes() + " "
			    + Driver.t_native_flush.averageTime() + " "
			    + Driver.t_native_flush.totalTime());
			System.err.println("t_native_post  "
			    + Driver.t_native_post.nrTimes() + " "
			    + Driver.t_native_post.averageTime() + " "
			    + Driver.t_native_post.totalTime());
			System.err.println("t_native_send  "
			    + Driver.t_native_send.nrTimes() + " "
			    + Driver.t_native_send.averageTime() + " "
			    + Driver.t_native_send.totalTime());
		    }
		    nStatistics();
		}
	    });
	}


	private static Hashtable lockIdVerifyTable = new Hashtable();

	static synchronized void verifyUnique(int lockId) {
	    Integer i = new Integer(lockId);
	    if (lockIdVerifyTable.contains(i)) {
		throw new Error("lockId not unique");
	    }
	    lockIdVerifyTable.put(i, i);
	}

	static synchronized String getLockTable() {
	    return lockIdVerifyTable.toString();
	}


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
	    super(ibis);

	    if (! PRIORITY && ibis.closedPoolRank() == 0) {
		System.err.println("No priority mutex in NetGM");
	    }
	}

	/**
	 * Returns the name of the driver.
	 *
	 * @return The driver name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates a new GM input.
	 *
	 * @param pt the input's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new GM input.
	 */
	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall)
		throws IOException {
                //System.err.println("new gm input");
		return new GmPoller(pt, this, context, inputUpcall);
	}

	/**
	 * Creates a new GM output.
	 *
	 * @param pt the output's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new GM output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
                //System.err.println("new gm output");
		return new GmSplitter(pt, this, context);
	}


	private static void lock() {
	    gmAccessLock.lock();
	}


	private static void unlock() {
	    gmAccessLock.unlock();
	}


	protected static void interruptPump(int[] lockIds) throws IOException {
	    if (VERBOSE_INTPT) {
		System.err.println(NetIbis.hostName()
			+ " "
			+ Thread.currentThread()
			+ ":********** perform interrupts[" + lockIds[0] + "] , Driver.interrupts " + (interrupts + 1));
		// Thread.dumpStack();
	    }
	    gmLockArray.interrupt(lockIds);
	    interrupts++;
	}


	protected static void interruptPump(int lockId) throws IOException {
	    if (VERBOSE_INTPT) {
		System.err.println(NetIbis.hostName()
			+ " "
			+ Thread.currentThread()
			+ ":********** perform interrupt[" + lockId + "] , Driver.interrupts " + (interrupts + 1));
		// Thread.dumpStack();
	    }
	    gmLockArray.interrupt(lockId);
	    interrupts++;
	}


	/* Must hold gmAccessLock on entry/exit */
        private static int pump(int lockId, int[] lockIds)
		throws IOException {
	    int result;
	    int interrupts = Driver.interrupts;

	    if (DEBUG) {
		System.err.print(NetIbis.hostName() + " "
				  + Thread.currentThread() + " [b");
		for (int i = 0; i < lockIds.length - 1; i++)
		System.err.print(lockIds[i] + ",");
	    }

	    if (Driver.TIMINGS) {
		t_poll.start();
		t_lock.start();
	    }

	    try {
		result = gmLockArray.ilockFirst(lockIds);
	    } catch (ibis.util.IllegalLockStateException e) {
		System.err.println("On ilockFirst[" + lockIds[0] + "]: catch IllegalLockStateException; Driver.interrupts " + interrupts + " was " + interrupts);
		System.err.println("Presume our channel was closed under our hands");
		throw new ConnectionClosedException(e);
	    }

	    if (Driver.TIMINGS) {
		t_lock.stop();
	    }

	    long start = -1;

	    if (result == lockIds.length - 1) {
pollers++;
		/* got GM main lock, let's pump */
		// We are NOT interested in lockIds[lockIds.length - 1], but
		// luckily we already got that, so no fear that we
		// get it again.
		try {
		    boolean locked;
		    int pollsBeforeYield = POLLS_BEFORE_YIELD;	    

		    do {
// System.err.print(">");
			if (Driver.TIMINGS) Driver.t_native_poll.start();
			nGmThread();
			if (Driver.TIMINGS) Driver.t_native_poll.stop();
// System.err.print("<");

			if (interrupts != Driver.interrupts) {
			    throw new InterruptedIOException("got interrupted, Driver.interrupts " + Driver.interrupts + " was " + interrupts);
			}

			if (lockId == -1) {
			    result = gmLockArray.trylockFirst(lockIds);
			    locked = (result != -1);
			} else {
			    locked = gmLockArray.trylock(lockId);
			}
			if (locked) {
			    break;
			} else if (pollsBeforeYield-- == 0) {
			    yields++;
yielders++;
			    gmAccessLock.unlock();
			    Thread.yield();
			    gmAccessLock.lock(false);
			    pollsBeforeYield = POLLS_BEFORE_YIELD;
yielders--;
			}
		    } while (true);

		} finally {
pollers--;
			/* request completed, release GM main lock */
		    gmLockArray.unlock(0);
		}

	    } else if (result > lockIds.length - 1) {
		throw new Error("invalid state");
	    }
	    /* else: request already completed */
// else System.err.print("A(" + result + ")");

// System.err.println(Thread.currentThread() + ": blockingPump: return " + result);

	    if (Driver.TIMINGS) {
		t_poll.stop();
	    }
	    if (DEBUG) {
		System.err.println("b" + lockIds[result] + "]");
	    }

	    return result;
        }


	/* Must hold gmAccessLock on entry/exit */
        protected static int blockingPump(int[] lockIds)
	    	throws IOException {
	    return pump(-1, lockIds);
	}


	/* Must hold gmAccessLock on entry/exit */
        protected static void blockingPump(int lockId, int[] lockIds)
	    	throws IOException {
	    pump(lockId, lockIds);
	}


	/* Must hold gmAccessLock on entry/exit */
        protected static int tryPump(int []lockIds) throws IOException {
                int result = gmLockArray.trylockFirst(lockIds);

                if (result == lockIds.length - 1) {
                        /* got GM main lock, let's pump */
			// We are NOT interested in lockIds[main], but
			// luckily we already got that, so no fear that we
			// get it again.
			int i = speculativePolls;
			do {
				nGmThread();
				result = gmLockArray.trylockFirst(lockIds);
			} while (result == -1 && --i > 0);

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                        return result;
                } else {
                        return result;
                }
        }


	/* Must hold gmAccessLock on entry/exit */
        protected static boolean tryPump(int lockId, int []lockIds) throws IOException {
                int result = gmLockArray.trylockFirst(lockIds);

                if (result == -1) {
                        return false;
                } else if (result == 0) {
                        return true;
                } else if (result == lockIds.length - 1) {
                        boolean value = false;

                        /* got GM main lock, let's pump */
			int i = speculativePolls;
			do {
				nGmThread();
				value = gmLockArray.trylock(lockId);
			} while (!value && --i > 0);

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                        return value;
                } else {
                        throw new Error("invalid state");
                }
        }


}
