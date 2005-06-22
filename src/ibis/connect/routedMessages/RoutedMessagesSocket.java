/* $Id$ */

package ibis.connect.routedMessages;

import ibis.connect.IbisSocket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

public class RoutedMessagesSocket extends IbisSocket {

    static Logger logger
            = ibis.util.GetLogger.getLogger(RoutedMessagesSocket.class.getName());

    HubLink hub = null;

    String remoteHostname = null;

    int remotePort = -1;

    int remoteHostPort = -1;

    int localPort = -1;

    RMInputStream in = null;

    RMOutputStream out = null;

    LinkedList incomingFragments = new LinkedList(); // list of byte[]

    byte[] currentArray = null;

    int currentIndex = 0;

    static final int state_NONE = 1;

    static final int state_CONNECTING = 2;

    static final int state_ACCEPTED = 3;

    static final int state_REJECTED = 4;

    static final int state_CONNECTED = 5;

    static final int state_CLOSED = 6;

    int state;

    /*
     * misc methods for the HubLink to feed us
     */
    protected synchronized void enqueueFragment(byte[] b) {
        logger.debug("# RoutedMessagesSocket.enqueueFragment, size = "
                + b.length);
        incomingFragments.addLast(b);
        this.notifyAll();
    }

    protected synchronized void enqueueAccept(int servantPort, int hport) {
        logger.debug("# RoutedMessagesSocket.enqueueAccept()- servantPort="
                + servantPort);
        state = state_ACCEPTED;
        remoteHostPort = hport;
        remotePort = servantPort;
        this.notifyAll();
    }

    protected synchronized void enqueueReject() {
        logger.debug("# RoutedMessagesSocket.enqueueReject()");
        state = state_REJECTED;
        this.notifyAll();
    }

    protected synchronized void enqueueClose() {
        logger.debug("# RoutedMessagesSocket.enqueueClose()- port = "
                + localPort + ", remotePort = " + remotePort);
        state = state_CLOSED;
        if (localPort != -1) {
            hub.removeSocket(localPort);
        }
        localPort = -1;
        remotePort = -1;
        this.notifyAll();
    }

    /*
     * Initialization
     */
    private void commonInit(String rHost) {
        try {
            hub = HubLinkFactory.getHubLink();
        } catch (Exception e) {
            throw new Error("Cannot initialize HubLink.");
        }
        remoteHostname = rHost;
        out = new RMOutputStream(this);
        in = new RMInputStream(this);
        state = state_NONE;
        logger.debug("# RoutedMessagesSocket.commonInit()- rHost=" + rHost);
    }

    // Incoming links constructor - reserved to RoutedMessagesServerSocket
    protected RoutedMessagesSocket(String rHost, int rPort, int lPort, int hport, Map p) throws IOException {
        super(p);
        logger.debug("# RoutedMessagesSocket()");
        commonInit(rHost);
        remotePort = rPort;
        localPort = lPort;
        remoteHostPort = hport;
        state = state_CONNECTED;
        hub.addSocket(this, localPort);
    }

    // Outgoing links constructor - public
    public RoutedMessagesSocket(InetAddress rAddr, int rPort, Map p)
            throws IOException {
        super(p);
        logger.debug("# RoutedMessagesSocket(" + rAddr + ", " + rPort + ")");
        commonInit(rAddr.getHostName());
        localPort = hub.newPort(0);
        hub.addSocket(this, localPort);

        logger.debug("# RoutedMessagesSocket()- sending CONNECT");
        state = state_CONNECTING;
        hub.sendPacket(remoteHostname, -1, new HubProtocol.HubPacketConnect(
                rPort, localPort));
        synchronized (this) {
            while (state == state_CONNECTING) {
                logger
                        .debug("# RoutedMessagesSocket()- waiting for ACCEPTED port = "
                                + localPort);
                try {
                    this.wait();
                } catch (InterruptedException e) { /* ignore */
                }
                logger.debug("# RoutedMessagesSocket()- unlocked");
            }
            if (state == state_ACCEPTED) {
                state = state_CONNECTED;
            } else if (state == state_REJECTED) {
                throw new IOException("connection refused");
            }
        }
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public void setTcpNoDelay(boolean on) {
        // ignored
    }

    public void setSoTimeout(int t) {
        // ignored
    }

    public void shutdownInput() {
        // ignored
    }

    public void shutdownOutput() {
        // ignored
    }

    public void setSendBufferSize(int sz) {
        // ignored
    }

    public void setReceiveBufferSize(int sz) {
        // ignored
    }

    public InputStream getInputStream() {
        return in;
    }

    public int getPort() {
        return remoteHostPort;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String toString() {
        return "RoutedMessages Socket";
    }

    public synchronized void close() {
        logger.debug("# RoutedMessagesSocket.close(), localPort = " + localPort
                + ", remotePort = " + remotePort);
        state = state_CLOSED;
        if (remotePort != -1) {
            hub.removeSocket(localPort);
        }
        localPort = -1;
        remotePort = -1;
    }

    /*
     * InputStream for RoutedMessagesSocket
     */
    private class RMInputStream extends InputStream {
        private RoutedMessagesSocket socket = null;

        private boolean open = false;

        private void checkOpen() throws IOException {
            if ((!open || state != state_CONNECTED)
                    && (socket.currentArray == null && socket.incomingFragments
                            .isEmpty())) {
                logger.debug("# Detected EOF! open=" + open + "; state="
                        + state + "; socket.currentArray="
                        + socket.currentArray + "; incomingFragment: "
                        + socket.incomingFragments.isEmpty());
                throw new EOFException();
            }
        }

        private void waitFragment() throws IOException {
            if (socket.currentArray == null) {
                while (incomingFragments.size() == 0) {
                    try {
                        checkOpen();
                        socket.wait();
                    } catch (InterruptedException e) {
                        /* ignored */
                    }
                }
                socket.currentArray = (byte[]) socket.incomingFragments
                        .removeFirst();
                socket.currentIndex = 0;
            }
        }

        private void pumpFragment(int amount) {
            socket.currentIndex += amount;
            if (socket.currentIndex >= socket.currentArray.length) {
                socket.currentArray = null;
            }
        }

        public RMInputStream(RoutedMessagesSocket s) {
            super();
            socket = s;
            open = true;
        }

        public int read(byte[] b) throws IOException {
            int rc = this.read(b, 0, b.length);
            return rc;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            int rc = -1;
            synchronized (socket) {
                int j = 0;

                if (len == 0) {
                    return 0;
                }

                try {
                    checkOpen();
                    waitFragment();
                } catch (EOFException e) {
                    logger.debug("# RMInputStream: reading- port="
                            + socket.localPort + " size=EOF");
                    return -1;
                }
                if (len <= socket.currentArray.length - socket.currentIndex) {
                    j = len;
                } else {
                    j = socket.currentArray.length - socket.currentIndex;
                }

                System.arraycopy(socket.currentArray, socket.currentIndex, b,
                        off, j);
                pumpFragment(j);
                rc = j;
                logger.debug("# RMInputStream: reading- port="
                        + socket.localPort + " size=" + rc);
            }
            return rc;
        }

        public int read() throws IOException {
            int r = -1;
            synchronized (socket) {
                while (r == -1) {
                    try {
                        checkOpen();
                        waitFragment();
                    } catch (EOFException e) {
                        logger.debug("# RMInputStream: reading- port="
                                + socket.localPort + " size=EOF");
                        return r;
                    }
                    if (socket.currentArray.length > socket.currentIndex) {
                        r = socket.currentArray[socket.currentIndex] & 0xff;

                        pumpFragment(1);
                        logger.debug("# RMInputStream: reading- port="
                                + socket.localPort + " size=1");
                    } else {
                        pumpFragment(0);
                    }
                }
            }
            return r;
        }

        public int available() throws IOException {
            logger.debug("# RMInputStream: available()");
            checkOpen();
            return socket.currentArray == null ? 0 : socket.currentArray.length
                    - socket.currentIndex;
        }

        public void close() {
            logger.debug("# RMInputStream: close()");
            synchronized (socket) {
                in = null;
                open = false;
                socket.notifyAll();
            }
        }
    }

    /*
     * OutputStream for RoutedMessagesSocket
     */
    private class RMOutputStream extends OutputStream {
        private RoutedMessagesSocket socket;

        private boolean open = false;

        private void checkOpen() throws IOException {
            if (!open || state != state_CONNECTED) {
                logger.debug("# checkOpen: open=" + open + "; state=" + state);
                throw new EOFException();
            }
        }

        public RMOutputStream(RoutedMessagesSocket s) {
            super();
            socket = s;
            open = true;
        }

        public void write(int v) throws IOException {
            checkOpen();
            byte[] b = new byte[1];
            b[0] = (byte) v;
            logger.debug("# RMOutputStream: writing- port=" + socket.remotePort
                    + " size=1");
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, b, localPort));
        }

        public void write(byte[] b) throws IOException {
            checkOpen();
            logger.debug("# RMOutputStream: writing- port=" + socket.remotePort
                    + " size=" + b.length);
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, b, localPort));
        }

        public void write(byte[] b, int off, int len) throws IOException {
            checkOpen();
            byte[] a = new byte[len];
            System.arraycopy(b, off, a, 0, len);
            logger.debug("# RMOutputStream: writing- port=" + socket.remotePort
                    + " size=" + len + " offset=" + off);
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, a, localPort));
        }

        public void flush() {
            //	    checkOpen();
            logger.debug("# RMOutputStream: flush()");
        }

        public void close() {
            logger.debug("# RMOutputStream: close()");
            synchronized (socket) {
                out = null;
                open = false;
                socket.notifyAll();
            }
        }
    }
}