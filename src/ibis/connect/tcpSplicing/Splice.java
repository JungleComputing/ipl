package ibis.connect.tcpSplicing;

import ibis.connect.util.MyDebug;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.ServerSocket;

public class Splice
{
    private static int serverPort = TypedProperties.intProperty("ibis.connect.spliceport", 20246);
    private static int hintPort = serverPort + 1;
    private static final int defaultSendBufferSize = 64*1024;
    private static final int defaultRecvBufferSize = 64*1024;
    private static NumServer server;
    private static final boolean setBufferSizes = ! TypedProperties.booleanProperty("ibis.connect.default.sizes");

    private Socket    socket = null;
    private String localHost = null;
    private int    localPort = -1;
    private InetSocketAddress localAddr;

    /**
     * A little server that takes care of assigning port numbers.
     * Such a server is needed when more than one JVM is running on the
     * same host and trying to set up TCPSplice connections. When this
     * is the case, there is a race when the same range of port numbers is used.
     * See the comment in the connectSplice routine.
     * The NumServer solves that.
     */
    private static class NumServer extends Thread {
	ServerSocket server;

	NumServer() {
	    try {
		server = new ServerSocket(serverPort);
	    } catch(Exception e) {
		// Assumption here is that another JVM has created this server.
		// System.out.println("Could not create server socket");
		// e.printStackTrace();
		server = null;
	    }
	}

	public void run() {
	    if (server == null) return;
	    while (true) {
		try {
		    Socket s = server.accept();
		    DataOutputStream out = new DataOutputStream(
					     new BufferedOutputStream(
						s.getOutputStream()));
		    out.writeInt(hintPort++);
		    out.flush();
		    out.close();
		    s.close();
		} catch(Exception e) {
		    System.out.println("Could not accept");
		    e.printStackTrace();
		}
	    }
	}
    }

    static {
	server = new NumServer();
	server.setDaemon(true);
	server.start();
    }

    public Splice()
	throws IOException
    {
	socket = new Socket();
	if (setBufferSizes) {
	    socket.setSendBufferSize(defaultSendBufferSize);
	    socket.setReceiveBufferSize(defaultRecvBufferSize);
	}
	try {
	    localHost = IPUtils.getLocalHostAddress().getCanonicalHostName();
	} catch(Exception e) {
	    localHost = "";
	}
    }

    public String getLocalHost()
    {
	return localHost;
    }

    private int newPort() {
	try {
	    Socket s = new Socket(IPUtils.getLocalHostAddress(), serverPort);
	    DataInputStream in = new DataInputStream(
				    new BufferedInputStream(
					s.getInputStream()));
	    int port = in.readInt();
	    in.close();
	    s.close();
	    return port;
	} catch(Exception e) {
	    System.err.println("Could not contact port number server: " + e);
	    e.printStackTrace();
	}
	return hintPort++;
    }

    public int findPort()
    {
	int port;
	do {
	    port = newPort();
	    try {
		localAddr = new InetSocketAddress(localHost, port);
	    } catch(Exception e) { throw new Error(e); }
	    try {
		MyDebug.trace("# Splice: trying port "+port);
		socket.bind(localAddr);
		localPort = port;
	    } catch(IOException e) {
		localPort = -1;
	    }
	} while(localPort == -1);
	MyDebug.trace("# Splice: found port "+localPort);
	return localPort;
    }

    public void close() {
	try {
	    socket.close();
	} catch(IOException dummy) {
	}
    }

    public Socket connectSplice(String rHost, int rPort)
	throws IOException
    {
	int i = 0;
	boolean connected = false;

	MyDebug.trace("# Splice: connecting to: "+rHost+":"+rPort);
	while(!connected)
	    {
		try {
		    InetSocketAddress remoteAddr = new InetSocketAddress(rHost, rPort);
		    socket.connect(remoteAddr);
		    connected = true;
		    MyDebug.trace("# Splice: success! i="+i);
		    MyDebug.trace("# Splice:   tcpSendBuffer="+socket.getSendBufferSize()+
				  "; tcpReceiveBuffer="+socket.getReceiveBufferSize());
		}
		catch (IOException e) {
		    try { socket.close(); } catch(IOException dummy) { /*ignore */ }
		    // There is a race here, if two JVM's running on the
		    // same node are both creating spliced sockets.
		    // After this close, another JVM might take this
		    // localAddr, and then the bind fails. (Ceriel)
		    // There is no race when using the NumServer. (Ceriel)
		    i++;
		    // re-init the socket
		    try {
			socket = new Socket();
			socket.setReuseAddress(true);
			if (setBufferSizes) {
			    socket.setSendBufferSize(defaultSendBufferSize);
			    socket.setReceiveBufferSize(defaultRecvBufferSize);
			}
			socket.bind(localAddr);
		    } catch(IOException f) { throw new Error(f); }
		}
	    }
	return socket;
    }
}

