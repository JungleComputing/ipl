package ibis.ipl.impl.net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Collection;
import java.util.Iterator;
import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public abstract class NetPoller extends NetInput {

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
        protected volatile Integer       activeNum   = null;
        protected volatile Thread    activeUpcallThread = null;

	/**
	 * Count the number of application threads that are blocked in a poll
	 */
	protected int		waitingThreads = 0;


	/**
	 * Upcall thread
	 */
	//private UpcallThread	upcallThread = null;


        /**
         * The first queue that should be poll first next time we have to poll the queues.
         */
        private int             firstToPoll  = 0;
        private boolean         upcallMode = false;

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
                inputTable = new Hashtable();
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
                log.in();
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
                                q = new ReceiveQueue(ni);
                                inputTable.put(key, q);

                                upcallMode = (upcallFunc != null);

                                if (upcallMode) {
                                        ni.setupConnection(cnx, this);
                                } else {
                                        ni.setupConnection(cnx, null);
                                }

                                if (NetReceivePort.useBlockingPoll) {
                                        if (upcallMode) {
                                                q.setName("GenPoller queue for input " + ni);
                                        } else {
                                                q.setName("GenPoller threaded queue for input " + ni);
                                                q.start();
                                        }
                                        /*
                                        if (upcallFunc != null && upcallThread == null) {
                                                startUpcallThread();
                                                upcallThread = new UpcallThread();
                                                upcallThread.setName(this + " - upcall thread");
                                                upcallThread.start();
                                        }
                                        */
                                } else {
                                        q.setName("GenPoller queue for input " + ni);
                                }
                                wakeupBlockedReceiver();
                        }
                }
                log.out();
	}


	/**
	 * Call this from inputUpcall when a message drifts up.
	 * Sets activeQueue to the value indicated by spn.
	 */
	protected abstract void selectInput(Integer spn)
                throws NetIbisClosedException;


        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                log.in("spn = "+spn);
                Thread me = Thread.currentThread();

                log.disp("waiting for sync");
                synchronized(this) {
                        while (activeQueue != null) {
                                try {
                                        log.disp("activeQueue is not null, waiting");
                                        wait();
                                        log.disp("woke up");
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }

                        log.disp("setting activeQueue, spn = "+spn);
                        selectInput(spn);

                        activeNum = spn;
                        activeUpcallThread = me;
                }

                log.disp("upcall--> spn = "+spn);
                upcallFunc.inputUpcall(this, spn);
                log.disp("upcall<-- spn = "+spn);

                synchronized(this) {
                        if (activeQueue != null && activeQueue.input == input && activeUpcallThread == me) {
                                finish();
                        }
                }
                log.out("spn = "+spn);
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

	protected final class ReceiveQueue extends Thread {

                private NetInput	input     = null;
                private boolean		stopped   = false;
                private Integer		activeNum = null;
                private boolean		finished  = false;

                ReceiveQueue(NetInput input) {
                        this.input = input;
                }

                public Integer activeNum() {
                        return activeNum;
                }

                public NetInput input() {
                        return input;
                }

                public void run() {
                        log.in();
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
                                                log.disp(this + ": NetPoller queue thread poll returns " + activeNum);

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
                                        //
                                        disp.disp(e.getMessage());
                                }
                        }
                        log.out();
                }


                /* Call this from synchronized (NetPoller.this) */
                Integer poll() throws NetIbisException {
                        log.in();
                        if (! NetReceivePort.useBlockingPoll && activeNum == null) {
                                activeNum = input.poll(false);
                        }

                        Integer spn = activeNum;

                        if (activeNum != null) {
                                activeNum = null;
                        }
                        log.out();

                        return spn;
                }



                /* Call this from synchronized (NetPoller.this) */
                public void finish() throws NetIbisException {
                        log.in();
                        if (!upcallMode) {
                                input.finish();
                                finished = true;
                                NetPoller.this.notifyAll();
                        }

                        log.out();
                }


                void free() throws NetIbisException {
                        log.in();
                        try {
                                stopped = true;
                                // input.interruptPoll();
                                // join(0);
                                join(100);
                        } catch (InterruptedException e) {
                                throw new NetIbisException(e);
                        }
                        log.out();
                }

	}


	private void wakeupBlockedReceiver() {
                log.in();
                if (waitingThreads > 0) {
                        notifyAll();
                }
                log.out();
	}


	private void blockReceiver() {
                log.in();
                waitingThreads++;
                try {
                        wait();
                } catch (InterruptedException e) {
                        // Ignore (as usual)
                }
                waitingThreads++;
                log.out();
	}


	/**
	 * Called from poll() when the input indicated by ni has a message
	 * to receive.
	 * Set the state local to your implementation here.
	 */
	protected abstract void selectConnection(ReceiveQueue rq);


        protected void initReceive(Integer num) {
                //
        }

	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 */
	public Integer doPoll(boolean block) throws NetIbisException {
                log.in();
                if (activeQueue != null) {
                        throw new NetIbisException("Call message.finish before calling Net.poll");
                }
                if (activeNum != null) {
                        throw new NetIbisException("Call message.finish before calling Net.poll");
                }

                synchronized (this) {
                        while (true) {
                                final Collection c = inputTable.values();
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

                                                        Integer      spn = null;

                                                        if ((spn = rq.poll()) != null) {
                                                                activeQueue = rq;
                                                                activeNum   = spn;

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
                                                        Integer      spn = null;

                                                        if ((spn = rq.poll()) != null) {
                                                                activeQueue = rq;
                                                                activeNum   = spn;

                                                                selectConnection(rq);
                                                                break dummy;
                                                        }
                                                }

                                        } while (false);


                                        firstToPoll++;

                                        if (activeNum != null) {
                                                log.out();
                                                return activeNum;
                                        }

                                        if (! block) {
                                                break;
                                        }
                                }

                                blockReceiver();
                        }
                }

                /* break because ! block && activeNum == null */

                Thread.yield();
                log.out();

                return activeNum;
	}


	/**
	 * {@inheritDoc}
	 */
	public void doFinish() throws NetIbisException {
                log.in();
                synchronized(this) {
                        activeQueue.finish();
                        activeQueue = null;
                        activeNum   = null;
                        activeUpcallThread = null;
                        notifyAll();
                }
                log.out();
	}


	/**
	 * {@inheritDoc}
	 */
	public void doFree() throws NetIbisException {
                log.in();
		if (inputTable != null) {
			Iterator i = inputTable.values().iterator();

			if (inputTable.values().size() == 1) {
                                log.disp("Pity, missed the chance of a blocking NetPoller without thread switch");
			} else {
                                log.disp("No chance of a blocking NetPoller without thread switch; size " + inputTable.values().size());
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

                log.out();
	}


        public abstract void closeConnection(ReceiveQueue rq, Integer num) throws NetIbisException;

        public synchronized final void doClose(Integer num) throws NetIbisException {
                log.in();
		if (inputTable != null) {
                        ReceiveQueue rq = (ReceiveQueue)inputTable.get(num);
                        closeConnection(rq, num);

			NetInput input = rq.input;
                        input.close(num);
                        inputTable.remove(num);

                        if (activeQueue == rq) {
                                activeQueue = null;
                                activeNum   = null;
                                activeUpcallThread = null;
                                notifyAll();
                        }

                }
                log.out();
        }


        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                log.in();
                NetReceiveBuffer b = activeQueue.input.readByteBuffer(expectedLength);
                log.out();
                return b;
        }

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                log.in();
                activeQueue.input.readByteBuffer(buffer);
                log.out();
        }

	public boolean readBoolean() throws NetIbisException {
                log.in();
                boolean v = activeQueue.input.readBoolean();
                log.out();
                return v;
        }

	public byte readByte() throws NetIbisException {
                log.in();
                byte v = activeQueue.input.readByte();
                log.out();
                return v;
        }

	public char readChar() throws NetIbisException {
                log.in();
                char v = activeQueue.input.readChar();
                log.out();
                return v;
        }

	public short readShort() throws NetIbisException {
                log.in();
                short v = activeQueue.input.readShort();
                log.out();
                return v;
        }

	public int readInt() throws NetIbisException {
                log.in();
                int v = activeQueue.input.readInt();
                log.out();
                return v;
        }

	public long readLong() throws NetIbisException {
                log.in();
                long v = activeQueue.input.readLong();
                log.out();
                return v;
        }

	public float readFloat() throws NetIbisException {
                log.in();
                float v = activeQueue.input.readFloat();
                log.out();
                return v;
        }

	public double readDouble() throws NetIbisException {
                log.in();
                double v = activeQueue.input.readDouble();
                log.out();
                return v;
        }

	public String readString() throws NetIbisException {
                log.in();
                String v = (String)activeQueue.input.readString();
                log.out();
                return v;
        }

	public Object readObject() throws NetIbisException {
                log.in();
                Object v = activeQueue.input.readObject();
                log.out();
                return v;
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceBoolean(b, o, l);
                log.out();
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceByte(b, o, l);
                log.out();
        }

	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceChar(b, o, l);
                log.out();
        }

	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceShort(b, o, l);
                log.out();
        }

	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceInt(b, o, l);
                log.out();
        }

	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceLong(b, o, l);
                log.out();
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceFloat(b, o, l);
                log.out();
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceDouble(b, o, l);
                log.out();
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                log.in();
                activeQueue.input.readArraySliceObject(b, o, l);
                log.out();
        }

}
