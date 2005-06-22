/* $Id$ */

package ibis.connect.routedMessages;

import ibis.connect.BrokeredSocketFactory;
import ibis.connect.IbisServerSocket;
import ibis.connect.IbisSocket;
import ibis.util.IPUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Map;

// SocketType descriptor for Routed Messages
// ------------------------------------------
public class RoutedMessagesSocketFactory extends BrokeredSocketFactory {
    public RoutedMessagesSocketFactory() throws IOException {

        if (!HubLinkFactory.isConnected()) {
            System.out
                    .println("RoutedMessages: cannot initialize- hub is not connected.");
            throw new IOException("Hub not connected.");
        }
    }

    public void destroySocketType() {
        HubLinkFactory.destroyHubLink();
    }

    public IbisSocket createClientSocket(InetAddress destAddr, int destPort,
            InetAddress localAddr, int localPort, int timeout, Map properties)
            throws IOException {
        throw new Error("createClientSocket not implemented by "
                + this.getClass().getName());
    }

    public IbisSocket createClientSocket(InetAddress addr, int port, Map p)
            throws IOException {
        IbisSocket s = new RoutedMessagesSocket(addr, port, p);
        return s;
    }

    public IbisServerSocket createServerSocket(InetSocketAddress addr, int backlog, Map p)
            throws IOException {
        IbisServerSocket s = new RoutedMessagesServerSocket(addr.getPort(), addr
                .getAddress(), p);
        return s;
    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, Map properties)
            throws IOException {

        IbisSocket s = null;
        if (hintIsServer) {
            IbisServerSocket server = createServerSocket(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1,
                    properties);
            ObjectOutputStream os = new ObjectOutputStream(out);
            Hashtable lInfo = new Hashtable();
            lInfo.put("socket_address", server.getInetAddress());
            lInfo.put("socket_port", new Integer(server.getLocalPort()));
            os.writeObject(lInfo);
            os.flush();
            s = (IbisSocket) server.accept();
        } else {
            ObjectInputStream is = new ObjectInputStream(in);
            Hashtable rInfo = null;
            try {
                rInfo = (Hashtable) is.readObject();
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }
            InetAddress raddr = (InetAddress) rInfo.get("socket_address");
            int rport = ((Integer) rInfo.get("socket_port")).intValue();
            s = createClientSocket(raddr, rport, properties);
        }
        return s;
    }
}