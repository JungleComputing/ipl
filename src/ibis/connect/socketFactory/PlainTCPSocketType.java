/* $Id$ */

package ibis.connect.socketFactory;

import ibis.util.IPUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

// SocketType descriptor for plain TCP sockets
// -------------------------------------------
public class PlainTCPSocketType extends SocketType implements
        ClientServerSocketFactory, BrokeredSocketFactory {
    public PlainTCPSocketType() {
        super("PlainTCP");
    }

    public Socket createClientSocket(InetAddress addr, int port)
            throws IOException {
        Socket s = new Socket(addr, port);
        return s;
    }

    public ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
            throws IOException {
        ServerSocket s = new ServerSocket();
        s.setReceiveBufferSize(0x10000);
        s.bind(addr, backlog);
        return s;
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, ConnectProperties p) throws IOException {
        Socket s = null;
        if (hintIsServer) {
            ServerSocket server = this.createServerSocket(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1);
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
            s = this.createClientSocket(raddr, rport);
        }
        return s;
    }
}
