package ibis.ipl.impl.messagePassing;

import ibis.ipl.ConditionVariable;


class IbisWorld extends Thread {

    private boolean isOpen = false;
    ConditionVariable opened;
    ibis.ipl.impl.messagePassing.Ibis myIbis;

    IbisIdentifier[] joinId;
    IbisIdentifier[] leaveId;


    IbisWorld() {
	if (myIbis == null) {
	    System.err.println("Gotcha!");
	}
	if (ibis.ipl.impl.messagePassing.Ibis.myIbis == null) {
	    System.err.println("Gotcha 2!");
	}
	System.err.println("ibis = " + myIbis);
	myIbis = ibis.ipl.impl.messagePassing.Ibis.myIbis;
	joinId = new IbisIdentifier[myIbis.nrCpus];
	leaveId = new IbisIdentifier[myIbis.nrCpus];
	System.err.println("static ibis = " + myIbis);
	opened = new ConditionVariable(myIbis);
	// setDaemon(true);
	// start();
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
	int	i_join = 0;
	int	i_leave = 0;

	while (true) {
	    synchronized (myIbis) {
		while (! isOpen) {
		    opened.cv_wait();
		}
	    }

	    while (isOpen && i_join < myIbis.nrCpus && joinId[i_join] != null) {
		myIbis.join(joinId[i_join]);
		i_join++;
	    }

	    while (isOpen && i_leave < myIbis.nrCpus && leaveId[i_leave] != null) {
		myIbis.leave(leaveId[i_leave]);
		i_leave++;
	    }
	}
    }

}
