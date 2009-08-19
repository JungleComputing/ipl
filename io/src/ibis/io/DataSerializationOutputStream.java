/* $Id$ */

package ibis.io;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the <code>SerializationOutputStream</code> version that is used
 * for data serialization. With data serialization, you can only write
 * basic types and arrays of basic types. It also serves as a base type
 * for Ibis serialization.
 */
public class DataSerializationOutputStream extends ByteSerializationOutputStream {
    
    private static final boolean DEBUG = IOProperties.DEBUG;
    
    private static final int SMALL_ARRAY_BOUND = IOProperties.SMALL_ARRAY_BOUND;
    
    private static final int ARRAY_BUFFER_SIZE = IOProperties.ARRAY_BUFFER_SIZE;
    
    /** When true, no buffering in this layer. */
    private static final boolean NO_ARRAY_BUFFERS
            = IOProperties.properties.getBooleanProperty(IOProperties.s_no_array_buffers);
    
    private static Logger logger = LoggerFactory.getLogger(DataSerializationOutputStream.class);

    /** If <code>false</code>, makes all timer calls disappear. */
    private static final boolean TIME_DATA_SERIALIZATION
            = IOProperties.properties.getBooleanProperty(IOProperties.s_timer_data)
                || IOProperties.properties.getBooleanProperty(IOProperties.s_timer_ibis);
    
    /** Boolean count is not used, use it for arrays. */
    static final int TYPE_ARRAY = Constants.TYPE_BOOLEAN;

    /** Storage for bytes (or booleans) written. */
    private byte[] byte_buffer;

    /** Storage for chars written. */
    private char[] char_buffer;

    /** Storage for shorts written. */
    private short[] short_buffer;

    /** Storage for ints written. */
    private int[] int_buffer;

    /** Storage for longs written. */
    private long[] long_buffer;

    /** Storage for floats written. */
    private float[] float_buffer;

    /** Storage for doubles written. */
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

    /**
     * Register how often we need to acquire a new set of primitive array
     * buffers.
     private int unfinished;

     {
     Runtime.getRuntime().addShutdownHook(new Thread() {
     public void run() {
     logger.info(DataSerializationOutputStream.this +
     ": unfinished calls " + unfinished);
     statistics();
     }
     });
     }
     */
    private final int BYTE_BUFFER_SIZE;
    
    private final int CHAR_BUFFER_SIZE;
    
    private final int SHORT_BUFFER_SIZE;
    
    private final int INT_BUFFER_SIZE;
    
    private final int LONG_BUFFER_SIZE;
    
    private final int FLOAT_BUFFER_SIZE;
    
    private final int DOUBLE_BUFFER_SIZE;
    
    /** Structure summarizing an array write. */
    private static final class ArrayDescriptor {
        int type;

        Object array;

        int offset;

        int len;
    }

    /** Where the arrays to be written are collected. */
    private ArrayDescriptor[] array;

    /** Index in the <code>array</code> array. */
    private int array_index;

    /** Collects all indices of the <code>_buffer</code> arrays. */
    private short[] indices_short;

    /** For each. */
    private boolean[] touched = new boolean[Constants.PRIMITIVE_TYPES];
    
    /** Timer. */
    final SerializationTimer timer;

    /**
     * Constructor with a <code>DataOutputStream</code>.
     * @param out		the underlying <code>DataOutputStream</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public DataSerializationOutputStream(DataOutputStream out)
            throws IOException {
        super(out);
        int bufferSize = out.bufferSize();
        if (bufferSize <= 0) {
            bufferSize = IOProperties.TYPED_BUFFER_SIZE;
        }
        BYTE_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_BYTE);
        CHAR_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_CHAR);
        SHORT_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_SHORT);
        INT_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_INT);
        LONG_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_LONG);
        FLOAT_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_FLOAT);
        DOUBLE_BUFFER_SIZE = DataSerializationInputStream.typedBufferSize(bufferSize, Constants.SIZEOF_DOUBLE);

        if (! NO_ARRAY_BUFFERS) {
            initArrays();
        }
        
        if (TIME_DATA_SERIALIZATION) {
            timer = new SerializationTimer(toString());
        } else {
            timer = null;
        }
    }

    /**
     * Constructor, may be used when this class is sub-classed.
     */
    protected DataSerializationOutputStream() throws IOException {
        super();
        BYTE_BUFFER_SIZE = 0;
        CHAR_BUFFER_SIZE = 0;
        SHORT_BUFFER_SIZE = 0;
        INT_BUFFER_SIZE = 0;
        LONG_BUFFER_SIZE = 0;
        FLOAT_BUFFER_SIZE = 0;
        DOUBLE_BUFFER_SIZE = 0;
        
        if (TIME_DATA_SERIALIZATION) {
            timer = new SerializationTimer(toString());
        } else {
            timer = null;
        }
    }

    public String serializationImplName() {
        return "data";
    }

    public void statistics() {
        // No statistics
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < IOProperties.SMALL_ARRAY_BOUND / Constants.SIZEOF_BOOLEAN) {
            /* Maybe lift the check from the writeBoolean? */
            for (int i = offset; i < offset + len; i++) {
                writeBoolean(ref[i]);
            }

        } else {
            if (array_index == IOProperties.ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayBoolean: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_BOOLEAN);
            }
            array[array_index].type = Constants.TYPE_BOOLEAN;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_BOOLEAN, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_BYTE) {
            for (int i = offset; i < offset + len; i++) {
                writeByte(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayByte: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_BYTE);
            }
            array[array_index].type = Constants.TYPE_BYTE;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_BYTE, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_CHAR) {
            for (int i = offset; i < offset + len; i++) {
                writeChar(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayChar: " + new String(ref) + " offset: "
                        + offset + " len: " + len + " type: " + Constants.TYPE_CHAR);
            }
            array[array_index].type = Constants.TYPE_CHAR;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_CHAR, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_SHORT) {
            for (int i = offset; i < offset + len; i++) {
                writeShort(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayShort: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_SHORT);
            }
            array[array_index].type = Constants.TYPE_SHORT;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref,Constants. TYPE_SHORT, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_INT) {
            for (int i = offset; i < offset + len; i++) {
                writeInt(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayInt: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_INT);
            }
            array[array_index].type = Constants.TYPE_INT;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_INT, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < IOProperties.SMALL_ARRAY_BOUND / Constants.SIZEOF_LONG) {
            for (int i = offset; i < offset + len; i++) {
                writeLong(ref[i]);
            }

        } else {
            if (array_index == IOProperties.ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayLong: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_LONG);
            }
            array[array_index].type = Constants.TYPE_LONG;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_LONG, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_FLOAT) {
            for (int i = offset; i < offset + len; i++) {
                writeFloat(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayFloat: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_FLOAT);
            }
            array[array_index].type = Constants.TYPE_FLOAT;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_FLOAT, len);
        }
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
        if (NO_ARRAY_BUFFERS) {
            out.writeArray(ref, offset, len);
        } else if (len < SMALL_ARRAY_BOUND / Constants.SIZEOF_DOUBLE) {
            for (int i = offset; i < offset + len; i++) {
                writeDouble(ref[i]);
            }

        } else {
            if (array_index == ARRAY_BUFFER_SIZE) {
                internalFlush();
            }
            if (DEBUG && logger.isDebugEnabled()) {
                logger.debug("writeArrayDouble: " + ref + " offset: " + offset
                        + " len: " + len + " type: " + Constants.TYPE_DOUBLE);
            }
            array[array_index].type = Constants.TYPE_DOUBLE;
            array[array_index].offset = offset;
            array[array_index].len = len;
            array[array_index].array = ref;
            array_index++;

            addStatSendArray(ref, Constants.TYPE_DOUBLE, len);
        }
    }

    void addStatSendArray(Object ref, int type, int len) {
        // empty
    }

    /**
     * Flushes everything collected sofar.
     * @exception IOException on an IO error.
     */
    public void flush() throws IOException {
        internalFlush();
        out.finish();
    }

    private void internalFlush() throws IOException {

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("doing a flush()");
        }

        if (TIME_DATA_SERIALIZATION) {
            timer.suspend();
        }

        if (! NO_ARRAY_BUFFERS) {
            flushBuffers();

            /* Retain the order in which the arrays were pushed. This 
             * costs a cast at receive time.
             */
            for (int i = 0; i < array_index; i++) {
                ArrayDescriptor a = array[i];
                switch (a.type) {
                case Constants.TYPE_BOOLEAN:
                    out.writeArray((boolean[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_BYTE:
                    out.writeArray((byte[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_CHAR:
                    out.writeArray((char[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_SHORT:
                    out.writeArray((short[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_INT:
                    out.writeArray((int[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_LONG:
                    out.writeArray((long[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_FLOAT:
                    out.writeArray((float[]) a.array, a.offset, a.len);
                    break;
                case Constants.TYPE_DOUBLE:
                    out.writeArray((double[]) a.array, a.offset, a.len);
                    break;
                }
                a.array = null;
            }

            array_index = 0;
        }

        out.flush();

        if (TIME_DATA_SERIALIZATION) {
            timer.resume();
        }

        if (! NO_ARRAY_BUFFERS && !out.finished()) {
            indices_short = new short[Constants.PRIMITIVE_TYPES];
            if (touched[Constants.TYPE_BYTE]) {
                byte_buffer = new byte[BYTE_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_CHAR]) {
                char_buffer = new char[CHAR_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_SHORT]) {
                short_buffer = new short[SHORT_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_INT]) {
                int_buffer = new int[INT_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_LONG]) {
                long_buffer = new long[LONG_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_FLOAT]) {
                float_buffer = new float[FLOAT_BUFFER_SIZE];
            }
            if (touched[Constants.TYPE_DOUBLE]) {
                double_buffer = new double[DOUBLE_BUFFER_SIZE];
            }
            // unfinished++;
        }

        for (int i = 0; i < Constants.PRIMITIVE_TYPES; i++) {
            touched[i] = false;
        }
    }

    /**
     * Writes a boolean value to the accumulator.
     * @param     value             The boolean value to write.
     * @exception IOException on IO error.
     */
    public void writeBoolean(boolean value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeBoolean(value);
        } else {
            if (byte_index == byte_buffer.length) {
                internalFlush();
            }
            byte_buffer[byte_index++] = (byte) (value ? 1 : 0);
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote boolean " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a byte value to the accumulator.
     * @param     value             The byte value to write.
     * @exception IOException on IO error.
     */
    public void writeByte(byte value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeByte(value);
        } else {
            if (byte_index == byte_buffer.length) {
                internalFlush();
            }
            byte_buffer[byte_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote byte " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a char value to the accumulator.
     * @param     value             The char value to write.
     * @exception IOException on IO error.
     */
    public void writeChar(char value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeChar(value);
        } else {
            if (char_index == char_buffer.length) {
                internalFlush();
            }
            char_buffer[char_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote char " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a short value to the accumulator.
     * @param     value             The short value to write.
     * @exception IOException on IO error.
     */
    public void writeShort(short value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeShort(value);
        } else {
            if (short_index == short_buffer.length) {
                internalFlush();
            }
            short_buffer[short_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote short " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a int value to the accumulator.
     * @param     value             The int value to write.
     * @exception IOException on IO error.
     */
    public void writeInt(int value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeInt(value);
        } else {
            if (int_index == int_buffer.length) {
                internalFlush();
            }
            int_buffer[int_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote int[HEX] " + value + "[0x"
                    + Integer.toHexString(value) + "]");
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a long value to the accumulator.
     * @param     value             The long value to write.
     * @exception IOException on IO error.
     */
    public void writeLong(long value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeLong(value);
        } else {
            if (long_index == long_buffer.length) {
                internalFlush();
            }
            long_buffer[long_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote long " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a float value to the accumulator.
     * @param     value             The float value to write.
     * @exception IOException on IO error.
     */
    public void writeFloat(float value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeFloat(value);
        } else {
            if (float_index == float_buffer.length) {
                internalFlush();
            }
            float_buffer[float_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote float " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Writes a double value to the accumulator.
     * @param     value             The double value to write.
     * @exception IOException on IO error.
     */
    public void writeDouble(double value) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (NO_ARRAY_BUFFERS) {
            out.writeDouble(value);
        } else {
            if (double_index == double_buffer.length) {
                internalFlush();
            }
            double_buffer[double_index++] = value;
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("wrote double " + value);
        }
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    /**
     * Allocates buffers.
     */
    private void initArrays() {
        array = new ArrayDescriptor[ARRAY_BUFFER_SIZE];
        for (int i = 0; i < ARRAY_BUFFER_SIZE; i++) {
            array[i] = new ArrayDescriptor();
        }

        indices_short = new short[Constants.PRIMITIVE_TYPES];
        byte_buffer = new byte[BYTE_BUFFER_SIZE];
        char_buffer = new char[CHAR_BUFFER_SIZE];
        short_buffer = new short[SHORT_BUFFER_SIZE];
        int_buffer = new int[INT_BUFFER_SIZE];
        long_buffer = new long[LONG_BUFFER_SIZE];
        float_buffer = new float[FLOAT_BUFFER_SIZE];
        double_buffer = new double[DOUBLE_BUFFER_SIZE];
    }

    /* This is the data output / object output part */

    public void writeString(String str) throws IOException {
        writeUTF(str);
    }

    public void writeUTF(String str) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        if (str == null) {
            writeInt(-1);
            if (TIME_DATA_SERIALIZATION) {
                timer.stop();
            }
            return;
        }

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("write UTF " + str);
        }
        int len = str.length();

        // writeInt(len);
        // writeArray(str.toCharArray(), 0, len);

        int bn = 0;

        for (int i = 0; i < len; i++) {
            int c = str.charAt(i);      // widening char to int zero-extends
            if (c > 0x0000 && c <= 0x007f) {
                bn++;
            } else if (c <= 0x07ff) {
                bn += 2;
            } else {
                bn += 3;
            }
        }

        byte[] b = new byte[bn];
        bn = 0;

        for (int i = 0; i < len; i++) {
            int c = str.charAt(i);      // widening char to int zero-extends
            if (c > 0x0000 && c <= 0x007f) {
                b[bn++] = (byte) c;
            } else if (c <= 0x07ff) {
                b[bn++] = (byte) (0xc0 | (0x1f & (c >> 6)));
                b[bn++] = (byte) (0x80 | (0x3f & c));
            } else {
                b[bn++] = (byte) (0xe0 | (0x0f & (c >> 12)));
                b[bn++] = (byte) (0x80 | (0x3f & (c >> 6)));
                b[bn++] = (byte) (0x80 | (0x3f & c));
            }
        }
        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writeUTF: len = " + bn);
            for (int i = 0; i < bn; i++) {
                logger.debug("writeUTF: b[" + i + "] = " + (b[i] & 0xff));
            }
        }

        writeInt(bn);
        writeArrayByte(b, 0, bn);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
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
        indices_short[TYPE_ARRAY] = (short) array_index;
        indices_short[Constants.TYPE_BYTE] = (short) byte_index;
        indices_short[Constants.TYPE_CHAR] = (short) char_index;
        indices_short[Constants.TYPE_SHORT] = (short) short_index;
        indices_short[Constants.TYPE_INT] = (short) int_index;
        indices_short[Constants.TYPE_LONG] = (short) long_index;
        indices_short[Constants.TYPE_FLOAT] = (short) float_index;
        indices_short[Constants.TYPE_DOUBLE] = (short) double_index;

        if (DEBUG && logger.isDebugEnabled()) {
            logger.debug("writing arrays " + array_index);
            logger.debug("writing bytes " + byte_index);
            logger.debug("writing chars " + char_index);
            logger.debug("writing shorts " + short_index);
            logger.debug("writing ints " + int_index);
            logger.debug("writing longs " + long_index);
            logger.debug("writing floats " + float_index);
            logger.debug("writing doubles " + double_index);
        }

        out.writeArray(indices_short, Constants.BEGIN_TYPES,
                Constants.PRIMITIVE_TYPES - Constants.BEGIN_TYPES);

        if (byte_index > 0) {
            out.writeArray(byte_buffer, 0, byte_index);
            touched[Constants.TYPE_BYTE] = true;
        }
        if (char_index > 0) {
            out.writeArray(char_buffer, 0, char_index);
            touched[Constants.TYPE_CHAR] = true;
        }
        if (short_index > 0) {
            out.writeArray(short_buffer, 0, short_index);
            touched[Constants.TYPE_SHORT] = true;
        }
        if (int_index > 0) {
            out.writeArray(int_buffer, 0, int_index);
            touched[Constants.TYPE_INT] = true;
        }
        if (long_index > 0) {
            out.writeArray(long_buffer, 0, long_index);
            touched[Constants.TYPE_LONG] = true;
        }
        if (float_index > 0) {
            out.writeArray(float_buffer, 0, float_index);
            touched[Constants.TYPE_FLOAT] = true;
        }
        if (double_index > 0) {
            out.writeArray(double_buffer, 0, double_index);
            touched[Constants.TYPE_DOUBLE] = true;
        }

        reset_indices();
    }

    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayBoolean(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(byte[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayByte(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(short[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayShort(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(char[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayChar(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(int[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayInt(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(long[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayLong(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(float[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayFloat(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void writeArray(double[] ref, int off, int len) throws IOException {
        if (TIME_DATA_SERIALIZATION) {
            timer.start();
        }
        writeArrayDouble(ref, off, len);
        if (TIME_DATA_SERIALIZATION) {
            timer.stop();
        }
    }

    public void close() throws IOException {

        super.close();

        byte_buffer = null;
        char_buffer = null;
        short_buffer = null;
        int_buffer = null;
        long_buffer = null;
        float_buffer = null;
        double_buffer = null;
        array = null;
    }
}
