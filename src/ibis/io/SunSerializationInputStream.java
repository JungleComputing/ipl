package ibis.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>SunSerializationInputStream</code> class is the "glue" between
 * <code>SerializationInputStream</code> and <code>ObjectInputStream</code>.
 * It provides implementations for the abstract methods in
 * <code>SerializationInputStream</code>, built on methods in
 * <code>ObjectInputStream</code>.
 */
public final class SunSerializationInputStream
	extends SerializationInputStream
{
    /**
     * Constructor. Calls constructor of superclass and flushes.
     *
     * @param s the underlying <code>InputStream</code>
     * @exception IOException when an IO error occurs.
     */
    public SunSerializationInputStream(InputStream s) throws IOException {
	super(s);
    }

    /**
     * Constructor. Calls constructor of superclass with a newly created
     * <code>InputStream</code> from the <code>IbisDissipator</code> parameter.
     *
     * @param in the <code>IbisDissipator</code>
     * @exception IOException when an IO error occurs.
     */

    public SunSerializationInputStream(IbisDissipator in) throws IOException {
	super(new DissipatorInputStream(in));
    }

    /**
     * Returns the name of the current serialization implementation: "sun".
     *
     * @return the name of the current serialization implementation.
     */
    public String serializationImplName() {
	return "sun";
    }

    /**
     * Dummy reset. For Ibis, we want to be able to remove the object table in
     * a SerializationInputStream.
     * With Sun serialization, this is accomplished by sending a RESET to it.
     * For Ibis serialization, we cannot do this because we can only send
     * a RESET when a handle is expected.
     */
    public void clear() {
    }

    /**
     * No statistics are printed for the Sun serialization version.
     */
    public void statistics() {
    }

    /**
     * Read a slice of an array of booleans. A consequence of the Ibis
     * <code>ReadMessage</code> interface is that a copy has to be made here,
     * because <code>ObjectInputStream</code> has no mechanism to read an array
     * "in place".
     *
     * @param ref the array to be read
     * @param off offset in the array from where reading starts
     * @param len the number of elements to be read
     *
     * @exception IOException when something is wrong.
     */
    public void readArray(boolean[] ref, int off, int len) throws IOException {
	try {
	    boolean[] temp = (boolean[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'boolean[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of bytes.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(byte[] ref, int off, int len) throws IOException {
	/*
	 * Calling write() and read() here turns out to be much, much faster.
	 * So, we go ahead and implement a fast path just for byte[].
	 * RFHH
	 */
	if (off == 0 && ref.length == len) {
	    int rd = 0;
	    do {
		rd += read(ref, rd, len - rd);
	    } while (rd < len);
	    return;
	}

	try {
	    byte[] temp = (byte[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'byte[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of chars.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(char[] ref, int off, int len) throws IOException {
	try {
	    char[] temp = (char[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'char[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of shorts.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(short[] ref, int off, int len) throws IOException {
	try {
	    short[] temp = (short[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'short[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of ints.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(int[] ref, int off, int len) throws IOException {
	try {
	    int[] temp = (int[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'int[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of longs.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(long[] ref, int off, int len) throws IOException {
	try {
	    long[] temp = (long[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'long[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of floats.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(float[] ref, int off, int len) throws IOException {
	try {
	    float[] temp = (float[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'float[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of doubles.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(double[] ref, int off, int len) throws IOException {
	try {
	    double[] temp = (double[]) readObject();
	    if(temp.length != len) {
		throw new ArrayIndexOutOfBoundsException(
				"Received sub array has wrong len");
	    }
	    System.arraycopy(temp, 0, ref, off, len);
	} catch (ClassNotFoundException f) {
	    throw new SerializationError("class 'double[]' not found", f);
	}
    }

    /**
     * Read a slice of an array of Objects.
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public void readArray(Object[] ref, int off, int len)
	    throws IOException, ClassNotFoundException
    {
	Object[] temp = (Object[]) readObject();
	if(temp.length != len) {
	    throw new ArrayIndexOutOfBoundsException(
			    "Received sub array has wrong len");
	}
	System.arraycopy(temp, 0, ref, off, len);
    }
}
