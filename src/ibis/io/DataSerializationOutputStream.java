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
     * The underlying <code>Accumulator</code>.
     */
    private final Accumulator out;

    /**
     * Allocator for the typed buffer arrays.
     */
    private DataAllocator	allocator;

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
	int		type;
	boolean[]	booleanArray;
	byte[]		byteArray;
	char[]		charArray;
	short[]		shortArray;
	int[]		intArray;
	long[]		longArray;
	float[]		floatArray;
	double[]	doubleArray;
	int		offset;
	int		len;
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
     * Constructor with an <code>Accumulator</code>.
     * @param out		the underlying <code>Accumulator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationOutputStream(Accumulator out)
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
     * Method to put a boolean array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayBoolean(boolean[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_BOOLEAN) {
// System.err.println("Special boolean array write len " + len);
	    /* Maybe lift the check from the writeBoolean? */
	    for (int i = offset; i < offset + len; i++) {
		writeBoolean(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayBoolean: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_BOOLEAN);
	    }
	    array[array_index].type   = TYPE_BOOLEAN;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].booleanArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_BOOLEAN, len);
	}
	stopTimer();
    }

    /**
     * Method to put a byte array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayByte(byte[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_BYTE) {
// System.err.println("Special byte array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeByte(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayByte: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_BYTE);
	    }
	    array[array_index].type   = TYPE_BYTE;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].byteArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_BYTE, len);
	}
	stopTimer();
    }

    /**
     * Method to put a char array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayChar(char[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_CHAR) {
// System.err.println("Special char array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeChar(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayChar: " + new String(ref) + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_CHAR);
	    }
	    array[array_index].type   = TYPE_CHAR;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].charArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_CHAR, len);
	}
	stopTimer();
    }

    /**
     * Method to put a short array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayShort(short[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_SHORT) {
// System.err.println("Special short array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeShort(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayShort: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_SHORT);
	    }
	    array[array_index].type   = TYPE_SHORT;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].shortArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_SHORT, len);
	}
	stopTimer();
    }

    /**
     * Method to put a int array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayInt(int[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_INT) {
// System.err.println("Special int array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeInt(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayInt: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_INT);
	    }
	    array[array_index].type   = TYPE_INT;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].intArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_INT, len);
	}
	stopTimer();
    }

    /**
     * Method to put a long array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayLong(long[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_LONG) {
// System.err.println("Special long array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeLong(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayLong: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_LONG);
	    }
	    array[array_index].type   = TYPE_LONG;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].longArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_LONG, len);
	}
	stopTimer();
    }

    /**
     * Method to put a float array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayFloat(float[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_FLOAT) {
// System.err.println("Special float array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeFloat(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayFloat: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_FLOAT);
	    }
	    array[array_index].type   = TYPE_FLOAT;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].floatArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_FLOAT, len);
	}
	stopTimer();
    }

    /**
     * Method to put a double array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream.
     * This method is public because it gets called from rewritten code.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     *
     * @exception IOException on IO error.
     */
    public void writeArrayDouble(double[] ref, int offset, int len)
	    throws IOException {
	startTimer();
	if (len < SMALL_ARRAY_BOUND / SIZEOF_DOUBLE) {
// System.err.println("Special double array write len " + len);
	    for (int i = offset; i < offset + len; i++) {
		writeDouble(ref[i]);
	    }

	} else {
	    if (array_index == ARRAY_BUFFER_SIZE) {
		flush();
	    }
	    if (DEBUG) {
		dbPrint("writeArrayDouble: " + ref + " offset: " 
			+ offset + " len: " + len + " type: " + TYPE_DOUBLE);
	    }
	    array[array_index].type   = TYPE_DOUBLE;
	    array[array_index].offset = offset;
	    array[array_index].len 	  = len;
	    array[array_index].doubleArray  = ref;
	    array_index++;

	    addStatSendArray(ref, TYPE_DOUBLE, len);
	}
	stopTimer();
    }

    void addStatSendArray(Object ref, int type, int len) {
    }

    /**
     * Flushes everything collected sofar.
     * @exception IOException on an IO error.
     */
    public void flush() throws IOException {

	if (DEBUG) {
	    dbPrint("doing a flush()");
	}

	suspendTimer();

	flushBuffers();

	/* Retain the order in which the arrays were pushed. This 
	 * costs a cast at receive time.
	 */
	for (int i = 0; i < array_index; i++) {
	    ArrayDescriptor a = array[i];
	    switch(a.type) {
	    case TYPE_BOOLEAN:
		out.writeArray(a.booleanArray, a.offset, a.len);
		break;
	    case TYPE_BYTE:
		out.writeArray(a.byteArray, a.offset, a.len);
		break;
	    case TYPE_CHAR:
		out.writeArray(a.charArray, a.offset, a.len);
		break;
	    case TYPE_SHORT:
		out.writeArray(a.shortArray, a.offset, a.len);
		break;
	    case TYPE_INT:
		out.writeArray(a.intArray, a.offset, a.len);
		break;
	    case TYPE_LONG:
		out.writeArray(a.longArray, a.offset, a.len);
		break;
	    case TYPE_FLOAT:
		out.writeArray(a.floatArray, a.offset, a.len);
		break;
	    case TYPE_DOUBLE:
		out.writeArray(a.doubleArray, a.offset, a.len);
		break;
	    }
	}

	array_index = 0;

	out.flush();

	resumeTimer();

	if (out instanceof ArrayOutputStream) {

	    ArrayOutputStream o = (ArrayOutputStream) out;

	    if (! o.finished()) {
		indices_short = allocator.getIndexArray();
		if (touched[TYPE_BYTE]) {
		    byte_buffer   = allocator.getByteArray();
		}
		if (touched[TYPE_CHAR]) {
		    char_buffer   = allocator.getCharArray();
		}
		if (touched[TYPE_SHORT]) {
		    short_buffer  = allocator.getShortArray();
		}
		if (touched[TYPE_INT]) {
		    int_buffer    = allocator.getIntArray();
		}
		if (touched[TYPE_LONG]) {
		    long_buffer   = allocator.getLongArray();
		}
		if (touched[TYPE_FLOAT]) {
		    float_buffer  = allocator.getFloatArray();
		}
		if (touched[TYPE_DOUBLE]) {
		    double_buffer = allocator.getDoubleArray();
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
	startTimer();
	if (byte_index == BYTE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote boolean " + value);
	}
	byte_buffer[byte_index++] = (byte) (value ? 1 : 0);
	stopTimer();
    }


    /**
     * Writes a byte value to the accumulator.
     * @param     value             The byte value to write.
     * @exception IOException on IO error.
     */
    public void writeByte(int value) throws IOException {
	startTimer();
	if (byte_index == BYTE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote byte " + value);
	}
	byte_buffer[byte_index++] = (byte) value;
	stopTimer();
    }

    /**
     * Writes a char value to the accumulator.
     * @param     value             The char value to write.
     * @exception IOException on IO error.
     */
    public void writeChar(char value) throws IOException {
	startTimer();
	if (char_index == CHAR_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote char " + value);
	}
	char_buffer[char_index++] = value;
	stopTimer();
    }

    /**
     * Writes a short value to the accumulator.
     * @param     value             The short value to write.
     * @exception IOException on IO error.
     */
    public void writeShort(int value) throws IOException {
	startTimer();
	if (short_index == SHORT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote short " + value);
	}
	short_buffer[short_index++] = (short) value;
	stopTimer();
    }

    /**
     * Writes a int value to the accumulator.
     * @param     value             The int value to write.
     * @exception IOException on IO error.
     */
    public void writeInt(int value) throws IOException {
	startTimer();
	if (int_index == INT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote int[HEX] " + value + "[0x" +
		    Integer.toHexString(value) + "]");
	}
	int_buffer[int_index++] = value;
	stopTimer();
    }

    /**
     * Writes a long value to the accumulator.
     * @param     value             The long value to write.
     * @exception IOException on IO error.
     */
    public void writeLong(long value) throws IOException {
	startTimer();
	if (long_index == LONG_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote long " + value);
	}
	long_buffer[long_index++] = value;
	stopTimer();
    }

    /**
     * Writes a float value to the accumulator.
     * @param     value             The float value to write.
     * @exception IOException on IO error.
     */
    public void writeFloat(float value) throws IOException {
	startTimer();
	if (float_index == FLOAT_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote float " + value);
	}
	float_buffer[float_index++] = value;
	stopTimer();
    }

    /**
     * Writes a double value to the accumulator.
     * @param     value             The double value to write.
     * @exception IOException on IO error.
     */
    public void writeDouble(double value) throws IOException {
	startTimer();
	if (double_index == DOUBLE_BUFFER_SIZE) {
	    flush();
	}
	if (DEBUG) {
	    dbPrint("wrote double " + value);
	}
	double_buffer[double_index++] = value;
	stopTimer();
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
	array = new ArrayDescriptor[ARRAY_BUFFER_SIZE];
	for (int i = 0; i < ARRAY_BUFFER_SIZE; i++) {
	    array[i] = new ArrayDescriptor();
	}
	allocator = new DataAllocator();
	indices_short  = allocator.getIndexArray();
	byte_buffer    = allocator.getByteArray();
	char_buffer    = allocator.getCharArray();
	short_buffer   = allocator.getShortArray();
	int_buffer     = allocator.getIntArray();
	long_buffer    = allocator.getLongArray();
	float_buffer   = allocator.getFloatArray();
	double_buffer  = allocator.getDoubleArray();
    }

    /**
     * The array buffer allocator. Use this to return data arrays
     * when they are finished. The allocator may cache/reuse them.
     */
    public DataAllocator getAllocator() {
	return allocator;
    }

    /**
     * Debugging print.
     * @param s	the string to be printed.
     */
    void dbPrint(String s) {
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

	if (DEBUG) {
	    dbPrint("writing bytes " + byte_index);
	    dbPrint("writing chars " + char_index);
	    dbPrint("writing shorts " + short_index);
	    dbPrint("writing ints " + int_index);
	    dbPrint("writing longs " + long_index);
	    dbPrint("writing floats " + float_index);
	    dbPrint("writing doubles " + double_index);
	}

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
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayBoolean(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(byte[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayByte(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayShort(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayChar(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayInt(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayLong(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayFloat(ref, off, len);
	stopTimer();
    }

    /**
     * {@inheritDoc}
     *
     * We must write something before the array to ensure that the receiver
     * reads its buffer bounds.
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	startTimer();
	writeInt(len);
	writeArrayDouble(ref, off, len);
	stopTimer();
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
