package ibis.connect.controlHub;

import java.util.List;
import java.util.Map;
import java.util.Hashtable;
import java.util.LinkedList;

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
	throws IOException{
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

		if(action == HubProtocol.DESTROY) {
		    /* packet for the hub itself */
		    nodeRunning = false;
		} else {
		    /* packet to forward */
		    if(node == null) {
			System.out.println("# ControlHub: node not found: "+destHost);
		    } else {
			node.sendPacket(packet);
		    }
		}

	    } catch(EOFException e) {
		System.out.println("# ControlHub: EOF detected for "+hostname);
		nodeRunning = false;
	    } catch(SocketException e) {
		System.out.println("# ControlHub: error detected for "+hostname+"; wire closed.");
		nodeRunning = false;
	    } catch(Exception e) {
		throw new Error(e);
	    }
	}
	ControlHub.unregisterNode(hostname);
    }
}

public class ControlHub
{
    private static Map nodes = new Hashtable(); // Hashtable of Ibisnodes; hash key is canonical hostname of node
    private static int nodesNum = 0;
    static final int port = 9827;

    public static void registerNode(String nodename, Object node) {
	nodes.put(nodename.toLowerCase(), node);
	nodesNum++;
    }
    public static void unregisterNode(String nodename) {
	nodes.remove(nodename.toLowerCase());
	nodesNum--;
    }
    public static Object resolveNode(String nodename) {
	return nodes.get(nodename.toLowerCase());
    }

    public static void main(String[] arg) {
	try {
	    ServerSocket server = new ServerSocket(port);
	    System.out.println("\n# ControlHub: listening on " +
			       InetAddress.getLocalHost().getHostName()+ ":" +
			       server.getLocalPort());
	    while(true)	{
		System.out.println("# ControlHub: ready- "+nodesNum+" nodes currently connected");
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
}
