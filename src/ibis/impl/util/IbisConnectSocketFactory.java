package ibis.impl.util;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.connect.socketFactory.ExtSocketFactory;
import ibis.ipl.ConnectionTimedOutException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class IbisConnectSocketFactory extends IbisNormalSocketFactory {

    /**
     * Simple ServerSocket factory.
     */
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress addr) throws IOException {
        ServerSocket s = ExtSocketFactory.createServerSocket(port, backlog,
                addr);
        return s;
    }

    /**
     * Simple client Socket factory.
     */
    public Socket createSocket(InetAddress rAddr, int rPort) throws IOException {
        Socket s = ExtSocketFactory.createClientSocket(rAddr, rPort);
        tuneSocket(s);
        return s;
    }

    public Socket createBrokeredSocket(Socket s, boolean isServer,
            ConnectProperties p) throws IOException {
        return ExtSocketFactory.createBrokeredSocket(s.getInputStream(), s
                .getOutputStream(), isServer, p);
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean isServer, ConnectProperties p) throws IOException {
        return ExtSocketFactory.createBrokeredSocket(in, out, isServer, p);
    }

    /** 
     * A host can have multiple local IPs (sierra)
     * if localIP is null, try to bind to the first of this machine's IP addresses.
     *
     * timeoutMillis < 0  means do not retry, throw exception on failure.
     * timeoutMillis == 0 means retry until success.
     * timeoutMillis > 0  means block at most for timeoutMillis milliseconds, then return. 
     * An IOException is thrown when the socket was not properly created within this time.
     */
    public Socket createSocket(InetAddress dest, int port, InetAddress localIP,
            long timeoutMillis) throws IOException {
        boolean connected = false;
        Socket s = null;
        long startTime = System.currentTimeMillis();
        long currentTime = 0;

        while (!connected) {
            if (DEBUG) {
                System.err
                        .println("ibis-connect: Trying to connect Socket (local:"
                                + (localIP == null ? "any" : localIP.toString())
                                + ") to " + dest + ":" + port);
            }

            try {
                s = ExtSocketFactory.createClientSocket(dest, port);
                if (DEBUG) {
                    System.err.println("DONE, local port: " + s.getLocalPort());
                }
                connected = true;
            } catch (IOException e1) {
                if (DEBUG) {
                    System.err.println("Socket connect to " + dest + ":" + port
                            + " failed (" + e1 + ")");
                    e1.printStackTrace();
                }

                if (s != null) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        /* ignore */
                    }
                }

                //System.err.println("Socket connect hits " + e1);

                if (timeoutMillis < 0) {
                    throw new ConnectionTimedOutException("" + e1);
                } else if (timeoutMillis == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e2) {
                        // don't care
                    }

                } else {
                    currentTime = System.currentTimeMillis();
                    if (currentTime - startTime < timeoutMillis) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e2) {
                            // don't care
                        }
                    } else {
                        throw new ConnectionTimedOutException("" + e1);
                    }
                }
            }
        }

        tuneSocket(s);
        return s;
    }

    public void shutdown() {
        ExtSocketFactory.shutdown();
    }
}