package ibis.ipl;

public interface ReceivePort { 
	/** Only one message is alive at one time for a given receiveport. This is done to prevent flow control problems. 
	    when a message is alive, and a new messages is requested with a receive, the requester is blocked until the
	    live message is finished. **/
	public ReadMessage receive() throws IbisException;

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
