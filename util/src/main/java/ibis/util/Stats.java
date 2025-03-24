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

/**
 * Class for doing some recurring statistical calculations.
 */
public final class Stats {

    /**
     * Prevent anyone from creating a <code>Stats</code> object.
     */
    private Stats() {
        /* do nothing */
    }

    /**
     * Calculates the mean of an array of numbers.
     * 
     * @param data the numbers to calculate the mean of.
     * @return the mean.
     */
    public static double mean(double[] data) {
        return mean(data, 0, data.length);
    }

    /**
     * Calculates the mean of a subset of an array of numbers.
     *
     * @param data the numbers to calculate the mean of
     * @param off  offset
     * @param len  length
     * @return the mean.
     */
    public static double mean(double[] data, int off, int len) {
        double total = 0;
        for (int i = off; i < (off + len); i++) {
            total += data[i];
        }
        if (len == 0) {
            return 0.0;
        }
        return total / len;
    }

    /**
     * Calculates the standard deviation of an array of numbers. See
     * http://www.davidmlane.com/hyperstat/A16252.html
     * 
     * @param data the numbers to calculate the standard deviation of.
     * @return the standard deviation.
     */
    public static double stdDev(double[] data) {
        return stdDev(data, 0, data.length);
    }

    /**
     * Calculates the standard deviation of a subset of an array of numbers. See
     * http://www.davidmlane.com/hyperstat/A16252.html
     * 
     * @param data the numbers to calculate the standard deviation of
     * @param off  offset
     * @param len  length
     * @return the standard deviation.
     */
    public static double stdDev(double[] data, int off, int len) {
        double mean = mean(data, off, len);
        double sum = 0;

        for (int i = off; i < (off + len); i++) {
            double v = data[i] - mean;
            sum += v * v;
        }

        if (len <= 1) {
            return 0.0;
        }

        return Math.sqrt(sum / (len - 1));
    }

    /**
     * Calculates a speed in Mb/s given the size in bytes and the time in
     * milliseconds. The result is rounded to one hundreth of an integer
     * 
     * @param bytes  size of the data in bytes
     * @param millis the time of the measurement in milliseconds
     * @return the speed.
     */
    public static double mbs(double bytes, double millis) {
        return round(((bytes / millis) * 1000.0) / (1024.0 * 1024.0));
    }

    /**
     * Calculates a speed in Mb/s given the size in bytes and a number of times in
     * milliseconds. The result is rounded to one hundreth of an integer.
     *
     * @param bytes  size of the data in bytes
     * @param millis a number of time measurements in milliseconds
     * @param off    the first measurement used in the calculation
     * @param len    the number of measurements used in the calculation
     * @return the speed.
     */
    public static double mbs(double bytes, double[] millis, int off, int len) {
        double mean = mean(millis, off, len);

        return mbs(bytes, mean);
    }

    /**
     * Rounds up a double. Rounds to one hundreth of an integer.
     * 
     * @param v the value to be rounded.
     * @return the rounded value.
     */
    public static double round(double v) {
        return (Math.ceil(v * 100.0) / 100.0);
    }

    /**
     * Returns the standard deviation as a percentage of the mean. The result is
     * rounded up to a hundredth of an integer.
     *
     * @param data array with input data (numbers)
     * @param off  offset in the data at which to start calculating
     * @param len  number of array elements to use for the calculation
     * @return the standard deviation as a percentage.
     */
    public static double stdDevError(double[] data, int off, int len) {
        double mean = mean(data, off, len);
        double stdDev = stdDev(data, off, len);

        return round((stdDev / mean) * 100.0);
    }
}
