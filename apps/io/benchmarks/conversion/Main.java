import ibis.io.*;
import ibis.util.Timer;

import java.util.Random;

public final class Main {

    Random random = new Random();

    static final int MIN_BUF_SIZE = 100 * 1024;
    static final int MAX_BUF_SIZE = 100 * 1024;
    static final int TOTAL_TEST_DATA_SIZE = 1024 * 1024 * 1024;
    static final int REPEATS = 5;

    /**
     * Measure conversion speed for various primitive arrays, depending on
     * array size.
     */

    String doubleFmt(double d, int dec) {
	int tenPower = 1;
	for (int i = 0; i < dec; i++) {
	    tenPower *= 10;
	}
	long ld = Math.round(d * tenPower);
	d = (double)ld / tenPower;
	return "" + d;
    }

    private void booleanConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {

	    // *** booleans ***

	    boolean[] booleans = new boolean[bufferLength / Conversion.BOOLEAN_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (booleans.length * Conversion.BOOLEAN_SIZE);
	    byte[] buf = new byte[booleans.length * Conversion.BOOLEAN_SIZE];
	    double thrp;
	    double MBs = ITERS * booleans.length * (double)Conversion.BOOLEAN_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.boolean2byte(booleans, 0, booleans.length, buf, 0);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("boolean2byte[" + booleans.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2boolean(buf, 0, booleans, 0, booleans.length);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2boolean[" + booleans.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	}

    }


    private void shortConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {
	    // *** shorts ***

	    short[] shorts = new short[bufferLength / Conversion.SHORT_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (shorts.length * Conversion.SHORT_SIZE);
	    byte[] buf = new byte[shorts.length * Conversion.SHORT_SIZE];
	    double thrp;
	    double MBs = ITERS * shorts.length * (double)Conversion.SHORT_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.short2byte(shorts, 0, shorts.length, buf, 0);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("short2byte[" + shorts.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2short(buf, 0, shorts, 0, shorts.length);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2short[" + shorts.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	}
    }


    private void intConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {

	    // *** ints ***

	    int[] ints = new int[bufferLength / Conversion.INT_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (ints.length * Conversion.INT_SIZE);
	    byte[] buf = new byte[ints.length * Conversion.INT_SIZE];
	    double thrp;
	    double MBs = ITERS * ints.length * (double)Conversion.INT_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.int2byte(ints, 0, ints.length, buf, 0);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("int2byte[" + ints.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }
	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2int(buf, 0, ints, 0, ints.length);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2int[" + ints.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }
	}

    }


    private void longConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {

	    // *** longs ***

	    long[] longs = new long[bufferLength / Conversion.LONG_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (longs.length * Conversion.LONG_SIZE);
	    byte[] buf = new byte[longs.length * Conversion.LONG_SIZE];
	    double thrp;
	    double MBs = ITERS * longs.length * (double)Conversion.LONG_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.long2byte(longs, 0, longs.length, buf, 0);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("long2byte[" + longs.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2long(buf, 0, longs, 0, longs.length);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2long[" + longs.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }

	}

    }


    private void floatConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {

	    // *** floats ***

	    float[] floats = new float[bufferLength / Conversion.FLOAT_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (floats.length * Conversion.FLOAT_SIZE);
	    byte[] buf = new byte[floats.length * Conversion.FLOAT_SIZE];
	    double thrp;
	    double MBs = ITERS * floats.length * (double)Conversion.FLOAT_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.float2byte(floats, 0, floats.length, buf, 0);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("float2byte[" + floats.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2float(buf, 0, floats, 0, floats.length);
		}
		timer.stop();
	    }
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2float[" + floats.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");

	}

    }


    private void doubleConversion(Conversion conversion) {

	Timer timer = Timer.newTimer("ibis.util.nativeCode.Rdtsc");

	for(int bufferLength = MIN_BUF_SIZE; bufferLength <= MAX_BUF_SIZE; bufferLength *= 2) {

	    // *** doubles ***

	    double[] doubles = new double[bufferLength / Conversion.DOUBLE_SIZE];
	    int ITERS = TOTAL_TEST_DATA_SIZE / (doubles.length * Conversion.DOUBLE_SIZE);
	    byte[] buf = new byte[doubles.length * Conversion.DOUBLE_SIZE];
	    double thrp;
	    double MBs = ITERS * doubles.length * (double)Conversion.DOUBLE_SIZE / (1 << 20);

	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.double2byte(doubles, 0, doubles.length, buf, 0);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("double2byte[" + doubles.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }
	    for (int warmup = 0; warmup < REPEATS; warmup ++) {
		timer.reset();
		timer.start();
		for (int i = 0; i < ITERS; i++) {
		    conversion.byte2double(buf, 0, doubles, 0, doubles.length);
		}
		timer.stop();
	    thrp = MBs / (timer.totalTimeVal() / 1048576.0);
	    System.out.println("byte2double[" + doubles.length + "] (" + ITERS + ") takes " + doubleFmt((timer.totalTimeVal() / ITERS), 1) + " us; thrp " + doubleFmt(thrp, 1) + " MB/s");
	    }
	}

    }



    Main(String args[]) {
	Conversion conversion;

	if(args.length == 0 || args[0] == null) {
	    System.err.println("Usage: Main CONVERSION_NAME, e.g Main ibis.io.nio.HybridChunkLittleConversion");
	    System.exit(1);
	}

	try {
	    conversion = Conversion.loadConversion(args[0]);

	    System.out.println("Now look into " + conversion);
	    intConversion(conversion);
	    longConversion(conversion);
	    doubleConversion(conversion);

	} catch (Exception e) {
	    System.err.println("Conversion test got Exception " + e);
	    e.printStackTrace(System.err);
	    System.exit(1);
	}
    }


    public static void main(String args[]) {
	new Main(args);
    }

}
