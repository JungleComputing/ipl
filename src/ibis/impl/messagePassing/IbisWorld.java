package ibis.ipl.impl.messagePassing;

import ibis.ipl.impl.generic.ConditionVariable;


class IbisWorld extends Thread {

    private boolean isOpen = false;
    ConditionVariable opened;
    ibis.ipl.impl.messagePassing.Ibis myIbis;

    IbisIdentifier[] joinId;
    IbisIdentifier[] leaveId;


    IbisWorld() {
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    if (myIbis == null) {
		System.err.println("Gotcha!");
	    }
	    if (ibis.ipl.impl.messagePassing.Ibis.myIbis == null) {
		System.err.println("Gotcha 2!");
	    }
	    System.err.println("ibis = " + myIbis);
	}
	myIbis = ibis.ipl.impl.messagePassing.Ibis.myIbis;
	joinId = new IbisIdentifier[myIbis.nrCpus];
	leaveId = new IbisIdentifier[myIbis.nrCpus];
	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.println("static ibis = " + myIbis);
	}
	opened = ibis.ipl.impl.messagePassing.Ibis.myIbis.createCV();
	setDaemon(true);
	start();
    }


    void open() {
	isOpen = true;
	opened.cv_signal();
    }


    void close() {
	isOpen = false;
    }


    void join(int cpu, IbisIdentifier id) {
	joinId[cpu] = id;
	if (isOpen) {
	    opened.cv_signal();
	}
    }


    void leave(int cpu, IbisIdentifier id) {
	leaveId[cpu] = id;
	if (isOpen) {
	    opened.cv_signal();
	}
    }


    public void run() {

	ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	while (! isOpen) {
	    opened.cv_wait();
	}
	ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.err.print("IbisWorld thread: action! joinId = ");
	    for (int i = 0; i < myIbis.nrCpus; i++) {
		System.err.print(joinId[i] + ", ");
	    }
	    System.err.println();
	}
	for (int i = 0; isOpen && i < myIbis.nrCpus; i++) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    while (joinId[i] == null) {
		opened.cv_wait();
	    }
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    myIbis.join(joinId[i]);
	}

	for (int i = 0; isOpen && i < myIbis.nrCpus; i++) {
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.lock();
	    while (leaveId[i] == null) {
		opened.cv_wait();
	    }
	    ibis.ipl.impl.messagePassing.Ibis.myIbis.unlock();
	    myIbis.leave(leaveId[i]);
	}
    }

}
