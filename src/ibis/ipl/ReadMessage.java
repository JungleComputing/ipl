package ibis.ipl;

public interface ReadMessage { 
	/** Only one message is alive at one time for a given
	    receiveport.  This is done to prevent flow control problems. 
	    A receiveport can be configured to generate upcalls or to support blocking receive, but NOT both!
	**/

	/**
	   This operation is used to indicate that the reader is done
	   with the message.  After the finish, no more bytes can be read from
	   the message.  The thread reading the message (can be an upcall) is NOT
	   allowed to block when a message is alive but not finished.  Only after
	   the finish is called can the thread that holds the message block.  The
	   finish operation must always be called when blocking receive is used.
	   When upcalls are used, the finish can be avoided when the user is sure
	   that the upcall never blocks / waits for a message to arrive. In that
	   case, the message is automatically finished when the upcall
	   terminates.  This is much more efficient, because this way, the
	   runtime system can reuse the upcall thread!
	 **/
	public void finish() throws IbisIOException;

	public long sequenceNumber();
	public SendPortIdentifier origin();

	public boolean readBoolean() throws IbisIOException;
	public byte readByte() throws IbisIOException;
	public char readChar() throws IbisIOException;
	public short readShort() throws IbisIOException;
	public int readInt() throws IbisIOException;
	public long readLong() throws IbisIOException;	
	public float readFloat() throws IbisIOException;
	public double readDouble() throws IbisIOException;
	public String readString() throws IbisIOException;
	public Object readObject() throws IbisIOException;

	/** Methods to receive arrays in place. No duplicate checks are done.
	    These methods are a shortcut for:
	    readArray(destination, 0, destination.length);

	    It is therefore legal to use a readArrayXXX, with a corresponding writeArray.
	**/
	public void readArray(boolean [] destination) throws IbisIOException;
	public void readArray(byte [] destination) throws IbisIOException;
	public void readArray(char [] destination) throws IbisIOException;
	public void readArray(short [] destination) throws IbisIOException;
	public void readArray(int [] destination) throws IbisIOException;
	public void readArray(long [] destination) throws IbisIOException;
	public void readArray(float [] destination) throws IbisIOException;
	public void readArray(double [] destination) throws IbisIOException;
	public void readArray(Object [] destination) throws IbisIOException;

	/** Read a slice of an array in place. No cycle checks are done. 
	    It is legal to use a readArray, with a corresponding writeArrayXXX.
	**/
	public void readArray(boolean [] destination, int offset, int size) throws IbisIOException;
	public void readArray(byte [] destination, int offset, int size) throws IbisIOException;
	public void readArray(char [] destination, int offset, int size) throws IbisIOException;
	public void readArray(short [] destination, int offset, int size) throws IbisIOException;
	public void readArray(int [] destination, int offset, int size) throws IbisIOException;
	public void readArray(long [] destination, int offset, int size) throws IbisIOException;
	public void readArray(float [] destination, int offset, int size) throws IbisIOException;
	public void readArray(double [] destination, int offset, int size) throws IbisIOException;
	public void readArray(Object [] destination, int offset, int size) throws IbisIOException;
} 
