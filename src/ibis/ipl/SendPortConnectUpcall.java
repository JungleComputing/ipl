package ibis.ipl;

public interface SendPortConnectUpcall {

	/**
	   If a SendPort has been created with a ConnectUpcall
	   parameter, an upcall is generated for each connection that is lost.
	   This may be because the sender just closed the connection,
	   or it may be because there is some problem with the conenction itself.
	   <P>
	   This upcall may run completely asynchronously, but only at most one is alive at any time.
	*/
    public void lostConnection(ReceivePortIdentifier johnDoe, Exception reason);
}
