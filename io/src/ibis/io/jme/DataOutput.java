/* $Id$ */

package ibis.io.jme;

import java.io.IOException;

/**
 * The <code>DataOutput</code> interface provides methods to write data.
 */
public interface DataOutput {

    /**
     * Writes a boolean value.
     * @param     value         the boolean value to write
     * @exception IOException	on an IO error
     */
    public void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a byte value.
     * @param     value         the byte value to write
     * @exception IOException	on an IO error
     */
    public void writeByte(byte value) throws IOException;

    /**
     * Writes a char value.
     * @param     value         the char value to write
     * @exception IOException	on an IO error
     */
    public void writeChar(char value) throws IOException;

    /**
     * Writes a short value.
     * @param     value         the short value to write
     * @exception IOException	on an IO error
     */
    public void writeShort(short value) throws IOException;

    /**
     * Writes a int value.
     * @param     value         the int value to write
     * @exception IOException	on an IO error
     */
    public void writeInt(int value) throws IOException;

    /**
     * Writes a long value.
     * @param     value         the long value to write
     * @exception IOException	on an IO error
     */
    public void writeLong(long value) throws IOException;

    /**
     * Writes a float value.
     * @param     value         the float value to write
     * @exception IOException	on an IO error
     */
    public void writeFloat(float value) throws IOException;

    /**
     * Writes a double value.
     * @param     value         the double value to write
     * @exception IOException	on an IO error
     */
    public void writeDouble(double value) throws IOException;

    /**
     * Writes (a slice of) an array of booleans.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(boolean[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of bytes.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(byte[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of characters.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(char[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of shorts.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(short[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of integers.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(int[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of longs.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(long[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of floats.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(float[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of doubles.
     * @param	source		the array to write
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(double[] source, int offset, int length)
            throws IOException;

    /**
     * Writes an array of booleans.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(boolean[] source) throws IOException;

    /**
     * Writes an array of bytes.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(byte[] source) throws IOException;

    /**
     * Writes an array of characters.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(char[] source) throws IOException;

    /**
     * Writes an array of shorts.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(short[] source) throws IOException;

    /**
     * Writes an array of integers.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(int[] source) throws IOException;

    /**
     * Writes an array of longs.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(long[] source) throws IOException;

    /**
     * Writes an array of floats.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(float[] source) throws IOException;

    /**
     * Writes an array of doubles.
     * @param	source		the array to write
     * @exception IOException	on an IO error
     */
    public void writeArray(double[] source) throws IOException;
}
