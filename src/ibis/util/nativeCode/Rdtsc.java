/* $Id$ */

package ibis.util.nativeCode;

public final class Rdtsc extends ibis.util.Timer {
    // We often seem to meet overflow problems.
    // For that, we don't just do time -= rdtsc() at start and
    // time += rdtsc() at finish, but time += rdtsc() - t_start
    // at finish.

    private static boolean loaded = false;

    private static final double MHz;

    private static final double GHz;

    public static native long rdtsc();

    private static native float getMHz();

    static {
        try {
            ibis.ipl.Ibis.loadLibrary("Rdtsc");
            loaded = true;
        } catch (Throwable e) {
            //        System.err.println("Could not load Rdtsc library");
        }

        if (loaded) {
            MHz = getMHz();
        } else {
            MHz = (float) 0.0;
        }
        GHz = MHz / 1000.0F;
    }

    public Rdtsc() {
        if (!loaded) {
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
        if (started) {
            throw new Error("timer started twice");
        }
        t_start = rdtsc();
        started = true;
    }

    public void stop() {
        if (!started) {
            throw new Error("timer stopped, but was not started");
        }
        lastTime = rdtsc() - t_start;
        time += lastTime;
        ++count;
        started = false;
    }

    public long currentTimeNanos() {
        long curr = rdtsc();

        return (long) (curr / GHz);
    }

    /**
     * Timer reading in us
     */
    public double totalTimeVal() {
        return time / MHz;
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
        if (count == 0) {
            return 0.0;
        }
        return time / (count * MHz);
    }

    /**
     * Timer reading in us
     */
    public String averageTime() {
        return format(averageTimeVal());
    }

    /**
     * Timer reading in us
     */
    public double lastTimeVal() {
        return lastTime / MHz;
    }

    /**
     * Timer reading in us
     */
    public String lastTime() {
        return format(lastTime / MHz);
    }

    public String toString() {
        return "Timer stats: total time = " + totalTime() + " average time = "
                + averageTime() + " count = " + count;
    }
}
