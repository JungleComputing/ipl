package ibis.ipl.impl.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public abstract class NetPoller extends NetInput {

	private final static boolean DEBUG = true;

	/**
	 * The set of inputs.
	 */
        protected Hashtable inputTable  = null;

	/**
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;

	/**
	 * The input queue that was last sucessfully polled, or <code>null</code>.
	 */
	protected volatile ReceiveQueue  activeQueue = null;
        protected volatile Thread    activeUpcallThread = null;

	/**
	 * Count the number of application threads that are blocked in a poll
	 */
	protected int		waitingThreads;


	/**
	 * Upcall thread
	 */
	private UpcallThread	upcallThread;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public NetPoller(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
	    super(pt, driver, context);
	    inputTable = new Hashtable();
	}


	private class UpcallThread extends Thread {
	    
	    private volatile boolean end = false;

	    public void end() {
		end = true;
		this.interrupt();
	    }

	    public void run() {
		while (! end) {
		    try {
			while (poll(true /* block */) == null) {
			    // Go on polling
			}
			if (DEBUG) {
			    System.err.println(this + ": run.poll() succeeds, activeNum " + activeNum);
			}

			upcallFunc.inputUpcall(NetPoller.this, activeNum);
			activeNum = null;
		    } catch (NetIbisException e) {
			System.err.println(this + ": catch exception " + e);
			break;
		    }
		}
	    }
	}


	/**
	 * {@inheritDoc}
	 *
	 * Call this from setupConnection(cnx) in the subclass.
	 */
	protected void setupConnection(NetConnection cnx,
				       Object key,
				       NetInput ni)
		throws NetIbisException {

	    if (! NetReceivePort.useBlockingPoll && upcallFunc != null) {
		ni.setupConnection(cnx, this);
	    } else {
		ni.setupConnection(cnx, null);
	    }

	    /*
	     * Because a blocking poll can be pending while we want
	     * to connect, the ReceivePort's inputLock cannot be taken
	     * during a connect.
	     * This implies that the blocking poll _and_ setupConnection
	     * must protect the data structures.
	     */
	    synchronized (this) {
		ReceiveQueue q = (ReceiveQueue)inputTable.get(key);
		if (q == null) {
		    q = new ReceiveQueue(ni, cnx.getNum());
		    inputTable.put(key, q);

		    if (NetReceivePort.useBlockingPoll) {
			q.setName("GenPoller thread for input " + ni);
			q.start();

			if (upcallFunc != null && upcallThread == null) {
			    upcallThread = new UpcallThread();
			    upcallThread.setName(this + " - upcall thread");
			    upcallThread.start();
			} 
		    } else {
			q.setName("GenPoller queue for input " + ni);
		    }
		}
	    }
	}


	/**
	 * Call this from inputUpcall when a message drifts up.
	 * Sets activeQueue to the value indicated by spn.
	 */
	protected abstract void selectInput(Integer spn)
	    throws NetIbisClosedException;


        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
	    Thread me = Thread.currentThread();

	    if (DEBUG) {
		System.err.println(this + ": inputUpcall--> spn = "+spn);
	    }
	    synchronized(this) {
		while (activeQueue != null) {
		    try {
			wait();
		    } catch (InterruptedException e) {
			throw new NetIbisInterruptedException(e);
		    }
		}
		
		if (DEBUG) {
		    System.err.println(this + ": inputUpcall - setting activeQueue, spn = "+spn);
		}
		selectInput(spn);

		activeNum = spn;
		activeUpcallThread = me;
		//System.err.println(this + ": inputUpcall - setting activeQueue - ok, spn = "+spn);
	    }
	    
	    if (DEBUG) {
		System.err.println(this + ": inputUpcall - upcall--> spn = "+spn);
	    }
	    upcallFunc.inputUpcall(this, spn);
	    if (DEBUG) {
		System.err.println(this + ": inputUpcall - upcall<-- spn = "+spn);
	    }

	    synchronized(this) {
		if (activeQueue != null && activeQueue.input == input && activeUpcallThread == me) {
		    if (DEBUG) {
			System.err.println(this + ": inputUpcall - clearing activeQueue, spn = "+spn);
		    }
		    activeQueue = null;
		    activeNum   = null;
		    activeUpcallThread = null;
		    notifyAll();
		    //System.err.println(this + ": inputUpcall - clearing activeQueue - ok, spn = "+spn);
		}
	    }
	    if (DEBUG) {
		System.err.println(this + ": inputUpcall<-- spn = "+spn);
	    }
        }


	/*
	 * Blocking receive is implemented as follows.
	 * Each subInput has a thread that polls it. When the thread
	 * sees that a message has arrived in its subInput, it registers
	 * that in its state, signals any waiting application threads,
	 * and waits until the message is finished.
	 *
	 * The application thread that wants to perform a blocking receive
	 * queries the state of all poller threads. If one has a pending
	 * message, that subInput becomes the current input. The message
	 * is read in the usual fashion. At finish time, the poller thread
	 * is woken up to continue polling in its subInput.
	 * If there is no pending succeeded poll, the application thread waits.
	 *
	 * Performance optimization: if there is only one subInput, the roll
	 * of the poller thread can be taken by the application thread.
	 */

	protected class ReceiveQueue extends Thread {

	    public NetInput	input;

	    boolean		stopped;
	    Integer		spn;
	    Integer		activeNum;
	    boolean		finished;

	    ReceiveQueue(NetInput input, Integer spn) {
		this.input = input;
		this.spn   = spn;
	    }

	    public void run() {
		while (! stopped) {
		    try {
			Integer i;
			while ((i = input.poll(true /* block */)) == null) {
			    if (stopped) {
				return;
			    }
			}
// System.err.print("-");

			synchronized (NetPoller.this) {
			    activeNum = i;
			    if (DEBUG) {
				System.err.println(this + ": NetPoller queue thread poll returns " + activeNum);
			    }

			    wakeupBlockedReceiver();

			    while (! finished) {
				try {
				    NetPoller.this.wait();
				} catch (InterruptedException e) {
				    // Ignore
				}
			    }
			    finished = false;
			}
		    } catch (NetIbisException e) {
		    }
		}
	    }


	    /* Call this from synchronized (NetPoller.this) */
	    Integer poll() throws NetIbisException {
		if (! NetReceivePort.useBlockingPoll && activeNum == null) {
		    activeNum = input.poll(false);
		}

		Integer spn = activeNum;

		if (activeNum != null) {
		    activeNum = null;
		}

		return spn;
	    }



	    /* Call this from synchronized (NetPoller.this) */
	    public void finish() throws NetIbisException {
		input.finish();
		finished = true;
		NetPoller.this.notifyAll();
	    }


	    void free() throws NetIbisException {
		try {
		    stopped = true;
		    // input.interruptPoll();
		    // join(0);
		    join(100);
		} catch (InterruptedException e) {
		    throw new NetIbisException(e);
		}
	    }

	}


	private void wakeupBlockedReceiver() {
	    if (waitingThreads > 0) {
		notifyAll();
	    }
	}


	private void blockReceiver() {
// System.err.println(this + ": block...");
	    waitingThreads++;
	    try {
		wait();
	    } catch (InterruptedException e) {
		// Ignore (as usual)
	    }
	    waitingThreads++;
// System.err.println(this + ": unblock...");
	}


	/**
	 * Called from poll() when the input indicated by ni has a message
	 * to receive.
	 * Set the state local to your implementation here.
	 */
	protected abstract void selectConnection(ReceiveQueue ni);


	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 */
	public Integer poll(boolean block) throws NetIbisException {
	    if (activeQueue != null) {
		throw new NetIbisException("Call message.finish before calling Net.poll");
	    }
	    if (activeNum != null) {
		throw new NetIbisException("Call message.finish before calling Net.poll");
	    }

	    /* ToDo: fairness in the order in which the queues are polled
	     */
	    synchronized (this) {
		while (true) {
		    Iterator i = inputTable.values().iterator();
		    while (i.hasNext()) {
			ReceiveQueue ni = (ReceiveQueue)i.next();
			if (DEBUG) {
			    System.err.println(this + ": inspect ReceiveQueue " + ni + " has activeNum " + ni.activeNum);
			}

			Integer spn;
			if ((spn = ni.poll()) != null) {
			    if (DEBUG) {
				System.err.println(this + ": inspect ReceiveQueue " + ni + " has activeNum " + spn);
			    }
			    activeQueue = ni;
			    activeNum   = spn;

			    selectConnection(ni);
			    break;
			}
		    }

		    if (activeNum != null) {
			return activeNum;
		    }

		    if (! block) {
			break;
		    }

		    blockReceiver();
		}
	    }

	    /* break because ! block && activeNum == null */

	    Thread.yield();

	    return activeNum;
	}


	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
	    //System.err.println("NetPoller: finish-->");
// System.err.println(this + ": finish msg");
// System.err.print("_");
	    super.finish();
	    synchronized(this) {
		    activeQueue.finish();
		    activeQueue = null;
		    activeNum   = null;
		    activeUpcallThread = null;
		    notifyAll();
	    }
	    //System.err.println("NetPoller: finish<--");
	}


	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws NetIbisException {
                //System.err.println("NetPoller: free-->");
		if (inputTable != null) {
			Iterator i = inputTable.values().iterator();

			if (inputTable.values().size() == 1) {
			    System.err.println(this + ": Pity, missed the chance of a blocking NetPoller without thread switch");
			} else {
			    System.err.println(this + ": No chance of a blocking NetPoller without thread switch; size " + inputTable.values().size());
			}

			while (i.hasNext()) {
				ReceiveQueue q = (ReceiveQueue)i.next();
				NetInput ni = q.input;
				q.free();
				ni.free();
                                i.remove();
			}
		}

		synchronized(this) {
                        activeQueue = null;
                        activeNum   = null;
                        activeUpcallThread = null;

//                          while (activeQueue != null)
//                                  wait();
                }
                
		super.free();
                //System.err.println("NetPoller: free<--");
	}


        public synchronized void close(Integer num) throws NetIbisException {
                //System.err.println("NetPoller: close-->");
		if (inputTable != null) {
                        ReceiveQueue q = (ReceiveQueue)inputTable.get(num);
			NetInput input = q.input;
                        input.close(num);
                        inputTable.remove(num);

                        if (activeQueue == q) {
                                activeQueue = null;
                                activeNum   = null;
                                activeUpcallThread = null;
                                notifyAll();
                        }

                }
                //System.err.println("NetPoller: close<--");
        }
        

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                return activeQueue.input.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                activeQueue.input.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws NetIbisException {
                return activeQueue.input.readBoolean();
        }

	public byte readByte() throws NetIbisException {
                return activeQueue.input.readByte();
        }

	public char readChar() throws NetIbisException {
                return activeQueue.input.readChar();
        }

	public short readShort() throws NetIbisException {
                return activeQueue.input.readShort();
        }

	public int readInt() throws NetIbisException {
                return activeQueue.input.readInt();
        }

	public long readLong() throws NetIbisException {
                return activeQueue.input.readLong();
        }
	
	public float readFloat() throws NetIbisException {
                return activeQueue.input.readFloat();
        }

	public double readDouble() throws NetIbisException {
                return activeQueue.input.readDouble();
        }

	public String readString() throws NetIbisException {
                return (String)activeQueue.input.readString();
        }

	public Object readObject() throws NetIbisException {
                return activeQueue.input.readObject();
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceBoolean(b, o, l);
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceByte(b, o, l);
        }

	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceChar(b, o, l);
        }

	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceShort(b, o, l);
        }

	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceInt(b, o, l);
        }

	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceLong(b, o, l);
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceFloat(b, o, l);
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                activeQueue.input.readArraySliceObject(b, o, l);
        }

} 
