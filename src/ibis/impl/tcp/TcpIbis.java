package ibis.ipl.impl.tcp;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.Registry;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.generic.IbisIdentifierTable;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class TcpIbis extends Ibis {

	static final boolean DEBUG = false;

	static TcpIbis globalIbis;

	IbisIdentifierTable identTable = new IbisIdentifierTable();

	private TcpIbisIdentifier ident; 

	protected TcpIbisNameServerClient        tcpIbisNameServerClient;
	protected TcpPortTypeNameServerClient    tcpPortTypeNameServerClient;
	protected TcpReceivePortNameServerClient tcpReceivePortNameServerClient;
	protected TcpElectionClient              tcpElectionClient;
	protected TcpRegistry                    tcpRegistry;

	private String nameServerName;
	private String nameServerPool;
	private InetAddress nameServerInet;

	private int poolSize;

	private Hashtable portTypeList = new Hashtable();

	private boolean open = false;
	private Vector joinedIbises = new Vector();
	private Vector leftIbises = new Vector();

	private final StaticProperties systemProperties = new StaticProperties();	

	static TcpPortHandler tcpPortHandler;

	public TcpIbis() throws IbisException {

		if (globalIbis == null) {
			globalIbis = this;
		}

		// Set my properties.
		systemProperties.add("reliability", "true");
		systemProperties.add("multicast", "false") ;
		systemProperties.add("totally ordered", "false") ;
	}
     
	public PortType createPortType(String name, StaticProperties p)
		    throws IbisException, IbisIOException {

		TcpPortType resultPort = new TcpPortType(this, name, p);		
		p = resultPort.properties();

		TcpPortTypeNameServerClient temp = tcpIbisNameServerClient.tcpPortTypeNameServerClient;

		if (temp.newPortType(name, p)) { 
			/* add type to our table */
			portTypeList.put(name, resultPort);

			if(TcpIbis.DEBUG) {
				System.out.println(this.name + ": created PortType '" + name + "'");
			}
		}

		return resultPort;
	}

	public Registry registry() {
		return tcpRegistry;
	} 

	public StaticProperties properties() { 
		return systemProperties;
	}

	public IbisIdentifier identifier() {
		return ident;
	}

	protected void init() throws IbisException, IbisIOException { 

		poolSize = 1;

		Properties p = System.getProperties();
		nameServerName = p.getProperty("name_server");
		if (nameServerName == null) {
			throw new IbisException("property name_server is not specified");
		}

		nameServerPool = p.getProperty("name_server_pool");
		if (nameServerPool == null) {
			throw new IbisException("property name_server_pool is not specified");
		}

		try {
			nameServerInet = InetAddress.getByName(nameServerName);
		} catch (UnknownHostException e) {
			throw new IbisIOException("cannot get ip of name server", e);
		}

		try {
			ident = new TcpIbisIdentifier();
			ident.address = InetAddress.getLocalHost();
			ident.name = name;
		} catch (UnknownHostException e) {
			throw new IbisIOException("cannot get ip of local host", e);
		}

		try { 
			tcpIbisNameServerClient = new TcpIbisNameServerClient(this, ident, nameServerPool, nameServerInet, 
									      TcpIbisNameServer.TCP_IBIS_NAME_SERVER_PORT_NR);
			tcpPortTypeNameServerClient = tcpIbisNameServerClient.tcpPortTypeNameServerClient;
			tcpReceivePortNameServerClient = tcpIbisNameServerClient.tcpReceivePortNameServerClient;
			tcpElectionClient = tcpIbisNameServerClient.tcpElectionClient;
			tcpRegistry = tcpIbisNameServerClient.tcpRegistry;
			tcpPortHandler = new TcpPortHandler(ident);
		} catch (IOException e) { 
			throw new IbisIOException("cannot create TcpIbisNameServerClient", e);
		}
	}

	void join(TcpIbisIdentifier joinIdent) { 
		synchronized (this) {
			if(!open && resizeHandler != null) {
				joinedIbises.add(joinIdent);
				return;
			}
		
			// this method forwards the join to the application running on top of ibis.		
			if(DEBUG) {
				System.out.println(name + ": Ibis '" + joinIdent.name + "' joined"); 
			}
			
			poolSize++;
		}

		if(resizeHandler != null) {
			resizeHandler.join(joinIdent);
		}
	}

	void leave(TcpIbisIdentifier leaveIdent) { 
		synchronized (this) {
			if(!open && resizeHandler != null) {
				leftIbises.add(leaveIdent);
				return;
			}

			// this method forwards the leave to the application running on top of ibis.
			if(DEBUG) {
				System.out.println(name + ": Ibis '" + leaveIdent.name + "' left"); 
			}
			poolSize--;
		}

		if(resizeHandler != null) {
			resizeHandler.leave(leaveIdent);
		}
	}

	public PortType getPortType(String name) { 
		return (PortType) portTypeList.get(name);
	} 

	public void openWorld() {
		if(resizeHandler != null) {
			while(joinedIbises.size() > 0) {
				resizeHandler.join((TcpIbisIdentifier)joinedIbises.remove(0));
				poolSize++;
			}

			while(leftIbises.size() > 0) {
				resizeHandler.leave((TcpIbisIdentifier)leftIbises.remove(0));
				poolSize--;
			}
		}
		
		synchronized (this) {
			open = true;
		}

		if(DEBUG) {
			System.out.println(name + ": Ibis started"); 
		}
	}

	public synchronized void closeWorld() {
		open = false;
	}

	public void end() {
		try { 
			tcpIbisNameServerClient.leave();		
			tcpPortHandler.quit();			
		} catch (Exception e) { 
			throw new RuntimeException("TcpIbisNameServerClient: leave failed " + e);
		} 
	}

	public void poll() {
		System.out.println("poll not implemented");
	}
}
