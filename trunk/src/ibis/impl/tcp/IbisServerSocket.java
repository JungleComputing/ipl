package ibis.impl.tcp;

import java.net.ServerSocket;

import smartsockets.virtual.VirtualServerSocket;

/**
 * Either an ordinary server socket or a smartsockets server socket.
 */
class IbisServerSocket {

    VirtualServerSocket smartSocket = null;

    ServerSocket socket = null;

    IbisServerSocket(ServerSocket s) {
        socket = s;
    }

    IbisServerSocket(VirtualServerSocket s) {
        smartSocket = s;
    }

    IbisSocket accept() throws java.io.IOException {
        if (socket != null) {
            return new IbisSocket(socket.accept());
        }
        return new IbisSocket(smartSocket.accept());
    }

    IbisSocketAddress getLocalSocketAddress() {
        if (socket != null) {
            return new IbisSocketAddress(socket.getLocalSocketAddress());
        }
        return new IbisSocketAddress(smartSocket.getLocalSocketAddress());
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
}
