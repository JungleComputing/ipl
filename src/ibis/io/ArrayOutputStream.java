package ibis.io;

import java.io.IOException;

/**
 * This is an implementation of the <code>IbisAccumulator</code> interface
 * which is actually the (old) <code>ArrayOutputStream</code>.
 * This way the whole thing becomes backwards
 * compatible with older implementations.
 */
public abstract class ArrayOutputStream implements IbisAccumulator, IbisStreamFlags {

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
    private ArrayDescriptor[] 	array = 
	new ArrayDescriptor[ARRAY_BUFFER_SIZE];

    /**
     * Index in the <code>array</code> array.
     */
    private int			array_index;

    /**
     * Collects all indices of the <code>..._buffer</code> arrays.
     */
    protected short[]	indices_short  = new short[PRIMITIVE_TYPES + 1];

    /**
     * Constructor.
     */
    public ArrayOutputStream() {

	for(int i = 0; i < ARRAY_BUFFER_SIZE; i++) {
	    array[i] = new ArrayDescriptor();
	}

	reset_indices();

	array_index = 0;
    }

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
    }

    /**
     * Method to put a array in the "array cache". If the cache is full
     * it is written to the arrayOutputStream
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     * @param type	type of the array elements
     *
     * @exception IOException on IO error.
     */
    private void writeArray(Object ref, int offset, int len, int type)
	    throws IOException {
	if (array_index == ARRAY_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing array " + ref + " offset: " 
		    + offset + " len: " + len + " type " + type );
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
    private void doFlush() throws IOException {
	if (array_index == 0 &&
	    byte_index == 0 &&
	    char_index == 0 &&
	    short_index == 0 &&
	    int_index == 0 &&
	    long_index == 0 &&
	    float_index == 0 &&
	    double_index == 0) {

	    return;
	}

	flushBuffers();

	/* Retain the order in which the arrays were pushed. This 
	 * costs a cast at send/receive.
	 */
	for (int i = 0; i < array_index; i++) {
	    doWriteArray(array[i].array, array[i].offset, array[i].len, array[i].type);
	}

	array_index = 0;
    }

    /**
     * Flush the primitive arrays.
     *
     * @exception IOException is thrown when any <code>writeArray</code>
     * throws it.
     */
    public void flushBuffers() throws IOException {
	indices_short[TYPE_BYTE]    = (short) byte_index;
	indices_short[TYPE_CHAR]    = (short) char_index;
	indices_short[TYPE_SHORT]   = (short) short_index;
	indices_short[TYPE_INT]     = (short) int_index;
	indices_short[TYPE_LONG]    = (short) long_index;
	indices_short[TYPE_FLOAT]   = (short) float_index;
	indices_short[TYPE_DOUBLE]  = (short) double_index;

	if (DEBUG) {
	    System.out.println("writing bytes " + byte_index);
	    System.out.println("writing chars " + char_index);
	    System.out.println("writing shorts " + short_index);
	    System.out.println("writing ints " + int_index);
	    System.out.println("writing longs " + long_index);
	    System.out.println("writing floats " + float_index);
	    System.out.println("writing doubles " + double_index);
	}

	doWriteArray(indices_short, 0, PRIMITIVE_TYPES, TYPE_SHORT);

	if (byte_index > 0)    doWriteArray(byte_buffer, 0, byte_index, TYPE_BYTE);
	if (char_index > 0)    doWriteArray(char_buffer, 0, char_index, TYPE_CHAR);
	if (short_index > 0)   doWriteArray(short_buffer, 0, short_index, TYPE_SHORT);
	if (int_index > 0)     doWriteArray(int_buffer, 0, int_index, TYPE_INT);
	if (long_index > 0)    doWriteArray(long_buffer, 0, long_index, TYPE_LONG);
	if (float_index > 0)   doWriteArray(float_buffer, 0, float_index, TYPE_FLOAT);
	if (double_index > 0)  doWriteArray(double_buffer, 0, double_index, TYPE_DOUBLE);

	reset_indices();
    }
    /**
     * Flushes the array to the underlying layer.
     * @param ref	the array to be written
     * @param offset	the offset at which to start
     * @param len	number of elements to write
     * @param type	type of the array elements
     *
     * @exception IOException on IO error.
     */
    public abstract void doWriteArray(Object ref, int offset, int len, int type)
	    throws IOException;

    /**
     * Tells the underlying implementation to flush all the data.
     * An <code>ArrayOutputStream</code> implementation will probably
     * redefine this method. If so, it must not forget to call
     * "super.flush()" first.
     *
     * @exception IOException on IO error.
     */
    public void flush() throws IOException {
	doFlush();
    }

    /**
     * Blocks until the data is written.
     * @exception IOException on IO error.
     */
    public abstract void finish() throws IOException;

    /**
     * Tells the underlying implementation that this stream is closed.
     * @exception IOException on IO error.
     */
    public abstract void close() throws IOException;

    /**
     * Returns the number of bytes that was written to the message, 
     * in the stream dependant format.
     * This is the number of bytes that will be sent over the network 
     * @return the number of bytes written
     */
    public abstract int bytesWritten();

    /** 
     * Resets the counter for the number of bytes written
     */
    public abstract void resetBytesWritten();

    /**
     * Writes a boolean value to the accumulator.
     * @param     value             The boolean value to write.
     * @exception IOException on IO error.
     */
    public void writeBoolean(boolean value) throws IOException {
	if (byte_index == BYTE_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing boolean " + value);
	}
	byte_buffer[byte_index++] = (byte) (value ? 1 : 0);
    }


    /**
     * Writes a byte value to the accumulator.
     * @param     value             The byte value to write.
     * @exception IOException on IO error.
     */
    public void writeByte(byte value) throws IOException {
	if (byte_index == BYTE_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing byte " + value);
	}
	byte_buffer[byte_index++] = value;
    }

    /**
     * Writes a char value to the accumulator.
     * @param     value             The char value to write.
     * @exception IOException on IO error.
     */
    public void writeChar(char value) throws IOException {
	if (char_index == CHAR_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing char " + value);
	}
	char_buffer[char_index++] = value;
    }

    /**
     * Writes a short value to the accumulator.
     * @param     value             The short value to write.
     * @exception IOException on IO error.
     */
    public void writeShort(short value) throws IOException {
	if (short_index == SHORT_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing short " + value);
	}
	short_buffer[short_index++] = value;
    }

    /**
     * Writes a int value to the accumulator.
     * @param     value             The int value to write.
     * @exception IOException on IO error.
     */
    public void writeInt(int value) throws IOException {
	if (int_index == INT_BUFFER_SIZE) {
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing int[HEX] " + value + "[" +
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
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing long " + value);
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
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing float " + value);
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
	    doFlush();
	}
	if (DEBUG) {
	    System.out.println(" Writing double " + value);
	}
	double_buffer[double_index++] = value;
    }

    /**
     * Writes (a slice of) an array of Booleans into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(boolean [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_BOOLEAN); 
    }

    /**
     * Writes (a slice of) an array of Bytes into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(byte [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_BYTE); 
    }

    /**
     * Writes (a slice of) an array of Characters into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(char [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_CHAR); 
    }

    /**
     * Writes (a slice of) an array of Short Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(short [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_SHORT);
    }

    /**
     * Writes (a slice of) an array of Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(int [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_INT); 
    }

    /**
     * Writes (a slice of) an array of Long Integers into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(long [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_LONG); 
    }

    /**
     * Writes (a slice of) an array of Floats into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(float [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_FLOAT); 
    }

    /**
     * Writes (a slice of) an array of Doubles into the accumulator.
     * @param	source		The array to write to the accumulator.
     * @param	offset		The offset at which to start.
     * @param	size		The number of elements to be copied.
     * @exception IOException on IO error.
     */
    public void writeArray(double [] source, int offset, int size)
	    throws IOException {
	writeArray(source, offset, size, TYPE_DOUBLE); 
    }
}
