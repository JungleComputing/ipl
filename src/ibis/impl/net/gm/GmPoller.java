package ibis.impl.net.gm;

import ibis.impl.net.InterruptedIOException;
import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * Provides a GM-specific multiple network input poller.
 *
 * The special thing here is to ensure:
 *  - for upcall mode:
 *    that there is only one thread that polls all our subinputs
 *    (i.s.o. one thread per subinput), i.e. one thread per ReceivePort
 *  - for downcall mode:
 *    that there are no threads to poll our subinputs: let the downcall
 *    thread perform a non-blocking poll of all subinputs
 *    (i.s.o. one thread per subinput)
 *
 * Locking: there is an object lock on this and the gm/Driver.gmAccessLock
 * lock. If both are required, synchronized (this) must be done first.
 */
public final class GmPoller extends NetPoller {

    private Driver	gmDriver;

    /*
     * These fields are protected by synchronized (this).
     */
    int[]		lockIds;
    Integer[]		spn;
    ReceiveQueue[]	rq;

    int			blockers;

    private boolean	ended = false;

    /**
     * Constructor.
     *
     * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
     * @param driver the driver of this poller.
     * @param context the context.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public GmPoller(NetPortType pt,
		    NetDriver driver,
		    String context,
		    NetInputUpcall inputUpcall)
	    throws IOException {
	super(pt, driver, context, false, inputUpcall);
	gmDriver = (Driver)driver;
	lockIds = new int[1];
	lockIds[0] = 0; // main lock
// System.err.println("************ Create a new GmPoller " + this + " upcallFunc " + upcallFunc);
    }


    protected NetInput newPollerSubInput(Object key, ReceiveQueue q)
	    throws IOException {
	return new GmInput(type, driver, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public void setupConnection(NetConnection cnx)
		throws IOException {
	log.in();

	Integer num = cnx.getNum();
	setupConnection(cnx, num);
	ReceiveQueue q = (ReceiveQueue)inputMap.get(num);
	GmInput ni = (GmInput)q.getInput();

	synchronized (this) {

	    int[] oldLockIds = lockIds;
	    int[] _lockIds = new int[lockIds.length + 1];
	    System.arraycopy(lockIds, 0, _lockIds, 0, lockIds.length - 1);
	    lockIds = _lockIds;
	    lockIds[lockIds.length - 1] = lockIds[lockIds.length - 2];
	    lockIds[lockIds.length - 2] = ni.getLockId();

	    Driver.verifyUnique(ni.getLockId());

// System.err.println(this + ": setup new connection; subInput " + ni + " lockId " + ni.getLockId());
	    if (false) {
		System.err.print(NetIbis.hostName() + " "
			// + this
			+ ": Now lockIds " + lockIds + " := [");
		for (int i = 0; i < lockIds.length; i++) {
		    System.err.print(lockIds[i] + ",");
		}
		System.err.println("]");
	    }

	    if (spn == null) {
		spn = new Integer[1];
		rq  = new ReceiveQueue[1];
	    } else {
		Integer[] _spn = new Integer[spn.length + 1];
		System.arraycopy(spn, 0, _spn, 0, spn.length);
		spn = _spn;
		ReceiveQueue[] _rq = new ReceiveQueue[spn.length + 1];
		System.arraycopy(rq, 0, _rq, 0, rq.length);
		rq = _rq;
	    }
	    spn[spn.length - 1] = num;
	    rq[spn.length - 1]  = (ReceiveQueue)inputMap.get(num);

	    /* This does a lazy allocation of an upcall thread.
	     * Multiple connections into this port, i.e. into this GmPoller,
	     * are serviced by one thread (unless the thread does a blocking
	     * upcall of course).
	     */
	    startUpcallThread();

	    Driver.gmAccessLock.lock();
	    try {
		Driver.interruptPump(oldLockIds);
	    } finally {
		Driver.gmAccessLock.unlock();
		log.out();
	    }

	    readBufferedSupported = true;

	    int _mtu = ni.getMaximumTransfertUnit();

	    if (mtu == 0  ||  mtu > _mtu) {
		mtu = _mtu;
	    }

// System.err.println(this + ": " + cnx.getServiceLink() + ": established connection");
	}
    }


    public void initReceive(Integer num) throws IOException {
	super.initReceive(num);
	((GmInput)activeInput()).initReceive(num);
    }


    public Integer doPoll(boolean block) throws IOException {
	/* Find something to resemble this:
	if (spn == nul) {
		return null;
	}
	*/
	log.in();

	int result = -1;

	Driver.gmAccessLock.lock();
	try {

	    boolean interrupted;
	    do {

		if (ended) {
		    throw new InterruptedIOException("Poller channel closed");
		}
		interrupted = false;
		if (block) {
// System.err.print("[B");
// for (int i = 0; i < lockIds.length - 1; i++) System.err.print(lockIds[i] + ",");
// synchronized (this) {
// blockers++;
// if (blockers > 1)
// System.err.println("Somebody polling concurrently with me!!!");
// }
		    try {
			result = Driver.blockingPump(lockIds);
// System.err.print("B");
// for (int i = 0; i < lockIds.length - 1; i++) System.err.print(lockIds[i] + ",");
// System.err.print("]=" + result);
		    } catch (InterruptedIOException e) {
			if (ended) {
			    return null;
			    // throw e;
			}

			// try once more
			interrupted = true;
			if (Driver.VERBOSE_INTPT) {
			    System.err.print(NetIbis.hostName() + " "
				    // + Thread.currentThread()
				    // + ": " + this
				    + ": ********** Catch InterruptedIOException");
			    System.err.print("; lockIds " + lockIds + " = {");
			    for (int i = 0; i < lockIds.length; i++) {
				System.err.print(lockIds[i] + ",");
			    }
			    System.err.println("}");
			}
// System.err.print("X");
// for (int i = 0; i < lockIds.length - 1; i++) System.err.print(lockIds[i] + ",");
// System.err.print("]");
		    }
// synchronized (this) {
// blockers--;
// }
		} else {
// System.err.print("[?");
		    result = Driver.tryPump(lockIds);
// System.err.print("?]");
		} 

	    } while (interrupted);

	    if (result == -1) {
		System.err.println("poll failed");
		return null;
	    }

	    if (Driver.TIMINGS) {
		Driver.t_wait_reply.stop();
		Driver.t_wait_service.start();
	    }

	} finally {
	    Driver.gmAccessLock.unlock();
	    log.out();
	}

	/* In fact, doing this 'synchronized' is not necessary because the
	 * caller of doPoll ensures single-threaded access to doPoll.
	 * But for consistency across pollers we do it anyway... */
	synchronized (this) {
	    activeQueue = rq[result];
	    selectConnection(activeQueue);
// System.err.println(Thread.currentThread() + ": " + this + ": return " + spn[result] + " = spn[" + result +"]");

	    return spn[result];
	}
    }

    protected void doClose(Integer num) throws IOException {
	super.doClose(num);
	ended = true;

	Driver.gmAccessLock.lock();
	try {
	    Driver.interruptPump(lockIds);
	} finally {
	    Driver.gmAccessLock.unlock();
	}
    }

}
