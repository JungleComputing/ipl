package ibis.impl.net.gm;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetLockArray;
import ibis.impl.net.NetMutex;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetPriorityMutex;
import ibis.impl.net.InterruptedIOException;
import ibis.ipl.Ibis;

import java.io.IOException;

/**
 * The NetIbis GM driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	// Native functions
	//private static native void gm_init();
	//private static native void gm_exit();

        static NetMutex         gmReceiveLock = null;
        static NetPriorityMutex gmAccessLock  = null;
        static NetLockArray     gmLockArray   = null;

	static final int        mtu = 2*1024*1024;

        static final int	packetMTU = 16384; // 4096;

	/**
	 * Reserve this amount of space for the byte buffer that is
	 * appended after each buffered message
	 */
	static final int	byteBufferSize = packetMTU / 2;

        private static final int speculativePolls = 16;

	private boolean		interrupted;	// Support poll interrupts


	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws IOException;
	static native void nCloseDevice(long deviceHandler) throws IOException;

        private static native boolean nGmThread();
        private static native void nGmBlockingThread();

	private static native void nStatistics();

	static final boolean TIMINGS = false;

	static ibis.util.nativeCode.Rdtsc t_wait_reply = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_wait_service = new ibis.util.nativeCode.Rdtsc();

	static ibis.util.nativeCode.Rdtsc t_poll = new ibis.util.nativeCode.Rdtsc();
	static ibis.util.nativeCode.Rdtsc t_lock = new ibis.util.nativeCode.Rdtsc();

	static {
                if (System.getProperty("ibis.net.gm.dynamic") != null) {
                        Ibis.loadLibrary("gm");
                }

		Ibis.loadLibrary("net_ibis_gm");
                gmReceiveLock = new NetMutex(false);

                gmAccessLock = new NetPriorityMutex(false);

                gmLockArray = new NetLockArray();
                gmLockArray.initLock(0, false);

		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
			if (TIMINGS) {
			    System.err.println("t_wait_reply   "
				+ t_wait_reply.nrTimes() + " "
				+ t_wait_reply.averageTime());
			    System.err.println("t_wait_service "
				+ t_wait_service.nrTimes() + " "
				+ t_wait_service.averageTime());
			    System.err.println("t_poll         "
				+ t_lock.nrTimes() + " "
				+ t_poll.averageTime());
			    System.err.println("t_lock         "
				+ t_lock.nrTimes() + " "
				+ t_lock.averageTime());
			}
			nStatistics();
		    }
		});
	}


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
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
	public NetInput newInput(NetPortType pt, String context)
		throws IOException {
                //System.err.println("new gm input");
		return new GmPoller(pt, this, context);
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


	protected void interruptPump() throws IOException {
System.err.println("********** perform interrupt");
	    interrupted = true;
	}


        private int pump(int lockId, int []lockIds) throws IOException {
	    int result;
// System.err.print("[b");
// for (int i = 0; i < lockIds.length - 1; i++)
// System.err.print(lockIds[i] + ",");

	    if (TIMINGS) {
		t_poll.start();
		t_lock.start();
	    }

	    result = gmLockArray.ilockFirst(lockIds);

	    if (TIMINGS) {
		t_lock.stop();
	    }

	    if (result == lockIds.length - 1) {
		/* got GM main lock, let's pump */
		// We are NOT interested in lockIds[lockIds.length - 1], but
		// luckily we already got that, so no fear that we
		// get it again.
		try {
		    boolean locked;
		    do {
			gmAccessLock.lock(false);
// System.err.print(">");
			nGmThread();
// System.err.print("<");
			gmAccessLock.unlock();

			if (interrupted) {
			    interrupted = false;
			    throw new InterruptedIOException("got interrupted");
			}

			if (lockId == -1) {
			    result = gmLockArray.trylockFirst(lockIds);
			    locked = (result != -1);
			} else {
			    locked = gmLockArray.trylock(lockId);
			}
			if (locked) {
			    break;
			} else {
			    Thread.yield();
			}
		    } while (true);
		} finally {
			/* request completed, release GM main lock */
		    gmLockArray.unlock(0);
		}

	    } else if (result > lockIds.length - 1) {
		throw new Error("invalid state");
	    }
	    /* else: request already completed */
// else System.err.print("A(" + result + ")");

// System.err.println(Thread.currentThread() + ": blockingPump: return " + result);

	    if (TIMINGS) {
		t_poll.stop();
	    }
// System.err.print("b" + lockIds[result] + "]");

	    return result;
        }


        protected int blockingPump(int []lockIds) throws IOException {
	    return pump(-1, lockIds);
	}


        protected void blockingPump(int lockId, int []lockIds) throws IOException {
	    pump(lockId, lockIds);
	}


        protected int tryPump(int []lockIds) throws IOException {
                int result = gmLockArray.trylockFirst(lockIds);

                if (result == lockIds.length - 1) {
                        /* got GM main lock, let's pump */
			// We are NOT interested in lockIds[main], but
			// luckily we already got that, so no fear that we
			// get it again.
                        if (gmAccessLock.trylock(false)) {
                                int i = speculativePolls;
                                do {
                                        nGmThread();
                                        result = gmLockArray.trylockFirst(lockIds);
                                } while (result == -1 && --i > 0);

                                gmAccessLock.unlock();
                        } else {
                                result = gmLockArray.trylockFirst(lockIds);
                        }

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                        return result;
                } else {
                        return result;
                }
        }


        protected boolean tryPump(int lockId, int []lockIds) throws IOException {
                int result = gmLockArray.trylockFirst(lockIds);

                if (result == -1) {
                        return false;
                } else if (result == 0) {
                        return true;
                } else if (result == lockIds.length - 1) {
                        boolean value = false;

                        /* got GM main lock, let's pump */
                        if (gmAccessLock.trylock(false)) {
                                int i = speculativePolls;
                                do {
                                        nGmThread();
                                        value = gmLockArray.trylock(lockId);
                                } while (!value && --i > 0);

                                gmAccessLock.unlock();
                        } else {
                                value = gmLockArray.trylock(lockId);
                        }

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                        return value;
                } else {
                        throw new Error("invalid state");
                }
        }


}
