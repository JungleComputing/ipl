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
		System.exit(1);
	}

}