package ibis.io;

import java.io.IOException;

/**
 * A general data accumulator.
 * Provides for methods to write data to an underlying implementation.
 * This can be just about everything. An array, bunch of arrays,
 * DataOutputstream, set of nio buffers, etc.
 * Data written to the Accumulator may NOT be touched until after the flush
 * method is called.
 */
public abstract class Accumulator {
    /**
     * Tells the underlying implementation to flush all the data
     * out to somewhere, so all the data written to the accumulator
     * can be touched.
     *
     * @exception IOException on IO error.
     */
    public abstract void flush() throws IOException;

    /**
     * Tells the underlying implementation that the stream is closing
     * down. After a call to close, nothing is required to work.
     *
     * @exception IOException on IO error.
     */
    public abstract void close() throws IOException;

    /**
     * Return the number of bytes that was written to the message, 
     * in the stream dependant format.
     * This is the number of bytes that will be sent over the network 
     */
    public abstract long bytesWritten();

    /** 
     * Reset the counter for the number of bytes written
     */
    public abstract void resetBytesWritten();

    /**
     * Writes a boolean value to the accumulator.
     * @param     value         the boolean value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeBoolean(boolean value) throws IOException;

    /**
     * Writes a byte value to the accumulator.
     * @param     value         the byte value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeByte(byte value) throws IOException;

    /**
     * Writes a char value to the accumulator.
     * @param     value         the char value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeChar(char value) throws IOException;

    /**
     * Writes a short value to the accumulator.
     * @param     value         the short value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeShort(short value) throws IOException;

    /**
     * Writes a int value to the accumulator.
     * @param     value         the int value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeInt(int value) throws IOException;

    /**
     * Writes a long value to the accumulator.
     * @param     value         the long value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeLong(long value) throws IOException;

    /**
     * Writes a float value to the accumulator.
     * @param     value         the float value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeFloat(float value) throws IOException;

    /**
     * Writes a double value to the accumulator.
     * @param     value         the double value to write
     * @exception IOException	on an IO error
     */
    public abstract void writeDouble(double value) throws IOException;

    /**
     * Writes (a slice of) an array of Booleans into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(boolean[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Bytes into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(byte[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Characters into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(char[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Short Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(short[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(int[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Long Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(long[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Floats into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(float[] source, int offset, int length)
            throws IOException;

    /**
     * Writes (a slice of) an array of Doubles into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	length		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public abstract void writeArray(double[] source, int offset, int length)
            throws IOException;
}