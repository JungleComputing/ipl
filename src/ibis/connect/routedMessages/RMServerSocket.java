package ibis.connect.routedMessages;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;

import ibis.connect.util.MyDebug;

public class RMServerSocket extends ServerSocket
{
    private HubLink hub = null;
    private int serverPort = -1;
    
    private int     requestPort  = -1;
    private String  requestHost  = null;
    private boolean socketOpened = false;

    public RMServerSocket(int port, int backlog, InetAddress addr)
	throws IOException
    {
	hub = HubLinkFactory.getHubLink();
	if(port == 0) {
		serverPort = hub.newPort();
	} else {
	    serverPort = port;
	}
	socketOpened = true;
	hub.addServer(this, serverPort);
	MyDebug.out.println("# RMServerSocket() addr="+addr+"; port="+port);
    }

    public InetAddress getInetAddress()
    {
	InetAddress addr = null;
	try { addr = InetAddress.getLocalHost(); } catch(Exception e) { throw new Error(e); }
	MyDebug.out.println("# RMServerSocket.getInetAddress() addr="+addr);
	return addr;
    }
    public int getLocalPort()
    {
	MyDebug.out.println("# RMServerSocket.getLocalPort() port="+serverPort);
	return serverPort;
    }

    public Socket accept()
	throws IOException
    {
	Socket s = null;
	MyDebug.out.println("# RMServerSocket.accept()- waiting...");
	hub = HubLinkFactory.getHubLink();
	synchronized(this)
	    {
		while(requestHost == null)
		    {
			if(!socketOpened)
			    throw new SocketException();
			try { 
			    this.wait();
			} catch(InterruptedException e) { /* ignore */ }
		    }
		
		int localPort = hub.newPort();
		MyDebug.out.println("# RMServerSocket.accept()- unlocked; from port="+requestPort+
				   "; host="+requestHost);
		s = new RMSocket(requestHost, requestPort, localPort);
		MyDebug.out.println("# RMServerSocket.accept()- new RMSocket created on port="+localPort+"- Sending ACK.");
		hub.sendPacket(requestHost, new HubProtocol.HubPacketAccept(requestPort, hub.localHostName, localPort));
		requestHost = null;
		requestPort = -1;
	    }
	return s;
    }

    public synchronized void close()
	throws IOException
    {
	MyDebug.out.println("# RMServerSocket.close()");
	socketOpened = false;
	this.notifyAll();
	hub.removeServer(serverPort);
    }

    /* Method for the HubLink to feed us with new incoming connections
     * returns: true=ok; false=connection refused
     */
    protected synchronized boolean enqueueConnect(String clientHost, int clientPort)
    {
	boolean accepted = (requestHost == null);
	if(accepted) {
	    requestPort = clientPort;
	    requestHost = clientHost;
	    accepted = true;
	    this.notify();
	}
	return accepted;
    }
}
