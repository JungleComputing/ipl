package ibis.ipl;

import java.io.IOException;

public interface ReceivePort { 
	/**
	   A receiveport maintains connections to one or more send
	   ports.  The general contract is as follows.
	   
	   When creating a sendport, it is possible to pass a
	   ConnectUpcall object. Ibis will call the gotConnection upcall
	   of this object when a sendport tries to initiate a new 
	   connection to the receiveport.  When a connection is lost 
	   for some reason (normal close or link error), the 
	   lostConnection upcall is performed. Both upcalls
	   are completely asynchronous!
	   
	   If no ConnectUpcall is registered, the user is NOT informed
	   of connections that are created or lost.  No exceptions are thrown by
	   the read message when a connection is lost.  It is the user's own
	   responsibility to use the lostConnections() and closedConnections
	   methods to poll for connections that are lost.  The newConnections
	   method can be used to find out about new connections.

	   Only one upcall is alive at a one time, this includes BOTH
	   normal (message) upcalls AND ConnectUpcalls.

	   Only one message is alive at one time for a given
	   receiveport. This is done to prevent flow control problems.
	   A receiveport can be configured to generate upcalls or to 
	   support blocking receive, but NOT both!  The message object 
	   always is destroyed when the upcall is finished; it is thus 
	   not correct to put it in a global variable / queue.
	 **/

	/** explicit receive. When an receiveport is configured to generate upcalls, 
	    this is NOT allowed; in that case a PortConfigurationException is thrown **/
	public ReadMessage receive() throws IOException;

	/** Utility function.
	 *  In essence, it goes through the following steps:
	 *  <tt>
	 *  <br>if (finishMe \!= null) finishMe.finish();
	 *  <br>return receive();
	 *  </tt>
	 *  <p>
	 *  Rationale is the possibility to save on locking overhead by
	 *  combining an oft-recurring sequence.
	 **/
	public ReadMessage receive(ReadMessage finishMe) throws IOException;


	/** 
	    Explicit receive with timeout. When an receiveport is
	    configured to generate upcalls, this is NOT allowed The receive blocks
	    at most timeoutMillis, but it might be shorter!  The method returns
	    null when no message has been received.  A timeoutMillis <= 0 means
	    just do a blocking receive.
	 **/
	public ReadMessage receive(long timeoutMillis) throws IOException;

	/** Utility function.
	 * In essence, it goes through the following steps:
	 *  <tt>
	 *  <br>if (finishMe \!= null) finishMe.finish();
	 *  <br>return receive(timeoutMillis);
	 *  </tt>
	 *  <p>
	 *  Rationale is the possibility to save on locking overhead by
	 *  combining an oft-recurring sequence.
	 **/
	public ReadMessage receive(ReadMessage finishMe, long timeoutMillis) throws IOException;

	/** Asynchronous receive. Return immediately when no message is available. 
	    Also works for upcalls, then it is a normal poll: it will always return null,
	    but it might generate an upcall.
	**/
	public ReadMessage poll() throws IOException;

	/** Asynchronous receive, as above, but free an old message.
	    Also works for upcalls, then it is a normal poll: it will always return null,
	    but it might generate an upcall.
	**/
	public ReadMessage poll(ReadMessage finishMe) throws IOException;

	public DynamicProperties properties();

	public ReceivePortIdentifier identifier();

	/** Start accepting new connections. When a ReceivePort is created it will not accept connections, 
	   until enableConnections() is invoked. This is done to avoid upcalls during initilization.
	   The implementation is free to postpone registration with the ReceivePortNameServer until
	   this call. 
	   After this call is done, connection upcalls may be triggered.
	**/
	public void enableConnections();

	/** Stop accepting new connections. It is allowed to invoke enableConnections() again after
	    disableConnections(). 
	    After the disableConnections returns, no more Connection upcalls will be given.
	**/
	public void disableConnections();

	/** Allow upcalls to occur. This call is meaningless for ReceivePorts that were created
	    with a null Upcall parameter is PortType.createReceivePort. Upon startup, upcalls
	    are disabled. They must be explicitly enabled to receive upcalls. This also disables the
	    ConnectUpcalls. **/
	public void enableUpcalls();

	/** Disallow upcalls to occur.
	    After this call, no upcalls will occur until
	    enableUpcalls() is called.  The disableUpcalls/enableUpcalls mechanism
	    allows the user to selectively allow or disallow upcalls during
	    program run.  Remember that only one upcall can be active at the same
	    time for each ReceivePort, so the disableUpcalls/enableUpcalls
	    mechanism is not necessary to enforce serialization of Upcalls for
	    this port. This also enables the ConnectUpcalls.
	**/
	public void disableUpcalls();

	/** 
	    Free the resources held by the ReceivePort. 
	    Important: the free blocks until all sendports that are connected to it have been freed. 
	**/
	public void free() throws IOException;

	/** 
	    Free the resources held by the ReceivePort. 
	    Important: this call does not block until all sendports that are connected to it have been freed. 
	    Therefore, messages may be lost! Use this with extreme caution!
	    When this call are used, and this port is configured to keep connection administration
	    (lostConnections / newConnections upcalls and downcalls), this call updates the administration.
	    It may thus generate lostConnection upcalls.
	**/
	public void forcedClose() throws IOException;

	/**
	   Free the resources held by the ReceivePort, with timeout. 
	   Calls free, but blocks at most timeout milliseconds.
	   When the free did not succeed within the timeout, this operation does a forcedClose.
	   When this call are used, and this port is configured to keep connection administration
	   (lostConnections / newConnections upcalls and downcalls), this call updates the administration.
	   It may thus generate lostConnection upcalls.
	**/
	public void forcedClose(long timeoutMillis);

	/** returns the set of sendports this receiveport is connected to **/
	public SendPortIdentifier[] connectedTo();

	/** Poll to see whether any conncetion was lost due to an error,
	    or because the sender disconnected. 
	    Returns the changes since the last lostConnections call,
	    or, if this is the first call, all connectcions that were lost since
	    the port was created.
	    This call only works if the connectionAdministration parameter was true when this port was created.
	    Otherwise, null is returned.
	**/
	public SendPortIdentifier[] lostConnections();

	/** Poll to see whether there were any new connections 
	    accepted by this port. 
	    Returns the changes since the last newConnections call,
	    or, if this is the first call, all connectcions that were created since
	    the port was created.
	    This call only works if the connectionAdministration parameter was true when this port was created.
	    Otherwise, null is returned.
	**/
	public SendPortIdentifier[] newConnections();
} 
