package ibis.ipl;

public interface ReceivePort { 
	/** Only one message is alive at one time for a given receiveport. This is done to prevent flow control problems. 
	    A receiveport can be configured to generate upcalls or to support blocking receive, but NOT both!
	 **/

	/** explicit receive. When an receiveport is configured to generate upcalls, this is NOT allowed **/
	public ReadMessage receive() throws IbisIOException;

	/** Utility function that in essence performs:
	 *  <tt>
	 *  <br>if (finishMe \!= null) finishMe.finish();
	 *  <br>return receive();
	 *  </tt>
	 *  <p>
	 *  Rationale is the possibility to save on locking overhead by
	 *  combining an oft-recurring sequence.
	 **/
	public ReadMessage receive(ReadMessage finishMe) throws IbisIOException;

	/** Asynchronous receive. Return immediately when no message is available. 
	 Also works for upcalls, then it is a normal poll. **/
	public ReadMessage poll() throws IbisIOException;

	/** Asynchronous receive, as above, but free an old message.
	    Also works for upcalls, then it is a normal poll. **/
	public ReadMessage poll(ReadMessage finishMe) throws IbisIOException;

	public DynamicProperties properties();

	public ReceivePortIdentifier identifier();

	/** Start accepting new connections. When a ReceivePort is created it will not accept connections, 
	   until enableConnections() is invoked. This is done to avoid upcalls during initilization.
	   The implementation is free to postpone registration with the ReceivePortNameServer until
	   this call. **/
	public void enableConnections();

	/** Stop accepting new connections. It is allowed to invoke enableConnections() again after
	    disableConnections(). **/
	public void disableConnections();

	/** Allow upcalls to occur. This call is meaningless for ReceivePorts that were created
	    with a null Upcall parameter is PortType.createReceivePort. Upon startup, upcalls
	    are disabled. They must be explicitly enabled to receive upcalls. **/
	public void enableUpcalls();

	/** Disallow upcalls to occur.
	    After this call, no upcalls will occur until enableUpcalls() is called.
	    The disableUpcalls/enableUpcalls mechanism allows the user to selectively allow or disallow
	    upcalls during program run.
	    Remember that only one upcall can be active at the same time for each ReceivePort,
	    so the disableUpcalls/enableUpcalls mechanism is not necessary to enforce
	    serialization of Upcalls for this port. **/
	public void disableUpcalls();

	/** Free the resources held by the ReceivePort. **/
	public void free();
} 
