/* $Id$ */

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
import ibis.util.Timer;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;

/**
 * The NetIbis GM driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

    static final String prefix = "ibis.net.gm.";

    static final String gm_mtu = prefix + "mtu";

    static final String gm_verbose = prefix + "intr.verbose";

    static final String gm_prio = prefix + "prioritymutex";

    static final String gm_polls = prefix + "polls";

    static final String gm_dynamic = prefix + "dynamic";

    static final String gm_debug = prefix + "debug";

    static final String gm_rndzv = prefix + "rendez-vous";

    private static final String[] properties = { gm_mtu, gm_verbose, gm_prio,
            gm_polls, gm_dynamic, gm_debug, gm_rndzv };

    // Native functions
    //private static native void gm_init();
    //private static native void gm_exit();

    static final boolean DEBUG = TypedProperties.booleanProperty(gm_debug,
            false);

    static final boolean VERBOSE_INTPT = DEBUG
            || TypedProperties.booleanProperty(gm_verbose, false);

    static final boolean TIMINGS = false;
    // static final boolean TIMINGS = true;

    static Monitor gmAccessLock = null;

    static NetLockArray gmLockArray = null;

    static final boolean PRIORITY = TypedProperties.booleanProperty(gm_prio,
            true);

    static final int mtu = TypedProperties.intProperty(gm_mtu, 128 * 1024);

    static final int packetMTU = TypedProperties.intProperty(gm_rndzv, 16384); // 4096;

    /**
     * Reserve this amount of space for the byte buffer that is
     * appended after each buffered message
     */
    static final int byteBufferSize = packetMTU / 2;

    private static final int speculativePolls = 16;

    private static int interrupts = 0; // Support poll interrupts

    private final static int POLLS_BEFORE_YIELD = TypedProperties.intProperty(
            gm_polls, 300);

    /**
     * The driver name.
     */
    private final String name = "gm";

    private static native void nInitGM() throws IOException;

    static native long nInitDevice(int deviceNum) throws IOException;

    static native void nCloseDevice(long deviceHandler) throws IOException;

    private static native boolean nGmThread();

    private static native void nGmBlockingThread();

    private static native void nStatistics();

    // If (TIMINGS)
    static Timer t_wait_reply = Timer.createTimer();

    static Timer t_wait_service = Timer.createTimer();

    static Timer t_poll = Timer.createTimer();

    static Timer t_lock = Timer.createTimer();

    static Timer t_native = Timer.createTimer();

    static Timer t_native_poll = Timer.createTimer();

    static Timer t_native_flush = Timer.createTimer();

    static Timer t_native_post = Timer.createTimer();

    static Timer t_native_send = Timer.createTimer();

    static private int yields;

    static private int pollers;

    static private int yielders;

    static {
        TypedProperties.checkProperties(prefix, properties, null);
        if (System.getProperty(gm_dynamic) != null) {
            Ibis.loadLibrary("gm");
        }

        Ibis.loadLibrary("net_ibis_gm");

        gmAccessLock = new Monitor(PRIORITY);

        gmLockArray = new NetLockArray(gmAccessLock);
        gmAccessLock.lock();
        gmLockArray.initLock(0, false);
        gmAccessLock.unlock();

        try {
            nInitGM();
        } catch (IOException e) {
            throw new Error("Could not initialise GM " + e);
        }

        Thread timesliceWatchDog = new TimesliceWatchDog();
        timesliceWatchDog.setDaemon(true);
        timesliceWatchDog.setName("Net.GM Driver time slice watchdog");
        timesliceWatchDog.start();

        Runtime.getRuntime().addShutdownHook(
                new Thread("NetGm Driver ShutdownHook") {
                    public void run() {
                        printStats();
                    }
                });
    }

    private static class TimesliceWatchDog extends Thread {

        public void run() {
            while (true) {
                if (gmAccessLock.tryLock()) {
                    nGmThread();
                    gmAccessLock.unlock();
                }
                try {
                    // 15 ms because that's close to a usual CPU time slice
                    Thread.sleep(15);
                } catch (InterruptedException e) {
                    // No idea what to do
                }
            }
        }

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
     * @param ibis the {@link ibis.impl.net.NetIbis} instance.
     */
    public Driver(NetIbis ibis) {
        super(ibis);

        if (!PRIORITY && ibis.closedPoolRank() == 0) {
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
    public NetInput newInput(NetPortType pt, String context,
            NetInputUpcall inputUpcall) throws IOException {
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

    /**
     * Called from native code. Do not discard.
     */
    private static void lock() {
        gmAccessLock.lock();
    }

    /**
     * Called from native code. Do not discard.
     */
    private static void unlock() {
        gmAccessLock.unlock();
    }

    protected static void interruptPump(int[] lockIds) throws IOException {
        if (VERBOSE_INTPT) {
            System.err.println(NetIbis.hostName() + " "
                    + Thread.currentThread()
                    + ":********** perform interrupts[" + lockIds[0]
                    + "] , Driver.interrupts " + (interrupts + 1));
            // Thread.dumpStack();
        }
        gmLockArray.interrupt(lockIds);
        interrupts++;
    }

    protected static void interruptPump(int lockId) throws IOException {
        if (VERBOSE_INTPT) {
            System.err.println(NetIbis.hostName() + " "
                    + Thread.currentThread() + ":********** perform interrupt["
                    + lockId + "] , Driver.interrupts " + (interrupts + 1));
            // Thread.dumpStack();
        }
        gmLockArray.interrupt(lockId);
        interrupts++;
    }

    /* Must hold gmAccessLock on entry/exit */
    private static int pump(int lockId, int[] lockIds) throws IOException {
        int result;
        int interrupts = Driver.interrupts;

        if (DEBUG) {
            System.err.print(NetIbis.hostName() + " " + Thread.currentThread()
                    + " [b");
            for (int i = 0; i < lockIds.length - 1; i++)
                System.err.print(lockIds[i] + ",");
        }

        try {
            if (Driver.TIMINGS) {
                t_lock.start();
            }
            try {
                result = gmLockArray.ilockFirst(lockIds);
            } finally {
                if (Driver.TIMINGS) {
                    t_lock.stop();
                }
            }

            if (result == lockIds.length - 1) {
                if (Driver.TIMINGS) {
                    t_poll.start();
                }

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
                        if (Driver.TIMINGS) {
                            t_native_poll.start();
                        }
                        nGmThread();
                        if (Driver.TIMINGS) {
                            t_native_poll.stop();
                        }
                        // System.err.print("<");

                        if (interrupts != Driver.interrupts) {
                            throw new InterruptedIOException(
                                    "got interrupted, Driver.interrupts "
                                            + Driver.interrupts + " was "
                                            + interrupts);
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
                            if (Driver.TIMINGS) {
                                t_poll.stop();
                            }
                            gmAccessLock.unlock();
                            NetIbis.yield();
                            gmAccessLock.lock(false);
                            if (Driver.TIMINGS) {
                                t_poll.start();
                            }
                            pollsBeforeYield = POLLS_BEFORE_YIELD;
                            yielders--;
                        }
                    } while (true);

                } finally {
                    pollers--;
                    if (Driver.TIMINGS) {
                        t_poll.stop();
                    }

                    /* request completed, release GM main lock */
                    gmLockArray.unlock(0);
                }

            } else if (result > lockIds.length - 1) {
                throw new Error("invalid state");
            }
            /* else: request already completed */
            // else System.err.print("A(" + result + ")");
            // System.err.println(Thread.currentThread() + ": blockingPump: return " + result);
        } catch (ibis.util.IllegalLockStateException e) {
            if (DEBUG) {
                System.err.println("catch IllegalLockStateException;"
                        + " Driver.interrupts " + interrupts
                        + " was " + interrupts);
                System.err.println("Presume our channel was closed under"
                        + " our hands");
            }
            throw new ConnectionClosedException(e);
        }

        if (DEBUG) {
            System.err.println("b" + lockIds[result] + "]");
        }

        return result;
    }

    /* Must hold gmAccessLock on entry/exit */
    protected static int blockingPump(int[] lockIds) throws IOException {
        return pump(-1, lockIds);
    }

    /* Must hold gmAccessLock on entry/exit */
    protected static void blockingPump(int lockId, int[] lockIds)
            throws IOException {
        pump(lockId, lockIds);
    }

    /* Must hold gmAccessLock on entry/exit */
    protected static int tryPump(int[] lockIds) throws IOException {
        try {
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
        } catch (ibis.util.IllegalLockStateException e) {
            if (DEBUG) {
                System.err.println("catch IllegalLockStateException;"
                        + " Driver.interrupts " + interrupts
                        + " was " + interrupts);
                System.err.println("Presume our channel was closed under"
                        + " our hands");
            }
            throw new ConnectionClosedException(e);
        }
    }

    /* Must hold gmAccessLock on entry/exit */
    protected static boolean tryPump(int lockId, int[] lockIds)
            throws IOException {
        try {
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
        } catch (ibis.util.IllegalLockStateException e) {
            if (DEBUG) {
                System.err.println("catch IllegalLockStateException;"
                        + " Driver.interrupts " + interrupts
                        + " was " + interrupts);
                System.err.println("Presume our channel was closed"
                        + " under our hands");
            }
            throw new ConnectionClosedException(e);
        }
    }

    public static void printStats()
    {
        PrintStream out = System.out;

        if (Driver.TIMINGS) {
            out.println("t_wait_reply   " + t_wait_reply.nrTimes() +
                        " " + t_wait_reply.averageTime() +
                        " " + t_wait_reply.totalTime());
            out.println("t_wait_service " + t_wait_service.nrTimes() +
                        " " + t_wait_service.averageTime() +
                        " " + t_wait_service.totalTime());
            out.println("t_lock         " + t_lock.nrTimes() +
                        " " + t_lock.averageTime() +
                        " " + t_lock.totalTime());
            out.println("t_poll         " + t_poll.nrTimes() +
                        " " + t_poll.averageTime() +
                        " " + t_poll.totalTime());
            out.println("t_native       " + t_native.nrTimes() +
                        " " + t_native.averageTime() +
                        " " + t_native.totalTime());
            out.println("t_native_poll  " + t_native_poll.nrTimes() +
                        " " + t_native_poll.averageTime() +
                        " " + t_native_poll.totalTime());
            out.println("t_native_flush " + t_native_flush.nrTimes() +
                        " " + t_native_flush.averageTime() +
                        " " + t_native_flush.totalTime());
            out.println("t_native_post  " + t_native_post.nrTimes() +
                        " " + t_native_post.averageTime() +
                        " " + t_native_post.totalTime());
            out.println("t_native_send  " + t_native_send.nrTimes() +
                        " " + t_native_send.averageTime() +
                        " " + t_native_send.totalTime());
        }

        nStatistics();
    }
}
