package ibis.ipl;

public interface ReadMessage { 
	/** Only one message is alive at one time for a given receiveport.
	    This is done to prevent flow control problems. 
	    when a message is alive, and a new messages is requested with a receive, the requester is blocked until the
	    live message is finished. **/
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
	public Object readObject() throws IbisIOException, ClassNotFoundException;

	public void readArrayBoolean(boolean [] destination) throws IbisIOException;
	public void readArrayByte(byte [] destination) throws IbisIOException;
	public void readArrayChar(char [] destination) throws IbisIOException;
	public void readArrayShort(short [] destination) throws IbisIOException;
	public void readArrayInt(int [] destination) throws IbisIOException;
	public void readArrayLong(long [] destination) throws IbisIOException;
	public void readArrayFloat(float [] destination) throws IbisIOException;
	public void readArrayDouble(double [] destination) throws IbisIOException;

	public void readSubArrayBoolean(boolean [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayByte(byte [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayChar(char [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayShort(short [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayInt(int [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayLong(long [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayFloat(float [] destination, int offset, int size) throws IbisIOException;
	public void readSubArrayDouble(double [] destination, int offset, int size) throws IbisIOException;
} 
