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
     * @param detail    the detail message.
     * @param receivePort
     *                 identifies the target port of the failed connection attempt.
     * @param cause     cause of the failure.
     */
    public AlreadyConnectedException(String detail, ReceivePortIdentifier receivePort,
            Throwable cause) {
        super(detail, receivePort, cause);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code>.
     * with the specified parameter.
     * @param detail the detail message.
     * @param receivePort
     *                 identifies the target port of the failed connection attempt.
     */
    public AlreadyConnectedException(String detail, ReceivePortIdentifier receivePort) {
        super(detail, receivePort);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code> with the
     * specified parameters.
     * @param detail the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param portName  the name of the receive port of the failed connection attempt.
     * @param cause     the cause of the failure.
     */
    public AlreadyConnectedException(String detail, IbisIdentifier ibisIdentifier,
            String portName, Throwable cause) {
        super(detail, ibisIdentifier, portName, cause);
    }
    
    /**
     * Constructs a <code>AlreadyConnectedException</code> with the
     * specified parameters.
     * @param detail the detail message.
     * @param ibisIdentifier 
     *                 identifies the Ibis instance of the target port of
     *                 the failed connection attempt.
     * @param portName  the name of the receive port of the failed connection attempt.
     */
    public AlreadyConnectedException(String detail, IbisIdentifier ibisIdentifier,
            String portName) {
        super(detail, ibisIdentifier, portName);
    }
}
