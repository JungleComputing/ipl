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

/** Incarnates a thread dedicated to HubWire management 
 * towards a given node.
 */
class NodeManager extends Thread
{
    private String hostname;
    private HubProtocol.HubWire wire;

    NodeManager(Socket s) 
	throws IOException, ClassNotFoundException {
	wire = new HubProtocol.HubWire(s);
	hostname = wire.getPeerName();
    }
    protected void sendPacket(HubProtocol.HubPacket p) 
	throws IOException {
	wire.sendMessage(hostname, p);
    }
    public void run() {
	boolean nodeRunning = true;
	ControlHub.registerNode(hostname, this);
	while(nodeRunning) {
	    try {
		HubProtocol.HubPacket packet = wire.recvPacket();
		int      action = packet.getType();
		String destHost = packet.getHost();
		NodeManager  node = (NodeManager)ControlHub.resolveNode(destHost);
		if(node == null) {
		  System.err.println("# ControlHub: node not found: "+destHost);
		} else {
		  node.sendPacket(packet);
		}
	    } catch(EOFException e) {
		System.err.println("# ControlHub: EOF detected for "+hostname);
		nodeRunning = false;
	    } catch(SocketException e) {
		System.err.println("# ControlHub: error detected for "+hostname+"; wire closed.");
		nodeRunning = false;
	    } catch(Exception e) {
		throw new Error(e);
	    }
	}
	ControlHub.unregisterNode(hostname);
    }
}

public class ControlHub extends Thread
{
    private static Map nodes = new Hashtable(); // Hashtable of Ibisnodes; hash key is canonical hostname of node
    private static int nodesNum = 0;
    public static final int defaultPort = 9827;

    private static void showCount() {
	System.err.println("# ControlHub: "+nodesNum+" nodes currently connected");
    }

    public static void registerNode(String nodename, Object node) {
	System.err.println("# ControlHub: new connection from "+nodename);
	nodes.put(nodename.toLowerCase(), node);
	nodesNum++;
	showCount();
    }

    public static void unregisterNode(String nodename) {
	nodes.remove(nodename.toLowerCase());
	nodesNum--;
	showCount();
    }

    public static Object resolveNode(String nodename) {
	return nodes.get(nodename.toLowerCase());
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
