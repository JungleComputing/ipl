package ibis.ipl.impl.tcp;

import ibis.smartsockets.virtual.VirtualSocket;

import java.io.IOException;
import java.net.Socket;

/**
 * Either an ordinary socket or a smart socket.
 */
class IbisSocket {

    VirtualSocket smartSocket = null;

    Socket socket = null;

    IbisSocket(Socket s) {
        socket = s;
    }

    IbisSocket(VirtualSocket s) {
        smartSocket = s;
    }

    void setTcpNoDelay(boolean val) throws IOException {
        if (socket != null) {
            socket.setTcpNoDelay(val);
        } else {
            smartSocket.setTcpNoDelay(val);
        }
    }

    java.io.OutputStream getOutputStream() throws IOException {
        if (socket != null) {
            return socket.getOutputStream();
        }
        return smartSocket.getOutputStream();
    }

    java.io.InputStream getInputStream() throws IOException {
        if (socket != null) {
            return socket.getInputStream();
        }
        return smartSocket.getInputStream();
    }

    int getLocalPort() {
        if (socket != null) {
            return socket.getLocalPort();
        }
        return smartSocket.getLocalPort();
    }

    int getPort() {
        if (socket != null) {
            return socket.getPort();
        }
        return smartSocket.getPort();
    }

    void close() throws java.io.IOException {
        try {
            if (socket != null) {
                socket.close();
            } else {
                smartSocket.close();
            }
        } finally {
            socket = null;
            smartSocket = null;
        }
    }

    IbisSocketAddress getAddress() {
        if (socket != null) {
            return new IbisSocketAddress(socket.getLocalSocketAddress());
        }
        return new IbisSocketAddress(smartSocket.getLocalSocketAddress());
    }

    public String toString() {
        if (socket != null) {
            return socket.toString();
        }
        return smartSocket.toString();
    }
}
