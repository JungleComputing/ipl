package ibis.ipl;

/**
 * Connection upcall interface for receiveports.
 */
public interface ReceivePortConnectUpcall {
    /**
     * Upcall that indicates that a new connection is being initiated by
     * a {@link SendPort}.
     * If a {@link ReceivePort} has been configured with connection upcalls,
     * this upcall is generated for each attempt to set up a connection
     * with this {@link ReceivePort}.
     * This upcall should return true to accept the connection and false to
     * refuse the connection.
     * If the connection is refused, the connect call at the {@link SendPort} throws
     * a {@link ConnectionRefusedException}.
     * <P>
     * This upcall may run completely asynchronously, but only at most one is
     * alive at any time.
     *
     * @param me the {@link ReceivePort} receiving a connection attempt.
     * @param applicant identifier for the {@link SendPort} attempting to set up a
     * connection.
     */
    public boolean gotConnection(ReceivePort me, SendPortIdentifier applicant);

    /**
     * Upcall that indicates that a connection to a sendport was lost.
     * If a {@link ReceivePort} has been configured with connection upcalls,
     * an upcall is generated for each connection that is lost.
     * This may be because the sender just closed the connection,
     * or it may be because there is some problem with the connection itself.
     * <P>
     * This upcall may run completely asynchronously,
     * but only at most one is alive at any time.
     *
     * @param me the {@link ReceivePort} losing a connection.
     * @param johnDoe identifier for the {@link SendPort} to which the connection is lost.
     * @param reason the reason for this upcall.
     */
    public void lostConnection(ReceivePort me, SendPortIdentifier johnDoe, Exception reason);
}
