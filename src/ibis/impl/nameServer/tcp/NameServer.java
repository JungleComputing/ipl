package ibis.impl.nameServer.tcp;

import ibis.connect.controlHub.ControlHub;
import ibis.io.DummyInputStream;
import ibis.io.DummyOutputStream;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.util.PoolInfoServer;
import ibis.util.TypedProperties;

import java.util.Calendar;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.DataInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

public class NameServer implements Protocol {

	public static final int TCP_IBIS_NAME_SERVER_PORT_NR = 9826;
	// public static final int TCP_IBIS_NAME_SERVER_PORT_NR = 5678;
        private static final int BUF_SIZE = 1024;
	
	static boolean DEBUG = TypedProperties.booleanProperty("ibis.ns.debug");
	static boolean VERBOSE = TypedProperties.booleanProperty("ibis.ns.verbose");

	static int PINGER_TIMEOUT = TypedProperties.intProperty("ibis.ns.timeout", 300) * 1000;	// Property is in seconds, convert to milliseconds.

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

	    public String toString() {
		return "ibisInfo(" + identifier + "at " + ibisNameServerAddress + ":" + ibisNameServerport + ")";
	    }
	} 

	static class RunInfo { 
		Vector pool; // a list of IbisInfos
		Vector toBeDeleted; // a list of ibis identifiers

		PortTypeNameServer    portTypeNameServer;
		ReceivePortNameServer receivePortNameServer;
		ElectionServer electionServer;

		long pingLimit;

		RunInfo() throws IOException { 
			pool = new Vector();
			toBeDeleted = new Vector();
			portTypeNameServer    = new PortTypeNameServer();
			receivePortNameServer = new ReceivePortNameServer();
			electionServer = new ElectionServer();
			pingLimit = System.currentTimeMillis() + PINGER_TIMEOUT;
		}

	    public String toString() {
		String res = "runinfo:\n" +
		    "  pool = \n";

		for(int i=0; i<pool.size(); i++) {
		    res += "    " + pool.get(i) + "\n";
		}

		res +=    "  toBeDeleted = \n";

		for(int i=0; i<toBeDeleted.size(); i++) {
		    res += "    " + toBeDeleted.get(i) + "\n";
		}

		return res;
	    }
	}

	private Hashtable pools;
	private ServerSocket serverSocket;

	private	ObjectInputStream in;
	private	ObjectOutputStream out;

	private boolean singleRun;
	private boolean joined;

	private NameServer(boolean singleRun, int port) throws IOException {
		this.singleRun = singleRun;
		this.joined = false;

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
		    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d, BUF_SIZE));
		    out.writeByte(IBIS_JOIN);
		    out.writeObject(id);
		    NameServerClient.socketFactory.close(null, out, s);
		    
		    if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.identifier.toString() + " DONE");
		    }
		} catch (Exception e) {
			System.err.println("Could not forward join of "  + 
					   id.toString() + " to " + dest.identifier.toString() + 
					   "error = " + e);					   
		}

	}

	private void handleIbisJoin() throws IOException, ClassNotFoundException { 
		String key = in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();
		InetAddress address = (InetAddress) in.readObject();
		int port = in.readInt();

		if (DEBUG) {
			System.err.print("NameServer: join to pool " + key);
			System.err.print(" requested by " + id.toString());
			System.err.println(", port " + port);
		}

		IbisInfo info = new IbisInfo(id, address, port);
		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			// new run
			poolPinger();
			p = new RunInfo();
			
			pools.put(key, p);
			joined = true;
			
			if (VERBOSE) { 
				System.err.println("NameServer: new pool " + key + " created");
			}
		}
		
		if (p.pool.contains(info)) { 
			out.writeByte(IBIS_REFUSED);

			if (DEBUG) { 
				System.err.println("NameServer: join to pool " + key + " of ibis " + id.toString() + " refused");
			}
			out.flush();
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
			out.flush();

			for (int i=0;i<p.pool.size();i++) { 
				IbisInfo temp = (IbisInfo) p.pool.get(i);
				forwardJoin(temp, id);
			}

			p.pool.add(info);

			String date = Calendar.getInstance().getTime().toString();
 
			System.out.println(date + " " + id.name() + " JOINS  pool " + key + " (" + p.pool.size() + " nodes)");
		}
	}	

	private void poolPinger(String key) {
		if (DEBUG) {
			System.err.print("NameServer: ping pool " + key);
		}

		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			return;
		}

		long t = System.currentTimeMillis();

		// If the pool has not reached its ping-limit yet, return.
		if (t < p.pingLimit) {
		    return;
		}

		for (int i=0;i<p.pool.size();i++) { 
			IbisInfo temp = (IbisInfo) p.pool.get(i);
			if (doPing(temp, key)) {
			    // Pool is still alive. Reset its ping-limit.
			    p.pingLimit = t + PINGER_TIMEOUT;
			    return;
			}
		}

		// Pool is dead.
		pools.remove(key);
		try {
		    String date = Calendar.getInstance().getTime().toString();
		    System.out.println(date + " pool " + key + " seems to be dead.");
		    killThreads(p);
		} catch(Exception e) {
		}
	}	

	/**
	 * Checks all pools to see if they still are alive. If a pool is dead
	 * (connect to all members fails), the pool is killed.
	 */
	private void poolPinger() {
		for (Enumeration e = pools.keys(); e.hasMoreElements() ;) {
		    String key = (String) e.nextElement();
		    poolPinger(key);
		}
	}

	private boolean doPing(IbisInfo dest, String key) {
		try {
		    Socket s = NameServerClient.socketFactory.createSocket(dest.ibisNameServerAddress,
							      dest.ibisNameServerport, null, -1 /* do not retry */);

		    DummyOutputStream d = new DummyOutputStream(s.getOutputStream());
		    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d));
		    out.writeByte(IBIS_PING);
		    out.flush();
		    DummyInputStream i = new DummyInputStream(s.getInputStream());
		    DataInputStream in = new DataInputStream(new BufferedInputStream(i));
		    String k = in.readUTF();
		    NameServerClient.socketFactory.close(in, out, s);
		    if (! k.equals(key)) {
			return false;
		    }
		} catch (Exception e) {
		    return false;
		}
		return true;
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
		    ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d, BUF_SIZE));
		    out.writeByte(IBIS_LEAVE);
		    out.writeObject(id);
		    NameServerClient.socketFactory.close(null, out, s);
		} catch (Exception e) {
			System.err.println("Could not forward leave of "  + 
					   id.toString() + " to " + dest.identifier.toString() + 
					   "error = " + e);					   
//			e.printStackTrace();
		}
	}
    
	private void killThreads(RunInfo p) throws IOException {
		Socket s = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.portTypeNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d = new DummyOutputStream(s.getOutputStream());

		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(d, BUF_SIZE));
		out.writeByte(PORTTYPE_EXIT);
		NameServerClient.socketFactory.close(null, out, s);

		Socket s2 = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.receivePortNameServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d2 = new DummyOutputStream(s2.getOutputStream());

		ObjectOutputStream out2 = new ObjectOutputStream(new BufferedOutputStream(d2, BUF_SIZE));
		out2.writeByte(PORT_EXIT);
		NameServerClient.socketFactory.close(null, out2, s2);

		Socket s3 = NameServerClient.socketFactory.createSocket(InetAddress.getLocalHost(), 
							  p.electionServer.getPort(), null, 0 /* retry */);
		DummyOutputStream d3 = new DummyOutputStream(s3.getOutputStream());
		ObjectOutputStream out3 = new ObjectOutputStream(new BufferedOutputStream(d3, BUF_SIZE));
		out3.writeByte(ELECTION_EXIT);
		NameServerClient.socketFactory.close(null, out3, s3);
	}


	private void handleIbisLeave() throws IOException, ClassNotFoundException {
		String key = in.readUTF();
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

				String date = Calendar.getInstance().getTime().toString();

				System.out.println(date + " " + id.name() + " LEAVES pool " + key + " (" + p.pool.size() + " nodes)");
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
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d, BUF_SIZE));
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
		String key = in.readUTF();
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
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(d, BUF_SIZE));
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
		String key = in.readUTF();

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
				out = new ObjectOutputStream(new BufferedOutputStream(dos, BUF_SIZE));

				DummyInputStream di = new DummyInputStream(s.getInputStream());
				in  = new ObjectInputStream(new BufferedInputStream(di, BUF_SIZE));

				opcode = in.readByte();

				switch (opcode) { 
				case (IBIS_JOIN): 
					handleIbisJoin();
					break;
				case (IBIS_LEAVE):
					handleIbisLeave();
					if (singleRun && pools.size() == 0) { 
					    if (joined) {
						stop = true;
					    }
					    // ignore invalid leave req.
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

//			System.err.println("Pools are now: " + pools);
		}

		try { 
			serverSocket.close();
		} catch (Exception e) {
			throw new IbisRuntimeException("NameServer got an error" , e);
		}

		if (VERBOSE) {
			System.err.println("NameServer: exit");			
		}
	}

	public static void main(String [] args) { 
		boolean single = false;
		boolean control_hub = false;
		boolean pool_server = true;
		NameServer ns = null;
		int port = TCP_IBIS_NAME_SERVER_PORT_NR;
		ControlHub h = null;

		for (int i = 0; i < args.length; i++) {
			if (false) {
			} else if (args[i].equals("-single")) {
				single = true;
			} else if (args[i].equals("-d")) {
				DEBUG = true;
			} else if (args[i].equals("-v")) {
				VERBOSE = true;
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
			} else if (args[i].equals("-poolport")) {
				i++;
				try {
					int n  = Integer.parseInt(args[i]);
					System.setProperty("ibis.pool.server.port", args[i]);
				} catch (Exception e) {
					System.err.println("invalid port");
					System.exit(1);
				}
			} else if (args[i].equals("-controlhub")) {
				control_hub = true;
			} else if (args[i].equals("-no-controlhub")) {
				control_hub = false;
			} else if (args[i].equals("-poolserver")) {
				pool_server = true;
			} else if (args[i].equals("-no-poolserver")) {
				pool_server = false;
			} else if (args[i].equals("-debug")) {
				// Accepted and ignored.
			} else {
				System.err.println("No such option: " + args[i]);
				System.exit(1);
			}
		}

		if (control_hub) {
		    h = new ControlHub();
		    h.setDaemon(true);
		    h.start();
		}

		if (pool_server) {
		    PoolInfoServer p = new PoolInfoServer(single);
		    p.setDaemon(true);
		    p.start();
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
		try {
		    ns.run();
		    if (h != null) {
			h.waitForCount(1);
		    }
		    System.exit(0);
		} catch (Throwable t) {
		    System.err.println("Nameserver got an exception: " + t);
		    t.printStackTrace();
		}
	} 
}
