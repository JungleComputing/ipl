package ibis.ipl.impl.messagePassing;

import ibis.ipl.impl.generic.ConditionVariable;


class IbisWorld implements Runnable {

    private boolean isOpen = false;
    ConditionVariable opened;

    IbisIdentifier[] joinId;
    IbisIdentifier[] leaveId;


    IbisWorld() {
	if (Ibis.DEBUG) {
	    if (Ibis.myIbis == null) {
		System.err.println("Gotcha 2!");
	    }
	    System.err.println("ibis = " + Ibis.myIbis);
	}
	joinId = new IbisIdentifier[Ibis.myIbis.nrCpus];
	leaveId = new IbisIdentifier[Ibis.myIbis.nrCpus];
	if (Ibis.DEBUG) {
	    System.err.println("static ibis = " + Ibis.myIbis);
	}
	opened = Ibis.myIbis.createCV();

	Thread thr = new Thread(this, "Ibis world");
	thr.setDaemon(true);
	thr.start();
    }


    void open() {
	isOpen = true;
	opened.cv_signal();
    }


    void close() {
	isOpen = false;
    }


    void join(IbisIdentifier id) {
	int cpu = id.getCPU();
	joinId[cpu] = id;
	if (isOpen) {
	    opened.cv_signal();
	}
    }


    void leave(IbisIdentifier id) {
	int cpu = id.getCPU();
	leaveId[cpu] = id;
	if (isOpen) {
	    opened.cv_signal();
	}
    }


    public void run() {

	Ibis.myIbis.lock();
	while (! isOpen) {
	    try {
		opened.cv_wait();
	    } catch (InterruptedException e) {
		// ignore
	    }
	}
	Ibis.myIbis.unlock();

	if (Ibis.DEBUG) {
	    System.err.print("IbisWorld thread: action! joinId = ");
	    for (int i = 0; i < Ibis.myIbis.nrCpus; i++) {
		System.err.print(joinId[i] + ", ");
	    }
	    System.err.println();
	}
	for (int i = 0; isOpen && i < Ibis.myIbis.nrCpus; i++) {
	    Ibis.myIbis.lock();
	    while (joinId[i] == null) {
		try {
		    opened.cv_wait();
		} catch (InterruptedException e) {
		    // ignore
		}
	    }
	    Ibis.myIbis.unlock();
	    Ibis.myIbis.join(joinId[i]);
	}

	for (int i = 0; isOpen && i < Ibis.myIbis.nrCpus; i++) {
	    Ibis.myIbis.lock();
	    while (leaveId[i] == null) {
		try {
		    opened.cv_wait();
		} catch (InterruptedException e) {
		    // ignore
		}
	    }
	    Ibis.myIbis.unlock();
	    Ibis.myIbis.leave(leaveId[i]);
	}
    }

}
