package ibis.impl.net.gm;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetPoller;
import ibis.impl.net.NetPortType;

import java.io.IOException;
import java.io.InterruptedIOException;

/**
 * Provides a GM-specific multiple network input poller.
 *
 * The special thing here is to ensure:
 *  - for upcall mode:
 *    that there is only one thread that polls all our subinputs
 *    (i.s.o. one thread per subinput)
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
	int[]			pumpedIds;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GmPoller(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context, false);
                gmDriver = (Driver)driver;
		lockIds = new int[1];
		lockIds[0] = 0; // main lock
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
                log.in();

		GmInput ni = new GmInput(type, driver, null);
		Integer num = cnx.getNum();
		setupConnection(cnx, num, ni);

		Driver.gmAccessLock.ilock(true);

		int[] _lockIds = new int[lockIds.length + 1];
		System.arraycopy(lockIds, 0, _lockIds, 0, lockIds.length - 1);
		lockIds = _lockIds;
		lockIds[lockIds.length - 1] = lockIds[lockIds.length - 2];
		lockIds[lockIds.length - 2] = ni.getLockId();
if (false) {
    System.err.print("Now lockIds := [");
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

		startUpcallThread();

		gmDriver.interruptPump();

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
		    try {
			result = gmDriver.blockingPump(lockIds);
		    } catch (InterruptedIOException e) {
			// try once more
			interrupted = true;
			System.err.println("********** Catch InterruptedIOException");
		    }
// System.err.print("B]=" + result);
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

	    activeQueue = rq[result];
	    selectConnection(activeQueue);
// System.err.println(Thread.currentThread() + ": " + this + ": return " + spn[result] + " = spn[" + result +"]");

	    return spn[result];
	}

}
