/* $Id$ */

package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.IbisStreamFlags;
import ibis.io.DataSerializationOutputStream;
import ibis.ipl.IbisError;

import java.io.IOException;

/**
 * This is the <code>SerializationOutputStream</code> version that is used
 * for data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class NioDataSerializationOutputStream extends DataSerializationOutputStream
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

    /* This is the data output / object output part */

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

}
