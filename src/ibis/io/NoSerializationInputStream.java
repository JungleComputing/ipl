package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>NoSerializationInputStream</code> class is the "glue" between
 * <code>SerializationInputStream</code> and <code>ObjectInputStream</code>.
 * It provides implementations for the abstract methods in
 * <code>SerializationInputStream</code>, built on methods in
 * <code>ObjectInputStream</code>.
 */
public final class NoSerializationInputStream extends SerializationInputStream {

    private InputStream in;

    /**
     * Constructor. Calls constructor of superclass.
     *
     * @param s the underlying <code>InputStream</code>
     * @exception IOException when an IO error occurs.
     */
    public NoSerializationInputStream(InputStream s) throws IOException {
	super();
	in = s;
    }

    /**
     * Constructor. Calls constructor of superclass and creates an
     * <code>InputStream</code> from the <code>Dissipator</code> parameter.
     *
     * @param d the <code>Dissipator</code>
     * @exception IOException when an IO error occurs.
     */

    public NoSerializationInputStream(Dissipator d) throws IOException {
	super();
	in = new DissipatorInputStream(d);
    }

    /**
     * Returns the name of the current serialization implementation: "byte".
     *
     * @return the name of the current serialization implementation.
     */
    public String serializationImplName() {
	return "byte";
    }

    /**
     * Dummy reset. For Ibis, we want to be able to remove the object table in
     * a SerializationInputStream.
     * With no serialization, this is accomplished by sending a RESET to it.
     * For Ibis serialization, we cannot do this because we can only send a
     * RESET when a handle is expected.
     */
    public void clear() {
    }

    /**
     * No statistics are printed for the No serialization version.
     */
    public void statistics() {
    }

    public final byte readByte() throws IOException {
	return (byte) in.read();
    }

    public final boolean readBoolean() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final char readChar() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final short readShort() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final int readInt() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final long readLong() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final float readFloat() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final double readDouble() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final int readUnsignedByte() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public final int readUnsignedShort() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public String readUTF() throws IOException {
	throw new IOException("Illegal data type read");
    }

    public Class readClass() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    public final Object readObjectOverride()
	    throws IOException, ClassNotFoundException
    {
	throw new IOException("Illegal data type read");
    }

    public GetField readFields() throws IOException, ClassNotFoundException {
	throw new IOException("Illegal data type read");
    }

    public void defaultReadObject() throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * Read a slice of an array of bytes.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
	/*
	 * Call write() and read() here. It is supported.
	 * RFHH
	 */
	if (off == 0 && ref.length == len) {
	    int rd = 0;
	    do {
		rd += in.read(ref, rd, len - rd);
	    } while (rd < len);
	    return;
	}
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	throw new IOException("Illegal data type read");
    }

    /**
     * @exception IOException when called, this is illegal.
     */
    public void readArray(Object[] ref, int off, int len)
	    throws IOException, ClassNotFoundException
    {
	throw new IOException("Illegal data type read");
    }

    public void close() throws IOException {
	in.close();
    }
}
