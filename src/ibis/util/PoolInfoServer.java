package ibis.util;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * A <code>PoolInfoServer</code> runs as a separate program or thread, and
 * collects information about nodes involved in a run. It is a bit like
 * the Ibis nameserver, but can be used by application programs.
 * An application run is identified by a key. Each node involved in the
 * run sends this key, the total number of hosts, and its cluster name
 * to the <code>PoolInfoServer</code>. When all nodes involved in the run
 * have sent their data, the <code>PoolInfoServer</code> sends back the
 * following information to each node:
 * <br>
 * - a rank number
 * <br>
 * - an array of cluster names, one for each node.
 * <br>
 * - an array of host addresses, one for each node.
 */
public class PoolInfoServer extends Thread {

    /**
     * Default port on which the <code>PoolInfoServer</code> is accepting
     * connections.
     */
    public static final int POOL_INFO_PORT = 9828;

    static final boolean DEBUG = false;

    static class RunInfo {
	int total_hosts;
	int removed_hosts;
	int connected_hosts;
	String [] host_clusters;
	InetAddress [] host_addresses;
	Socket [] host_sockets;
	DataInputStream [] host_inputs;
	String key;

	RunInfo(int nhosts, String key) {
	    total_hosts = nhosts;
	    connected_hosts = 0;
	    removed_hosts = 0;
	    host_clusters = new String[nhosts];
	    host_addresses = new InetAddress[nhosts];
	    host_sockets = new Socket[nhosts];
	    host_inputs = new DataInputStream[nhosts];
	    this.key = key;
	}

	boolean add(int nhosts,
		    int remove_doubles,
		    String cluster,
		    Socket socket,
		    DataInputStream in) throws IOException 
	{
	    if (nhosts != total_hosts) {
		System.err.println("PoolInfoServer: EEK, different total_hosts in PoolInfoServer, ignoring this connection...");
		in.close();
		socket.close();
		return false;
	    }

	    InetAddress addr = socket.getInetAddress();

	    if (remove_doubles != 0) {
		for (int i = 0; i < connected_hosts; i++) {
		    if (host_addresses[i].equals(addr)) {
			ObjectOutputStream out
			    = new ObjectOutputStream(socket.getOutputStream());
			out.writeInt(-1);
			out.close();
			in.close();
			socket.close();
			removed_hosts++;
			return connected_hosts + removed_hosts == total_hosts;
		    }
		}
	    }
	    if (DEBUG) {
		System.err.println("PoolInfoServer: Key " + key +
				   " Host " + connected_hosts +
				   " (" + socket.getInetAddress().getHostName() + ")" +
				   " has connected");
	    }
	    host_clusters[connected_hosts] = cluster;
	    host_addresses[connected_hosts] = addr;
	    host_sockets[connected_hosts] = socket;
	    host_inputs[connected_hosts] = in;
	    connected_hosts++;
	    return connected_hosts + removed_hosts == total_hosts;
	}

	void broadcast() throws IOException {
	    if (DEBUG) {
		System.err.println("PoolInfoServer: Key " + key +
				   ": All hosts have connected, " +
				   "now broadcasting host info...");
	    }

	    total_hosts -= removed_hosts;

	    for (int i = 0; i < total_hosts; i++) {
		ObjectOutputStream out
		    = new ObjectOutputStream(host_sockets[i].getOutputStream());
		out.writeInt(i); //give the node a rank
		out.writeInt(total_hosts);
		out.writeObject(host_clusters);
		out.writeObject(host_addresses);

		host_inputs[i].close();
		out.close();
		host_sockets[i].close();
	    }

	    if (DEBUG) {
		System.err.println("PoolInfoServer: Key " + key +
				   ": Broadcast done");
	    }
	}
    }

    private HashMap map = new HashMap(); 
    private ServerSocket serverSocket;
    private boolean singleRun;

    /**
     * Main program of the <code>PoolInfoServer</code>. 
     * The parameters accepted are:
     * <br>
     * <code>-single</code>&nbsp&nbsp&nbsp
     * a "single" run: exit as soon as no key is being processed anymore.
     * <br>
     * <code>-port</code> <i>portnum</i>&nbsp&nbsp&nbsp
     * accept connections on port <i>portnum</i> instead of on the default port.
     */
    public static void main(String [] argv) {
	boolean single = false;
	int serverPort = POOL_INFO_PORT;
	for (int i = 0; i < argv.length; i++) {
	    if (false) { /* do nothing */
	    } else if (argv[i].equals("-single")) {
		single = true;
	    } else if (argv[i].equals("-port")) {
		i++;
		try {
		    serverPort = Integer.parseInt(argv[i]);
		} catch(Exception e) {
		    System.err.println("invalid port");
		    System.exit(1);
		}
	    }
	    else {
		System.err.println("No such option: " + argv[i]);
		System.exit(1);
	    }
	}
	new PoolInfoServer(serverPort, single).run();
    }

    /**
     * Creates a <code>PoolInfoServer</code> that will accept
     * connections on the specified port.
     * @param port the port number to accept connections on.
     * @param single when <code>true</code>, the server returns as soon
     *               as no keys are being processed anymore.
     */
    public PoolInfoServer(int port, boolean single) {
	singleRun = single;
	try {
	    serverSocket = new ServerSocket(port);
	} catch (IOException e) {
	    throw new RuntimeException("Could not create server socket");
	}
    }

    /**
     * Creates a <code>PoolInfoServer</code> that will accept
     * connections on the default port.
     * @param single when <code>true</code>, the server returns as soon
     *               as no keys are being processed anymore.
     */
    public PoolInfoServer(boolean single) {
	this(TypedProperties.intProperty(PoolInfo.s_port,
					 POOL_INFO_PORT),
	     single);
    }

    /**
     * Main loop of the <code>PoolInfoServer</code>.
     * Accepts new connections and processes the information sent.
     * As soon as all data associated with a key is available, it
     * is broadcasted among all members of the run, and the key is
     * made available for future runs.
     */
    public void run() {
	/* we have to keep references to the input streams otherwise they can
	 * be closed by the garbage collector, and the socket will also be
	 * closed then */

	Socket socket;
	DataInputStream in;
	boolean stop = false;

	try {
	    while (! stop) {
		if (DEBUG) {
		    System.err.println("PoolInfoServer: starting run, " + 
			    "waiting for a host to connect...");
		}
		socket = serverSocket.accept();
		in = new DataInputStream(socket.getInputStream());
		String key = in.readUTF();
		int total_hosts = in.readInt();
		int remove_doubles = in.readInt();
		String cluster = in.readUTF();
		RunInfo r = (RunInfo) map.get(key);
		if (r == null) {
		    r = new RunInfo(total_hosts, key);
		    map.put(key, r);
		}

		if (r.add(total_hosts, remove_doubles, cluster, socket, in)) {
		    map.remove(key);
		    r.broadcast();
		    if (singleRun) {
			stop = map.isEmpty();
		    }
		}
	    }
	    serverSocket.close();
	} catch (IOException e) {
	    throw new RuntimeException("Got IO exception");
	}
    }
}
