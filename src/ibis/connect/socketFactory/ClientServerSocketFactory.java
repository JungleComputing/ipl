package ibis.connect.socketFactory;

import java.io.IOException;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public interface ClientServerSocketFactory
{
    public Socket       createClientSocket(InetAddress addr, int port)
	throws IOException;
    public ServerSocket createServerSocket(InetSocketAddress addr, int backlog)
	throws IOException;
}
