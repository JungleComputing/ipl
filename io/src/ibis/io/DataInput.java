/* $Id$ */

package ibis.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * The <code>DataInput</code> interface provides methods to read data.
 */
public interface DataInput {

    /**
     * Reads a boolean value.
     * @return	The boolean read.
     */
    public boolean readBoolean() throws IOException;

    /**
     * Reads a byte value.
     * @return	The byte read.
     */
    public byte readByte() throws IOException;

    /**
     * Reads a character value.
     * @return	The character read.
     */
    public char readChar() throws IOException;

    /**
     * Reads a short value.
     * @return	The short read.
     */
    public short readShort() throws IOException;

    /**
     * Reads a integer value.
     * @return	The integer read.
     */
    public int readInt() throws IOException;

    /**
     * Reads an unsigned byte value.
     * @return	The unsiged byte read.
     */
    public int readUnsignedByte() throws IOException;

    /**
     * Reads an unsigned short value.
     * @return	The unsiged short read.
     */
    public int readUnsignedShort() throws IOException;

    /**
     * Reads a long value.
     * @return	The long read.
     */
    public long readLong() throws IOException;

    /**
     * Reads a float value.
     * @return	The float read.
     */
    public float readFloat() throws IOException;

    /**
     * Reads a double value.
     * @return	The double read.
     */
    public double readDouble() throws IOException;

    /**
     * Reads a (slice of) an array of booleans.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(boolean[] destination, int offset,
            int length) throws IOException;

    /**
     * Reads a (slice of) an array of bytes.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(byte[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of characters.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(char[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of shorts.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(short[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of integers.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(int[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of longs.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(long[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of floats.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(float[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads a (slice of) an array of doubles.
     * @param	destination	The array to write to.
     * @param	offset		The first element to write
     * @param	length		The number of elements to write
     */
    public void readArray(double[] destination, int offset, int length)
            throws IOException;

    /**
     * Reads an array of booleans.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(boolean[] source) throws IOException;

    /**
     * Reads an array of bytes.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(byte[] source) throws IOException;

    /**
     * Reads an array of characters.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(char[] source) throws IOException;

    /**
     * Reads an array of shorts.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(short[] source) throws IOException;

    /**
     * Reads an array of integers.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(int[] source) throws IOException;

    /**
     * Reads an array of longs.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(long[] source) throws IOException;

    /**
     * Reads an array of floats.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(float[] source) throws IOException;

    /**
     * Reads an array of doubles.
     * @param	source		the array to read
     * @exception IOException	on an IO error
     */
    public void readArray(double[] source) throws IOException;
    
    /**
     * Reads into the contents of the byte buffer (between its current position and its
     * limit).
     * @param value
     * 		the byte buffer from which data is to be written
     * @exception IOException
     *          an error occurred
     * @exception ReadOnlyBufferException
     *          is thrown when the buffer is read-only.
     */
    public void readByteBuffer(ByteBuffer value)
    		throws IOException, ReadOnlyBufferException;
}
