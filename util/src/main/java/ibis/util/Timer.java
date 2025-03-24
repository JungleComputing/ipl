/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for measuring time.
 */
public class Timer implements java.io.Serializable {

    static final Logger logger = LoggerFactory.getLogger(Timer.class);

    private static final long serialVersionUID = 1L;

    /**
     * Counts the number of start/stop sequences.
     */
    protected int count;

    protected long time = 0;

    protected long lastTime = 0;

    protected long maxTime = 0;

    protected long minTime = Long.MAX_VALUE;

    protected long t_start = 0;

    protected boolean started = false;

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

    /**
     * Adds the contents of the parameter to this timer.
     *
     * @param t the timer to be added.
     */
    public synchronized void add(Timer t) {
        time += t.time;
        count += t.count;
    }

    /**
     * Adds the contents of the parameters to this timer.
     *
     * @param t   the time in microseconds to be added.
     * @param cnt the number of start/stop sequences.
     */
    public synchronized void add(long t, int cnt) {
        time += t * 1000;
        count += cnt;
    }

    /**
     * Returns accuracy of this timer in seconds. Note that this only gives an
     * upperbound for the precision of this timer.
     *
     * @return the accuracy.
     */
    public double accuracy() {
        return 1e-9;
    }

    /**
     * Returns the current time stamp in nano seconds.
     *
     * @return the current time stamp.
     */
    public long currentTimeNanos() {
        return System.nanoTime();
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

        long cur_time = 0;
        if (started) {
            cur_time = System.nanoTime() - t_start;
        }
        return (time + cur_time) / 1000.0;
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
            return time / (count * 1000.0);
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
        return lastTime / 1000.0;
    }

    /**
     * Returns the maximum measured time in microseconds.
     *
     * @return the maximum measured time.
     */
    public double maxTimeVal() {
        return maxTime / 1000.0;
    }

    /**
     * Returns the minimum measured time in microseconds.
     *
     * @return the minimum measured time.
     */
    public double minTimeVal() {
        return minTime / 1000.0;
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
     * Returns the maximum measured time in microseconds, nicely formatted.
     *
     * @return the maximum measured time.
     */
    public String maxTime() {
        return format(maxTimeVal());
    }

    /**
     * Returns the minimum measured time in microseconds, nicely formatted.
     *
     * @return the minimum measured time.
     */
    public String minTime() {
        return format(minTimeVal());
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
     * Starts the timer. If the timer is already started, this is a no-op. The next
     * {@link #stop()}call will stop the timer and add the result to the total.
     */
    public void start() {
        if (started) {
            logger.error("Timer started twice!", new Error("Timer started twice"));
        }
        started = true;
        t_start = System.nanoTime();
    }

    /**
     * Stops the timer. If the timer is not started, this is a no-op. The timer is
     * stopped, and the time between the last {@link #start()}and "now" is added to
     * the total.
     */
    public void stop() {
        if (!started) {
            logger.error("Timer stopped, but not started!", new Error("Timer stopped, but not started!"));
        }

        lastTime = System.nanoTime() - t_start;
        time += lastTime;
        if (lastTime > maxTime) {
            maxTime = lastTime;
        }
        if (lastTime < minTime) {
            minTime = lastTime;
        }
        ++count;
        started = false;
    }

    /**
     * Formats a time in microseconds
     *
     * @param micros the time to be formatted.
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
        if (micros <= 0.0) {
            return "  0.000  s";
        }

        if (micros < 1.0) {
            double nanos = micros * 1000;
            if (nanos < 10) {
                return "  " + nf.format(nanos) + " ns";
            }
            if (nanos < 100) {
                return " " + nf.format(nanos) + " ns";
            }
            return nf.format(nanos) + " ns";
        } else if (micros < 1000.0) {
            if (micros < 10) {
                return "  " + nf.format(micros) + " us";
            }
            if (micros < 100) {
                return " " + nf.format(micros) + " us";
            }
            return nf.format(micros) + " us";
        } else if (micros < 1000000.0) {
            double millis = micros / 1000;
            if (millis < 10) {
                return "  " + nf.format(millis) + " ms";
            }
            if (millis < 100) {
                return " " + nf.format(millis) + " ms";
            }
            return nf.format(millis) + " ms";
        } else {
            double secs = micros / 1000000;
            if (secs < 10) {
                return "  " + nf.format(secs) + "  s";
            }
            if (secs < 100) {
                return " " + nf.format(secs) + "  s";
            }
            return nf.format(secs) + "  s";
        }
    }

    /**
     * Returns a Timer instance indicated by the implementation name provided.
     * Returns the default timer when the implementation could not be loaded. This
     * default could use a native timer or System.nanoTime(). The accuracy of the
     * timer can be obtained through the {@link #accuracy()} method.
     *
     * @param impl the name of the Timer implementation.
     * @return the new Timer instance.
     */
    public static Timer createTimer(String impl) {
        try {
            Class<?> c = Class.forName(impl);
            return (Timer) c.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            System.err.println("Warning could not create timer " + impl + ", falling back to default");
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
            Class<?> c = null;
            c = Class.forName("ibis.util.nativeCode.Rdtsc");
            return (Timer) c.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            return new Timer();
        }
    }

    /**
     * Times the timer itself.
     *
     * @param args the first argument, if given, indicates the number of start/stop
     *             sequences to time. Default is 1000000.
     */
    public static void main(String[] args) {
        int n = 1000000;

        if (args.length > 0) {
            n = Integer.parseInt(args[0]);
        }

        Timer t = createTimer();
        for (int i = 0; i < n; i++) {
            t.start();
            t.stop();
        }

        System.err.println("Timer(" + n + ") per call " + t.averageTime());
    }
}
