package ibis.ipl.impl.tcp;

import ibis.util.IPUtils;

import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class IbisServerSocket {

    ServerSocket socket = null;

    IbisServerSocket(ServerSocket s) {
        socket = s;
    }

    IbisSocket accept() throws java.io.IOException {
        Socket s = socket.accept();
        int b = s.getInputStream().read();
        if (b > 1) {
            ServerSocket n = new ServerSocket();
            Socket[] result = new Socket[b];
            result[0] = s;
            try {
                InetSocketAddress local = new InetSocketAddress(
                        IPUtils.getLocalHostAddress(), 0);
                n.bind(local);
                IbisSocketAddress addr = new IbisSocketAddress(n.getLocalSocketAddress());
                byte[] baddr = addr.toBytes();
                DataOutputStream d = new DataOutputStream(s.getOutputStream());
                d.writeInt(baddr.length);
                d.write(baddr);
                d.flush();
                for (int i = 1; i < b; i++) {
                    System.out.println("Accept from address " + addr.toString());
                    result[i] = n.accept();
                }
                return new IbisSocket(result);
            } finally {
                n.close();
            }
        } else {
            return new IbisSocket(s);
        }
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
