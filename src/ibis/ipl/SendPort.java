package ibis.ipl;

import java.io.IOException;

         /**
	    Sendports maintain connections to one or more receive ports.
	    The general contract is as follows.
	    Connections are unrelated to messages! If the sending of a message 
	    did not generate an exception, this does not mean that it actually 
	    arrived at the receive port. There may still be data in Ibis or 
	    operating system buffers, or in the network itself. 
	    
	    When creating a sendport, it is possible to pass a ConnectUpcall 
	    object. Ibis will call the gotConnection upcall of this object 
	    when a sendport tries to initiate a new connection.
	    When a connection is lost for some reason (normal close or 
	    link error), the lostConnection ore closedConnection upcalls are performed. Both 
	    upcalls are completely asynchronous, but Ibis ensures that 
	    only one ConnectUpcall is alive at any given time.
	    
	    If no ConnectUpcall is registered, the user is NOT informed 
	    of connections that are lost. No exceptions are thrown by 
	    the write message. It is then the user's own responisbility 
	    to use the lostConnections() method to poll for connections 
	    that are lost.
         **/
public interface SendPort {        

	/**
	   Request a new message from this sendport.
	   Only one message is alive at one time for a given
	   sendport. This is done to prevent flow control problems.  When a
	   message is alive, and a new message is requested, the request is
	   blocked until the live message is finished.
	   It is allowed to get a message for a sendport that is not connected.
	   All data that is written into the message is then silently discarded.

	   @return a <code>WriteMessage</code>.
	   @exception IOException may be thrown when something goes wrong.
	**/
	public WriteMessage newMessage() throws IOException;

	/**
	   Returns the {@link DynamicProperties} of this ibis implementation.
	   What is the idea? Currently there are no Ibis implementations
	   that implement this! ????
	**/
	public DynamicProperties properties();

	/**
	   Obtain an identification for this sendport.
	   @return the identification.
	**/
	public SendPortIdentifier identifier();

	public String name();

	/**
	 * Returns the number of bytes that was written to this sendport.
	 * @return the number of bytes written.
	 **/
	public long getCount();

	/**
	 * Resets the counter for the number of bytes that was written to this sendport
	 * to zero.
	 **/
	public void resetCount();

	/**
	   Attempt a connection with receiver.
	   @exception ConnectionRefusedException is thrown
	   if the receiver denies the connection, or if the port was already
	   connected to the receiver.
	   Multiple connections to the same receiver are NOT allowed.
	   @exception PortConfigurationException is thrown if this receive
	   port and the send port are of different types.
	   @param receiver identifies the <code>ReceivePort</code> to connect to
	*/
	public void connect(ReceivePortIdentifier receiver) throws IOException;

	/**
	   Attempt a connection with receiver.
	   @exception ibis.ipl.ConnectionTimedOutException is thrown
	   if an accept/deny has not arrived within <code>timeout_millis</code>.
	   A value of 0 for <code>timeout_millis</code> signifies no
	   timeout on the connection attempt.
	   @exception ConnectionRefusedException is thrown
	   if the receiver denies the connection.
	   Multiple connections to the same receiver are NOT allowed.
	   @exception PortConfigurationException is thrown if this receive
	   port and the send port are of different types.
	   @param receiver identifies the <code>ReceivePort</code> to connect to
	   @param timeoutMillis timeout in milliseconds
	*/
	public void connect(ReceivePortIdentifier receiver, long timeoutMillis) throws IOException;

	/** Free the resources held by the SendPort.
	    If a free is attempted when a message is still alive, an exception will be thrown. 
	    Even if this call throws an exception, the connection cannot be
	    used anymore.
	   @exception IOException in case of trouble.
	**/
	public void free() throws IOException;

	/** 
	   Returns the set of receiveports this sendport is connected to.
	   @return a set of receiveport identifiers.
	**/
	public ReceivePortIdentifier[] connectedTo();

	/** 
	   Poll to find out whether any connections are lost or closed.
	   Returns the changes since the last lostConnections call,
	   or, if this is the first call, all connectcions that were lost since
	   the port was created.
	   This call only works if the connectionAdministration parameter was true when this port was created.
	   Otherwise, null is returned.
	   @return a set of receiveport identifiers to which the connection
	   is lost.
	**/
	public ReceivePortIdentifier[] lostConnections();
}
