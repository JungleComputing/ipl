package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;
import ibis.ipl.ConditionVariable;

public abstract class ReceivePortNameServerClient
    implements ReceivePortNameServerProtocol {

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
	ConditionVariable	ns_free = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);

	boolean	ns_is_done;
	ConditionVariable	ns_done = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
	boolean bound;

	void bind(String name, ibis.ipl.impl.messagePassing.ReceivePortIdentifier id) throws IbisIOException {

	    if (! name.equals(id.name)) {
		throw new IbisIOException("Corrupted ReceivePort name");
	    }

	    // request a new Port.
	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	    synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		while (ns_busy) {
		    ns_free.cv_wait();
		}
		ns_busy = true;

		bound = false;
// System.err.println(Thread.currentThread() + "Call this rp-ns bind() \"" + name + "\"");
		ns_bind(id.name, id.type, id.cpu, id.port);
// System.err.println(Thread.currentThread() + "Called this rp-ns bind()" + this);

// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my bind reply");
		ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, 0, true);
// System.err.println(Thread.currentThread() + "Bind reply arrived, client woken up" + this);

		ns_busy = false;
		ns_free.cv_signal();
	    }
	}

    }

    /* Called from native */
    private void bind_reply() {
	// assume already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "Bind reply arrives, signal client" + this);
	bind.bound = true;
	bind.ns_done.cv_signal();
    }

    protected abstract void ns_bind(String name,
				String type,
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
	ConditionVariable	ns_free = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);

	boolean	ns_is_done;
	ConditionVariable	ns_done = new ConditionVariable(ibis.ipl.impl.messagePassing.Ibis.myIbis);
	ibis.ipl.impl.messagePassing.ReceivePortIdentifier ri;

	private static final int BACKOFF_MILLIS = 1000;

	public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException {

// System.err.println(Thread.currentThread() + "Lookup receive port \"" + name + "\"");
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: grab Ibis lock.....");

	    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	    synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		while (ns_busy) {
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait until the previous client is finished" + this);
		    ns_free.cv_wait();
		}
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: set lookup.ns_busy" + this);
		ns_busy = true;
	    }

	    long start = System.currentTimeMillis();
	    long last_try = start - BACKOFF_MILLIS;
	    while (true) {
		long now = System.currentTimeMillis();
		if (timeout > 0 && now - start > timeout) {
		    throw new IbisIOException("messagePassing.Ibis ReceivePort lookup failed");
		}
		if (now - last_try >= BACKOFF_MILLIS) {
		    synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
			ri = null;
			ns_lookup(name);

// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Wait for my lookup \"" + name + "\" reply " + ns_done);
			ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, BACKOFF_MILLIS, true);
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: Lookup reply says ri.cpu = " + ri.cpu + " ns_done = " + ns_done);

			if (ri.cpu != -1) {
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: clear lookup.ns_busy" + this);
			    ns_busy = false;
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: signal potential waiters");
			    ns_free.cv_signal();
			    return ri;
			}
		    }
		    last_try = System.currentTimeMillis();
		}
		Thread.yield();

		/*
		try {
		    Thread.sleep(BACKOFF_MILLIS);
		} catch (InterruptedException e) {
		    // Well, if somebody interrupts us, would there be news?
		}
		*/
	    }
	}

    }

    protected abstract void ns_lookup(String name);

    /* Called from native */
    private void lookup_reply(ibis.ipl.impl.messagePassing.ReceivePortIdentifier ri) {
	// assume already taken: synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis)
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
// System.err.println(Thread.currentThread() + "ReceivePortNSClient: lookup reply " + ri + " " + lookup.ns_done);
	lookup.ri = ri;
	lookup.ns_done.cv_signal();
    }

    Lookup lookup = new Lookup();

    public ibis.ipl.ReceivePortIdentifier lookup(String name, long timeout) throws IbisIOException {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + ": Do a ReceivePortId NS lookup(" + name + ", " + timeout + ") in " + lookup);
	}
	return lookup.lookup(name, timeout);
    }

    public ibis.ipl.ReceivePortIdentifier[] query(ibis.ipl.IbisIdentifier ident) throws IbisIOException {
	/* not implemented yet */
	return null;
    }

    protected abstract void ns_unbind(String public_name);

    void unbind(String name) throws IbisIOException {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();

	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    ns_unbind(name);
	}
    }
}
