package ibis.io;

import ibis.ipl.IbisIOException;

/**
 * An Outputstream for writing arrays of primitive types.
 */

public abstract class ArrayOutputStream
	implements IbisStreamFlags {

    /**
     * Storage for bytes (or booleans) written.
     */
    public byte[]	byte_buffer    = new byte[BYTE_BUFFER_SIZE];

    /**
     * Storage for chars written.
     */
    public char[]	char_buffer    = new char[CHAR_BUFFER_SIZE];

    /**
     * Storage for shorts written.
     */
    public short[]	short_buffer   = new short[SHORT_BUFFER_SIZE];

    /**
     * Storage for ints written.
     */
    public int[]	int_buffer     = new int[INT_BUFFER_SIZE];

    /**
     * Storage for longs written.
     */
    public long[]	long_buffer    = new long[LONG_BUFFER_SIZE];

    /**
     * Storage for floats written.
     */
    public float[]	float_buffer   = new float[FLOAT_BUFFER_SIZE];

    /**
     * Storage for doubles written.
     */
    public double[]	double_buffer  = new double[DOUBLE_BUFFER_SIZE];

    /**
     * Storage for handles written.
     */
    public int[]	handle_buffer  = new int[HANDLE_BUFFER_SIZE];

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
     * Current index in <code>handle_buffer</code>.
     */
    public int		handle_index;

    /**
     * Writes (a slice of) an array of booleans.
     * @param a		the array to write
     * @param off	the offset at which to start
     * @param len	the number of elements to be written
     * @exception IOException on an IO error
     */
    public abstract void writeArray(boolean[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a byte array.
     */
    public abstract void writeArray(byte[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a short array.
     */
    public abstract void writeArray(short[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a char array.
     */
    public abstract void writeArray(char[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a int array.
     */
    public abstract void writeArray(int[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a long array.
     */
    public abstract void writeArray(long[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a float array.
     */
    public abstract void writeArray(float[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Like {@link #writeArray(boolean[], int, int)} but for a double array.
     */
    public abstract void writeArray(double[] a, int off, int len)
	    throws IbisIOException;

    /**
     * Return the number of bytes that was written to the message,
     * in the stream dependant format.
     * This is the number of bytes that will be sent over the network.
     */
    public abstract int bytesWritten();

    /**
     * Reset the counter for the number of bytes written.
     */
    public abstract void resetBytesWritten();

    /**
     * Initialize all buffer indices to zero.
     */
    final protected void reset_indices() {
	byte_index = 0;
	char_index = 0;
	short_index = 0;
	int_index = 0;
	long_index = 0;
	float_index = 0;
	double_index = 0;
	handle_index = 0;
    }

    protected short [] indices_short = new short[PRIMITIVE_TYPES];

    /**
     * Flush the primitive arrays.
     *
     * @exception IOException is thrown when any <code>writeArray</code>
     * throws it.
     */
    public void flushBuffers() throws IbisIOException {
	indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;
	indices_short[TYPE_HANDLE]  = (short) handle_index;

	if (DEBUG) {
	    System.out.println("writing bytes " + byte_index);
	    System.out.println("writing chars " + char_index);
	    System.out.println("writing shorts " + short_index);
	    System.out.println("writing ints " + int_index);
	    System.out.println("writing longs " + long_index);
	    System.out.println("writing floats " + float_index);
	    System.out.println("writing doubles " + double_index);
	    System.out.println("writing handles " + handle_index);
	}

	writeArray(indices_short, 0, PRIMITIVE_TYPES);

	if (byte_index > 0)    writeArray(byte_buffer, 0, byte_index);
	if (char_index > 0)    writeArray(char_buffer, 0, char_index);
	if (short_index > 0)   writeArray(short_buffer, 0, short_index);
	if (int_index > 0)     writeArray(int_buffer, 0, int_index);
	if (long_index > 0)    writeArray(long_buffer, 0, long_index);
	if (float_index > 0)   writeArray(float_buffer, 0, float_index);
	if (double_index > 0)  writeArray(double_buffer, 0, double_index);
	if (handle_index > 0)  writeArray(handle_buffer, 0, handle_index);

	reset_indices();
    }

    /**
     * Tells the underlying implementation to flush all the data.
     * @exception IOException on IO error.
     */
    public abstract void flush() throws IbisIOException;

    /**
     * Blocks until the data is written.
     * @exception IOException on IO error.
     */
    public abstract void finish() throws IbisIOException;

    /**
     * Tells the underlying implementation that this stream is closed.
     * @exception IOException on IO error.
     */
    public abstract void close() throws IbisIOException;
}
