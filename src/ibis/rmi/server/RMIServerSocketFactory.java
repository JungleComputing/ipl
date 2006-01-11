/* $Id$ */

package ibis.rmi.server;

/**
 * Ibis has no support for RMI socket factories.
 */
public interface RMIServerSocketFactory {

    /**
     * Creates a server socket on the specified port.
     * @param  port the port number
     * @return the server socket on the specified port
     * @exception IOException if an I/O error occurs
     */
    public java.net.ServerSocket createServerSocket(int port)
            throws java.io.IOException;
}
