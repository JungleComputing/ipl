package ibis.connect.routedMessages;

import ibis.connect.util.MyDebug;
import ibis.util.IPUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class RMServerSocket extends ServerSocket
{
    private HubLink hub = null;
    private int serverPort = -1;
    private InetAddress addr;
    
    private boolean socketOpened = false;
    private ArrayList requests = new ArrayList();

    private static class Request {
	int requestPort;
	String requestHost;
	int requestHubPort;

	Request(int clientPort, String clientHost, int clienthubport) {
	    requestPort = clientPort;
	    requestHost = clientHost;
	    requestHubPort = clienthubport;
	}
    }

    public RMServerSocket(int port, int backlog, InetAddress addr)
	throws IOException
    {
	hub = HubLinkFactory.getHubLink();
	serverPort = hub.newPort(port);
	this.addr = addr;
	socketOpened = true;
	hub.addServer(this, serverPort);
	MyDebug.out.println("# RMServerSocket() addr="+addr+"; port="+serverPort);
    }

    public InetAddress getInetAddress()
    {
	InetAddress addr = this.addr;
	if (addr == null) {
	    try { addr = IPUtils.getLocalHostAddress(); } catch(Exception e) { throw new Error(e); }
	}
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
	Request r = null;
	synchronized(this)
	    {
		while(requests.size() == 0)
		    {
			if(!socketOpened)
			    throw new SocketException();
			try { 
			    this.wait();
			} catch(InterruptedException e) { /* ignore */ }
		    }
		r = (Request) requests.remove(0);
	    }

	int localPort = hub.newPort(0);
	MyDebug.out.println("# RMServerSocket.accept()- unlocked; from port="
			    +r.requestPort+ "; host="+r.requestHost);
	s = new RMSocket(r.requestHost, r.requestPort, localPort, r.requestHubPort);
	MyDebug.out.println("# RMServerSocket.accept()- new RMSocket created on port="+localPort+"- Sending ACK.");
	hub.sendPacket(r.requestHost, r.requestHubPort, new HubProtocol.HubPacketAccept(r.requestPort, hub.localHostName, localPort));
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
    protected synchronized void enqueueConnect(String clientHost, int clientPort, int clienthubport)
    {
	requests.add(new Request(clientPort, clientHost, clienthubport));
	if (requests.size() == 1) {
	    this.notify();
	}
    }
}
