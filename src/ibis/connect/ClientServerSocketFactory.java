/* $Id$ */

package ibis.connect;



import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

public abstract class ClientServerSocketFactory {
/*    public abstract IbisSocket createClientSocket(InetAddress destAddr, int destPort, 
            Map properties)
            throws IOException;
*/
    /** Creates a socket to a destination address.
     * @param destAddr destination IP
     * @param destPort destination port
     * @param localAddr local IP address, null for any
     * @param localPort local port, 0 for any
     * @param timeout the timout in milliseconds, 0 is infinite
     * @param properties Properties for the socket
     * @return
     * @throws IOException
     */
    public abstract IbisSocket createClientSocket(InetAddress destAddr, int destPort, 
            InetAddress localAddr, int localPort, int timeout, Map properties)
            throws IOException;
    
    public abstract IbisServerSocket createServerSocket(InetSocketAddress addr, int backlog, Map properties)
            throws IOException;
    
    /**
     * Configures a socket according to user-specified properties. Currently,
     * the input buffer size and output buffer size can be set using the system
     * properties "ibis.util.socketfactory.InputBufferSize" and
     * "ibis.util.socketfactory.OutputBufferSize".
     * 
     * @param s
     *            the socket to be configured
     * @exception IOException
     *                when configuring fails for some reason.
     */
    protected static void tuneSocket(Socket s) throws IOException {
        if (ConnectionProperties.inputBufferSize != 0) {
            s.setReceiveBufferSize(ConnectionProperties.inputBufferSize);
        }
        if (ConnectionProperties.outputBufferSize != 0) {
            s.setSendBufferSize(ConnectionProperties.outputBufferSize);
        }
        s.setTcpNoDelay(true);
    }
}