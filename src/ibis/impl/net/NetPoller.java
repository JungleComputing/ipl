package ibis.ipl.impl.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;

/**
 * Provides a generic multiple network input poller.
 */
public class NetPoller extends NetInput {

	/**
	 * The set of inputs.
	 */
        protected HashMap inputMap  = null;
	private ReceiveQueue singleton;

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
	protected boolean	upcallModeAllowed;

	/**
	 * Constructor.
	 *
	 * @param pt      the port type.
	 * @param driver  the driver of this poller.
	 * @param context the context string.
	 */
	public NetPoller(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
                super(pt, driver, context);
                inputMap = new HashMap();
		upcallModeAllowed = true;	// default, override at will
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();

		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetInput ni = newSubInput(subDriver);

		setupConnection(cnx, cnx.getNum(), ni);

		if (! upcallModeAllowed) {
		    startUpcallThread();
		}
                log.out();
	}



	private void setSingleton() {
	    if (inputMap.values().size() == 1) {
		Collection c = inputMap.values();
		Iterator i = c.iterator();
		singleton = (ReceiveQueue)i.next();
	    } else {
		singleton = null;
	    }
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
	public void setupConnection(NetConnection cnx,
				    Object key,
				    NetInput ni)
		throws NetIbisException {

		if (singleton != null) {
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
                synchronized (this) {
                        ReceiveQueue q = (ReceiveQueue)inputMap.get(key);
                        if (q == null) {
                                q = new ReceiveQueue(ni);
                                inputMap.put(key, q);
// System.err.println(this + ": add subInput " + ni + "; key " + key + "; upcallFunc " + upcallFunc);
				setSingleton();
                        }

                        upcallMode = (upcallFunc != null);

                        if (upcallModeAllowed &&
			    (NetReceivePort.useBlockingPoll || upcallMode)) {
                                ni.setupConnection(cnx, q);
                        } else {
                                ni.setupConnection(cnx, null);
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

                        // Don't understand: wakeupBlockedReceiver();
                }

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

                ReceiveQueue(NetInput input) {
                        this.input = input;
                }

                public Integer activeNum() {
                        return activeNum;
                }

                public NetInput input() {
                        return input;
                }

		public void inputUpcall(NetInput input, Integer spn)
			throws NetIbisException {
                        log.in();

                        Thread me;

                        synchronized (NetPoller.this) {
                                if (spn == null) {
                                        throw new NetIbisClosedException("connection closed");
                                }

                                if (upcallMode) {
                                        grabUpcallLock(this);
                                } else {
                                        wakeupBlockedReceiver();
                                }
                                activeNum = spn;

                                me = Thread.currentThread();
                                activeUpcallThread = me;
                                log.disp("NetPoller queue thread poll returns ",activeNum);
nCurrent++;
                        }

                        if (upcallMode) {
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
                                        while (activeNum == spn) {
                                                try {
							waitingThreads++;
                                                        NetPoller.this.wait();
							waitingThreads--;
                                                } catch (InterruptedException e) {
                                                        throw new NetIbisInterruptedException(e);
                                                }
                                        }
                                }
                        }

                        log.out();
		}


                /* Call this from synchronized (NetPoller.this) */
                Integer poll(boolean block) throws NetIbisException {
                        log.in();
			if (upcallModeAllowed) {
			    if (! NetReceivePort.useBlockingPoll && activeNum == null) {
				    activeNum = input.poll(block);
			    }
			} else {
// System.err.print("P>");
			    activeNum = input.poll(block); // (false);
// System.err.print("<");
			}

                        return activeNum;
                }


                /* Call this from synchronized (NetPoller.this) */
                Integer poll() throws NetIbisException {
                        log.in();
// System.err.print("p");
                        if (! NetReceivePort.useBlockingPoll && activeNum == null) {
                                activeNum = input.poll(false);
                        }

                        log.out();

                        return activeNum;
                }



                /* Call this from synchronized (NetPoller.this) */
                public void finish(boolean implicit) throws NetIbisException {
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


                void free() throws NetIbisException {
                        log.in();
			input.free();
                        log.out();
                }

	}


	// Call the method synchronized(this)
	private void grabUpcallLock(ReceiveQueue q) throws NetIbisInterruptedException {
                log.in();trace.in();

                while (activeQueue != null) {
                        upcallWaiters++;
                        try {
                                wait();
                        } catch (InterruptedException e) {
                                throw new NetIbisInterruptedException(e);
                        }
                        upcallWaiters--;
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
                // System.err.println(this + ": gonna unblock receiver thread");
                if (waitingThreads > 0) {
                        notify();
                        // notifyAll();
                }
                log.out();
	}


	private void blockReceiver() throws NetIbisException {
                // System.err.println(this + ": block receiver thread");
                log.in();
                waitingThreads++;
                try {
                        wait();
                } catch (InterruptedException e) {
                        throw new NetIbisInterruptedException(e);
                }
                waitingThreads--;
                // System.err.println(this + ": unblocked receiver thread");
                log.out();
	}


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
                log.out();
	}


        protected void initReceive(Integer num) throws NetIbisException {
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
	public Integer doPoll(boolean block) throws NetIbisException {
                log.in();

		Integer      spn = null;

                synchronized (this) {
		    if (activeQueue != null) {
			    throw new NetIbisException("Call message.finish before calling Net.poll");
		    }

// System.err.print("[");
		    if (singleton != null) {
			/* Do we actually require synchronized here?
			 * Arrgghhhh --- we cannot really block here because
			 * we must release the lock to allow for connections.
			 * Arrrghhhh ----
			 * Usually not :-) */
			if ((spn = singleton.poll(block)) != null) {
			    activeQueue = singleton;

			    selectConnection(singleton);
			}

		    } else {

                        while (true) {
                                final Collection c = inputMap.values();
                                final int        s = c.size();

                                if (s != 0) {
                                        firstToPoll %= s;

                                        // The pair of loops is used to implement
                                        // some kind of fairness in ReceiveQueue polling.
                                        dummy:
                                        do {
                                                Iterator i = null;
                                                int      j =    0;

                                                // first pass
                                                i = c.iterator();
                                                j = 0;
                                                while (i.hasNext()) {
                                                        ReceiveQueue rq  = (ReceiveQueue)i.next();
                                                        if (j++ < firstToPoll)
                                                                continue;

                                                        if ((spn = rq.poll()) != null) {
                                                                activeQueue = rq;

                                                                selectConnection(rq);
                                                                break dummy;
                                                        }
                                                }

                                                // second pass
                                                i = c.iterator();
                                                j = 0;
                                                while (i.hasNext()) {
                                                        if (j++ >= firstToPoll)
                                                                break;

                                                        ReceiveQueue rq  = (ReceiveQueue)i.next();
                                                        if ((spn = rq.poll()) != null) {
                                                                activeQueue = rq;

                                                                selectConnection(rq);
                                                                break dummy;
                                                        }
                                                }

                                        } while (false);


                                        firstToPoll++;

                                        if (spn != null) {
                                                log.out();
                                                return spn;
                                        }

                                        if (! block) {
                                                break;
                                        }
                                }

				blockReceiver();
			    }
                        }
                }
// System.err.print("]");

                /* break because ! block && spn == null */

		if (false) {
			/* It is better to yield at a higher level.
			 * Here it is uncontrollable, anyway. */
			Thread.yield();
		}
                log.out();

                return spn;
	}


	/**
	 * @param fromUpcall indicates whether this is an implicit finish
	 *        from a returned upcall
	 *
	 * Call this synchronized(this)
	 */
	private void finishLocked(boolean implicit) throws NetIbisException {
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
	public void doFinish() throws NetIbisException {
                log.in();
		synchronized (this) {
                        finishLocked(false);
		}
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
                log.in();trace.in();
                trace.disp("0, ", this);
// // if (singleton != null)
// System.err.println(this + ": singleton = " + singleton + " nCurrent " + nCurrent);
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

			setSingleton();
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


        /**
         * {@inheritDoc}
         */
        public synchronized void closeConnection(ReceiveQueue rq, Integer num) throws NetIbisException {
                //
                NetInput input = rq.input();
                if (input != null) {
                        input.close(num);
                }
        }


        public synchronized final void doClose(Integer num) throws NetIbisException {
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

        protected NetInput activeInput() throws NetIbisException {
                try {
                        ReceiveQueue  rq    = activeQueue;
                        NetInput      input = rq.input;
                        if (input == null) {
                                throw new NetIbisClosedException("input closed");
                        }
                        return input;
                } catch (NullPointerException e) {
                        throw new NetIbisClosedException(e);
                }
        }

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                NetReceiveBuffer b = activeInput().readByteBuffer(expectedLength);
                log.out();
                return b;
        }

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                log.in();
                activeInput().readByteBuffer(buffer);
                log.out();
        }

	public boolean readBoolean() throws NetIbisException {
                log.in();
                boolean v = activeInput().readBoolean();
                log.out();
                return v;
        }

	public byte readByte() throws NetIbisException {
                log.in();
                byte v = activeInput().readByte();
                log.out();
                return v;
        }

	public char readChar() throws NetIbisException {
                log.in();
                char v = activeInput().readChar();
                log.out();
                return v;
        }

	public short readShort() throws NetIbisException {
                log.in();
                short v = activeInput().readShort();
                log.out();
                return v;
        }

	public int readInt() throws NetIbisException {
                log.in();
                int v = activeInput().readInt();
                log.out();
                return v;
        }

	public long readLong() throws NetIbisException {
                log.in();
                long v = activeInput().readLong();
                log.out();
                return v;
        }

	public float readFloat() throws NetIbisException {
                log.in();
                float v = activeInput().readFloat();
                log.out();
                return v;
        }

	public double readDouble() throws NetIbisException {
                log.in();
                double v = activeInput().readDouble();
                log.out();
                return v;
        }

	public String readString() throws NetIbisException {
                log.in();
                String v = (String)activeInput().readString();
                log.out();
                return v;
        }

	public Object readObject() throws NetIbisException {
                log.in();
                Object v = activeInput().readObject();
                log.out();
                return v;
        }

	public void readArray(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(byte [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(char [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(short [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(int [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(long [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(float [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(double [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

	public void readArray(Object [] b, int o, int l) throws NetIbisException {
                log.in();
                activeInput().readArray(b, o, l);
                log.out();
        }

}
