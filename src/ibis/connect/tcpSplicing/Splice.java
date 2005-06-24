/* $Id$ */

package ibis.connect.tcpSplicing;

import ibis.connect.ConnectionProperties;
import ibis.connect.IbisSocket;
import ibis.connect.plainSocketFactories.PlainTCPSocket;
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
import java.util.Map;

import org.apache.log4j.Logger;

public class Splice {
    static Logger logger = ibis.util.GetLogger.getLogger(Splice.class.getName());

    static int serverPort = TypedProperties.intProperty(
            ConnectionProperties.SPLICE_PORT, 20246);

    static int hintPort = serverPort + 1;

    private static NumServer server;

    private IbisSocket socket = null;

    private String localHost = null;

    private int localPort = -1;

    private InetSocketAddress localAddr;

    private Map p;
    
    /**
     * A little server that takes care of assigning port numbers. Such a server
     * is needed when more than one JVM is running on the same host and trying
     * to set up TCPSplice connections. When this is the case, there is a race
     * when the same range of port numbers is used. See the comment in the
     * connectSplice routine. The NumServer solves that.
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

    public Splice(Map p) throws IOException {
        this.p = p;
        socket = new PlainTCPSocket(p);
        socket.setSendBufferSize(ConnectionProperties.outputBufferSize);
        socket.setReceiveBufferSize(ConnectionProperties.inputBufferSize);
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
            DataInputStream in = new DataInputStream(new BufferedInputStream(s
                    .getInputStream()));
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

    /** this returns null after a minute. If it takes longer, splicaing makes no sense. **/
    IbisSocket connectSplice(String rHost, int rPort) throws IOException {
        int i = 0;
        boolean connected = false;

        logger.debug("# Splice: connecting to: " + rHost + ":" + rPort);
        
        long start = System.currentTimeMillis();
        
        while (!connected) {
            long duration = System.currentTimeMillis() - start;
            if(duration > 1000 * 60) throw new IOException("splicing timed out");
                
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
                } catch (IOException dummy) { /* ignore */
                }
                // There is a race here, if two JVM's running on the
                // same node are both creating spliced sockets.
                // After this close, another JVM might take this
                // localAddr, and then the bind fails. (Ceriel)
                // There is no race when using the NumServer. (Ceriel)
                i++;
                // re-init the socket
                try {
                    socket = new PlainTCPSocket(p);
                    socket.setReuseAddress(true);
                    socket
                            .setSendBufferSize(ConnectionProperties.outputBufferSize);
                    socket
                            .setReceiveBufferSize(ConnectionProperties.inputBufferSize);
                    socket.bind(localAddr);
                } catch (IOException f) {
                    throw new Error(f);
                }
            }
        }
        return socket;
    }
}

