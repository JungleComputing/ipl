/* $Id$ */

package ibis.ipl;

/**
 * Disconnect upcall interface for sendports. An Ibis implementation may
 * choose to block while processing this upcall.
 */
public interface SendPortDisconnectUpcall {
    /**
     * Upcall that indicates that a connection to a receiveport was lost.
     * If a {@link SendPort} has been configured with connection upcalls,
     * an upcall is generated for each connection that is lost.
     * A receiveport can forcibly close the connection,
     * in which case any communication from the sendport
     * will cause a lostConnection upcall. 
     * <strong>
     * The user may not assume that the mere fact that a
     * receive port forcibly closes its connections causes a lostConnection
     * call on the send port side.
     * The send port has to do communication to detect that there is trouble.
     * </strong>
     * <p>
     * This upcall may run completely asynchronously,
     * but only at most one is alive at any time.
     *
     * @param origin
     *          the {@link SendPort} losing a connection.
     * @param receiver
     *          identifier for the {@link ReceivePort} to which the connection
     *          is lost.
     * @param cause
     *          the reason for this upcall.
     */
    public void lostConnection(SendPort origin, ReceivePortIdentifier receiver,
            Throwable cause);
}
