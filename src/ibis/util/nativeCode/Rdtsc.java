package ibis.util.nativeCode;

import java.io.*;

public final class Rdtsc extends ibis.ipl.Timer {
	private long time;
	private static float MHz;

	public static native long rdtsc();

	static {
		boolean loaded = false;
		try {
			System.loadLibrary("Rdtsc");
			loaded = true;
		} catch (RuntimeException e) {
			System.err.println("Could not load Rdtsc library");
		}

		if(loaded) {
			try {
				String	     line;
				RandomAccessFile file = new RandomAccessFile("/proc/cpuinfo", "r");
				
				while ((line = file.readLine()) != null)
					if (line.startsWith("cpu MHz")) {
						int colonPos = line.indexOf(':');
						if(colonPos == -1) {
							System.err.println("eek, could not find Mhz");
							System.exit(1);
						}
						String s = line.substring(colonPos+1);

						MHz = Float.parseFloat(s);
						break;
					}

				if (MHz == 0) {
					System.err.println("Cannot find \"cpu MHz\" line");
					System.exit(1);
				}
//			System.out.println("CPU MHz = " + MHz);
			}
			catch (IOException ex) {
				System.err.println("Rdtsc: Could not find out CPU speed");
//			System.err.println(ex.getMessage());
//			System.exit(1);
			}
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

	public double totalTimeVal() {
		return (double) time / MHz;
	}

	public String totalTime() {
		return format(time / MHz);
	}

	public double averageTimeVal() {
		return (double) time / (count * MHz);
	}

	public String averageTime() {
		return format(time / (count * MHz));
	}
}
