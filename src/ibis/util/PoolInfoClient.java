package ibis.util;

import java.io.DataOutputStream;
import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The <code>PoolInfoClient</code> class provides a utility for finding out
 * information about the nodes involved in a closed-world run.
 * It is a client for a
 * {@link ibis.util.PoolInfoServer PoolInfoServer}.
 * The best way to access pool information is to obtain a
 * {@link ibis.util.PoolInfo PoolInfo} by means of the
 * {@link ibis.util.PoolInfo#createPoolInfo PoolInfo.createPoolInfo} static
 * method. This is the most flexible, only creating a
 * <code>PoolInfoClient</code> when a more knowledgeable <code>PoolInfo</code>
 * cannot be created.
 * <br>
 * The <code>PoolInfoClient</code> class depends on the following
 * system properties:
 * <br>
 * <pre>ibis.pool.total_hosts</pre>
 * must be present, and contain the total number of hosts involved in the run.
 * <br>
 * <pre>ibis.pool.cluster</pre>
 * must contain the cluster name of the current node. If not present,
 * "unknown" is used.
 * <br>
 * <pre>ibis.pool.key</pre>
 * must contain the key identifying the current run. If not present,
 * the <code>ibis.name_server.key</code> is tried. If that is not present
 * either, "unknown" is used.
 * <br>
 * <pre>ibis.pool.server.port</pre>
 * must contain the port number on which the <code>PoolInfoServer</code>
 * is accepting connections. If not present, <code>ibis.name_server.port</code>
 * is tried. If present, 1 is added and that is used as port number. If not,
 * the default is used.
 * <br>
 * <pre>ibis.pool.server.host</pre>
 * must contain the hostname of the host on which the
 * <code>PoolInfoServer</code> runs. If not present,
 * <code>ibis.name_server.host</code> is tried.
 * One of the two system properties must be defined.
 * <br>
 */
public class PoolInfoClient extends PoolInfo {
    private String [] host_clusters;

    private static PoolInfoClient instance;

    /**
     * For testing purposes: a main program.
     */
    public static void main(String [] argv) {
	try {
	    PoolInfoClient test = create();
	    System.out.println(test.toString());
	} catch(Exception e) {
	    System.out.println("Got exception: ");
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /**
     * Creates a <code>PoolInfoClient</code> if not already present.
     * @return the <code>PoolInfoClient</code>.
     */
    public static PoolInfoClient create() {
	if (instance == null) {
	    instance = new PoolInfoClient();
	}
	return instance;
    }

    private PoolInfoClient() {
	super(0);

	InetAddress serverAddress;

	total_hosts = TypedProperties.intProperty(s_total);
	int remove_doubles = TypedProperties.booleanProperty(s_single) ? 1 : 0;

	int serverPort = TypedProperties.intProperty(s_port, -1);
	if (serverPort == -1) {
	    serverPort = TypedProperties.intProperty("ibis.name_server.port", -1);
	    if (serverPort == -1) {
		serverPort = PoolInfoServer.POOL_INFO_PORT;
	    } else {
		serverPort++;
	    }
	}
	String serverName = TypedProperties.stringProperty(s_host);
	if (serverName == null) {
	    serverName = TypedProperties.stringProperty("ibis.name_server.host");
	    if (serverName == null) {
		throw new RuntimeException("property " + s_host + " is not specified");
	    }
	}
	String key = TypedProperties.stringProperty(s_key);
	if (key == null) {
	    key = TypedProperties.stringProperty("ibis.name_server.key");
	    if (key == null) {
		// System.err.println("Warning: " + s_key + " property not set, using 'unknown'");
		key = "unknown";
	    }
	}
	try {
	    serverAddress = InetAddress.getByName(serverName);
	} catch (UnknownHostException e) {
	    throw new RuntimeException("cannot get ip of pool server");
	}
	Socket socket = null;
	int cnt = 0;
	while (socket == null) {
	    try {
		cnt++;
		socket = new Socket(serverAddress, serverPort);
	    } catch(Exception e) {
		if (cnt == 60) {
		    System.err.println("Could not connect to PoolInfoServer after 60 seconds; giving up ...");
		    throw new RuntimeException("Got exception: " + e);
		}
		try {
		    Thread.sleep(1000);
		} catch(Exception ex) {
		}
	    }
	}
	try {
	    DataOutputStream out
		= new DataOutputStream(socket.getOutputStream());
	    out.writeUTF(key);
	    out.writeInt(total_hosts);
	    out.writeInt(remove_doubles);
	    out.writeUTF(clusterName);
	    out.flush();

	    ObjectInputStream in
		= new ObjectInputStream(socket.getInputStream());
	    host_number = in.readInt();
	    if (host_number == -1) {
		in.close();
		out.close();
		socket.close();
		throw new RuntimeException("This node is already registered");
	    }
	    total_hosts = in.readInt();
	    host_clusters = (String []) in.readObject();
	    hosts = (InetAddress []) in.readObject();
	    host_names = new String[total_hosts];
	    for (int i = 0; i < total_hosts; i++) {
		host_names[i] = hosts[i].getHostName();
	    }

	    in.close();
	    out.close();
	    socket.close();
	} catch (Exception e) {
	    throw new RuntimeException("Got exception: " + e);
	}

	if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
	    throw new RuntimeException("Sanity check on host numbers failed!");
	}
    }

    public String clusterName() {
	return host_clusters[host_number];
    }

    public String clusterName(int rank) {
	return host_clusters[rank];
    }

    public String[] clusterNames() {
	return (String[]) host_clusters.clone();
    }

    public String toString() {
	String result = "pool info: size = " + total_hosts +
	    "; my rank is " + host_number + "; host list:\n";
	for (int i = 0; i < total_hosts; i++) {
	    result += i + ": address= " + hosts[i] + 
		" cluster=" + host_clusters[i] + "\n";
	}
	return result;
    }
}
