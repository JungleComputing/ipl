package ibis.connect.tcpSplicing;

import ibis.connect.util.MyDebug;
import ibis.util.IPUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

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
	    localHost = IPUtils.getLocalHostAddress().getCanonicalHostName();
	} catch(Exception e) {
	    localHost = "";
	}
	MyDebug.trace("# Splice: creating splice on host "+localHost);
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
			MyDebug.trace("# Splice: trying port "+port);
			socket.bind(localAddr);
			localPort = port;
		    }
	    } catch(IOException e) {
		localPort = -1;
		port++;
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

