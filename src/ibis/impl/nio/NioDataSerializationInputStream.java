package ibis.impl.nio;

import ibis.io.Dissipator;
import ibis.io.IbisStreamFlags;
import ibis.io.SerializationInputStream;

import java.io.IOException;



/**
 * This is the <code>SerializationInputStream</code> version that is used
 * for Data serialization. With data serialization, you can only write
 * basic types and arrays of basic types.
 */
public class NioDataSerializationInputStream
	extends SerializationInputStream
	implements IbisStreamFlags
{
    /**
     * The underlying <code>Dissipator</code>.
     */
    private final Dissipator dissipator;

    /**
     * Constructor with an <code>Dissipator</code>.
     * @param dissipator	the underlying <code>Dissipator</code>
     * @exception IOException	gets thrown when an IO error occurs.
     */
    public NioDataSerializationInputStream(Dissipator dissipator) throws IOException {
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
    protected void readIntArray(int ref[], int off, int len)
	    throws IOException {
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

    /**
     * @exception IOException is thrown, as this is not allowed.
     */
    public void readArray(Object[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    public void clear() {
    }

    public void statistics() {
    }

    /* This is the data output / object output part */

    public int read() throws IOException {
	return readByte();
    }

    public int read(byte[] b) throws IOException {
	return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
	return len;
    }

    public long skip(long n) throws IOException {
	throw new IOException("skip not meaningful in typed input stream");
    }

    public int skipBytes(int n) throws IOException {
	throw new IOException("skipBytes not meaningful in typed input stream");
    }


    public void readFully(byte[] b) throws IOException {
	readFully(b, 0, b.length);
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
	readArray(b, off, len);
    }

    public final int readUnsignedByte() throws IOException {
	int i = readByte();
	if (i < 0) {
	    i += 256;
	}
	return i;
    }

    public final int readUnsignedShort() throws IOException {
	int i = readShort();
	if (i < 0) {
	    i += 65536;
	}
	return i;
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public String readUTF() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public Class readClass() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public String readString() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public final Object readObjectOverride() throws IOException
    {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public GetField readFields() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal
     */
    public void defaultReadObject() throws IOException {
	throw new IOException("Illegal data type read");
    }
}
