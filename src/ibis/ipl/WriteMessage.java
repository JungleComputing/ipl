package ibis.ipl;

/** 
    For all write methods in this class, the invariant is that the reads must match the writes one by one.
    This means that the results are UNDEFINED when an array is written with writeXXXArray, but read with readObject.
**/

public interface WriteMessage { 

	/**
	 * Start sending the message to all ReceivePorts this SendPort is connected to.
	 * Data may be streamed, so the user is not allowed to touch the data, as the send is NON-blocking.
	 * @exception IbisIOException       an error occurred 
	 **/
	public void send() throws IbisIOException; 

	/**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
	public void finish() throws IbisIOException;

	/**
	   Block until the entire message has been sent and clear data within the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished.
	   The message stays alive for subsequent writes and sends.
	   reset can be seen as a shorthand for finish(); sendPort.newMessage() **/
	public void reset() throws IbisIOException;

	/**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
	public void writeBoolean(boolean value) throws IbisIOException;

	/**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
	public void writeByte(byte value) throws IbisIOException;

	/**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
	public void writeChar(char value) throws IbisIOException;

	/**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
	public void writeShort(short value) throws IbisIOException;

	/**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
	public void writeInt(int value) throws IbisIOException;

	/**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
	public void writeLong(long value) throws IbisIOException;

	/**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
	public void writeFloat(float value) throws IbisIOException;

	/**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
	public void writeDouble(double value) throws IbisIOException;

	/**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
	public void writeString(String value) throws IbisIOException;

	/**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
	public void writeObject(Object value) throws IbisIOException;

	public void writeArrayBoolean(boolean [] destination) throws IbisIOException;
	public void writeArrayByte(byte [] destination) throws IbisIOException;
	public void writeArrayChar(char [] destination) throws IbisIOException;
	public void writeArrayShort(short [] destination) throws IbisIOException;
	public void writeArrayInt(int [] destination) throws IbisIOException;
	public void writeArrayLong(long [] destination) throws IbisIOException;
	public void writeArrayFloat(float [] destination) throws IbisIOException;
	public void writeArrayDouble(double [] destination) throws IbisIOException;

	public void writeSubArrayBoolean(boolean [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayByte(byte [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayChar(char [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayShort(short [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayInt(int [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayLong(long [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayFloat(float [] destination, int offset, int size) throws IbisIOException;
	public void writeSubArrayDouble(double [] destination, int offset, int size) throws IbisIOException;
} 
