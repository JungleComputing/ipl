package ibis.io;

import ibis.ipl.IbisIOException;

/**
 *
 * Extends OutputStream with write of array of primitives and writeSingleInt
 **/

public abstract class ArrayOutputStream
	implements TypeSize, IbisStreamFlags {

    public byte[]   byte_buffer   = new byte[BYTE_BUFFER_SIZE];
    public char[]   char_buffer   = new char[CHAR_BUFFER_SIZE];
    public short[]  short_buffer  = new short[SHORT_BUFFER_SIZE];
    public int[]    int_buffer    = new int[INT_BUFFER_SIZE];
    public long[]   long_buffer   = new long[LONG_BUFFER_SIZE];
    public float[]  float_buffer  = new float[FLOAT_BUFFER_SIZE];
    public double[] double_buffer = new double[DOUBLE_BUFFER_SIZE];
    public int[]    handle_buffer = new int[HANDLE_BUFFER_SIZE];

    public int	byte_index;
    public int	char_index;
    public int	short_index;
    public int	int_index;
    public int	long_index;
    public int	float_index;
    public int	double_index;
    public int	handle_index;

    public abstract void writeArray(boolean[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(byte[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(short[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(char[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(int[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(long[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(float[] a, int off, int len)
	    throws IbisIOException;

    public abstract void writeArray(double[] a, int off, int len)
	    throws IbisIOException;

    public abstract int bytesWritten();
    public abstract void resetBytesWritten();

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

    /* protected */ public void flushBuffers() throws IbisIOException {

//System.err.println("Call this base type flushBuffers. HAVOC");
	indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;
	indices_short[TYPE_HANDLE]  = (short) handle_index;

//    indices_short[PRIMITIVE_TYPES] = (short)  (eof ? 1 : 0);

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
//System.err.println("Sure, reset them indeces");
    }

    public abstract void flush() throws IbisIOException;

//    /* protected */ public abstract void flush(boolean lastFrag) throws IbisIOException;

    /* protected */ public abstract void finish() throws IbisIOException;

    public abstract void close() throws IbisIOException;
}
