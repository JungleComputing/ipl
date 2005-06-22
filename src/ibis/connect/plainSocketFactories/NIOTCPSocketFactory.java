/* $Id$ */

package ibis.connect.plainSocketFactories;

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
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;

// SocketType descriptor for java.nio TCP sockets
// -------------------------------------------
public class NIOTCPSocketFactory extends BrokeredSocketFactory {

    SocketChannel channel;

    ServerSocketChannel serverChannel;

    public NIOTCPSocketFactory() {
        String c = System.getProperty("ibis.tcp.cache");
        if (c != null && !c.equals("false")) {
            throw new Error(
                    "NIO sockets cannot be used in combination with the TCP connection cache."
                            + "Please disable it with -Dibis.tcp.cache=false");
        }
    }
    
    public IbisSocket createClientSocket(InetAddress destAddr, int destPort,
            InetAddress localAddr, int localPort, int timeout, Map properties)
            throws IOException {

        InetSocketAddress local = new InetSocketAddress(localAddr, localPort);
        InetSocketAddress remote = new InetSocketAddress(destAddr, destPort);

        channel = SocketChannel.open();
        channel.configureBlocking(true);
        Socket s = channel.socket();

        s.bind(local);
        s.connect(remote, timeout);
        
        tuneSocket(s);
        return new PlainTCPSocket(s, properties);
    }
    
    public IbisSocket createClientSocket(InetAddress addr, int port, Map p)
            throws IOException {
        InetSocketAddress a = new InetSocketAddress(addr, port);
        channel = SocketChannel.open(a);
        channel.configureBlocking(true);
        Socket s = channel.socket();
        tuneSocket(s);
        return new PlainTCPSocket(s, p);
    }

    public IbisServerSocket createServerSocket(InetSocketAddress addr, int backlog, Map p)
            throws IOException {
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(true);
        IbisServerSocket s = new PlainTCPServerSocket(serverChannel.socket(), p);
//        s.setReceiveBufferSize(0x10000);
        s.bind(addr, backlog);
        return s;
    }

    public IbisSocket createBrokeredSocket(InputStream in, OutputStream out,
            boolean hintIsServer, Map p)
            throws IOException {
        IbisSocket s = null;
        if (hintIsServer) {
            IbisServerSocket server = this.createServerSocket(
                    new InetSocketAddress(IPUtils.getLocalHostAddress(), 0), 1, p);
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(server.getInetAddress());
            os.writeInt(server.getLocalPort());
            os.flush();
            s = (IbisSocket) server.accept();
            tuneSocket(s);
        } else {
            ObjectInputStream is = new ObjectInputStream(in);
            InetAddress raddr;
            try {
                raddr = (InetAddress) is.readObject();
            } catch (ClassNotFoundException e) {
                throw new Error(e);
            }
            int rport = is.readInt();
            s = this.createClientSocket(raddr, rport, p);
        }
        return s;
    }
}