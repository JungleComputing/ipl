package ibis.ipl;

/**
 * Connection upcall interface for sendports. An Ibis implementation may
 * choose to block while processing this upcall.
 */
public interface SendPortConnectUpcall {
    /**
     * Upcall that indicates that a connection to a receiveport was lost.
     * If a {@link SendPort} has been configured with connection upcalls,
     * an upcall is generated for each connection that is lost.
     * This may be because the sender just closed the connection,
     * or it may be because there is some problem with the connection itself.
     * <P>
     * This upcall may run completely asynchronously,
     * but only at most one is alive at any time.
     *
     * @param me the {@link SendPort} losing a connection.
     * @param johnDoe identifier for the {@link ReceivePort} to which the
     *  connection is lost.
     * @param reason the reason for this upcall.
     */
    public void lostConnection(SendPort me,
			       ReceivePortIdentifier johnDoe,
			       Exception reason);
}
