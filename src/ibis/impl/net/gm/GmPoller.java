package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

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
	int			signals;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GmPoller(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
		upcallModeAllowed = false;
                gmDriver = (Driver)driver;
		lockIds = new int[1];
		lockIds[0] = 0; // main lock
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();

		GmInput ni = new GmInput(type, driver, null);
		Integer num = cnx.getNum();
		setupConnection(cnx, num, ni);

		int[] _lockIds = new int[lockIds.length + 1];
		System.arraycopy(lockIds, 0, _lockIds, 0, lockIds.length - 1);
		lockIds = _lockIds;
		lockIds[lockIds.length - 1] = lockIds[lockIds.length - 2];
		lockIds[lockIds.length - 2] = ni.getLockId();
System.err.print("Now lockIds := [");
for (int i = 0; i < lockIds.length; i++) {
    System.err.print(lockIds[i] + ",");
}
System.err.println("]");

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

		if (pumpedIds != null) {
System.err.println("**** Connection established while we are polling. Signal the poller thread!!!!");
System.err.println("unlock(" + pumpedIds[0] + ")");
		    gmDriver.gmLockArray.unlock(pumpedIds[0]);
		    signals++;
		}

                log.out();
	}


	protected void initReceive(Integer num) throws NetIbisException {
	    ((GmInput)activeInput()).initReceive(num);
	}


	public Integer doPoll(boolean block) throws NetIbisException {
	    /* Find something to resemble this:
	    if (spn == nul) {
		    return null;
	    }
	    */

	    int result = -1;

	    while (true) {

		synchronized (this) {
		    pumpedIds = lockIds;
		}

		if (block) {
		    result = gmDriver.blockingPump(lockIds);
		} else {
		    result = gmDriver.tryPump(lockIds);
		} 
		if (result == -1) {
		    System.err.println("poll failed");
		    return null;
		}

		synchronized (this) {
		    pumpedIds = null;
		    if (signals > 0) {
			signals--;
		    } else {
			break;
		    }
		}
	    }

	    activeQueue = rq[result];
	    selectConnection(activeQueue);

	    return spn[result];
	}

}
