class WaitingSendThread extends Thread {

    i_SOR dest;
    boolean dir;
    int lb;
    double[] row;
    boolean filled = false;

    WaitingSendThread() {
    }

    synchronized void put(i_SOR dest, boolean dir, int lb, double[] row) {

	while(filled) {
	    try {
		wait();			
	    } catch (Exception e) {
		System.err.println("WaitingSendThread.put during wait");
		e.printStackTrace();
	    }
	}
	filled = true;

	this.dest = dest;
	this.dir  = dir;
	this.lb   = lb;
	this.row  = row;

	// System.out.println("put row " + lb);

	notifyAll();
    }

    synchronized void finish_send() {

	while(filled) {
	    try {
		wait();			
	    } catch (Exception e) {
		System.err.println("WaitingSendThread.finish_send during wait");
		e.printStackTrace();
	    }
	}

	// System.out.println("finished row " + lb);
    }

    synchronized void send() {

	try {
	    while(!filled) {
		wait();
	    }

	    dest.putCol(dir, lb, row);
	    // System.out.println("sent row " + lb);
	    filled = false;

	    notifyAll();

	} catch (Exception e) {
	    System.out.println("Oops in SendThread " + e);
	    System.exit(1);
	}
    }

    public void run() {
	while(true) send();
    }
}

