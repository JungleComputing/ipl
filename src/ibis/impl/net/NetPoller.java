package ibis.impl.net;

import ibis.ipl.ConnectionClosedException;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Provides a generic multiple network input poller.
 */
public class NetPoller
	extends NetInput
	implements NetBufferedInputSupport {

    /**
     * Verbosity at manipulation of dynamic singleton state
     */
    private static final boolean VERBOSE_SINGLETON = TypedProperties.booleanProperty(NetIbis.poll_single_v, false);

    /**
     * The set of inputs.
     */
    protected HashMap inputMap  = null;


    /**
     * Important optimization for the downcall case:
     * If there is only one current connection, we do not need an
     * upcall thread from each subInput and a queue structure for
     * the downcall thread to block in: the downcall thread can
     * call subInput.poll(block=true).
     * This is a rather complicated optimization because disabling
     * it (when a second connection comes by) breaks a lot of the
     * configuration:
     *  - the subInput must switch to upcall receives
     *  - any blocked polls must be interrupted
     *  - any interrupted polls must be caught and restarted in the
     *    new regime
     */
    private ReceiveQueue	singleton;
    private boolean		doingPoll;
    protected int		waitingConnections;
    private static final boolean SINGLETON_FASTPATH = TypedProperties.booleanProperty(NetIbis.poll_single_dyn, true);

    /**
     * The driver used for the inputs.
     */
    protected NetDriver		subDriver   = null;

    /**
     * The input queue that was last sucessfully polled, or <code>null</code>.
     */
    protected ReceiveQueue	activeQueue = null;
    protected Thread		activeUpcallThread = null;

    private int			upcallWaiters;

    /**
     * Count the number of application threads that are blocked in a poll
     */
    protected int		waitingThreads = 0;

    /**
     * Support for interrupting poller threads.
     */
    private boolean		interrupted = false;


    /**
     * The first queue that should be polled first next time we have to poll the queues.
     */
    private int			firstToPoll  = 0;

    /**
     * In the downcall case, this module usually starts an upcall thread
     * in each subInput. If the subInput is a multiplexer, it only
     * requires one thread in the multiplexer = one thread per ReceivePort.
     * So, this driver should not start a thread by itself, but depend
     * on the multiplexer thread to perform the upcalls.
     * To switch on this behaviour, set decouplePoller = false.
     */
    protected final boolean	decouplePoller;

    protected boolean		readBufferedSupported = true;

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public NetPoller(NetPortType pt,
		     NetDriver driver,
		     String context,
		     NetInputUpcall inputUpcall)
	    throws IOException {
	this(pt, driver, context, true, inputUpcall);
    }

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param decouplePoller en/disable decoupled message delivery in this class
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public NetPoller(NetPortType pt,
		     NetDriver driver,
		     String context,
		     boolean decouplePoller,
		     NetInputUpcall inputUpcall)
	    throws IOException {
	super(pt, driver, context, inputUpcall);
	inputMap = new HashMap();
	this.decouplePoller = decouplePoller;
    }


    /**
     * Enables or disables the singleton fastpath optimization.
     *
     * If on is true and the configuration allows it (i.e. this is a
     * downcall receive port, and the subinput is Interruptible), enable
     * the singleton fastpath optimization. Else disable the singleton
     * fastpath optimization.
     *
     * This method must be called synchronized (this).
     * This method must be called after connecting the subInput, because the
     * subInput must be queried as to its interruptibility.
     */
    private void setSingleton(ReceiveQueue q, boolean on) throws IOException {
	if (upcallFunc != null) {
	    return;
	}
	if (type.inputSingletonOnly()) {
	    return;
	}

	if (VERBOSE_SINGLETON) {
	    System.err.println(this + ": SINGLETON_FASTPATH=" + SINGLETON_FASTPATH + " on=" + on + " upcallFunc=" + upcallFunc + " now singleton=" + singleton + " current=" + q.getInput());
	    System.err.print(this + ": Now subInputs: [");
	    Collection c = inputMap.values();
	    Iterator i = c.iterator();

	    while (i.hasNext()) {
		ReceiveQueue rq  = (ReceiveQueue)i.next();
		NetInput ni = rq.getInput();
		System.err.print(ni + "(" + ni.getUpcallFunc() + ") ");
	    }
	    System.err.println("]");
	}
	boolean switch_to_upcall = true;
	NetInput ni = q.getInput();

	if (SINGLETON_FASTPATH && on) {

	    if (singleton != null) {
		if (VERBOSE_SINGLETON) {
		    System.err.println(this + ": Retain singleton fastpath, subInput " + q.getInput());
		}
		return;
	    }

	    if (! isSingleton()) {
		throw new IllegalArgumentException("Cannot enable singleton if #connections != 1");
	    }

	    try {
		ni.setInterruptible(true);
		if (VERBOSE_SINGLETON) {
		    System.err.println("Set the thing to interruptible");
		    System.err.println(this + ": " + q.getInput()
			    + " pollIsInterruptible "
			    + q.pollIsInterruptible());
		}
		if (q.pollIsInterruptible()) {

		    if (VERBOSE_SINGLETON) {
			System.err.println(this + ": Set singleton fastpath, subInput " + q.getInput());
		    }
		    singleton = q;
		    switch_to_upcall = false;
		    // singleton.switchToDowncallMode();
		} else {
		    ni.setInterruptible(false);
		}
	    } catch (IllegalArgumentException e) {
		if (VERBOSE_SINGLETON) {
		    System.err.println(this + ": " + q.getInput() + " does not support setInterruptible. Give up singleton");
		}
		// Ah well, the subInput does not even support setInterruptible.
		// Give up.
	    }

	} else if (singleton != null) {
	    if (VERBOSE_SINGLETON) {
		System.err.println(this + ": Disable singleton " + singleton.input + " fastpath; add " + ni);
	    }

	    if (q == singleton) {
		throw new Error(this + ": cannot be multipoller and also be the singleton " + singleton);
	    }

	    while (doingPoll) {
		singleton.interruptPoll();
		waitingConnections++;
		try {
		    wait();
		} catch (InterruptedException e) {
		    // Ignore
		}
		waitingConnections--;
	    }
	    notifyAll();
	    singleton.switchToUpcallMode();
	    singleton.setInterruptible(false);

	    singleton = null;
	}

	if (switch_to_upcall) {
	    if (VERBOSE_SINGLETON) {
		System.err.println(this + ": " + q.getInput() + " Switch to upcall mode");
	    }
	    // Since we own the lock on this, no downcall receive can have
	    // occurred yet. We may safely switch the subInput to upcall mode
	    // without interrupting.
	    q.switchToUpcallMode();
	}
    }


    protected void setReadBufferedSupported() {
	readBufferedSupported = true;
	Collection c = inputMap.values();
	Iterator i = c.iterator();

	while (i.hasNext()) {
	    ReceiveQueue rq  = (ReceiveQueue)i.next();
	    if (! rq.getInput().readBufferedSupported()) {
		readBufferedSupported = false;
		break;
	    }
	}
    }


    public boolean readBufferedSupported() {
	return readBufferedSupported;
    }


    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws IOException {
	log.in();

	if (subDriver == null) {
	    String subDriverName = getMandatoryProperty("Driver");
	    subDriver = driver.getIbis().getDriver(subDriverName);
	}

	setupConnection(cnx, cnx.getNum());
if (false && singleton != null)
System.err.println(this + ": OK, we enabled singleton " + singleton.input + " fastpath");

	log.out();
    }

    public void startReceive() throws IOException {
	if (decouplePoller) {
	    /*
	     * If our subclass is a multiplexer, it starts all necessary
	     * upcall threads. Then we do not want an upcall thread in
	     * this class.
	     */
	} else {
	    startUpcallThread();
	}

	Collection c = inputMap.values();
	Iterator i = c.iterator();

	while (i.hasNext()) {
	    ReceiveQueue rq  = (ReceiveQueue)i.next();
	    NetInput in = rq.getInput();

	    in.startReceive();
	}
    }

    protected NetInput newPollerSubInput(Object key, ReceiveQueue q)
	    throws IOException {
	NetInput ni;

	if (decouplePoller && upcallFunc != null) {
	    ni = newSubInput(subDriver, q);
	} else {
	    ni = newSubInput(subDriver, null);
	}

	return ni;
    }

    protected boolean isSingleton() {
	return (inputMap.values().size() == 1);
    }

    /**
     * Actually establish a connection with a remote port
     *
     * @param cnx the connection attributes.
     * @param key the connection key in the splitter {@link #inputMap map}.
     */
    protected synchronized void setupConnection(NetConnection cnx,
						Object key)
	    throws IOException {

	if (false && singleton != null) {
	    System.err.println("Race between NetPoller.connect and poll(block = true). Repair by having one lock :-(");
	}

	log.in();

	/*
	 * Because a blocking poll can be pending while we want
	 * to connect, the ReceivePort's inputLock cannot be taken
	 * during a connect.
	 * This implies that the blocking poll _and_ setupConnection
	 * must protect the data structures.
	 */
	ReceiveQueue q = (ReceiveQueue)inputMap.get(key);

	boolean createNewSubInput = (q == null);
	NetInput ni;
	if (createNewSubInput) {
	    q = new ReceiveQueue();
	    inputMap.put(key, q);

	    ni = newPollerSubInput(key, q);
	    q.setInput(ni);
	} else {
	    ni = q.getInput();
	    if (VERBOSE_SINGLETON) {
		System.err.println(this + ": recycle existing " + q + " for key " + key);
	    }
	}

	ni.setupConnection(cnx);

	if (createNewSubInput) {
	    setSingleton(q, isSingleton());
	}

	/* If this NetPoller is used in downcallMode, the
	 * upcall threads of the subInputs deliver their
	 * message to the queue here. They do not enter
	 * application space. If the message is finished,
	 * they can continue without having to mind other
	 * possible spawning of other upcall threads.
	 * 				RFHH
	 */
	if (upcallFunc == null) {
	    ni.disableUpcallSpawnMode();
	}

	setReadBufferedSupported();

	wakeupBlockedReceiver();

	log.out();
    }


    public boolean pollIsInterruptible() throws IOException {
	return singleton != null && singleton.pollIsInterruptible();
    }


    public void interruptPoll() throws IOException {
	if (singleton != null) {
	    if (singleton.pollIsInterruptible()) {
		singleton.interruptPoll();
	    } else {
		throw new Error("interruptPoll on singleton that is nonInterruptible");
	    }
	} else {
	    // System.err.println(this + ": OH HO.... interruptPoll on a nonsingleton!");
	    // Thread.dumpStack();
	    if (false) {
		throw new Error("interruptPoll was designed for singletons, but now used on nonsingleton");
	    } else {
		synchronized (this) {
		    interrupted = true;
		    wakeupBlockedReceiver(true);
		    while (waitingThreads > 0) {
			try {
			    wait();
			} catch (InterruptedException e) {
			    // Ignore
			}
		    }
		}
	    }
	}
    }


    public void setInterruptible(boolean interruptible) throws IOException {
	Collection c = inputMap.values();
	Iterator i = c.iterator();

	while (i.hasNext()) {
	    ReceiveQueue rq  = (ReceiveQueue)i.next();
	    NetInput in = rq.getInput();
	    in.setInterruptible(interruptible);
	}
    }


    /*
    protected void switchToDowncallMode() throws IOException {
	installUpcallFunc(null);
	// Should we manage upcallSpawnMode?
    }
    */


    /**
     * {@inheritDoc}
     *
     * <BR>
     * If we are in singleton mode, just pass on the inputUpcall to the
     * singleton subInput.
     * If we are not, kinda panic.
     */
    synchronized
    public void switchToUpcallMode(NetInputUpcall inputUpcall)
	    throws IOException {
	if (VERBOSE_SINGLETON) {
	    System.err.println(this + ": switchToUpcallMode singleton " + (singleton == null ? null : singleton.getInput()) + " interruptible " + (singleton == null ? "N/A" : (singleton.pollIsInterruptible() ? "true" : "false")));
	}

	/*
	 * This must be done before any calls to ReceiveQueue.switchToUpcallMode
	 */
	upcallFunc = inputUpcall;

	if (singleton != null) {
	    if (singleton.pollIsInterruptible()) {
		singleton.switchToUpcallMode();
	    } else {
		throw new Error("switchToUpcallMode invoked on nonInterruptible singleton");
	    }
	} else {
	    // System.err.println(this + ": OH HO.... switchToUpcall on a nonsingleton!");

	    Collection c = inputMap.values();
	    Iterator i = c.iterator();
	    while (i.hasNext()) {
		ReceiveQueue rq  = (ReceiveQueue)i.next();
		NetInput ni = rq.getInput();
		if (ni.getUpcallFunc() == null) {
		    throw new Error(this + ": switch nonSingleton subInput " + rq.getInput() + " to upcallMode");
		}
		rq.interruptDeliveringThread();
	    }

	    /*
	     * If there is a downcall receive blocked in our queue, interrupt
	     * it. If there is no downcall thread, all is well as it is.
	     */
	    interruptPoll();
	    if (activeUpcallThread != null) {
		// System.err.println(this + ": Oh ho, oh ho -- some subInput has a pending message; how can we handle that?");
	    }
	}
	// Should we manage upcallSpawnMode?
    }


    protected void verifyNonSingletonPoller() {
	if (singleton != null) {
	    throw new Error("Cannot be a singleton Poller AND have upcall threads");
	}
    }


    /*
     * Blocking receive is implemented as follows.
     * Each subInput has an inputUpcall thread that is blocked in a
     * blocking poll. When a message arrives in the subInput, behaviour
     * is different for upcallMode and downcallMode.
     *
     * 1. upcallMode
     * The subInput.upcallThread grabs this.upcallLock and performs the
     * upcall. finish() unlocks this.upcallLock.
     *
     * 2. downcallMode
     * The subInput.upcallThread registers in its state that it is
     * active, signals any waiting application threads, and waits until
     * the message is finished. The application thread that wants to
     * perform a blocking receive queries the state of all poller threads.
     * If one has a pending message, that subInput becomes the current
     * input. The message is read in the usual fashion. At finish time,
     * the subInput.upcallThread is woken up to continue polling in its
     * subInput. If there is no pending succeeded poll, the application
     * thread waits.
     *
     * Performance optimization: if there is only one subInput, the role
     * of the poller thread is taken by the application thread.
     */
private int nCurrent;

    protected final class ReceiveQueue implements NetInputUpcall {

	private NetInput		input     = null;
	private Integer			activeNum = null;
	private int			waitingPollers = 0;

	public Integer activeNum() {
	    return activeNum;
	}

	public NetInput getInput() {
	    return input;
	}

	public void setInput(NetInput input) {
	    this.input = input;
	}

	public void inputUpcall(NetInput input, Integer spn)
		throws IOException {
	    log.in();

	    if (spn == null) {
		throw new ConnectionClosedException("connection closed");
	    }
	    Thread me = Thread.currentThread();
	    boolean upcallMode;

	    /*
	     * Loop here to recognize the case that we are interrupted by
	     * an incoming switchToUpcallMode.
	     */
	    while (true) {
		/*
		 * Must lock NetPoller.this <strong>before</strong> we attempt
		 * to read upcallFunc. Its value may be changed by
		 * switchTo*callMode with the lock on NetPoller.this held.
		 */
		synchronized (NetPoller.this) {
		    upcallMode = (upcallFunc != null);
		    if (upcallMode) {

			grabUpcallLock(this);
nCurrent++;
		    } else {
			wakeupBlockedReceiver();
nCurrent++;
		    }
		    activeNum = spn;
		    activeUpcallThread = me;
		    log.disp("NetPoller queue thread poll returns ",activeNum);
		}

		if (upcallMode) {
		    /* Must release the lock because some other
		     * thread may finish the message */
		    log.disp("upcallFunc.inputUpcall-->");
		    upcallFunc.inputUpcall(NetPoller.this, spn);
		    log.disp("upcallFunc.inputUpcall<--");

		    synchronized (NetPoller.this) {
			if (activeUpcallThread == me) {
			    // implicit finish()
			    finishLocked(true);
			}
		    }
		    break;

		} else {

		    synchronized (this) {
			if (activeNum == spn) {
			    waitingPollers++;
			    try {
				wait();
			    } catch (InterruptedException e) {
				// ignore
			    }
			    waitingPollers--;
			}
			if (activeNum == null) {
			    break;
			}
		    }
		}
	    }

	    log.out();
	}


	/* Call this synchronized (NetPoller.this) */
	Integer poll(boolean block) throws IOException {
	    log.in();
	    if (decouplePoller && singleton == null) {
		if (! NetReceivePort.useBlockingPoll && activeNum == null) {
		    activeNum = input.poll(block);
		}
	    } else {
		activeNum = input.poll(block);
	    }

	    return activeNum;
	}


	/* Call this synchronized (NetPoller.this) */
	Integer poll() throws IOException {
	    log.in();
	    if (! NetReceivePort.useBlockingPoll && activeNum == null) {
		    activeNum = input.poll(false);
	    }

	    log.out();

	    return activeNum;
	}


	boolean pollIsInterruptible() throws IOException {
	    return input.pollIsInterruptible();
	}


	void setInterruptible(boolean interruptible) throws IOException {
	    input.setInterruptible(interruptible);
	}


	/*
	void switchToDowncallMode() throws IOException {
	    input.switchToDowncallMode();
	}
	*/


	/**
	 * Switch our subInput to upcall mode so it delivers messages to our
	 * queue, where we pick them up to handle them.
	 */
	void switchToUpcallMode() throws IOException {
	    if (NetReceivePort.useBlockingPoll || upcallFunc != null) {
		input.switchToUpcallMode(this);
	    } else {
		System.err.println(this + "OHHHH HOOOOO switchToUpcallMode and upcall null");
		if (true) {
		    throw new Error(this + ": switchToUpcallMode is only supported for blockingPoll or upcall receives");
		} else {
		    input.switchToUpcallMode(null);
		}
	    }
	}


	void interruptPoll() throws IOException {
	    input.interruptPoll();
	}


	/* Call this from synchronized (NetPoller.this) */
	public void finish(boolean implicit) throws IOException {
	    log.in();

	    activeNum = null;
	    if (! implicit) {
		input.finish();
	    }

	    if (upcallFunc != null) {
		releaseUpcallLock();
	    } else {
		synchronized (this) {
		    if (waitingPollers > 0) {
			notify();
		    }
		}
	    }

	    log.out();
	}


	synchronized
	void interruptDeliveringThread() {
	    if (waitingPollers > 0) {
		notifyAll();
	    }
	}


	void free() throws IOException {
	    log.in();
	    input.free();
	    log.out();
	}

    }


    // Call the method synchronized(this)
    private void grabUpcallLock(ReceiveQueue q) throws InterruptedIOException {
	log.in();trace.in();

	while (activeQueue != null) {
	    upcallWaiters++;
	    try {
		wait();
	    } catch (InterruptedException e) {
System.err.println(this + ": Catch " + e  + ". This CANNOT BE");
		throw new InterruptedIOException(e);
	    } finally {
		upcallWaiters--;
	    }
	}
	activeQueue = q;
	mtu = activeQueue.input.getMaximumTransfertUnit();
	headerOffset = activeQueue.input.getHeadersLength();

	log.out();trace.out();
    }


    // Call the method synchronized(this)
    private void releaseUpcallLock() {
	log.in();trace.in();

	activeQueue = null;
	if (upcallWaiters > 0) {
	    if (waitingConnections > 0 || waitingThreads > 0) {
		notifyAll();
	    } else {
		notify();
	    }
	}

	log.out();trace.out();
    }


    private void wakeupBlockedReceiver() {
	log.in();
	wakeupBlockedReceiver(false);
	log.out();
    }


    private void wakeupBlockedReceiver(boolean all) {
	log.in();
	if (waitingThreads > 0) {
	    if (all) {
		notifyAll();
	    } else {
		notify();
	    }
	}
	log.out();
    }


    private void blockReceiver() throws IOException {
	// System.err.println(this + ": block receiver thread");
	log.in();
	waitingThreads++;
	try {
	    // This wait is not in a loop here, because blockReceiver
	    // is always called from within a loop that tests the
	    // associated condition.
	    wait();
	    if (interrupted) {
		interrupted = false;
		if (VERBOSE_SINGLETON) {
		    System.err.println(this + ": blocked receiver is interrupted. Throw interruptedIOException");
		}
		notifyAll();
		throw new InterruptedIOException("Wait interrupted");
	    }
	} catch (InterruptedException e) {
System.err.println(this + ": Catch " + e  + ". This CANNOT BE");
	    throw new InterruptedIOException(e);
	} finally {
	    waitingThreads--;
	}
	log.out();
    }

    // private ibis.util.Timer rcveTimer = new ibis.util.Timer.createTimer();

    /**
     * Called from poll() when the input indicated by ni has a message
     * to receive.
     * Set the state local to your implementation here.
     *
     * Call this synchronized (this)
     */
    protected void selectConnection(ReceiveQueue rq) {
	log.in();
	NetInput    input = rq.getInput();
	log.disp("1");
	mtu = input.getMaximumTransfertUnit();
	log.disp("2");
	headerOffset = input.getHeadersLength();
	if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
	    System.err.println(this + ": selectConnection, input " + input + " headerOffset " + headerOffset);
	}
	// rcveTimer.start();
	log.out();
    }


    public void initReceive(Integer num) throws IOException {
	    //
    }

    /**
     * Polls the inputs.
     *
     * Fast path:
     * - record whether there is exactly one subInput.
     *   If so, skip manipulating the inputMap.
     * - If there is exactly one subInput, don't use threads for
     *   downcall receives. Let the thread that does a receive to us
     *   perform the poll to the lower layer itself.
     *   Caveat for one subInput: if this thread is blocked in a poll and
     *   a second connection is set up, the poll must be interrupted.
     *   NetIbis boasts a mechanism to interrupt blocking polls
     *   for that, depending on the subInput implementing
     *   NetPollInterruptible,
     */
    private Integer pollSingleton(boolean block) throws IOException {

	Integer      spn = null;

	if ((spn = singleton.poll(block)) != null) {
	    activeQueue = singleton;
	    selectConnection(singleton);
	}

	return spn;
    }


    private synchronized Integer pollNonSingleton(boolean block)
	    throws IOException {

	Integer      spn = null;
	ReceiveQueue rq  = null;

	if (activeQueue != null) {
	    throw new IOException("Call message.finish before calling Net.poll");
	}

	while (singleton == null) {
	    // If singleton == null, we were woken up because the
	    // first connection was established. Return to the level
	    // above.

	    final Collection c = inputMap.values();
	    final int        s = c.size();

	    if (s != 0) {
		firstToPoll %= s;

		// The pair of loops is used to implement
		// some kind of fairness in ReceiveQueue polling.
		// first pass
		Iterator i = c.iterator();
		int j;
		for (j = 0; j < firstToPoll; j++) {
		    i.next();
		}
		while (spn == null && i.hasNext()) {
		    rq  = (ReceiveQueue)i.next();
		    spn = rq.poll();
		}

		if (spn == null) {
		    // second pass
		    i = c.iterator();
		    j = 0;
		    while (spn == null && j++ < firstToPoll &&
			    i.hasNext()) {
			rq  = (ReceiveQueue)i.next();
			spn = rq.poll();
		    }
		}

		firstToPoll++;

		if (spn != null) {
		    activeQueue = rq;
		    if (ibis.impl.net.NetIbis.DEBUG_RUTGER) {
			System.err.println(this + ": pollNonSingleton sets activeQueue " + activeQueue);
		    }
		    selectConnection(rq);
		    break;
		}
	    }

	    if (! block) {
		break;
	    }
	    blockReceiver();
	}

	return spn;
    }


    public Integer doPoll(boolean block) throws IOException {
	    log.in();

	Integer      spn = null;

	do {

	    synchronized (this) {
		if (activeQueue != null) {
		    throw new IOException("Call message.finish before calling Net.poll");
		}

		if (doingPoll) {
		    throw new java.util.ConcurrentModificationException("Only one poll at a time allowed");
		}

	    if (false)	// No concurrent polls
		while (doingPoll) {
		    blockReceiver();
		}

		doingPoll = true;
	    }

	    try {
		// Here, should have synchronized access on singleton
		if (singleton == null) {
		    spn = pollNonSingleton(block);
		} else {
		    spn = pollSingleton(block);
		}
	    } catch (InterruptedIOException e) {
		System.err.println(this + ": Ha, it throws us an InterruptedIOException. Sync with the interrupter and continue " + e);
		e.printStackTrace(System.err);
		if (waitingConnections == 0) {
		    System.err.println(this + ": It is not for us. Throw it on for our superInput");
		    throw e;
		}

	    } finally {
		synchronized (this) {
		    doingPoll = false;
		    while (waitingConnections > 0) {
			notifyAll();
			try {
			    wait();
			} catch (InterruptedException e) {
			    //
			}
		    }
		}
	    }

	} while (block && spn == null);

	if (false) {
		/* It is better to yield at a higher level.
		 * Here it is uncontrollable, anyway. */
		NetIbis.yield();
	}
	log.out();

	return spn;
    }


    /**
     * @param implicit indicates whether this is an implicit finish
     *        from a returned upcall
     *
     * Call this synchronized(this)
     */
    private void finishLocked(boolean implicit) throws IOException {
	log.in();
	if (activeQueue != null) {
	    activeQueue.finish(implicit);
	    activeQueue = null;
	}

	activeUpcallThread = null;
	log.out();
    }


    public void doFinish() throws IOException {
	log.in();
	// rcveTimer.stop();
	synchronized (this) {
	    finishLocked(false);
	}
	log.out();
    }


    public void doFree() throws IOException {
	log.in();trace.in();
	trace.disp("0, ", this);
	// System.err.println("Time between receive and finish " + rcveTimer.averageTime());
	if (inputMap != null) {
	    Iterator i = inputMap.values().iterator();

	    trace.disp("1, ", this);
	    if (inputMap.values().size() == 1) {
		    log.disp("Pity, missed the chance of a blocking NetPoller without thread switch");
	    } else {
		    log.disp("No chance of a blocking NetPoller without thread switch; size ", inputMap.values().size());
	    }

	    trace.disp("2, ", this);
	    while (i.hasNext()) {
		ReceiveQueue q = (ReceiveQueue)i.next();
		//NetInput ni = q.input;
		q.free();
		//ni.free();
		i.remove();
	    }
	    trace.disp("3, ", this);
	}

	synchronized(this) {
	    activeQueue = null;
	    activeUpcallThread = null;

	    //                          while (activeQueue != null)
	    //                                  wait();
	}
	trace.disp("4, ", this);
	trace.out();log.out();
    }


    protected Object getKey(Integer num) {
	return num;
    }


    public synchronized void closeConnection(ReceiveQueue rq, Integer num) throws IOException {
	//
	NetInput input = rq.getInput();
	if (input != null) {
	    input.close(num);
	}
    }


    protected synchronized void doClose(Integer num) throws IOException {
	log.in();
	if (inputMap != null) {
	    Object       key = getKey(num);
	    ReceiveQueue rq  = (ReceiveQueue)inputMap.get(key);

	    if (rq != null) {
		closeConnection(rq, num);

		if (activeQueue == rq) {
		    activeQueue = null;
		    activeUpcallThread = null;
		    notifyAll();
		}
	    }

	}
	log.out();
    }

    protected NetInput activeInput() throws IOException {
	try {
	    ReceiveQueue  rq    = activeQueue;
	    NetInput      input = rq.getInput();
	    if (input == null) {
		throw new ConnectionClosedException("input closed");
	    }
	    return input;
	} catch (NullPointerException e) {
	    throw new ConnectionClosedException(e);
	}
    }


    public int readBuffered(byte[] data, int offset, int length)
	    throws IOException {
	log.in();
	if (length < 0 || offset + length > data.length) {
	    throw new ArrayIndexOutOfBoundsException("Illegal buffer bounds");
	}
	if (! readBufferedSupported) {
	    throw new IOException("readBuffered not supported");
	}

	NetBufferedInputSupport bi = (NetBufferedInputSupport)activeInput();
	int rd = bi.readBuffered(data, offset, length);
	log.out();

	return rd;
    }

    public NetReceiveBuffer readByteBuffer(int expectedLength) throws IOException {
	log.in();
	NetReceiveBuffer b = activeInput().readByteBuffer(expectedLength);
	log.out();
	return b;
    }

    public void readByteBuffer(NetReceiveBuffer buffer) throws IOException {
	log.in();
	activeInput().readByteBuffer(buffer);
	log.out();
    }

    public boolean readBoolean() throws IOException {
	log.in();
	boolean v = activeInput().readBoolean();
	log.out();
	return v;
    }

    public byte readByte() throws IOException {
	log.in();
	byte v = activeInput().readByte();
	log.out();
	return v;
    }

    public char readChar() throws IOException {
	log.in();
	char v = activeInput().readChar();
	log.out();
	return v;
    }

    public short readShort() throws IOException {
	log.in();
	short v = activeInput().readShort();
	log.out();
	return v;
    }

    public int readInt() throws IOException {
	log.in();
	int v = activeInput().readInt();
	log.out();
	return v;
    }

    public long readLong() throws IOException {
	log.in();
	long v = activeInput().readLong();
	log.out();
	return v;
    }

    public float readFloat() throws IOException {
	log.in();
	float v = activeInput().readFloat();
	log.out();
	return v;
    }

    public double readDouble() throws IOException {
	log.in();
	double v = activeInput().readDouble();
	log.out();
	return v;
    }

    public String readString() throws IOException {
	log.in();
	String v = (String)activeInput().readString();
	log.out();
	return v;
    }

    public Object readObject() throws IOException, ClassNotFoundException {
	log.in();
	Object v = activeInput().readObject();
	log.out();
	return v;
    }

    public void readArray(boolean [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(byte [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(char [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(short [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(int [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(long [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(float [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(double [] b, int o, int l) throws IOException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

    public void readArray(Object [] b, int o, int l) throws IOException, ClassNotFoundException {
	log.in();
	activeInput().readArray(b, o, l);
	log.out();
    }

}
