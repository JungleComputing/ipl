/* $Id$ */

class Worker extends Thread {

    static final int JOBSIZE = 10000;

    int		jobs = 0;
    boolean	stopped = false;
    String	name;
    long	t_start;

    Worker(String name) {
	this.name = name;
    }

    Worker() {
	this("Worker");
    }

    public void run() {

	System.err.println("Run " + name);
	reset();

	while (true) {
	    synchronized (this) {
		if (stopped) {
		    break;
		}
	    }

	    for (int i = 0; i < JOBSIZE; i++) {
		double x = Math.cos(7.33);
	    }
	    jobs++;
	}

	System.out.println(name + ": did " + (jobs / ((System.currentTimeMillis() - t_start) / 1000.0)) + " jobs/s");
    }

    synchronized void reset() {
	jobs = 0;
	t_start = System.currentTimeMillis();
    }

    synchronized void quit() {
	stopped = true;
    }

    public static void main(String[] args) {
	Worker w = new Worker();

	w.start();
	synchronized (w) {
	    try {
		w.wait(10000);
	    } catch (InterruptedException e) {
		System.err.println("interrupted " + e);
	    }
	}

	w.quit();
    }

}
