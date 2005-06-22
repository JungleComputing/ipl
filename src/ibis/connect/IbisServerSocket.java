/*
 * Created on Apr 20, 2005 by rob
 */
package ibis.connect;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.util.Map;

/**
 * @author rob
 */

/**
 * The class <code>IbisSocket</code> is a common superclass of all the ibis
 * connect socket types. It is mostly there to warn implementations for methods
 * that are used but not implemented.
 */
public abstract class IbisServerSocket extends ServerSocket {

    protected Map props;
    
    private IbisServerSocket() throws IOException {
    }

    protected IbisServerSocket(Map p) throws IOException {
        props = p;
    }

    public Socket accept() throws IOException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public void bind(SocketAddress arg0, int arg1) throws IOException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public void bind(SocketAddress arg0) throws IOException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public void close() throws IOException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public ServerSocketChannel getChannel() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public InetAddress getInetAddress() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public int getLocalPort() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public SocketAddress getLocalSocketAddress() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public boolean getReuseAddress() throws SocketException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public synchronized int getSoTimeout() throws IOException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public boolean isBound() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public boolean isClosed() {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public synchronized void setReceiveBufferSize(int arg0)
            throws SocketException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public void setReuseAddress(boolean arg0) throws SocketException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public synchronized void setSoTimeout(int arg0) throws SocketException {
        throw new RuntimeException("operation not implemented by " + this);
    }

    public String toString() {
        throw new RuntimeException("operation not implemented by " + this);
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
 }