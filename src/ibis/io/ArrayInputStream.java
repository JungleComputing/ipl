package ibis.io;

import java.io.IOException;

/**
 * An inputstream for reading arrays of primitive types.
 */

public abstract class ArrayInputStream implements IbisStreamFlags, IbisDissipator {

    /**
     * Each "bunch" of data is preceded by a header array, telling for
     * each type, how many of those must be read. This header array is
     * read into <code>indices_short</code>.
     */
    protected short[]	indices_short  = new short[PRIMITIVE_TYPES + 1];

    /**
     * Storage for bytes (or booleans) read.
     */
    public byte[]	byte_buffer    = new byte[BYTE_BUFFER_SIZE];

    /**
     * Storage for chars read.
     */
    public char[]	char_buffer    = new char[CHAR_BUFFER_SIZE];

    /**
     * Storage for shorts read.
     */
    public short[]	short_buffer   = new short[SHORT_BUFFER_SIZE];

    /**
     * Storage for ints read.
     */
    public int[]	int_buffer     = new int[INT_BUFFER_SIZE];

    /**
     * Storage for longs read.
     */
    public long[]	long_buffer    = new long[LONG_BUFFER_SIZE];

    /**
     * Storage for floats read.
     */
    public float[]	float_buffer   = new float[FLOAT_BUFFER_SIZE];

    /**
     * Storage for doubles read.
     */
    public double[]	double_buffer  = new double[DOUBLE_BUFFER_SIZE];

    /**
     * Current index in <code>byte_buffer</code>.
     */
    public int		byte_index;

    /**
     * Current index in <code>char_buffer</code>.
     */
    public int		char_index;

    /**
     * Current index in <code>short_buffer</code>.
     */
    public int		short_index;

    /**
     * Current index in <code>int_buffer</code>.
     */
    public int		int_index;

    /**
     * Current index in <code>long_buffer</code>.
     */
    public int		long_index;

    /**
     * Current index in <code>float_buffer</code>.
     */
    public int		float_index;

    /**
     * Current index in <code>double_buffer</code>.
     */
    public int		double_index;

    /**
     * Number of bytes in <code>byte_buffer</code>.
     */
    public int		max_byte_index;

    /**
     * Number of chars in <code>char_buffer</code>.
     */
    public int		max_char_index;

    /**
     * Number of shorts in <code>short_buffer</code>.
     */
    public int		max_short_index;

    /**
     * Number of ints in <code>int_buffer</code>.
     */
    public int		max_int_index;

    /**
     * Number of longs in <code>long_buffer</code>.
     */
    public int		max_long_index;

    /**
     * Number of floats in <code>float_buffer</code>.
     */
    public int		max_float_index;

    /**
     * Number of doubles in <code>double_buffer</code>.
     */
    public int		max_double_index;

    /**
     * Receive a new bunch of data.
     *
     * @exception IOException gets thrown when any of the reads throws it.
     */
    public void receive() throws IOException {
	if(ASSERTS) {
	    int sum = (max_byte_index - byte_index) + 
		    (max_char_index - char_index) + 
		    (max_short_index - short_index) + 
		    (max_int_index - int_index) + 
		    (max_long_index - long_index) + 
		    (max_float_index - float_index) + 
		    (max_double_index - double_index);
	    if (sum != 0) { 
		System.err.println("EEEEK : receiving while there is data in buffer !!!");
		System.err.println("byte_index "   + (max_byte_index - byte_index));
		System.err.println("char_index "   + (max_char_index - char_index));
		System.err.println("short_index "  + (max_short_index -short_index));
		System.err.println("int_index "    + (max_int_index - int_index));
		System.err.println("long_index "   + (max_long_index -long_index));
		System.err.println("double_index " + (max_double_index -double_index));
		System.err.println("float_index "  + (max_float_index - float_index));

		new Exception().printStackTrace();
		System.exit(1);
	    }
	}

	readArray(indices_short, 0, PRIMITIVE_TYPES);

	byte_index    = 0;
	char_index    = 0;
	short_index   = 0;
	int_index     = 0;
	long_index    = 0;
	float_index   = 0;
	double_index  = 0;

	max_byte_index    = indices_short[TYPE_BYTE];
	max_char_index    = indices_short[TYPE_CHAR];
	max_short_index   = indices_short[TYPE_SHORT];
	max_int_index     = indices_short[TYPE_INT];
	max_long_index    = indices_short[TYPE_LONG];
	max_float_index   = indices_short[TYPE_FLOAT];
	max_double_index  = indices_short[TYPE_DOUBLE];

	if(DEBUG) {
	    System.err.println("reading bytes " + max_byte_index);
	    System.err.println("reading char " + max_char_index);
	    System.err.println("reading short " + max_short_index);
	    System.err.println("reading int " + max_int_index);
	    System.err.println("reading long " + max_long_index);
	    System.err.println("reading float " + max_float_index);
	    System.err.println("reading double " + max_double_index);
	}

	if (max_byte_index > 0) {
	    readArray(byte_buffer, 0, max_byte_index);
	}
	if (max_char_index > 0) {
	    readArray(char_buffer, 0, max_char_index);
	}
	if (max_short_index > 0) {
	    readArray(short_buffer, 0, max_short_index);
	}
	if (max_int_index > 0) {
	    readArray(int_buffer, 0, max_int_index);
	}
	if (max_long_index > 0) {
	    readArray(long_buffer, 0, max_long_index);
	}
	if (max_float_index > 0) {
	    readArray(float_buffer, 0, max_float_index);
	}
	if (max_double_index > 0) {
	    readArray(double_buffer, 0, max_double_index);
	}
    }

    /**
     * Reads (a slice of) an array of booleans in place.
     *
     * @param a		array where data is to be stored
     * @param off	offset in <code>a</code> where the slice begins
     * @param len	the number of elements to be read
     *
     * @exception IOException on IO error.
     */
    public abstract void readArray(boolean[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of bytes in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(byte[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of shorts in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(short[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of chars in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(char[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of ints in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(int[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of longs in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(long[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of floats in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(float[] a, int off, int len)
	    throws IOException;

    /**
     * Reads (a slice of) an array of doubles in place.
     * See {@link #readArray(boolean[], int, int) for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(double[] a, int off, int len)
	    throws IOException;

    /**
     * Returns the number of bytes that can safely be read without
     * blocking.
     *
     * @return number of bytes.
     * @exception IOException on IO error.
     */
    public abstract int available() throws IOException;

    /**
     * The number of bytes read from the network 
     * since the last reset of this counter
     * @return The number of bytes read.
     */
    public int bytesRead() {
	System.err.println("IbisArrayInputStreamDissipator: bytesRead() not implemented (yet)");
	return 0;
    }

    /**
     * Resets the bytes read counter
     */
    public void resetBytesRead() {
	System.err.println("IbisArrayInputStreamDissipator: resetBytesRead() not implemented (yet)");
    }

    /**
     * Tells the underlying implementation that this input stream is
     * closing down.
     *
     * @exception IOException on IO error.
     */
    public abstract void close() throws IOException;

    /**
     * Reads a Boolean.
     * @return	The Boolean read from the Buffer.
     * @exception IOException when an IO error occurs.
     */
    public boolean readBoolean() throws IOException {
	while(byte_index == max_byte_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read boolean: " + 
	      ((byte_buffer[byte_index]) == (byte)1));
	}
	return (byte_buffer[byte_index++] == (byte) 1);
    }

    /**
     * Reads a Byte from the dissipator
     * @return	The Byte read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public byte readByte() throws IOException {
	while (byte_index == max_byte_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read byte: " + 
	      byte_buffer[byte_index]);
	}
	return byte_buffer[byte_index++];
    }

    /**
     * Reads a Character from the dissipator
     * @return	The Character read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public char readChar() throws IOException {
	while (char_index == max_char_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read char: " + 
	      char_buffer[char_index]);
	}
	return char_buffer[char_index++];
    }

    /**
     * Reads a Short from the dissipator
     * @return	The Short read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public short readShort() throws IOException {
	while (short_index == max_short_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read short: " + 
	      short_buffer[short_index]);
	}
	return short_buffer[short_index++];
    }

    /**
     * Reads a Integer from the dissipator
     * @return	The Integer read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public int readInt() throws IOException {
	while (int_index == max_int_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read int[HEX]: " + 
	      int_buffer[int_index] + "[" +
	      Integer.toHexString(int_buffer[int_index]) + "]");
	}
	return int_buffer[int_index++];
    }

    /**
     * Reads a Long from the dissipator
     * @return	The Long read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public long readLong() throws IOException {
	while (long_index == max_long_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read long: " + 
	      long_buffer[long_index]);
	}
	return long_buffer[long_index++];
    }

    /**
     * Reads a Float from the dissipator
     * @return	The Float read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public float readFloat() throws IOException {
	while (float_index == max_float_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read float: " + 
	      float_buffer[float_index]);
	}
	return float_buffer[float_index++];
    }

    /**
     * Reads a Double from the dissipator
     * @return	The Double read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public double readDouble() throws IOException {
	while (double_index == max_double_index) {
	    receive();
	}
	if (DEBUG) {
	    System.out.println(" Read double: " + 
	      double_buffer[double_index]);
	}
	return double_buffer[double_index++];
    }

}
