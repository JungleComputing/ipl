/*
 * Created on Apr 20, 2005 by rob
 */
package ibis.connect.plainSocketFactories;

import ibis.connect.IbisServerSocket;
import ibis.connect.IbisSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;


class PlainTCPServerSocket extends IbisServerSocket {
    ServerSocket s;
    
    PlainTCPServerSocket(ServerSocket s, Map p) throws IOException {
        super(p);
        this.s = s;
    }

    PlainTCPServerSocket(Map p) throws IOException {
        super(p);
        s = new ServerSocket();
    }

    PlainTCPServerSocket(int port, Map p) throws IOException {
        super(p);
        s = new ServerSocket(port);
    }

    PlainTCPServerSocket(int port, int backlog, Map p) throws IOException {
        super(p);
        s = new ServerSocket(port, backlog);
    }

    PlainTCPServerSocket(int port, int backlog, InetAddress bindAddr, Map p)
            throws IOException {
        super(p);
        s = new ServerSocket(port, backlog, bindAddr);
    }

    public Socket accept() throws IOException {
        IbisSocket res = new PlainTCPSocket(s.accept(), props);
        res.tuneSocket();
        return res;
    }

    public void bind(SocketAddress arg0) throws IOException {
        s.bind(arg0);
    }

    public void bind(SocketAddress arg0, int arg1) throws IOException {
        s.bind(arg0, arg1);
    }

    public void close() throws IOException {
        s.close();
    }

    public boolean equals(Object arg0) {
        return s.equals(arg0);
    }

    public ServerSocketChannel getChannel() {
        return s.getChannel();
    }

    public InetAddress getInetAddress() {
        return s.getInetAddress();
    }

    public int getLocalPort() {
        return s.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return s.getLocalSocketAddress();
    }

    public int getReceiveBufferSize() throws SocketException {
        return s.getReceiveBufferSize();
    }

    public boolean getReuseAddress() throws SocketException {
        return s.getReuseAddress();
    }

    public int getSoTimeout() throws IOException {
        return s.getSoTimeout();
    }

    public int hashCode() {
        return s.hashCode();
    }

    public boolean isBound() {
        return s.isBound();
    }

    public boolean isClosed() {
        return s.isClosed();
    }

    public void setReceiveBufferSize(int arg0) throws SocketException {
        s.setReceiveBufferSize(arg0);
    }

    public void setReuseAddress(boolean arg0) throws SocketException {
        s.setReuseAddress(arg0);
    }

    public void setSoTimeout(int arg0) throws SocketException {
        s.setSoTimeout(arg0);
    }

    public String toString() {
        return s.toString();
    }
}