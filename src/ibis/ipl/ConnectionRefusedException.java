/* $Id$ */

package ibis.ipl;

/**
 * Signals that a connection has been refused. A
 * <code>ConnectionRefusedException</code> is thrown to indicate
 * that a sendport connect was refused.
 */
public class ConnectionRefusedException extends ConnectionFailedException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>ConnectionRefusedException</code>.
     * with the specified parameters.
     * @param detail    the detail message.
     * @param receivePort
     *                 identifies the target port of the failed connection attempt.
     * @param cause     cause of the failure.
     */
    public ConnectionRefusedException(String detail, ReceivePortIdentifier receivePort,
            Throwable cause) {
        super(detail, receivePort, cause);
    }
    
    /**
     * Constructs a <code>ConnectionRefusedException</code>.
     * with the specified parameter.
     * @param detail the detail message.
     * @param receivePort
     *                 identifies the target port of the failed connection attempt.
     */
    public ConnectionRefusedException(String detail, ReceivePortIdentifier receivePort) {
        super(detail, receivePort);
    }
    
    /**
     * Constructs a <code>ConnectionRefusedException</code> with the
     * specified parameters.
     * @param detail the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param portName  the name of the receive port of the failed connection attempt.
     * @param cause     the cause of the failure.
     */
    public ConnectionRefusedException(String detail, IbisIdentifier ibisIdentifier,
            String portName, Throwable cause) {
        super(detail, ibisIdentifier, portName, cause);
    }
    
    /**
     * Constructs a <code>ConnectionRefusedException</code> with the
     * specified parameters.
     * @param detail the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param portName  the name of the receive port of the failed connection attempt.
     */
    public ConnectionRefusedException(String detail, IbisIdentifier ibisIdentifier,
            String portName) {
        super(detail, ibisIdentifier, portName);
    }
}
