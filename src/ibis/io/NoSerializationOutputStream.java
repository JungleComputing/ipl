package ibis.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * The <code>NoSerializationOutputStream</code> class is can be used when 
 * no serialization is needed.
 * It provides implementations for the abstract methods in
 * <code>SerializationOutputStream</code>, build on methods in
 * <code>ObjectOutputStream</code>.
 * However, the only data that can be sent are bytes and byte arrays.
 * All other methods throw an exception.
 */
public final class NoSerializationOutputStream
	extends SerializationOutputStream
{

    /**
     * Constructor. Calls constructor of superclass and flushes.
     *
     * @param s the underlying <code>OutputStream</code>
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public NoSerializationOutputStream(OutputStream s) throws IOException {
	super(s);
	flush();
    }

    /**
     * Constructor. Calls constructor of superclass with a newly created
     * <code>OututStream</code> from the <code>IbisAccumulator</code>
     * parameter and flushes.
     *
     * @param out the <code>IbisAccumulator</code>
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public NoSerializationOutputStream(IbisAccumulator out) 
	    throws IOException {
	super(new AccumulatorOutputStream(out));
	flush();
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
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeUTF(String str) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeClass(Class ref) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeBoolean(boolean value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeChar(char value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeShort(int value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeInt(int value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeLong(long value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeFloat(float value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeDouble(double value) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeBytes(String s) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeChars(String s) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeObjectOverride(Object ref) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeUnshared(Object ref) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeFields() throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public PutField putFields() throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void defaultWriteObject() throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(byte[] ref, int off, int len) throws IOException {

	/*
	 * Call write() and read() here. It is supported.
	 * RFHH
	 */
	if (off == 0 && len == ref.length) {
	    write(ref);
	} else {
	    throw new IOException("Illegal data type written");
	}
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * @exception <code>IOException</code> is thrown, as this is not allowed.
     */
    public void writeArray(Object[] ref, int off, int len) throws IOException {
	    throw new IOException("Illegal data type written");
    }

    /**
     * No statistics are printed for the No serialization version.
     */
    public void statistics() {
    }
}
