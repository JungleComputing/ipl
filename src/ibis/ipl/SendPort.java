package ibis.ipl;

public interface SendPort {        
	/** Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	 when a message is alive, and a new messages is requested, the requester is blocked until the
	live message is finished. **/
	public WriteMessage newMessage() throws IbisException;

	public DynamicProperties properties();

	public SendPortIdentifier identifier();

	/**
	    Attempt a connection with receiver.
	    If receiver denies the connection, an
	    IbisConnectionRefusedException is thrown.
	 */
	public void connect(ReceivePortIdentifier receiver) throws IbisException;

	/**
	    Attempt a connection with receiver.
	    If receiver denies the connection, an
	    IbisConnectionRefusedException is thrown.
	    If an accept/deny has not arrived within timeout_millis, an
	    IbisConnectionTimedOutException is thrown.
	    A value timeout_millis of 0 signifies no timeout on the connection
	    attempt.
	 */
	public void connect(ReceivePortIdentifier receiver, int timeout_millis) throws IbisException;

	/** Free the resources held by the SendPort. **/
	public void free();

} 

