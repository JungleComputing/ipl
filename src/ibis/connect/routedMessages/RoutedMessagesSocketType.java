/* $Id$ */

package ibis.connect.routedMessages;

import ibis.connect.socketFactory.BrokeredSocketFactory;
import ibis.connect.socketFactory.ClientServerSocketFactory;
import ibis.connect.socketFactory.ConnectProperties;
import ibis.connect.socketFactory.ExtSocketFactory;
import ibis.connect.socketFactory.SocketType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

// SocketType descriptor for Routed Messages
// ------------------------------------------
public class RoutedMessagesSocketType extends SocketType implements
        ClientServerSocketFactory, BrokeredSocketFactory {
    public RoutedMessagesSocketType() throws IOException {
        super("RoutedMessages");
        if (!HubLinkFactory.isConnected()) {
            System.out.println(
                    "RoutedMessages: cannot initialize- hub is not connected.");
            throw new IOException("Hub not connected.");
        }
    }

    public void destroySocketType() {
        HubLinkFactory.destroyHubLink();
    }

    public Socket createClientSocket(InetAddress addr, int port)
            throws IOException {
        Socket s = new RMSocket(addr, port);
        return s;
    }

    public ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
            throws IOException {
        ServerSocket s = new RMServerSocket(addr.getPort(), addr.getAddress());
        return s;
    }

    public Socket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, ConnectProperties p) throws IOException {
        return ExtSocketFactory.createBrokeredSocketFromClientServer(this, in,
                out, hintIsServer, p);
    }
}
