package ibis.impl.net.gm;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;
import ibis.impl.net.InterruptedIOException;

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
 */
public final class GmPoller extends NetPoller {

        private Driver		gmDriver;
	int[]			lockIds;
	Integer[]		spn;
	ReceiveQueue[]		rq;

	int			blockers;

	/**
	 * Constructor.
	 *
	 * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param driver the driver of this poller.
	 * @param context the context.
	 */
	public GmPoller(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context, false);
                gmDriver = (Driver)driver;
		lockIds = new int[1];
		lockIds[0] = 0; // main lock
// System.err.println("************ Create a new GmPoller " + this + " upcallFunc " + upcallFunc);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();

		GmInput ni = new GmInput(type, driver, null);
		Integer num = cnx.getNum();
		setupConnection(cnx, num, ni);

		Driver.gmAccessLock.lock(true);

		int[] oldLockIds = lockIds;
		int[] _lockIds = new int[lockIds.length + 1];
		System.arraycopy(lockIds, 0, _lockIds, 0, lockIds.length - 1);
		lockIds = _lockIds;
		lockIds[lockIds.length - 1] = lockIds[lockIds.length - 2];
		lockIds[lockIds.length - 2] = ni.getLockId();

		Driver.verifyUnique(ni.getLockId());

// System.err.println(this + ": setup new connection; subInput " + ni + " lockId " + ni.getLockId());
		if (false) {
		    System.err.print(NetIbis.poolInfo.hostName() + " "
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

		gmDriver.interruptPump(oldLockIds);

		readBufferedSupported = true;

		Driver.gmAccessLock.unlock();
// System.err.println(this + ": " + cnx.getServiceLink() + ": established connection");

                log.out();
	}


	protected void initReceive(Integer num) throws IOException {
	    ((GmInput)activeInput()).initReceive(num);
	}


	public Integer doPoll(boolean block) throws IOException {
	    /* Find something to resemble this:
	    if (spn == nul) {
		    return null;
	    }
	    */

	    int result = -1;

	    boolean interrupted;
	    do {

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
			gmDriver.gmAccessLock.lock(false);
			int interrupts = gmDriver.interrupts();
			int[] lockIds = this.lockIds;
			gmDriver.gmAccessLock.unlock();
			result = gmDriver.blockingPump(interrupts, lockIds);
// System.err.print("B");
// for (int i = 0; i < lockIds.length - 1; i++) System.err.print(lockIds[i] + ",");
// System.err.print("]=" + result);
		    } catch (InterruptedIOException e) {
			// try once more
			synchronized (this) {
			    // synchronized to ensure we get an up-to-date value
			    // for lockIds
			}
			interrupted = true;
			System.err.print(NetIbis.poolInfo.hostName() + " "
				// + Thread.currentThread()
				// + ": " + this
				+ ": ********** Catch InterruptedIOException");
			System.err.print("; lockIds " + lockIds + " = {");
			for (int i = 0; i < lockIds.length; i++) {
			    System.err.print(lockIds[i] + ",");
			}
			System.err.println("}");
// System.err.print("X");
// for (int i = 0; i < lockIds.length - 1; i++) System.err.print(lockIds[i] + ",");
// System.err.print("]");
		    }
// synchronized (this) {
// blockers--;
// }
		} else {
// System.err.print("[?");
		    result = gmDriver.tryPump(lockIds);
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

	    synchronized (this) {
		activeQueue = rq[result];
		selectConnection(activeQueue);
// System.err.println(Thread.currentThread() + ": " + this + ": return " + spn[result] + " = spn[" + result +"]");

		return spn[result];
	    }
	}

}
