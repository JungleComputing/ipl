package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

public abstract class Poll implements Runnable {

    boolean MANTA_COMPILE;

    Thread	poller;
    PollClient waiting_threads;
    int		preemptive_pollers;
    Thread	peeker;

    protected Poll() {
	// Sun doesn't set java.compiler, so getProperty returns null --Rob
	String compiler = java.lang.System.getProperty("java.compiler");
	MANTA_COMPILE = compiler != null && compiler.equals("manta");

	peeker = new Thread(this);
//	manta.runtime.RuntimeSystem.DebugMe(peeker, this);
	peeker.setDaemon(true);
    }


    void wakeup() {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("Now start the poll peeker thread. Sure we need it (?)");
	}
	peeker.start();
    }


    private long poll_preempt;
    private long poll_non_preempt;
    private long poll_wait;
    private long poll_yield_non_preempt;
    private long poll_yield_preempt;
    private long poll_poll;
    private long poll_poll_direct;

    final static int polls_before_yield = 5000;	// 1; // 2000;

    private void insert(PollClient client) {
	client.setNext(waiting_threads);
	if (waiting_threads != null) {
	    waiting_threads.setPrev(client);
	}
	waiting_threads = client;
	client.setPrev(null);
// client.setThread(Thread.currentThread());
    }

    private void remove(PollClient client) {
	if (waiting_threads == client) {
	    waiting_threads = client.next();
	} else {
	    client.prev().setNext(client.next());
	}
	if (client.next() != null) {
	    client.next().setPrev(client.prev());
	}
    }


    protected abstract void msg_poll() throws IbisIOException;
    native void abort();

    void poll() throws IbisIOException {
	poll_poll_direct++;
	// long t = Ibis.currentTime();
	msg_poll();
	ibis.ipl.impl.messagePassing.Ibis.myIbis.inputStreamPoll();
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.tMsgPoll += Ibis.currentTime() - t;
    }

    void waitPolling(PollClient client, long timeout, boolean preempt)
	    throws IbisIOException {

	long t_start = 0;
	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}

	if (preempt) {
	    poll_preempt++;
	} else {
	    poll_non_preempt++;
	}
// System.err.println("  -]]]] " + Thread.currentThread() + " enter pre-poll; poller = " + poller);
	if (preempt) {
	    for (int i = 0; i < polls_before_yield; i++) {
		poll();
		// poll_poll++;
// System.err.println("  -]]]] " + Thread.currentThread() + " in pre-poll: call client.satisfied(); poller = " + poller);
		if (client.satisfied()) {
// System.err.println("  -]]]] " + Thread.currentThread() + " in pre-poll: client.satisfied() succeeds; poller = " + poller);
		    return;
		}
// System.err.println("  -]]]] " + Thread.currentThread() + " in pre-poll: client.satisfied() failed; poller = " + poller);
	    }
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG &&
		    preemptive_pollers > 0) {
		System.err.println("Gee, some other preemptive poller active");
	    }
	    preemptive_pollers++;
	}

	int polls = polls_before_yield;
	Thread me = Thread.currentThread();
// System.err.println("  -]]]] " + me + " enter poll; poller = " + poller);
// Thread.dumpStack();

	while (! client.satisfied()) {
	    if (timeout > 0) {
		if (System.currentTimeMillis() >= t_start + timeout) {
		    break;
		}
	    }

	    if (poller == null || preempt) {
		// OK, let me become poller
// if (poller == null) {
// System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + ":  -]]]] " + me + " become the poller, preempt = " + preempt + " previous poller " + poller);
// Thread.dumpStack();
// }
		poller = me;
	    }

	    if (poller == me) {
		poll();
		poll_poll++;
		if (client.satisfied()) {
		    break;
		}
		if (! preempt || --polls == 0) {
		    polls = polls_before_yield;
		    if (preempt) {
			poll_yield_preempt++;
		    } else {
			poll_yield_non_preempt++;
		    }
// if (! preempt) System.err.println("  -]]]] " + me + " yield in poll(), preempt = " + preempt);
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
		    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
		    if (true || MANTA_COMPILE || preempt) {
			Thread.yield();
		    } else {
			try {
			    Thread.sleep(0,1);
			} catch (InterruptedException e) {
			    /* Ignore */
			}
		    }
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
// if (! preempt) System.err.println("  -]]]] " + me + " back from yield in poll(), preempt = " + preempt);
		}
	    } else {
		poll_wait++;
		insert(client);
		client.poll_wait(timeout);
// System.err.println("  -]]]] " + me + " wakes up in poll, take a peek; poller = " + poller);
// for (PollClient c = waiting_threads; c != null; c = c.next()) {
    // System.err.println("    " + c.thread());
// }
		remove(client);
	    }
	}

	if (poller == me) {
	    // Quit being the poller
	    poller = null;
// System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + ":  -]]]] " + me + " quit being the poller");
	    if (waiting_threads != null) {
		// Wake up another poller thread
		waiting_threads.wakeup();
	    }
	}

if (preempt) {
preemptive_pollers--;
}
// System.err.println("  -]]]] " + me + " leave poll");
    }


    public void run() {
	System.err.println("Poll peeker lives");
	while (true) {
	    if (ibis.ipl.impl.messagePassing.Ibis.myIbis != null) {
		synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		    try {
// System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + " do a peeker poll...");
			poll();
		    } catch (IbisIOException e) {
			System.err.println("Poll peeker catches exception " + e);
		    }
		}
	    }
	    Thread.yield();
	}
    }


    void report() {
	System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu +
		": Poll: /preempt " + poll_preempt +
		" /non-preempt " + poll_non_preempt +
		" poll " + poll_poll +
		" (direct " + poll_poll_direct +
		") wait " + poll_wait +
		" yield/preempt " + poll_yield_preempt +
		" /non-preempt " + poll_yield_non_preempt);
    }

    void reset_stats() {
	poll_preempt = 0;
	poll_non_preempt = 0;
	poll_wait = 0;
	poll_yield_non_preempt = 0;
	poll_yield_preempt = 0;
	poll_poll = 0;
	poll_poll_direct = 0;
    }

    protected void finalize() {
	report();
    }

}
