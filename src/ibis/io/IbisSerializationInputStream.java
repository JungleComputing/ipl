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
 * for Ibis serialization.
 * An effort has been made to make it look like and extend
 * <code>java.io.ObjectInputStream</code>.
 * However, versioning is not supported, like it is in Sun serialization.
 */
public class IbisSerializationInputStream
	extends SerializationInputStream
	implements IbisStreamFlags
{
    private ClassLoader customClassLoader;
      
    /**
     * List of objects, for cycle checking.
     */
    private IbisVector objects;

    /**
     * First free object index.
     */
    private int next_handle;

    /**
     * The underlying <code>IbisDissipator</code>.
     */
    private final IbisDissipator in;

    /**
     * First free type index.
     */
    private int next_type = 1;

    /**
     * List of types seen sofar.
     */
    private IbisVector types;

    /**
     * There is a notion of a "current" object. This is needed when a
     * user-defined <code>readObject</code> refers to
     * <code>defaultReadObject</code> or to
     * <code>getFields</code>.
     */
    private Object current_object;

    /**
     * There also is a notion of a "current" level.
     * The "level" of a serializable class is computed as follows:<ul>
     * <li> if its superclass is serializable: the level of the superclass + 1.
     * <li> if its superclass is not serializable: 1.
     * </ul>
     * This level implies a level at which an object can be seen. The "current"
     * level is the level at which <code>current_object</code> is being
     * processed.
     */
    private int current_level;

    /**
     * The <code>current_object</code> and <code>current_level</code>
     * are maintained in
     * stacks, so that they can be managed by IOGenerator-generated code.
     */
    private Object[] object_stack;
    private int[] level_stack;
    private int max_stack_size = 0;
    private int stack_size = 0;

    /**
     * <code>IbisTypeInfo</code> for <code>boolean</code> arrays.
     */
    private static IbisTypeInfo booleanArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classBooleanArray);

    /**
     * <code>IbisTypeInfo</code> for <code>byte</code> arrays.
     */
    private static IbisTypeInfo byteArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classByteArray);

    /**
     * <code>IbisTypeInfo</code> for <code>char</code> arrays.
     */
    private static IbisTypeInfo charArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classCharArray);

    /**
     * <code>IbisTypeInfo</code> for <code>short</code> arrays.
     */
    private static IbisTypeInfo shortArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classShortArray);

    /**
     * <code>IbisTypeInfo</code> for <code>int</code> arrays.
     */
    private static IbisTypeInfo intArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classIntArray);

    /**
     * <code>IbisTypeInfo</code> for <code>long</code> arrays.
     */
    private static IbisTypeInfo longArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classLongArray);

    /**
     * <code>IbisTypeInfo</code> for <code>float</code> arrays.
     */
    private static IbisTypeInfo floatArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classFloatArray);

    /**
     * <code>IbisTypeInfo</code> for <code>double</code> arrays.
     */
    private static IbisTypeInfo doubleArrayInfo =
	IbisTypeInfo.getIbisTypeInfo(classDoubleArray);

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
    public IbisSerializationInputStream(IbisDissipator in) throws IOException {
	super();
	objects = new IbisVector(1024);
	init(true);
	this.in = in;
	initArrays();
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected IbisSerializationInputStream() throws IOException {
	super();
	objects = new IbisVector(1024);
	init(true);
	in = null;
    }

    /*
     * If you at some point want to override IbisSerializationOutputStream,
     * you probably need to override the methods from here on up until
     * comment tells you otherwise.
     */

    /**
     * {@inheritDoc}
     */
    public String serializationImplName() {
	return "ibis";
    }

    /**
     * {@inheritDoc}
     */
    public final boolean readBoolean() throws IOException {
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
    public final byte readByte() throws IOException {
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
    public final char readChar() throws IOException {
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
    public final short readShort() throws IOException {
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
    public final int readInt() throws IOException {
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
    public final long readLong() throws IOException {
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
    public final float readFloat() throws IOException {
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
    public final double readDouble() throws IOException {
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
	types = null;
	objects.clear();
	in.close();
    }

    /*
     * If you are overriding IbisSerializationInputStream,
     * you can stop now :-) 
     * The rest is built on top of these.
     */

    /**
     * {@inheritDoc}
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classBooleanArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require boolean[]", e);
	}
	readBooleanArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classByteArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require byte[]", e);
	}
	readByteArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classCharArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require char[]", e);
	}
	readCharArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classShortArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require short[]", e);
	}
	readShortArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classIntArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require int[]", e);
	}
	readIntArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classLongArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require long[]", e);
	}
	readLongArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classFloatArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require float[]", e);
	}
	readFloatArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	try {
	    readArrayHeader(classDoubleArray, len);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("require double[]", e);
	}
	readDoubleArray(ref, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public void readArray(Object[] ref, int off, int len)
	throws IOException, ClassNotFoundException {
	readArrayHeader(ref.getClass(), len);
	for (int i = off; i < off + len; i++) {
	    ref[i] = readObjectOverride();
	}
    }

    /**
     * Allocates and reads an array of bytes from the input stream.
     * This method is used by IOGenerator-generated code.
     * @return the array read.
     * @exception IOException in case of error.
     */
    public byte[] readArrayByte() throws IOException {
	int len = readInt();
	byte[] b = new byte[len];
	addObjectToCycleCheck(b);
	readByteArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of boolans.
     */
    public boolean[] readArrayBoolean() throws IOException {
	int len = readInt();
	boolean[] b = new boolean[len];
	addObjectToCycleCheck(b);
	readBooleanArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of chars.
     */
    public char[] readArrayChar() throws IOException {
	int len = readInt();
	char[] b = new char[len];
	addObjectToCycleCheck(b);
	readCharArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of shorts.
     */
    public short[] readArrayShort() throws IOException {
	int len = readInt();
	short[] b = new short[len];
	addObjectToCycleCheck(b);
	readShortArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of ints.
     */
    public int[] readArrayInt() throws IOException {
	int len = readInt();
	int[] b = new int[len];
	addObjectToCycleCheck(b);
	readIntArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of longs.
     */
    public long[] readArrayLong() throws IOException {
	int len = readInt();
	long[] b = new long[len];
	addObjectToCycleCheck(b);
	readLongArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of floats.
     */
    public float[] readArrayFloat() throws IOException {
	int len = readInt();
	float[] b = new float[len];
	addObjectToCycleCheck(b);
	readFloatArray(b, 0, len);
	return b;
    }

    /**
     * See {@link #readArrayByte()}, this one is for an array of doubles.
     */
    public double[] readArrayDouble() throws IOException {
	int len = readInt();
	double[] b = new double[len];
	addObjectToCycleCheck(b);
	readDoubleArray(b, 0, len);
	return b;
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
     * Debugging print, also for IbisSerializationOutputStream.
     * @param s the string to be printed.
     */
    protected synchronized static void debuggerPrint(String s) {
	System.err.println(s);
    }

    /**
     * Initializes the <code>objects</code> and <code>types</code> fields,
     * including their indices.
     *
     * @param do_types	set when the type table must be initialized as well
     *  (this is not needed after a reset).
     */
    private void init(boolean do_types) {
	if (do_types) {
	    types = new IbisVector();
	    types.add(0, null);	// Vector requires this
	    types.add(TYPE_BOOLEAN,	booleanArrayInfo);
	    types.add(TYPE_BYTE,	byteArrayInfo);
	    types.add(TYPE_CHAR,	charArrayInfo);
	    types.add(TYPE_SHORT,	shortArrayInfo);
	    types.add(TYPE_INT,		intArrayInfo);
	    types.add(TYPE_LONG,	longArrayInfo);
	    types.add(TYPE_FLOAT,	floatArrayInfo);
	    types.add(TYPE_DOUBLE,	doubleArrayInfo);

	    next_type = PRIMITIVE_TYPES;
	}

	objects.clear();
	next_handle = CONTROL_HANDLES;
    }

    /**
     * resets the stream, by clearing the object and type table.
     */
    private void do_reset() {
	if (DEBUG) {
	    dbPrint("received reset: next handle = " + next_handle + ".");
	}
	init(false);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
	if (DEBUG) {
	    dbPrint("explicit clear: next handle = " + next_handle + ".");
	}
	init(false);
    }

    /**
     * {@inheritDoc}
     */
    public void statistics() {
	System.err.println("IbisSerializationInputStream: " +
			   "statistics() not yet implemented");
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
	read(b, off, len);
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
     * Reads a handle, which is just an int representing an index
     * in the object table.
     * @exception IOException	gets thrown when an IO error occurs.
     * @return 			the handle read.
     */
    private final int readHandle() throws IOException {
	int handle = readInt();

	/* this replaces the checks for the reset handle
	   everywhere else. --N */
	while(handle == RESET_HANDLE) {
	    if (DEBUG) {
		dbPrint("received a RESET");
	    }
	    do_reset();
	    handle = readInt();
	}

	if (DEBUG) {
	    dbPrint("read handle " + handle);
	}

	return handle;
    }

    /**
     * {@inheritDoc}
     */
    public String readUTF() throws IOException {
	int bn = readInt();

	if (DEBUG) {
	    dbPrint("readUTF: len = " + bn);
	}

	if (bn == -1) {
	    return null;
	}

	byte[] b = new byte[bn];
	readArray(b, 0, bn);

	int len = 0;
	char[] c = new char[bn];

	for (int i = 0; i < bn; i++) {
	    if ((b[i] & ~0x7f) == 0) {
		c[len++] = (char)(b[i] & 0x7f);
	    } else if ((b[i] & ~0x1f) == 0xc0) {
		if (i + 1 >= bn || (b[i + 1] & ~0x3f) != 0x80) {
		    throw new UTFDataFormatException(
				    "UTF Data Format Exception");
		}
		c[len++] = (char)(((b[i] & 0x1f) << 6) | (b[i] & 0x3f));
		i++;
	    } else if ((b[i] & ~0x0f) == 0xe0) {
		if (i + 2 >= bn ||
		    (b[i + 1] & ~0x3f) != 0x80 ||
		    (b[i + 2] & ~0x3f) != 0x80) {
		    throw new UTFDataFormatException(
				    "UTF Data Format Exception");
		}
		c[len++] = (char)(((b[i] & 0x0f) << 12) |
				  ((b[i+1] & 0x3f) << 6) | 
				  (b[i+2] & 0x3f));
	    } else {
		throw new UTFDataFormatException("UTF Data Format Exception");
	    }
	}

	String s = new String(c, 0, len);
	// dbPrint("readUTF: " + s);

	if (DEBUG) {
	    dbPrint("read string "  + s);
	}
	return s;
    }

    /**
     * Reads a <code>Class</code> object from the stream and tries to load it.
     * @exception IOException when an IO error occurs.
     * @exception ClassNotFoundException when the class could not be loaded.
     * @return the <code>Class</code> object read.
     */
    public Class readClass() throws IOException, ClassNotFoundException {
	int handle = readHandle();

	if (handle == NUL_HANDLE) {
	    return null;
	}

	if ((handle & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    Class o = (Class) objects.get(handle);

	    if (DEBUG) {
		dbPrint("readobj: handle = " + (handle - CONTROL_HANDLES) +
			" obj = " + o);
	    }
	    return o;
	}

	IbisTypeInfo t = readType(handle & TYPE_MASK);

	String s = readUTF();
	Class c = getClassFromName(s);

	addObjectToCycleCheck(c);
	return c;
    }

    /**
     * Reads the header of an array.
     * This header consists of a handle, a type, and an integer representing
     * the length.
     * Note that the data read is mostly redundant.
     *
     * @exception IOException when an IO error occurs.
     * @exception ClassNotFoundException when the array class could
     *  not be loaded.
     */
    private void readArrayHeader(Class clazz, int len)
	throws IOException, ClassNotFoundException {

	if (DEBUG) {
	    dbPrint("readArrayHeader: class = " + clazz.getName() +
		    " len = " + len);
	}
	int type = readHandle();

	if ((type & TYPE_BIT) == 0) {
	    throw new StreamCorruptedException(
			"Array slice header but I receive a HANDLE!");
	}

	Class in_clazz = readType(type & TYPE_MASK).clazz;
	int in_len = readInt();

	if (ASSERTS && !clazz.isAssignableFrom(in_clazz)) {
	    throw new ClassCastException("Cannot assign class " + clazz +
					 " from read class " + in_clazz);
	}
	if (ASSERTS && in_len != len) {
	    throw new ArrayIndexOutOfBoundsException("Cannot read " + in_len +
						     " into " + len +
						     " elements");
	}
    }

    /**
     * Adds an object <code>o</code> to the object table, for cycle checking.
     * This method is public because it gets called from IOGenerator-generated
     * code.
     * @param o		the object to be added
     */
    public void addObjectToCycleCheck(Object o) {
	if (DEBUG) {
	    dbPrint("addObjectToCycleCheck: handle = " + next_handle);
	}
	objects.add(next_handle, o);
	next_handle++;
    }

    /**
     * Looks up an object in the object table.
     * This method is public because it gets called from IOGenerator-generated
     * code.
     * @param handle	the handle of the object to be looked up
     * @return		the corresponding object.
     */
    public Object getObjectFromCycleCheck(int handle) {
	Object o = objects.get(handle); // - CONTROL_HANDLES);

	if (DEBUG) {
	    dbPrint("getObjectFromCycleCheck: handle = " + handle);
	}

	return o;
    }

    /**
     * Method used by IOGenerator-generated code to read a handle, and
     * determine if it has to read a new object or get one from the object
     * table.
     *
     * @exception IOException		when an IO error occurs.
     * @return	0 for a null object, -1 for a new object, and the handle for an
     * 		object already in the object table.
     */
    public final int readKnownTypeHeader()
	    throws IOException, ClassNotFoundException
    {
	int handle_or_type = readHandle();

	if ((handle_or_type & TYPE_BIT) == 0) {
	    // Includes NUL_HANDLE.
	    if (DEBUG) {
		if (handle_or_type == NUL_HANDLE) {
		    dbPrint("readKnownTypeHeader -> read NUL_HANDLE");
		}
		else {
		    dbPrint("readKnownTypeHeader -> read OLD HANDLE " +
				       handle_or_type);
		}
	    }
	    return handle_or_type;
	}

	handle_or_type &= TYPE_MASK;
	if (handle_or_type >= next_type) {
	    readType(handle_or_type);
	}
	if (DEBUG) {
	    IbisTypeInfo t = (IbisTypeInfo) types.get(handle_or_type);
	    dbPrint("readKnownTypeHeader -> reading NEW object, class = " +
			       t.clazz.getName());
	}
	return -1;
    }

    /**
     * Reads an array from the stream.
     * The handle and type have already been read.
     *
     * @param clazz		the type of the array to be read
     * @param type		an index in the types table, but
     * 				also an indication of the base type of
     * 				the array
     *
     * @exception IOException			when an IO error occurs.
     * @exception ClassNotFoundException	when element type is Object and
     * 						readObject throws it.
     *
     * @return the array read.
     */
    private Object readArray(Class clazz, int type)
	throws IOException, ClassNotFoundException {

	if (DEBUG) {
	    if (clazz != null) {
		dbPrint("readArray " + clazz.getName() + " type " + type);
	    }
	}

	switch (type) {
	case TYPE_BOOLEAN:
	    return readArrayBoolean();
	case TYPE_BYTE:
	    return readArrayByte();
	case TYPE_SHORT:
	    return readArrayShort();
	case TYPE_CHAR:
	    return readArrayChar();
	case TYPE_INT:
	    return readArrayInt();
	case TYPE_LONG:
	    return readArrayLong();
	case TYPE_FLOAT:
	    return readArrayFloat();
	case TYPE_DOUBLE:
	    return readArrayDouble();
	default:
	    int len = readInt();
	    Object ref = java.lang.reflect.Array.newInstance(
				    clazz.getComponentType(),
				    len);
	    addObjectToCycleCheck(ref);

	    for (int i = 0; i < len; i++) {
		Object o = readObjectOverride();
		((Object[])ref)[i] = o;
	    }

	    return ref;
	}
    }

    /**
     * This method tries to load a class given its name. It tries the
     * default classloader, and the one from the thread context. Also,
     * apparently some classloaders do not understand array classes, and
     * from the Java documentation, it is not clear that they should.
     * Therefore, if the typeName indicates an array type, and the
     * obvious attempts to load the class fail, this method also tries
     * to load the base type of the array.
     *
     * @param typeName	the name of the type to be loaded
     * @exception ClassNotFoundException is thrown when the class could
     * not be loaded.
     * @return the loaded class
     */
    private Class getClassFromName(String typeName)
	    throws ClassNotFoundException
    {
	try {
	    return Class.forName(typeName);
	} catch (ClassNotFoundException e) {
	    try {
		if (DEBUG) {
		    dbPrint("Could not load class " + typeName +
			    " using Class.forName(), trying " +
			    "Thread.currentThread()." +
			    "getContextClassLoader().loadClass()");
		    dbPrint("Default class loader is " +
			    this.getClass().getClassLoader());
		    dbPrint("now trying " +
			    Thread.currentThread().getContextClassLoader());
		}
		return Thread.currentThread().getContextClassLoader().
				loadClass(typeName);
	    } catch (ClassNotFoundException e2) {
		int dim = 0;

		/* Some classloaders are not able to load array classes.
		 * Therefore, if the name
		 * describes an array, try again with the base type.
		 */
		if (typeName.length() > 0 && typeName.charAt(0) == '[') {
		    char[] s = typeName.toCharArray();
		    while (dim < s.length && s[dim] == '[') {
			dim++;
		    }
		    int begin = dim;
		    int end = s.length;
		    if (dim < s.length && s[dim] == 'L') {
			begin++;
		    }
		    if (s[end-1] == ';') {
			end--;
		    }
		    typeName = typeName.substring(begin, end);

		    int dims[] = new int[dim];
		    for (int i = 0; i < dim; i++) dims[i] = 0;

		    /* Now try to load the base class, create an array
		     * from it and then return its class.
		     */
		    return java.lang.reflect.Array.newInstance(
				getClassFromName(typeName), dims).getClass();
		} else {
                    return this.loadClassFromCustomCL(typeName);
		}
	    }
	}
    }
    
    private Class loadClassFromCustomCL(String className)
	    throws ClassNotFoundException {
        if (DEBUG) {
            System.out.println("loadClassTest " + className);
        }
        if (customClassLoader == null) {
            String clName = System.getProperty(
				"ibis.serialization.classloader");
            if (clName != null) {
                //we try to instanciate it
                try {
                    Class classDefinition = Class.forName(clName);
                    customClassLoader = (ClassLoader)
				classDefinition.newInstance();
                } catch (Exception e) {
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (customClassLoader == null) {
            throw new ClassNotFoundException(className);
        }
        if (DEBUG) {
            System.out.println("******* Calling custom classloader");
        } 
        return customClassLoader.loadClass(className);      
    }
  

    /**
     * Returns the <code>IbisTypeInfo</code> corresponding to the type
     * number given as parameter.
     * If the parameter indicates a type not yet read, its name is read
     * (as an UTF), and the class is loaded.
     *
     * @param type the type number
     * @exception ClassNotFoundException is thrown when the class could
     *  not be loaded.
     * @exception IOException is thrown when an IO error occurs
     * @return the <code>IbisTypeInfo</code> for <code>type</code>.
     */
    private IbisTypeInfo readType(int type)
	    throws IOException, ClassNotFoundException
    {
	if (type < next_type) {
	    if (DEBUG) {
		dbPrint("read type number 0x" + Integer.toHexString(type));
	    }
	    return (IbisTypeInfo) types.get(type);
	}

	if (next_type != type) {
	    throw new SerializationError("Internal error: next_type != type");
	}

	String typeName = readUTF();

	if (DEBUG) {
	    dbPrint("read NEW type number 0x" + Integer.toHexString(type) +
		    " type " + typeName);
	}

	Class clazz = getClassFromName(typeName);

	IbisTypeInfo t = IbisTypeInfo.getIbisTypeInfo(clazz);

	types.add(next_type, t);
	next_type++;

	return t;
    }

    /**
     * Native methods needed for assigning to final fields of objects that are
     * not rewritten.
     */
    private native void setFieldDouble(Object ref, String fieldname, double d);
    private native void setFieldLong(Object ref, String fieldname, long l);
    private native void setFieldFloat(Object ref, String fieldname, float f);
    private native void setFieldInt(Object ref, String fieldname, int i);
    private native void setFieldShort(Object ref, String fieldname, short s);
    private native void setFieldChar(Object ref, String fieldname, char c);
    private native void setFieldByte(Object ref, String fieldname, byte b);
    private native void setFieldBoolean(Object ref,
					String fieldname,
					boolean b);
    private native void setFieldObject(Object ref,
				       String fieldname,
				       String osig,
				       Object o);

    /**
     * This method reads a value from the stream and assigns it to a
     * final field.
     * IOGenerator uses this method when assigning final fields of an
     * object that is rewritten, 
     * but super is not, and super is serializable. The problem with
     * this situation is that
     * IOGenerator cannot create a proper constructor for this object,
     * so cannot assign
     * to native fields without falling back to native code.
     *
     * @param ref		object with a final field
     * @param fieldname		name of the field
     * @exception IOException	is thrown when an IO error occurs.
     */
    public void readFieldDouble(Object ref, String fieldname)
	    throws IOException
    {
	setFieldDouble(ref, fieldname, readDouble());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldLong(Object ref, String fieldname) throws IOException {
	setFieldLong(ref, fieldname, readLong());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldFloat(Object ref, String fieldname)
	    throws IOException
    {
	setFieldFloat(ref, fieldname, readFloat());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldInt(Object ref, String fieldname) throws IOException {
	setFieldInt(ref, fieldname, readInt());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldShort(Object ref, String fieldname)
	    throws IOException
    {
	setFieldShort(ref, fieldname, readShort());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldChar(Object ref, String fieldname) throws IOException {
	setFieldChar(ref, fieldname, readChar());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldByte(Object ref, String fieldname) throws IOException {
	setFieldByte(ref, fieldname, readByte());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldBoolean(Object ref, String fieldname)
	    throws IOException
    {
	setFieldBoolean(ref, fieldname, readBoolean());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     */
    public void readFieldString(Object ref, String fieldname) 
	    throws IOException
    {
	setFieldObject(ref, fieldname, "Ljava/lang/String;", readString());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @exception ClassNotFoundException when the class could not be loaded.
     */
    public void readFieldClass(Object ref, String fieldname)
	    throws IOException, ClassNotFoundException
    {
	setFieldObject(ref, fieldname, "Ljava/lang/Class;", readClass());
    }

    /**
     * See {@link #readFieldDouble(Object, String)} for a description.
     * @param fieldsig	signature of the field
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readFieldObject(Object ref, String fieldname, String fieldsig)
	    throws IOException, ClassNotFoundException
    {
	setFieldObject(ref, fieldname, fieldsig, readObjectOverride());
    }

    /**
     * Reads the serializable fields of an object <code>ref</code> using the
     * type information <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be read
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field is denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeDefaultReadObject(AlternativeTypeInfo t, Object ref)
	    throws ClassNotFoundException, IllegalAccessException, IOException
    {
	int temp = 0;
	if (DEBUG) {
	    dbPrint("alternativeDefaultReadObject, class = " +
		    t.clazz.getName());
	}
	for (int i=0;i<t.double_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldDouble(ref,
			       t.serializable_fields[temp].getName(),
			       readDouble());
	    }
	    else {
		t.serializable_fields[temp].setDouble(ref, readDouble());
	    }
	    temp++;
	}
	for (int i=0;i<t.long_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldLong(ref,
			     t.serializable_fields[temp].getName(),
			     readLong());
	    }
	    else {
		t.serializable_fields[temp].setLong(ref, readLong());
	    }
	    temp++;
	}
	for (int i=0;i<t.float_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldFloat(ref,
			      t.serializable_fields[temp].getName(),
			      readFloat());
	    }
	    else {
		t.serializable_fields[temp].setFloat(ref, readFloat());
	    }
	    temp++;
	}
	for (int i=0;i<t.int_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldInt(ref,
			    t.serializable_fields[temp].getName(),
			    readInt());
	    }
	    else {
		t.serializable_fields[temp].setInt(ref, readInt());
	    }
	    temp++;
	}
	for (int i=0;i<t.short_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldShort(ref,
			      t.serializable_fields[temp].getName(),
			      readShort());
	    }
	    else {
		t.serializable_fields[temp].setShort(ref, readShort());
	    }
	    temp++;
	}
	for (int i=0;i<t.char_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldChar(ref,
			     t.serializable_fields[temp].getName(),
			     readChar());
	    }
	    else {
		t.serializable_fields[temp].setChar(ref, readChar());
	    }
	    temp++;
	}
	for (int i=0;i<t.byte_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldByte(ref,
			     t.serializable_fields[temp].getName(),
			     readByte());
	    }
	    else {
		t.serializable_fields[temp].setByte(ref, readByte());
	    }
	    temp++;
	}
	for (int i=0;i<t.boolean_count;i++) {
	    if (t.fields_final[temp]) {
		setFieldBoolean(ref,
				t.serializable_fields[temp].getName(),
				readBoolean());
	    }
	    else {
		t.serializable_fields[temp].setBoolean(ref, readBoolean());
	    }
	    temp++;
	}
	for (int i=0;i<t.reference_count;i++) {
	    if (t.fields_final[temp]) {
		String fieldname = t.serializable_fields[temp].getName();
		String fieldtype = t.serializable_fields[temp].getType().
					getName();

		if (fieldtype.startsWith("[")) {
		} else {
		    fieldtype = "L" + fieldtype.replace('.', '/') + ";";
		}

		// dbPrint("fieldname = " + fieldname);
		// dbPrint("signature = " + fieldtype);

		setFieldObject(ref, fieldname, fieldtype, readObjectOverride());
	    }
	    else {
		Object o = readObjectOverride();
		if (DEBUG) {
		    if (o == null) {
			dbPrint("Assigning null to field " +
				    t.serializable_fields[temp].getName());
		    }
		    else {
			dbPrint("Assigning an object of type " +
				    o.getClass().getName() + " to field " +
				    t.serializable_fields[temp].getName());
		    }
		}
		t.serializable_fields[temp].set(ref, o);
	    }
	    temp++;
	}
    }

    /**
     * De-serializes an object <code>ref</code> using the type information
     * <code>t</code>.
     *
     * @param t		the type info for object <code>ref</code>
     * @param ref	the object of which the fields are to be read
     *
     * @exception IOException		 when an IO error occurs
     * @exception IllegalAccessException when access to a field or
     * 					 <code>readObject</code> method is
     * 					 denied.
     * @exception ClassNotFoundException when readObject throws it.
     */
    private void alternativeReadObject(AlternativeTypeInfo t, Object ref)
	throws ClassNotFoundException, IllegalAccessException, IOException {

	if (t.superSerializable) {
	    alternativeReadObject(t.alternativeSuperInfo, ref);
	}

	if (t.hasReadObject) {
	    current_level = t.level;
	    try {
		if (DEBUG) {
		    dbPrint("invoking readObject() of class " +
			    t.clazz.getName());
		}
		t.invokeReadObject(ref, this);
		if (DEBUG) {
		    dbPrint("done with readObject() of class " +
			    t.clazz.getName());
		}
	    } catch (java.lang.reflect.InvocationTargetException e) {
		if (DEBUG) {
		    dbPrint("Caught exception: " + e);
		    e.printStackTrace();
		    dbPrint("now rethrow as IllegalAccessException ...");
		}
		throw new IllegalAccessException("readObject method: " + e);
	    }
	    return;
	}
	alternativeDefaultReadObject(t, ref);
    }

    /**
     * This method takes care of reading the serializable fields of the
     * parent object, and also those of its parent objects.
     * Its gets called by IOGenerator-generated code when an object
     * has a superclass that is serializable but not Ibis serializable.
     *
     * @param ref	the object with a non-Ibis-serializable parent object
     * @param classname	the name of the superclass
     * @exception IOException	gets thrown on IO error
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void readSerializableObject(Object ref, String classname)
	    throws ClassNotFoundException, IOException
    {
	AlternativeTypeInfo t = AlternativeTypeInfo.
				    getAlternativeTypeInfo(classname);
	push_current_object(ref, 0);
	try {
	    alternativeReadObject(t, ref);
	} catch (IllegalAccessException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as NotSerializableException ...");
	    }
	    throw new NotSerializableException(classname + " " + e);
	}
	pop_current_object();
    }

    /**
     * This method reads the serializable fields of object <code>ref</code>
     * at the level indicated by <code>depth</code> (see the explanation at
     * the declaration of the <code>current_level</code> field.
     * It gets called from IOGenerator-generated code, when a parent object
     * is serializable but not Ibis serializable.
     *
     * @param ref	the object of which serializable fields must be written
     * @param depth	an indication of the current "view" of the object
     * @exception IOException	gets thrown when an IO error occurs.
     * @exception ClassNotFoundException when readObject throws it.
     */
    public void defaultReadSerializableObject(Object ref, int depth)
	throws ClassNotFoundException, IOException {
	Class type = ref.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.
				    getAlternativeTypeInfo(type);

	/*  Find the type info corresponding to the current invocation.
	    See the invokeReadObject invocation in alternativeReadObject.
	*/
	while (t.level > depth) {
	    t = t.alternativeSuperInfo;
	}
	try {
	    alternativeDefaultReadObject(t, ref);
	} catch (IllegalAccessException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as NotSerializableException ...");
	    }
	    throw new NotSerializableException(type + " " + e);
	}
    }

    /**
     * Native method for creating an uninitialized object.
     * We need such a method to call the right constructor for it,
     * which is the parameter-less constructor of the "highest" superclass
     * that is not serializable.
     * @param type the type of the object to be created
     * @param non_serializable_super the "highest" superclass of
     * <code>type</code> that is not serializable
     * @return the object created
     */
    private native Object createUninitializedObject(
				Class type,
				Class non_serializable_super);

    /**
     * Creates an uninitialized object of the type indicated by
     * <code>classname</code>.
     * The corresponding constructor called is the parameter-less
     * constructor of the "highest" superclass that is not serializable.
     *
     * @param classname		name of the class
     * @exception IOException	gets thrown when an IO error occurs.
     * @exception ClassNotFoundException when class <code>classname</code>
     *  cannot be loaded.
     */
    public Object create_uninitialized_object(String classname)
	throws ClassNotFoundException, IOException {
	Class clazz = getClassFromName(classname);

	Class t2 = clazz;
	while (Serializable.class.isAssignableFrom(t2)) {
	    /* Find first non-serializable super-class. */
	    t2 = t2.getSuperclass();
	}
	// Calls constructor for non-serializable superclass.
	Object obj = createUninitializedObject(clazz, t2);

	addObjectToCycleCheck(obj);

	return obj;
    }

    /**
     * Push the notions of <code>current_object</code> and
     * <code>current_level</code> on their stacks, and set new ones.
     * @param ref	the new <code>current_object</code> notion
     * @param level	the new <code>current_level</code> notion
     */
    public void push_current_object(Object ref, int level) {
	if (stack_size >= max_stack_size) {
	    max_stack_size = 2 * max_stack_size + 10;
	    Object[] new_o_stack = new Object[max_stack_size];
	    int[] new_l_stack = new int[max_stack_size];
	    for (int i = 0; i < stack_size; i++) {
		new_o_stack[i] = object_stack[i];
		new_l_stack[i] = level_stack[i];
	    }
	    object_stack = new_o_stack;
	    level_stack = new_l_stack;
	}
	object_stack[stack_size] = current_object;
	level_stack[stack_size] = current_level;
	stack_size++;
	current_object = ref;
	current_level = level;
    }

    /**
     * Pop the notions of <code>current_object</code> and
     * <code>current_level</code> from their stacks.
     */
    public void pop_current_object() {
	stack_size--;
	current_object = object_stack[stack_size];
	current_level = level_stack[stack_size];
    }

    /**
     * Reads and returns a <code>String</code> object. This is a special case,
     * because strings are written as an UTF.
     *
     * @exception IOException   gets thrown on IO error
     * @return the string read.
     */
    public String readString() throws IOException {
	int handle = readHandle();

	if (handle == NUL_HANDLE) {
	    if (DEBUG) {
		dbPrint("readString: --> null");
	    }
	    return null;
	}

	if ((handle & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    String o = (String) objects.get(handle);

	    if (DEBUG) {
		dbPrint("readString: duplicate handle = " + handle +
			    " string = " + o);
	    }
	    return o;
	}

	IbisTypeInfo t;
	try {
	    t = readType(handle & TYPE_MASK);
	} catch (ClassNotFoundException e) {
	    if (DEBUG) {
		dbPrint("Caught exception: " + e);
		e.printStackTrace();
		dbPrint("now rethrow as SerializationError ...");
	    }
	    throw new SerializationError("Cannot find java.lang.String?", e);
	}

	String s = readUTF();
	if (DEBUG) {
	    dbPrint("readString returns " + s);
	}
	addObjectToCycleCheck(s);
	return s;
    }

    /**
     * We cannot redefine <code>readObject, because it is final
     * in <code>ObjectInputStream</code>. The trick for Ibis serialization
     * is to have the <code>ObjectInputStream</code> be initialized with
     * its parameter-less constructor.  This will cause its
     * <code>readObject</code> method to call <code>readObjectOverride</code>
     * instead of doing its own thing.
     *
     * @return the object read
     * @exception IOException is thrown on an IO error.
     * @exception ClassNotFoundException is thrown when the class of a
     * serialized object is not found.
     */
    public final Object readObjectOverride()
	    throws IOException, ClassNotFoundException
    {
	/*
	 * ref < 0:    type
	 * ref = 0:    null ptr
	 * ref > 0:    handle
	 */

	int handle_or_type = readHandle();

	if (handle_or_type == NUL_HANDLE) {
	    return null;
	}

	if ((handle_or_type & TYPE_BIT) == 0) {
	    /* Ah, it's a handle. Look it up, return the stored ptr */
	    Object o = objects.get(handle_or_type);

	    if (DEBUG) {
		dbPrint("readObject: duplicate handle " + handle_or_type +
			" class = " + o.getClass());
	    }
	    return o;
	}

	int type = handle_or_type & TYPE_MASK;
	IbisTypeInfo t = readType(type);

	if (DEBUG) {
	    dbPrint("start readObject of class " + t.clazz.getName() +
			" handle = " + next_handle);
	}

	Object obj;

	if (t.isArray) {
	    obj = readArray(t.clazz, type);
	} else if (t.isString) {
	    obj = readUTF();
	    addObjectToCycleCheck(obj);
	} else if (t.isClass) {
	    String name = readUTF();
	    obj = getClassFromName(name);
	    addObjectToCycleCheck(obj);
	} else if (t.gen != null) {
	    obj = t.gen.generated_newInstance(this);
	} else if (Externalizable.class.isAssignableFrom(t.clazz)) {
	    try {
		// Also calls parameter-less constructor
		obj = t.clazz.newInstance();
	    } catch(Exception e) {
		if (DEBUG) {
		    dbPrint("Caught exception: " + e);
		    e.printStackTrace();
		    dbPrint("now rethrow as ClassNotFound ...");
		}
		throw new ClassNotFoundException("Could not instantiate" + e);
	    }
	    addObjectToCycleCheck(obj);
	    push_current_object(obj, 0);
	    ((java.io.Externalizable) obj).readExternal(this);
	    pop_current_object();
	} else {
	    // obj = t.clazz.newInstance(); Not correct:
	    // calls wrong constructor.
	    Class t2 = t.clazz;
	    while (Serializable.class.isAssignableFrom(t2)) {
		// Find first non-serializable super-class.
		t2 = t2.getSuperclass();
	    }
	    // Calls constructor for non-serializable superclass.
	    obj = createUninitializedObject(t.clazz, t2);
	    addObjectToCycleCheck(obj);
	    push_current_object(obj, 0);
	    try {
		alternativeReadObject(t.altInfo, obj);
	    } catch (IllegalAccessException e) {
		if (DEBUG) {
		    dbPrint("Caught exception: " + e);
		    e.printStackTrace();
		    dbPrint("now rethrow as NotSerializableException ...");
		}
		throw new NotSerializableException(type + " " + e);
	    }
	    pop_current_object();
	}

	if (DEBUG) {
	    dbPrint("finished readObject of class " + t.clazz.getName());
	}

	return obj;
    }

    /**
     * Ignored for Ibis serialization.
     */
    protected void readStreamHeader() {
    }

    /**
     * {@inheritDoc}
     */
    public GetField readFields() throws IOException, ClassNotFoundException {
	if (current_object == null) {
	    throw new NotActiveException("not in readObject");
	}
	Class type = current_object.getClass();
	AlternativeTypeInfo t = AlternativeTypeInfo.
				    getAlternativeTypeInfo(type);
	ImplGetField current_getfield = new ImplGetField(t);
	current_getfield.readFields();
	return current_getfield;
    }

    /**
     * The Ibis serialization implementation of <code>GetField</code>.
     */
    private class ImplGetField extends GetField {
	private double[]  doubles;
	private long[]	  longs;
	private int[]	  ints;
	private float[]   floats;
	private short[]   shorts;
	private char[]    chars;
	private byte[]	  bytes;
	private boolean[] booleans;
	private Object[]  references;
	private AlternativeTypeInfo t;

	ImplGetField(AlternativeTypeInfo t) {
	    doubles = new double[t.double_count];
	    longs = new long[t.long_count];
	    ints = new int[t.int_count];
	    shorts = new short[t.short_count];
	    floats = new float[t.float_count];
	    chars = new char[t.char_count];
	    bytes = new byte[t.byte_count];
	    booleans = new boolean[t.boolean_count];
	    references = new Object[t.reference_count];
	    this.t = t;
	}

	public ObjectStreamClass getObjectStreamClass() {
	    /*  I don't know how it could be used here, but ... */
	    return ObjectStreamClass.lookup(t.clazz);
	}

	public boolean defaulted(String name) {
	    return false;
	}

	public boolean get(String name, boolean dflt) {
	    return booleans[t.getOffset(name, Boolean.TYPE)];
	}

	public char get(String name, char dflt) {
	    return chars[t.getOffset(name, Character.TYPE)];
	}

	public byte get(String name, byte dflt) {
	    return bytes[t.getOffset(name, Byte.TYPE)];
	}

	public short get(String name, short dflt) {
	    return shorts[t.getOffset(name, Short.TYPE)];
	}

	public int get(String name, int dflt) {
	    return ints[t.getOffset(name, Integer.TYPE)];
	}

	public long get(String name, long dflt) {
	    return longs[t.getOffset(name, Long.TYPE)];
	}

	public float get(String name, float dflt) {
	    return floats[t.getOffset(name, Float.TYPE)];
	}

	public double get(String name, double dflt) {
	    return doubles[t.getOffset(name, Double.TYPE)];
	}

	public Object get(String name, Object dflt) {
	    return references[t.getOffset(name, Object.class)];
	}

	void readFields() throws IOException, ClassNotFoundException {
	    for (int i = 0; i < t.double_count; i++) {
		doubles[i] = readDouble();
	    }
	    for (int i = 0; i < t.float_count; i++) {
		floats[i] = readFloat();
	    }
	    for (int i = 0; i < t.long_count; i++) {
		longs[i] = readLong();
	    }
	    for (int i = 0; i < t.int_count; i++) {
		ints[i] = readInt();
	    }
	    for (int i = 0; i < t.short_count; i++) {
		shorts[i] = readShort();
	    }
	    for (int i = 0; i < t.char_count; i++) {
		chars[i] = readChar();
	    }
	    for (int i = 0; i < t.byte_count; i++) {
		bytes[i] = readByte();
	    }
	    for (int i = 0; i < t.boolean_count; i++) {
		booleans[i] = readBoolean();
	    }
	    for (int i = 0; i < t.reference_count; i++) {
		references[i] = readObjectOverride();
	    }
	}
    }

    /**
     * Determines whether a class is Ibis-serializable.
     * We cannot use "instanceof ibis.io.Serializable", because that would
     * also return true if a parent class implements ibis.io.Serializable,
     * which is not good enough.
     *
     * @param clazz	the class to be tested
     * @return whether the class is ibis-serializable.
     */
    protected static boolean isIbisSerializable(Class clazz) {
	Class[] intfs = clazz.getInterfaces();

	for (int i = 0; i < intfs.length; i++) {
	    if (intfs[i].equals(ibis.io.Serializable.class)) return true;
	}
	return false;
    }

    /**
     * {@inheritDoc}
     */
    public void defaultReadObject()
	throws ClassNotFoundException, IOException, NotActiveException {
	if (current_object == null) {
	    throw new NotActiveException("defaultReadObject without a current object");
	}
	Object ref = current_object;
	Class type = ref.getClass();

	if (isIbisSerializable(type)) {
	    if (DEBUG) {
		dbPrint("generated_DefaultReadObject, class = " + type +
			    ", level = " + current_level);
	    }
	    ((ibis.io.Serializable)ref).
		generated_DefaultReadObject(this, current_level);
	} else if (ref instanceof java.io.Serializable) {
	    AlternativeTypeInfo t = AlternativeTypeInfo.
					getAlternativeTypeInfo(type);

	    /*  Find the type info corresponding to the current invocation.
	     *  See the invokeReadObject invocation in alternativeReadObject.
	     */
	    while (t.level > current_level) {
		t = t.alternativeSuperInfo;
	    }
	    try {
		alternativeDefaultReadObject(t, ref);
	    } catch (IllegalAccessException e) {
		if (DEBUG) {
		    dbPrint("Caught exception: " + e);
		    e.printStackTrace();
		    dbPrint("now rethrow as NotSerializableException ...");
		}
		throw new NotSerializableException(type + " " + e);
	    }
	} else {
	    throw new NotSerializableException("Not Serializable : " +
						type.toString());
	}
    }

    /*  Need conversion for allocation of uninitialized objects.
	This lib is now loaded from Ibis.java, to avoid dependancies. --Rob
    */
}
