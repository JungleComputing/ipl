package ibis.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

/**
 * This abstract class is the interface provided by Ibis Serialization.
 * There are basically two ways to use this class:
 * <ul>
 * <li>Actually use <code>ObjectInputStream</code>. In this case, the
 *     constructor version with an <code>InputStream</code> parameter must
 *     be used. We call this the Sun serialization version.
 * <li>Redefine all of the <code>ObjectInputStream</code>. In this case,
 *     the constructor without parameters must be used, and all methods
 *     of <code>ObjectInputStream</code>  must be redefined. This is the
 *     path taken when Ibis serialization is used.
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
    protected SerializationInputStream() throws IOException {
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
     * Returns the actual implementation used by the stream.
     *
     * @return the name of the actual serialization implementation used
     */
    public abstract String serializationImplName();

    /**
     * Prints some statistics.
     */
    public abstract void statistics();

    /**
     * Ibis serialization profits from an explicit clear of the object table,
     * so that any stubs in it can be garbage-collected.
     * This significantly reduces the number of connections kept alive.
     */
    public abstract void clear();

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
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(byte[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(short[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(char[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(int[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(long[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(float[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(double[] dest) throws IOException {
	readArray(dest, 0, dest.length);
    }

    /**
     * See {@link #readArray(boolean[])} for a description.
     */
    public void readArray(Object[] dest)
	    throws IOException, ClassNotFoundException
    {
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
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(byte[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(char[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(short[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(int[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(long[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(float[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     */
    public abstract void readArray(double[] ref, int off, int len)
	throws IOException;

    /**
     * See {@link #readArray(boolean[], int, int)} for a description.
     * This one may also throw a <code>ClassNotFoundException</code>.
     *
     * @exception ClassNotFoundException is thrown when the class of a
     * serialized object is not found.
     */
    public abstract void readArray(Object[] ref, int off, int len)
	throws IOException, ClassNotFoundException;
}
