/* $Id$ */

package ibis.io;

import java.io.IOException;

/**
 * An inputstream for reading arrays of primitive types.
 */

public abstract class ArrayInputStream extends Dissipator implements
        IbisStreamFlags {
    /**
     * Reads (a slice of) an array of booleans in place.
     *
     * @param a		array where data is to be stored
     * @param off	offset in <code>a</code> where the slice begins
     * @param len	the number of elements to be read
     *
     * @exception IOException on IO error.
     */
    public abstract void readArray(boolean[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of bytes in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(byte[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of shorts in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(short[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of chars in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(char[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of ints in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(int[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of longs in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(long[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of floats in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(float[] a, int off, int len)
            throws IOException;

    /**
     * Reads (a slice of) an array of doubles in place.
     * See {@link #readArray(boolean[], int, int)} for a description of
     * the parameters and exceptions.
     */
    public abstract void readArray(double[] a, int off, int len)
            throws IOException;

    /**
     * Returns the number of bytes that can safely be read without
     * blocking.
     *
     * @return number of bytes.
     * @exception IOException on IO error.
     */
    public abstract int available() throws IOException;

    /**
     * The number of bytes read from the network 
     * since the last reset of this counter
     * @return The number of bytes read.
     */
    public long bytesRead() {
        System.err.println("ArrayInputStream: bytesRead() unimplemented");
        return 0;
    }

    /**
     * Resets the bytes read counter
     */
    public void resetBytesRead() {
        System.err.println("ArrayInputStream: resetBytesRead() unimplemented");
    }

    /**
     * Tells the underlying implementation that this input stream is
     * closing down.
     *
     * @exception IOException on IO error.
     */
    public abstract void close() throws IOException;

    /**
     * Reads a Boolean.
     * @return	The Boolean read from the Buffer.
     * @exception IOException when an IO error occurs.
     */
    public boolean readBoolean() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Byte from the dissipator
     * @return	The Byte read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public byte readByte() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Character from the dissipator
     * @return	The Character read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public char readChar() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Short from the dissipator
     * @return	The Short read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public short readShort() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Integer from the dissipator
     * @return	The Integer read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public int readInt() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Long from the dissipator
     * @return	The Long read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public long readLong() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Float from the dissipator
     * @return	The Float read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public float readFloat() throws IOException {
        throw new IOException("not implemented");
    }

    /**
     * Reads a Double from the dissipator
     * @return	The Double read from the Buffer
     * @exception IOException when an IO error occurs.
     */
    public double readDouble() throws IOException {
        throw new IOException("not implemented");
    }
}