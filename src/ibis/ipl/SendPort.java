package ibis.ipl;

import java.io.IOException;

public interface SendPort {        
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

	/**
	   Only one message is alive at one time for a given
	   sendport. This is done to prevent flow control problems.  when a
	   message is alive, and a new messages is requested, the requester is
	   blocked until the live message is finished.
	   It is allowed to get a message for a sendport that is not connected.
	   All data that is written into the message is then silently discarded.
	**/
	public WriteMessage newMessage() throws IOException;

	public DynamicProperties properties();

	public SendPortIdentifier identifier();

	/**
	   Attempt a connection with receiver.
	   @exception ConnectionRefusedException is thrown
	   if the receiver denies the connection.
	   Multiple connections to the same receiver are NOT allowed.
	   @exception PortConfigurationException is thrown if this receive
	   port and the send port are of different types.
	*/
	public void connect(ReceivePortIdentifier receiver) throws IOException;

	/**
	   Attempt a connection with receiver.
	   @exception ibis.ipl.ConnectionTimedOutException is thrown
	   if an accept/deny has not arrived within timeout_millis.
	   A value timeout_millis of 0 signifies no timeout on the connection
	   attempt.
	   @exception ConnectionRefusedException is thrown
	   if the receiver denies the connection.
	   Multiple connections to the same receiver are NOT allowed.
	   @exception PortConfigurationException is thrown if this receive
	   port and the send port are of different types.
	*/
	public void connect(ReceivePortIdentifier receiver, int timeout_millis) throws IOException;

	/** Free the resources held by the SendPort. **/
	public void free() throws IOException;

	/** Returns the set of receiveports this sendport is connected to. **/
	public ReceivePortIdentifier[] connectedTo();

	/** Poll to find out whether any connections are lost or closed. **/
	public ReceivePortIdentifier[] lostConnections();
}
