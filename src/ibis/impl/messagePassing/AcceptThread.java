package ibis.ipl.impl.messagePassing;

import ibis.ipl.ConditionVariable;

class AcceptThread extends Thread {

    ReceivePort port;
    ibis.ipl.ConnectUpcall upcall;
    ConditionVariable there_is_work = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
    boolean	stopped;


    class AcceptQ {
	AcceptQ		next;
	boolean		finished;
	boolean		accept;
	ibis.ipl.SendPortIdentifier port;
	ConditionVariable decided = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
    }

    AcceptQ	acceptQ_front;
    AcceptQ	acceptQ_tail;
    AcceptQ	acceptQ_freelist;


    AcceptThread(ReceivePort port, ibis.ipl.ConnectUpcall upcall) {
	this.port = port;
	this.upcall = upcall;
    }


    private void enqueue(AcceptQ q) {
	q.next = null;
	if (acceptQ_front == null) {
	    acceptQ_front = q;
	} else {
	    acceptQ_tail.next = q;
	}
	acceptQ_tail = q;
    }


    private AcceptQ dequeue() {
	if (acceptQ_front == null) {
	    return null;
	}

	AcceptQ q = acceptQ_front;
	acceptQ_front = q.next;

	return q;
    }


    private AcceptQ get() {
	if (acceptQ_freelist == null) {
	    return new AcceptQ();
	}

	AcceptQ q = acceptQ_freelist;
	acceptQ_freelist = q.next;

	return q;
    }


    private void release(AcceptQ q) {
	q.next = acceptQ_freelist;
	acceptQ_freelist = q;
    }


    boolean checkAccept(ibis.ipl.SendPortIdentifier p) {
	boolean	accept;
	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    AcceptQ q = get();

	    q.port = p;
	    enqueue(q);

	    while (! q.finished) {
		q.decided.cv_wait();
	    }

	    accept = q.accept;

	    release(q);

	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	return accept;
    }


    public void run() {
	// synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    AcceptQ q;

	    while (true) {
		while ((q = dequeue()) == null && ! stopped) {
		    there_is_work.cv_wait();
		}

		if (q == null) {
		    break;
		}

		q.accept = upcall.upcall(q.port);
		q.finished = true;
		q.decided.cv_signal();
	    }
	// }
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
    }


    void free() {
	stopped = true;
	there_is_work.cv_signal();
    }

}
