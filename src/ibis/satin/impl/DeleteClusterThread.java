package ibis.satin.impl;

class DeleteClusterThread extends Thread {

	int milis;

	DeleteClusterThread(int time) {
		super("SatinDeleteClusterThread");
		this.milis = 1000 * time;
	}

	public void run() {
		try {
			sleep(milis);
		} catch (InterruptedException e) {
			//ignore
		}
		Satin satin = Satin.this_satin;
		satin.deleteCluster(satin.ident.cluster());
	}

}