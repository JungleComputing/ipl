package ibis.impl.messagePassing;

import ibis.util.ConditionVariable;

import java.io.IOException;

public class Syncer implements PollClient {
    boolean	signalled;
    boolean	accepted;
    ConditionVariable cv = Ibis.myIbis.createCV();

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
	try {
	    cv.cv_wait(timeout);
	} catch (InterruptedException e) {
	    // ignore
	}
    }

    Thread me;

    public Thread thread() {
	return me;
    }

    public void setThread(Thread thread) {
	me = thread;
    }

    boolean s_wait(long timeout) throws IOException {
	Ibis.myIbis.checkLockOwned();

	long	t_start = 0;

	if (timeout > 0) {
	    t_start = System.currentTimeMillis();
	}
	while (! signalled) {
	    Ibis.myIbis.waitPolling(this, timeout, Poll.PREEMPTIVE);
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
	Ibis.myIbis.checkLockOwned();
	this.accepted = accepted;
	signalled = true;
	wakeup();
    }
}
