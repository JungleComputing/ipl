package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisIOException;

public class Poll implements Runnable {

    boolean MANTA_COMPILE;

    Thread	poller;
    PollClient	waiting_threads;
    int		preemptive_pollers;
    Thread	peeker;
    boolean	last_is_preemptive;
    boolean	comm_lives = true;

    private static final boolean DEBUG = true;

    protected Poll() {
	// Sun doesn't set java.compiler, so getProperty returns null --Rob
	String compiler = java.lang.System.getProperty("java.compiler");
	MANTA_COMPILE = compiler != null && compiler.equals("manta");

	peeker = new Thread(this);
	peeker.setDaemon(true);

	if (DEBUG) {
	    System.err.println("Turn on Poll.DEBUG");
	}
	if (MANTA_COMPILE) {
	    System.err.println("Ibis/Panda knows this is Manta");
	}
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
    private long poll_from_thread;

    final static int polls_before_yield = 5000;	// 1; // 2000;

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


    protected native void msg_poll() throws IbisIOException;

    native void abort();


    final void poll() throws IbisIOException {
	if (DEBUG) {
	    poll_poll_direct++;
	}
	// long t = Ibis.currentTime();
	msg_poll();
	ibis.ipl.impl.messagePassing.Ibis.myIbis.inputStreamPoll();
    }


    final void waitPolling(PollClient client, long timeout, boolean preempt)
	    throws IbisIOException {

	long t_start = 0;
	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}

	if (DEBUG) {
	    if (preempt) {
		poll_preempt++;
	    } else {
		poll_non_preempt++;
	    }
	}
	if (preempt) {
	    for (int i = 0; i < polls_before_yield; i++) {
		poll();
		// poll_poll++;
		if (client.satisfied()) {
		    return;
		}
	    }
	    if (DEBUG || ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		if (preemptive_pollers > 0) {
		    System.err.println("Gee, some other preemptive poller active");
		}
		preemptive_pollers++;
	    }
	}
	last_is_preemptive = preempt;

	int polls = polls_before_yield;
	Thread me = Thread.currentThread();

	while (! client.satisfied()) {
	    if (timeout > 0) {
		if (System.currentTimeMillis() >= t_start + timeout) {
		    break;
		}
	    }

	    if (poller == null || preempt) {
		// OK, let me become poller
		poller = me;
	    }

	    if (poller == me) {
		poll();
		if (DEBUG) {
		    poll_poll++;
		}
		if (client.satisfied()) {
		    break;
		}
		if (! preempt || --polls == 0) {
		    polls = polls_before_yield;
		    if (DEBUG) {
			if (preempt) {
			    poll_yield_preempt++;
			} else {
			    if (DEBUG && preemptive_pollers > 0) {
				System.err.println("Am non-preemptive but I seem to preempt a preemptive poller");
			    }
			    poll_yield_non_preempt++;
			}
		    }

		    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
		    // ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockNotOwned();
		    if (! MANTA_COMPILE && ! preempt && last_is_preemptive) {
			try {
			    Thread.sleep(0,1);
			} catch (InterruptedException e) {
			}
		    } else {
			Thread.yield();
		    }
		    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
		}

	    } else {
		if (DEBUG) {
		    poll_wait++;
		}
		insert(client);
		client.poll_wait(timeout);
		remove(client);
	    }
	}

	if (poller == me) {
	    // Quit being the poller
	    poller = null;
	    if (waiting_threads != null) {
		// Wake up another poller thread
		waiting_threads.wakeup();
	    }
	}

	if ((DEBUG || ibis.ipl.impl.messagePassing.Ibis.DEBUG) && preempt) {
	    preemptive_pollers--;
	}
    }


    public void run() {
	System.err.println("Poll peeker lives");
	while (comm_lives) {
	    if (ibis.ipl.impl.messagePassing.Ibis.myIbis != null) {
		// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
		ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
// System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu + " do a peeker poll...");
		try {
		    poll();
		    if (DEBUG) {
			poll_from_thread++;
		    }
		// }
		} catch (IbisIOException e) {
		    System.err.println("Poll throws " + e);
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


    void report() {
	System.err.println(ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu +
		": Poll: /preempt " + poll_preempt +
		" /non-preempt " + poll_non_preempt +
		" poll " + poll_poll +
		" (direct " + poll_poll_direct +
		") wait " + poll_wait +
		" yield/preempt " + poll_yield_preempt +
		" /non-preempt " + poll_yield_non_preempt +
		" poller thread poll " + poll_from_thread);
    }

    void reset_stats() {
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

    protected void finalize() {
	comm_lives = false;
	report();
    }

}
