package ibis.impl.util;

import ibis.connect.socketFactory.ConnectProperties;
import ibis.ipl.ConnectionTimedOutException;
import ibis.util.IPUtils;
import ibis.util.IbisSocketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class IbisNormalSocketFactory extends IbisSocketFactory {

    /**
     * Simple ServerSocket factory.
     */
    public ServerSocket createServerSocket(int port, int backlog,
            InetAddress addr) throws IOException {
        return new ServerSocket(port, backlog, addr);
    }

    /**
     * Simple client Socket factory.
     */
    public Socket createSocket(InetAddress rAddr, int rPort) throws IOException {
        Socket s = null;
        s = new Socket(rAddr, rPort);
        tuneSocket(s);
        return s;
    }

    /** 
     *  A host can have multiple local IPs (sierra)
     *  if localIP is null, try to bind to the first of this machine's IP addresses.
     *
     *  timeoutMillis < 0  means do not retry, throw exception on failure.
     *  timeoutMillis == 0 means retry until success.
     *  timeoutMillis > 0  means block at most for timeoutMillis milliseconds, then return. 
     *  An IOException is thrown when the socket was not properly created within this time.
     */
    // timneout is not implemented correctly @@@@
    // this can only be done with 1.4 functions... --Rob
    public Socket createSocket(InetAddress dest, int port, InetAddress localIP,
            long timeoutMillis) throws IOException {
        boolean connected = false;
        Socket s = null;
        long startTime = System.currentTimeMillis();
        long currentTime = 0;

        if (localIP != null && dest.getHostAddress().equals("127.0.0.1")) {
            /* Avoid ConnectionRefused exception */
            dest = InetAddress.getLocalHost();
        }

        while (!connected) {
            int localPort = allocLocalPort();
            if (DEBUG) {
                System.err.println("Trying to connect Socket (local:"
                        + (localIP == null ? "any" : localIP.toString()) + ":"
                        + localPort + ") to " + dest + ":" + port);
            }

            try {
                s = null;

                if (localIP == null) {
                    s = new Socket(dest, port);
                } else {
                    s = new Socket(dest, port, localIP, localPort);
                }

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

    /**
     * A host can have multiple local IPs (sierra).
     * If localIP is null, try to bind to the first of this machine's IP addresses.
     * port of 0 means choose a free port.
     */
    public ServerSocket createServerSocket(int port, InetAddress localAddress,
            boolean retry) throws IOException {
        boolean connected = false;
        /*Ibis*/ServerSocket s = null;
        int localPort;

        while (!connected) {
            try {
                if (port == 0) {
                    localPort = allocLocalPort();
                } else {
                    localPort = port;
                }

                if (DEBUG) {
                    System.err.println("Creating new ServerSocket on "
                            + localAddress + ":" + localPort);
                }

                s = createServerSocket(localPort, 50, localAddress);

                if (DEBUG) {
                    System.err.println("DONE, with port = " + s.getLocalPort());
                }
                connected = true;
            } catch (IOException e1) {
                if (!retry) {
                    throw e1;
                }
                if (DEBUG) {
                    System.err.println("ServerSocket connect to " + port
                            + " failed: " + e1 + "; retrying");
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e2) {
                    // don't care
                }
            }
        }

        return s;
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean isServer, ConnectProperties p) throws IOException {
        Socket s = null;
        if (isServer) {
            ServerSocket server = createServerSocket(0, 1, IPUtils
                    .getLocalHostAddress());
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(server.getInetAddress());
            os.writeInt(server.getLocalPort());
            os.flush();
            s = server.accept();
        } else {
            ObjectInputStream is = new ObjectInputStream(in);
            InetAddress raddr;
            try {
                raddr = (InetAddress) is.readObject();
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }
            int rport = is.readInt();
            s = this.createSocket(raddr, rport);
        }
        return s;
    }
}