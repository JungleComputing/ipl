/* $Id$ */

package ibis.io;

import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.UTFDataFormatException;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class DataSerializationInputStream extends ByteSerializationInputStream
        implements IbisStreamFlags {
    /** When true, no buffering in this layer. */
    private static final boolean NO_ARRAY_BUFFERS
            = TypedProperties.booleanProperty(IOProps.s_no_array_buffers);

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

    /**
     * Constructor with an <code>DataInputStream</code>.
     * @param in		the underlying <code>DataInputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationInputStream(DataInputStream in) throws IOException {
        super(in);
        if (! NO_ARRAY_BUFFERS) {
            initArrays();
        }
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected DataSerializationInputStream() throws IOException {
        super();
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
        if (DEBUG) {
            dbPrint("read boolean: " + a);
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
        if (DEBUG) {
            dbPrint("read byte: " + a);
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
        if (DEBUG) {
            dbPrint("read char: " + a);
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
        if (DEBUG) {
            dbPrint("read short: " + a);
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
        if (DEBUG) {
            dbPrint("read int[HEX]: " + a + "[0x" + Integer.toHexString(a)
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
        if (DEBUG) {
            dbPrint("read long: " + a);
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
        if (DEBUG) {
            dbPrint("read float: " + a);
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
        if (DEBUG) {
            dbPrint("read double: " + a);
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
            if (DEBUG) {
                dbPrint("readArrayBoolean: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_BOOLEAN);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special boolean array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayByte: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_BYTE);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special byte array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayChar: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_CHAR);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special char array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayShort: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_SHORT);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special short array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayInt: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_INT);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special int array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayLong: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_LONG);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special long array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayFloat: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_FLOAT);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special float array read len " + len);
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
            if (DEBUG) {
                dbPrint("readArrayDouble: " + ref + " offset: " + off
                        + " len: " + len + " type: " + TYPE_DOUBLE);
            }
            in.readArray(ref, off, len);
        } else {
            // System.err.println("Special double array read len " + len);
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
        if (DEBUG) {
            dbPrint("doing a receive()");
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
                dbPrint("EEEEK : receiving while there is data in buffer !!!");
                dbPrint("byte_index " + (max_byte_index - byte_index));
                dbPrint("char_index " + (max_char_index - char_index));
                dbPrint("short_index " + (max_short_index - short_index));
                dbPrint("int_index " + (max_int_index - int_index));
                dbPrint("long_index " + (max_long_index - long_index));
                dbPrint("float_index " + (max_float_index - float_index));
                dbPrint("double_index " + (max_double_index - double_index));
                dbPrint("array_index " + (max_array_index - array_index));

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

        if (DEBUG) {
            dbPrint("reading arrays " + max_array_index);
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

        if (DEBUG) {
            dbPrint("readUTF: len = " + bn);
        }

        if (bn == -1) {
            if (TIME_DATA_SERIALIZATION) {
                stopTimer();
            }
            return null;
        }

        byte[] b = new byte[bn];
        readArray(b, 0, bn);

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
        // dbPrint("readUTF: " + s);
        if (TIME_DATA_SERIALIZATION) {
            stopTimer();
        }

        if (DEBUG) {
            dbPrint("read string " + s);
        }
        return s;
    }

    public String readString() throws IOException {
        return readUTF();
    }
}
