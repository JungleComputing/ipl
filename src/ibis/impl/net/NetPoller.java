package ibis.impl.net;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.InterruptedIOException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Provides a generic multiple network input poller.
 */
public class NetPoller extends NetInput implements NetBufferedInputSupport {

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
	private boolean		handlingSingleton;
	private int		waitingConnections;

	/**
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;

	/**
	 * The input queue that was last sucessfully polled, or <code>null</code>.
	 */
	protected ReceiveQueue  activeQueue = null;
        protected Thread	activeUpcallThread = null;

	private int		upcallWaiters;

	/**
	 * Count the number of application threads that are blocked in a poll
	 */
	protected int		waitingThreads = 0;


        /**
         * The first queue that should be polled first next time we have to poll the queues.
         */
        private int             firstToPoll  = 0;

        private boolean         upcallMode = false;

	/**
	 * In the downcall case, this module usually starts an upcall thread
	 * in each subInput. If the subInput is a multiplexer, it only
	 * requires one thread in the multiplexer = one thread per ReceivePort.
	 * So, this driver should not start a thread by itself, but depend
	 * on the multiplexer thread to perform the upcalls.
	 * To switch on this behaviour, set decouplePoller = false.
	 */
	private boolean		decouplePoller;

	protected boolean	readBufferedSupported = true;

	/**
	 * Constructor.
	 *
	 * @param pt      the port type.
	 * @param driver  the driver of this poller.
	 * @param context the context string.
	 */
	public NetPoller(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		this(pt, driver, context, true);
	}

	/**
	 * Constructor.
	 *
	 * @param pt      the port type.
	 * @param driver  the driver of this poller.
	 * @param context the context string.
	 * @param decouplePoller en/disable decoupled message delivery in this class
	 */
	public NetPoller(NetPortType pt, NetDriver driver, String context, boolean decouplePoller)
		throws IOException {
                super(pt, driver, context);
                inputMap = new HashMap();
		this.decouplePoller = decouplePoller;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();

		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetInput ni = newSubInput(subDriver);

		setupConnection(cnx, cnx.getNum(), ni);
if (singleton != null)
System.err.println(this + ": OK, we enabled singleton fastpath");

		/*
		 * If our subclass is a multiplexer, it starts all necessary
		 * upcall threads. Then we do not want an upcall thread in
		 * this class.
		 */
		if (! decouplePoller) {
// System.err.println(this + ": start upcall thread");
		    startUpcallThread();
		}
                log.out();
	}


	/**
	 * Call this synchronized (this)
	 *
	 * If on is true and the configuration allows it (i.e. this is a
	 * downcall receive port, and the subinput is Interruptible), enable
	 * the singleton fastpath optimization. Else disable the singleton
	 * fastpath optimization.
	 */
	private void setSingleton(boolean on) throws IOException {
	    if (on) {
		if (singleton != null) {
		    throw new IllegalArgumentException("Only one singleton is allowed");
		}

		Collection c = inputMap.values();

		if (c.size() != 1) {
		    throw new IllegalArgumentException("Cannot enable singleton if #connections != 1");
		}

		Iterator i = c.iterator();
		ReceiveQueue q = (ReceiveQueue)i.next();
		if (! upcallMode && q.pollIsInterruptible()) {
// System.err.println("Set singleton fastpath, #connections = " + inputMap.values().size());
		    singleton = q;
		}
	    } else if (singleton != null) {
System.err.println(Thread.currentThread() + ": " + this + ": Disable singleton fastpath.");
		while (handlingSingleton) {
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
		singleton.clearInterruptible();
		singleton = null;
	    }
	}

	protected void setReadBufferedSupported() {
	    readBufferedSupported = false;
	    Collection c = inputMap.values();
	    Iterator i = c.iterator();

	    while (i.hasNext()) {
		ReceiveQueue rq  = (ReceiveQueue)i.next();
		if (! rq.input().readBufferedSupported()) {
		    readBufferedSupported = false;
		    break;
		}
	    }
	}


	/**
	 * {@inheritDoc}
	 */
	public boolean readBufferedSupported() {
	    return readBufferedSupported;
	}


	/**
         * Actually establish a connection with a remote port and
         * register an upcall function for incoming message
         * notification.
	 *
         * @param cnx the connection attributes.
         * @param key the connection key in the splitter {@link #inputMap map}.
         * @param ni the connection's input.
	 */
	protected synchronized void setupConnection(NetConnection cnx,
						    Object key,
						    NetInput ni)
		throws IOException {

		if (false && singleton != null) {
		    System.err.println("Race between NetPoller.connect and poll(block = true). Repair by having one lock :-(");
		}

                log.in();
		boolean only = inputMap.values().size() == 0;
                /*
                 * Because a blocking poll can be pending while we want
                 * to connect, the ReceivePort's inputLock cannot be taken
                 * during a connect.
                 * This implies that the blocking poll _and_ setupConnection
                 * must protect the data structures.
                 */
		ReceiveQueue q = (ReceiveQueue)inputMap.get(key);

		upcallMode = (upcallFunc != null);

		if (q == null) {
			q = new ReceiveQueue(ni);
			inputMap.put(key, q);
// System.err.println(this + ": add subInput " + ni + "; key " + key + "; upcallFunc " + upcallFunc);
			setSingleton(only);
		}

		if (decouplePoller && singleton == null &&
		    (NetReceivePort.useBlockingPoll || upcallMode)) {
			ni.setupConnection(cnx, q);
		} else {
			ni.setupConnection(cnx, null);
		}

		if (singleton == q) {
// System.err.println("Set the thing to interruptible");
		    singleton.setInterruptible();
		}

		/* If this NetPoller is used in downcallMode, the
		 * upcall threads of the subInputs deliver their
		 * message to the queue here. They do not enter
		 * application space. If the message is finished,
		 * they can continue without having to mind other
		 * possible spawning of other upcall threads.
		 * 				RFHH
		 */
		if (! upcallMode) {
		    ni.disableUpcallSpawnMode();
		}

		setReadBufferedSupported();

		wakeupBlockedReceiver();

                log.out();
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
	 * ToDo:
	 * Performance optimization: if there is only one subInput, the role
	 * of the poller thread can be taken by the application thread.
	 */
private int nCurrent;

	protected final class ReceiveQueue
		implements NetInputUpcall {

                private          NetInput	input     = null;
                private volatile Integer	activeNum = null;
		private          NetPollInterruptible intpt = null;
		private          boolean        interruptible;

                ReceiveQueue(NetInput input) {
                        this.input = input;
			if (input instanceof NetPollInterruptible) {
			    intpt = (NetPollInterruptible)input;
			}
                }

                public Integer activeNum() {
                        return activeNum;
                }

                public NetInput input() {
                        return input;
                }

		public void inputUpcall(NetInput input, Integer spn)
			throws IOException {
                        log.in();

			if (spn == null) {
				throw new ConnectionClosedException("connection closed");
			}
                        Thread me = Thread.currentThread();

                        if (upcallMode) {
				synchronized (NetPoller.this) {

					grabUpcallLock(this);
					activeNum = spn;
					activeUpcallThread = me;
					log.disp("NetPoller queue thread poll returns ",activeNum);
nCurrent++;
				}

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

                        } else {
                                synchronized (NetPoller.this) {
					wakeupBlockedReceiver();
					activeNum = spn;
					// Do we require currentThread in the
					// downcall case?????
					activeUpcallThread = me;
					log.disp("NetPoller queue thread poll returns ",activeNum);
nCurrent++;
                                        while (activeNum == spn) {
						blockReceiver();
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
// else if (NetReceivePort.useBlockingPoll)
// System.err.print("_");
			} else {
// System.err.print("P>");
			    activeNum = input.poll(block);
// System.err.print("<");
			}

                        return activeNum;
                }


                /* Call this synchronized (NetPoller.this) */
                Integer poll() throws IOException {
                        log.in();
// System.err.print("p");
// System.err.println(Thread.currentThread() + ": " + this + ": poll this subInput, activeNum " + activeNum);
                        if (! NetReceivePort.useBlockingPoll && activeNum == null) {
                                activeNum = input.poll(false);
                        }

                        log.out();

                        return activeNum;
                }


		boolean pollIsInterruptible() {
		    return intpt != null;
		}


		void setInterruptible() throws IOException {
		    interruptible = true;
		    intpt.setInterruptible();
		}


		void clearInterruptible() throws IOException {
		    interruptible = false;
		    if (NetReceivePort.useBlockingPoll || upcallMode) {
			intpt.clearInterruptible(this);
		    } else {
			intpt.clearInterruptible(null);
		    }
		}


		void interruptPoll() throws IOException {
		    intpt.interruptPoll();
		}


                /* Call this from synchronized (NetPoller.this) */
                public void finish(boolean implicit) throws IOException {
                        log.in();

                        activeNum = null;
			if (! implicit) {
				input.finish();
			}

                        if (upcallMode) {
                                releaseUpcallLock();
                        } else if (waitingThreads > 0) {
                                // NetPoller.this.notifyAll();
                                NetPoller.this.notify();
                        }

                        log.out();
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
                                throw new InterruptedIOException(e);
                        } finally {
				upcallWaiters--;
			}
                }
                activeQueue = q;

                log.out();trace.out();
	}


	// Call the method synchronized(this)
	private void releaseUpcallLock() {
                log.in();trace.in();

                activeQueue = null;
                if (upcallWaiters > 0) {
                        notify();
                        // notifyAll();
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
                        wait();
                } catch (InterruptedException e) {
                        throw new InterruptedIOException(e);
                } finally {
			waitingThreads--;
		}
                log.out();
	}

// private ibis.util.nativeCode.Rdtsc rcveTimer = new ibis.util.nativeCode.Rdtsc();

	/**
	 * Called from poll() when the input indicated by ni has a message
	 * to receive.
	 * Set the state local to your implementation here.
	 *
	 * Call this synchronized (this)
	 */
	protected void selectConnection(ReceiveQueue rq) {
                log.in();
                NetInput    input = rq.input();
                log.disp("1");
                mtu = input.getMaximumTransfertUnit();
                log.disp("2");
                headerOffset = input.getHeadersLength();
// rcveTimer.start();
                log.out();
	}


        protected void initReceive(Integer num) throws IOException {
                //
        }

	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 *
	 * Opportunities for faster path:
	 * - record whether there is exactly one subInput.
	 *   If so, skip manipulating the inputMap.
	 * - Manana: if there is exactly one subInput, don't use threads for
	 *   downcall receives. Let the thread that does a receive to us
	 *   perform the poll to the lower layer itself.
	 *   Caveat for one subInput: if this thread is blocked in a poll and
	 *   a second connection is set up, the poll must be interrupted.
	 *   I think NetIbis requires a mechanism to interrupt blocking polls
	 *   for that. (Implementation hints: either close the connection and
	 *   catch the exception or ourselves send a control message to the
	 *   thread.)
	 */
	private Integer pollSingleton(boolean block) throws IOException {

	    Integer      spn = null;

	    synchronized (this) {
		if (activeQueue != null) {
		    throw new IOException("Call message.finish before calling Net.poll");
		}
		while (handlingSingleton && singleton != null) {
		    blockReceiver();
		}
		if (singleton == null) {
		    return spn;
		}
		handlingSingleton = true;
	    }

	    boolean all = false;
	    try {
		if ((spn = singleton.poll(block)) != null) {
		    activeQueue = singleton;
		    selectConnection(singleton);
		}
	    } catch (InterruptedIOException e) {
		System.err.println(Thread.currentThread() + ": " + this + ": Ha, it throws us an InterruptedIOException. Sync with the interrupter and continue");
		all = true;
	    }

	    synchronized (this) {
		handlingSingleton = false;
		while (waitingConnections > 0) {
		    notifyAll();
		    try {
			wait();
		    } catch (InterruptedException e) {
			//
		    }
		}
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

// System.err.print("[");

		if (singleton == null) {
// System.err.print("BLA ");
		    spn = pollNonSingleton(block);
		} else {
		    spn = pollSingleton(block);
		}

// System.err.print("] spn=" + spn);
	    } while (block && spn == null);

	    if (false) {
		    /* It is better to yield at a higher level.
		     * Here it is uncontrollable, anyway. */
		    Thread.yield();
	    }
	    log.out();
// if (spn == null)
// System.err.println(Thread.currentThread() + ": " + this + ".doPoll returns " + spn);

	    return spn;
	}


	/**
	 * @param fromUpcall indicates whether this is an implicit finish
	 *        from a returned upcall
	 *
	 * Call this synchronized(this)
	 */
	private void finishLocked(boolean implicit) throws IOException {
// System.err.println(this + ": finish()");
                log.in();
                if (activeQueue != null) {
                        activeQueue.finish(implicit);
                        activeQueue = null;
                }

                activeUpcallThread = null;
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void doFinish() throws IOException {
                log.in();
// rcveTimer.stop();
		synchronized (this) {
                        finishLocked(false);
		}
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws IOException {
                log.in();trace.in();
                trace.disp("0, ", this);
// // if (singleton != null)
// System.err.println(this + ": singleton = " + singleton + " nCurrent " + nCurrent);
// System.err.println("Time between receive and finish " + rcveTimer.averageTime());
		if (inputMap != null) {
			Iterator i = inputMap.values().iterator();

                        trace.disp("1, ", this);
			if (inputMap.values().size() == 1) {
                                log.disp("Pity, missed the chance of a blocking NetPoller without thread switch");
			} else {
                                log.disp("No chance of a blocking NetPoller without thread switch; size " + inputMap.values().size());
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
                NetInput input = rq.input();
                if (input != null) {
                        input.close(num);
                }
        }


        public synchronized final void doClose(Integer num) throws IOException {
                log.in();
		if (inputMap != null) {
                        Object       key = getKey(num);
                        ReceiveQueue rq  = (ReceiveQueue)inputMap.get(num);

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
                        NetInput      input = rq.input;
                        if (input == null) {
                                throw new ConnectionClosedException("input closed");
                        }
                        return input;
                } catch (NullPointerException e) {
                        throw new ConnectionClosedException(e);
                }
        }


	/**
	 * {@inheritDoc}
	 */
	public int readBuffered(byte[] data, int offset, int length)
		throws IOException {
	    log.in();
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
