package ibis.impl.tcp;

import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
//import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.IPUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

public final class TcpIbis extends Ibis implements Config {

	private TcpIbisIdentifier ident;
	private InetAddress myAddress;

	private NameServer nameServer;
	private int poolSize;

	private Hashtable portTypeList = new Hashtable();

	private boolean open = false;
	private ArrayList joinedIbises = new ArrayList();
	private ArrayList leftIbises = new ArrayList();

	TcpPortHandler tcpPortHandler;
	private boolean ended = false;

	public TcpIbis() throws IbisException {
		// this is a 1.4 method.
		try {
			Runtime.getRuntime().addShutdownHook(new TcpShutdown());
		} catch (Exception e) {
			System.err.println("Warning: could not register tcp shutdown hook");
		}
	}
     
	protected PortType newPortType(String name, StaticProperties p)
		    throws IOException, IbisException {

		TcpPortType resultPort = new TcpPortType(this, name, p);
		p = resultPort.properties();

		if (nameServer.newPortType(name, p)) { 
			/* add type to our table */
			portTypeList.put(name, resultPort);

			if(DEBUG) {
				System.out.println(this.name + ": created PortType '" + name + "'");
			}
		}

		return resultPort;
	}

	long getSeqno(String name) throws IOException {
		return nameServer.getSeqno(name);
	}

	public Registry registry() {
		return nameServer;
	} 

	public StaticProperties properties() { 
		return staticProperties(implName);
	}

	public IbisIdentifier identifier() {
		return ident;
	}

	protected void init() throws IbisException, IOException { 
		if(DEBUG) {
			System.err.println("In TcpIbis.init()");
		}
		poolSize = 1;

		myAddress = IPUtils.getLocalHostAddress();
		if(myAddress == null) {
			System.err.println("ERROR: could not get my own IP address, exiting.");
			System.exit(1);
		}
		ident = new TcpIbisIdentifier(name, myAddress);

		if(DEBUG) {
			System.err.println("Created IbisIdentifier " + ident);
		}

		nameServer = NameServer.loadNameServer(this);

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
		synchronized(this) {
			if(ended) return;
			ended = true;
		}
		try { 
			if(nameServer != null) {
				nameServer.leave();
			}
			if(tcpPortHandler != null) {
				tcpPortHandler.quit();
			}
		} catch (Exception e) { 
			throw new IbisRuntimeException("TcpIbisNameServerClient: leave failed ", e);
		} 
	}

	public void poll() throws IOException {
		// Empty implementation, as TCP Ibis has interrupts.
	}

	void bindReceivePort(String name, ReceivePortIdentifier p) throws IOException {
		nameServer.bind(name, p);
	}

	void unbindReceivePort(String name) throws IOException {
		nameServer.unbind(name);
	}

	class TcpShutdown extends Thread {
		public void run() {
			end();
		}
	}
}
