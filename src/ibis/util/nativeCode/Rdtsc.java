package ibis.util.nativeCode;

import java.io.*;

public final class Rdtsc extends ibis.ipl.Timer {
	private long time;
	private static final float MHz;

	public static native long rdtsc();
	private static native float getMHz();

	static {
		boolean loaded = false;
		try {
			System.loadLibrary("Rdtsc");
			loaded = true;
		} catch (RuntimeException e) {
			System.err.println("Could not load Rdtsc library");
		}

		if(loaded) {
			MHz = getMHz();
		} else {
			MHz = (float)0.0;
		}
	}

	public String implementationName() {
		return "ibis.util.nativeCode.Rdtsc";
	}

	public double accuracy() {
		return 1000000.0 * MHz;
	}

	public void reset() {
		time = 0;
	}

	public void start() {
		time -= rdtsc();
	}

	public void stop() {
		time += rdtsc();
		++ count;
	}

	public long currentTimeNanos() {
		long curr = rdtsc();

		return (long) (curr / (MHz/1000.0));
	}

	/**
	 * Timer reading in us
	 */
	public double totalTimeVal() {
		return (double) time / MHz;
	}

	/**
	 * Timer reading in us
	 */
	public String totalTime() {
		return format(time / MHz);
	}

	/**
	 * Timer reading in us
	 */
	public double averageTimeVal() {
		return (double) time / (count * MHz);
	}

	/**
	 * Timer reading in us
	 */
	public String averageTime() {
		return format(time / (count * MHz));
	}
}
