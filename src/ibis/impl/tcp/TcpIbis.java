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

import ibis.ipl.impl.nameServer.*;

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

public final class TcpIbis extends Ibis implements Config {

	static TcpIbis globalIbis;

	IbisIdentifierTable identTable = new IbisIdentifierTable();

	private TcpIbisIdentifier ident;

	protected NameServerClient        tcpIbisNameServerClient;
	protected PortTypeNameServerClient    tcpPortTypeNameServerClient;
	protected ReceivePortNameServerClient tcpReceivePortNameServerClient;
	protected ElectionClient              tcpElectionClient;
	protected Registry                    tcpRegistry;

	private String nameServerName;
	private String nameServerPool;
	private InetAddress nameServerInet;
	private int nameServerPort;

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
		systemProperties.add("multicast", "true") ;
		systemProperties.add("totally ordered multicast", "false") ;

		ibis.io.Conversion.classInit();
	}
     
	public PortType createPortType(String name, StaticProperties p)
		    throws IbisException, IbisIOException {

		TcpPortType resultPort = new TcpPortType(this, name, p);		
		p = resultPort.properties();

		PortTypeNameServerClient temp = tcpIbisNameServerClient.tcpPortTypeNameServerClient;

		if (temp.newPortType(name, p)) { 
			/* add type to our table */
			portTypeList.put(name, resultPort);

			if(DEBUG) {
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

		if(DEBUG) {
			System.err.println("In TcpIbis.init()");
		}
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

		String nameServerPortString = p.getProperty("name_server_port");
		if (nameServerPortString == null) {
			nameServerPort = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
		} else {
			try {
				nameServerPort = Integer.parseInt(nameServerPortString);
				System.err.println("Using nameserver port: " + nameServerPort);
			} catch (Exception e) {
				System.err.println("illegal nameserver port: " + nameServerPortString + ", using default");
			}
		}

		try {
			nameServerInet = InetAddress.getByName(nameServerName);
		} catch (UnknownHostException e) {
			throw new IbisIOException("cannot get ip of name server", e);
		}
		if(DEBUG) {
			System.err.println("Found nameServerInet " + nameServerInet);
		}

		try {
			ident = new TcpIbisIdentifier(name, InetAddress.getLocalHost());
		} catch (UnknownHostException e) {
			throw new IbisIOException("cannot get ip of local host", e);
		}
		if(DEBUG) {
			System.err.println("Made IbisIdentifier " + ident);
		}

		try { 
			tcpIbisNameServerClient = new NameServerClient(this, ident, nameServerPool, nameServerInet, 
									      nameServerPort);
			tcpPortTypeNameServerClient = tcpIbisNameServerClient.tcpPortTypeNameServerClient;
			tcpReceivePortNameServerClient = tcpIbisNameServerClient.tcpReceivePortNameServerClient;
			tcpElectionClient = tcpIbisNameServerClient.tcpElectionClient;
			tcpRegistry = tcpIbisNameServerClient.tcpRegistry;
			tcpPortHandler = new TcpPortHandler(ident);
		} catch (IOException e) { 
			throw new IbisIOException("cannot create TcpIbisNameServerClient", e);
		}
		if(DEBUG) {
			System.err.println("Out of TcpIbis.init()");
		}
	}

	/**
	 * this method forwards the join to the application running on top of ibis.
	 */
	public void join(IbisIdentifier joinIdent) { 
		synchronized (this) {
			if(!open && resizeHandler != null) {
				joinedIbises.add(joinIdent);
				return;
			}

			if(DEBUG) {
				System.out.println(name + ": Ibis '" + joinIdent.name() + "' joined"); 
			}
			
			poolSize++;
		}

		if(resizeHandler != null) {
			resizeHandler.join(joinIdent);
		}
	}

	/**
	 * this method forwards the leave to the application running on top of
	 * ibis.
	 */
	public void leave(IbisIdentifier leaveIdent) { 
		synchronized (this) {
			if(!open && resizeHandler != null) {
				leftIbises.add(leaveIdent);
				return;
			}


			if(DEBUG) {
				System.out.println(name + ": Ibis '" + leaveIdent.name() + "' left"); 
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
