package ibis.satin.impl;

import ibis.ipl.IbisIdentifier;

class DeleteThread extends Thread {

    int milis;
    String cluster = null;

    DeleteThread(int time) {
	super("SatinDeleteThread");
	this.milis = 1000 * time;
    }

    public void run() {
	try {
	    sleep(milis);
	} catch (InterruptedException e) {
	    //ignore
	}
	Satin satin = SatinBase.this_satin;
	satin.mustLeave(new IbisIdentifier[] { satin.ident });
    }

}
