package ibis.io;

import ibis.ipl.IbisIOException;

/**
 * A general data accumulator.
 * Provides for methods to write data to an underlying implementation.
 * This can be just about everything. An array, bunch of arrays,
 * DataOutputstream, set of nio buffers, etc.
 * Data written to the Accumulator may NOT be touched untill after the flush
 * method is called.
 */
public interface IbisAccumulator {

	/**
	 * Tells the underlying implementation to flush all the data
	 * out to somewhere, so all the data written to the accumulator
	 * can be touched.
	 */
	public void flush() throws IbisIOException;

	/**
	 * Tells the underlying implementation that the stream is closing
	 * down. After a call to close, nothing is required to work.
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
	 * @param     value             The boolean value to write.
	 */
	public void writeBoolean(boolean value) throws IbisIOException;

	/**
	 * Writes a byte value to the accumulator.
	 * @param     value             The byte value to write.
	 */
	public void writeByte(byte value) throws IbisIOException;

	/**
         * Writes a char value to the accumulator.
         * @param     value             The char value to write.
         */
        public void writeChar(char value) throws IbisIOException;

        /**
         * Writes a short value to the accumulator.
         * @param     value             The short value to write.
         */
        public void writeShort(short value) throws IbisIOException;
                                                                               
        /**
         * Writes a int value to the accumulator.
         * @param     value             The int value to write.
         */
        public void writeInt(int value) throws IbisIOException;
                                                                               
        /**
         * Writes a long value to the accumulator.
         * @param     value             The long value to write.
         */
        public void writeLong(long value) throws IbisIOException;

        /**
         * Writes a float value to the accumulator.
         * @param     value             The float value to write.
         */
        public void writeFloat(float value) throws IbisIOException;
                                                                               
        /**
         * Writes a double value to the accumulator.
         * @param     value             The double value to write.
         */
        public void writeDouble(double value) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Booleans into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
	public void writeArray(boolean [] source, 
			       int offset,
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Bytes into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(byte [] source, 
			       int offset, 
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Characters into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(char [] source, 
			       int offset, 
			       int size) throws IbisIOException;
					
	/**
	 * Writes a (slice of) an array of Short Integers into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(short [] source, 
			       int offset, 
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Integers into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(int [] source, 
			       int offset, 
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Long Integers into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(long [] source, 
			       int offset, 
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Floats into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(float [] source, 
			       int offset, 
			       int size) throws IbisIOException;

	/**
	 * Writes a (slice of) an array of Doubles into the accumulator.
	 * @param	source		The array to write to the accumulator.
	 * @param	offset		The offset at which to start.
	 * @param	size		The number of elements to be copied.
	 */
        public void writeArray(double [] source, 
			       int offset, 
			       int size) throws IbisIOException;
}
