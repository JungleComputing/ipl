package ibis.ipl.impl.nameServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.EOFException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.util.Properties;
import java.util.Vector;
import java.util.Hashtable;

import ibis.ipl.*;
import ibis.ipl.impl.generic.IbisSocketFactory;

public class NameServer implements NameServerProtocol, PortTypeNameServerProtocol, ReceivePortNameServerProtocol, ElectionProtocol {

	public static final int TCP_IBIS_NAME_SERVER_PORT_NR = 9826;

	public static final boolean DEBUG = false;
	public static final boolean VERBOSE = true;

	class IbisInfo { 		
		IbisIdentifier identifier;
		int ibisNameServerport;

		IbisInfo(IbisIdentifier identifier, int ibisNameServerport) {
			this.identifier = identifier;
			this.ibisNameServerport = ibisNameServerport; 
		} 

		public boolean equals(Object other) { 
			if (other instanceof IbisInfo) { 
				return identifier.equals(((IbisInfo) other).identifier);
			} else { 
				return false;
			}
		}
	} 

	class RunInfo { 
		String poolKey;		
		Vector pool;
		
		PortTypeNameServer    portTypeNameServer;   
		ReceivePortNameServer receivePortNameServer;   
		ElectionServer electionServer;   

		RunInfo(String poolKey) throws IOException { 
			this.poolKey = poolKey;
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

	private NameServer(boolean singleRun) throws IbisIOException {

		this.singleRun = singleRun;

		if (DEBUG) { 
			System.err.println("NameServer: singleRun = " + singleRun);
		}

		// Create a server socket.
		serverSocket = IbisSocketFactory.createServerSocket(TCP_IBIS_NAME_SERVER_PORT_NR, false);
       		       
		pools = new Hashtable();

		if (VERBOSE) { 
			System.err.println("NameServer: created server on port " + serverSocket.getLocalPort());
		}
	}

	private void forwardJoin(IbisInfo dest, IbisIdentifier id) throws IbisIOException { 

		if (DEBUG) { 
			System.err.println("NameServer: forwarding join of " + id.toString() + " to " + dest.identifier.toString());
		}

		Socket s = IbisSocketFactory.createSocket(dest.identifier.address(), dest.ibisNameServerport, true);

		try {
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			out.writeByte(IBIS_JOIN);
			out.writeObject(id);
			out.flush();
			out.close();
		}  catch (IOException e) {
			throw new IbisIOException(e);
		}
	}

	private void handleIbisJoin() throws IbisIOException, ClassNotFoundException { 
	    try {

		String key = (String) in.readUTF();
		IbisIdentifier id = (IbisIdentifier) in.readObject();
		int port = in.readInt();

		if (DEBUG) {
			System.err.println("NameServer: join to pool " + key);
			System.err.println(" requested by " + id.toString());
		}

		IbisInfo info = new IbisInfo(id, port);
	       
		RunInfo p = (RunInfo) pools.get(key);

		if (p == null) { 
			// new run
			p = new RunInfo(key);
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
//				forwardJoin(info, temp.identifier);
			}

			p.pool.add(info);
		}
			
		out.flush();
		System.out.println("JOIN: pool " + key + " now contains " + p.pool.size() + " nodes");
	    } catch (IOException e) {
		throw new IbisIOException(e);
	    }
	}	

	private void forwardLeave(IbisInfo dest, IbisIdentifier id) throws IbisIOException { 



		if (DEBUG) { 
			System.err.println("NameServer: forwarding leave of " + id.toString() + " to " + dest.identifier.toString());
		}

		Socket s = IbisSocketFactory.createSocket(dest.identifier.address(), 
							     dest.ibisNameServerport, true);
		try {
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));
			out.writeByte(IBIS_LEAVE);
			out.writeObject(id);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
	}

	private void killThreads(RunInfo p) throws IbisIOException {
		try {
			Socket s = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
								  p.portTypeNameServer.getPort(), true);
					ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
				s.getOutputStream()));
			out.writeByte(PORTTYPE_EXIT);
			out.flush();
			out.close();

			Socket s2 = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
								  p.receivePortNameServer.getPort(), true);
					ObjectOutputStream out2 = new ObjectOutputStream(new BufferedOutputStream(
				s2.getOutputStream()));
			out2.writeByte(PORT_EXIT);
			out2.flush();
			out2.close();

			Socket s3 = IbisSocketFactory.createSocket(InetAddress.getLocalHost(), 
								  p.electionServer.getPort(), true);
					ObjectOutputStream out3 = new ObjectOutputStream(new BufferedOutputStream(
				s3.getOutputStream()));
			out3.writeByte(ELECTION_EXIT);
			out3.flush();
			out3.close();
		}  catch (IOException e) {
			throw new IbisIOException(e);
		}
	}
	
	private void handleIbisLeave() throws IbisIOException, ClassNotFoundException {

		try {
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

				    p.pool.remove(index);

				    for (int i=0;i<p.pool.size();i++) { 
					    forwardLeave((IbisInfo) p.pool.get(i), id);
				    } 

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
		    }
		    System.out.println("LEAVE: pool " + key + " now contains " + p.pool.size() + " nodes");
		} catch (IOException e) {
		    throw new IbisIOException(e);
		}
	} 

	public void run() {

		int opcode;
		Socket s;
		boolean stop = false;

		while (!stop) {
			
			try {
				s = serverSocket.accept();
				s.setTcpNoDelay(true);

				if (DEBUG) { 
					System.err.println("NameServer: incoming connection from " + s.toString());
				}

			} catch (Exception e) {
				throw new RuntimeException("NameServer got an error " + e.getMessage());
			}

			try {
				in  = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
				out = new ObjectOutputStream(new BufferedOutputStream(s.getOutputStream()));

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
					System.err.println("NameServer got an illegal opcode " + opcode);					
				}

				out.flush();
				out.close();
				in.close();
				s.close();
								
			} catch (Exception e1) {
				System.err.println("Got an exception in NameServer.run " + e1.toString());
				e1.printStackTrace();
				if (s != null) { 
					try { 
						s.close();
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
		try { 
			boolean temp;

			Properties p = System.getProperties();
			String single = p.getProperty("single_run");

			temp = (single != null && single.equals("true")); 
			NameServer i = new NameServer(temp);
			i.run();

			System.exit(1);
		} catch (Exception e) { 
			System.err.println("Main got " + e);
			e.printStackTrace();
		} 
	} 
}
