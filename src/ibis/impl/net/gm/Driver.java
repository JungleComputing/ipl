package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

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

        private static final int speculativePolls = 16;

	/**
	 * The driver name.
	 */
	private final String name = "gm";

	static native long nInitDevice(int deviceNum) throws NetIbisException;
	static native void nCloseDevice(long deviceHandler) throws NetIbisException;
        static native void nGmThread();
        static native void nGmBlockingThread();

	static {
		System.loadLibrary("net_ibis_gm");
                gmReceiveLock = new NetMutex(false);
                gmAccessLock = new NetPriorityMutex(false);
                gmLockArray = new NetLockArray();
                gmLockArray.initLock(0, false);
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
		throws NetIbisException {

		return new GmInput(pt, this, context);
	}

	/**
	 * Creates a new GM output.
	 *
	 * @param sp the properties of the output's
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @return The new GM output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws NetIbisException {
		return new GmOutput(pt, this, context);
	}


        protected void blockingPump(int lockId, int []lockIds) throws NetIbisException {
                int result = gmLockArray.lockFirst(lockIds);
                if (result == 1) {
                        /* got GM main lock, let's pump */
                        do {
                                gmAccessLock.lock(false);
                                nGmThread();
                                gmAccessLock.unlock(false);
                        } while (!gmLockArray.trylock(lockId));

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                } /* else: request already completed */
                else if (result != 0) {
                        throw new Error("invalid state");
                }
        }

        protected void pump(int lockId, int []lockIds) throws NetIbisException {
                int result = gmLockArray.lockFirst(lockIds);
                if (result == 1) {
                        /* got GM main lock, let's pump */
                        gmAccessLock.lock(false);
                        nGmThread();
                        gmAccessLock.unlock(false);
                        if (!gmLockArray.trylock(lockId)) {
                                int i = speculativePolls;

                                do {
                                        // WARNING: yield
                                        if (--i == 0) {
                                                (Thread.currentThread()).yield();
                                                i = speculativePolls;
                                        }

                                        gmAccessLock.lock(false);
                                        nGmThread();
                                        gmAccessLock.unlock(false);
                                } while (!gmLockArray.trylock(lockId));
                        }

                        /* request completed, release GM main lock */
                        gmLockArray.unlock(0);

                } /* else: request already completed */
                else if (result != 0) {
                        throw new Error("invalid state");
                }
        }

        protected boolean tryPump(int lockId, int []lockIds) throws NetIbisException {
                int result = gmLockArray.trylockFirst(lockIds);
                if (result == -1) {
                        return false;
                } else if (result == 0) {
                        return true;
                } else if (result == 1) {
                        boolean value = false;

                        /* got GM main lock, let's pump */
                        if (gmAccessLock.trylock(false)) {
                                int i = speculativePolls;
                                do {
                                        nGmThread();
                                        value = gmLockArray.trylock(lockId);
                                } while (!value && --i > 0);

                                gmAccessLock.unlock(false);
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
