package ibis.util;


/**
 * Class for doing some recurring statistical calculations
 */
public final class Stats {

    /**
     * Shortcut for mean(double[] data, 0, data.length)
     */
    public static double mean(double[] data) {
	return mean(data, 0, data.length);
    }

    /**
     * Calculates the mean of a subset of an array of numbers.
     *
     * @param data the numbers to calculate the mean of
     * @param off offset
     * @param int len
     */
    public static double mean(double[] data, int off, int len) {
	double total = 0;
	for (int i = off; i < (off+len); i++) {
	    total += data[i];
	}
	return total / len;
    }

    /**
     * Shortcut for stdDev(data, 0, data.length)
     */
    public static double stdDev(double[] data) {
	return stdDev(data, 0, data.length);
    }

    /**
     * Calculates the standard deviation of a subset of an array
     * of numbers.
     * see http://davidmlane.com/hyperstat/A16252.html 
     */
    public static double stdDev(double[] data, int off, int len) {
	double mean = mean(data, off, len);
	double sum = 0;

	for (int i = off; i < (off+len); i++) {
	    double v = data[i] - mean;
	    sum += v * v;
	}

	return Math.sqrt(sum / (len - 1));
    }

    /**
     * Calculates a speed in Mb/s given the size in bytes and the time
     * in milliseconds. The result is rounded to one hundreth of an integer
     */
    public static double mbs(double bytes, double millis) {
	return round(((bytes / millis) * 1000.0) / (1024.0 * 1024.0));
    }

    /**
      * Calculates a speed in Mb/s given the size in bytes and a number of
      * times in milliseconds. The result is rounded to one hundreth of an 
      * integer.
      *
      * @param bytes size of the data in bytes
      * @param millis a number of time measurements in milliseconds
      * @param off the first measurement used in the calculation
      * @param len the number of measurements used in the calculation
      */
    public static double mbs(double bytes, double[] millis, int off, int len) {
	double mean = mean(millis, off, len);

	return mbs(bytes, mean);
    }

    /**
     * Round up a double. Rounds to one houndreth of an integer
     */
    public static double round(double v) {
	return (Math.ceil(v*100.0)/100.0);
    }

    /**
     * Returns the standard deviation as a percentage of the mean. 
     * The result is rounded up to a houndreth of an integer.
     *
     * @param data array with input data (numbers)
     * @param off offset in the data at which to start calculating
     * @param len number of array elements to use for the calculation
     */
    public static double stdDevError(double[] data, int off, int len) {
	double mean = mean(data, off, len);
	double stdDev = stdDev(data, off, len);

	return round((stdDev / mean) * 100.0);
    }




}
