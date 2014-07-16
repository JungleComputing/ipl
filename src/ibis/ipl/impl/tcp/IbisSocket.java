package ibis.ipl.impl.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

class IbisSocket {

    Socket[] sockets = null;
    InputStream in;
    OutputStream out;

    IbisSocket(Socket s) throws IOException {
        sockets = new Socket[1];
        sockets[0] = s;
        in = s.getInputStream();
        out = s.getOutputStream();
    }

    public IbisSocket(Socket[] sockets) throws IOException {
        this.sockets = sockets;
        in = new PInputStream(sockets);
        out = new POutputStream(sockets);
    }

    void setTcpNoDelay(boolean val) throws IOException {
        for (Socket socket : sockets) {
            socket.setTcpNoDelay(val);
        }
    }

    java.io.OutputStream getOutputStream() throws IOException {
        if (sockets.length == 1) {
            return sockets[0].getOutputStream();
        }
        return out;
    }

    java.io.InputStream getInputStream() throws IOException {
        if (sockets.length == 1) {
            return sockets[0].getInputStream();
        }
        return in;
    }

    void close() throws java.io.IOException {
        if (sockets == null) {
            return;
        }
        try {
            for (Socket socket : sockets) {
                if (socket != null) {
                    socket.close();
                }
            }
        } finally {
            sockets = null;
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(sockets);
    }
}
