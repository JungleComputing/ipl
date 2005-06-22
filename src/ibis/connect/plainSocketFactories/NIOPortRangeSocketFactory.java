/* $Id$ */

package ibis.connect.plainSocketFactories;


import ibis.connect.BrokeredSocketFactory;
import ibis.connect.ConnectionProperties;
import ibis.connect.IbisServerSocket;
import ibis.connect.IbisSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

// SocketType descriptor for PortRange sockets
// -------------------------------------------
public class NIOPortRangeSocketFactory extends BrokeredSocketFactory {
    static Logger logger = Logger.getLogger(NIOPortRangeSocketFactory.class
            .getName());

    private static int portNumber;

    private static int startRange;

    private static int endRange;

    private static NIOTCPSocketFactory nioSocketFactory;

    static {
        Properties p = System.getProperties();
        String range = p.getProperty(ConnectionProperties.PORT_RANGE);
        if (range != null) {
            try {
                int pos = range.indexOf('-');
                if (pos == -1) {
                    pos = range.indexOf(',');
                }
                String from = range.substring(0, pos);
                String to = range.substring(pos + 1, range.length());
                startRange = Integer.parseInt(from);
                endRange = Integer.parseInt(to);
                portNumber = startRange;
                logger.info("# PortRange: ports = " + startRange + "-"
                        + endRange);

                nioSocketFactory = new NIOTCPSocketFactory();
            } catch (Exception e) {
                throw new Error(
                        "# PortRange : specify a port range property: "
                                + "ibis.connect.PORT_RANGE=3000-4000 or ibis.connect.PORT_RANGE=3000,4000");
            }
        }
    }

    public NIOPortRangeSocketFactory() {
    }

    
    
    public IbisSocket createClientSocket(InetAddress destAddr, int destPort,
            InetAddress localAddr, int localPort, int timeout, Map properties)
            throws IOException {
        return nioSocketFactory.createClientSocket(destAddr, destPort, localAddr, localPort, timeout, properties);
    }
    
    public IbisSocket createClientSocket(InetAddress addr, int port, Map p)
            throws IOException {
        return nioSocketFactory.createClientSocket(addr, port, p);
    }

    public IbisServerSocket createServerSocket(InetSocketAddress addr, int backlog, Map p)
            throws IOException {
        return nioSocketFactory.createServerSocket(addr, backlog, p);
    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, Map p)
            throws IOException {
        return nioSocketFactory.createBrokeredSocket(in, out, hintIsServer, p);
    }

    private synchronized int allocLocalPort() {
        int res = portNumber++;
        if (portNumber >= endRange) {
            portNumber = startRange;
            System.err
                    .println("WARNING, used more ports than available within "
                            + "firewall range. Wrapping around");
        }
        return res;
    }
}