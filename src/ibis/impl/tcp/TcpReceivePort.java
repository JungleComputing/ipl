package ibis.impl.tcp;

import ibis.ipl.*;

import java.io.IOException;
import java.io.InputStream;
import ibis.util.*;
import java.util.ArrayList;

// why was shouldLeave here?
// If I create a receiveport, do a receive, and someone leaves, 
// the user gets an upcall, or can find out with a downcall.
// why should the receivePort die? --Rob

final class TcpReceivePort implements ReceivePort, TcpProtocol, Config {
	TcpPortType type;
	String name; // needed to unbind
	private TcpIbis ibis;
	private TcpReceivePortIdentifier ident;
	private int sequenceNumber = 0;
	private int connectCount = 0;
	private ConnectionHandler [] connections;
	private int connectionsIndex;
	private volatile boolean stop = false;
	private boolean allowUpcalls = false;
	private Upcall upcall;
	private ReceivePortConnectUpcall connUpcall;
	private boolean started = false;
	private boolean connection_setup_present = false;
	private TcpReadMessage m = null;
//	private boolean shouldLeave;
	private boolean delivered = false;
	private ArrayList lostConnections = new ArrayList();
	private ArrayList newConnections = new ArrayList();
	private boolean connectionAdministration = false;

	TcpReceivePort(TcpIbis ibis, TcpPortType type, String name, Upcall upcall, 
		       boolean connectionAdministration, ReceivePortConnectUpcall connUpcall) throws IOException {

		this.type   = type;
		this.upcall = upcall;
		this.connUpcall = connUpcall;
		this.ibis = ibis;
		this.connectionAdministration = connectionAdministration;
		if(connUpcall != null) connectionAdministration = true;

		if(name == null) {
			this.name = "anonymous";
		} else {
			this.name = name;
		}

		connections = new ConnectionHandler[2];
		connectionsIndex = 0;

		int port = ibis.tcpPortHandler.register(this);
		ident = new TcpReceivePortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier(), port);
	}

	// returns:  was the message already finised?
	private boolean doUpcall(TcpReadMessage m) throws IOException {
	        synchronized (this) {
				// Wait until the previous message was finished.
			while(this.m != null) {
				try {
					wait();
				} catch (InterruptedException e) {
					// Ignore.
				}
			}

			this.m = m;
		}

		upcall.upcall(m);

		/* The code below was so terribly wrong.
		 * You cannot touch m here anymore if it indeed
		 * was finished, because it might represent another message now!
		 * And, if that is not yet finished, things go terribly wrong ....
		 * On the other hand, if m is not finished yet, it is valid here.
		 * Problem here is, we don't know what is the case.
		 *
		 * The problem is fixed now, by allocating a new TcpReadMessage
		 * in the finish() call.
		 */
		synchronized(this) {
			if(!m.isFinished) { // It wasn't finished. Cool, this means that we don't have to start a new thread!
				this.m = null;
				notifyAll();

				return false;
			}
		}
		return true;
	}

	synchronized void finishMessage() throws IOException {
		TcpReadMessage old = m;

		if(m.isFinished) {
			throw new IOException("Finish is called twice on this message, port = " + name);
		}

		m.isFinished = true;
		m = null;
		notifyAll();

		if(upcall != null) {
			/* We need to create a new TcpReadMessage here.
			 * Otherwise, there is no way to find out later if a message
			 * was finished or not. The code at the end of doUpcall() (after
			 * the upcall itself) would be very wrong indeed (it was!)
			 * if we would not allocate a new message. The point is, after
			 * a finish(), the TcpReadMessage is used for new
			 * messages!
			 */
			ConnectionHandler h = old.getHandler();
			h.m = new TcpReadMessage(old);
			ThreadPool.createNew(h);
		}
	}


	boolean setMessage(TcpReadMessage m) throws IOException {
		m.isFinished = false;
		if(upcall != null) {
			return doUpcall(m);
		} else {
			setBlockingReceiveMessage(m);
			return false;
		}
	}

	private synchronized void setBlockingReceiveMessage(TcpReadMessage m) {
		// Wait until the previous message was finished.
		while(this.m != null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}

		this.m = m;
		delivered = false;
		notifyAll(); // now handle this message.

		// Wait until the receiver thread finishes this message.
		// We must wait here, because the thread that calls this method 
		// wants to read an opcode from the stream.
		// It can only read this opcode after the whole message is gone first.
		while(this.m != null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}
	}

	private synchronized TcpReadMessage getMessage(long timeout) {
		while((m == null || delivered)/* && ! shouldLeave*/) {
			try {
				if(timeout > 0) {
					wait(timeout);
				} else {
					wait();
				}
			} catch (Exception e) {
				// Ignore.
			}
		}
		delivered = true;
/*
		if(shouldLeave) {
			System.out.println("returned from receive operation");
		}
*/
		return m;
	}

	public synchronized void enableConnections() {		
		// Set 'starting' to true. This is always OK.
		started = true;
	}

	public synchronized void disableConnections() {
		// We may only set 'starting' to false if there is no 
		// connection being set up at the moment.
		
		while (connection_setup_present) { 
			try { 
				wait();
			} catch (Exception e) { 
				// Ignore
			} 
		} 
		started = false;
	}

	synchronized boolean connectionAllowed(TcpSendPortIdentifier id) { 
		if (started) { 
			if(connectionAdministration) {
				if (connUpcall != null) {
					if (! connUpcall.gotConnection(this, id)) {
						return false;
					}
				} else {
					newConnections.add(id);
				}
			}
			connection_setup_present = true;
			notifyAll();
			return true;
		} else {
			return false;
		}
	} 

	public synchronized void enableUpcalls() {
		allowUpcalls = true;
		notifyAll();
	}

	public synchronized void disableUpcalls() {
		allowUpcalls = false;
	}

	public ReadMessage poll() throws IOException {
		if(upcall != null) {
			Thread.yield();
			return null;
		}

		// Blocking receive...
		synchronized (this) { // must this be synchronized? --Rob
			return m;
		}
	}

	public synchronized ReadMessage poll(ReadMessage finishMe) throws IOException {
		if (finishMe != null) {
			finishMe.finish();
		}
		return poll();
	}

	public ReadMessage receive() throws IOException { 
		if(upcall != null) {
			throw new IOException("Configured Receiveport for upcalls, downcall not allowed");
		}

		ReadMessage m = getMessage(-1);

		if (m == null) {
			throw new IOException("receive port closed");
		}
		return m;
	}

	public ReadMessage receive(ReadMessage finishMe) throws IOException { 
		if (finishMe != null) {
			finishMe.finish();
		}

		return receive();
	}

	public ReadMessage receive(long timeoutMillis) throws IOException {
		if(upcall != null) {
			throw new IOException("Configured Receiveport for upcalls, downcall not allowed");
		}

		return getMessage(timeoutMillis);
	}

	public ReadMessage receive(ReadMessage finishMe, long timeoutMillis) throws IOException {
		if (finishMe != null) {
			finishMe.finish();
		}

		return receive(timeoutMillis);
	}

	public DynamicProperties properties() {
		return null;
	}

	public String name() {
		return name;
	}

	public ReceivePortIdentifier identifier() {
		return ident;
	}

	// called from the connectionHander.
	void leave(ConnectionHandler leaving, Exception e) {
		synchronized(this) {
			boolean found = false;
			if (DEBUG) {
				System.err.println("TcpReceivePort.leave: " + name);
			}
			for (int i=0;i<connectionsIndex; i++) {
				if (connections[i] == leaving) {
					connections[i] = connections[connectionsIndex-1];
					connections[connectionsIndex-1] = null;
					connectionsIndex--;
					found = true;
					break;
				}
			}

			if(!found) {
				throw new IbisError("Connection handler not found in leave");
			}
		}

		// Don't hold the lock when calling user upcall functions. --Rob
		if(connectionAdministration) {
			if (connUpcall != null) {
				Exception x = e;
				if(x == null) {
					x = new Exception("sender closed connection");
				}
				connUpcall.lostConnection(this, leaving.origin, x);
			} else {
				lostConnections.add(leaving.origin);
			}
		}

		synchronized(this) {
//			shouldLeave = true;
			notifyAll();
		}
	}

	private synchronized ConnectionHandler removeConnection(int index) {
		ConnectionHandler res = connections[index];
		connections[index] = connections[connectionsIndex-1];
		connections[connectionsIndex-1] = null;
		connectionsIndex--;
			
		return res;
	}

	public synchronized void free() {
		if (DEBUG) { 
			System.err.println("TcpReceivePort.free: " + name + ": Starting");
		}

		if(m != null) {
			throw new IbisError("Doing free while a msg is alive, port = " + name + " fin = " + m.isFinished);
		}

		/* unregister with nameserver */
		try {
			type.freeReceivePort(name);
		} catch (Exception e) {
			// Ignore.
		}

		while (connectionsIndex > 0) {
			if (DEBUG) {
				System.err.println(name + " waiting for all connections to close (" + connectionsIndex + ")");
			}
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}

		if (DEBUG) { 
			System.err.println(name + " all connections closed");
		}

		if (DEBUG) { 
			System.err.println(name + ":done receiveport.free");
		}
	}

	synchronized void connect(TcpSendPortIdentifier origin, InputStream in) {
		try {
			ConnectionHandler con = 
				new ConnectionHandler(ibis, origin, this, in);

			if (connections.length == connectionsIndex) { 
				ConnectionHandler [] temp = 
					new ConnectionHandler[2*connections.length];
				for (int i=0;i<connectionsIndex;i++) { 
					temp[i] = connections[i];
				}
				
				connections = temp;
			} 

			connections[connectionsIndex++] = con;
			ThreadPool.createNew(con);

			connection_setup_present = false;
			notifyAll();
		} catch (Exception e) {
			System.err.println("Got exception " + e);
			e.printStackTrace();
		}
	}

	public void forcedClose() {
		if(m != null) {
			throw new IbisError("Doing forcedClose while a msg is alive, port = " + name + " fin = " + m.isFinished);
		}

		/* unregister with nameserver */
		try {
			type.freeReceivePort(name);
		} catch (Exception e) {
			// Ignore.
		}

		while(connectionsIndex > 0) {
			ConnectionHandler conn = removeConnection(0);
			conn.die();

			if(connectionAdministration) {
				if (connUpcall != null) {
					connUpcall.lostConnection(this, conn.origin,
						  new Exception("receiver forcibly closed connection"));
				} else {
					lostConnections.add(conn.origin);
				}
			}
		}
	}

	public void forcedClose(long timeoutMillis) {
		try {
			wait(timeoutMillis);
		} catch (Exception e) {
				// Ignore.
		}

		forcedClose();
	}

	public synchronized SendPortIdentifier[] connectedTo() {
		SendPortIdentifier[] res = new SendPortIdentifier[connectionsIndex];
		for(int i=0; i<connectionsIndex; i++) {
			res[i] = connections[i].origin;
		}

		return res;
	}

	public synchronized SendPortIdentifier[] lostConnections() {
		SendPortIdentifier[] res = (SendPortIdentifier[]) lostConnections.toArray();
		lostConnections.clear();
		return res;
	}

	public synchronized SendPortIdentifier[] newConnections() {
		SendPortIdentifier[] res = (SendPortIdentifier[]) newConnections.toArray();
		newConnections.clear();
		return res;
	}

	public int hashCode() {
		return name.hashCode();
	}

	public boolean equals(Object obj) {
		if(obj instanceof TcpReceivePort) {
			TcpReceivePort other = (TcpReceivePort) obj;
			return name.equals(other.name);
		} else if (obj instanceof String) {
			String s = (String) obj;
			return s.equals(name);
		} else {
			return false;
		}
	}
}
