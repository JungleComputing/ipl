package ibis.connect.routedMessages;

import ibis.connect.socketFactory.DummySocket;
import ibis.connect.util.MyDebug;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.LinkedList;

public class RMSocket extends DummySocket {
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

    /* misc methods for the HubLink to feed us
     */
    protected synchronized void enqueueFragment(byte[] b) {
        MyDebug.out.println("# RMSocket.enqueueFragment, size = " + b.length);
        incomingFragments.addLast(b);
        this.notifyAll();
    }

    protected synchronized void enqueueAccept(int servantPort, int hport) {
        MyDebug.out.println("# RMSocket.enqueueAccept()- servantPort="
                + servantPort);
        state = state_ACCEPTED;
        remoteHostPort = hport;
        remotePort = servantPort;
        this.notifyAll();
    }

    protected synchronized void enqueueReject() {
        MyDebug.out.println("# RMSocket.enqueueReject()");
        state = state_REJECTED;
        this.notifyAll();
    }

    protected synchronized void enqueueClose() {
        MyDebug.out.println("# RMSocket.enqueueClose()- port = " + localPort
                + ", remotePort = " + remotePort);
        state = state_CLOSED;
        if (localPort != -1) {
            hub.removeSocket(localPort);
        }
        localPort = -1;
        remotePort = -1;
        this.notifyAll();
    }

    /* Initialization
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
        MyDebug.out.println("# RMSocket.commonInit()- rHost=" + rHost);
    }

    // Incoming links constructor - reserved to RMServerSocket
    protected RMSocket(String rHost, int rPort, int lPort, int hport) {
        MyDebug.out.println("# RMSocket()");
        commonInit(rHost);
        remotePort = rPort;
        localPort = lPort;
        remoteHostPort = hport;
        state = state_CONNECTED;
        hub.addSocket(this, localPort);
    }

    // Outgoing links constructor - public
    public RMSocket(InetAddress rAddr, int rPort) throws IOException {
        MyDebug.out.println("# RMSocket(" + rAddr + ", " + rPort + ")");
        commonInit(rAddr.getHostName());
        localPort = hub.newPort(0);
        hub.addSocket(this, localPort);

        MyDebug.out.println("# RMSocket()- sending CONNECT");
        state = state_CONNECTING;
        hub.sendPacket(remoteHostname, -1, new HubProtocol.HubPacketConnect(
                rPort, localPort));
        synchronized (this) {
            while (state == state_CONNECTING) {
                MyDebug.out
                        .println("# RMSocket()- waiting for ACCEPTED- port = "
                                + localPort);
                try {
                    this.wait();
                } catch (InterruptedException e) { /* ignore */
                }
                MyDebug.out.println("# RMSocket()- unlocked");
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
        MyDebug.out.println("# RMSocket.close(), localPort = " + localPort
                + ", remotePort = " + remotePort);
        state = state_CLOSED;
        if (remotePort != -1) {
            // hub.sendPacket(remoteHostname, remoteHostPort, new HubProtocol.HubPacketClose(remotePort, localPort));
            hub.removeSocket(localPort);
        }
        localPort = -1;
        remotePort = -1;
    }

    /* InputStream for RMSocket
     */
    private class RMInputStream extends InputStream {
        private RMSocket socket = null;

        private boolean open = false;

        private void checkOpen() throws IOException {
            if ((!open || state != state_CONNECTED)
                    && (socket.currentArray == null && socket.incomingFragments
                            .isEmpty())) {
                MyDebug.out.println("# Detected EOF! open=" + open + "; state="
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

        public RMInputStream(RMSocket s) {
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

                if (len == 0)
                    return 0;

                try {
                    checkOpen();
                    waitFragment();
                } catch (EOFException e) {
                    MyDebug.out.println("# RMInputStream: reading- port="
                            + socket.localPort + " size=EOF");
                    return -1;
                }
                if (len <= socket.currentArray.length - socket.currentIndex)
                    j = len;
                else
                    j = socket.currentArray.length - socket.currentIndex;

                System.arraycopy(socket.currentArray, socket.currentIndex, b,
                        off, j);
                pumpFragment(j);
                rc = j;
                MyDebug.out.println("# RMInputStream: reading- port="
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
                        MyDebug.out.println("# RMInputStream: reading- port="
                                + socket.localPort + " size=EOF");
                        return r;
                    }
                    if (socket.currentArray.length > socket.currentIndex) {
                        r = socket.currentArray[socket.currentIndex] & 0xff;

                        pumpFragment(1);
                        MyDebug.out.println("# RMInputStream: reading- port="
                                + socket.localPort + " size=1");
                    } else {
                        pumpFragment(0);
                    }
                }
            }
            return r;
        }

        public int available() throws IOException {
            MyDebug.out.println("# RMInputStream: available()");
            checkOpen();
            return socket.currentArray == null ? 0 : socket.currentArray.length
                    - socket.currentIndex;
        }

        public void close() {
            MyDebug.out.println("# RMInputStream: close()");
            synchronized (socket) {
                in = null;
                open = false;
                socket.notifyAll();
            }
        }
    }

    /* OutputStream for RMSocket
     */
    private class RMOutputStream extends OutputStream {
        private RMSocket socket;

        private boolean open = false;

        private void checkOpen() throws IOException {
            if (!open || state != state_CONNECTED) {
                MyDebug.out.println("# checkOpen: open=" + open + "; state="
                        + state);
                throw new EOFException();
            }
        }

        public RMOutputStream(RMSocket s) {
            super();
            socket = s;
            open = true;
        }

        public void write(int v) throws IOException {
            checkOpen();
            byte[] b = new byte[1];
            b[0] = (byte) v;
            MyDebug.out.println("# RMOutputStream: writing- port="
                    + socket.remotePort + " size=1");
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, b, localPort));
        }

        public void write(byte[] b) throws IOException {
            checkOpen();
            MyDebug.out.println("# RMOutputStream: writing- port="
                    + socket.remotePort + " size=" + b.length);
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, b, localPort));
        }

        public void write(byte[] b, int off, int len) throws IOException {
            checkOpen();
            byte[] a = new byte[len];
            System.arraycopy(b, off, a, 0, len);
            MyDebug.out.println("# RMOutputStream: writing- port="
                    + socket.remotePort + " size=" + len + " offset=" + off);
            hub.sendPacket(remoteHostname, remoteHostPort,
                    new HubProtocol.HubPacketData(remotePort, a, localPort));
        }

        public void flush() {
            //	    checkOpen();
            MyDebug.out.println("# RMOutputStream: flush()");
        }

        public void close() {
            MyDebug.out.println("# RMOutputStream: close()");
            synchronized (socket) {
                out = null;
                open = false;
                socket.notifyAll();
            }
        }
    }
}