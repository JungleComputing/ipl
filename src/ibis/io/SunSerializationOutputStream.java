package ibis.io;

import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * The <code>SunSerializationOutputStream</code> class is the "glue" between
 * <code>SerializationOutputStream</code> and <code>ObjectOutputStream</code>.
 * It provides implementations for the abstract methods in
 * <code>SerializationOutputStream</code>, build on methods in
 * <code>ObjectOutputStream</code>.
 */
public final class SunSerializationOutputStream extends SerializationOutputStream {

    /**
     * Constructor. Calls constructor of superclass and flushes.
     *
     * @param s the underlying <code>OutputStream</code>
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public SunSerializationOutputStream(OutputStream s) throws IOException {
	super(s);
	flush();
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
     * Write a slice of an array of booleans.
     * Warning: duplicates are NOT detected when these calls are used!
     * If the slice consists of the complete array, the complete array
     * is written using <code>writeUnshared</code>. Otherwise a copy is
     * made into an array of length <code>len</code> and that copy is written.
     * This is a bit unfortunate, but a consequence of the Ibis
     * <code>WriteMessage</code> interface.
     *
     * @param ref the array to be written
     * @param off offset in the array from where writing starts
     * @param len the number of elements to be written
     *
     * @exception <code>IOException</code> is thrown on an IO error.
     */
    public void writeArray(boolean[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    /* So no cycle detection is used ... */
	    writeUnshared(ref);
	}
	else {
	    boolean[] temp = new boolean[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(byte[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    byte[] temp = new byte[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(short[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    short[] temp = new short[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(char[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    char[] temp = new char[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(int[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    int[] temp = new int[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(long[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    long[] temp = new long[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(float[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    float[] temp = new float[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(double[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    double[] temp = new double[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    public void writeArray(Object[] ref, int off, int len) throws IOException {
	if (off == 0 && len == ref.length) {
	    writeUnshared(ref);
	}
	else {
	    Object[] temp = new Object[len];
	    System.arraycopy(ref, off, temp, 0, len);
	    writeObject(temp);
	}
    }

    /**
     * This method should never be called. When it is, something went wrong
     * with the initialization.
     *
     * @param ref the object to be written
     * @exception <code>IOException</code> is always thrown.
     */
    protected void doWriteObject(Object ref) throws IOException {
	/* We should not get here, because doWriteObject is only
	   called from writeObjectOverride(), which is only called
	   when we are not doing Sun serialization.
	*/
	throw new IOException("doWriteObject called from sun serialization");
    }

    /**
     * No statistics are printed for the Sun serialization version.
     */
    public void statistics() {
    }
}
