package ibis.connect.controlHub;

import java.util.List;
import java.util.Map;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Properties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;

import ibis.connect.routedMessages.HubProtocol;
import ibis.connect.util.MyDebug;

/** Incarnates a thread dedicated to HubWire management 
 * towards a given node.
 */
class NodeManager extends Thread
{
    private String hostname;
    private HubProtocol.HubWire wire;
    private int hostport;

    NodeManager(Socket s) 
	throws IOException, ClassNotFoundException {
	wire = new HubProtocol.HubWire(s);
	hostname = wire.getPeerName();
	hostport = wire.getPeerPort();
    }
    protected void sendPacket(String host, int port, HubProtocol.HubPacket p) 
	throws IOException{
	wire.sendMessage(host, port, p);
    }
    public void run() {
	boolean nodeRunning = true;
	ControlHub.registerNode(hostname, hostport, this);
	while(nodeRunning) {
	    try {
		HubProtocol.HubPacket packet = wire.recvPacket();

		int      action = packet.getType();
		String destHost = packet.getHost();
		int    destPort	= packet.getPort();
		boolean send = true;
		HubProtocol.HubPacketClose cls = null;

		switch(action) {
		case HubProtocol.GETPORT: {
		    /* packet for the hub itself */
		    HubProtocol.HubPacketGetPort p = (HubProtocol.HubPacketGetPort) packet;
		    int prt = ControlHub.checkPort(hostname, hostport, p.proposedPort);
		    sendPacket(hostname, hostport, new HubProtocol.HubPacketPutPort(prt));
		    send = false;
		    }
		    break;

		case HubProtocol.CONNECT: {
		    /* need to figure out a destPort. */
		    HubProtocol.HubPacketConnect p = (HubProtocol.HubPacketConnect) packet;
		    destPort = ControlHub.resolvePort(destHost, p.serverPort);
		    if (destPort == -1) {
			sendPacket(destHost, destPort, new HubProtocol.HubPacketReject(p.clientPort, destHost));
		    }
		    break;
		    }

		case HubProtocol.CLOSE: {
		    /* need to remove the port.
		     * Postpone the removal until after the send!
		     */
		    cls = (HubProtocol.HubPacketClose) packet;
		    }

		default:
		    break;
		}

		if (send) {
		    NodeManager  node = (NodeManager)ControlHub.resolveNode(destHost, destPort);
		    /* packet to forward */
		    if(node == null) {
			System.err.println("# ControlHub: node not found: "+destHost + ":" + destPort);
		    } else {
			/* replaces the destination with the sender. */
			node.sendPacket(hostname, hostport, packet);
			if (cls != null) {
			    ControlHub.removePort(destHost, destPort, cls.closePort);
			}
		    }
		}
	    } catch(EOFException e) {
		System.err.println("# ControlHub: EOF detected for "+hostname+":"+hostport);
		nodeRunning = false;
	    } catch(SocketException e) {
		System.err.println("# ControlHub: error detected for "+hostname+":"+hostport+"; wire closed.");
		nodeRunning = false;
	    } catch(Exception e) {
		throw new Error(e);
	    }
	}
	ControlHub.unregisterNode(hostname, hostport);
    }
}

class Node {
    private int portno;
    private String hostname;

    public Node(int portno, String hostname) {
	this.portno = portno;
	this.hostname = hostname;
    }

    public boolean equals(Object o) {
	if (o instanceof Node) {
	    Node po = (Node) o;
	    return portno == po.portno && hostname.equals(po.hostname);
	}
	return false;
    }

    public int hashCode() {
	return hostname.hashCode();
    }
}

public class ControlHub extends Thread
{
    private static Map nodes = new Hashtable(); // Hashtable of Ibisnodes; hash key is canonical hostname of node
    private static Map portNodeMap = new Hashtable();
    private static int nodesNum = 0;
    public static final int defaultPort = 9827;

    private static void showCount() {
	System.err.println("# ControlHub: "+nodesNum+" nodes currently connected");
    }

    public static void registerNode(String nodename, int nodeport, Object node) {
	System.err.println("# ControlHub: new connection from "+nodename+":"+nodeport);
	nodes.put(new Node(nodeport, nodename.toLowerCase()), node);
	nodesNum++;
	showCount();
    }

    public static void unregisterNode(String nodename, int nodeport) {
	nodes.remove(new Node(nodeport, nodename.toLowerCase()));
	nodesNum--;
	showCount();
    }

    public static Object resolveNode(String nodename, int nodeport) {
	return nodes.get(new Node(nodeport, nodename.toLowerCase()));
    }

    public static int checkPort(String hostname, int hostport, int portno) {
	Object o = portNodeMap.get(hostname);
	Hashtable h;
	if (o == null) {
	    h = new Hashtable();
	    portNodeMap.put(hostname, h);
	}
	else h = (Hashtable) o;
	if (portno == 0) {
	    int i = 1;
	    while (h.containsKey(new Integer(i))) {
		i++;
	    }
	    h.put(new Integer(i), new Integer(hostport));
	    MyDebug.trace("# ControlHub: giving portno " + i + " to " +
			    hostname + ":" + hostport);
	    return i;
	}
	if (h.containsKey(new Integer(portno))) {
	    MyDebug.trace("# ControlHub: could not give portno " + portno +
			    " to " + hostname + ":" + hostport);
	    return -1;
	}
	MyDebug.trace("# ControlHub: giving portno " + portno + " to " +
			hostname + ":" + hostport);
	h.put(new Integer(portno), new Integer(hostport));
	return portno;
    }

    public static void removePort(String hostname, int hostport, int portno) {
	Object o = portNodeMap.get(hostname);
	Hashtable h;
	if (o == null) return;
	h = (Hashtable) o;
	h.remove(new Integer(portno));
	MyDebug.trace("# ControlHub: removing portno " + portno + " of " +
				hostname + ":" + hostport);
    }

    public static int resolvePort(String hostname, int portno) {
	Object o = portNodeMap.get(hostname);
	Hashtable h;
	if (o == null) {
	    System.err.println("# ControlHub: could not resolve " + portno +
				" for host " + hostname);
	    return -1;
	}
	h = (Hashtable) o;
	o = h.get(new Integer(portno));
	if (o == null) {
	    System.err.println("# ControlHub: could not resolve " + portno +
				" for host " + hostname);
	    return -1;
	}
	return ((Integer) o).intValue();
    }

    public void run() {
	int port = defaultPort;
	try {
	    Properties p = System.getProperties();
	    String portString = p.getProperty("ibis.connect.hub_port");
	    if(portString != null){
		port = Integer.parseInt(portString);
	    }
	    ServerSocket server = new ServerSocket(port);
	    System.err.println("\n# ControlHub: listening on " +
			       InetAddress.getLocalHost().getHostName()+ ":" +
			       server.getLocalPort());
	    while(true)	{
		Socket s = server.accept();
		try {
		    NodeManager node = new NodeManager(s);
		    node.start();
		} catch(Exception e) { /* ignore */ }
	    }
	} catch(Exception e) {
	    throw new Error(e);
	}
    }

    public static void main(String[] arg) {
	new ControlHub().run();
    }
}
