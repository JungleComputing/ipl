package ibis.io;

import ibis.ipl.Replacer;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * This abstract class is the interface provided by Ibis Serialization.
 * There are basically two ways to use this class:
 * <ul>
 * <li>Actually use <code>ObjectOutputStream</code>. In this case, the
 *     constructor version with an <code>OutputStream</code> parameter must
 *     be used. We call this the Sun serialization version.
 * <li>Redefine all of the <code>ObjectOutputStream</code>. In this case,
 *     the constructor without parameters must be used, and all methods of
 *     <code>ObjectOutputStream</code> must be redefined. This is the path
 *     taken when Ibis serialization is used.
 * </ul>
 */
public abstract class SerializationOutputStream extends ObjectOutputStream {
    protected Replacer replacer;

    /**
     * Constructor which must be called for Ibis serialization.
     * The corresponding ObjectOutputStream constructor must be called,
     * so that all of the ObjectOutputStream methods can be redefined.
     *
     * @exception <code>IOException</code> is thrown on an IO error.
     */
    protected SerializationOutputStream() throws IOException {
	super();
    }

    /**
     * Constructor which must be called for Sun serialization.
     *
     * @param s the <code>OutputStream</code> to be used
     * @exception <code>IOException</code> is thrown on an IO error.
     */
    protected SerializationOutputStream(OutputStream s) throws IOException {
	super(s);
    }

    /**
     * Set a replacer. The replacement mechanism can be used to replace
     * an object with another object during serialization. This is used
     * in RMI, for instance, to replace a remote object with a stub. 
     * The replacement mechanism provided here is independent of the
     * serialization implementation (Ibis serialization, Sun
     * serialization).
     * 
     * @param replacer the replacer object to be associated with this
     *  output stream
     */
    public void setReplacer(Replacer replacer) {
	try {
	    enableReplaceObject(true);
	} catch (Exception e) {
	}
	this.replacer = replacer;
    }

    /**
     * Object replacement for Sun serialization. This method gets called by
     * Sun object serialization when replacement is enabled.
     *
     * @param obj the object to be replaced
     * @return the result of the object replacement
     */
    protected Object replaceObject(Object obj) {
	if (obj != null && replacer != null) {
	    obj = replacer.replace(obj);
	}
	return obj;
    }

    /**
     * Returns the actual implementation used by the stream.
     *
     * @return the name of the actual serialization implementation used
     */
    public abstract String serializationImplName();

    /**
     * Write a slice of an array of booleans.
     * Warning: duplicates are NOT detected when these calls are used!
     *
     * @param ref the array to be written
     * @param off offset in the array from where writing starts
     * @param len the number of elements to be written
     *
     * @exception <code>IOException</code> is thrown on an IO error.
     */
    abstract public void writeArray(boolean[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of bytes.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(byte[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of shorts.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(short[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of chars.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(char[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of ints.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(int[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of longs.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(long[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of floats.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(float[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of doubles.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(double[] ref, int off, int len)
	throws IOException;

    /**
     * Write a slice of an array of objects.
     * See {@link #writeArray(boolean[], int, int)} for a description.
     */
    abstract public void writeArray(Object[] ref, int off, int len)
	throws IOException;

    /**
     * Write an array of booleans.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(boolean[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of bytes.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(byte[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of shorts.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(short[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of chars.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(char[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of ints.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(int[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of longs.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(long[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of floats.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(float[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of doubles.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(double[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Write an array of objects.
     *
     * @param ref the array to be written.
     * @exception <code>IOException</code> is thrown when an IO error occurs.
     */
    public void writeArray(Object[] ref) throws IOException {
	writeArray(ref, 0, ref.length);
    }

    /**
     * Print some statistics. 
     */
    abstract public void statistics();
}
