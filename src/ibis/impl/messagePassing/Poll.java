package ibis.impl.messagePassing;

import ibis.util.TypedProperties;

import java.io.IOException;

/**
 * Poll the native network. All kinds of polling heuristics are implemented.
 */
final public class Poll implements Runnable {

    final static int NON_POLLING = 1;
    final static int NON_PREEMPTIVE = NON_POLLING + 1;
    final static int PREEMPTIVE     = NON_PREEMPTIVE + 1;

    private final static String preempt(int preempt) {
	switch (preempt) {
	    case NON_POLLING:		return "NON_POLLING";
	    case NON_PREEMPTIVE:	return "NON_PREEMPTIVE";
	    case PREEMPTIVE:		return "PREEMPTIVE";
	    default:			return "ERROR-PREEMPTIVE";
	}
    }

    private static final boolean DEBUG = false;
    private static final boolean STATISTICS = false;
    private static final boolean NEED_POLLER_THREAD = true;
    private static final boolean NONPREEMPTIVE_MAY_POLL = false;
    private static final boolean PREEMPTIVE_MAY_POLL = true;

    private static final int DEFAULT_YIELD_POLLS = 500;	// 1; // 2000;
    private static final int POLLS_BEFORE_YIELD;

    static final boolean USE_SLEEP_FOR_YIELD;

    private Thread	poller;
    private PollClient	waiting_threads;
    private int		preemptive_waiters;
    private int		preemptive_pollers;
    private Thread	peeker;
    private boolean	last_is_preemptive;
    private boolean	comm_lives = true;

    static {
	int  polls = DEFAULT_YIELD_POLLS;

	String envPoll = System.getProperty(MPProps.s_polls_yield);
	if (envPoll != null) {
	    polls = Integer.parseInt(envPoll);
	}
	POLLS_BEFORE_YIELD = polls;

	USE_SLEEP_FOR_YIELD = ! TypedProperties.booleanProperty(MPProps.s_yield)
		&& ! TypedProperties.stringProperty("java.compiler", "manta");

	if (Ibis.myIbis.myCpu == 0) {
	    if (DEBUG) {
		System.err.println("Turn on Poll.DEBUG");
	    }
	    if (DEBUG) {
		if (NEED_POLLER_THREAD) {
		    System.err.println("Poll: use a poll peeker thread. Sure we need it (?)");
		} else {
		    System.err.println("Poll: don't start the poll peeker thread. Sure we don't need it?");
		}
	    }
	    if (STATISTICS) {
		System.err.println("Turn on Poll.STATISTICS");
	    }
	    if (TypedProperties.stringProperty("java.compiler", "manta")) {
		System.err.println("Ibis/Panda knows this is Manta");
	    }
	}
    }


    void wakeup() {
	if (NEED_POLLER_THREAD) {
	    peeker = new Thread(this, "Poll peeker");
	    peeker.setDaemon(true);

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


    private native boolean msg_poll() throws IOException;

    private native void abort();


    final boolean poll() throws IOException {
	boolean result;

	if (STATISTICS) {
	    poll_poll_direct++;
	}
	
	result = msg_poll();
	return Ibis.myIbis.inputStreamPoll() || result;
    }


    final void waitPolling(PollClient client, long timeout, int preempt)
	    throws IOException {

	Ibis.myIbis.checkLockOwned();
// System.err.print(Thread.currentThread() + "/" + preempt(preempt) + ">");
// Thread.dumpStack();

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
	    for (int i = 0; i < POLLS_BEFORE_YIELD; i++) {
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

	int polls = POLLS_BEFORE_YIELD;
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
		    polls = POLLS_BEFORE_YIELD;
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

		    Ibis.myIbis.unlock();

		    doYield(NONPREEMPTIVE_MAY_POLL && preempt != PREEMPTIVE
				&& prev_last);

		    Ibis.myIbis.lock();
		}

	    } else {
		if (STATISTICS) {
		    poll_wait++;
		}
		if (preempt == PREEMPTIVE) {
		    preemptive_waiters++;
		}
		if (NONPREEMPTIVE_MAY_POLL || preempt == PREEMPTIVE) {
		    insert(client);
		}
		client.waitNonPolling(timeout);
		if (NONPREEMPTIVE_MAY_POLL || preempt == PREEMPTIVE) {
		    remove(client);
		}
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
// System.err.print("<");
    }

    private void doYield(boolean sleep) {
	if (USE_SLEEP_FOR_YIELD && sleep) {
	    try {
		Thread.sleep(0,1);
	    } catch (InterruptedException e) {
	    }
	} else {
	    Thread.yield();
	}
    }


    public void run() {
	// System.err.println("Poll peeker lives");
	while (comm_lives) {
	    if (Ibis.myIbis != null) {
		Ibis.myIbis.lock();
// System.err.println(Ibis.myIbis.myCpu + " do a peeker poll...");
		try {
		    // while (poll());
		    poll();
		    if (STATISTICS) {
			poll_from_thread++;
		    }
		} catch (IOException e) {
		    System.err.println("Poll throws " + e);
		    e.printStackTrace(System.err);
		}
		Ibis.myIbis.unlock();
	    }
	    doYield(true);
	}
    }


    void report(java.io.PrintStream out) {
	if (STATISTICS) {
	    out.println(Ibis.myIbis.myCpu +
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
