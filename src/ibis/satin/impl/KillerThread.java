package ibis.satin.impl;

class KillerThread extends Thread {

	int milis; //wait that long before dying
	String cluster = null; //die only if your are in this cluster

	KillerThread(int time) {
		super("SatinKillerThread");
		this.milis = time * 1000;
	}
	
	KillerThread(int time, String cluster) {
		super("SatinKillerThread");
		this.milis = time * 1000;
		this.cluster = cluster;
	}

	public void run() {
		try {
			sleep(milis);
		} catch (InterruptedException e) {
			//ignore
		}
		Satin satin = Satin.this_satin;
//		if (satin.allIbises.indexOf(satin.ident) >= (satin.allIbises.size() / 2)) {
		if (satin.ident.cluster().equals(cluster) || cluster == null) {
			System.exit(1);
		}
	}

}