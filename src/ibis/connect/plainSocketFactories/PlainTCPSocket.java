/*
 * Created on Apr 20, 2005 by rob
 */
package ibis.connect.plainSocketFactories;

import ibis.connect.IbisSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImplFactory;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Map;

/**
 * @author rob
 */
public class PlainTCPSocket extends IbisSocket {
    Socket s;

    private PlainTCPSocket() throws IOException {
        super(null);
        // avoid that subclasses create an instance without props.
        throw new Error("cannot happen");
    }
    
    public PlainTCPSocket(InetAddress destAddr, int destPort,
            InetAddress localAddr, int localPort, int timeout, Map properties) throws IOException {
        super(properties);
        
        s = new Socket();
        InetSocketAddress local = new InetSocketAddress(localAddr, localPort);
        s.bind(local);
        
        InetSocketAddress remote = new InetSocketAddress(destAddr, destPort);
        s.connect(remote, timeout);
        
        tuneSocket();
    }

    // used for NIO sockets
    PlainTCPSocket(Socket s, Map p) throws IOException {
        super(p);
        this.s = s;
//        tuneSocket(); it is already tuned
    }

    public PlainTCPSocket(Map p) throws IOException {
        super(p);
        s = new Socket();
        tuneSocket();
    }

    public PlainTCPSocket(String arg0, int arg1, Map p) throws UnknownHostException,
            IOException {
        super(p);
        s = new Socket(arg0, arg1);
        tuneSocket();
    }

    public PlainTCPSocket(String arg0, int arg1, boolean arg2, Map p)
            throws IOException {
        super(p);
        s = new Socket(arg0, arg1, arg2);
        tuneSocket();
    }

    public PlainTCPSocket(String arg0, int arg1, InetAddress arg2, int arg3, Map p)
            throws IOException {
        super(p);
        s = new Socket(arg0, arg1, arg2, arg3);
        tuneSocket();
    }

    public PlainTCPSocket(InetAddress arg0, int arg1, Map p) throws IOException {
        super(p);
        s = new Socket(arg0, arg1);
        tuneSocket();
    }

    public PlainTCPSocket(InetAddress arg0, int arg1, boolean arg2, Map p)
            throws IOException {
        super(p);
        s = new Socket(arg0, arg1, arg2);
        tuneSocket();
    }

    public PlainTCPSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3, Map p)
            throws IOException {
        super(p);
        s = new Socket(arg0, arg1, arg2, arg3);
        tuneSocket();
    }

    public static void setSocketImplFactory(SocketImplFactory arg0)
            throws IOException {
        Socket.setSocketImplFactory(arg0);
    }

    public void bind(SocketAddress arg0) throws IOException {
        s.bind(arg0);
    }

    public void close() throws IOException {
        s.close();
    }

    public void connect(SocketAddress arg0) throws IOException {
        s.connect(arg0);
    }

    public void connect(SocketAddress arg0, int arg1) throws IOException {
        s.connect(arg0, arg1);
    }

    public boolean equals(Object arg0) {
        return s.equals(arg0);
    }

    public SocketChannel getChannel() {
        return s.getChannel();
    }

    public InetAddress getInetAddress() {
        return s.getInetAddress();
    }

    public InputStream getInputStream() throws IOException {
        return s.getInputStream();
    }

    public boolean getKeepAlive() throws SocketException {
        return s.getKeepAlive();
    }

    public InetAddress getLocalAddress() {
        return s.getLocalAddress();
    }

    public int getLocalPort() {
        return s.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return s.getLocalSocketAddress();
    }

    public boolean getOOBInline() throws SocketException {
        return s.getOOBInline();
    }

    public OutputStream getOutputStream() throws IOException {
        return s.getOutputStream();
    }

    public int getPort() {
        return s.getPort();
    }

    public int getReceiveBufferSize() throws SocketException {
        return s.getReceiveBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
        return s.getRemoteSocketAddress();
    }

    public boolean getReuseAddress() throws SocketException {
        return s.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
        return s.getSendBufferSize();
    }

    public int getSoLinger() throws SocketException {
        return s.getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
        return s.getSoTimeout();
    }

    public boolean getTcpNoDelay() throws SocketException {
        return s.getTcpNoDelay();
    }

    public int getTrafficClass() throws SocketException {
        return s.getTrafficClass();
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

    public boolean isConnected() {
        return s.isConnected();
    }

    public boolean isInputShutdown() {
        return s.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return s.isOutputShutdown();
    }

    public void sendUrgentData(int arg0) throws IOException {
        s.sendUrgentData(arg0);
    }

    public void setKeepAlive(boolean arg0) throws SocketException {
        s.setKeepAlive(arg0);
    }

    public void setOOBInline(boolean arg0) throws SocketException {
        s.setOOBInline(arg0);
    }

    public void setReceiveBufferSize(int arg0) throws SocketException {
        s.setReceiveBufferSize(arg0);
    }

    public void setReuseAddress(boolean arg0) throws SocketException {
        s.setReuseAddress(arg0);
    }

    public void setSendBufferSize(int arg0) throws SocketException {
        s.setSendBufferSize(arg0);
    }

    public void setSoLinger(boolean arg0, int arg1) throws SocketException {
        s.setSoLinger(arg0, arg1);
    }

    public void setSoTimeout(int arg0) throws SocketException {
        s.setSoTimeout(arg0);
    }

    public void setTcpNoDelay(boolean arg0) throws SocketException {
        s.setTcpNoDelay(arg0);
    }

    public void setTrafficClass(int arg0) throws SocketException {
        s.setTrafficClass(arg0);
    }

    public void shutdownInput() throws IOException {
        s.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        s.shutdownOutput();
    }

    public String toString() {
        return s.toString();
    }
}