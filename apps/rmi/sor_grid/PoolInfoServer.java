//package poolinfo;

import java.util.Properties;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PoolInfoServer {

	private static boolean DEBUG = true;

	private ServerSocket serverSocket;

	public static void main(String [] argv) {
		if (argv.length != 1) {
			System.err.println("Incorrect argument count, PoolInfoServer " +
							   "needs exactly one argument: the port at " +
							   "which the server is to be run.");
			System.exit(1);
		}
		int serverPort = Integer.parseInt(argv[0]);
		new PoolInfoServer(serverPort).run();
	}

	public PoolInfoServer(int port) {
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void run() {
		int total_hosts;
		int connected_hosts;
		String [] host_clusters;
		InetAddress [] host_addresses;
		Socket [] host_sockets;

		/* we have to keep references to the input streams otherwise they can
		 * be closed by the garbage collector, and the socket will also be
		 * closed then */
		ObjectInputStream [] host_inputs;

		Socket socket;
		ObjectInputStream in;
		ObjectOutputStream out;

		try {
			if (DEBUG) {
				System.err.println("PoolInfoServer: starting run, " + 
								   "waiting for a host to connect...");
			}

			socket = serverSocket.accept();
			in = new ObjectInputStream(socket.getInputStream());
			total_hosts = in.readInt();
			host_clusters = new String [total_hosts];
			host_addresses = new InetAddress [total_hosts];
			host_sockets = new Socket [total_hosts];
			host_inputs = new ObjectInputStream [total_hosts];

			host_clusters[0] = in.readUTF();
			host_addresses[0] = socket.getInetAddress();
			host_sockets[0] = socket;
			host_inputs[0] = in;

			connected_hosts = 1;

			if (DEBUG) {
				System.err.println("PoolInfoServer: host 1 has connected, total hosts = " +
								   total_hosts);
			}

			while (connected_hosts < total_hosts) {
				socket = serverSocket.accept();
				in = new ObjectInputStream(socket.getInputStream());
				if (in.readInt() != total_hosts) {
					System.err.println("PoolInfoServer: EEK, different total_hosts in PoolInfoServer, ignoring this connection...");
					in.close();
					socket.close();
				}

				host_clusters[connected_hosts] = in.readUTF();
				host_addresses[connected_hosts] = socket.getInetAddress();
				host_sockets[connected_hosts] = socket;
				host_inputs[connected_hosts] = in;

				connected_hosts += 1;

				if (DEBUG) {
					System.err.println("PoolInfoServer: Host " + connected_hosts +
									   " has connected");
				}
			}

			if (DEBUG) {
				System.err.println("PoolInfoServer: all hosts have connected, " +
								   "now broadcasting host info...");
			}

			for (int i = 0; i < total_hosts; i++) {
				out= new ObjectOutputStream(host_sockets[i].getOutputStream());
				out.writeInt(i); //give the node a rank
				out.writeObject(host_clusters);
				out.writeObject(host_addresses);

				host_inputs[i].close();
				out.close();
				host_sockets[i].close();
			}

			if (DEBUG) {
				System.err.println("PoolInfoServer: broadcast done, run is finished");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
		
	private int getIntProperty(Properties p, String name) throws RuntimeException {
		String temp = p.getProperty(name);
		
		if (temp == null) { 
			throw new RuntimeException("Property " + name + " not found !");
		}
		
		return Integer.parseInt(temp);
	}	
}
