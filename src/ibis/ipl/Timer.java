package ibis.ipl;

/**
 * Utility for measuring time.
 */
public abstract class Timer {
    /**
     * Counts the number of start/stop sequences.
     */
    protected int count;

    /**
     * Returns implementation name of this timer ("e.g., "javaTimer").
     * @return the implementation name.
     */
    public abstract String implementationName();

    /**
     * Returns accuracy of this timer in seconds.
     * @return the accuracy.
     */
    public abstract double accuracy();

    /**
     * Returns the current time stamp in nano seconds.
     * @return the current time stamp.
     */
    public abstract long currentTimeNanos();

    /**
     * Resets the timer.
     */
    public abstract void reset();

    /**
     * Starts the timer.
     * If the timer is already started, this is a no-op.
     * The next {@link #stop()} call will stop the timer and add the result to the
     * total.
     */
    public abstract void start();

    /**
     * Stops the timer.
     * If the timer is not started, this is a no-op.
     * The timer is stopped, and the time between the last {@link #start()} and
     * "now" is added to the total.
     */
    public abstract void stop();

    /**
     * Returns the total measured time in microseconds.
     * @return total measured time.
     */
    public abstract double totalTimeVal();

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
    public abstract double averageTimeVal();

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
    public abstract double lastTimeVal();

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
    public String format(double micros) {
	if (micros < 1.0) {
	    return String.valueOf(micros * 1000) + " ns";
	} else if (micros < 1000.0) {
	    return String.valueOf(micros) + " us";
	} else if (micros < 1000000.0) {
	    return String.valueOf(micros / 1000) + " ms";
	} else {
	    return String.valueOf(micros / 1000000) + "  s";
	}
    }
}
