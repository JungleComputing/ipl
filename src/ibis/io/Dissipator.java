package ibis.io;

import java.io.IOException;

/**
 * A general data dissipator.
 * Provides for methods to read data from an underlying implementation.
 * This can be just about everything. An array, bunch of arrays,
 * DataInputstream, set of nio buffers, etc.
 * Calls to read functions may block until data is available.
 */
public abstract class Dissipator {
    /**
     * Returns the number of bytes that can safely be read without
     * blocking
     */
    public abstract int available() throws IOException;

    /**
     * Tells the underlying implementation that the stream is closing
     * down. After a call to close, nothing is required to work.
     */
    public abstract void close() throws IOException;

    /**
     * The number of bytes read from the network 
     * since the last reset of this counter
     * @return The number of bytes read.
     */
    public abstract long bytesRead();

    /**
     * Resets the bytes read counter
     */
    public abstract void resetBytesRead();

    /**
     * Reads a Boolean from the dissipator
     * @return	The Boolean read from the Buffer
     */
    public abstract boolean readBoolean() throws IOException;

    /**
     * Reads a Byte from the dissipator
     * @return	The Byte read from the Buffer
     */
    public abstract byte readByte() throws IOException;

    /**
     * Reads a Character from the dissipator
     * @return	The Character read from the Buffer
     */
    public abstract char readChar() throws IOException;

    /**
     * Reads a Short from the dissipator
     * @return	The Short read from the Buffer
     */
    public abstract short readShort() throws IOException;

    /**
     * Reads a Integer from the dissipator
     * @return	The Integer read from the Buffer
     */
    public abstract int readInt() throws IOException;

    /**
     * Reads a Long from the dissipator
     * @return	The Long read from the Buffer
     */
    public abstract long readLong() throws IOException;

    /**
     * Reads a Float from the dissipator
     * @return	The Float read from the Buffer
     */
    public abstract float readFloat() throws IOException;

    /**
     * Reads a Double from the dissipator
     * @return	The Double read from the Buffer
     */
    public abstract double readDouble() throws IOException;

    /**
     * Reads a (slice of) an array of Booleans out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(boolean[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Booleans out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(byte[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Chacacters out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(char[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Short Integers out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(short[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Integers out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(int[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Long Integers  out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(long[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Floats out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(float[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of Doubles out of the dissipator
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public abstract void readArray(double[] destination, int offset, int length)
            throws IOException;

}