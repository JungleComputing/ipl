package ibis.ipl.impl.tcp;

import ibis.ipl.Ibis;
import ibis.ipl.PortType;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Upcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.IbisException;
import ibis.ipl.Registry;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.impl.generic.IbisIdentifierTable;
import ibis.ipl.ReadMessage;

import ibis.ipl.impl.nameServer.*;

import java.net.Socket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.StringTokenizer;
import java.util.Properties;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.Hashtable;

import java.io.IOException;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.DataOutputStream;

public final class TcpIbis extends Ibis implements Config {

	private TcpIbisIdentifier ident;
	private InetAddress myAddress;

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
	private ArrayList joinedIbises = new ArrayList();
	private ArrayList leftIbises = new ArrayList();

	private final StaticProperties systemProperties = new StaticProperties();	

	TcpPortHandler tcpPortHandler;

	public TcpIbis() throws IbisException {
		// Set my properties.
		systemProperties.add("reliability", "true");
		systemProperties.add("multicast", "true") ;
		systemProperties.add("totally ordered multicast", "false") ;

		ibis.io.Conversion.classInit();
	}
     
	public PortType createPortType(String name, StaticProperties p)
		    throws IOException, IbisException {

		TcpPortType resultPort = new TcpPortType(this, name, p);
		p = resultPort.properties();

		PortTypeNameServerClient temp = tcpIbisNameServerClient.portTypeNameServerClient;

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

	protected void init() throws IbisException, IOException { 
		if(DEBUG) {
			System.err.println("In TcpIbis.init()");
		}
		poolSize = 1;

		Properties p = System.getProperties();
		nameServerName = p.getProperty("ibis.name_server.host");
		if (nameServerName == null) {
			throw new IbisException("property ibis.name_server.host is not specified");
		}

		nameServerPool = p.getProperty("ibis.name_server.key");
		if (nameServerPool == null) {
			throw new IbisException("property ibis.name_server.key is not specified");
		}

		String nameServerPortString = p.getProperty("ibis.name_server.port");
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

		nameServerInet = InetAddress.getByName(nameServerName);
		if(DEBUG) {
			System.err.println("Found nameServerInet " + nameServerInet);
		}

		String myIp = p.getProperty("ip_address");
		if (myIp == null) {
			myAddress = InetAddress.getLocalHost();
		} else {
			myAddress = InetAddress.getByName(myIp);
		}
		ident = new TcpIbisIdentifier(name, myAddress);

		if(DEBUG) {
			System.err.println("Created IbisIdentifier " + ident);
/*
			InetAddress[] res = InetAddress.getAllByName(myIp);
			for(int i=0; i<res.length; i++) {
				System.err.println("IP: " + res[i] + 
				    (res[i].isSiteLocalAddress() ? " SL" : " !SL") + 
				    (res[i].isLinkLocalAddress() ? " LL" : " !LL") + 
				    (res[i].isLoopbackAddress() ? " LOOP" : " !LOOP") + 
				    (res[i].isAnyLocalAddress() ? " ANYL" : " !ANYL") + 
				    (res[i].isMulticastAddress() ? " MULTI" : " !MULTI"));
			}
*/
		}

		tcpIbisNameServerClient = new NameServerClient(this, myAddress, ident, nameServerPool, nameServerInet, 
							       nameServerPort);
		tcpPortTypeNameServerClient = tcpIbisNameServerClient.portTypeNameServerClient;
		tcpReceivePortNameServerClient = tcpIbisNameServerClient.receivePortNameServerClient;
		tcpElectionClient = tcpIbisNameServerClient.electionClient;
		tcpRegistry = tcpIbisNameServerClient.registry;
		tcpPortHandler = new TcpPortHandler(ident);
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
		TcpIbisIdentifier ident = null;

		if(resizeHandler != null) {
			while(true) {
				synchronized(this) {
					if(joinedIbises.size() == 0) break;
					poolSize++;
					ident = (TcpIbisIdentifier)joinedIbises.remove(0);
				}
				resizeHandler.join(ident); // Don't hold the lock during user upcall
			}

			while(true) {
				synchronized(this) {
					if(leftIbises.size() == 0) break;
					poolSize--;
					ident = (TcpIbisIdentifier)leftIbises.remove(0);
				}
				resizeHandler.leave(ident); // Don't hold the lock during user upcall

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

	public ReadMessage poll() throws IOException {
		Object[] a = portTypeList.values().toArray();

		for(int i=0; i<a.length; i++) {
			TcpPortType t = (TcpPortType) a[i];
			ReadMessage m = t.poll(); // just forward all exceptions
			if(m != null) {
				return m;
			}
		}

		return null;
	}
}
