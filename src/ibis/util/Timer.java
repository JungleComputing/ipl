package ibis.util;

public final class Timer extends ibis.ipl.Timer {
	private long time = 0;
	private long lastTime = 0;

	public String implementationName() {
		return "ibis.util.Timer";
	}

	public double accuracy() {
		return 1e-3;
	}

	public long currentTimeNanos() {
		return System.currentTimeMillis() * 1000000L;
	}

	public void reset() {
		time = 0;
	}

	public void start() {
		lastTime = System.currentTimeMillis();
		time -= lastTime;
	}

	public void stop() {
		long tmp = System.currentTimeMillis();
		time += tmp;
		lastTime = tmp - lastTime;
		++ count;
	}

	public double totalTimeVal() {
		return 1000.0*(double) time;
	}

	public String totalTime() {
		return format(1000*time);
	}

	public double averageTimeVal() {
		if(count > 0) {
			return 1000.0*(double) time / (count);
		}
		return 0.0;
	}

	public String averageTime() {
		if(count > 0) {
			return format(1000*time / (count));
		}
		return "0.0";
	}

	public double lastTimeVal() {
		return 1000.0*(double)lastTime;
	}

	public String lastTime() {
		return format(1000*lastTime);
	}
}
