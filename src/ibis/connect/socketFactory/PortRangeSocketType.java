/* $Id$ */

package ibis.connect.socketFactory;

import ibis.connect.util.ConnectionProperties;
import ibis.connect.util.MyDebug;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

// SocketType descriptor for PortRange sockets
// -------------------------------------------
public class PortRangeSocketType extends SocketType implements
        ClientServerSocketFactory, BrokeredSocketFactory {
    private static int portNumber;

    private static int startRange;

    private static int endRange;

    static {
        Properties p = System.getProperties();
        String range = p.getProperty(ConnectionProperties.port_range);
        if (range != null) {
            try {
                int pos = range.indexOf('-');
                if(pos == -1) {
                	pos = range.indexOf(',');
                }
                String from = range.substring(0, pos);
                String to = range.substring(pos + 1, range.length());
                startRange = Integer.parseInt(from);
                endRange = Integer.parseInt(to);
                portNumber = startRange;
                MyDebug.trace("# PortRange: ports = " + startRange + "-"
                        + endRange);
            } catch (Exception e) {
                throw new Error(
                        "# PortRange : specify a port range property: "
                        + "ibis.connect.port_range=3000-4000 or ibis.connect.port_range=3000,4000");
            }
        }
    }

    public PortRangeSocketType() {
        super("PortRange");
    }

    public Socket createClientSocket(InetAddress addr, int port)
            throws IOException {
        Socket s = new Socket(addr, port);
        return s;
    }

    public ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
            throws IOException {
        ServerSocket s = new ServerSocket();
        s.setReceiveBufferSize(0x10000);
        if (addr.getPort() == 0) {
            addr = new InetSocketAddress(addr.getAddress(), allocLocalPort());
        }
        s.bind(addr, backlog);
        return s;
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, ConnectionPropertiesProvider p) throws IOException {
        return ExtSocketFactory.createBrokeredSocketFromClientServer(this, in,
                out, hintIsServer, p);
    }

    private synchronized int allocLocalPort() {
        int res = portNumber++;
        if (portNumber >= endRange) {
            portNumber = startRange;
            System.err.println("WARNING, used more ports than available within "
                    + "firewall range. Wrapping around");
        }
        return res;
    }
}
