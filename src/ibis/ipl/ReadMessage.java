package ibis.ipl;

public interface ReadMessage { 
	/** Only one message is alive at one time for a given receiveport.
	    This is done to prevent flow control problems. 
	    when a message is alive, and a new messages is requested with a receive, the requester is blocked until the
	    live message is finished. **/
       	public void finish() throws IbisException;

	public long sequenceNumber();
	public SendPortIdentifier origin();

	public boolean readBoolean() throws IbisException;
	public byte readByte() throws IbisException;
	public char readChar() throws IbisException;
	public short readShort() throws IbisException;
	public int readInt() throws IbisException;
	public long readLong() throws IbisException;	
	public float readFloat() throws IbisException;
	public double readDouble() throws IbisException;
	public String readString() throws IbisException;
	public Object readObject() throws IbisException;

	public void readArrayBoolean(boolean [] destination) throws IbisException;
	public void readArrayByte(byte [] destination) throws IbisException;
	public void readArrayChar(char [] destination) throws IbisException;
	public void readArrayShort(short [] destination) throws IbisException;
	public void readArrayInt(int [] destination) throws IbisException;
	public void readArrayLong(long [] destination) throws IbisException;
	public void readArrayFloat(float [] destination) throws IbisException;
	public void readArrayDouble(double [] destination) throws IbisException;

	public void readSubArrayBoolean(boolean [] destination, int offset, int size) throws IbisException;
	public void readSubArrayByte(byte [] destination, int offset, int size) throws IbisException;
	public void readSubArrayChar(char [] destination, int offset, int size) throws IbisException;
	public void readSubArrayShort(short [] destination, int offset, int size) throws IbisException;
	public void readSubArrayInt(int [] destination, int offset, int size) throws IbisException;
	public void readSubArrayLong(long [] destination, int offset, int size) throws IbisException;
	public void readSubArrayFloat(float [] destination, int offset, int size) throws IbisException;
	public void readSubArrayDouble(double [] destination, int offset, int size) throws IbisException;
} 
