package ibis.impl.nio;

import ibis.io.Dissipator;
import ibis.io.IbisStreamFlags;
import ibis.io.DataSerializationInputStream;

import java.io.IOException;

/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class NioDataSerializationInputStream extends DataSerializationInputStream
        implements IbisStreamFlags {
    /**
     * The underlying <code>Dissipator</code>.
     */
    private final Dissipator dissipator;

    /**
     * Constructor with an <code>Dissipator</code>.
     * @param dissipator	the underlying <code>Dissipator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public NioDataSerializationInputStream(Dissipator dissipator)
            throws IOException {
        super();
        this.dissipator = dissipator;
    }

    public String serializationImplName() {
        return "data";
    }

    public boolean readBoolean() throws IOException {
        return dissipator.readBoolean();
    }

    public byte readByte() throws IOException {
        return dissipator.readByte();
    }

    public char readChar() throws IOException {
        return dissipator.readChar();
    }

    public short readShort() throws IOException {
        return dissipator.readShort();
    }

    public int readInt() throws IOException {
        return dissipator.readInt();
    }

    public long readLong() throws IOException {
        return dissipator.readLong();
    }

    public float readFloat() throws IOException {
        return dissipator.readFloat();
    }

    public double readDouble() throws IOException {
        return dissipator.readDouble();
    }

    /**
     * Reads (part of) an array of booleans.
     * This method is here to make extending this class easier.
     */
    protected void readBooleanArray(boolean ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of bytes.
     * This method is here to make extending this class easier.
     */
    protected void readByteArray(byte ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of chars.
     * This method is here to make extending this class easier.
     */
    protected void readCharArray(char ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of shorts.
     * This method is here to make extending this class easier.
     */
    protected void readShortArray(short ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of ints.
     * This method is here to make extending this class easier.
     */
    protected void readIntArray(int ref[], int off, int len) throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of longs.
     * This method is here to make extending this class easier.
     */
    protected void readLongArray(long ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of floats.
     * This method is here to make extending this class easier.
     */
    protected void readFloatArray(float ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    /**
     * Reads (part of) an array of doubles.
     * This method is here to make extending this class easier.
     */
    protected void readDoubleArray(double ref[], int off, int len)
            throws IOException {
        dissipator.readArray(ref, off, len);
    }

    public int available() throws IOException {
        return dissipator.available();
    }

    public void close() throws IOException {
        dissipator.close();
    }

    public void readArray(boolean[] ref, int off, int len) throws IOException {
        readBooleanArray(ref, off, len);
    }

    public void readArray(byte[] ref, int off, int len) throws IOException {
        readByteArray(ref, off, len);
    }

    public void readArray(char[] ref, int off, int len) throws IOException {
        readCharArray(ref, off, len);
    }

    public void readArray(short[] ref, int off, int len) throws IOException {
        readShortArray(ref, off, len);
    }

    public void readArray(int[] ref, int off, int len) throws IOException {
        readIntArray(ref, off, len);
    }

    public void readArray(long[] ref, int off, int len) throws IOException {
        readLongArray(ref, off, len);
    }

    public void readArray(float[] ref, int off, int len) throws IOException {
        readFloatArray(ref, off, len);
    }

    public void readArray(double[] ref, int off, int len) throws IOException {
        readDoubleArray(ref, off, len);
    }

}
