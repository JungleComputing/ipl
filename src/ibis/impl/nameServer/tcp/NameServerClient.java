package ibis.impl.nameServer.tcp;

import ibis.impl.nameServer.NameServer;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ReceivePort;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisException;
import ibis.util.*;
import ibis.ipl.StaticProperties;
import ibis.util.DummyInputStream;
import ibis.util.DummyOutputStream;
import ibis.util.TypedProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;

import java.io.IOException;
import java.io.EOFException;
import java.io.StreamCorruptedException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Properties;

public class NameServerClient extends NameServer implements Runnable, Protocol {
	static final boolean DEBUG = false;

	private PortTypeNameServerClient portTypeNameServerClient;
	private ReceivePortNameServerClient receivePortNameServerClient;
	private ElectionClient electionClient;

	private ServerSocket serverSocket;	
	private Ibis ibisImpl;
	private	IbisIdentifier id;

	private	volatile boolean stop = false;
	private InetAddress serverAddress;
	private String server;
	private int port;
	private String poolName;
	private InetAddress myAddress;

	static IbisSocketFactory socketFactory =
	    IbisSocketFactory.createFactory("");

	public NameServerClient() {
	}

	protected void init(Ibis ibisImpl) throws IOException, IbisConfigurationException {
		this.ibisImpl   = ibisImpl;
		this.id     = ibisImpl.identifier();

		Properties p = System.getProperties();

		String myIp = p.getProperty("ip_address");
		if (myIp == null) {
			myAddress = InetAddress.getLocalHost();
		} else {
			myAddress = InetAddress.getByName(myIp);
		}

		server = p.getProperty("ibis.name_server.host");
		if (server == null) {
			throw new IbisConfigurationException("property ibis.name_server.host is not specified");
		}

		poolName = p.getProperty("ibis.name_server.key");
		if (poolName == null) {
			throw new IbisConfigurationException("property ibis.name_server.key is not specified");
		}

		String nameServerPortString = p.getProperty("ibis.name_server.port");
		if (nameServerPortString == null) {
			port = ibis.impl.nameServer.tcp.NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
		} else {
			try {
				port = Integer.parseInt(nameServerPortString);
				if(DEBUG) {
					System.err.println("Using nameserver port: " + port);
				}
			} catch (Exception e) {
				System.err.println("illegal nameserver port: " + nameServerPortString + ", using default");
			}
		}

		serverAddress = InetAddress.getByName(server);
		if(DEBUG) {
			System.err.println("Found nameServerInet " + serverAddress);
		}

		serverSocket = socketFactory.createServerSocket(0, myAddress, true);

		boolean retry = TypedProperties.booleanProperty("ibis.name_server.retry");

		Socket s = null;
		boolean failed_once = false;
		while(s == null) {
		    try {
			s = socketFactory.createSocket(serverAddress, 
				port, myAddress, -1);
		    } catch (ConnectionTimedOutException e) {
		        if(!retry) {
			    throw new ConnectionTimedOutException("Could not connect to name server "+server+":"+port);
			}
			if(!failed_once) {
			    System.err.println("Nameserver client failed"
			       + " to connect to nameserver\n at " 
			       + serverAddress + ":" + port 
			       + ", will keep trying");
			    failed_once = true;
			}
			try {
			    Thread.sleep(1000);
			} catch (InterruptedException e2) { 
			    // don't care
			}
		    }
		}


		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		ObjectOutputStream out =
			new ObjectOutputStream(new BufferedOutputStream(dos));

		if (DEBUG) { 
			System.out.println("NameServerClient: contacting nameserver");
		}
		out.writeByte(IBIS_JOIN);
		out.writeUTF(poolName);
		out.writeObject(id);
		out.writeObject(myAddress);
		out.writeInt(serverSocket.getLocalPort());
		out.flush();

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		ObjectInputStream in  = new ObjectInputStream(new BufferedInputStream(di));

		int opcode = in.readByte();

		if (DEBUG) { 
			System.out.println("NameServerClient: nameserver reply, opcode " + opcode);
		}

		switch (opcode) { 
		case IBIS_REFUSED:
			socketFactory.close(in, out, s);
			throw new ConnectionRefusedException("NameServerClient: " + id.name() + " is not unique!");
		case IBIS_ACCEPTED:
			// read the ports for the other name servers and start the receiver thread...
			int temp = in.readInt(); /* Port for the PortTypeNameServer */
			portTypeNameServerClient = new PortTypeNameServerClient(myAddress, serverAddress, temp);
			
			temp = in.readInt(); /* Port for the ReceivePortNameServer */
			receivePortNameServerClient = new ReceivePortNameServerClient(myAddress, serverAddress, temp);

			temp = in.readInt(); /* Port for the ElectionServer */
			electionClient = new ElectionClient(myAddress, serverAddress, temp);

			int poolSize = in.readInt();
			if (DEBUG) { 
				System.out.println("NameServerClient: accepted by nameserver, poolsize " + poolSize);
			}
			for(int i=0; i<poolSize; i++) {
				IbisIdentifier newid;
				try {
					newid = (IbisIdentifier) in.readObject();
				} catch (ClassNotFoundException e) {
					throw new IOException("Receive IbisIdent of unknown class " + e);
				}
				if(DEBUG) {
					System.out.println("NameServerClient: join of " + newid);
				}
				ibisImpl.join(newid);
				if(DEBUG) {
					System.out.println("NameServerClient: join of " + newid + " DONE");
				}
			}

			// Should we join ourselves?
			ibisImpl.join(id);

			socketFactory.close(in, out, s);
			new Thread(this, "NameServerClient accept thread").start();
			break;
		default:
			socketFactory.close(in, out, s);

			throw new StreamCorruptedException("NameServerClient: got illegal opcode " + opcode);
		}
	} 

	public boolean newPortType(String name, StaticProperties p) throws IOException {
		return portTypeNameServerClient.newPortType(name, p);
	}

	public long getSeqno(String name) throws IOException { 
		return portTypeNameServerClient.getSeqno(name);
	}

	public void leave() throws IOException { 
		if(DEBUG) {
			System.err.println("NS client: leave");
		}
		Socket s = socketFactory.createSocket(serverAddress, port, myAddress, 0 /* retry */);
		
		DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
		ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(dos));

		out.writeByte(IBIS_LEAVE);
		out.writeUTF(poolName);
		out.writeObject(id);
		out.flush();
		if(DEBUG) {
			System.err.println("NS client: leave sent");
		}

		DummyInputStream di = new DummyInputStream(s.getInputStream());
		ObjectInputStream in  = new ObjectInputStream(new BufferedInputStream(di));
		
		int temp = in.readByte();
		if(DEBUG) {
			System.err.println("NS client: leave ack received");
		}

		socketFactory.close(null, out, s);

//		stop = true;
//		this.interrupt();
		if(DEBUG) {
			System.err.println("NS client: leave DONE");
		}

	} 

	public void delete(IbisIdentifier ident) throws IOException {
			System.err.println("NS client: delete");	
			Socket s = socketFactory.createSocket(serverAddress, port, myAddress, 0 /*retry*/);
		
			DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(dos));

			out.writeByte(IBIS_DELETE);
			out.writeUTF(poolName);
			out.writeObject(ident);
			out.flush();
			System.err.println("NS client: delete sent");			

			DummyInputStream di = new DummyInputStream(s.getInputStream());
			ObjectInputStream in  = new ObjectInputStream(new BufferedInputStream(di));
			
			int temp = in.readByte();
			System.err.println("NS client: delete ack received");			

			socketFactory.close(in, out, s);
			System.err.println("NS client: delete DONE");			
	}
	
	public void reconfigure() throws IOException {
			Socket s = socketFactory.createSocket(serverAddress, port, myAddress, 0 /*retry*/);
		
			DummyOutputStream dos = new DummyOutputStream(s.getOutputStream());
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(dos));

			out.writeByte(IBIS_RECONFIGURE);
			out.writeUTF(poolName);
			out.flush();

			DummyInputStream di = new DummyInputStream(s.getInputStream());
			ObjectInputStream in  = new ObjectInputStream(new BufferedInputStream(di));
			
			int temp = in.readByte();

			socketFactory.close(in, out, s);
	}

	public void run() {
		if (DEBUG) { 
			System.out.println("NameServerClient: stread started");
		}

		while (true) { // !stop

			Socket s;
			IbisIdentifier id;

			try {
				s = socketFactory.accept(serverSocket);

				if (DEBUG) {
					System.out.println("NameServerClient: incoming connection from " + s.toString());
				}

			} catch (Exception e) {
				if (stop) { 
					if (DEBUG) { 
						System.out.println("NameServerClient: thread dying");
					}
					try { 
						serverSocket.close();
					} catch (IOException e1) { 						
					}
					return;
                                }
				throw new RuntimeException("NameServerClient: got an error " + e.getMessage());
			}
	
			int opcode = 666;

			try {
				DummyInputStream di = new DummyInputStream(s.getInputStream());			
				ObjectInputStream in  = new ObjectInputStream(new BufferedInputStream(di));

				opcode = in.readByte();
				if (DEBUG) {
				    System.out.println("NameServerClient: opcode " + opcode);
				}
	  
				switch (opcode) {
				case (IBIS_JOIN):
					id = (IbisIdentifier) in.readObject();
					if (DEBUG) {
					    System.out.println("NameServerClient: receive join request " + id);
					}
					socketFactory.close(in, null, s);
					ibisImpl.join(id);
					break;
				case (IBIS_LEAVE):
					id = (IbisIdentifier) in.readObject();
					socketFactory.close(in, null, s);
					if(id.equals(this.id)) {
						// received an ack from the nameserver that I left.
						if (DEBUG) { 
							System.out.println("NameServerClient: thread dying");
						}
						return;
					} else {
						ibisImpl.leave(id);
					}
					break;
				case (IBIS_DELETE):
					id = (IbisIdentifier) in.readObject();
					socketFactory.close(in, null, s);
					ibisImpl.delete(id);
					break;
				case (IBIS_RECONFIGURE):
					socketFactory.close(in, null, s);
					ibisImpl.reconfigure();
					break;
				default: 
					System.out.println("NameServerClient: got an illegal opcode " + opcode);
				}
			} catch (Exception e1) {
				System.out.println("Got an exception in NameServerClient.run (opcode = " + opcode + ") " + e1.toString());
				if(stop) return;
				e1.printStackTrace();
	  
				if (s != null) { 
					socketFactory.close(null, null, s);
				}
	  
			}
		}
	}  

	public ReceivePortIdentifier lookup(String name) throws IOException {
		return lookup(name, 0);
	}

	public ReceivePortIdentifier lookup(String name, long timeout) throws IOException {
		return receivePortNameServerClient.lookup(name, timeout);
	}

	public IbisIdentifier locate(String name) throws IOException {
		return locate(name, 0);
	} 

	public IbisIdentifier locate(String name, long timeout) throws IOException {
		/* not implemented yet */
		return null;
	} 

	public ReceivePortIdentifier [] query(IbisIdentifier ident)  throws IOException, ClassNotFoundException { 
		/* not implemented yet */
		return new ReceivePortIdentifier[0];
	}

	public Object elect(String election, Object candidate) throws IOException, ClassNotFoundException {
		return electionClient.elect(election, candidate);
	}

	//gosia	

	public Object reelect(String election, Object candidate, Object formerRuler) throws IOException, ClassNotFoundException {
		return electionClient.reelect(election, candidate, formerRuler);
	}
	
	public void bind(String name, ReceivePortIdentifier rpi) throws IOException {
		receivePortNameServerClient.bind(name, rpi);
	}
	
	public void rebind(String name, ReceivePortIdentifier rpi) throws IOException {
		receivePortNameServerClient.rebind(name, rpi);
	}
	
	public void unbind(String name) throws IOException {
		receivePortNameServerClient.unbind(name);
	}

	public String[] list(String pattern) throws IOException {
		return receivePortNameServerClient.list(pattern);
	}
	//end gosia
} 
