package ibis.io;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * This abstract class is the interface provided by Ibis Serialization.
 * There are basically two ways to use this class:
 * <ul>
 * <li>Actually use <code>ObjectInputStream</code>. In this case, the constructor
 *    version with an <code>InputStream</code> parameter must be used. We call this
 *    the Sun serialization version.
 * <li>Redefine all of the <code>ObjectInputStream</code>. In this case, the constructor
 *    without parameters must be used, and all methods of <code>ObjectInputStream</code>
 *    must be redefined. This is the path taken when Ibis serialization
 *    is used.
 * </ul>
 */
public abstract class SerializationInputStream extends ObjectInputStream {

    /**
     * Constructor which must be called for Ibis serialization.
     * The corresponding ObjectIputStream constructor must be called,
     * so that all of the ObjectInputStream methods can be redefined.
     *
     * @exception IOException is thrown on an IO error.
     */
    SerializationInputStream() throws IOException {
	super();
    }

    /**
     * Constructor which must be called for Sun serialization.
     *
     * @param s the <code>InputStream</code> to be used
     * @exception IOException is thrown on an IO error.
     */
    SerializationInputStream(InputStream s) throws IOException {
	super(s);
    }

    /**
     * Returns the actual implementation used by the stream, either "sun" or "ibis".
     *
     * @return the name of the actual serialization implementation used
     */
    public abstract String serializationImplName();
    
    /**
     * Prints some statistics.
     */
    public abstract void statistics();

    /**
     * Receives an array in place. No duplicate checks are done.
     * These methods are a shortcut for: readArray(dest, 0, dest.length);
     * The destination array should be of the correct length!
     *
     * @param dest where the received array is stored.
     * @exception IOException is thrown on an IO error.
     */
    public void readArray(boolean[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(byte[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(short[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(char[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(int[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(long[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(float[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(double[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[]) for a description.
     */
    public void readArray(Object[] dest) throws IOException, ClassNotFoundException {
	readArray(dest, 0, dest.length);
    }

    /**
     * Reads a slice of an array in place. No cycle checks are done. 
     *
     * @param ref array in which the slice is stored
     * @param off offset where the slice starts
     * @param len length of the slice (the number of elements)
     * @exception IOException is thrown on an IO error.
     */
    public abstract void readArray(boolean[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(byte[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(char[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(short[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(int[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(long[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(float[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     */
    public abstract void readArray(double[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int) for a description.
     * This one may also throw a <code>ClassNotFoundException</code>.
     *
     * @exception ClassNotFoundException is thrown when the class of a
     * serialized object is not found.
     */
    public abstract void readArray(Object[] ref, int off, int len)
	throws IOException, ClassNotFoundException;

    /**
     * We cannot redefine <code>readObject, because it is final
     * in <code>ObjectInputStream</code>. The trick for Ibis serialization
     * is to have the <code>ObjectInputStream</code> be initialized with
     * its parameter-less constructor.  This will cause its <codeLreadObject</code>
     * method to call <code>readObjectOverride</code> instead of doing its own thing.
     *
     * @return the object read
     * @exception IOException is thrown on an IO error.
     * @exception ClassNotFoundException is thrown when the class of a
     * serialized object is not found.
     */
    protected final Object readObjectOverride() throws IOException, ClassNotFoundException {
	return doReadObject();
    }

    /**
     * Reads objects and arrays. To be specified by <code>IbisSerializationOutputStream</code>.
     * The <code>SunSerializationInputStream</code> version should never be
     * called, because <code>doReadObject</code> is only called from
     * <code>readObjectOverride</code>, which only gets called when we are
     * doing Ibis serialization.
     *
     * @return the object read
     * @exception IOException is thrown on an IO error.
     * @exception ClassNotFoundException is thrown when the class of a
     * serialized object is not found.
     */
    protected abstract Object doReadObject() throws IOException, ClassNotFoundException;
}
