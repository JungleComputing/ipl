package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.IbisStreamFlags;
import ibis.io.SerializationOutputStream;
import ibis.ipl.IbisError;

import java.io.IOException;

/**
 * This is the <code>SerializationOutputStream</code> version that is used
 * for data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class NioDataSerializationOutputStream extends SerializationOutputStream
        implements IbisStreamFlags {
    /**
     * The underlying <code>Accumulator</code>.
     */
    private final Accumulator accumulator;

    /**
     * Constructor with an <code>Accumulator</code>.
     * @param accumulator	the underlying <code>Accumulator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public NioDataSerializationOutputStream(Accumulator accumulator)
            throws IOException {
        super();

        this.accumulator = accumulator;
    }

    public String serializationImplName() {
        return "data";
    }

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
        switch (type) {
        case TYPE_BOOLEAN:
            accumulator.writeArray((boolean[]) ref, offset, len);
            break;
        case TYPE_BYTE:
            accumulator.writeArray((byte[]) ref, offset, len);
            break;
        case TYPE_CHAR:
            accumulator.writeArray((char[]) ref, offset, len);
            break;
        case TYPE_SHORT:
            accumulator.writeArray((short[]) ref, offset, len);
            break;
        case TYPE_INT:
            accumulator.writeArray((int[]) ref, offset, len);
            break;
        case TYPE_LONG:
            accumulator.writeArray((long[]) ref, offset, len);
            break;
        case TYPE_FLOAT:
            accumulator.writeArray((float[]) ref, offset, len);
            break;
        case TYPE_DOUBLE:
            accumulator.writeArray((double[]) ref, offset, len);
            break;
        default:
            throw new IbisError("Error: writing unknown array type");
        }
    }

    /**
     * Flushes everything collected sofar.
     * @exception IOException on an IO error.
     */
    public void flush() throws IOException {
        accumulator.flush();
    }

    public void writeBoolean(boolean value) throws IOException {
        accumulator.writeBoolean(value);
    }

    public void writeByte(byte value) throws IOException {
        accumulator.writeByte(value);
    }

    public void writeByte(int value) throws IOException {
        accumulator.writeByte((byte) (value & 0xFF));
    }

    public void writeChar(char value) throws IOException {
        accumulator.writeChar(value);
    }

    public void writeShort(short value) throws IOException {
        accumulator.writeShort(value);
    }

    public void writeShort(int value) throws IOException {
        accumulator.writeShort((short) value);
    }

    public void writeInt(int value) throws IOException {
        accumulator.writeInt(value);
    }

    public void writeLong(long value) throws IOException {
        accumulator.writeLong(value);
    }

    public void writeFloat(float value) throws IOException {
        accumulator.writeFloat(value);
    }

    public void writeDouble(double value) throws IOException {
        accumulator.writeDouble(value);
    }

    public void close() throws IOException {
        accumulator.close();
    }

    public void reset() throws IOException {
    }

    /* This is the data output / object output part */

    public void write(int v) throws IOException {
        writeByte((byte) (0xff & v));
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

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

    public void writeArray(boolean[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(byte[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(short[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(char[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(int[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(long[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(float[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
    }

    public void writeArray(double[] ref, int off, int len) throws IOException {
        accumulator.writeArray(ref, off, len);
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