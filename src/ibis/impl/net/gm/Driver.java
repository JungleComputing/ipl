package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import java.io.IOException;
import java.io.InterruptedIOException;

import ibis.ipl.Ibis;

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

        private static final int speculativePolls = 16;

	private boolean		interrupted;	// Support poll interrupts


	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws IOException;
	static native void nCloseDevice(long deviceHandler) throws IOException;
        static native boolean nGmThread();
        static native void nGmBlockingThread();

	// static ibis.util.nativeCode.Rdtsc t_wait_reply = new ibis.util.nativeCode.Rdtsc();
	// static ibis.util.nativeCode.Rdtsc t_wait_service = new ibis.util.nativeCode.Rdtsc();

	// static ibis.util.nativeCode.Rdtsc t_poll = new ibis.util.nativeCode.Rdtsc();
	// static ibis.util.nativeCode.Rdtsc t_lock = new ibis.util.nativeCode.Rdtsc();

	static {
                if (System.getProperty("ibis.net.gm.dynamic") != null) {
                        System.loadLibrary("gm");
                }

		System.loadLibrary("net_ibis_gm");
                gmReceiveLock = new NetMutex(false);

                gmAccessLock = new NetPriorityMutex(false);

                gmLockArray = new NetLockArray();
                gmLockArray.initLock(0, false);

		Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
			// System.err.println("t_wait_reply   " + t_wait_reply.nrTimes() + " " + t_wait_reply.averageTime());
			// System.err.println("t_wait_service " + t_wait_service.nrTimes() + " " + t_wait_service.averageTime());
			// System.err.println("t_poll         " + t_lock.nrTimes() + " " + t_poll.averageTime());
			// System.err.println("t_lock         " + t_lock.nrTimes() + " " + t_lock.averageTime());
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
	 * @param sp the properties of the input's
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
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
	 * @param sp the properties of the output's
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @return The new GM output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
                //System.err.println("new gm output");
		return new GmSplitter(pt, this, context);
	}


	protected void interruptPump() throws IOException {
System.err.println("********** interrupted");
		interrupted = true;
	}


        protected int blockingPump(int []lockIds) throws IOException {
                int result;
		int[] origLockIds = lockIds;

		// t_poll.start();
		// t_lock.start();

		result = gmLockArray.ilockFirst(lockIds);
if (false) {
    System.err.print(Thread.currentThread() + ": " + this + ": blockingPump: ilockFirst -> " + result + " [");
    for (int i = 0; i < lockIds.length; i++) {
	System.err.print(lockIds[i] + ",");
    }
    System.err.println("] main=" + (lockIds.length - 1));
    Thread.dumpStack();
}

		// t_lock.stop();

                if (result == lockIds.length - 1) {
                        /* got GM main lock, let's pump */
			// We are NOT interested in lockIds[lockIds.length - 1], but
			// luckily we already got that, so no fear that we
			// get it again.
			try {
				do {
                                        gmAccessLock.ilock(false);

// System.err.print(">");
					nGmThread();
// System.err.print("<");
					gmAccessLock.unlock();

					if (interrupted) {
					    interrupted = false;
					    throw new InterruptedIOException("got interrupted");
					}

					result = gmLockArray.trylockFirst(lockIds);
// if (result != -1)
// System.err.println("blockingPump: trylockFirst -> " + result);
					if (result == lockIds.length - 1) {
					    throw new Error("invalid trylock return");
					}
					if (result == -1) {
						Thread.yield();
					}
				} while (result == -1);
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

		// t_poll.stop();

		return result;
        }


        protected void blockingPump(int lockId, int []lockIds) throws IOException {
                int result = 0;

		result = gmLockArray.ilockFirst(lockIds);

                if (result == lockIds.length - 1) {
if (lockIds[result] != 0) {
    System.err.println("BAAAAAAAAAAAAAAAAAAAAAA");
}
			try {
				/* got GM main lock, let's pump */
				do {
					try {
						gmAccessLock.ilock(false);
						nGmThread();
					} finally {
						gmAccessLock.unlock();
					}

					if (interrupted) {
					    interrupted = false;
					    throw new InterruptedIOException("got interrupted");
					}
				} while (!gmLockArray.trylock(lockId));

			} finally {
				/* request completed, release GM main lock */
				gmLockArray.unlock(0);
			}

                } else if (result > lockIds.length - 1) {
                        throw new Error("invalid state");
                } /* else: request already completed */
        }

        protected void pump(int lockId, int []lockIds) throws IOException {
                int result = 0;

		result = gmLockArray.ilockFirst(lockIds);

                if (result == lockIds.length - 1) {
if (lockIds[result] != 0) {
    System.err.println("BAAAAAAAAAAAAAAAAAAAAAA");
}
                        /* got GM main lock, let's pump */
                        try {
				gmAccessLock.ilock(false);

				nGmThread();
				gmAccessLock.unlock();

				if (interrupted) {
				    interrupted = false;
				    throw new InterruptedIOException("got interrupted");
				}

				if (!gmLockArray.trylock(lockId)) {
					int i = speculativePolls;

					do {
						// WARNING: yield
						if (--i == 0) {
							(Thread.currentThread()).yield();
							i = speculativePolls;
						}

						gmAccessLock.ilock(false);

						nGmThread();
						gmAccessLock.unlock();
					} while (!gmLockArray.trylock(lockId));
				}

			} finally {
				/* request completed, release GM main lock */
				gmLockArray.unlock(0);
			}

                } /* else: request already completed */
                else if (result != 0) {
                        throw new Error("invalid state");
                }
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
