import ibis.io.*;
import ibis.io.nio.*;

import java.util.Random;

public final class Main {

    Random random = new Random();

    /**
     * Checks to see if conversion from a primitive type[] to bytes
     * and back again returns the bytes we began with. Also checks
     * intermediate byte format.
     */
    boolean checkConversion(Conversion conversion) {

	for(int bufferLength = 10; bufferLength < 15000;bufferLength += 1000) {
	    byte[] bytes = new byte[bufferLength];

	    // *** boolean ***

	    boolean[] booleans = new boolean[bufferLength];
	    boolean[] booleanResult = new boolean[bufferLength];

	    for (int i = 0; i < booleans.length; i++) {
		booleans[i] = random.nextBoolean();
	    }

	    // array conversion, skip first element
	    conversion.boolean2byte(booleans, 1, booleans.length - 1, bytes, 1);
	    conversion.byte2boolean(bytes, 1, booleanResult, 
		    1, booleans.length - 1);

	    // single conversion to and from beginning of byte[] which we 
	    // left open
	    bytes[0] = conversion.boolean2byte(booleans[0]);
	    booleanResult[0] = conversion.byte2boolean(bytes[0]);

	    for(int i = 0; i < booleans.length;i++) {
		// check boolean->byte conversion
		if(booleans[i] && (bytes[i] != (byte)1)) {
		    System.out.println("boolean[] conversion doesn't work(1)");
		    return false;
		} else if(!booleans[i] && (bytes[i] != (byte)0)) {
		    System.out.println("boolean[] conversion doesn't work(2)");
		    return false;
		}

		// check boolean->byte->boolean conversion
		if(booleans[i] != booleanResult[i]) {
		    System.out.println("boolean[] conversion doesn't work(3)");
		    return false;
		}
	    }

	    // *** char ***

	    char[] chars = new char[bufferLength / 2];
	    char[] charResult = new char[bufferLength / 2];

	    for(int i = 0;i < chars.length; i++) {
		chars[i] = (char)random.nextInt();
	    }

	    conversion.char2byte(chars, 1, chars.length - 1, bytes, 2);
	    conversion.byte2char(bytes, 2, charResult, 1, chars.length - 1);

	    conversion.char2byte(chars[0], bytes, 0);
	    charResult[0] = conversion.byte2char(bytes, 0);

	    for (int i = 0;i < chars.length;i++) {
		byte msb = (byte) (0xff & (chars[i] >> 8));
		byte lsb = (byte) (0xff & chars[i]);

		if(conversion.bigEndian()) {
		    if(msb != bytes[i * 2] || lsb != bytes[(i*2)+1]) {
			System.err.println("Char conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if(msb != bytes[(i*2)+1] || lsb != bytes[i*2]) {
			System.err.println("Char conversion doesn't work(2)");
			return false;
		    }
		}

		if(chars[i] != charResult[i]) {
		    System.out.println("Char conversion doesn't work(3)");
		    return false;
		}
	    }


	    // *** short ***

	    short[] shorts = new short[bufferLength / 2];
	    short[] shortResult = new short[bufferLength / 2];

	    for(int i = 0;i < shorts.length; i++) {
		shorts[i] = (short)random.nextInt();
	    }

	    conversion.short2byte(shorts, 1, shorts.length - 1, bytes, 2);
	    conversion.byte2short(bytes, 2, shortResult, 1, shorts.length - 1);

	    conversion.short2byte(shorts[0], bytes, 0);
	    shortResult[0] = conversion.byte2short(bytes, 0);

	    for (int i = 0;i < shorts.length;i++) {
		byte msb = (byte) (0xff & (shorts[i] >> 8));
		byte lsb = (byte) (0xff & shorts[i]);

		if(conversion.bigEndian()) {
		    if(msb != bytes[i * 2] || lsb != bytes[(i*2)+1]) {
			System.err.println("Short conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if(msb != bytes[(i*2)+1] || lsb != bytes[i*2]) {
			System.err.println("Short conversion doesn't work(2)");
			return false;
		    }
		}

		if(shorts[i] != shortResult[i]) {
		    System.err.println("Short conversion doesn't work(3)");
		    return false;
		}
	    }

	    // *** int ***

	    int[] ints = new int[bufferLength / 4];
	    int[] intResult = new int[bufferLength / 4];

	    for(int i = 0;i < ints.length; i++) {
		ints[i] = random.nextInt();
	    }

	    conversion.int2byte(ints, 1, ints.length - 1, bytes, 4);
	    conversion.byte2int(bytes, 4, intResult, 1, ints.length - 1);

	    conversion.int2byte(ints[0], bytes, 0);
	    intResult[0] = conversion.byte2int(bytes, 0);

	    for (int i = 0;i < ints.length;i++) {
		byte msb = (byte) (0xff & (ints[i] >> 24));
		byte two = (byte) (0xff & (ints[i] >> 16));
		byte three = (byte) (0xff & (ints[i] >> 8));
		byte lsb = (byte) (0xff & ints[i]);

		if(conversion.bigEndian()) {
		    if (msb != bytes[(i*4) + 0] 
			    || two != bytes[(i*4) + 1]
			    || three != bytes[(i*4) + 2]
			    || lsb != bytes[(i*4) + 3]) {
			System.err.println("Int conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if (msb != bytes[(i*4) + 3] 
			    || two != bytes[(i*4) + 2]
			    || three != bytes[(i*4) + 1]
			    || lsb != bytes[(i*4) + 0]) {
			System.err.println("Int conversion doesn't work(2)");
			return false;
		    }
		}

		if(ints[i] != intResult[i]) {
		    System.err.println("Int conversion doesn't work(3)");
		    return false;
		}
	    }

	    // *** long ***

	    long[] longs = new long[bufferLength / 8];
	    long[] longResult = new long[bufferLength / 8];

	    for(int i = 0;i < longs.length; i++) {
		longs[i] = random.nextLong();
	    }

	    conversion.long2byte(longs, 1, longs.length - 1, bytes, 8);
	    conversion.byte2long(bytes, 8, longResult, 1, longs.length - 1);

	    conversion.long2byte(longs[0], bytes, 0);
	    longResult[0] = conversion.byte2long(bytes, 0);

	    for (int i = 0;i < longs.length;i++) {
		byte msb = (byte) (0xff & (longs[i] >> (32+24)));
		byte two = (byte) (0xff & (longs[i] >> (32+16)));
		byte three = (byte) (0xff & (longs[i] >> (32 + 8)));
		byte four = (byte) (0xff & (longs[i] >> (32 + 0)));
		byte five = (byte) (0xff & (longs[i] >> 24));
		byte six = (byte) (0xff & (longs[i] >> 16));
		byte seven = (byte) (0xff & (longs[i] >> 8));
		byte lsb = (byte) (0xff & longs[i]);

		if(conversion.bigEndian()) {
		    if (msb != bytes[(i*8) + 0] 
			    || two != bytes[(i*8) + 1]
			    || three != bytes[(i*8) + 2]
			    || four != bytes[(i*8) + 3]
			    || five != bytes[(i*8) + 4]
			    || six != bytes[(i*8) + 5]
			    || seven != bytes[(i*8) + 6]
			    || lsb != bytes[(i*8) + 7]) {
			System.err.println("Long conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if (msb != bytes[(i*8) + 7] 
			    || two != bytes[(i*8) + 6]
			    || three != bytes[(i*8) + 5]
			    || four != bytes[(i*8) + 4]
			    || five != bytes[(i*8) + 3]
			    || six != bytes[(i*8) + 2]
			    || seven != bytes[(i*8) + 1]
			    || lsb != bytes[(i*8) + 0]) {
			System.err.println("Long conversion doesn't work(2)");
			return false;
		    }
		}

		if(longs[i] != longResult[i]) {
		    System.err.println("Long conversion doesn't work(3)");
		    return false;
		}
	    }

	    // *** float ***

	    float[] floats = new float[bufferLength / 4];
	    float[] floatResult = new float[bufferLength / 4];

	    for(int i = 0;i < floats.length; i++) {
		floats[i] = random.nextFloat();
	    }

	    conversion.float2byte(floats, 1, floats.length - 1, bytes, 4);
	    conversion.byte2float(bytes, 4, floatResult, 1, floats.length - 1);

	    conversion.float2byte(floats[0], bytes, 0);
	    floatResult[0] = conversion.byte2float(bytes, 0);

	    for (int i = 0;i < floats.length;i++) {
		int temp = Float.floatToIntBits(floats[i]);
		byte msb = (byte) (0xff & (temp >> 24));
		byte two = (byte) (0xff & (temp >> 16));
		byte three = (byte) (0xff & (temp >> 8));
		byte lsb = (byte) (0xff & temp);

		if(conversion.bigEndian()) {
		    if (msb != bytes[(i*4) + 0] 
			    || two != bytes[(i*4) + 1]
			    || three != bytes[(i*4) + 2]
			    || lsb != bytes[(i*4) + 3]) {
			System.err.println("Float conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if (msb != bytes[(i*4) + 3] 
			    || two != bytes[(i*4) + 2]
			    || three != bytes[(i*4) + 1]
			    || lsb != bytes[(i*4) + 0]) {
			System.err.println("Float conversion doesn't work(2)");
			return false;
		    }
		}

		if(floats[i] != floatResult[i]) {
		    System.err.println("Float conversion doesn't work(3)");
		    return false;
		}
	    }

	    // *** double ***

	    double[] doubles = new double[bufferLength / 8];
	    double[] doubleResult = new double[bufferLength / 8];

	    for(int i = 0;i < doubles.length; i++) {
		doubles[i] = random.nextDouble();
	    }

	    conversion.double2byte(doubles, 1, doubles.length - 1, bytes, 8);
	    conversion.byte2double(bytes, 8, doubleResult, 
		    1, doubles.length - 1);

	    conversion.double2byte(doubles[0], bytes, 0);
	    doubleResult[0] = conversion.byte2double(bytes, 0);

	    for (int i = 0;i < doubles.length;i++) {
		long temp = Double.doubleToLongBits(doubles[i]);
		byte msb = (byte) (0xff & (temp >> (32+24)));
		byte two = (byte) (0xff & (temp >> (32+16)));
		byte three = (byte) (0xff & (temp >> (32 + 8)));
		byte four = (byte) (0xff & (temp >> (32 + 0)));
		byte five = (byte) (0xff & (temp >> 24));
		byte six = (byte) (0xff & (temp >> 16));
		byte seven = (byte) (0xff & (temp >> 8));
		byte lsb = (byte) (0xff & temp);

		if(conversion.bigEndian()) {
		    if (msb != bytes[(i*8) + 0] 
			    || two != bytes[(i*8) + 1]
			    || three != bytes[(i*8) + 2]
			    || four != bytes[(i*8) + 3]
			    || five != bytes[(i*8) + 4]
			    || six != bytes[(i*8) + 5]
			    || seven != bytes[(i*8) + 6]
			    || lsb != bytes[(i*8) + 7]) {
			System.err.println("Double conversion doesn't work(1)");
			return false;
		    }
		} else {
		    if (msb != bytes[(i*8) + 7] 
			    || two != bytes[(i*8) + 6]
			    || three != bytes[(i*8) + 5]
			    || four != bytes[(i*8) + 4]
			    || five != bytes[(i*8) + 3]
			    || six != bytes[(i*8) + 2]
			    || seven != bytes[(i*8) + 1]
			    || lsb != bytes[(i*8) + 0]) {
			System.err.println("Double conversion doesn't work(2)");
			return false;
		    }
		}

		if(doubles[i] != doubleResult[i]) {
		    System.err.println("Double conversion doesn't work(3)");
		    return false;
		}
	    }

	}
	return true;
    }

    Main() {
	Conversion conversion;
	boolean success = true;

	System.out.println("Testing available conversions one-by-one");

	String[] conversions = {
	    "ibis.io.SimpleLittleConversion",
	    "ibis.io.SimpleBigConversion",
	    "ibis.io.nio.NioWrapLittleConversion",
	    "ibis.io.nio.NioWrapBigConversion",
	    "ibis.io.nio.NioChunkLittleConversion",
	    "ibis.io.nio.NioChunkBigConversion",
	    "ibis.io.nio.HybridWrapLittleConversion",
	    "ibis.io.nio.HybridWrapBigConversion",
	    "ibis.io.nio.HybridChunkLittleConversion",
	    "ibis.io.nio.HybridChunkBigConversion"};

	for (int i = 0; i < conversions.length; i++) {

	    try {

		conversion = Conversion.loadConversion(conversions[i]);

		if(!checkConversion(conversion)) {
		    System.err.println(conversions[i] 
			    + " is not a valid Conversion");
		    success = false;
		}
	    } catch (Exception e) {
		System.err.println("Conversion test got Exception " + e);
		//e.printStackTrace(System.err);
		success = false;
	    }
	}

	if(success) {
	    System.out.println("Conversion tests ended successfully");
	} else {
	    System.out.println("Conversion tests failed");
	    System.exit(1);
	}
    }


    public static void main(String args[]) {
	new Main();
    }

}
