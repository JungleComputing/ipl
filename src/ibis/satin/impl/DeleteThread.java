package ibis.satin.impl;

class DeleteThread extends Thread {

	Satin satin;

	int milis;

	DeleteThread(Satin satin, int time) {

		super("SatinDeleteThread");
		this.satin = satin;
		this.milis = 1000 * time;
	}

	public void run() {
		try {
			sleep(milis);
		} catch (InterruptedException e) {
			//ignore
		}
		satin.delete(satin.ident);
	}

}