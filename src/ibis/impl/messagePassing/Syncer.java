package ibis.ipl.impl.messagePassing;

import ibis.ipl.impl.generic.ConditionVariable;

public class Syncer implements PollClient {
    boolean	signalled;
    boolean	accepted;
    ConditionVariable cv = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();

    PollClient	next;
    PollClient	prev;


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
	return signalled;
    }

    public void wakeup() {
	cv.cv_signal();
    }

    public void poll_wait(long timeout) {
	cv.cv_wait(timeout);
    }

    Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    boolean s_wait(int timeout) throws ibis.ipl.IbisIOException {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();

	long	t_start = 0;

	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}
	while (! signalled) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.waitPolling(this, timeout, Poll.PREEMPTIVE);
	    if (timeout > 0) {
		if (System.currentTimeMillis() >= t_start + timeout) {
		    return false;
		}
	    }
	}

	signalled = false;

	return true;
    }

    void s_signal(boolean accepted) {
	// ibis.ipl.impl.messagePassing.Ibis.myIbis.checkLockOwned();
	this.accepted = accepted;
	signalled = true;
	wakeup();
    }
}
