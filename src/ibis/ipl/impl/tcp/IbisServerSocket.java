package ibis.ipl.impl.tcp;

import java.net.ServerSocket;

class IbisServerSocket {

    ServerSocket socket = null;

    IbisServerSocket(ServerSocket s) {
        socket = s;
    }

    IbisSocket accept() throws java.io.IOException {
        return new IbisSocket(socket.accept());
    }

    IbisSocketAddress getLocalSocketAddress() {
        return new IbisSocketAddress(socket.getLocalSocketAddress());
    }

    void close() throws java.io.IOException {
        try {
            socket.close();
        } finally {
            socket = null;
        }
    }
}
