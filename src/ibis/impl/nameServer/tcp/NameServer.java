package ibis.impl.nameServer.tcp;

import ibis.ipl.IbisIdentifier;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;
import ibis.util.IbisSocketFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

public class NameServer implements Protocol {

	public static final int TCP_IBIS_NAME_SERVER_PORT_NR = 9826;

	public static final boolean DEBUG = false;
	public static final boolean VERBOSE = true; // false; // true;

	static class IbisInfo { 		
		IbisIdentifier identifier;
		int ibisNameServerport;
		InetAddress ibisNameServerAddress;

		IbisInfo(IbisIdentifier identifier, InetAddress ibisNameServerAddress, int ibisNameServerport) {
			this.identifier = identifier;
			this.ibisNameServerAddress = ibisNameServerAddress;
			this.ibisNameServerport = ibisNameServerport; 
		} 

		public boolean equals(Object other) { 
			if (other instanceof IbisInfo) { 
				return identifier.equals(((IbisInfo) other).identifier);
			} else { 
				return false;
			}
		}
		
		public int hashCode() {
		    return identifier.hashCode();
		}
	} 

	static class RunInfo { 
		Vector pool;
		
		PortTypeNameServer    portTypeNameServer;   
		ReceivePortNameServer receivePortNameServer;   
		ElectionServer electionServer;   

		RunInfo() throws IOException { 
			pool = new Vector();
			portTypeNameServer    = new PortTypeNameServer();
			receivePortNameServer = new ReceivePortNameServer();
			electionServer = new ElectionServer();
		}
	}

	private Hashtable pools;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	private boolean singleRun;

	private NameServer(boolean singleRun, int port) throws IOException {
		this.singleRun = singleRun;

		if (DEBUG) { 
			System.err.println("NameServer: singleRun = " + singleRun);
		}

		// Create a server socket.
		serverSocket = IbisSocketFactory.createServerSocket(port, null, true);
       		       
		pools = new Hashtable();

		if (VERBOSE) { 
			System.err.println("NameServer: created server on port " + serverSocket.getLocalPort());
		}
	}

	private void forwardJoin(IbisInfo dest, IbisIdentifier id) throws IOException { 

		if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.ibisNameServerAddress + ", dest port: " + dest.ibisNameServerport);
		}

		Socket s = IbisSocketFactory.createSocket(dest.ibisNameServerAddress, dest.ibisNameServerport, null, 0 /* retry */);

		DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		out.writeByte(IBIS_JOIN);
		out.writeObject(id);
		IbisSocketFactory.close(null, out, s);

		if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.identifier.toString() + " DONE");
		}
	}

	private void handleIbisJoin() throws IOException, ClassNotFoundException { 
		String key = (String) in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();
		InetAddress address = (InetAddress) in.readObject();
		int port = in.readInt();

		if (DEBUG) {
			System.err.println("NameServer: join to pool " + key);
			System.err.println(" requested by " + id.toString());
		}

		IbisInfo info = new IbisInfo(id, address, port);
		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			// new run
			p = new RunInfo();
			pools.put(key, p);
			
			if (VERBOSE) { 
				System.err.println("NameServer: new pool " + key + " created");
			}
		}
		
		if (p.pool.contains(info)) { 
			out.writeByte(IBIS_REFUSED);

			if (DEBUG) { 
				System.err.println("NameServer: join to pool " + key + " of ibis " + id.toString() + " refused");
			}
		} else { 
			out.writeByte(IBIS_ACCEPTED);
			out.writeInt(p.portTypeNameServer.getPort());
			out.writeInt(p.receivePortNameServer.getPort());
			out.writeInt(p.electionServer.getPort());

			if (DEBUG) { 
				System.err.println("NameServer: join to pool " + key + " of ibis " + id.toString() + " accepted");
			}

			// first send all existing nodes to the new one.
			out.writeInt(p.pool.size());

			for (int i=0;i<p.pool.size();i++) {
				IbisInfo temp = (IbisInfo) p.pool.get(i);
				out.writeObject(temp.identifier);
			}

			for (int i=0;i<p.pool.size();i++) { 
				IbisInfo temp = (IbisInfo) p.pool.get(i);
				forwardJoin(temp, id);
			}

			p.pool.add(info);
			System.out.println("JOIN  of node " + id.name() + " to pool " + key + " (" + p.pool.size() + " nodes)");
		}
			
		out.flush();
	}	

	private void forwardLeave(IbisInfo dest, IbisIdentifier id) throws IOException { 
		if (DEBUG) { 
			System.err.println("NameServer: forwarding leave of " + 
					   id.toString() + " to " + dest.identifier.toString());
		}

		Socket s = IbisSocketFactory.createSocket(dest.ibisNameServerAddress,
							     dest.ibisNameServerport, null, 0 /* retry */);
		DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		out.writeByte(IBIS_LEAVE);
		out.writeObject(id);
		IbisSocketFactory.close(null, out, s);
	}

	private void killThreads(RunInfo p) throws IOException {
		Socket s = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.portTypeNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d = new DummyOutputStream(s.getOutputStream());

		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		out.writeByte(PORTTYPE_EXIT);
		IbisSocketFactory.close(null, out, s);

		Socket s2 = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.receivePortNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d2 = new DummyOutputStream(s2.getOutputStream());

		ObjectOutputStream out2 = new ObjectOutputStream(new BufferedOutputStream(d2));
		out2.writeByte(PORT_EXIT);
		IbisSocketFactory.close(null, out2, s2);

		Socket s3 = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.electionServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d3 = new DummyOutputStream(s3.getOutputStream());
		ObjectOutputStream out3 = new ObjectOutputStream(new BufferedOutputStream(d3));
		out3.writeByte(ELECTION_EXIT);
		IbisSocketFactory.close(null, out3, s3);
	}
	
	private void handleIbisLeave() throws IOException, ClassNotFoundException {
		String key = (String) in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();

		RunInfo p = (RunInfo) pools.get(key);

		if (DEBUG) { 
			System.err.println("NameServer: leave from pool " + key + " requested by " + id.toString());
		}

		if (p == null) { 
			// new run
			System.err.println("NameServer: unknown ibis " + id.toString() + "/" + key + " tried to leave");
			return;				
		} else {
			int index = -1;

			for (int i=0;i<p.pool.size();i++) { 				
				IbisInfo info = (IbisInfo) p.pool.get(i);
				if (info.identifier.equals(id)) { 
					index = i;
					break;
				}
			}

			if (index != -1) { 
				// found it.
				if (DEBUG) { 
					System.err.println("NameServer: leave from pool " + key + " of ibis " + id.toString() + " accepted");
				}

				// Also forward the leave to the requester.
				// It is used as an acknowledgement, and
				// the leaver is only allowed to exit when it
				// has received its own leave message.
				for (int i=0; i<p.pool.size(); i++) { 
					forwardLeave((IbisInfo) p.pool.get(i), id);
				} 
				p.pool.remove(index);

				System.out.println("LEAVE of node " + id.name() + " to pool " + key + " (" + p.pool.size() + " nodes)");
				id.free();


				if (p.pool.size() == 0) { 
					if (VERBOSE) { 
						System.err.println("NameServer: removing pool " + key);
					}

					pools.remove(key);
					killThreads(p);
				} 				
			} else { 
				System.err.println("NameServer: unknown ibis " + id.toString() + "/" + key + " tried to leave");
			}

			out.writeByte(0);
			out.flush();
		}
	} 

	public void run() {
		int opcode;
		Socket s;
		boolean stop = false;

		while (!stop) {
			
			try {
				s = IbisSocketFactory.accept(serverSocket);

				if (DEBUG) { 
					System.err.println("NameServer: incoming connection from " + s.toString());
				}

			} catch (Exception e) {
				System.err.println("NameServer got an error " + e.getMessage());
				continue;
			}

			try {
				DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
				out = new ObjectOutputStream(new BufferedOutputStream(dos));

				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new ObjectInputStream(new BufferedInputStream(di));

				opcode = in.readByte();

				switch (opcode) { 
				case (IBIS_JOIN): 
					handleIbisJoin();
					break;
				case (IBIS_LEAVE):
					handleIbisLeave();
					if (singleRun && pools.size() == 0) { 
						stop = true;
					}
					break;
				default: 
					System.err.println("NameServer got an illegal opcode: " + opcode);
				}

				IbisSocketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in NameServer.run " + e1.toString());
//				e1.printStackTrace();
				if (s != null) { 
					try { 
						IbisSocketFactory.close(null, null, s);
					} catch (IOException e2) { 
						// don't care.
					} 
				}
			}
		}

		try { 
			serverSocket.close();
		} catch (Exception e) {
			throw new RuntimeException("NameServer got an error " + e.getMessage());
		}

		if (VERBOSE) {
			System.err.println("NameServer: exit");			
		}
	}

	public static void main(String [] args) { 
		boolean single = false;
		NameServer ns = null;
		int port = TCP_IBIS_NAME_SERVER_PORT_NR;

		for (int i = 0; i < args.length; i++) {
			if (false) {
			} else if (args[i].equals("-single")) {
				single = true;
			} else if (args[i].equals("-port")) {
				i++;
				try {
					port = Integer.parseInt(args[i]);
				} catch (Exception e) {
					System.err.println("invalid port");
					System.exit(1);
				}
								
			} else {
				System.err.println("No such option: " + args[i]);
				System.exit(1);
			}
		}

		if(!single) {
			Properties p = System.getProperties();
			String singleS = p.getProperty("single_run");
			
			single = (singleS != null && singleS.equals("true")); 
		}

		while (true) {
			try { 
				ns = new NameServer(single, port);
				break;
			} catch (Throwable e) { 
				System.err.println("Main got " + e + ", retry in 1 second");
				try {Thread.sleep(1000);} catch (Exception ee) {}
//				e.printStackTrace();
			}
		}

		ns.run();
		System.exit(0);
	} 
}
