package ibis.satin.impl;

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
		Satin satin = Satin.this_satin;
		satin.delete(satin.ident);
	}

}