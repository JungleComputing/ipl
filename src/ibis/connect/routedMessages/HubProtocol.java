package ibis.connect.routedMessages;

import ibis.connect.util.MyDebug;

import ibis.util.IPUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/* On-the-wire protocol management for links between 
 * computing nodes and control hub. Used by the HubLink
 * manager and the ControlHub itself.
 */
public class HubProtocol
{
    public static final int CONNECT = 1; // connection request
    public static final int ACCEPT  = 2; // notification for an accepted connection
    public static final int REJECT  = 3; // notification for a refused connection
    public static final int DATA    = 4; // data packet
    public static final int CLOSE   = 5; // notification for socket close
    public static final int GETPORT = 6; // get port number from controlhub
    public static final int PUTPORT = 7; // received port number from controlhub

    public static class HubWire
    {
	private Socket             socket;
	private ObjectInputStream  in;
	private ObjectOutputStream out;
	private String             peerName;
	private int		   peerPort;
	private String             localHostName;
	private int		   localPort;
	private boolean            hubConnected = false;
	
	public HubWire(Socket s) throws IOException, ClassNotFoundException {
	    socket = s;
	    localHostName = IPUtils.getLocalHostAddress().getHostName();
	    localPort = s.getLocalPort();
	    out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream(), 4096));
	    out.writeObject(localHostName);
	    out.writeInt(s.getLocalPort());
	    out.flush();
	    in = new ObjectInputStream(new BufferedInputStream(s.getInputStream(), 4096));
	    peerName = ((String)in.readObject()).toLowerCase();
	    peerPort = in.readInt();
	    if(MyDebug.VERBOSE()) {
		String canonicalPeerName = s.getInetAddress().getCanonicalHostName().toLowerCase();
		String msg = "# HubWire: new hub wire- local: "+localHostName+"; remote: "+peerName;
		if(!canonicalPeerName.equals(peerName)) {
		    msg = msg + " (seen from hub: "+canonicalPeerName+":"+s.getPort()+")";
		}
		System.err.println(msg);
	    }
	    hubConnected = true;
	}
	public void close() throws IOException {
	    if(MyDebug.VERBOSE())
		System.err.println("# HubWire: closing wire...");
	    in.close();
	    out.flush();
	    out.close();
	    socket.close();
	    if(MyDebug.VERBOSE())
		System.err.println("# HubWire: closed.");
	}
	public String getPeerName() { return peerName; }
	public int getPeerPort() { return peerPort; }
	public String getLocalName() { return localHostName; }
	public int    getLocalPort() { return localPort; }
	public HubPacket recvPacket()
	    throws IOException, ClassNotFoundException {
	    HubPacket p = null;
	    synchronized(in) {
		int      action = in.readInt();
		String host = (String)in.readObject();
		int port = in.readInt();
		switch(action) {
		case CONNECT: p = HubPacketConnect.recv(in);
		    break;
		case ACCEPT:  p = HubPacketAccept.recv(in);
		    break;
		case REJECT:  p = HubPacketReject.recv(in);
		    break;
		case DATA:    p = HubPacketData.recv(in);
		    break;
		case CLOSE:   p = HubPacketClose.recv(in);
		    break;
		case PUTPORT: p = HubPacketPutPort.recv(in);
		    break;
		case GETPORT: p = HubPacketGetPort.recv(in);
		    break;
		default:
		    throw new Error("Received unknown type of HubPacket: "+action);
		}
		p.h = host;
		p.p = port;
		if(action != p.getType())
		    throw new Error("Internal error. Consistency check failed.");
	    }
	    return p;
	}
	public void sendMessage(HubPacket p)
	    throws IOException {
	    if(!hubConnected)
		throw new IOException("hub not connected");
	    synchronized(out) {
		out.writeInt(p.getType());
		out.writeObject(p.h);
		out.writeInt(p.p);
		p.send(out);
		out.flush();
	    }
	}
	public void sendMessage(String destHost, int destPort, HubPacket p)
	    throws IOException {
	    p.h = destHost;
	    p.p = destPort;
	    sendMessage(p);
	}
    }

    public static abstract class HubPacket {
	protected String h;
	protected int p;
	public    String getHost() { return h; }
	public    int    getPort() { return p; }
	abstract public int getType();
	abstract public void send(ObjectOutputStream out) throws IOException;
	// static HubPacket recv(ObjectInputStream in) throws IOException, ClassNotFoundException;
    }
    public static class HubPacketConnect extends HubPacket {
	public int    serverPort;
	public int    clientPort;
	public int getType() { return CONNECT; }
	HubPacketConnect(int serverPort, int clientPort) {
	    this.serverPort = serverPort;
	    this.clientPort = clientPort;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketConnect.send()- sending CONNECT to port " + serverPort + " on " + h + ":" + p);
	    out.writeInt(serverPort);
	    out.writeInt(clientPort);
	}
	static public HubPacket recv(ObjectInputStream in) 
	    throws IOException, ClassNotFoundException {
	    int    serverPort = in.readInt();
	    int    clientPort = in.readInt();
	    return new HubPacketConnect(serverPort, clientPort);
	}
    };

    public static class HubPacketAccept extends HubPacket {
	public int    clientPort;
	public String serverHost;
	public int    servantPort;
	public int getType() { return ACCEPT; }
	HubPacketAccept(int clientPort, String serverHost, int servantPort) {
	    this.clientPort  = clientPort;
	    this.serverHost  = serverHost;
	    this.servantPort = servantPort;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketAccept.send()- sending ACCEPT");
	    out.writeInt(clientPort);
	    out.writeObject(serverHost);
	    out.writeInt(servantPort);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int    clientPort  = in.readInt();
	    String serverHost  = (String)in.readObject();
	    int    servantPort = in.readInt();
	    return new HubPacketAccept(clientPort, serverHost, servantPort);
	}
    }

    public static class HubPacketReject extends HubPacket {
	public int    clientPort;
	public String serverHost;
	public int getType() { return REJECT; }
	public HubPacketReject(int clientPort, String serverHost) {
	    this.clientPort = clientPort;
	    this.serverHost = serverHost;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketReject.send()- sending REJECT");
	    out.writeInt(clientPort);
	    out.writeObject(serverHost);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int    clientPort = in.readInt();
	    String serverHost = (String)in.readObject();
	    return new HubPacketReject(clientPort, serverHost);
	}
    }

    public static class HubPacketGetPort extends HubPacket {
	public int    proposedPort;
	public int getType() { return GETPORT; }
	HubPacketGetPort(int port) {
	    proposedPort = port;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketGetPort.send()- sending GETPORT");
	    out.writeInt(proposedPort);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int    port = in.readInt();
	    return new HubPacketGetPort(port);
	}
    }

    public static class HubPacketPutPort extends HubPacket {
	public int    resultPort;
	public int getType() { return PUTPORT; }
	public HubPacketPutPort(int port) {
	    resultPort = port;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketPutPort.send()- sending PUTPORT");
	    out.writeInt(resultPort);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int    port = in.readInt();
	    return new HubPacketPutPort(port);
	}
    }

    public static class HubPacketData extends HubPacket {
	int    port;
	byte[] b;
	public int getType() { return DATA; }
	HubPacketData(int port, byte[] b) {
	    this.port = port;
	    this.b = b;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketData.send()- sending- port="+port+"; size="+b.length);
	    out.writeInt(port);
	    out.writeInt(b.length);
	    out.write(b);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int port = in.readInt();
	    int len = in.readInt();
	    byte[] b = new byte[len];
	    in.readFully(b);
	    return new HubPacketData(port, b);
	}
    }

    public static class HubPacketClose extends HubPacket {
	public int  closePort;
	public int  localPort;
	public int getType() { return CLOSE; }
	HubPacketClose(int port, int lport) {
	    closePort = port;
	    localPort = lport;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketConnect.send()- sending CLOSE of port " + closePort + " to " + h + ":" + p);
	    out.writeInt(closePort);
	    out.writeInt(localPort);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int port = in.readInt();
	    int lport = in.readInt();
	    return new HubPacketClose(port, lport);
	}
    }
}
