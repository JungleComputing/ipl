package ibis.io;

import java.io.IOException;


/**
 * This is the <code>SerializationOutputStream</code> version that is used
 * for data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class DataSerializationOutputStream
	extends SerializationOutputStream
	implements IbisStreamFlags
{
    /**
     * The underlying <code>IbisAccumulator</code>.
     */
    private final IbisAccumulator out;

    /**
     * Storage for bytes (or booleans) written.
     */
    private byte[]	byte_buffer;

    /**
     * Storage for chars written.
     */
    private char[]	char_buffer;

    /**
     * Storage for shorts written.
     */
    private short[]	short_buffer;

    /**
     * Storage for ints written.
     */
    private int[]	int_buffer;

    /**
     * Storage for longs written.
     */
    private long[]	long_buffer;

    /**
     * Storage for floats written.
     */
    private float[]	float_buffer;

    /**
     * Storage for doubles written.
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
     * Register how often we need to acquire a new set of primitive array
     * buffers.
    private int unfinished;

    {
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    public void run() {
		System.err.println(DataSerializationOutputStream.this +
				    ": unfinished calls " + unfinished);
		statistics();
	    }
	});
    }
     */

    /**
     * Structure summarizing an array write.
     */
    private static final class ArrayDescriptor {
	int	type;
	Object	array;
	int	offset;
	int	len;
    }

    /**
     * Where the arrays to be written are collected.
     */
    private ArrayDescriptor[] 	array;

    /**
     * Index in the <code>array</code> array.
     */
    private int			array_index;

    /**
     * Collects all indices of the <code>_buffer</code> arrays.
     */
    private short[]	indices_short;

    /**
     * For each 
     */
    private boolean[] touched = new boolean[PRIMITIVE_TYPES];

    /**
     * Constructor with an <code>IbisAccumulator</code>.
     * @param out		the underlying <code>IbisAccumulator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationOutputStream(IbisAccumulator out)
							 throws IOException {
	super();

	this.out    = out;
	initArrays();
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected DataSerializationOutputStream() throws IOException {

	super();

	out = null;
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
    public void statistics() {
    }

    /**
     * Method to put a array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     * @param type	type of the array elements
     *
     * @exception IOException on IO error.
     */
    public void writeArray(Object ref, int offset, int len, int type)
	    throws IOException {
	if (array_index == ARRAY_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("writeArray: " + ref + " offset: " 
		    + offset + " len: " + len + " type: " + type);
	}
	array[array_index].type   = type;
	array[array_index].offset = offset;
	array[array_index].len 	  = len;
	array[array_index].array  = ref;
	array_index++;
    }

    /**
     * Flushes everything collected sofar.
     * @exception IOException on an IO error.
     */
    public void flush() throws IOException {

	if (DEBUG) {
	    dbPrint("doing a flush()");
	}
	flushBuffers();

	/* Retain the order in which the arrays were pushed. This 
	 * costs a cast at send/receive.
	 */
	for (int i = 0; i < array_index; i++) {
	    ArrayDescriptor a = array[i];
	    switch(a.type) {
	    case TYPE_BOOLEAN:
		out.writeArray( (boolean[])(a.array), a.offset, a.len);
		break;
	    case TYPE_BYTE:
		out.writeArray( (byte[])(a.array), a.offset, a.len);
		break;
	    case TYPE_CHAR:
		out.writeArray( (char[])(a.array), a.offset, a.len);
		break;
	    case TYPE_SHORT:
		out.writeArray( (short[])(a.array), a.offset, a.len);
		break;
	    case TYPE_INT:
		out.writeArray( (int[])(a.array), a.offset, a.len);
		break;
	    case TYPE_LONG:
		out.writeArray( (long[])(a.array), a.offset, a.len);
		break;
	    case TYPE_FLOAT:
		out.writeArray( (float[])(a.array), a.offset, a.len);
		break;
	    case TYPE_DOUBLE:
		out.writeArray( (double[])(a.array), a.offset, a.len);
		break;
	    }
	}

	array_index = 0;

	out.flush();

	if (out instanceof ArrayOutputStream) {

	    ArrayOutputStream o = (ArrayOutputStream) out;

	    if (! o.finished()) {
		indices_short = new short[PRIMITIVE_TYPES];
		if (touched[TYPE_BYTE]) {
		    byte_buffer   = new byte[BYTE_BUFFER_SIZE];
		}
		if (touched[TYPE_CHAR]) {
		    char_buffer   = new char[CHAR_BUFFER_SIZE];
		}
		if (touched[TYPE_SHORT]) {
		    short_buffer  = new short[SHORT_BUFFER_SIZE];
		}
		if (touched[TYPE_INT]) {
		    int_buffer    = new int[INT_BUFFER_SIZE];
		}
		if (touched[TYPE_LONG]) {
		    long_buffer   = new long[LONG_BUFFER_SIZE];
		}
		if (touched[TYPE_FLOAT]) {
		    float_buffer  = new float[FLOAT_BUFFER_SIZE];
		}
		if (touched[TYPE_DOUBLE]) {
		    double_buffer = new double[DOUBLE_BUFFER_SIZE];
		}
// unfinished++;
	    }
	}

	for (int i = 0; i < PRIMITIVE_TYPES; i++) {
	    touched[i] = false;
	}
    }

    /**
     * Writes a boolean value to the accumulator.
     * @param     value             The boolean value to write.
     * @exception IOException on IO error.
     */
    public void writeBoolean(boolean value) throws IOException {
	if (byte_index == BYTE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote boolean " + value);
	}
	byte_buffer[byte_index++] = (byte) (value ? 1 : 0);
    }


    /**
     * Writes a byte value to the accumulator.
     * @param     value             The byte value to write.
     * @exception IOException on IO error.
     */
    public void writeByte(int value) throws IOException {
	if (byte_index == BYTE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote byte " + value);
	}
	byte_buffer[byte_index++] = (byte) value;
    }

    /**
     * Writes a char value to the accumulator.
     * @param     value             The char value to write.
     * @exception IOException on IO error.
     */
    public void writeChar(char value) throws IOException {
	if (char_index == CHAR_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote char " + value);
	}
	char_buffer[char_index++] = value;
    }

    /**
     * Writes a short value to the accumulator.
     * @param     value             The short value to write.
     * @exception IOException on IO error.
     */
    public void writeShort(int value) throws IOException {
	if (short_index == SHORT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote short " + value);
	}
	short_buffer[short_index++] = (short) value;
    }

    /**
     * Writes a int value to the accumulator.
     * @param     value             The int value to write.
     * @exception IOException on IO error.
     */
    public void writeInt(int value) throws IOException {
	if (int_index == INT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote int[HEX] " + value + "[0x" +
		    Integer.toHexString(value) + "]");
	}
	int_buffer[int_index++] = value;
    }

    /**
     * Writes a long value to the accumulator.
     * @param     value             The long value to write.
     * @exception IOException on IO error.
     */
    public void writeLong(long value) throws IOException {
	if (long_index == LONG_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote long " + value);
	}
	long_buffer[long_index++] = value;
    }

    /**
     * Writes a float value to the accumulator.
     * @param     value             The float value to write.
     * @exception IOException on IO error.
     */
    public void writeFloat(float value) throws IOException {
	if (float_index == FLOAT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote float " + value);
	}
	float_buffer[float_index++] = value;
    }

    /**
     * Writes a double value to the accumulator.
     * @param     value             The double value to write.
     * @exception IOException on IO error.
     */
    public void writeDouble(double value) throws IOException {
	if (double_index == DOUBLE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote double " + value);
	}
	double_buffer[double_index++] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws IOException {
	flush();
	out.close();
    }

    /*
     * If you are overriding DataSerializationOutputStream,
     * you can stop now :-) 
     * The rest is built on top of these.
     */

    /**
     * Allocates buffers.
     */
    private void initArrays() {
	indices_short  = new short[PRIMITIVE_TYPES];
	array = new ArrayDescriptor[ARRAY_BUFFER_SIZE];
	for (int i = 0; i < ARRAY_BUFFER_SIZE; i++) {
	    array[i] = new ArrayDescriptor();
	}
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
	DataSerializationInputStream.debuggerPrint(this + ": " + s);
    }

    /**
     * {@inheritDoc}
     */
    public void reset() throws IOException {
    }

    /* This is the data output / object output part */

    /**
     * {@inheritDoc}
     */
    public void write(int v) throws IOException {
	writeByte((byte)(0xff & v));
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b) throws IOException {
	write(b, 0, b.length);
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] b, int off, int len) throws IOException {
	writeArray(b, off, len);
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeUTF(String str) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeClass(Class ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * Initialize all buffer indices to zero.
     */
    private void reset_indices() {
	byte_index = 0;
	char_index = 0;
	short_index = 0;
	int_index = 0;
	long_index = 0;
	float_index = 0;
	double_index = 0;
    }


    /**
     * Flush the primitive arrays.
     *
     * @exception IOException is thrown when any <code>writeArray</code>
     * throws it.
     */
    private void flushBuffers() throws IOException {
	indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;

	out.writeArray(indices_short, BEGIN_TYPES, PRIMITIVE_TYPES-BEGIN_TYPES);

	if (byte_index > 0) {
	    out.writeArray(byte_buffer, 0, byte_index);
	    touched[TYPE_BYTE] = true;
	}
	if (char_index > 0) {
	    out.writeArray(char_buffer, 0, char_index);
	    touched[TYPE_CHAR] = true;
	}
	if (short_index > 0) {
	    out.writeArray(short_buffer, 0, short_index);
	    touched[TYPE_SHORT] = true;
	}
	if (int_index > 0) {
	    out.writeArray(int_buffer, 0, int_index);
	    touched[TYPE_INT] = true;
	}
	if (long_index > 0) {
	    out.writeArray(long_buffer, 0, long_index);
	    touched[TYPE_LONG] = true;
	}
	if (float_index > 0) {
	    out.writeArray(float_buffer, 0, float_index);
	    touched[TYPE_FLOAT] = true;
	}
	if (double_index > 0) {
	    out.writeArray(double_buffer, 0, double_index);
	    touched[TYPE_DOUBLE] = true;
	}

	reset_indices();
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeBytes(String s) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeChars(String s) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_BOOLEAN);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(byte[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_BYTE);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_SHORT);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_CHAR);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_INT);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_LONG);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_FLOAT);
    }

    /**
     * {@inheritDoc}
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	writeArray(ref, off, len, TYPE_DOUBLE);
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeArray(Object[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeObjectOverride(Object ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeUnshared(Object ref) throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * {@inheritDoc}
     */
    public void useProtocolVersion(int version) {
	/* ignored. */
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void writeFields() throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public PutField putFields() throws IOException {
	throw new IOException("Illegal data type written");
    }

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void defaultWriteObject() throws IOException {
	throw new IOException("Illegal data type written");
    }
}
