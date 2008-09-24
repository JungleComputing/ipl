/* $Id$ */

package ibis.io.jme;

import java.io.IOException;
import java.io.UTFDataFormatException;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class DataSerializationInputStream extends ByteSerializationInputStream {
    /** When true, no buffering in this layer. */
    private static final boolean NO_ARRAY_BUFFERS
            = properties.getBooleanProperty(s_no_array_buffers);

    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_DATA_SERIALIZATION = true;

    /** Boolean count is not used, use it for arrays. */
    static final int TYPE_ARRAY = TYPE_BOOLEAN;

    /**
     * Each "bunch" of data is preceded by a header array, telling for
     * each type, how many of those must be read. This header array is
     * read into <code>indices_short</code>. The first entry of this
     * array is used as an array count.
     */
    private short[] indices_short;

    /** Storage for bytes (or booleans) read. */
    private byte[] byte_buffer;

    /** Storage for chars read. */
    private char[] char_buffer;

    /** Storage for shorts read. */
    private short[] short_buffer;

    /** Storage for ints read. */
    private int[] int_buffer;

    /** Storage for longs read. */
    private long[] long_buffer;

    /** Storage for floats read. */
    private float[] float_buffer;

    /** Storage for doubles read. */
    private double[] double_buffer;

    /** Current index in <code>byte_buffer</code>. */
    private int byte_index;

    /** Current index in <code>char_buffer</code>. */
    private int char_index;

    /** Current index in <code>short_buffer</code>. */
    private int short_index;

    /** Current index in <code>int_buffer</code>. */
    private int int_index;

    /** Current index in <code>long_buffer</code>. */
    private int long_index;

    /** Current index in <code>float_buffer</code>. */
    private int float_index;

    /** Current index in <code>double_buffer</code>. */
    private int double_index;

    /** Number of arrays read in current bunch. */
    private int array_index;

    /** Number of bytes in <code>byte_buffer</code>. */
    private int max_byte_index;

    /** Number of chars in <code>char_buffer</code>. */
    private int max_char_index;

    /** Number of shorts in <code>short_buffer</code>. */
    private int max_short_index;

    /** Number of ints in <code>int_buffer</code>. */
    private int max_int_index;

    /** Number of longs in <code>long_buffer</code>. */
    private int max_long_index;

    /** Number of floats in <code>float_buffer</code>. */
    private int max_float_index;

    /** Number of doubles in <code>double_buffer</code>. */
    private int max_double_index;

    /** Number of arrays in current bunch. */
    private int max_array_index;
    
    private final int BYTE_BUFFER_SIZE;
    
    private final int CHAR_BUFFER_SIZE;
    
    private final int SHORT_BUFFER_SIZE;
    
    private final int INT_BUFFER_SIZE;
    
    private final int LONG_BUFFER_SIZE;
    
    private final int FLOAT_BUFFER_SIZE;
    
    private final int DOUBLE_BUFFER_SIZE;
    
    static int typedBufferSize(int bufferSize, int elSize) {
        return (bufferSize -(PRIMITIVE_TYPES - BEGIN_TYPES) * SIZEOF_SHORT) / elSize;    
    }
    
    /**
     * Constructor with an <code>DataInputStream</code>.
     * @param in		the underlying <code>DataInputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationInputStream(DataInputStream in) throws IOException {
        super(in);
        int bufferSize = in.bufferSize();
        if (bufferSize <= 0) {
            bufferSize = BUFFER_SIZE;
        }
        BYTE_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_BYTE);
        CHAR_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_CHAR);
        SHORT_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_SHORT);
        INT_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_INT);
        LONG_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_LONG);
        FLOAT_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_FLOAT);
        DOUBLE_BUFFER_SIZE = typedBufferSize(bufferSize, SIZEOF_DOUBLE);

        if (! NO_ARRAY_BUFFERS) {
            initArrays();
        }
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected DataSerializationInputStream() throws IOException {
        super();
        BYTE_BUFFER_SIZE = 0;
        CHAR_BUFFER_SIZE = 0;
        SHORT_BUFFER_SIZE = 0;
        INT_BUFFER_SIZE = 0;
        LONG_BUFFER_SIZE = 0;
        FLOAT_BUFFER_SIZE = 0;
        DOUBLE_BUFFER_SIZE = 0;
    }

    public String serializationImplName() {
        return "data";
    }

    public boolean readBoolean() throws IOException {
        boolean a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readBoolean();
        } else {
            while (byte_index == max_byte_index) {
                receive();
            }
            a = (byte_buffer[byte_index++] != (byte) 0);
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read boolean: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public byte readByte() throws IOException {
        byte a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readByte();
        } else {
            while (byte_index == max_byte_index) {
                receive();
            }
            a = byte_buffer[byte_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read byte: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public char readChar() throws IOException {
        char a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readChar();
        } else {
            while (char_index == max_char_index) {
                receive();
            }
            a = char_buffer[char_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read char: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public short readShort() throws IOException {
        short a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readShort();
        } else {
            while (short_index == max_short_index) {
                receive();
            }
            a = short_buffer[short_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read short: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public int readInt() throws IOException {
        int a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readInt();
        } else {
            while (int_index == max_int_index) {
                receive();
            }
            a = int_buffer[int_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read int[HEX]: " + a + "[0x" + Integer.toHexString(a)
                    + "]");
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public long readLong() throws IOException {
        long a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readLong();
        } else {
            while (long_index == max_long_index) {
                receive();
            }
            a = long_buffer[long_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read long: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public float readFloat() throws IOException {
        float a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readFloat();
        } else {
            while (float_index == max_float_index) {
                receive();
            }
            a = float_buffer[float_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read float: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    public double readDouble() throws IOException {
        double a;

        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        if (NO_ARRAY_BUFFERS) {
            a = in.readDouble();
        } else {
            while (double_index == max_double_index) {
                receive();
            }
            a = double_buffer[double_index++];
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read double: " + a);
        }
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
        return a;
    }

    /**
     * Reads (part of) an array of booleans.
     * This method is here to make extending this class easier.
     */
    protected void readBooleanArray(boolean ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_BOOLEAN) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayBoolean: offset: " + off
                        + " len: " + len + " type: " + TYPE_BOOLEAN);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readBoolean();
            }
        }
    }

    /**
     * Reads (part of) an array of bytes.
     * This method is here to make extending this class easier.
     */
    protected void readByteArray(byte ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_BYTE) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayByte: offset: " + off
                        + " len: " + len + " type: " + TYPE_BYTE);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readByte();
            }
        }
    }

    /**
     * Reads (part of) an array of chars.
     * This method is here to make extending this class easier.
     */
    protected void readCharArray(char ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_CHAR) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayChar: offset: " + off
                        + " len: " + len + " type: " + TYPE_CHAR);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readChar();
            }
        }
    }

    /**
     * Reads (part of) an array of shorts.
     * This method is here to make extending this class easier.
     */
    protected void readShortArray(short ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_SHORT) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayShort: offset: " + off
                        + " len: " + len + " type: " + TYPE_SHORT);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readShort();
            }
        }
    }

    /**
     * Reads (part of) an array of ints.
     * This method is here to make extending this class easier.
     */
    protected void readIntArray(int ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_INT) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayInt: offset: " + off
                        + " len: " + len + " type: " + TYPE_INT);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readInt();
            }
        }
    }

    /**
     * Reads (part of) an array of longs.
     * This method is here to make extending this class easier.
     */
    protected void readLongArray(long ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_LONG) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayLong: offset: " + off
                        + " len: " + len + " type: " + TYPE_LONG);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readLong();
            }
        }
    }

    /**
     * Reads (part of) an array of floats.
     * This method is here to make extending this class easier.
     */
    protected void readFloatArray(float ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_FLOAT) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayFloat: offset: " + off
                        + " len: " + len + " type: " + TYPE_FLOAT);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readFloat();
            }
        }
    }

    /**
     * Reads (part of) an array of doubles.
     * This method is here to make extending this class easier.
     */
    protected void readDoubleArray(double ref[], int off, int len)
            throws IOException {
        if (NO_ARRAY_BUFFERS) {
            in.readArray(ref, off, len);
        } else if (len >= SMALL_ARRAY_BOUND / SIZEOF_DOUBLE) {
            while (array_index == max_array_index) {
                receive();
            }
            array_index++;
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("readArrayDouble: offset: " + off
                        + " len: " + len + " type: " + TYPE_DOUBLE);
            }
            in.readArray(ref, off, len);
        } else {
            for (int i = off; i < off + len; i++) {
                ref[i] = readDouble();
            }
        }
    }

    public void readArray(boolean[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readBooleanArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(byte[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readByteArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(char[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readCharArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(short[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readShortArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(int[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readIntArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(long[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readLongArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(float[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readFloatArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    public void readArray(double[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        readDoubleArray(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }
    }

    /**
     * Allocates arrays.
     */
    private void initArrays() {
        indices_short = new short[PRIMITIVE_TYPES];
        byte_buffer = new byte[BYTE_BUFFER_SIZE];
        char_buffer = new char[CHAR_BUFFER_SIZE];
        short_buffer = new short[SHORT_BUFFER_SIZE];
        int_buffer = new int[INT_BUFFER_SIZE];
        long_buffer = new long[LONG_BUFFER_SIZE];
        float_buffer = new float[FLOAT_BUFFER_SIZE];
        double_buffer = new double[DOUBLE_BUFFER_SIZE];
    }

    /**
     * Receive a new bunch of data.
     *
     * @exception IOException gets thrown when any of the reads throws it.
     */
    private void receive() throws IOException {
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("doing a receive()");
        }
        if (ASSERTS) {
            int sum = (max_byte_index - byte_index)
                    + (max_char_index - char_index)
                    + (max_short_index - short_index)
                    + (max_int_index - int_index)
                    + (max_long_index - long_index)
                    + (max_float_index - float_index)
                    + (max_double_index - double_index)
                    + (max_array_index - array_index);
            if (sum != 0) {
                logger.debug("EEEEK : receiving while there is data in buffer !!!");
                logger.debug("byte_index " + (max_byte_index - byte_index));
                logger.debug("char_index " + (max_char_index - char_index));
                logger.debug("short_index " + (max_short_index - short_index));
                logger.debug("int_index " + (max_int_index - int_index));
                logger.debug("long_index " + (max_long_index - long_index));
                logger.debug("float_index " + (max_float_index - float_index));
                logger.debug("double_index " + (max_double_index - double_index));
                logger.debug("array_index " + (max_array_index - array_index));

                throw new SerializationError("Internal error!");
            }
        }

        if (TIME_DATA_SERIALIZATION) {
            suspendTimer();
        }

        in.readArray(indices_short, BEGIN_TYPES, PRIMITIVE_TYPES - BEGIN_TYPES);

        array_index = 0;
        byte_index = 0;
        char_index = 0;
        short_index = 0;
        int_index = 0;
        long_index = 0;
        float_index = 0;
        double_index = 0;

        max_array_index = indices_short[TYPE_ARRAY];
        max_byte_index = indices_short[TYPE_BYTE];
        max_char_index = indices_short[TYPE_CHAR];
        max_short_index = indices_short[TYPE_SHORT];
        max_int_index = indices_short[TYPE_INT];
        max_long_index = indices_short[TYPE_LONG];
        max_float_index = indices_short[TYPE_FLOAT];
        max_double_index = indices_short[TYPE_DOUBLE];

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("reading arrays " + max_array_index);
            logger.debug("reading bytes " + max_byte_index);
            logger.debug("reading char " + max_char_index);
            logger.debug("reading short " + max_short_index);
            logger.debug("reading int " + max_int_index);
            logger.debug("reading long " + max_long_index);
            logger.debug("reading float " + max_float_index);
            logger.debug("reading double " + max_double_index);
        }

        if (max_byte_index > 0) {
            if (max_byte_index > byte_buffer.length) {
                byte_buffer = new byte[max_byte_index];
            }
            in.readArray(byte_buffer, 0, max_byte_index);
        }
        if (max_char_index > 0) {
            if (max_char_index > char_buffer.length) {
                char_buffer = new char[max_char_index];
            }
            in.readArray(char_buffer, 0, max_char_index);
        }
        if (max_short_index > 0) {
            if (max_short_index > short_buffer.length) {
                short_buffer = new short[max_short_index];
            }
            in.readArray(short_buffer, 0, max_short_index);
        }
        if (max_int_index > 0) {
            if (max_int_index > int_buffer.length) {
                int_buffer = new int[max_int_index];
            }
            in.readArray(int_buffer, 0, max_int_index);
        }
        if (max_long_index > 0) {
            if (max_long_index > long_buffer.length) {
                long_buffer = new long[max_long_index];
            }
            in.readArray(long_buffer, 0, max_long_index);
        }
        if (max_float_index > 0) {
            if (max_float_index > float_buffer.length) {
                float_buffer = new float[max_float_index];
            }
            in.readArray(float_buffer, 0, max_float_index);
        }
        if (max_double_index > 0) {
            if (max_double_index > double_buffer.length) {
                double_buffer = new double[max_double_index];
            }
            in.readArray(double_buffer, 0, max_double_index);
        }

        if (TIME_DATA_SERIALIZATION) {
            resumeTimer();
        }
    }

    public int available() throws IOException {
        return super.available() + (max_byte_index - byte_index) * SIZEOF_BYTE
                + (max_char_index - char_index) * SIZEOF_CHAR
                + (max_short_index - short_index) * SIZEOF_SHORT
                + (max_int_index - int_index) * SIZEOF_INT
                + (max_long_index - long_index) * SIZEOF_LONG
                + (max_float_index - float_index) * SIZEOF_FLOAT
                + (max_double_index - double_index) * SIZEOF_DOUBLE;
    }

    public String readUTF() throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            startTimer();
        }
        int bn = readInt();

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("readUTF: len = " + bn);
        }

        if (bn == -1) {
            if (TIME_DATA_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        byte[] b = new byte[bn];
        readByteArray(b, 0, bn);

        int len = 0;
        char[] c = new char[bn];

        for (int i = 0; i < bn; i++) {
            if ((b[i] & ~0x7f) == 0) {
                c[len++] = (char) (b[i] & 0x7f);
            } else if ((b[i] & ~0x1f) == 0xc0) {
                if (i + 1 >= bn || (b[i + 1] & ~0x3f) != 0x80) {
                    throw new UTFDataFormatException(
                            "UTF Data Format Exception");
                }
                c[len++] = (char) (((b[i] & 0x1f) << 6) | (b[i] & 0x3f));
                i++;
            } else if ((b[i] & ~0x0f) == 0xe0) {
                if (i + 2 >= bn || (b[i + 1] & ~0x3f) != 0x80
                        || (b[i + 2] & ~0x3f) != 0x80) {
                    throw new UTFDataFormatException(
                            "UTF Data Format Exception");
                }
                c[len++] = (char) (((b[i] & 0x0f) << 12)
                        | ((b[i + 1] & 0x3f) << 6) | (b[i + 2] & 0x3f));
            } else {
                throw new UTFDataFormatException("UTF Data Format Exception");
            }
        }

        String s = new String(c, 0, len);
        // logger.debug("readUTF: " + s);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("read string " + s);
        }
        return s;
    }

    public String readString() throws IOException {
        return readUTF();
    }
    
    public void close() throws IOException {
        indices_short = null;
        byte_buffer = null;
        char_buffer = null;
        short_buffer = null;
        int_buffer = null;
        long_buffer = null;
        float_buffer = null;
        double_buffer = null;
        super.close();
    }
}
