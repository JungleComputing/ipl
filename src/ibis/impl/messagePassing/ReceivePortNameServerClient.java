package ibis.impl.messagePassing;

import ibis.io.Conversion;
import ibis.util.ConditionVariable;

import java.io.IOException;

final class ReceivePortNameServerClient
    implements ReceivePortNameServerProtocol {

    static {
	if (ReceivePortNameServerProtocol.DEBUG) {
	    if (Ibis.myIbis.myCpu == 0) {
		System.err.println("Turn on ReceivePortNS.DEBUG");
	    }
	}
    }


    private class Bind implements PollClient {

	private PollClient next;
	private PollClient prev;

	public PollClient next() {
	    return next;
	}

	public PollClient prev() {
	    return prev;
	}

	public void setNext(PollClient c) {
	    next = c;
	}

	public void setPrev(PollClient c) {
	    prev = c;
	}

	public boolean satisfied() {
	    return bound;
	}

	public void wakeup() {
	    ns_done.cv_signal();
	}

	public void poll_wait(long timeout) {
	    try {
		ns_done.cv_wait(timeout);
	    } catch (InterruptedException e) {
		// ignore
	    }
	}

	private Thread me;

	public Thread thread() {
	    return me;
	}

	public void setThread(Thread thread) {
	    me = thread;
	}

	private boolean	ns_busy = false;
	private ConditionVariable	ns_free = Ibis.myIbis.createCV();

	private ConditionVariable	ns_done = Ibis.myIbis.createCV();
	private boolean bound;

	void bind(String name, ReceivePortIdentifier id)
		throws IOException {

	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println("Try to bind ReceivePortId " + id + " ibis " + id.ibis().name());
	    }

//	    if (! name.equals(id.name)) {
//		System.out.println("name = " + name);
//		System.out.println("id.name = " + id.name);
//		throw new StreamCorruptedException("Corrupted ReceivePort name");
//	    }

	    // request a new Port.
	    Ibis.myIbis.checkLockNotOwned();

	    Ibis.myIbis.lock();
	    try {
		while (ns_busy) {
		    try {
			ns_free.cv_wait();
		    } catch (InterruptedException e) {
			// ignore
		    }
		}
		ns_busy = true;

		bound = false;
		if (ReceivePortNameServerProtocol.DEBUG) {
		    System.err.println(Thread.currentThread() + "Call this rp-ns bind() \"" + name + "\"");
		    Thread.dumpStack();
		}

		ns_bind(name, id.getSerialForm());
// System.err.println(Thread.currentThread() + "Called this rp-ns bind()" + this);

// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my bind reply");
		if (ReceivePortNameServerProtocol.DEBUG) {
		    if (bound) {
			System.err.println("******** Reply arrives early, bind=" + this);
		    }
		}
		Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
		if (ReceivePortNameServerProtocol.DEBUG) {
		    System.err.println(Thread.currentThread() + "Bind reply arrived, client woken up" + this);
		}

		ns_busy = false;
		ns_free.cv_signal();
	    } finally {
		Ibis.myIbis.unlock();
	    }
	}

    }

    /* Called from native */
    private void bind_reply() {
	Ibis.myIbis.checkLockOwned();
	if (ReceivePortNameServerProtocol.DEBUG) {
	    System.err.println(Thread.currentThread() + "Bind reply arrives, signal client" + this + " bind = " + bind);
	}
	bind.bound = true;
	bind.ns_done.cv_signal();
    }

    private native void ns_bind(String name, byte[] recvPortId);

    private Bind bind = new Bind();

    public void bind(String name, ReceivePortIdentifier id)
	    throws IOException {
	bind.bind(name, id);
    }


    private class Lookup implements PollClient {

	private PollClient next;
	private PollClient prev;

	public PollClient next() {
	    return next;
	}

	public PollClient prev() {
	    return prev;
	}

	public void setNext(PollClient c) {
	    next = c;
	}

	public void setPrev(PollClient c) {
	    prev = c;
	}

	public boolean satisfied() {
	    return ri != null;
	}

	public void wakeup() {
	    ns_done.cv_signal();
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: signal waiter" + this);
	}

	public void poll_wait(long timeout) {
	    try {
		ns_done.cv_wait(timeout);
	    } catch (InterruptedException e) {
		// ignore
	    }
	}

	private Thread me;

	public Thread thread() {
	    return me;
	}

	public void setThread(Thread thread) {
	    me = thread;
	}

	private boolean	ns_busy = false;
	private ConditionVariable	ns_free = Ibis.myIbis.createCV();

	private ConditionVariable	ns_done = Ibis.myIbis.createCV();
	private ReceivePortIdentifier ri;

	private static final int BACKOFF_MILLIS = 1000;

	public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout)
		throws IOException {

	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Lookup receive port \"" + name + "\"");
	    }
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: grab Ibis lock.....");

	    Ibis.myIbis.lock();
	    while (ns_busy) {
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait until the previous client is finished" + this);
		try {
		    ns_free.cv_wait();
		} catch (InterruptedException e) {
		    // ignore
		}
	    }
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: set lookup.ns_busy" + this);
	    ns_busy = true;
	    Ibis.myIbis.unlock();

	    long start = System.currentTimeMillis();
	    long last_try = start - BACKOFF_MILLIS;
	    while (true) {
		long now = System.currentTimeMillis();
		if (timeout > 0 && now - start > timeout) {
		    Ibis.myIbis.lock();
		    ns_busy = false;
		    ns_free.cv_signal();
		    Ibis.myIbis.unlock();
		    throw new ibis.ipl.ConnectionTimedOutException("messagePassing.Ibis ReceivePort lookup failed");
		}
		if (now - last_try >= BACKOFF_MILLIS) {
		    Ibis.myIbis.lock();
		    if (ReceivePortNameServerProtocol.DEBUG) System.err.println("Got lock ...");
		    try {
			ri = null;
			ns_lookup(name);

			if (ReceivePortNameServerProtocol.DEBUG) System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my lookup \"" + name + "\" reply " + ns_done);
			Ibis.myIbis.waitPolling(this, BACKOFF_MILLIS, Poll.PREEMPTIVE);
			if (ReceivePortNameServerProtocol.DEBUG) System.err.println(Thread.currentThread() + "ReceivePortNSClient: Lookup reply says ri = " + ri + " ns_done = " + ns_done);

			if (ri != null && ri.cpu != -1) {
			    if (ReceivePortNameServerProtocol.DEBUG) System.err.println(Thread.currentThread() + "ReceivePortNSClient: clear lookup.ns_busy" + this);
			    ns_busy = false;
			    if (ReceivePortNameServerProtocol.DEBUG) System.err.println(Thread.currentThread() + "ReceivePortNSClient: signal potential waiters");
			    ns_free.cv_signal();
			    return ri;
			}
		    } finally {
			if (ReceivePortNameServerProtocol.DEBUG) System.err.println("Releasing lock ...");
			Ibis.myIbis.unlock();
			if (ReceivePortNameServerProtocol.DEBUG) System.err.println("Released lock ...");
		    }
		    last_try = System.currentTimeMillis();
		}
		/* Thread.yield(); */

		if (false) {
		    // I don't see why I should sleep here, when waitPolling
		    // also takes a timeout argument
		    try {
			Thread.sleep(BACKOFF_MILLIS);
		    } catch (InterruptedException e) {
			// Well, if somebody interrupts us, would there be news?
		    }
		}
	    }
	}
    }

    private native void ns_lookup(String name);

    /* Called from native */
    private void lookup_reply(byte[] rcvePortId) {
	Ibis.myIbis.checkLockOwned();
	if (ReceivePortNameServerProtocol.DEBUG) System.err.println(Thread.currentThread() + "ReceivePortNSClient: lookup reply " + rcvePortId + " " + lookup.ns_done);
	lookup.ri = null;
	if (rcvePortId != null) {
	    try {
		lookup.ri = (ReceivePortIdentifier)Conversion.byte2object(rcvePortId);
		lookup.ns_done.cv_signal();
	    } catch (ClassNotFoundException e) {
		System.err.println("Cannot deserialize ReceivePortId");
		Thread.dumpStack();
	    } catch (IOException e) {
		System.err.println("Cannot deserialize ReceivePortId");
		Thread.dumpStack();
	    }
	}
    }

    private Lookup lookup = new Lookup();

    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout)
	    throws IOException {
	if (ReceivePortNameServerProtocol.DEBUG) {
	    System.err.println(Ibis.myIbis.myCpu + ": Do a ReceivePortId NS lookup(" + name + ", " + timeout + ") in " + lookup);
	}
	return lookup.lookup(name, timeout);
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident)
	    throws IOException {
	/* not implemented yet */
	return null;
    }

    private native void ns_unbind(String public_name);

    void unbind(String name) {
	Ibis.myIbis.lock();
	if (ReceivePortNameServerProtocol.DEBUG) {
	    System.err.println(Ibis.myIbis.myCpu + ": Do an UNBIND of \"" + name + "\"");
	}
	ns_unbind(name);
	Ibis.myIbis.unlock();
    }
}
