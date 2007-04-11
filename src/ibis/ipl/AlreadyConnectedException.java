/* $Id$ */

package ibis.ipl;

/**
 * Signals that a connection has been refused, because it already exists.
 */
public class AlreadyConnectedException extends ConnectionFailedException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a <code>AlreadyConnectedException</code>.
     * with the specified parameters.
     * @param detailMessage    the detail message.
     * @param receivePortIdentifier
     *                 identifies the target port of the failed connection attempt.
     * @param cause     cause of the failure.
     */
    public AlreadyConnectedException(String detailMessage, ReceivePortIdentifier receivePortIdentifier,
            Throwable cause) {
        super(detailMessage, receivePortIdentifier, cause);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code>.
     * with the specified parameter.
     * @param detailMessage the detail message.
     * @param receivePortIdentifier
     *                 identifies the target port of the failed connection attempt.
     */
    public AlreadyConnectedException(String detailMessage, ReceivePortIdentifier receivePortIdentifier) {
        super(detailMessage, receivePortIdentifier);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code> with the
     * specified parameters.
     * @param detailMessage the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param receivePortName  the name of the receive port of the failed connection attempt.
     * @param cause     the cause of the failure.
     */
    public AlreadyConnectedException(String detailMessage, IbisIdentifier ibisIdentifier,
            String receivePortName, Throwable cause) {
        super(detailMessage, ibisIdentifier, receivePortName, cause);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code> with the
     * specified parameters.
     * @param detailMessage the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param receivePortName  the name of the receive port of the failed connection attempt.
     */
    public AlreadyConnectedException(String detailMessage, IbisIdentifier ibisIdentifier,
            String receivePortName) {
        super(detailMessage, ibisIdentifier, receivePortName);
    }
}
