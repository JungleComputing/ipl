//package poolinfo;

import java.util.Properties;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class PoolInfoClient {

	private int total_hosts;
	private int host_number;
	private String [] host_clusters;
	private InetAddress [] host_addresses;

	private static PoolInfoClient instance;

	public static void main(String [] argv) {
		PoolInfoClient test = create();
		System.out.println(test.toString());
	}

	public static PoolInfoClient create() {
		if (instance == null) {
			instance = new PoolInfoClient();
		}
		return instance;
	}

	private PoolInfoClient() {
		InetAddress serverAddress;
		Socket socket;
		ObjectInputStream in;
		ObjectOutputStream out;

		Properties p = System.getProperties();
		
		total_hosts = getIntProperty(p, "pool.total_hosts");
		String cluster = p.getProperty("cluster");
		if (cluster == null) {
			System.err.println("Warning: cluster property not set, using 'unknown'");
			cluster = "unknown";
		}
		int serverPort = getIntProperty(p, "pool.server.port");
		String serverName = p.getProperty("pool.server.host");
		if (serverName == null) {
			throw new RuntimeException("property pool.server.host is not specified");
		}
		try {
			serverAddress = InetAddress.getByName(serverName);
		} catch (UnknownHostException e) {
			throw new RuntimeException("cannot get ip of pool server");
		}
		try {
			socket = new Socket(serverAddress, serverPort);
			out = new ObjectOutputStream(socket.getOutputStream());
			out.writeInt(total_hosts);
			out.writeUTF(cluster);
			out.flush();

			in = new ObjectInputStream(socket.getInputStream());
			host_number = in.readInt();
			host_clusters = (String []) in.readObject();
			host_addresses = (InetAddress []) in.readObject();
			
			in.close();
			out.close();
			socket.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (host_number >= total_hosts || host_number < 0 || total_hosts < 1) {
			throw new RuntimeException("Sanity check on host numbers failed!");
		}
	}

	public int size() {
		return total_hosts;
	}

	public int rank() {
		return host_number;
	}

	public InetAddress hostAddress() {
		return host_addresses[host_number];
	}

	public InetAddress hostAddress(int rank) {
		return host_addresses[rank];
	}

	public String hostName() {
		return host_addresses[host_number].getHostName();
	}

	public String hostName(int rank) {
		return host_addresses[rank].getHostName();
	}

	public InetAddress[] hostAddresses() {
		return host_addresses;
	}

	private int getIntProperty(Properties p, String name) throws RuntimeException {
		String temp = p.getProperty(name);
		
		if (temp == null) { 
			throw new RuntimeException("Property " + name + " not found !");
		}
		
		return Integer.parseInt(temp);
	}	

	public String toString() {
		String result = "pool info: size = " + total_hosts +
			"; my rank is " + host_number + "; host list:\n";
		for (int i = 0; i < total_hosts; i++) {
			result += i + ": address= " + host_addresses[i] + 
				" cluster=" + host_clusters[i] + "\n";
		}
		return result;
	}
}
