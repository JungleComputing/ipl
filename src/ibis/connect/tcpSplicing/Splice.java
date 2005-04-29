/* $Id$ */

package ibis.connect.tcpSplicing;

import ibis.connect.util.ConnectionProperties;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;

public class Splice {
    static Logger logger = ibis.util.GetLogger.getLogger(Splice.class.getName());

    static int serverPort = TypedProperties.intProperty(ConnectionProperties.splice_port,
            20246);

    static int hintPort = serverPort + 1;

    private static final int defaultSendBufferSize = 64 * 1024;

    private static final int defaultRecvBufferSize = 64 * 1024;

    private static NumServer server;

    private static final boolean setBufferSizes
            = !TypedProperties.booleanProperty(ConnectionProperties.sizes);

    private Socket socket = null;

    private String localHost = null;

    private int localPort = -1;

    private InetSocketAddress localAddr;

    /**
     * A little server that takes care of assigning port numbers.
     * Such a server is needed when more than one JVM is running on the
     * same host and trying to set up TCPSplice connections. When this
     * is the case, there is a race when the same range of port numbers is used.
     * See the comment in the connectSplice routine.
     * The NumServer solves that.
     */
    private static class NumServer extends Thread {
        ServerSocket srvr;

        NumServer() {
            try {
                srvr = new ServerSocket(serverPort);
                logger.debug("# Splice: numserver created");
                Runtime.getRuntime().addShutdownHook(
                        new Thread("NumServer killer") {
                            public void run() {
                                try {
                                    srvr.close();
                                    srvr = null;
                                } catch (Exception e) {
                                    // ignored
                                }
                            }
                        });
            } catch (Exception e) {
                // Assumption here is that another JVM has created this server.
                // System.out.println("Could not create server socket");
                // e.printStackTrace();
                srvr = null;
                logger.debug("# Splice: numserver refused");
            }
        }

        public void run() {
            while (true) {
                if (srvr == null) {
                    return;
                }
                try {
                    Socket s = srvr.accept();
                    DataOutputStream out = new DataOutputStream(
                            new BufferedOutputStream(s.getOutputStream()));
                    out.writeInt(hintPort++);
                    out.flush();
                    out.close();
                    s.close();
                } catch (Exception e) {
                    logger.debug("# Splice: numserver got " + e);
                }
            }
        }
    }

    static {
        server = new NumServer();
        server.setDaemon(true);
        server.start();
    }

    public Splice() throws IOException {
        socket = new Socket();
        if (setBufferSizes) {
            socket.setSendBufferSize(defaultSendBufferSize);
            socket.setReceiveBufferSize(defaultRecvBufferSize);
        }
        try {
            localHost = IPUtils.getLocalHostAddress().getCanonicalHostName();
        } catch (Exception e) {
            localHost = "";
        }
    }

    public String getLocalHost() {
        return localHost;
    }

    private int newPort() {
        try {
            Socket s = new Socket(IPUtils.getLocalHostAddress(), serverPort);
            DataInputStream in = new DataInputStream(
                    new BufferedInputStream(s.getInputStream()));
            int port = in.readInt();
            in.close();
            s.close();
            return port;
        } catch (Exception e) {
            System.err.println("Could not contact port number server: " + e);
            e.printStackTrace();
        }
        return hintPort++;
    }

    public int findPort() {
        int port;
        do {
            port = newPort();
            try {
                localAddr = new InetSocketAddress(localHost, port);
            } catch (Exception e) {
                throw new Error(e);
            }
            try {
                logger.debug("# Splice: trying port " + port);
                socket.bind(localAddr);
                localPort = port;
            } catch (IOException e) {
                localPort = -1;
            }
        } while (localPort == -1);
        logger.debug("# Splice: found port " + localPort);
        return localPort;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException dummy) {
            // ignored
        }
    }

    public Socket connectSplice(String rHost, int rPort) {
        int i = 0;
        boolean connected = false;

        logger.debug("# Splice: connecting to: " + rHost + ":" + rPort);
        while (!connected) {
            try {
                InetSocketAddress remoteAddr = new InetSocketAddress(rHost,
                        rPort);
                socket.connect(remoteAddr);
                connected = true;
                logger.debug("# Splice: success! i=" + i);
                logger.debug("# Splice:   tcpSendBuffer="
                        + socket.getSendBufferSize() + "; tcpReceiveBuffer="
                        + socket.getReceiveBufferSize());
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException dummy) { /*ignore */
                }
                // There is a race here, if two JVM's running on the
                // same node are both creating spliced sockets.
                // After this close, another JVM might take this
                // localAddr, and then the bind fails. (Ceriel)
                // There is no race when using the NumServer. (Ceriel)
                i++;
                // re-init the socket
                try {
                    socket = new Socket();
                    socket.setReuseAddress(true);
                    if (setBufferSizes) {
                        socket.setSendBufferSize(defaultSendBufferSize);
                        socket.setReceiveBufferSize(defaultRecvBufferSize);
                    }
                    socket.bind(localAddr);
                } catch (IOException f) {
                    throw new Error(f);
                }
            }
        }
        return socket;
    }
}

