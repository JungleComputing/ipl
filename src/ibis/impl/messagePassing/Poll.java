package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

public class Poll implements Runnable {

    final static int NON_POLLING = 1;
    final static int NON_PREEMPTIVE = NON_POLLING + 1;
    final static int PREEMPTIVE     = NON_PREEMPTIVE + 1;

    private static final boolean DEBUG = false;
    private static final boolean STATISTICS = true;
    private static final boolean NEED_POLLER_THREAD = true;
    private static final boolean NONPREEMPTIVE_MAY_POLL = false;
    private static final boolean PREEMPTIVE_MAY_POLL = true;

    final static int polls_before_yield = 500;	// 1; // 2000;

    boolean MANTA_COMPILE;

    Thread	poller;
    PollClient	waiting_threads;
    int		preemptive_waiters;
    int		preemptive_pollers;
    Thread	peeker;
    boolean	last_is_preemptive;
    boolean	comm_lives = true;

    protected Poll() {
	// Sun doesn't set java.compiler, so getProperty returns null --Rob
	String compiler = java.lang.System.getProperty("java.compiler");
	MANTA_COMPILE = compiler != null && compiler.equals("manta");

	peeker = new Thread(this, "Poll peeker");
	peeker.setDaemon(true);

	if (DEBUG) {
	    System.err.println("Turn on Poll.DEBUG");
	}
	if (STATISTICS) {
	    System.err.println("Turn on Poll.STATISTICS");
	}
	if (MANTA_COMPILE) {
	    System.err.println("Ibis/Panda knows this is Manta");
	}
    }


    void wakeup() {
	if (NEED_POLLER_THREAD) {
	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		System.err.println("Now start the poll peeker thread. Sure we need it (?)");
	    }
	    peeker.start();
	}
    }


    private long poll_preempt;
    private long poll_non_preempt;
    private long poll_wait;
    private long poll_yield_non_preempt;
    private long poll_yield_preempt;
    private long poll_poll;
    private long poll_poll_direct;
    private long poll_from_thread;

    private void insert(PollClient client) {
	client.setNext(waiting_threads);
	if (waiting_threads != null) {
	    waiting_threads.setPrev(client);
	}
	waiting_threads = client;
	client.setPrev(null);
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


    protected native boolean msg_poll() throws IbisIOException;

    native void abort();


    final boolean poll() throws IbisIOException {
	boolean result;

	if (STATISTICS) {
	    poll_poll_direct++;
	}
	
	result = msg_poll();
	return ibis.ipl.impl.messagePassing.Ibis.myIbis.inputStreamPoll() || result;
    }


    final void waitPolling(PollClient client, long timeout, int preempt)
	    throws IbisIOException {

	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	long t_start = 0;
	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}

	if (STATISTICS) {
	    if (preempt == PREEMPTIVE) {
		poll_preempt++;
	    } else if (preempt == NON_PREEMPTIVE) {
		poll_non_preempt++;
	    }
	}
	if (preempt == PREEMPTIVE) {
if (false)
	    for (int i = 0; i < polls_before_yield; i++) {
		poll();
		// poll_poll++;
		if (client.satisfied()) {
		    return;
		}
	    }
	    if (DEBUG) {
		if (preemptive_pollers > 0) {
		    System.err.println("Gee, some other preemptive poller active");
		    // Thread.dumpStack();
		}
	    }
	    if (STATISTICS) {
		preemptive_pollers++;
	    }
	}

	int polls = polls_before_yield;
	Thread me = Thread.currentThread();

	boolean go_to_sleep = false;

	while (! client.satisfied()) {
	    if (timeout > 0) {
		if (System.currentTimeMillis() >= t_start + timeout) {
		    break;
		}
	    }

	    if (! go_to_sleep && poller == null &&
		    ((PREEMPTIVE_MAY_POLL && preempt == PREEMPTIVE) ||
		     (NONPREEMPTIVE_MAY_POLL && preempt != NON_POLLING))) {
		// OK, let me become poller
		poller = me;
	    }

	    if (poller == me) {
		boolean poll_succeeded = poll();
		if (STATISTICS) {
		    poll_poll++;
		}
		if (client.satisfied()) {
		    break;
		}

		if (--polls == 0 || poll_succeeded) {
		    polls = polls_before_yield;
		    // polls = 1;

		    if (NONPREEMPTIVE_MAY_POLL && preempt != PREEMPTIVE) {
			/* If this is a nonpreemptive thread, polling for one
			 * slice is (more than) enough. Go to sleep. */
			go_to_sleep = true;
			poller = null;
		    }

		    if (STATISTICS) {
			if (preempt == PREEMPTIVE) {
			    poll_yield_preempt++;
			} else {
			    if (DEBUG && preemptive_pollers > 0) {
				System.err.println("Am non-preemptive but I seem to preempt a preemptive poller");
			    }
			    poll_yield_non_preempt++;
			}
		    }

		    boolean prev_last = last_is_preemptive;
		    last_is_preemptive = (preempt == PREEMPTIVE);

		    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();

		    if (! MANTA_COMPILE &&
			    NONPREEMPTIVE_MAY_POLL &&
			    preempt != PREEMPTIVE && prev_last) {
			try {
			    Thread.sleep(0,1);
			} catch (InterruptedException e) {
			}
		    } else {
// System.err.println("poll_succeeded = " + poll_succeeded);
			Thread.yield();
		    }

		    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
		}

	    } else {
		if (STATISTICS) {
		    poll_wait++;
		}
		if (preempt == PREEMPTIVE) {
		    preemptive_waiters++;
		}
		if (NONPREEMPTIVE_MAY_POLL || preempt == PREEMPTIVE) insert(client);
		client.poll_wait(timeout);
		if (NONPREEMPTIVE_MAY_POLL || preempt == PREEMPTIVE) remove(client);
		if (preempt == PREEMPTIVE) {
		    preemptive_waiters--;
		}
	    }
	}

	if (poller == me) {
	    // Quit being the poller
	    poller = null;
	    if (waiting_threads != null &&
		    (NONPREEMPTIVE_MAY_POLL || preemptive_waiters > 0)) {
		// Wake up another poller thread
		waiting_threads.wakeup();
	    } else {
		last_is_preemptive = false;
	    }
	}

	if (STATISTICS && preempt == PREEMPTIVE) {
	    preemptive_pollers--;
	}
    }


    public void run() {
	System.err.println("Poll peeker lives");
	while (comm_lives) {
	    if (ibis.ipl.impl.messagePassing.Ibis.myIbis != null) {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
// System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + " do a peeker poll...");
		try {
		    poll();
		    if (STATISTICS) {
			poll_from_thread++;
		    }
		} catch (IbisIOException e) {
		    System.err.println("Poll throws " + e);
		    e.printStackTrace(System.err);
		}
		ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    }
	    if (! MANTA_COMPILE) {
		try {
		    Thread.sleep(0,1);
		} catch (InterruptedException e) {
		}
	    } else {
		Thread.yield();
	    }
	}
    }


    void report(java.io.PrintStream out) {
	if (STATISTICS) {
	    out.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu +
		    ": Poll: /preempt " + poll_preempt +
		    " /non-preempt " + poll_non_preempt +
		    " poll " + poll_poll +
		    " (direct " + poll_poll_direct +
		    ") wait " + poll_wait +
		    " yield/preempt " + poll_yield_preempt +
		    " /non-preempt " + poll_yield_non_preempt +
		    " poller thread poll " + poll_from_thread);
	}
    }


    void reset_stats() {
	if (STATISTICS) {
	    System.err.println("Reset Ibis.Poll statistics");
	    poll_preempt = 0;
	    poll_non_preempt = 0;
	    poll_wait = 0;
	    poll_yield_non_preempt = 0;
	    poll_yield_preempt = 0;
	    poll_poll = 0;
	    poll_poll_direct = 0;
	    poll_from_thread = 0;
	}
    }

    protected void finalize() {
	comm_lives = false;
	report(System.out);
    }

}
