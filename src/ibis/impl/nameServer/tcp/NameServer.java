package ibis.impl.nameServer.tcp;

import ibis.connect.controlHub.ControlHub;
import ibis.ipl.IbisIdentifier;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
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
	// public static final int TCP_IBIS_NAME_SERVER_PORT_NR = 5678;
	
	public static final boolean DEBUG = false;
	public static final boolean VERBOSE = true;

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
		Vector toBeDeleted;
		
		PortTypeNameServer    portTypeNameServer;   
		ReceivePortNameServer receivePortNameServer;   
		ElectionServer electionServer;   

		RunInfo() throws IOException { 
			pool = new Vector();
			toBeDeleted = new Vector();
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
		serverSocket = NameServerClient.socketFactory.createServerSocket(port, null, true);
       		       
		pools = new Hashtable();

		if (VERBOSE) { 
			System.err.println("NameServer: created server on port " + serverSocket.getLocalPort());
		}
	}

	private void forwardJoin(IbisInfo dest, IbisIdentifier id) {

		if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.ibisNameServerAddress + ", dest port: " + dest.ibisNameServerport);
		}
		try {

		    Socket s = NameServerClient.socketFactory.createSocket(dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1 /* do not retry */);
		    
		    DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
		    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		    out.writeByte(IBIS_JOIN);
		    out.writeObject(id);
		    NameServerClient.socketFactory.close(null, out, s);
		    
		    if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.identifier.toString() + " DONE");
		    }
		} catch (Exception e) {
		    if (DEBUG) {
			System.err.println("Could not forward join of "  + 
					   id.toString() + " to " + dest.identifier.toString() + 
					   "error = " + e);					   
		    }
		}

	}

	private void handleIbisJoin() throws IOException, ClassNotFoundException { 
		String key = (String) in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();
		InetAddress address = (InetAddress) in.readObject();
		int port = in.readInt();

//		if (DEBUG) {
			System.err.println("NameServer: join to pool " + key);
			System.err.println(" requested by " + id.toString());
//		}

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
			
			//send all nodes about to leave to the new one
			out.writeInt(p.toBeDeleted.size());
			
			for (int i=0;i<p.toBeDeleted.size();i++) {
			    out.writeObject(p.toBeDeleted.get(i));
			}

			for (int i=0;i<p.pool.size();i++) { 
				IbisInfo temp = (IbisInfo) p.pool.get(i);
				forwardJoin(temp, id);
			}

			p.pool.add(info);
			System.out.println(id.name() + " JOINS  pool " + key + " (" + p.pool.size() + " nodes)");
		}
			
		out.flush();
	}	

    private void forwardLeave(IbisInfo dest, IbisIdentifier id) {
		if (DEBUG) { 
			System.err.println("NameServer: forwarding leave of " + 
					   id.toString() + " to " + dest.identifier.toString());
		}
		
		try {
		    Socket s = NameServerClient.socketFactory.createSocket(dest.ibisNameServerAddress,
							      dest.ibisNameServerport, null, -1 /* do not retry */);

		    DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
		    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		    out.writeByte(IBIS_LEAVE);
		    out.writeObject(id);
		    NameServerClient.socketFactory.close(null, out, s);
		} catch (Exception e) {
		    if (DEBUG) {
			System.err.println("Could not forward leave of "  + 
					   id.toString() + " to " + dest.identifier.toString() + 
					   "error = " + e);					   
			e.printStackTrace();
		    }
		}
    }
    
	private void killThreads(RunInfo p) throws IOException {
		Socket s = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.portTypeNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d = new DummyOutputStream(s.getOutputStream());

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(d));
		out.writeByte(PORTTYPE_EXIT);
		NameServerClient.socketFactory.close(null, out, s);

		Socket s2 = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.receivePortNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d2 = new DummyOutputStream(s2.getOutputStream());

		ObjectOutputStream out2 = new ObjectOutputStream(new BufferedOutputStream(d2));
		out2.writeByte(PORT_EXIT);
		NameServerClient.socketFactory.close(null, out2, s2);

		Socket s3 = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.electionServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d3 = new DummyOutputStream(s3.getOutputStream());
		ObjectOutputStream out3 = new ObjectOutputStream(new BufferedOutputStream(d3));
		out3.writeByte(ELECTION_EXIT);
		NameServerClient.socketFactory.close(null, out3, s3);
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
				p.toBeDeleted.remove(id);

				System.out.println(id.name() + " LEAVES pool " + key + " (" + p.pool.size() + " nodes)");
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

	private void forwardDelete(IbisInfo dest, IbisIdentifier id) throws IOException { 

		if (DEBUG) { 
			System.err.println("NameServer: forwarding delete of " + id.toString() + " to " + dest.identifier.toString());
		}

		try {
			Socket s = NameServerClient.socketFactory.createSocket(dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1 /*do not retry*/);

			DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
			out.writeByte(IBIS_DELETE);
			out.writeObject(id);
			NameServerClient.socketFactory.close(null, out, s);
		} catch (Exception e) {
		    if (DEBUG) {
			System.err.println("Could not forward delete of "  + 
					   id.toString() + " to " + dest.identifier.toString() + 
					   "error = " + e);					   
		    }
		}
		

		if (DEBUG) { 
			System.err.println("NameServer: forwarding delete of " + id.toString() + " to " + dest.identifier.toString() + " DONE");
		}
	}

	private void handleIbisDelete() throws IOException, ClassNotFoundException { 
		String key = (String) in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();

		if (DEBUG) {
			System.err.println("NameServer: delete of host " 
			+ id.toString() + " from pool " + key);
		}

		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			// new run
			System.err.println("NameServer: unknown ibis " + id.toString() + "/" + key + " was requested to be deleted");
			return;
		}
		
		int index = -1;

		for (int i=0;i<p.pool.size();i++) { 				
			IbisInfo info = (IbisInfo) p.pool.get(i);
			if (info.identifier.equals(id)) { 
				index = i;
				break;
			}
		}

		if (index != -1) { 
			//found it
		
			p.toBeDeleted.add(id);
			
			System.out.println("DELETE: pool " + key);

			for (int i=0;i<p.pool.size();i++) { 
				IbisInfo temp = (IbisInfo) p.pool.get(i);
				forwardDelete(temp, id);
			}
			
			System.err.println("all deletes forwarded");

		} else {
		    System.err.println("NameServer: unknown ibis " + id.toString() + "/" + key + " was requested to be deleted");
		}
		
		out.writeByte(0);	
		out.flush();
		
	}	


	private void forwardReconfigure(IbisInfo dest) throws IOException { 

		if (DEBUG) { 
			System.err.println("NameServer: forwarding reconfigure to " + dest.identifier.toString());
		}

		try {
			Socket s = NameServerClient.socketFactory.createSocket(dest.ibisNameServerAddress, dest.ibisNameServerport, null, -1 /*do not retry*/);

			DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
			out.writeByte(IBIS_RECONFIGURE);
			NameServerClient.socketFactory.close(null, out, s);
		} catch (Exception e) {
		    if (DEBUG) {
			System.err.println("Could not forward reconfigure to " + dest.identifier.toString() + 
					   "error = " + e);					   
		    }
		}


		if (DEBUG) { 
			System.err.println("NameServer: forwarding reconfigure to " + dest.identifier.toString() + " DONE");
		}
	}

	private void handleIbisReconfigure() throws IOException, ClassNotFoundException { 
		String key = (String) in.readUTF();

		if (DEBUG) {
			System.err.println("NameServer: reconfigure of hosts in pool " + key);
		}

		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			// new run
			System.err.println("NameServer: unknown pool " + key + " was requested to be reconfigured");
			return;
		}
		

		for (int i=0;i<p.pool.size();i++) { 
			IbisInfo temp = (IbisInfo) p.pool.get(i);
			forwardReconfigure(temp);
		}

		
		out.writeByte(0);	
		out.flush();
		System.out.println("RECONFIGURE: pool " + key);
	}

	public void run() {
		int opcode;
		Socket s;
		boolean stop = false;

		while (!stop) {
			
			try {
			    if (DEBUG) { 
				System.err.println("NameServer: accepting incoming connections... ");
			    }	
			    s = NameServerClient.socketFactory.accept(serverSocket);

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
				case (IBIS_DELETE):
					handleIbisDelete();
					break;
				case (IBIS_RECONFIGURE):
					handleIbisReconfigure();
					break;
				default: 
					System.err.println("NameServer got an illegal opcode: " + opcode);
				}

				NameServerClient.socketFactory.close(in, out, s);
			} catch (Exception e1) {
				System.err.println("Got an exception in NameServer.run " + e1.toString());
				e1.printStackTrace();
				if (s != null) { 
					NameServerClient.socketFactory.close(null, null, s);
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
		boolean control_hub = false;
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
			} else if (args[i].equals("-hubport")) {
				i++;
				try {
					int n  = Integer.parseInt(args[i]);
					System.setProperty("ibis.connect.hub_port", args[i]);
					control_hub = true;
				} catch (Exception e) {
					System.err.println("invalid port");
					System.exit(1);
				}
			} else if (args[i].equals("-controlhub")) {
				control_hub = true;
			} else if (args[i].equals("-debug")) {
				// Accepted and ignored.
			} else {
				System.err.println("No such option: " + args[i]);
				System.exit(1);
			}
		}

		if (control_hub) {
		    ControlHub h = new ControlHub();
		    h.setDaemon(true);
		    h.start();
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
				e.printStackTrace();
				System.err.println("Main got " + e + ", retry in 1 second");
				try {Thread.sleep(1000);} catch (Exception ee) {}
//				e.printStackTrace();
			}
		}

		ns.run();
		System.exit(0);
	} 
}
