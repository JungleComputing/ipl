package ibis.io;

import java.io.Externalizable;
import java.io.IOException;
import java.io.NotActiveException;
import java.io.NotSerializableException;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.UTFDataFormatException;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class DataSerializationInputStream
	extends SerializationInputStream
	implements IbisStreamFlags
{
    /**
     * The underlying <code>IbisDissipator</code>.
     */
    private final IbisDissipator in;

    /**
     * Each "bunch" of data is preceded by a header array, telling for
     * each type, how many of those must be read. This header array is
     * read into <code>indices_short</code>.
     */
    private short[]	indices_short;

    /**
     * Storage for bytes (or booleans) read.
     */
    private byte[]	byte_buffer;

    /**
     * Storage for chars read.
     */
    private char[]	char_buffer;

    /**
     * Storage for shorts read.
     */
    private short[]	short_buffer;

    /**
     * Storage for ints read.
     */
    private int[]	int_buffer;

    /**
     * Storage for longs read.
     */
    private long[]	long_buffer;

    /**
     * Storage for floats read.
     */
    private float[]	float_buffer;

    /**
     * Storage for doubles read.
     */
    private double[]	double_buffer;

    /**
     * Current index in <code>byte_buffer</code>.
     */
    private int		byte_index;

    /**
     * Current index in <code>char_buffer</code>.
     */
    private int		char_index;

    /**
     * Current index in <code>short_buffer</code>.
     */
    private int		short_index;

    /**
     * Current index in <code>int_buffer</code>.
     */
    private int		int_index;

    /**
     * Current index in <code>long_buffer</code>.
     */
    private int		long_index;

    /**
     * Current index in <code>float_buffer</code>.
     */
    private int		float_index;

    /**
     * Current index in <code>double_buffer</code>.
     */
    private int		double_index;

    /**
     * Number of bytes in <code>byte_buffer</code>.
     */
    private int		max_byte_index;

    /**
     * Number of chars in <code>char_buffer</code>.
     */
    private int		max_char_index;

    /**
     * Number of shorts in <code>short_buffer</code>.
     */
    private int		max_short_index;

    /**
     * Number of ints in <code>int_buffer</code>.
     */
    private int		max_int_index;

    /**
     * Number of longs in <code>long_buffer</code>.
     */
    private int		max_long_index;

    /**
     * Number of floats in <code>float_buffer</code>.
     */
    private int		max_float_index;

    /**
     * Number of doubles in <code>double_buffer</code>.
     */
    private int		max_double_index;

    /**
     * Constructor with an <code>IbisDissipator</code>.
     * @param in		the underlying <code>IbisDissipator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationInputStream(IbisDissipator in) throws IOException {
	super();
	this.in = in;
	initArrays();
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected DataSerializationInputStream() throws IOException {
	super();
	in = null;
    }

    /**
     * {@inheritDoc}
     */
    public String serializationImplName() {
	return "data";
    }

    /**
     * {@inheritDoc}
     */
    public boolean readBoolean() throws IOException {
	while(byte_index == max_byte_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read boolean: " + ((byte_buffer[byte_index]) != (byte)0));
	}
	return (byte_buffer[byte_index++] != (byte)0);
    }

    /**
     * {@inheritDoc}
     */
    public byte readByte() throws IOException {
	while (byte_index == max_byte_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read byte: " + byte_buffer[byte_index]);
	}
	return byte_buffer[byte_index++];
    }

    /**
     * {@inheritDoc}
     */
    public char readChar() throws IOException {
	while (char_index == max_char_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read char: " + char_buffer[char_index]);
	}
	return char_buffer[char_index++];
    }

    /**
     * {@inheritDoc}
     */
    public short readShort() throws IOException {
	while (short_index == max_short_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read short: " + short_buffer[short_index]);
	}
	return short_buffer[short_index++];
    }

    /**
     * {@inheritDoc}
     */
    public int readInt() throws IOException {
	while (int_index == max_int_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read int[HEX]: " + int_buffer[int_index] + "[0x" +
		    Integer.toHexString(int_buffer[int_index]) + "]");
	}
	return int_buffer[int_index++];
    }

    /**
     * {@inheritDoc}
     */
    public long readLong() throws IOException {
	while (long_index == max_long_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read long: " + long_buffer[long_index]);
	}
	return long_buffer[long_index++];
    }

    /**
     * {@inheritDoc}
     */
    public float readFloat() throws IOException {
	while (float_index == max_float_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read float: " + float_buffer[float_index]);
	}
	return float_buffer[float_index++];
    }

    /**
     * {@inheritDoc}
     */
    public double readDouble() throws IOException {
	while (double_index == max_double_index) {
	    receive();
	}
	if (DEBUG) {
	    dbPrint("read double: " + double_buffer[double_index]);
	}
	return double_buffer[double_index++];
    }

    /**
     * Reads (part of) an array of booleans.
     * This method is here to make extending this class easier.
     */
    protected void readBooleanArray(boolean ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of bytes.
     * This method is here to make extending this class easier.
     */
    protected void readByteArray(byte ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of chars.
     * This method is here to make extending this class easier.
     */
    protected void readCharArray(char ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of shorts.
     * This method is here to make extending this class easier.
     */
    protected void readShortArray(short ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of ints.
     * This method is here to make extending this class easier.
     */
    protected void readIntArray(int ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of longs.
     * This method is here to make extending this class easier.
     */
    protected void readLongArray(long ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of floats.
     * This method is here to make extending this class easier.
     */
    protected void readFloatArray(float ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of doubles.
     * This method is here to make extending this class easier.
     */
    protected void readDoubleArray(double ref[], int off, int len)
	    throws IOException {
	in.readArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public int available() throws IOException {
        return in.available() +
		(max_byte_index - byte_index) * SIZEOF_BYTE +
		(max_char_index - char_index) * SIZEOF_CHAR +
		(max_short_index - short_index) * SIZEOF_SHORT +
		(max_int_index - int_index) * SIZEOF_INT +
		(max_long_index - long_index) * SIZEOF_LONG +
		(max_float_index - float_index) * SIZEOF_FLOAT +
		(max_double_index - double_index) * SIZEOF_DOUBLE;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	in.close();
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	readBooleanArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
	readByteArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	readCharArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	readShortArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	readIntArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	readLongArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	readFloatArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	readDoubleArray(ref, off, len);
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void readArray(Object[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * Allocates arrays.
     */
    private void initArrays() {
	indices_short  = new short[PRIMITIVE_TYPES];
	byte_buffer    = new byte[BYTE_BUFFER_SIZE];
	char_buffer    = new char[CHAR_BUFFER_SIZE];
	short_buffer   = new short[SHORT_BUFFER_SIZE];
	int_buffer     = new int[INT_BUFFER_SIZE];
	long_buffer    = new long[LONG_BUFFER_SIZE];
	float_buffer   = new float[FLOAT_BUFFER_SIZE];
	double_buffer  = new double[DOUBLE_BUFFER_SIZE];
    }

    /**
     * Debugging print.
     * @param s	the string to be printed.
     */
    private void dbPrint(String s) {
	debuggerPrint(this + ": " + s);
    }

    /**
     * Debugging print, also for DataSerializationOutputStream.
     * @param s the string to be printed.
     */
    protected synchronized static void debuggerPrint(String s) {
	System.err.println(s);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
    }

    /**
     * {@inheritDoc}
     */
    public void statistics() {
    }

    /* This is the data output / object output part */

    /**
     * {@inheritDoc}
     */
    public int read() throws IOException {
	return readByte();
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
	return len;
    }

    /**
     * {@inheritDoc}
     */
    public long skip(long n) throws IOException {
	throw new IOException("skip not meaningful in typed input stream");
    }

    /**
     * {@inheritDoc}
     */
    public int skipBytes(int n) throws IOException {
	throw new IOException("skipBytes not meaningful in typed input stream");
    }


    /**
     * {@inheritDoc}
     */
    public void readFully(byte[] b) throws IOException {
	readFully(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
    }

    /**
     * Receive a new bunch of data.
     *
     * @exception IOException gets thrown when any of the reads throws it.
     */
    private void receive() throws IOException {
	if (DEBUG) {
	    dbPrint("doing a receive()");
	}
	if(ASSERTS) {
	    int sum = (max_byte_index - byte_index) + 
		    (max_char_index - char_index) + 
		    (max_short_index - short_index) + 
		    (max_int_index - int_index) + 
		    (max_long_index - long_index) + 
		    (max_float_index - float_index) + 
		    (max_double_index - double_index);
	    if (sum != 0) { 
		dbPrint("EEEEK : receiving while there is data in buffer !!!");
		dbPrint("byte_index "   + (max_byte_index - byte_index));
		dbPrint("char_index "   + (max_char_index - char_index));
		dbPrint("short_index "  + (max_short_index -short_index));
		dbPrint("int_index "    + (max_int_index - int_index));
		dbPrint("long_index "   + (max_long_index -long_index));
		dbPrint("double_index " + (max_double_index -double_index));
		dbPrint("float_index "  + (max_float_index - float_index));

		throw new SerializationError("Internal error!");
	    }
	}

	in.readArray(indices_short, BEGIN_TYPES, PRIMITIVE_TYPES-BEGIN_TYPES);

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
	    dbPrint("reading bytes " + max_byte_index);
	    dbPrint("reading char " + max_char_index);
	    dbPrint("reading short " + max_short_index);
	    dbPrint("reading int " + max_int_index);
	    dbPrint("reading long " + max_long_index);
	    dbPrint("reading float " + max_float_index);
	    dbPrint("reading double " + max_double_index);
	}

	if (max_byte_index > 0) {
	    in.readArray(byte_buffer, 0, max_byte_index);
	}
	if (max_char_index > 0) {
	    in.readArray(char_buffer, 0, max_char_index);
	}
	if (max_short_index > 0) {
	    in.readArray(short_buffer, 0, max_short_index);
	}
	if (max_int_index > 0) {
	    in.readArray(int_buffer, 0, max_int_index);
	}
	if (max_long_index > 0) {
	    in.readArray(long_buffer, 0, max_long_index);
	}
	if (max_float_index > 0) {
	    in.readArray(float_buffer, 0, max_float_index);
	}
	if (max_double_index > 0) {
	    in.readArray(double_buffer, 0, max_double_index);
	}
    }

    /**
     * {@inheritDoc}
     */
    public final int readUnsignedByte() throws IOException {
	int i = readByte();
	if (i < 0) {
	    i += 256;
	}
	return i;
    }

    /**
     * {@inheritDoc}
     */
    public final int readUnsignedShort() throws IOException {
	int i = readShort();
	if (i < 0) {
	    i += 65536;
	}
	return i;
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public String readUTF() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public Class readClass() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public String readString() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public final Object readObjectOverride() throws IOException
    {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public GetField readFields() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public void defaultReadObject() throws IOException {
	throw new IOException("Illegal data type read");
    }
}
