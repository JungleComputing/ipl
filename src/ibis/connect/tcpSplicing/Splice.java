package ibis.connect.tcpSplicing;

import ibis.connect.util.MyDebug;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Splice
{
    private static int hintPort = 20247;
    private static final int defaultSendBufferSize = 64*1024;
    private static final int defaultRecvBufferSize = 64*1024;

    private Socket    socket = null;
    private String localHost = null;
    private int    localPort = -1;
    private InetSocketAddress localAddr;

    public Splice()
	throws IOException
    {
	socket = new Socket();
	socket.setSendBufferSize(defaultSendBufferSize);
	socket.setReceiveBufferSize(defaultRecvBufferSize);
	try {
	    localHost = InetAddress.getLocalHost().getCanonicalHostName();
	} catch(Exception e) {
	    localHost = "";
	}
	MyDebug.out.println("# Splice: creating splice on host "+localHost);
    }

    public String getLocalHost()
    {
	return localHost;
    }

    public int findPort()
    {
	int port = hintPort++;
	do {
	    try {
		localAddr = new InetSocketAddress(localHost, port);
	    } catch(Exception e) { throw new Error(e); }
	    try {
		synchronized(Splice.class)
		    {
			MyDebug.out.println("# Splice: trying port "+port);
			socket.bind(localAddr);
			localPort = port;
		    }
	    } catch(IOException e) {
		localPort = -1;
		port++;
	    }
	} while(localPort == -1);
	MyDebug.out.println("# Splice: found port "+localPort);
	return localPort;
    }

    public Socket connectSplice(String rHost, int rPort)
	throws IOException
    {
	int i = 0;
	boolean connected = false;

	MyDebug.out.println("# Splice: connecting to: "+rHost+":"+rPort);
	while(!connected)
	    {
		try {
		    InetSocketAddress remoteAddr = new InetSocketAddress(rHost, rPort);
		    socket.connect(remoteAddr);
		    connected = true;
		    System.out.println("# Splice: success! i="+i);
		    System.out.println("# Splice:   tcpSendBuffer="+socket.getSendBufferSize()+
					"; tcpReceiveBuffer="+socket.getReceiveBufferSize());
		}
		catch (IOException e) {
		    synchronized(Splice.class) // disallow other spliced sockets creation
			{
			    try { socket.close(); } catch(IOException dummy) { /*ignore */ }
			    i++;
			    // re-init the socket
			    try {
				socket = new Socket();
				socket.setReuseAddress(true);
				socket.setSendBufferSize(defaultSendBufferSize);
				socket.setReceiveBufferSize(defaultRecvBufferSize);
				socket.bind(localAddr);
			    } catch(IOException f) { throw new Error(f); }
			}
		}
	    }
	return socket;
    }
}

