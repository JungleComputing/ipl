package ibis.ipl;

import java.io.IOException;

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
	public void finish() throws IOException;

	public long sequenceNumber();
	public SendPortIdentifier origin();

	public boolean readBoolean() throws IOException;
	public byte readByte() throws IOException;
	public char readChar() throws IOException;
	public short readShort() throws IOException;
	public int readInt() throws IOException;
	public long readLong() throws IOException;	
	public float readFloat() throws IOException;
	public double readDouble() throws IOException;
	public String readString() throws IOException;
	/**
	 * Note: implementations should take care that an array, written with one
	 * of the writeArray variants, can be read with <code>readObject</code>.
	 *
	 * @exception ClassNotFoundException is thrown when an object arrives
	 *	of a class that cannot be loaded locally.
	 */
	public Object readObject() throws IOException, ClassNotFoundException;

	/** Methods to receive arrays in place. No duplicate checks are done.
	    These methods are a shortcut for:
	    readArray(destination, 0, destination.length);

	    It is therefore legal to use a readArrayXXX, with a corresponding writeArray.
	    @exception
		If an object of a different type than requested arrives, a
		ClassCastException is thrown.
	    @exception
		If an array of the correct type but of different size arrives,
		an ArrayIndexOutOfBoundsException is thrown.
	    @exception
		If the connection is broken, 
		a ConnectionClosedException is thrown.
	**/
	public void readArray(boolean [] destination) throws IOException;
	public void readArray(byte [] destination) throws IOException;
	public void readArray(char [] destination) throws IOException;
	public void readArray(short [] destination) throws IOException;
	public void readArray(int [] destination) throws IOException;
	public void readArray(long [] destination) throws IOException;
	public void readArray(float [] destination) throws IOException;
	public void readArray(double [] destination) throws IOException;
	/**
	 * @exception May throw a ClassNotFoundException if an object arrives
	 *	of a class that cannot be loaded locally
	 */
	public void readArray(Object [] destination) throws IOException, ClassNotFoundException;

	/** Read a slice of an array in place. No cycle checks are done. 
	    It is legal to use a readArray, with a corresponding writeArrayXXX.

	    @exception
		If an object of a different type than requested arrives, a
		ClassCastException is thrown.
	    @exception
		If an array of the correct type but of different size arrives,
		an ArrayIndexOutOfBoundsException is thrown.
	    @exception
		If the connection is broken, 
		a ConnectionClosedException is thrown.
	**/
	public void readArray(boolean [] destination, int offset, int size) throws IOException;
	public void readArray(byte [] destination, int offset, int size) throws IOException;
	public void readArray(char [] destination, int offset, int size) throws IOException;
	public void readArray(short [] destination, int offset, int size) throws IOException;
	public void readArray(int [] destination, int offset, int size) throws IOException;
	public void readArray(long [] destination, int offset, int size) throws IOException;
	public void readArray(float [] destination, int offset, int size) throws IOException;
	public void readArray(double [] destination, int offset, int size) throws IOException;
	public void readArray(Object [] destination, int offset, int size) throws IOException, ClassNotFoundException;
} 
