package ibis.io;

import ibis.ipl.IbisIOException;

/**
 * A general data dissipator.
 * Provides for methods to read data from an underlying implementation.
 * This can be just about everything. An array, bunch of arrays,
 * DataInputstream, set of nio buffers, etc.
 * Calls to read functions may block until data is available.
 */

public interface IbisDissipator {

	/**
	 * Returns the number of bytes that can safely be read without
	 * blocking
	 */
	public int available() throws IbisIOException;

	/**
         * Tells the underlying implementation that the stream is closing
         * down. After a call to close, nothing is required to work.
         */
        public void close() throws IbisIOException;

	/**
	 * The number of bytes read from the network 
	 * since the last reset of this counter
	 * @return The number of bytes read.
	 */
	public int bytesRead();

	/**
	 * Resets the bytes read counter
	 */
	public void resetBytesRead();	

	/**
	 * Reads a Boolean from the dissipator
	 * @return	The Boolean read from the Buffer
	 */
	public boolean readBoolean() throws IbisIOException;

	/**
	 * Reads a Byte from the dissipator
	 * @return	The Byte read from the Buffer
	 */
	public byte readByte() throws IbisIOException;
	
	/**
	 * Reads a Character from the dissipator
	 * @return	The Character read from the Buffer
	 */
	public char readChar() throws IbisIOException;

	/**
	 * Reads a Short from the dissipator
	 * @return	The Short read from the Buffer
	 */
	public short readShort() throws IbisIOException;

	/**
	 * Reads a Integer from the dissipator
	 * @return	The Integer read from the Buffer
	 */
	public int readInt() throws IbisIOException;

	/**
	 * Reads a Long from the dissipator
	 * @return	The Long read from the Buffer
	 */
	public long readLong() throws IbisIOException;

	/**
	 * Reads a Float from the dissipator
	 * @return	The Float read from the Buffer
	 */
	public float readFloat() throws IbisIOException;

	/**
	 * Reads a Double from the dissipator
	 * @return	The Double read from the Buffer
	 */
	public double readDouble() throws IbisIOException;

	                                                                    
	/**
	 * Reads a (slice of) an array of Booleans out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
	public void readArray(boolean [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Booleans out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(byte [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Chacacters out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(char [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Short Integers out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(short [] destination, 
	                      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Integers out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(int [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Long Integers  out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(long [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Floats out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(float [] destination, 
			      int offset, 
			      int size) throws IbisIOException;

	/**
	 * Reads a (slice of) an array of Doubles out of the dissipator
	 * @param	destination	The array to write to.
	 * @param	offset		The first element to write
	 * @param	size		The number of elements to write
	 */
        public void readArray(double [] destination, 
			      int offset, 
			      int size) throws IbisIOException;
 
}
	
