package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation of <code>ArrayInputStream</code> on top of an
 * <code>InputStream</code>.
 */

public final class BufferedArrayInputStream extends ArrayInputStream {

    /**
     * When set to <code>true</code>, debugging prints are enabled.
     */
    private static final boolean DEBUG = false;

    /**
     * The underlying <code>InputStream</code>.
     */
    private InputStream in;

    /**
     * The buffer size.
     */
    private static final int BUF_SIZE = IbisStreamFlags.BUFFER_SIZE;

    /**
     * The buffer.
     */
    private byte [] buffer;
    private int index, buffered_bytes;

    // Object used to convert primitive types to bytes.
    // No need to inistalize this more than once, so static.
    private Conversion conversion;

    public BufferedArrayInputStream(InputStream in) {
	this.in = in;
	buffer = new byte[BUF_SIZE];

	conversion = Conversion.loadConversion(false);
    }

    private void dump(byte[] buffer, int off, int len, String caller) {
	System.err.print(caller + " buffer read[" + off + ":" + len + "] = [");
	for (int i = off; i < min(off + len, 256); i++) {
	    System.err.print(Integer.toHexString(buffer[i] & 0xff) + " ");
	}
	System.err.println("]");
    }

    private static final int min(int a, int b) {
	return (a > b) ? b : a;
    }

    public final int read() throws IOException {
	throw new IOException("int read() has no meaning for typed stream");
    }

    private final void fillBuffer(int len) throws IOException {

	// This ensures that there are at least 'len' bytes in the buffer
	// PRECONDITION: 'index + buffered_bytes' should never be larger
	// than BUF_SIZE!!

	while (buffered_bytes < len) {
//	    System.err.println("buffer -> filled from " + index + " with "
//	    	+ buffered_bytes + " size " + BUF_SIZE + " read " + len);
	    
	    int n = in.read(buffer, index + buffered_bytes,
			    BUF_SIZE-(index+buffered_bytes));
	    if (n < 0) {
		throw new java.io.EOFException("EOF encountered");
	    }

	    buffered_bytes += n;
	}
    }

    public final int available() throws IOException {
	return (buffered_bytes + in.available());
    }

    public void readArray(boolean[] a, int off, int len)
		throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(boolean[" + off +
			       " ... " + (off+len) + "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_BOOLEAN;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_BOOLEAN;
		conversion.byte2boolean(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_BOOLEAN;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2boolean(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;
    }

    public void readArray(byte[] a, int off, int len) throws IOException {
	if (DEBUG) {
	    System.err.println("readArray(byte[" + off + " ... " +
				    (off+len) + "])");
	}

	if (buffered_bytes >= len) {
	    //System.err.println("IN BUF");

	    // data is already in the buffer.
//	    System.err.println("Data is in buffer -> copying " + index +
//	    			" ... " + (index+len) + " to " + off);

	    System.arraycopy(buffer, index, a, off, len);
	    index += len;
	    buffered_bytes -= len;

	    //System.err.println("DONE");

	} else {
	    if (buffered_bytes != 0) {
//		System.err.println("PARTLY IN BUF " + buffered_bytes +
//					" " + len);
		// first, copy the data we do have to 'a' .
		System.arraycopy(buffer, index, a, off, buffered_bytes);
	    }
	    int rd = buffered_bytes;
	    index = 0;
	    do {
		int n = in.read(a, off + rd, len - rd);
		if (n < 0) {
		    throw new java.io.EOFException("EOF encountered");
		}
		rd += n;
	    } while (rd < len);

	    buffered_bytes = 0;
	}

	//		System.err.print("result -> byte[");
	//		for (int i=0;i<len;i++) { 
	//			System.err.print(a[off+i] + ",");
	//		}
	//		System.err.println("]");
    }


    //	static int R = 0;
    //	static int W = 0;

    public void readArray(short[] a, int off, int len)
	throws IOException {

	int useable, converted;
	int to_convert = len * SIZEOF_SHORT;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_SHORT;
		conversion.byte2short(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_SHORT;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2short(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;

	if (DEBUG) {
	    System.err.print("readArray(short[");
	    for (int i=0;i<len;i++) {
		System.err.print(a[off+i] + ",");
	    }
	    System.err.println("]");
	    System.err.flush();
	}
    }

    public void readArray(char[] a, int off, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(char[" + off + " ... " +
				    (off+len) + "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_CHAR;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_CHAR;
		conversion.byte2char(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_CHAR;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2char(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;
    }

    public void readArray(int[] a, int off, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(int[" + off + " ... " +
				    (off+len) + "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_INT;

//	System.err.println("To convert " + to_convert);
//	System.err.println("Buffered " + buffered_bytes);

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_INT;

//		System.err.println("converting " + useable +
//			" ints from " + off);
		conversion.byte2int(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_INT;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

//		System.err.println("Leftover " + len + " ints to convert, " +
//			buffered_bytes + " bytes buffered" + 
//			to_convert + " bytes to convert");

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
//	System.err.println("converting " + len + " ints from " + index +
//				" to " + off);

	conversion.byte2int(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;

//	System.err.println("Done converting int [], buffer contains " +
//		buffered_bytes + " bytes (starting at " + index + ")");


    }

    public void readArray(long[] a, int off, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(long[" + off + " ... " + (off+len) +
		    "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_LONG;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_LONG;
		conversion.byte2long(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_LONG;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2long(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;
    }


    public void readArray(float[] a, int off, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(float[" + off + " ... " + (off+len) +
		    "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_FLOAT;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_FLOAT;
		conversion.byte2float(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_FLOAT;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2float(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;
    }

    public void readArray(double[] a, int off, int len)
	throws IOException {

	if (DEBUG) {
	    System.err.println("readArray(double[" + off + " ... " + (off+len)
		    + "])");
	}

	int useable, converted;
	int to_convert = len * SIZEOF_DOUBLE;

	while (buffered_bytes < to_convert) {
	    // not enough data in the buffer

	    if (buffered_bytes == 0) {
		index = 0;
		fillBuffer(min(BUF_SIZE, to_convert));
	    } else {
		// first, copy the data we do have to 'a' .
		useable = buffered_bytes / SIZEOF_DOUBLE;
		conversion.byte2double(buffer, index, a, off, useable);

		len -= useable;
		off += useable;

		converted = useable * SIZEOF_DOUBLE;
		index += converted;
		buffered_bytes -= converted;
		to_convert -= converted;

		// second, copy the leftovers to the start of the buffer.
		for (int i=0;i<buffered_bytes;i++) {
		    buffer[i] = buffer[index+i];
		}
		index = 0;

		// third, fill the buffer as far as possible.
		fillBuffer(min(BUF_SIZE, to_convert));
	    }
	}

	// enough data in the buffer
	conversion.byte2double(buffer, index, a, off, len);
	buffered_bytes -= to_convert;
	index += to_convert;
    }

    public void close() throws IOException {
	/* Ignore */
    }
}
