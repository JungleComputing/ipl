/* $Id$ */

package ibis.connect;

import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

/**
 * A generalized SocketFactory which supports the normal client/server connection
 * scheme and brokered connections through a control link.
 */
public class IbisSocketFactory {
    private static IbisSocketFactory staticFactory;

    static Logger logger = ibis.util.GetLogger.getLogger(IbisSocketFactory.class.getName());

    /**
     * Map which converts a SocketType nicknames into the class name which
     * implements it.
     */
    private static Hashtable nicknames = new Hashtable();

    private static final String DEFAULT_CLIENT_SERVER = "PlainTCP";

    private static final String DEFAULT_BROKERED = "AnyTCP";

    private static ClientServerSocketFactory clientServerSocketFactory = null;

    private static BrokeredSocketFactory brokeredSocketFactory = null;

    private static BrokeredSocketFactory parallelBaseSocketFactory = null;

    protected IbisSocketFactory() {
        logger.info("IbisSocketFactory: starting configuration.");

        // init types table
        declareNickname("PlainTCP",
                "ibis.connect.plainSocketFactories.PlainTCPSocketFactory");
        declareNickname("NIOTCP",
                "ibis.connect.plainSocketFactories.NIOTCPSocketFactory");
        declareNickname("TCPSplice",
                "ibis.connect.tcpSplicing.TCPSpliceSocketFactory");
        declareNickname("RoutedMessages",
                "ibis.connect.routedMessages.RoutedMessagesSocketFactory");
        declareNickname("ParallelStreams",
                "ibis.connect.parallelStreams.ParallelStreamsSocketFactory");
        declareNickname("NIOParallelStreams",
                "ibis.connect.NIOParallelStreams.NIOParallelStreamsSocketFactory");
        declareNickname("PortRange",
                "ibis.connect.plainSocketFactories.PortRangeSocketFactory");
        declareNickname("SSL", "ibis.connect.plainSocketFactories.SSLSocketType");
        declareNickname("AnyTCP",
                "ibis.connect.plainSocketFactories.AnyBrokeredTCPSocketFactory");
        // Declare new nicknames here.

        String dataLinksProperty = TypedProperties
                .stringProperty(ConnectionProperties.DATA_LINKS);
        if (dataLinksProperty == null) {
            dataLinksProperty = DEFAULT_BROKERED;
        }

        String controlLinksProperty = TypedProperties
                .stringProperty(ConnectionProperties.CONTROL_LINKS);
        if (controlLinksProperty == null) {
            controlLinksProperty = DEFAULT_CLIENT_SERVER;
        }

        String parallelBaseType = null;

        StringTokenizer st = new StringTokenizer(dataLinksProperty,
                " ,\t\n\r\f");
        String s = st.nextToken();
        if (s.equalsIgnoreCase("ParallelStreams")
                || s.equalsIgnoreCase("NIOParallelStreams")) {
            // ParallelStreams has an underlying brokered socket type.
            String tmp = s;
            parallelBaseType = st.nextToken();
            if (parallelBaseType == null) {
                if (tmp.equalsIgnoreCase("ParallelStreams")) {
                    parallelBaseType = "PlainTCP";
                } else {
                    parallelBaseType = "NIOTCP";
                }
            }
            dataLinksProperty = tmp;
        }

        brokeredSocketFactory = loadBrokeredSocketType(dataLinksProperty);
        if (brokeredSocketFactory == null) {
            throw new Error("no brokered socket type found!");
        }

        if (parallelBaseType != null) {
            parallelBaseSocketFactory = loadBrokeredSocketType(parallelBaseType);
            if (parallelBaseSocketFactory == null) {
                throw new Error(
                        "no brokered socket type for parallel streams found!");
            }
        }

        clientServerSocketFactory = loadSocketType(controlLinksProperty);
        if (clientServerSocketFactory == null) {
            throw new Error("no client-server socket type found!");
        }

        logger.info("client-server links: "
                + clientServerSocketFactory.getClass());
        logger.info("brokered links: " + brokeredSocketFactory.getClass());
        if (parallelBaseType != null) {
            logger.info("base links for parallel streams: "
                    + parallelBaseSocketFactory.getClass());
        }
    }

    public static IbisSocketFactory getFactory() {
        if (staticFactory == null) {
            staticFactory = new IbisSocketFactory();
        }
        return staticFactory;
    }

    private void declareNickname(String nickname, String className) {
        nicknames.put(nickname.toLowerCase(), className);
    }

    /*
     * loads a SocketType into the factory name: a nickname from the 'nicknames'
     * hashtable, or a fully-qualified class name which extends SocketType
     */
    private synchronized ClientServerSocketFactory loadSocketType(
            String socketType) {
        ClientServerSocketFactory t = null;
        String className = (String) nicknames.get(socketType.toLowerCase());
        Class c = null;
        if (className == null) {
            className = socketType;
        }
        try {
            c = Class.forName(className);
        } catch (Exception e) {
            logger.error("IbisSocketFactory: socket type " + socketType
                    + " not found.");
            logger.error("  known types are:");
            Enumeration i = nicknames.keys();
            while (i.hasMoreElements()) {
                logger.error((String) i.nextElement());
            }
            throw new Error("IbisSocketFactory: class not found: " + className,
                    e);
        }
        try {
            t = (ClientServerSocketFactory) c.newInstance();
            logger.info("Registering socket type: " + t.getClass());
            logger.info("  class name: " + t.getClass().getName());
        } catch (Exception e) {
            logger.error("IbisSocketFactory: Socket type constructor "
                    + className + " got exception:", e);
            logger.error("IbisSocketFactory: loadSocketType returns null");
        }
        return t;
    }

    private synchronized BrokeredSocketFactory loadBrokeredSocketType(
            String socketType) {
        BrokeredSocketFactory f = null;
        try {
            f = (BrokeredSocketFactory) loadSocketType(socketType);
        } catch (Exception e) {
            logger.info("SocketFactory: SocketType " + f.getClass()
                    + " does not support brokered connection "
                    + "establishment.");
            throw new Error(e);
        }

        return f;
    }

    public BrokeredSocketFactory getBrokeredType() {
        return brokeredSocketFactory;
    }

    public BrokeredSocketFactory getParallelStreamsBaseType() {
        return parallelBaseSocketFactory;
    }

    // Bootstrap client sockets: Socket(addr, port);
    public IbisSocket createClientSocket(InetAddress addr, int port,
            Map properties) throws IOException {
        logger.info("create client socket: " + addr + " type = "
                + clientServerSocketFactory);
        return createClientSocket(addr, port, null, 0, 0, properties);
    }

    /**
     * client Socket creator method with a timeout. Creates a client socket and
     * connects it to the the specified Inetaddress and port. Some hosts have
     * multiple local IP addresses. If the specified <code>localIP</code>
     * address is <code>null</code>, this method tries to bind to the first
     * of this machine's IP addresses. Otherwise, it uses the specified address.
     * 
     * @param dest
     *            the IP address
     * @param port
     *            the port
     * @param localIP
     *            the local IP address, or <code>null</code>
     * @param localPort
     *             the local port to bind to, 0 means any port
     * @param timeoutMillis
     *            if < 0, throw exception on failure. If 0, retry until success.
     *            if > 0, block at most <code>timeoutMillis</code>
     *            milliseconds.
     * @exception IOException
     *                is thrown when the socket was not properly created within
     *                this time.
     * @return the socket created.
     */
    public IbisSocket createClientSocket(InetAddress dest, int port,
            InetAddress localIP, int localPort, int timeoutMillis,
            Map properties) throws IOException {
        logger.info("create client socket: " + dest + " type = "
                + clientServerSocketFactory);

        if (timeoutMillis == 0) {
            while (true) {
                try {
                    return clientServerSocketFactory
                            .createClientSocket(dest, port, localIP, localPort,
                                    timeoutMillis, properties);
                } catch (Exception e) {
                    logger.info(e);
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // ignore 

                }
            }
        } else if (timeoutMillis < 0) {
            return clientServerSocketFactory.createClientSocket(dest, port,
                    localIP, localPort, 0, properties);
        }

        return clientServerSocketFactory.createClientSocket(dest, port,
                localIP, localPort, timeoutMillis, properties);
    }

    public IbisServerSocket createServerSocket(int port, int backlog,
            InetAddress addr, Map properties) throws IOException {
        return clientServerSocketFactory.createServerSocket(
                new InetSocketAddress(addr, port), backlog, properties);
    }

    public IbisServerSocket createServerSocket(int port,
            InetAddress localAddress, boolean retry, Map properties)
            throws IOException {
        return createServerSocket(port, localAddress, 50, retry, properties);
    }

    /**
     * Simple ServerSocket creator method. Creates a server socket that will
     * accept connections on the specified port, on the specified local address.
     * If the specified address is <code>null</code>, the first of this
     * machine's IP addresses is chosen.
     * 
     * @param port
     *            the local TCP port, or 0, in which case a free port is chosen.
     * @param localAddress
     *            the local Inetaddress the server will bind to, or
     *            <code>null</code>.
     * @param retry
     *            when <code>true</code>, the method blocks until the socket
     *            is successfuly created.
     * @return the server socket created.
     * @exception IOException
     *                when the socket could not be created for some reason.
     */
    public IbisServerSocket createServerSocket(int port,
            InetAddress localAddress, int backlog, boolean retry, Map properties)
            throws IOException {
        boolean connected = false;
        IbisServerSocket s = null;
        int localPort;

        while (!connected) {
            try {
                logger.info("Creating new ServerSocket on " + localAddress
                        + ":" + port);

                s = createServerSocket(port, backlog, localAddress, properties);

                logger.info("DONE, with port = " + s.getLocalPort());
                connected = true;
            } catch (IOException e1) {
                if (!retry) {
                    throw e1;
                }
                logger.info("ServerSocket connect to " + port + " failed: "
                        + e1 + "; retrying");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    // don't care
                }
            }
        }

        return s;
    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, Map properties) throws IOException {
        logger.info("SocketFactory: creating brokered socket- hint="
                + hintIsServer + "; type=" + brokeredSocketFactory.getClass());
        return brokeredSocketFactory.createBrokeredSocket(in, out,
                hintIsServer, properties);
    }
}