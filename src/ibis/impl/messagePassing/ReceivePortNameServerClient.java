package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import ibis.ipl.impl.generic.ConditionVariable;

final class ReceivePortNameServerClient
    implements ReceivePortNameServerProtocol {

    static {
	if (ReceivePortNameServerProtocol.DEBUG) {
	    if (Ibis.myIbis.myCpu == 0) {
		System.err.println("Turn on ReceivePortNS.DEBUG");
	    }
	}
    }


    class Bind implements PollClient {

	PollClient next;
	PollClient prev;

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
	    ns_done.cv_wait(timeout);
	}

	Thread me;

	public Thread thread() {
	    return me;
	}

	public void setThread(Thread thread) {
	    me = thread;
	}

	boolean	ns_busy = false;
	ConditionVariable	ns_free = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

	boolean	ns_is_done;
	ConditionVariable	ns_done = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
	boolean bound;

	void bind(String name, ibis.ipl.impl.messagePassing.ReceivePortIdentifier id) throws IbisIOException {

	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println("Try to bind ReceivePortId " + id + " ibis " + id.ibis().name());
	    }

//	    if (! name.equals(id.name)) {
//		System.out.println("name = " + name);
//		System.out.println("id.name = " + id.name);
//		throw new IbisIOException("Corrupted ReceivePort name");
//	    }

	    // request a new Port.
	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    try {
		while (ns_busy) {
		    ns_free.cv_wait();
		}
		ns_busy = true;

		bound = false;
		if (ReceivePortNameServerProtocol.DEBUG) {
		    System.err.println(Thread.currentThread() + "Call this rp-ns bind() \"" + name + "\"");
		    Thread.dumpStack();
		}
		ns_bind(name, id.type, id.ibis().name(), id.cpu, id.port);
// System.err.println(Thread.currentThread() + "Called this rp-ns bind()" + this);

// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my bind reply");
		if (ReceivePortNameServerProtocol.DEBUG) {
		    if (bound) {
			System.err.println("******** Reply arrives early, bind=" + this);
		    }
		}
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, Poll.PREEMPTIVE);
// System.err.println(Thread.currentThread() + "Bind reply arrived, client woken up" + this);

		ns_busy = false;
		ns_free.cv_signal();
	    } finally {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    }
	}

    }

    /* Called from native */
    private void bind_reply() {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "Bind reply arrives, signal client" + this + " bind = " + bind);
	bind.bound = true;
	bind.ns_done.cv_signal();
    }

    native void ns_bind(String name,
			String type,
			String ibis_name,
			int port_cpu,
			int port_port);

    Bind bind = new Bind();

    public void bind(String name, ibis.ipl.impl.messagePassing.ReceivePortIdentifier id) throws IbisIOException {
	bind.bind(name, id);
    }


    class Lookup implements PollClient {

	PollClient next;
	PollClient prev;

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
	    ns_done.cv_wait(timeout);
	}

	Thread me;

	public Thread thread() {
	    return me;
	}

	public void setThread(Thread thread) {
	    me = thread;
	}

	boolean	ns_busy = false;
	ConditionVariable	ns_free = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

	boolean	ns_is_done;
	ConditionVariable	ns_done = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
	ibis.ipl.impl.messagePassing.ReceivePortIdentifier ri;

	private static final int BACKOFF_MILLIS = 1000;

	public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException {

	    if (ReceivePortNameServerProtocol.DEBUG) {
		System.err.println(Thread.currentThread() + "Lookup receive port \"" + name + "\"");
	    }
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: grab Ibis lock.....");

	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    while (ns_busy) {
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait until the previous client is finished" + this);
		ns_free.cv_wait();
	    }
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: set lookup.ns_busy" + this);
	    ns_busy = true;
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();

	    long start = System.currentTimeMillis();
	    long last_try = start - BACKOFF_MILLIS;
	    while (true) {
		long now = System.currentTimeMillis();
		if (timeout > 0 && now - start > timeout) {
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
		    ns_busy = false;
		    ns_free.cv_signal();
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
		    throw new IbisIOException("messagePassing.Ibis ReceivePort lookup failed");
		}
		if (now - last_try >= BACKOFF_MILLIS) {
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
// System.err.println("Got lock ...");
		    try {
			ri = null;
			ns_lookup(name);

// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my lookup \"" + name + "\" reply " + ns_done);
			ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, BACKOFF_MILLIS, Poll.PREEMPTIVE);
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Lookup reply says ri.cpu = " + ri.cpu + " ns_done = " + ns_done);

			if (ri != null && ri.cpu != -1) {
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: clear lookup.ns_busy" + this);
			    ns_busy = false;
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: signal potential waiters");
			    ns_free.cv_signal();
			    return ri;
			}
		    } finally {
// System.err.println("Releasing lock ...");
			ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
// System.err.println("Released lock ...");
		    }
		    last_try = System.currentTimeMillis();
		}
		/* Thread.yield(); */

		try {
		    Thread.sleep(BACKOFF_MILLIS);
		} catch (InterruptedException e) {
		    // Well, if somebody interrupts us, would there be news?
		}
	    }
	}
    }

    native void ns_lookup(String name);

    /* Called from native */
    private void lookup_reply(ibis.ipl.impl.messagePassing.ReceivePortIdentifier ri) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: lookup reply " + ri + " " + lookup.ns_done);
	lookup.ri = ri;
	lookup.ns_done.cv_signal();
    }

    Lookup lookup = new Lookup();

    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException {
	if (ReceivePortNameServerProtocol.DEBUG) {
	    System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + ": Do a ReceivePortId NS lookup(" + name + ", " + timeout + ") in " + lookup);
	}
	return lookup.lookup(name, timeout);
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident) throws IbisIOException {
	/* not implemented yet */
	return null;
    }

    native void ns_unbind(String public_name);

    void unbind(String name) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	ns_unbind(name);
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }
}
