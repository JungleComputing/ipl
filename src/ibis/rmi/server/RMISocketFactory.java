package ibis.rmi.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Ibis has no support for RMISocketFactories.
 */
public abstract class RMISocketFactory {

    /**
     * Constructs an <code>RMISocketFactory</code>.
     */
    public RMISocketFactory() {
        // nothing
    }

    /**
     * Creates a client socket connected to the specified host and port.
     * @param  host  the host name.
     * @param  port  the port number.
     * @return a socket connected to the specified host and port.
     * @exception IOException if an I/O error occurs during socket creation.
     */
    public abstract Socket createSocket(String host, int port)
            throws IOException;

    /**
     * Creates a server socket on the specified port.
     * @param  port the port number
     * @return the server socket on the specified port
     * @exception IOException if an I/O error occurs during server socket
     * creation
     */
    public abstract ServerSocket createServerSocket(int port)
            throws IOException;

    /**
     * Set the global socket factory from which RMI gets sockets (not supported
     * in Ibis RMI).
     * @param fac the socket factory.
     */
    public static void setSocketFactory(RMISocketFactory fac) {
        throw new IllegalArgumentException(
                "Ibis RMI does not support socket factories. "
                        + "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    /**
     * Gets the global socket factory from which RMI gets sockets (not supported
     * in Ibis RMI).
     * @return the socket factory.
     */
    public static RMISocketFactory getSocketFactory() {
        throw new IllegalArgumentException(
                "Ibis RMI does not support socket factories. "
                        + "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    /**
     * Returns a reference to the default socket factory used by this RMI
     * implementation. Not supported.
     * @return the default RMI socket factory.
     */
    public static RMISocketFactory getDefaultSocketFactory() {
        throw new IllegalArgumentException(
                "Ibis RMI does not support socket factories. "
                        + "Use the mechanisms of ibis.util.IbisSocketFactory");
    }

    /**
     * Sets the failure handler to be called by the RMI runtime if server socket
     * creation fails. Not supported in Ibis RMI.
     * @param fh the failure handler.
     */
    public static void setFailureHandler(RMIFailureHandler fh) {
        // not implemented
    }

    /**
     * Gets the failure handler that is called by the RMI runtime if server
     * socket creation fails. Not supported in Ibis RMI.
     * @return the failure handler.
     */
    public static RMIFailureHandler getFailureHandler() {
        return null;
    }
}
