package ibis.util;

/**
 * Utility for measuring time.
 */
public class Timer implements java.io.Serializable {
    /**
     * Counts the number of start/stop sequences.
     */
    protected int count;

    private long time = 0;
    private long lastTime = 0;
    private long t_start = 0;
    private boolean started = false;

    /**
     * Constructs a <code>Timer</code>.
     */
    public Timer() {
    }

    /**
     * Returns implementation name of this timer ("e.g., "javaTimer").
     * @return the implementation name.
     */
    public String implementationName() {
	return "ibis.util.Timer";
    }

    /**
     * Returns accuracy of this timer in seconds.
     * @return the accuracy.
     */
    public double accuracy() {
	return 1e-3;
    }

    /**
     * Returns the current time stamp in nano seconds.
     * @return the current time stamp.
     */
    public long currentTimeNanos() {
	return System.currentTimeMillis() * 1000000L;
    }

    /**
     * Resets the timer.
     */
    public void reset() {
	time = 0;
	count = 0;
    }

    /**
     * Starts the timer.
     * If the timer is already started, this is a no-op.
     * The next {@link #stop()} call will stop the timer and add the result to the
     * total.
     */
    public void start() {
	if(started) return;
	started = true;
	t_start = System.currentTimeMillis();
    }

    /**
     * Stops the timer.
     * If the timer is not started, this is a no-op.
     * The timer is stopped, and the time between the last {@link #start()} and
     * "now" is added to the total.
     */
    public void stop() {
	if(!started) return;

	lastTime = System.currentTimeMillis() - t_start;
	time += lastTime;
	++ count;
	started = false;
    }

    /**
     * Returns the total measured time in microseconds.
     * @return total measured time.
     */
    public double totalTimeVal() {
	return 1000.0*(double) time;
    }

    /**
     * Returns the total measured time in microseconds, nicely formatted.
     * @return total measured time.
     */
    public String totalTime() {
	return format(totalTimeVal());
    }

    /**
     * Returns the average measured time in microseconds.
     * @return the average measured time.
     */
    public double averageTimeVal() {
	if (count > 0) {
	    return 1000.0*(double) time / (count);
	}
	return 0.0;
    }

    /**
     * Returns the average measured time in microseconds, nicely formatted.
     * @return the average measured time.
     */
    public String averageTime() {
	return format(averageTimeVal());
    }

    /**
     * Returns the last measured time in microseconds.
     * @return the last measured time.
     */
    public double lastTimeVal() {
	return 1000.0*(double)lastTime;
    }

    /**
     * Returns the last measured time in microseconds, nicely formatted.
     * @return the last measured time.
     */
    public String lastTime() {
	return format(lastTimeVal());
    }

    /**
     * Returns the number of measurements.
     * @return the number of measurements.
     */
    public int nrTimes() {
	return count;
    }

    /**
     * Formats a time in microseconds
     * @param micros the time to be formatted.
     * @return the result of the format.
     */
    public static String format(double micros) {
	java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
	nf.setMaximumFractionDigits(3);
	nf.setMinimumFractionDigits(3);
//	nf.setMaximumIntegerDigits(3);
//	nf.setMinimumIntegerDigits(3);
	nf.setGroupingUsed(false);

        // special case for 0
	if(micros == 0 || micros == Double.NaN || micros < 0.0) {
	    return "  0.000  s";
	}

	if (micros < 1.0) {
	    double nanos = micros * 1000;
	    if(nanos < 10) return "  " + nf.format(nanos) + " ns";
	    if(nanos < 100) return " " + nf.format(nanos) + " ns";
	    return nf.format(nanos) + " ns";
	} else if (micros < 1000.0) {
	    if(micros < 10) return "  " + nf.format(micros) + " us";
	    if(micros < 100) return " " + nf.format(micros) + " us";
	    return nf.format(micros) + " us";
	} else if (micros < 1000000.0) {
	    double millis = micros / 1000;
	    if(millis < 10) return "  " + nf.format(millis) + " ms";
	    if(millis < 100) return " " + nf.format(millis) + " ms";
	    return nf.format(millis) + " ms";
	} else {
	    double secs = micros / 1000000;
	    if(secs < 10) return "  " + nf.format(secs) + "  s";
	    if(secs < 100) return " " + nf.format(secs) + "  s";
	    return nf.format(secs) + "  s";
	}
    }

    /**
     * Returns a Timer instance indicated by the implementation name
     * provided.
     * Returns null when the implementation could not be loaded.
     * @param impl the name of the Timer implementation.
     * @return the new Timer instance, or <code>null</code>.
     */
    public static Timer newTimer(String impl) {
	try {
	    Class c = Class.forName(impl);
	    return (Timer) c.newInstance();
	} catch (Throwable t) {
	    return null;
	}
    }
}
