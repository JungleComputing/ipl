package ibis.io;

import ibis.ipl.IbisIOException;

/**
 * A general data accumulator.
 * Provides for methods to write data to an underlying implementation.
 * This can be just about everything. An array, bunch of arrays,
 * DataOutputstream, set of nio buffers, etc.
 * Data written to the Accumulator may NOT be touched until after the flush
 * method is called.
 */
public interface IbisAccumulator {

    /**
     * Tells the underlying implementation to flush all the data
     * out to somewhere, so all the data written to the accumulator
     * can be touched.
     *
     * @exception IOException on IO error.
     */
    public void flush() throws IbisIOException;

    /**
     * Tells the underlying implementation that the stream is closing
     * down. After a call to close, nothing is required to work.
     *
     * @exception IOException on IO error.
     */
    public void close() throws IbisIOException;

    /**
     * Return the number of bytes that was written to the message, 
     * in the stream dependant format.
     * This is the number of bytes that will be sent over the network 
     */
    public int bytesWritten();

    /** 
     * Reset the counter for the number of bytes written
     */
    public void resetBytesWritten();

    /**
     * Writes a boolean value to the accumulator.
     * @param     value         the boolean value to write
     * @exception IOException	on an IO error
     */
    public void writeBoolean(boolean value) throws IbisIOException;

    /**
     * Writes a byte value to the accumulator.
     * @param     value         the byte value to write
     * @exception IOException	on an IO error
     */
    public void writeByte(byte value) throws IbisIOException;

    /**
     * Writes a char value to the accumulator.
     * @param     value         the char value to write
     * @exception IOException	on an IO error
     */
    public void writeChar(char value) throws IbisIOException;

    /**
     * Writes a short value to the accumulator.
     * @param     value         the short value to write
     * @exception IOException	on an IO error
     */
    public void writeShort(short value) throws IbisIOException;

    /**
     * Writes a int value to the accumulator.
     * @param     value         the int value to write
     * @exception IOException	on an IO error
     */
    public void writeInt(int value) throws IbisIOException;

    /**
     * Writes a long value to the accumulator.
     * @param     value         the long value to write
     * @exception IOException	on an IO error
     */
    public void writeLong(long value) throws IbisIOException;

    /**
     * Writes a float value to the accumulator.
     * @param     value         the float value to write
     * @exception IOException	on an IO error
     */
    public void writeFloat(float value) throws IbisIOException;

    /**
     * Writes a double value to the accumulator.
     * @param     value         the double value to write
     * @exception IOException	on an IO error
     */
    public void writeDouble(double value) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Booleans into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(boolean [] source, 
	    int offset,
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Bytes into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(byte [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Characters into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(char [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Short Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(short [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(int [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Long Integers into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(long [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Floats into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(float [] source, 
	    int offset, 
	    int size) throws IbisIOException;

    /**
     * Writes a (slice of) an array of Doubles into the accumulator.
     * @param	source		the array to write to the accumulator
     * @param	offset		the offset at which to start
     * @param	size		the number of elements to be copied
     * @exception IOException	on an IO error
     */
    public void writeArray(double [] source, 
	    int offset, 
	    int size) throws IbisIOException;
}
