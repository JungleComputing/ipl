package ibis.satin.impl;

class KillerThread extends Thread {

	int milis;

	KillerThread(int time) {
		super("SatinKillerThread");
		milis = time * 1000;
	}

	public void run() {
		try {
			sleep(milis);
		} catch (InterruptedException e) {
			//ignore
		}
		Satin satin = Satin.this_satin;
		if (satin.allIbises.indexOf(satin.ident) >= (satin.allIbises.size() / 2)) {
		    System.exit(1);
		}
	}

}