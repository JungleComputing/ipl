package ibis.util;

import java.util.Properties;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
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

    private static boolean DEBUG = true;

    static class RunInfo {
	int total_hosts;
	int connected_hosts;
	String [] host_clusters;
	InetAddress [] host_addresses;
	Socket [] host_sockets;
	DataInputStream [] host_inputs;
	String key;

	RunInfo(int nhosts, String key) {
	    total_hosts = nhosts;
	    connected_hosts = 0;
	    host_clusters = new String[nhosts];
	    host_addresses = new InetAddress[nhosts];
	    host_sockets = new Socket[nhosts];
	    host_inputs = new DataInputStream[nhosts];
	    this.key = key;
	}

	boolean add(int nhosts,
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
	    if (DEBUG) {
		System.err.println("PoolInfoServer: Key " + key +
				   " Host " + connected_hosts +
				   " has connected");
	    }
	    host_clusters[connected_hosts] = cluster;
	    host_addresses[connected_hosts] = socket.getInetAddress();
	    host_sockets[connected_hosts] = socket;
	    host_inputs[connected_hosts] = in;
	    connected_hosts++;
	    return connected_hosts == total_hosts;
	}

	void broadcast() throws IOException {
	    if (DEBUG) {
		System.err.println("PoolInfoServer: Key " + key +
				   ": All hosts have connected, " +
				   "now broadcasting host info...");
	    }

	    for (int i = 0; i < total_hosts; i++) {
		ObjectOutputStream out
		    = new ObjectOutputStream(host_sockets[i].getOutputStream());
		out.writeInt(i); //give the node a rank
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
     * <pre>-single</pre>
     * <br>
     * a "single" run: exit as soon as no key is being processed anymore.
     * <br>
     * <pre>-port</pre> <i>portnum</i>
     * <br>
     * accept connections on port <i>portnum</i> instead of on the default port.
     */
    public static void main(String [] argv) {
	boolean single = false;
	int serverPort = POOL_INFO_PORT;
	for (int i = 0; i < argv.length; i++) {
	    if (false) {
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
	this(POOL_INFO_PORT, single);
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
		String cluster = in.readUTF();
		RunInfo r = (RunInfo) map.get(key);
		if (r == null) {
		    r = new RunInfo(total_hosts, key);
		    map.put(key, r);
		}

		if (r.add(total_hosts, cluster, socket, in)) {
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
