/* $Id$ */

package ibis.rmi.server;

/**
 * Ibis has no support for RMI socket factories.
 */
public interface RMIClientSocketFactory {

    /**
     * Creates a socket connected to the specified port on the specified host.
     * @param  port the port number
     * @param  host the host name
     * @return the socket
     * @exception IOException if an I/O error occurs
     */
    public java.net.Socket createSocket(String host, int port)
            throws java.io.IOException;
}
