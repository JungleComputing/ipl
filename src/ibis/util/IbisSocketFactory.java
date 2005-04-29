/* $Id$ */

package ibis.util;

import ibis.connect.socketFactory.ConnectionPropertiesProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

/**
 * Abstract socket factory class for creating client and server sockets.
 * An implementation can be chosen by means of the
 * <code>ibis.util.socketfactory</code> system property. If not set,
 * a default implementation is chosen.
 */
public abstract class IbisSocketFactory {
    private static final String prefix = "ibis.util.socketfactory.";

    private static final String sf = prefix + "class";

    private static final String rng = prefix + "port.range";

    private static final String inbufsize = prefix + "InputBufferSize";

    private static final String outbufsize = prefix + "OutputBufferSize";

    private static final String[] sysprops = { sf, inbufsize,
            outbufsize, rng };

    protected static Logger logger
            = ibis.util.GetLogger.getLogger(IbisSocketFactory.class.getName());

    private static String DEFAULT_SOCKET_FACTORY
            = "ibis.impl.util.IbisConnectSocketFactory";

//    private static String DEFAULT_SOCKET_FACTORY
//            = "ibis.impl.util.IbisNormalSocketFactory";

    private static String socketFactoryName;

    static boolean firewall = false;

    static int portNr = 0;

    static int startRange = 0;

    static int endRange = 0;

    static int inputBufferSize = 0;

    static int outputBufferSize = 0;

    static {
        TypedProperties.checkProperties(prefix, sysprops, null);
        String sfClass = System.getProperty(sf);
        String range = System.getProperty(rng);
        if (sfClass != null) {
            socketFactoryName = sfClass;
        } else {
            socketFactoryName = DEFAULT_SOCKET_FACTORY;
        }
        if (range != null && range.length() != 0) {
            int pos = range.indexOf('-');
	    if(pos == -1) {
		pos = range.indexOf(',');
	    }
            if (pos < 0) {
                System.err.println("Port range format: 3000-4000 or 3000,4000. Ignoring");
//                System.exit(1);
            } else {
                String from = range.substring(0, pos);
                String to = range.substring(pos + 1, range.length());

                try {
                    startRange = Integer.parseInt(from);
                    endRange = Integer.parseInt(to);
                    firewall = true;
                    portNr = startRange;
		    logger.debug("IbisSocketFactory: using port range "
                            + startRange + " - " + endRange);
                } catch (Exception e) {
                    System.err.println("Port range format: 3000-4000 or 3000,4000. Ignoring");
//                    System.exit(1);
                }
            }
        } else {
//	    System.err.println("IbisSocketFactory: no port range");
	}
        inputBufferSize = TypedProperties.intProperty(inbufsize, 0);
        outputBufferSize = TypedProperties.intProperty(outbufsize, 0);
    }

    protected IbisSocketFactory() {
        /* do nothing */
    }

    /** 
     * Simple ServerSocket creator method.
     * Creates a server socket that will accept connections on the
     * specified port,
     * with the specified listen backlog, on the specified local address.
     * @param port the local TCP port
     * @param backlog the listen backlog
     * @param addr the local Inetaddress the server will bind to
     * @return the server socket created.
     * @exception IOException when the socket could not be created for some
     * reason.
     */
    public abstract ServerSocket createServerSocket(int port, int backlog,
            InetAddress addr) throws IOException;

    /**
     * Simple client Socket creator method.
     * Creates a client socket and connects it to the the specified Inetaddress
     * and port.
     * @param rAddr the IP address
     * @param rPort the port
     * @exception IOException when the socket could not be created for some
     * reason.
     */
    public abstract Socket createSocket(InetAddress rAddr, int rPort)
            throws IOException;

    /**
     * Returns a port number.
     * The system property <code>ibis.port.range</code> can be used to specify a
     * port range, for instance 3000-4000. This can be used to choose port
     * numbers that are for instance not protected by a firewall.
     * If such a range is not given, 0 is returned.
     * @return a port number, or 0, which means that any free port will do.
     */
    public synchronized int allocLocalPort() {
        if (firewall) {
            int res = portNr++;
            if (portNr >= endRange) {
                portNr = startRange;
                System.err.println("WARNING, used more ports than available "
                        + "within specified range. Wrapping around");
            }

            if (logger.isDebugEnabled()) {
                logger.debug("allocating local port in open range, returning: "
                        + res);
            }
            return res;
        }
        return 0; /* any free port */
    }

    /**
     * client Socket creator method with a timeout.
     * Creates a client socket and connects it to the the specified Inetaddress
     * and port. Some hosts have multiple local IP addresses. If the specified
     * <code>localIP</code> address is <code>null</code>, this method tries to
     * bind to the first of this machine's IP addresses. Otherwise, it uses the
     * specified address.
     * @param dest the IP address
     * @param port the port
     * @param localIP the local IP address, or <code>null</code>
     * @param timeoutMillis if < 0, throw exception on failure.
     * If 0, retry until success.
     * if > 0, block at most <code>timeoutMillis</code> milliseconds.
     * @exception IOException is thrown when the socket was not properly created
     * within this time.
     * @return the socket created.
     */
    public abstract Socket createSocket(InetAddress dest, int port,
            InetAddress localIP, long timeoutMillis) throws IOException;

    /** 
     * Simple ServerSocket creator method.
     * Creates a server socket that will accept connections on the specified
     * port, on the specified local address.
     * If the specified address is <code>null</code>,
     * the first of this machine's IP addresses is chosen.
     * @param port the local TCP port, or 0, in which case a free port is
     * chosen.
     * @param localAddress the local Inetaddress the server will bind to, or
     * <code>null</code>.
     * @param retry when <code>true</code>, the method blocks until the socket
     * is successfuly created.
     * @return the server socket created.
     * @exception IOException when the socket could not be created for some
     * reason.
     */
    public abstract ServerSocket createServerSocket(int port,
            InetAddress localAddress, boolean retry) throws IOException;

    /**
     * Accepts a connection to the specified server socket, and returns
     * the resulting socket.
     * @param a the server socket
     * @return the resulting socket
     * @exception IOException is thrown when the accept fails for some reason.
     */
    public Socket accept(ServerSocket a) throws IOException {
        Socket s;
        s = a.accept();
        tuneSocket(s);
        if (logger.isDebugEnabled()) {
            logger.debug("accepted new connection from "
                    + s.getInetAddress() + ":" + s.getPort() + ", local = "
                    + s.getLocalAddress() + ":" + s.getLocalPort());
        }

        return s;
    }

    /**
     * Creates a brokered socket, using the specified socket to negotiate.
     * The default implementation just returns this socket.
     * @param s socket to negotiate on
     * @param isServer must be set to true on one side, false on the other.
     * @param p connection properties, can be used to pass on socket-dependent
     *   parameters.
     * @return the socket created
     * @exception IOException is thrown when the socket could not
     *   be created for some reason.
     */
    public Socket createBrokeredSocket(Socket s, boolean isServer,
            ConnectionPropertiesProvider p) throws IOException {
        return s;
    }

    /**
     * Creates a brokered socket, using the specified streams to negotiate
     * and determine addresses.
     * The default implementation returns null.
     * @param in input stream
     * @param out output stream
     * @param isServer must be set to true on one side, false on the other.
     * @param p connection properties, can be used to pass on socket-dependent
     *   parameters.
     * @return the socket created
     * @exception IOException is thrown when the socket could not
     *   be created for some reason.
     */
    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean isServer, ConnectionPropertiesProvider p) throws IOException {
        return null;
    }

    /**
     * Closes a socket and streams that are associated with it.
     * These streams are given as separate parameters, because they may be
     * streams that are built on top of the actual socket streams.
     * @param in the inputstream ot be closed
     * @param out the outputstream to be closed
     * @param s the socket to be closed
     */
    public void close(InputStream in, OutputStream out, Socket s) {
        if (out != null) {
            try {
                out.flush();
            } catch (Exception e) {
                // ignore
            }
            try {
                out.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
                // ignore
            }
        }

        if (s != null) {
            try {
                s.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Hook for shutdown of socket factory. The default implementation does
     * nothing.
     */
    public void shutdown() {
        /* do nothing */
    }

    /**
     * Creates an <code>IbisSocketFactory</code>.
     * An implementation can be chosen by means of the
     * <code>ibis.socketfactory</code> system property. If not set,
     * a default implementation is chosen.
     * @return the created socket factory, or <code>null</code> if it
     * could not be found.
     */
    public static IbisSocketFactory createFactory() {
        try {
            Class classDefinition = Class.forName(socketFactoryName);
            return (IbisSocketFactory) classDefinition.newInstance();
        } catch (Exception e) {
            System.err.println("Could not create an instance of "
                    + socketFactoryName);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Configures a socket according to user-specified properties.
     * Currently, the input buffer size and output buffer size can
     * be set using the system properties
     * "ibis.util.socketfactory.InputBufferSize"
     * and "ibis.util.socketfactory.OutputBufferSize".
     * @param s the socket to be configured
     * @exception IOException when configuring fails for some reason.
     */
    public static void tuneSocket(Socket s) throws IOException {
        if (inputBufferSize != 0) {
            s.setReceiveBufferSize(inputBufferSize);
        }
        if (outputBufferSize != 0) {
            s.setSendBufferSize(outputBufferSize);
        }
        s.setTcpNoDelay(true);
    }
}
