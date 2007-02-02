/* $Id:$ */

package ibis.impl;

/**
 * Abstract class for implementation-dependent connection info for sendports.
 */
public abstract class SendPortConnectionInfo {
    /** Identifies the receiveport side of the connection. */
    protected final ReceivePortIdentifier target;

    /** The sendport of the connection. */
    protected SendPort port;

    /**
     * Constructs a <code>SendPortConnectionInfo</code> with the specified parameters.
     * @param port the sendport.
     * @param target identifies the receiveport.
     */
    protected SendPortConnectionInfo(SendPort port, ReceivePortIdentifier target) {
        this.port = port;
        this.target = target;
    }

    /**
     * Should close this particular connection.
     */
    protected abstract void closeConnection();
}
