package ibis.util;

public final class Timer extends ibis.ipl.Timer {
	private long time = 0;

	public String implementationName() {
		return "ibis.util.IbisTimer";
	}

	public double accuracy() {
		return 1e-3;
	}

	public void reset() {
		time = 0;
	}

	public void start() {
		time -= System.currentTimeMillis();
	}

	public void stop() {
		time += System.currentTimeMillis();
		++ count;
	}

	public double totalTimeVal() {
		return 1000.0*(double) time;
	}

	public String totalTime() {
		return format(1000*time);
	}

	public double averageTimeVal() {
		return 1000.0*(double) time / (count);
	}

	public String averageTime() {
		return format(1000*time / (count));
	}
}
