package ibis.connect.routedMessages;

import java.net.Socket;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;

import ibis.connect.util.MyDebug;

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
    public static final int DESTROY = 6; // destroy the hub
    public static final int GETADDR = 7; 

    public static class WireException extends IOException {
	public WireException() { super(); }
    }

    public static class HubWire
    {
	private Socket             socket;
	private ObjectInputStream  in;
	private ObjectOutputStream out;
	private String             peerName;
	private String             localHostName;
	private boolean            hubConnected = false;
	
	public HubWire(Socket s) throws IOException, ClassNotFoundException {
	    socket = s;
	    localHostName = s.getLocalAddress().getCanonicalHostName();
	    out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream(), 4096));
	    out.writeObject(localHostName);
	    out.flush();
	    in = new ObjectInputStream(new BufferedInputStream(s.getInputStream(), 4096));
	    peerName = ((String)in.readObject()).toLowerCase();
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
	public String getLocalName() { return localHostName; }
	public HubPacket recvPacket()
	    throws IOException, ClassNotFoundException {
	    HubPacket p = null;
	    synchronized(in) {
		int      action = in.readInt();
		String destHost = (String)in.readObject();
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
		default:
		    throw new Error("Received unknown type of HubPacket: "+action);
		}
		p.destHost = destHost;
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
		out.writeObject(p.destHost);
		p.send(out);
		out.flush();
	    }
	}
	public void sendMessage(String destHost, HubPacket p)
	    throws IOException {
	    p.destHost = destHost;
	    sendMessage(p);
	}
    }

    public static abstract class HubPacket {
	protected String destHost;
	public    String getHost() { return destHost; }
	abstract public int getType();
	abstract public void send(ObjectOutputStream out) throws IOException;
	// static HubPacket recv(ObjectInputStream in) throws IOException, ClassNotFoundException;
    }
    public static class HubPacketConnect extends HubPacket {
	int    serverPort;
	String clientHost;
	int    clientPort;
	public int getType() { return CONNECT; }
	HubPacketConnect(int serverPort, String clientHost, int clientPort) {
	    this.serverPort = serverPort;
	    this.clientHost = clientHost; 
	    this.clientPort = clientPort;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketConnect.send()- sending CONNECT");
	    out.writeInt(serverPort);
	    out.writeObject(clientHost);
	    out.writeInt(clientPort);
	}
	static public HubPacket recv(ObjectInputStream in) 
	    throws IOException, ClassNotFoundException {
	    int    serverPort = in.readInt();
	    String clientHost = (String)in.readObject();
	    int    clientPort = in.readInt();
	    return new HubPacketConnect(serverPort, clientHost, clientPort);
	}
    };

    public static class HubPacketAccept extends HubPacket {
	int    clientPort;
	String serverHost;
	int    servantPort;
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
	int    clientPort;
	String serverHost;
	public int getType() { return REJECT; }
	HubPacketReject(int clientPort, String serverHost) {
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
	int  port;
	public int getType() { return CLOSE; }
	HubPacketClose(int port) {
	    this.port = port;
	}
	public void send(ObjectOutputStream out) throws IOException {
	    MyDebug.out.println("# HubPacketClose.send()- sending CLOSE");
	    out.writeInt(port);
	}
	static public HubPacket recv(ObjectInputStream in)
	    throws IOException, ClassNotFoundException {
	    int port = in.readInt();
	    return new HubPacketClose(port);
	}
    }
}
