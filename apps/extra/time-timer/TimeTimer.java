/* $Id$ */

import ibis.util.Timer;

/**
 * Try to load a timer and measure its overhead
 */
class TimeTimer {

    public static void main(String[] args) {

	String timerName = "ibis.util.nativeCode.Rdtsc";

	int options = 0;
	int n = 1000000;

	for (int i = 0; i < args.length; i++) {
	    if (false) {
	    } else if (options == 0) {
		timerName = args[i];
	    } else if (options == 1) {
		n = Integer.parseInt(args[i]);
	    } else {
		System.err.println("Usage: TimeTimer {#calls {timer class}}");
		System.exit(7);
	    }
	}

	try {
	    Timer timer = Timer.createTimer(timerName);
	    Timer outerTimer = Timer.createTimer(timerName);

	    System.out.println("Timer accuracy = " + timer.accuracy());

	    long t = System.currentTimeMillis();
	    outerTimer.start();
	    for (int i = 0; i < n; i++) {
		timer.start();
		timer.stop();
	    }
	    outerTimer.stop();
	    t = System.currentTimeMillis() - t;

	    System.err.println("TimeTimer: " + n + " calls " +
		    " wallclock time " + (t / 1000.0) + "s " +
		    " own elapsed " +
		    (outerTimer.totalTimeVal() / 1000000.0) + "s " +
		    " per tick " + (t * 1000.0 / n) + "us");
	} catch (Exception e) {
	    System.err.println("Got exception? " + e);
	}

    }
}
