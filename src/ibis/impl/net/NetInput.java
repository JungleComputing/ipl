/* $Id$ */

package ibis.impl.net;

import ibis.io.Conversion;
import ibis.ipl.ConnectionClosedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.SendPortIdentifier;
import ibis.util.TypedProperties;

import java.io.IOException;

/**
 * Provide an abstraction of a network input.
 */
public abstract class NetInput extends NetIO implements NetInputUpcall {
    /**
     * Active {@link ibis.impl.net.NetConnection connection} number or
     * <code>null</code> if no connection is active.
     */
    private Integer activeNum = null;

    private PooledUpcallThread activeThread = null;

    private final static int threadStackSize = 256;

    private int threadStackPtr = 0;

    private PooledUpcallThread[] threadStack
        = new PooledUpcallThread[threadStackSize];

    private NetMutex threadStackLock = new NetMutex(false);

    private int upcallThreadNum = 0;

    private boolean upcallThreadNotStarted = true;

    private boolean receiveStarted = false;

    private NetThreadStat utStat = null;

    private boolean freeCalled = false;

    final private Integer takenNum = new Integer(-1);

    private int pollWaiters;

    /**
     * Upcall interface for incoming messages.
     */
    protected NetInputUpcall upcallFunc = null;

    /**
     * The top level input must spawn a new thread if the thread continues
     * in application space <strong>after</strong> the finish.
     */
    private boolean upcallSpawnMode = true;

    private Object nonSpawnSyncer = new Object();

    private int nonSpawnWaiters;

    private static final boolean VERBOSE_INPUT_EXCEPTION
        = TypedProperties.booleanProperty(NetIbis.input_exc_v, false);

    /**
     * Synchronize between threads if an upcallFunc is installed after
     * this Input has been in use as a downcall receive handler
     */
    private Object upcallInstaller = new Object();

    private int upcallInstallWaiters;

    static private int threadCount = 0;

    static private boolean globalThreadStat = false;
    static {
        if (globalThreadStat) {
            Runtime.getRuntime().addShutdownHook(
                    new Thread("NetInput ShutdownHook") {
                        public void run() {
                            System.err.println("used " + threadCount
                                    + " Upcall threads");
                            System.err.println("current memory values: "
                                    + Runtime.getRuntime().totalMemory() + "/"
                                    + Runtime.getRuntime().freeMemory() + "/"
                                    + Runtime.getRuntime().maxMemory());
                        }
                    });
        }

    }

    private Conversion conversion;

    public final class NetThreadStat extends NetStat {
        private int nb_thread_requested = 0;

        private int nb_thread_allocated = 0;

        private int nb_thread_reused = 0;

        private int nb_thread_discarded = 0;

        private int nb_thread_poll = 0;

        private int nb_max_thread_stack = 0;

        public NetThreadStat(boolean on, String moduleName) {
            super(on, moduleName);

            if (on) {
                pluralExceptions.put("entry", "entries");
            }
        }

        public NetThreadStat(boolean on) {
            this(on, "");
        }

        public void addAllocation() {
            if (on) {
                nb_thread_allocated++;
                nb_thread_requested++;
            }

        }

        public void addReuse() {
            if (on) {

                nb_thread_requested++;
                nb_thread_reused++;
            }

        }

        public void addStore() {
            if (on) {
                if (nb_max_thread_stack < threadStackPtr) {
                    nb_max_thread_stack = threadStackPtr;
                }
            }
        }

        public void addDiscarded() {
            if (on) {
                nb_thread_discarded++;
            }
        }

        public void addPoll() {
            if (on) {
                nb_thread_poll++;
            }
        }

        public void report() {
            if (on) {
                System.err.println();
                System.err.println("Upcall thread allocation stats for module "
                        + moduleName + " " + NetInput.this);
                System.err.println("------------------------------------");

                reportVal(nb_thread_requested, " thread request");
                reportVal(nb_thread_allocated, " thread allocation");
                reportVal(nb_thread_reused, " thread reuse");
                reportVal(nb_thread_discarded, " thread discardal");
                reportVal(nb_thread_poll, " poll");
                reportVal(nb_max_thread_stack, " stack", "entry", "used");
            }
        }
    }

    private int finishedUpcallThreads;

    private final class PooledUpcallThread extends Thread {

        private boolean end = false;

        private NetMutex sleep = new NetMutex(true);

        public PooledUpcallThread(String name) {
            super(NetInput.this + ".PooledUpcallThread[" + (threadCount++)
                    + "]: " + name);
        }

        public void run() {
            log.in();
            while (!end) {

                log.disp("sleeping...");
                try {
                    sleep.ilock();
                    if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
                        if (activeThread != null && activeThread != this) {
                            System.err.println(this
                                    + ": want to become activeThread; "
                                    + " but activeThread is " + activeThread);
                        }
                    }
                    activeThread = this;

                    if (activeNum != null) {
                        System.err.println(NetInput.this + ": "
                                + Thread.currentThread()
                                + ": connection unavailable " + activeNum);
                        throw new Error(Thread.currentThread()
                                + ": connection unavailable: " + activeNum);
                    }
                    if (finishedUpcallThreads > 1) {
                        for (int i = 0; i < finishedUpcallThreads; i++) {
                            NetIbis.yield();
                        }
                    }

                } catch (InterruptedIOException e) {
                    if (VERBOSE_INPUT_EXCEPTION) {
                        System.err.println("Sleeping upcall thread catches "
                                + e);
                    }
                    log.disp("was interrupted...");
                    end = true;
                    return;

                } catch (Throwable e) {
                    System.err.println("Sleeping upcall thread catches " + e);
                    break;
                }

                utStat.addPoll();
                log.disp("just woke up, polling...");
                while (!end) {
                    try {
                        Integer num = doPoll(true);

                        if (num == null) {
                            // the connection was probably closed
                            // let the 'while' test the end flag
                            continue;
                        }

                        activeNum = num;
                        initReceive(activeNum);
                    } catch (ConnectionClosedException e) {
                        if (VERBOSE_INPUT_EXCEPTION) {
                            System.err.println("PooledUpcallThread + doPoll"
                                    + " throws ConnectionClosedException. "
                                    + " Should I quit??? " + e);
                        }
                        end = true;
                        return;
                    } catch (InterruptedIOException e) {
                        if (VERBOSE_INPUT_EXCEPTION) {
                            System.err.println("PooledUpcallThread + doPoll"
                                    + " throws InterruptedIOException; end "
                                    + end + ". Should I quit??? " + e);
                        }
                        if (end) {
                            return;
                        } else {
                            throw new Error(e);
                        }
                    } catch (IOException e) {
                        System.err.println("PooledUpcallThread + doPoll"
                                + " throws IOException. Should I quit??? " + e);
                        // throw new Error(e);
                        return;

                    } catch (Throwable e) {
                        System.err.println(this
                                + ": Polling upcall thread catches " + e
                                + "; end = " + end);
                        e.printStackTrace();
                        break;
                    }

                    try {
                        upcallFunc.inputUpcall(NetInput.this, activeNum);
                    } catch (InterruptedIOException e) {
                        System.err.println("Sleeping upcall thread catches "
                                + e);
                        if (!end) {
                            throw new Error(e);
                        }
                        return;
                    } catch (ConnectionClosedException e) {
                        if (VERBOSE_INPUT_EXCEPTION) {
                            System.err.println(
                                    "PooledUpcallThread.inputUpcall()"
                                    + " throws ConnectionClosedException " + e);
                        }
                        end = true;
                        return;
                    } catch (IOException e) {
                        System.err.println("PooledUpcallThread.inputUpcall()"
                                + " throws IOException. Should I quit??? " + e);
                        e.printStackTrace(System.err);
                        // throw new Error(e);
                        end = true;
                        return;

                    } catch (Throwable e) {
                        System.err.println("Upcall in upcall thread catches "
                                + e);
                        e.printStackTrace(System.err);
                        break;
                    }
                    if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
                        System.err.println(this + ": upcallSpawnMode "
                                + upcallSpawnMode + " activeThread "
                                + activeThread + " this " + this);
                    }

                    if (!upcallSpawnMode) {
                        synchronized (nonSpawnSyncer) {
                            while (activeThread != null) {
                                try {
                                    nonSpawnWaiters++;
                                    nonSpawnSyncer.wait();
                                    nonSpawnWaiters--;
                                } catch (InterruptedException ie) {
                                    // Ignore
                                }
                            }
                        }

                    } else if (activeThread == this) {
                        try {
                            implicitFinish();
                        } catch (Exception e) {
                            System.err.println(
                                    "PooledUpcallThread,implicitFinish()"
                                    + " throws IOException. Should I quit??? "
                                    + e);
                            // throw new Error(e);
                            return;
                        }
                        log.disp("reusing thread");
                        utStat.addReuse();
                        continue;
                    } else {
                        synchronized (this) {
                            finishedUpcallThreads--;
                        }
                        try {
                            threadStackLock.lock();
                        } catch (InterruptedIOException e) {
                            if (VERBOSE_INPUT_EXCEPTION) {
                                System.err.println("PooledUpcallThread throws "
                                        + e);
                            }
                            if (!end) {
                                throw new Error(e);
                            }
                            return;
                        }

                        if (threadStackPtr < threadStackSize) {
                            threadStack[threadStackPtr++] = this;
                            log.disp("storing thread into the stack");
                            utStat.addStore();
                        } else {
                            log.disp("discarding the thread");
                            end = true;
                        }
                        threadStackLock.unlock();

                        break;
                    }
                }
            }
            log.out();
        }

        public void exec() {
            log.in();
            sleep.unlock();
            log.out();
        }

        public void end() {
            log.in();
            synchronized (sleep) {
                end = true;
            }
            interrupt();
            log.out();
        }
    }

    /**
     * Constructor.
     *
     * @param portType the port {@link ibis.impl.net.NetPortType} type.
     * @param driver the driver.
     * @param context the context.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    protected NetInput(NetPortType portType, NetDriver driver, String context,
            NetInputUpcall inputUpcall) {
        super(portType, driver, context);
        // setBufferFactory(new NetBufferFactory(new NetReceiveBufferFactoryDefaultImpl()));
        // Stat object
        String s = "//" + type.name() + this.context + ".input";
        boolean utStatOn = type.getBooleanStringProperty(this.context,
                "UpcallThreadStat", false);
        utStat = new NetThreadStat(utStatOn, s);
        this.upcallFunc = inputUpcall;
        log.disp("this.upcallFunc = ", this.upcallFunc);

        if (type.numbered()) {
            // The default conversion is *much* slower than the other
            // default. Select the other default, i.e. NIO.
            conversion = Conversion.loadConversion(false);
        }
    }

    /**
     * Default incoming message upcall method.
     *
     * Note: this method is only useful for filtering drivers.
     *
     * @param input the {@link ibis.impl.net.NetInput sub-input} that
     * 		generated the upcall.
     * @param num   the active connection number
     * @exception IOException in case of trouble.
     */
    public synchronized void inputUpcall(NetInput input, Integer num)
            throws IOException {
        log.in();
        activeNum = num;
        upcallFunc.inputUpcall(this, num);
        activeNum = null;
        log.out();
    }

    public abstract void initReceive(Integer num) throws IOException;

    protected long readSeqno() throws IOException {
        byte[] buf = new byte[Conversion.LONG_SIZE];
        readArray(buf, 0, buf.length);
        return conversion.byte2long(buf, 0);
    }

    protected void handleEmptyMsg() throws IOException {
        readByte();
    }

    protected void enableUpcallSpawnMode() {
        upcallSpawnMode = true;
    }

    protected void disableUpcallSpawnMode() {
        upcallSpawnMode = false;
    }

    public boolean readBufferedSupported() {
        return false;
    }

    public int readBuffered(byte[] data, int offset, int length)
            throws IOException {
        throw new IOException(
                "read buffered/incomplete byte array not supported");
    }

    /**
     * Test for incoming data.
     *
     * Note: if {@linkplain #poll} is called again immediately
     * after a successful {@linkplain #poll} without extracting the message and
     * {@linkplain #finish finishing} the input, the result is
     * undefined and data might get lost. Use {@link
     * #getActiveSendPortNum} instead.
     *
     * @param blockForMessage indicates whether this method must block until
     *        a message has arrived, or just query the input one.
     * @return the {@link ibis.impl.net.NetConnection connection} identifier
     * 		or <code>null</code> if no data is available.
     * @exception InterruptedIOException if the polling fails (!= the
     * polling is unsuccessful).
     */
    public final Integer poll(boolean blockForMessage) throws IOException {
        log.in();

        synchronized (this) {
            while (activeNum != null) {
                pollWaiters++;
                try {
                    wait();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException(e);
                } finally {
                    pollWaiters--;
                }
            }

            activeNum = takenNum;
        }

        Integer num = doPoll(blockForMessage);

        synchronized (this) {
            if (activeNum.equals(takenNum)) {
                activeNum = num;
            } else {
                // Closing?
                num = null;
            }
        }

        if (num != null) {
            initReceive(num);
        }
        log.out();

        return num;
    }

    /*
     * Do not make this synchronized. The calling <code>poll</code> will
     * ensure concurrency issues. If this is called synchronized and
     * block=ttrue, deadlock may ensue with concurrent accept calls.
     */
    protected abstract Integer doPoll(boolean blockForMessage)
            throws IOException;

    /**
     * Unblockingly test for incoming data.
     *
     * Note: if {@linkplain #poll} is called again immediately
     * after a successful {@linkplain #poll} without extracting the message and
     * {@linkplain #finish finishing} the input, the result is
     * undefined and data might get lost. Use {@link
     * #getActiveSendPortNum} instead.
     *
     * @return the {@link ibis.impl.net.NetConnection connection} identifier
     * 		or <code>null</code> if no data is available.
     * @exception InterruptedIOException if the polling fails (!= the
     * polling is unsuccessful).
     */
    public Integer poll() throws IOException {
        return poll(false);
    }

    /**
     * Return the active {@link ibis.impl.net.NetConnection connection}
     * identifier or <code>null</code> if no {@link
     * ibis.impl.net.NetConnection connection} is active.
     *
     * @return the active {@link ibis.impl.net.NetConnection connection}
     * 		identifier or <code>null</code> if no {@link
     * 		ibis.impl.net.NetConnection connection} is active.
     */
    public final Integer getActiveSendPortNum() {
        return activeNum;
    }

    /**
     * Second phase of the NetInput a 2-phase connection start.
     * First, all data structures are initialized and all properties
     * between inputs (like upcall/downcall mode for {@link NetPoller
     * Pollers} and their subInputs) are negotiated from {@link
     * #setupConnection}. After that, the receiver threads are started.
     */
    public void startReceive() throws IOException {
        startUpcallThread();
    }

    /**
     * Lazy start of a receiver thread for this NetInput if {@link
     * #upcallFunc} is non<code>null</code>.
     */
    protected final void startUpcallThread() throws IOException {
        log.in();
        threadStackLock.lock();
        receiveStarted = true;
        if (upcallFunc != null && upcallThreadNotStarted) {
            if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
                System.err.println(this + ": start UpcallThread; upcallFunc "
                        + upcallFunc);
            }
            verifyNonSingletonPoller();
            upcallThreadNotStarted = false;
            PooledUpcallThread up = new PooledUpcallThread("no "
                    + upcallThreadNum++);
            utStat.addAllocation();
            up.setDaemon(true);
            up.start();
            up.exec();
        }
        threadStackLock.unlock();
        log.out();
    }

    protected void verifyNonSingletonPoller() {
        // No-op, reserve for subclasses
    }

    public NetInputUpcall getUpcallFunc() {
        return upcallFunc;
    }

    protected void installUpcallFunc(NetInputUpcall upcallFunc)
            throws IOException {
        if (upcallFunc != null && this.upcallFunc != null) {
            System.err.println(this + ": try to override upcall "
                    + this.upcallFunc + " with " + upcallFunc);
            throw new IllegalArgumentException("Cannot restart upcall");
        }
        this.upcallFunc = upcallFunc;

        /*
         * Fight race with poll: if an upcallFunc is installed for
         * a NetInput that used to do downcall receives, we must ensure
         * that the downcall poll has finished completely. In that case,
         * activeNum is null.
         */
        synchronized (upcallInstaller) {
            while (activeNum != null) {
                System.err.println("Wait for release the install handler");
                upcallInstallWaiters++;
                try {
                    upcallInstaller.wait();
                } catch (InterruptedException e) {
                    // Ignore
                }
                upcallInstallWaiters--;
                System.err.println("Unwait for release the install handler");
            }
        }

        if (receiveStarted) {
            startUpcallThread();
        }
    }

    /**
     * {@linkplain ibis.impl.net.NetInput NetInput}s may be capable of
     * switching between upcall mode and downcall mode on-the-fly by
     * invoking {@link #switchToUpcallMode} or {@link #switchToDowncallMode}.
     *
     * <BR>
     * The capacity to switch on-the-fly between upcall and downcall mode
     * is equivalent to the capacity to interrupt a blocking poll or
     * receiving upcall thread.
     *
     * <BR>
     * This value <strong>cannot</strong> be cached between calls to
     * {@link #switchToDowncallMode} and {@link #switchToUpcallMode}. E.g.,
     * NetInputs may support only one of the two transitions.
     *
     * @see	#pollIsInterruptible
     * @see	#interruptPoll
     * @see	#switchToDowncallMode
     * @see	#switchToUpcallMode
     *
     * @return whether blocking poll is interruptible, which is equivalent
     * 		to whether this {@linkplain ibis.impl.net.NetInput NetInput}
     * 		supports switching between upcall mode and downcall mode.
     */
    public boolean pollIsInterruptible() throws IOException {
        return false;
    }

    /**
     * Provide a hint that switching between upcall mode and downcall mode
     * is desired or not desired. The rationale for this method is that some
     * {@linkplain ibis.impl.net.NetInput NetInput}s can provide
     * interruptibility but only at an extra cost: <code>tcp_blk</code> is a
     * case in point.
     *
     * @see	#pollIsInterruptible
     * @see	#interruptPoll
     * @see	#switchToDowncallMode
     * @see	#switchToUpcallMode
     *
     * @param  interruptible switch interruptibility on/off
     * @throws {@link IllegalArgumentException} if this {@linkplain
     * 		NetInput} does not actually support poll interrupts.
     */
    public void setInterruptible(boolean interruptible) throws IOException {
        throw new IllegalArgumentException(
                "Upcall/downcall mode switch not supported");
    }

    /**
     * Interrupt threads that block in a blocking poll (in the case of
     * downcall receive) or receive (in the case of an upcall thread).
     * Upon an interruptPoll(), a thread that is blocked in a (blocking)
     * poll returns abnormally by throwing an {@link
     * InterruptedIOException}. A thread that is blocked in a receive
     * should generate an <code>interruptedUpcall</code> -- to be
     * implemented.
     *
     * <BR>
     * Method {@link #pollIsInterruptible} informs whether this {@linkplain
     * ibis.impl.net.NetInput NetInput} supports poll interrupts and switching
     * between upcall mode and downcall mode.
     *
     * <BR>
     * The implementation of this method may be costly.
     *
     * @see	#pollIsInterruptible
     * @see	#interruptPoll
     * @see	#switchToDowncallMode
     * @see	#switchToUpcallMode
     *
     * @throws {@link IllegalArgumentException} if this {@linkplain
     * 		NetInput} does not actually support poll interrupts.
     */
    public void interruptPoll() throws IOException {
        throw new IllegalArgumentException(
                "Upcall/downcall mode switch not supported");
    }

    /**
     * Switch this {@linkplain ibis.impl.net.NetInput NetInput} to downcall
     * mode.
     *
     * Threads that are blocked in a receive must be interrupted before this
     * switch by calling {@link #interruptPoll}. These threads will return
     * abnormally by throwing an {@link InterruptedIOException}.
     *
     * <BR>
     * Any previously registered {@link NetInputUpcall} is cleared and
     * forgotten. The {@linkplain ibis.impl.net.NetInput NetInput} of which
     * this is a subInput is responsible for doing downcall receives from
     * now on.
     *
     * <BR>
     * Method {@link #pollIsInterruptible} informs whether this {@linkplain
     * ibis.impl.net.NetInput NetInput} supports poll interrupts and switching
     * between upcall mode and downcall mode.
     *
     * <BR>
     * The implementation of this method may be costly.
     *
     * @see	#pollIsInterruptible
     * @see	#interruptPoll
     * @see	#switchToDowncallMode
     * @see	#switchToUpcallMode
     *
     * @throws {@link IllegalArgumentException} if this {@linkplain
     * 		NetInput} does not actually support poll interrupts.
     */
    public void switchToDowncallMode() throws IOException {
        throw new IllegalArgumentException(
                "Upcall/downcall mode switch not supported");
    }

    /**
     * Switch this {@linkplain ibis.impl.net.NetInput NetInput} to upcall mode.
     *
     * Threads that are blocked in a receive must be interrupted before this
     * switch by calling {@link #interruptPoll}. These threads will return
     * abnormally by throwing an {@link InterruptedIOException}.
     *
     * <BR>
     * If the caller is certain that no downcall receive may have been
     * posted yet, this method may be called without first interrupting
     * the poll. In that case this {@linkplain ibis.impl.net.NetInput
     * NetInput} need not be {@linkplain #pollIsInterruptible}. This occurs
     * for instance when the poll and the connect are protected by one lock,
     * that is still taken indivisibly with the connect when this method is
     * invoked.
     *
     * <BR>
     * Method {@link #pollIsInterruptible} informs whether this
     * {@linkplain ibis.impl.net.NetInput NetInput} supports poll interrupts
     * and switching between upcall mode and downcall mode.
     *
     * <BR>
     * The implementation of this method may be costly.
     *
     * @see	#pollIsInterruptible
     * @see	#interruptPoll
     * @see	#switchToDowncallMode
     * @see	#switchToUpcallMode
     *
     * @param inputUpcall if this parameter is nonnull, message receipt will
     * 		be done using this upcall after switching off the
     * 		interruptibility has been handled.
     * @throws {@link IllegalArgumentException} if this {@linkplain
     * 		NetInput} does not actually support poll interrupts.
     */
    public void switchToUpcallMode(NetInputUpcall inputUpcall)
            throws IOException {
        installUpcallFunc(inputUpcall);
    }

    /**
     * Utility function to get a {@link ibis.impl.net.NetReceiveBuffer} from our
     * {@link ibis.impl.net.NetBufferFactory}.
     * This is only valid for a Factory with MTU.
     *
     * @param contentsLength indicates how many bytes of data must be received.
     * 0 indicates that any length is fine and that the buffer.length field
     * should be filled with the length actually read.
     * @throws an {@link IllegalArgumentException} if the factory has no
     * 		default MTU
     * @return the new {@link ibis.impl.net.NetReceiveBuffer}.
     */
    public NetReceiveBuffer createReceiveBuffer(int contentsLength) {
        log.in();
        NetReceiveBuffer b = (NetReceiveBuffer) createBuffer();
        b.length = contentsLength;
        log.out();
        return b;
    }

    /**
     * Utility function to get a {@link ibis.impl.net.NetReceiveBuffer} from our
     * {@link ibis.impl.net.NetBufferFactory}.
     *
     * @param length the length of the data stored in the buffer
     * @param contentsLength indicates how many bytes of data must be received.
     * 0 indicates that any length is fine and that the buffer.length field
     * should be filled with the length actually read.
     * @return the new {@link ibis.impl.net.NetReceiveBuffer}.
     */
    public NetReceiveBuffer createReceiveBuffer(int length,
            int contentsLength) {
        log.in();
        NetReceiveBuffer b = (NetReceiveBuffer) createBuffer(length);
        b.length = contentsLength;
        log.out();
        return b;
    }

    public final void close(Integer num) throws IOException {
        log.in();
        synchronized (this) {
            doClose(num);
            if (activeNum == num) {
                activeNum = null;
                notifyAll();

            }
        }

        threadStackLock.lock();
        if (activeThread != null) {
            ((PooledUpcallThread) activeThread).end();
            activeThread = null;
        }

        while (threadStackPtr > 0) {
            threadStack[--threadStackPtr].end();
        }
        upcallThreadNotStarted = true;
        threadStackLock.unlock();
        log.out();
    }

    protected abstract void doClose(Integer num) throws IOException;

    /*
     * Closes the I/O.
     *
     * Note: methods redefining this one should also call it, just in case
     *       we need to add something here
     * @exception IOException if this operation fails.
     */
    public void free() throws IOException {
        log.in();
        trace.in("this = ", this);
        freeCalled = true;
        doFree();
        activeNum = null;

        threadStackLock.lock();
        if (activeThread != null) {
            trace.disp(this, ": active thread end --> ",
                    activeThread.getName());
            ((PooledUpcallThread) activeThread).end();
            while (true) {
                try {
                    ((PooledUpcallThread) activeThread).join();
                    activeThread = null;
                    break;
                } catch (InterruptedException e) {
                    //
                }
            }
            trace.disp(this, ": active thread end<--");
        }

        for (int i = 0; i < threadStackSize; i++) {
            if (threadStack[i] != null) {
                trace.disp(this, ": end--> thread stack@", i);
                threadStack[i].end();
                while (true) {
                    try {
                        threadStack[i].join();
                        threadStack[i] = null;
                        break;
                    } catch (InterruptedException e) {
                        //
                    }
                }
                trace.disp(this, ": <-- thread stack@", i);
            }
        }
        threadStackLock.unlock();

        super.free();
        trace.out("this = " + this);
        log.out();
    }

    protected abstract void doFree() throws IOException;

    protected void finalize() throws Throwable {
        log.in();
        free();
        super.finalize();
        log.out();
    }

    private final void implicitFinish() throws IOException {
        log.in();

        doFinish();

        synchronized (this) {
            activeNum = null;
            if (pollWaiters > 0) {
                notify();
                // notifyAll();
            }
        }
        log.out();
    }

    /**
     * Complete the current incoming message extraction.
     *
     * Only one message is alive at one time for a given
     * receiveport. This is done to prevent flow control
     * problems. when a message is alive, and a new messages is
     * requested with a receive, the requester is blocked until
     * the live message is finished.
     *
     * @exception IOException in case of trouble.
     */
    public long finish() throws IOException {
        log.in();

        implicitFinish();

        if (!upcallSpawnMode) {
            synchronized (nonSpawnSyncer) {
                activeThread = null;
                if (nonSpawnWaiters > 0) {
                    nonSpawnSyncer.notify();
                    // nonSpawnSyncer.notifyAll();
                }
            }
        } else if (activeThread != null) {
            synchronized (this) {
                finishedUpcallThreads++;
            }
            PooledUpcallThread ut = null;

            threadStackLock.lock();
            if (threadStackPtr > 0) {
                ut = threadStack[--threadStackPtr];
                utStat.addReuse();
            } else {
                ut = new PooledUpcallThread("no " + upcallThreadNum++);
                if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
                    System.err.println(this
                                + ": msg.finish creates another"
                                + " PooledUpcallThread " + ut + " "
                                + threadStackPtr + "; finishedUpcallThreads "
                                + finishedUpcallThreads);
                }
                ut.start();
                utStat.addAllocation();
            }
            threadStackLock.unlock();

            activeThread = ut;

            if (ut != null) {
                ut.exec();
            }
        }
        log.out();

        // TODO: return size of message.
        return 0;
    }

    public void finish(IOException e) {
        // What to do here? Rutger?
        try {
            finish();
        } catch (IOException e2) {
            // Give up
        }
    }

    protected abstract void doFinish() throws IOException;

    public long sequenceNumber() {
        return -1;
    }

    /**
     * Unimplemented.
     *
     * @return <code>null</code>.
     */
    public SendPortIdentifier origin() {
        return null;
    }

    public ibis.ipl.ReceivePort localPort() {
        // what the @#@ should we do here --Rob
        throw new ibis.ipl.IbisError("AAAAA");
    }

    /**
     * Atomic packet read function.
     *
     * @param expectedLength a hint about how many bytes are expected.
     * @exception IOException in case of trouble.
     */
    public NetReceiveBuffer readByteBuffer(int expectedLength)
            throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readByteBuffer");
    }

    /**
     * Atomic packet read function.
     *
     * @param buffer the buffer to fill.
     * @exception IOException in case of trouble.
     */
    public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readByteBuffer");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public boolean readBoolean() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readBoolean");
    }

    /**
     * Extract a byte from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public abstract byte readByte() throws IOException;

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public char readChar() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readChar");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public short readShort() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readShort");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public int readInt() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readInt");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public long readLong() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readLong");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public float readFloat() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readFloat");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public double readDouble() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readDouble");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public String readString() throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readString");
    }

    /**
     * Extract an element from the current message.
     *
     * @exception IOException in case of trouble.
     */
    public Object readObject() throws IOException, ClassNotFoundException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readObject");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(boolean[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(boolean[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(byte[] b, int o, int l) throws IOException {
        for (int i = o; i < (o + l); i++) {
            b[i] = readByte();
        }
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(char[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(char[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(short[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(short[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(int[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(int[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(long[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(long[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(float[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(float[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(double[] b, int o, int l) throws IOException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(double[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     * @param o the offset.
     * @param l the number of elements to extract.
     *
     * @exception IOException in case of trouble.
     */
    public void readArray(Object[] b, int o, int l) throws IOException,
            ClassNotFoundException {
        throw new IbisConfigurationException("NetIbis driver \"" + context
                + "\" does not support readArray(Object[])");
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(boolean[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(byte[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(char[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(short[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(int[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(long[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(float[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(double[] b) throws IOException {
        readArray(b, 0, b.length);
    }

    /**
     * Extract some elements from the current message.
     *
     * @param b the array.
     *
     * @exception IOException in case of trouble.
     */
    public final void readArray(Object[] b) throws IOException,
            ClassNotFoundException {
        readArray(b, 0, b.length);
    }

}
