package ibis.ipl;

public abstract class Timer {
	protected int count;

	/** Returns implementation name of this timer ("e.g., "javaTimer"). **/
	public abstract String implementationName();

	/** Returns accuracy of this timer in seconds. **/
	public abstract double accuracy();

	public abstract long currentTimeNanos();

	/** Reset the timer. **/
	public abstract void reset();

	/** Start the timer. **/
	public abstract void start();

	/** Stop the timer. **/
	public abstract void stop();


	/** Return the total measured time in microseconds. **/
	public abstract double totalTimeVal();

	/** Return the total measured time in microseconds, nicely formatted. **/
	public abstract String totalTime();

	/** Return the average measured time in microseconds. **/
	public abstract double averageTimeVal();

	/** Return the average measured time in microseconds, nicely formatted. **/
	public abstract String averageTime();

	/** Return the last measured time in microseconds. **/
	public abstract double lastTimeVal();

	/** Return the last measured time in microseconds, nicely formatted. **/
	public abstract String lastTime();

	/** Return the number of measurements. **/
	public int nrTimes() {
		return count;
	}

	/** Format a time in microseconds **/
	public static String format(double micros) {
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
