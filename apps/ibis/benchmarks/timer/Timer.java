public class Timer {

    public static void main(String[] args) {
	int n = 1000000;

	if (args.length > 0) {
	    n = Integer.parseInt(args[0]);
	}

	ibis.util.Timer t = ibis.util.Timer.createTimer();
	for (int i = 0; i < n; i++) {
	    t.start();
	    t.stop();
	}

	System.err.println("Timer(" + n + ") per call " + t.averageTime());
    }
}
