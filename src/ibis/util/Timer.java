package ibis.util;

import java.lang.reflect.Method;

/**
 * Utility for measuring time.
 */
public class Timer implements java.io.Serializable {
    /**
     * Counts the number of start/stop sequences.
     */
    protected int count;

    protected long time = 0;

    protected long lastTime = 0;

    protected long t_start = 0;

    protected boolean started = false;

    private static Method nanoTimer = null;

    private static boolean nanoMode = false;

    private static boolean nanoTimerFailed = false;

    {
        // load the java.lang.System class descriptor.
        Class cl = null;
        try {
            cl = Class.forName("java.lang.System");
        } catch (ClassNotFoundException e) {
            // O well, 
        }

        if (cl != null) {
            // Find the "nanoTime" method.
            try {
                nanoTimer = cl.getMethod("nanoTime", new Class[] {});
            } catch (Exception e) {
                // O well, older Java version ...
            }
        }
        if (nanoTimer != null) {
            invokeNanoTimer();
            if (!nanoTimerFailed) {
                nanoMode = true;
            }
        }
    }

    /**
     * Constructs a <code>Timer</code>.
     */
    protected Timer() {
        // protected, users should use createTimer.
    }

    /**
     * Returns implementation name of this timer ("e.g., "javaTimer").
     * 
     * @return the implementation name.
     */
    public String implementationName() {
        return "ibis.util.Timer";
    }

    public synchronized void add(Timer t) {
        time += t.time;
        count += t.count;
    }

    /**
     * Returns accuracy of this timer in seconds.
     * Note that this only gives an upperbound for the precision of this timer.
     * 
     * @return the accuracy.
     */
    public double accuracy() {
        if (nanoMode) {
            return 1e-9;
        }
        return 1e-3;
    }

    /**
     * Returns the current time stamp in nano seconds.
     * 
     * @return the current time stamp.
     */
    public long currentTimeNanos() {
        if (nanoMode) {
            return invokeNanoTimer();
        }
        return System.currentTimeMillis() * 1000000L;
    }

    static long invokeNanoTimer() {
        try {
            Object o = nanoTimer.invoke(null, new Object[] {});
            return ((Long) o).longValue();
        } catch (Exception e) {
            nanoTimerFailed = true;
        }
        return 0L;
    }

    /**
     * Resets the timer.
     */
    public void reset() {
        time = 0;
        count = 0;
    }

    /**
     * Returns the total measured time in microseconds.
     * 
     * @return total measured time.
     */
    public double totalTimeVal() {
        if (nanoMode) {
            return time / 1000.0;
        }
        return 1000.0 * time;
    }

    /**
     * Returns the total measured time in microseconds, nicely formatted.
     * 
     * @return total measured time.
     */
    public String totalTime() {
        return format(totalTimeVal());
    }

    /**
     * Returns the average measured time in microseconds.
     * 
     * @return the average measured time.
     */
    public double averageTimeVal() {
        if (count > 0) {
            if (nanoMode) {
                return ((double) time) / (1000 * count);
            }
            return 1000.0 * time / (count);
        }
        return 0.0;
    }

    /**
     * Returns the average measured time in microseconds, nicely formatted.
     * 
     * @return the average measured time.
     */
    public String averageTime() {
        return format(averageTimeVal());
    }

    /**
     * Returns the last measured time in microseconds.
     * 
     * @return the last measured time.
     */
    public double lastTimeVal() {
        if (nanoMode) {
            return lastTime / 1000.0;
        }
        return 1000.0 * lastTime;
    }

    /**
     * Returns the last measured time in microseconds, nicely formatted.
     * 
     * @return the last measured time.
     */
    public String lastTime() {
        return format(lastTimeVal());
    }

    /**
     * Returns the number of measurements.
     * 
     * @return the number of measurements.
     */
    public int nrTimes() {
        return count;
    }

    /**
     * Starts the timer. If the timer is already started, this is a no-op. The
     * next {@link #stop()}call will stop the timer and add the result to the
     * total.
     */
    public void start() {
        if (started) {
            throw new Error("Timer started twice");
        }
        started = true;
        if (nanoMode) {
            t_start = invokeNanoTimer();
        } else {
            t_start = System.currentTimeMillis();
        }
    }

    /**
     * Stops the timer. If the timer is not started, this is a no-op. The timer
     * is stopped, and the time between the last {@link #start()}and "now" is
     * added to the total.
     */
    public void stop() {
        if (!started) {
            throw new Error("Time stopped, but not started");
        }

        if (nanoMode) {
            lastTime = invokeNanoTimer() - t_start;
        } else {
            lastTime = System.currentTimeMillis() - t_start;
        }
        time += lastTime;
        ++count;
        started = false;
    }

    /**
     * Formats a time in microseconds
     * 
     * @param micros
     *            the time to be formatted.
     * @return the result of the format.
     */
    public static String format(double micros) {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance();
        nf.setMaximumFractionDigits(3);
        nf.setMinimumFractionDigits(3);
        // nf.setMaximumIntegerDigits(3);
        // nf.setMinimumIntegerDigits(3);
        nf.setGroupingUsed(false);

        // special case for 0
        if (micros == 0 || micros == Double.NaN || micros < 0.0) {
            return "  0.000  s";
        }

        if (micros < 1.0) {
            double nanos = micros * 1000;
            if (nanos < 10)
                return "  " + nf.format(nanos) + " ns";
            if (nanos < 100)
                return " " + nf.format(nanos) + " ns";
            return nf.format(nanos) + " ns";
        } else if (micros < 1000.0) {
            if (micros < 10)
                return "  " + nf.format(micros) + " us";
            if (micros < 100)
                return " " + nf.format(micros) + " us";
            return nf.format(micros) + " us";
        } else if (micros < 1000000.0) {
            double millis = micros / 1000;
            if (millis < 10)
                return "  " + nf.format(millis) + " ms";
            if (millis < 100)
                return " " + nf.format(millis) + " ms";
            return nf.format(millis) + " ms";
        } else {
            double secs = micros / 1000000;
            if (secs < 10)
                return "  " + nf.format(secs) + "  s";
            if (secs < 100)
                return " " + nf.format(secs) + "  s";
            return nf.format(secs) + "  s";
        }
    }

    /**
     * Returns a Timer instance indicated by the implementation name provided.
     * Returns the default timer when the implementation could not be loaded.
     * This default could use a native timer, System.currentTimeMillis(), or
     * System.nanoTime() if it exists. The accuracy of the timer can be
     * obtained through the {@link #accuracy()} method.
     * 
     * @param impl
     *            the name of the Timer implementation.
     * @return the new Timer instance.
     */
    public static Timer createTimer(String impl) {
        try {
            Class c = Class.forName(impl);
            return (Timer) c.newInstance();
        } catch (Throwable t) {
            System.err.println("Warning could not create timer " + impl
                    + ", falling back to default");
            return createTimer();
        }
    }

    /**
     * Returns a Timer instance suitable for the system at hand.
     * 
     * @return the new Timer instance.
     */
    public static Timer createTimer() {
        try {
            Class c = Class.forName("ibis.util.nativeCode.Rdtsc");
            return (Timer) c.newInstance();
        } catch (Throwable t) {
            return new Timer();
        }
    }
}
