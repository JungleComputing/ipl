package ibis.connect.socketFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;

/**
 * The class <code>DummySocket</code> is a common superclass 
 * of all the ibis connect socket types. It is mostly there to
 * warn implementations for methods that are used but not
 * implemented.
 */
public class DummySocket extends Socket {

    public void bind(SocketAddress p) throws IOException {
	throw new RuntimeException("bind(SocketAddress) not implemented by " + this);
    }

    public  void connect(SocketAddress p, int timeout) throws IOException {
	throw new RuntimeException("connect(SocketAddress, int) not implemented by " + this);
    }

    public  void connect(SocketAddress p) throws IOException {
	throw new RuntimeException("connect(SocketAddress) not implemented by " + this);
    }

    public  void close() throws IOException {
	throw new RuntimeException("close() not implemented by " + this);
    }

    public  SocketChannel getChannel() {
	throw new RuntimeException("getChannel() not implemented by " + this);
    }

    public  InetAddress getInetAddress() {
	throw new RuntimeException("getInetAddress() not implemented by " + this);
    }

    public  InputStream getInputStream() throws IOException {
	throw new RuntimeException("getInputStream() not implemented by " + this);
    }

    public  boolean getKeepAlive() throws SocketException {
	throw new RuntimeException("getKeepAlive() not implemented by " + this);
    }

    public  InetAddress getLocalAddress() {
	throw new RuntimeException("getLocalAddress() not implemented by " + this);
    }

    public  int getLocalPort() {
	throw new RuntimeException("getLocalPort() not implemented by " + this);
    }

    public  SocketAddress getLocalSocketAddress() {
	throw new RuntimeException("getLocalSocketAddress() not implemented by " + this);
    }

    public boolean getOOBInline() throws SocketException {
	throw new RuntimeException("getOOBInline() not implemented by " + this);
    }

    public  OutputStream getOutputStream() throws IOException {
	throw new RuntimeException("getOutputStream() not implemented by " + this);
    }

    public  int getPort() {
	throw new RuntimeException("getPort() not implemented by " + this);
    }

    public  int getReceiveBufferSize() throws SocketException {
	throw new RuntimeException("getReceiveBufferSize() not implemented by " + this);
    }

    public  SocketAddress getRemoteSocketAddress() {
	throw new RuntimeException("getRemoteSocketAddress() not implemented by " + this);
    }

    public boolean getReuseAddress() throws SocketException {
	throw new RuntimeException("getReuseAddress() not implemented by " + this);
    }

    public  int getSendBufferSize() throws SocketException {
	throw new RuntimeException("getSendBufferSize() not implemented by " + this);
    }

    public  int getSoLinger() throws SocketException {
	throw new RuntimeException("getSoLinger() not implemented by " + this);
    }

    public  int getSoTimeout() throws SocketException {
	throw new RuntimeException("getSoTimeout() not implemented by " + this);
    }

    public boolean getTcpNoDelay() throws SocketException {
	throw new RuntimeException("getTcpNoDelay() not implemented by " + this);
    }

    public int getTrafficClass() throws SocketException {
	throw new RuntimeException("getTrafficClass() not implemented by " + this);
    }

    public boolean isBound() {
	throw new RuntimeException("isBound() not implemented by " + this);
    }

    public boolean isClosed() {
	throw new RuntimeException("isClosed() not implemented by " + this);
    }

    public boolean isConnected() {
	throw new RuntimeException("isConnected() not implemented by " + this);
    }

    public boolean isInputShutdown() {
	throw new RuntimeException("isInputShutdown() not implemented by " + this);
    }

    public boolean isOutputShutdown() {
	throw new RuntimeException("isOutputShutdown() not implemented by " + this);
    }

    public void sendUrgentData(int data) throws IOException {
	throw new RuntimeException("sendUrgentData(int) not implemented by " + this);
    }

    public void setKeepAlive(boolean on) throws SocketException {
	throw new RuntimeException("setKeepAlive(boolean) not implemented by " + this);
    }

    public void setOOBInline(boolean on) throws SocketException {
	throw new RuntimeException("setOOBInline(boolean) not implemented by " + this);
    }

    public void setReceiveBufferSize(int sz) throws SocketException {
	throw new RuntimeException("setReceiveBufferSize(int) not implemented by " + this);
    }

    public void setReuseAddress(boolean on) throws SocketException {
	throw new RuntimeException("setReuseAddress(boolean) not implemented by " + this);
    }

    public void setSendBufferSize(int sz) throws SocketException {
	throw new RuntimeException("setSendBufferSize(int) not implemented by " + this);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
	throw new RuntimeException("setSoLinger(boolean, int) not implemented by " + this);
    }

    public void setSoTimeout(int t) throws SocketException {
	throw new RuntimeException("setSoTimeout(int) not implemented by " + this);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
	throw new RuntimeException("setTcpNoDelay(boolean) not implemented by " + this);
    }

    public void setTrafficClass(int tc) throws SocketException {
	throw new RuntimeException("setTrafficClass(int) not implemented by " + this);
    }

    public void shutdownInput() throws IOException {
	throw new RuntimeException("shutdownInput() not implemented by " + this);
    }

    public void shutdownOutput() throws IOException {
	throw new RuntimeException("shutdownOutput() not implemented by " + this);
    }

    public String toString() {
	throw new RuntimeException("toString() not implemented by " + this);
    }
}
