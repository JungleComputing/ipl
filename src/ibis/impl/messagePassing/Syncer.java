package ibis.impl.messagePassing;

import ibis.util.ConditionVariable;

import java.io.IOException;

public abstract class Syncer implements PollClient {

    private ConditionVariable cv = Ibis.myIbis.createCV();

    private int		waiters;

    private PollClient	next;
    private PollClient	prev;


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

    public abstract boolean satisfied();

    /**
     * Override this to modify the satisfaction condition of this Syncer
     * and call wakeup as a chained function.
     */
    public void signal() {
	wakeup();
    }

    /**
     * Wake up one waiting thread. satisfied() need not be true.
     */
    public final void wakeup() {
	if (waiters > 0) {
	    cv.cv_signal();
	}
    }

    /**
     * Wake up all waiting threads. satisfied() need not be true.
     */
    public final void wakeupAll() {
	if (waiters > 0) {
	    cv.cv_bcast();
	}
    }

    public void waitNonPolling(long timeout) {
	try {
	    waiters++;
	    cv.cv_wait(timeout);
	    waiters--;
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

    void waitPolling() throws IOException {
	waitPolling(0);
    }

    boolean waitPolling(long timeout) throws IOException {
	Ibis.myIbis.checkLockOwned();

	long	t_start = 0;

	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}
	while (! satisfied()) {
	    waiters++;
	    Ibis.myIbis.waitPolling(this, timeout, Poll.PREEMPTIVE);
	    waiters--;
	    if (timeout > 0) {
		if (System.currentTimeMillis() >= t_start + timeout) {
		    return false;
		}
	    }
	}

	return true;
    }

}
