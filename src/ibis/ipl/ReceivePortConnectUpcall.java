package ibis.ipl;

public interface ReceivePortConnectUpcall {

	/**
	   If a ReceivePort has been created with a ConnectUpcall
	   parameter, an upcall is generated for each SendPort that
	   attempts a connection with this ReceivePort.
	   The upcall should return true to accept the connection
	   and false to refuse the connection. If the connection
	   is refused, the connect call at the SendPort throws
	   an IbisConnectionRefusedException.
	   <P>
	   This upcall may run completely asynchronously, but only at most one is alive at any time.
	*/
	public boolean gotConnection(SendPortIdentifier applicant);
	
	
	/**
	   If a ReceivePort has been created with a ConnectUpcall
	   parameter, an upcall is generated for each connection that is lost.
	   This may be because the sender just closed the connection,
	   or it may be because there is some problem with the conenction itself.
	   <P>
	   This upcall may run completely asynchronously, but only at most one is alive at any time.
	*/
	public void lostConnection(SendPortIdentifier johnDoe);
}
