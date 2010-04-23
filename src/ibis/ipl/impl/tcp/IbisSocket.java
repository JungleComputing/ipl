package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.net.Socket;

class IbisSocket {

    Socket socket = null;

    IbisSocket(Socket s) {
        socket = s;
    }

    void setTcpNoDelay(boolean val) throws IOException {
        socket.setTcpNoDelay(val);
    }

    java.io.OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    java.io.InputStream getInputStream() throws IOException {
        return socket.getInputStream();
    }

    int getLocalPort() {
        return socket.getLocalPort();
    }

    int getPort() {
        return socket.getPort();
    }

    void close() throws java.io.IOException {
        try {
            socket.close();
        } finally {
            socket = null;
        }
    }

    IbisSocketAddress getAddress() {
        return new IbisSocketAddress(socket.getLocalSocketAddress());
    }

    public String toString() {
        return socket.toString();
    }
}
