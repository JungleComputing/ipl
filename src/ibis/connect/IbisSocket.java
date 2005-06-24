/* $Id$ */

package ibis.connect;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The class <code>IbisSocket</code> is a common superclass of all the ibis
 * connect socket types. It is mostly there to warn implementations for methods
 * that are used but not implemented.
 */
public abstract class IbisSocket extends Socket {

    static Logger logger = ibis.util.GetLogger.getLogger(IbisSocket.class
            .getName());

    Map props;
    
    private IbisSocket() {
        // make private to avoid subclasses to forget the properties
    }

    protected IbisSocket(Map p) throws IOException {
        this.props = p;
    }

    public void bind(SocketAddress p) throws IOException {
        System.err.println("bind(SocketAddress) not implemented by " + this);
        throw new RuntimeException("bind(SocketAddress) not implemented by "
                + this);
    }

    public void connect(SocketAddress p, int timeout) throws IOException {
        System.err.println("connect(SocketAddress, int) not implemented by "
                + this);
        throw new RuntimeException(
                "connect(SocketAddress, int) not implemented by " + this);
    }

    public void connect(SocketAddress p) throws IOException {
        System.err.println("connect(SocketAddress) not implemented by " + this);
        throw new RuntimeException("connect(SocketAddress) not implemented by "
                + this);
    }

    public void close() throws IOException {
        System.err.println("close() not implemented by " + this);
        throw new RuntimeException("close() not implemented by " + this);
    }

    public SocketChannel getChannel() {
        System.err.println("getChannel() not implemented by " + this);
        throw new RuntimeException("getChannel() not implemented by " + this);
    }

    public InetAddress getInetAddress() {
        System.err.println("getInetAddress() not implemented by " + this);
        throw new RuntimeException("getInetAddress() not implemented by "
                + this);
    }

    public InputStream getInputStream() throws IOException {
        System.err.println("getInputStream() not implemented by " + this);
        throw new RuntimeException("getInputStream() not implemented by "
                + this);
    }

    public boolean getKeepAlive() throws SocketException {
        System.err.println("getKeepAlive() not implemented by " + this);
        throw new RuntimeException("getKeepAlive() not implemented by " + this);
    }

    public InetAddress getLocalAddress() {
        System.err.println("getLocalAddress() not implemented by " + this);
        throw new RuntimeException("getLocalAddress() not implemented by "
                + this);
    }

    public int getLocalPort() {
        System.err.println("getLocalPort() not implemented by " + this);
        throw new RuntimeException("getLocalPort() not implemented by " + this);
    }

    public SocketAddress getLocalSocketAddress() {
        System.err
                .println("getLocalSocketAddress() not implemented by " + this);
        throw new RuntimeException(
                "getLocalSocketAddress() not implemented by " + this);
    }

    public boolean getOOBInline() throws SocketException {
        System.err.println("getOOBInline() not implemented by " + this);
        throw new RuntimeException("getOOBInline() not implemented by " + this);
    }

    public OutputStream getOutputStream() throws IOException {
        System.err.println("getOutputStream() not implemented by " + this);
        throw new RuntimeException("getOutputStream() not implemented by "
                + this);
    }

    public int getPort() {
        System.err.println("getPort() not implemented by " + this);
        throw new RuntimeException("getPort() not implemented by " + this);
    }

    public int getReceiveBufferSize() throws SocketException {
        System.err.println("getReceiveBufferSize() not implemented by " + this);
        throw new RuntimeException("getReceiveBufferSize() not implemented by "
                + this);
    }

    public SocketAddress getRemoteSocketAddress() {
        System.err.println("getRemoteSocketAddress() not implemented by "
                + this);
        throw new RuntimeException(
                "getRemoteSocketAddress() not implemented by " + this);
    }

    public boolean getReuseAddress() throws SocketException {
        System.err.println("getReuseAddress() not implemented by " + this);
        throw new RuntimeException("getReuseAddress() not implemented by "
                + this);
    }

    public int getSendBufferSize() throws SocketException {
        System.err.println("getSendBufferSize() not implemented by " + this);
        throw new RuntimeException("getSendBufferSize() not implemented by "
                + this);
    }

    public int getSoLinger() throws SocketException {
        System.err.println("getSoLinger() not implemented by " + this);
        throw new RuntimeException("getSoLinger() not implemented by " + this);
    }

    public int getSoTimeout() throws SocketException {
        System.err.println("getSoTimeout() not implemented by " + this);
        throw new RuntimeException("getSoTimeout() not implemented by " + this);
    }

    public boolean getTcpNoDelay() throws SocketException {
        System.err.println("getTcpNoDelay() not implemented by " + this);
        throw new RuntimeException("getTcpNoDelay() not implemented by " + this);
    }

    public int getTrafficClass() throws SocketException {
        System.err.println("getTrafficClass() not implemented by " + this);
        throw new RuntimeException("getTrafficClass() not implemented by "
                + this);
    }

    public boolean isBound() {
        System.err.println("isBound() not implemented by " + this);
        throw new RuntimeException("isBound() not implemented by " + this);
    }

    public boolean isClosed() {
        System.err.println("isClosed() not implemented by " + this);
        throw new RuntimeException("isClosed() not implemented by " + this);
    }

    public boolean isConnected() {
        System.err.println("isConnected() not implemented by " + this);
        throw new RuntimeException("isConnected() not implemented by " + this);
    }

    public boolean isInputShutdown() {
        System.err.println("isInputShutdown() not implemented by " + this);
        throw new RuntimeException("isInputShutdown() not implemented by "
                + this);
    }

    public boolean isOutputShutdown() {
        System.err.println("isOutputShutdown() not implemented by " + this);
        throw new RuntimeException("isOutputShutdown() not implemented by "
                + this);
    }

    public void sendUrgentData(int data) throws IOException {
        System.err.println("sendUrgentData(int) not implemented by " + this);
        throw new RuntimeException("sendUrgentData(int) not implemented by "
                + this);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        System.err.println("setKeepAlive(boolean) not implemented by " + this);
        throw new RuntimeException("setKeepAlive(boolean) not implemented by "
                + this);
    }

    public void setOOBInline(boolean on) throws SocketException {
        System.err.println("setOOBInline(boolean) not implemented by " + this);
        throw new RuntimeException("setOOBInline(boolean) not implemented by "
                + this);
    }

    public void setReceiveBufferSize(int sz) throws SocketException {
        System.err.println("setReceiveBufferSize(int) not implemented by "
                + this);
        throw new RuntimeException(
                "setReceiveBufferSize(int) not implemented by " + this);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        System.err.println("setReuseAddress(boolean) not implemented by "
                + this);
        throw new RuntimeException(
                "setReuseAddress(boolean) not implemented by " + this);
    }

    public void setSendBufferSize(int sz) throws SocketException {
        System.err.println("setSendBufferSize(int) not implemented by " + this);
        throw new RuntimeException("setSendBufferSize(int) not implemented by "
                + this);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        System.err.println("setSoLinger(boolean, int) not implemented by "
                + this);
        throw new RuntimeException(
                "setSoLinger(boolean, int) not implemented by " + this);
    }

    public void setSoTimeout(int t) throws SocketException {
        System.err.println("setSoTimeout(int) not implemented by " + this);
        throw new RuntimeException("setSoTimeout(int) not implemented by "
                + this);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        System.err.println("setTcpNoDelay(boolean) not implemented by " + this);
        throw new RuntimeException("setTcpNoDelay(boolean) not implemented by "
                + this);
    }

    public void setTrafficClass(int tc) throws SocketException {
        System.err.println("setTrafficClass(int) not implemented by " + this);
        throw new RuntimeException("setTrafficClass(int) not implemented by "
                + this);
    }

    public void shutdownInput() throws IOException {
        System.err.println("shutdownInput() not implemented by " + this);
        throw new RuntimeException("shutdownInput() not implemented by " + this);
    }

    public void shutdownOutput() throws IOException {
        System.err.println("shutdownOutput() not implemented by " + this);
        throw new RuntimeException("shutdownOutput() not implemented by "
                + this);
    }

    public String toString() {
        return "unimplemented toString()!";
    }

    /**
     * Returns the dynamic properties of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port, by means of the
     * {@link ibis.ipl.ReceivePort#setProperty(String, Object)
     * setProperty} method.
     */
    public Map properties() {
        return props;
    }

    /**
     * Sets a number of dynamic properties of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port.
     */
    public void setProperties(Map properties) {
        props = properties;
    }

    /**
     * Returns a dynamic property of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port, by means of the
     * {@link ibis.ipl.ReceivePort#setProperty(String, Object)
     * setProperty} method.
     */
    public Object getProperty(String key) {
        return props.get(key);
    }

    /**
     * Sets a dynamic property of
     * this port. The user can set some implementation-specific dynamic
     * properties of the port.
     */
    public void setProperty(String key, Object val) {
        props.put(key, val);
    }

    /**
     * Configures a socket according to user-specified properties. Currently,
     * the input buffer size and output buffer size can be set using the system
     * properties "ibis.util.socketfactory.InputBufferSize" and
     * "ibis.util.socketfactory.OutputBufferSize".
     * 
     * @param s
     *            the socket to be configured
     * @exception IOException
     *                when configuring fails for some reason.
     */
    public void tuneSocket() throws IOException {
        if (props != null) {
            String a = (String) props.get(ConnectionProperties.ISIZE);
            if (a != null) {
                logger.info("set recv buffer size to " + a);
                setReceiveBufferSize(Integer.parseInt(a));
            }
            a = (String) props.get(ConnectionProperties.OSIZE);
            if (a != null) {
                logger.info("set send buffer size to " + a);
                setSendBufferSize(Integer.parseInt(a));
            }
        }
        logger.info("TCP_NO_DELAY enabled");
        setTcpNoDelay(true);
    }
}