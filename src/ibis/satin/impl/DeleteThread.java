package ibis.satin.impl;

class DeleteThread extends Thread {

	int milis;
	String cluster = null;

	DeleteThread(int time) {
		super("SatinDeleteThread");
		this.milis = 1000 * time;
	}

	DeleteThread(int time, String cluster) {
		super("SatinDeleteThread");
		this.milis = 1000 * time;
		this.cluster = cluster;
	}

	public void run() {
		try {
			sleep(milis);
		} catch (InterruptedException e) {
			//ignore
		}
		Satin satin = Satin.this_satin;
		if (satin.ident.cluster().equals(cluster) || cluster == null) {
			satin.delete(satin.ident);
		}
	}

}