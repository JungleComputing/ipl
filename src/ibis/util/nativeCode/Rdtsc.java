package ibis.util.nativeCode;

public final class Rdtsc extends ibis.util.Timer {
	private long time;
	// We often seem to meet overflow problems.
	// For that, we don't just do time -= rdtsc() at start and
	// time += rdtsc() at finish, but time += rdtsc() - t_start
	// at finish.
	private long t_start;
	private long lastTime;
	private static final float MHz;
	private static final float GHz;

	private boolean started;

	public static native long rdtsc();
	private static native float getMHz();
        private static boolean loaded = false;

	static {
		try {
			ibis.ipl.Ibis.loadLibrary("Rdtsc");
			loaded = true;
		} catch (RuntimeException e) {
//			System.err.println("Could not load Rdtsc library");
		}

		if(loaded) {
			MHz = getMHz();
		} else {
			MHz = (float)0.0;
		}
		GHz = MHz * 1000.0F;
	}

    public Rdtsc() {
	if(!loaded) {
	    throw new Error("Could not load Rdtsc library");
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
		count = 0;
	}

	public void start() {
		if (! started) {
			t_start = rdtsc();
			started = true;
		}
	}

	public void stop() {
		if (started) {
			lastTime = rdtsc() - t_start;
			time += lastTime;
			++ count;
			started = false;
		}
	}

	public long currentTimeNanos() {
		long curr = rdtsc();

		return (long) (curr / GHz);
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

	/**
	 * Timer reading in us
	 */
	public double lastTimeVal() {
		return (double) lastTime / MHz;
	}

	/**
	 * Timer reading in us
	 */
	public String lastTime() {
		return format(lastTime / MHz);
	}
}
