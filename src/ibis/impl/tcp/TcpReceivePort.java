package ibis.ipl.impl.tcp;

import ibis.ipl.ReceivePort;
import ibis.ipl.ReadMessage;
import ibis.ipl.DynamicProperties;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.IbisIOException;
import ibis.ipl.Upcall;

import java.net.Socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import ibis.io.*;
import ibis.ipl.impl.generic.*;
import java.io.*;
import java.util.ArrayList;

final class TcpReceivePort implements ReceivePort, TcpProtocol, Config {
	private int sequenceNr = 0;
	protected TcpPortType type;
	private TcpReceivePortIdentifier ident;
	private int sequenceNumber = 0;
	private int connectCount = 0;
	String name; // needed to unbind

	private SerializationStreamConnectionHandler [] connections;
	private int connectionsSize;
	private int connectionsIndex;

	private volatile boolean stop = false;

	boolean allowUpcalls = false;
	Upcall upcall;
	
	private boolean started = false;
	private boolean connection_setup_present = false;

	private SerializationStreamReadMessage m = null;


	TcpReceivePort(TcpPortType type, String name) throws IbisIOException {
		this(type, name, null);
	}

	TcpReceivePort(TcpPortType type, String name, Upcall upcall) throws IbisIOException {
		this.type   = type;
		this.name   = name;
		this.upcall = upcall;

		connections = new SerializationStreamConnectionHandler[2];
		connectionsSize = 2;
		connectionsIndex = 0;

		int port = TcpIbis.tcpPortHandler.register(this);
		ident = new TcpReceivePortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier(), port);
	}

	// returns:  was the message already finised?
	boolean doUpcall(SerializationStreamReadMessage m) {
	        synchronized (this) {
			// Wait until the previous message was finished.
			while(this.m != null) {
				try {
					wait();
				} catch (Exception e) {
				// Ignore.
				}
			}

			this.m = m;
		}

		upcall.upcall(m);

		synchronized(this) {
			if(!m.isFinished) { // It wasn't finished. Cool, this means that we don't have to start a new thread!
				this.m = null;
				notifyAll();

				return false;
			}
		}
		return true;
	}

	synchronized void finishMessage() {
		SerializationStreamReadMessage old = m;
		m = null;
		notifyAll();

		if(upcall != null) {
			startNewHandlerThread(old);
		}
	}

	void startNewHandlerThread(SerializationStreamReadMessage old) {
		SerializationStreamConnectionHandler h = old.getHandler();
		h.createNewMessage();
//		new Thread(h).start();
		ThreadPool.createNew(h);
	}


	boolean setMessage(SerializationStreamReadMessage m) {
		if(upcall != null) {
			return doUpcall(m);
		} else {
			setBlockingReceiveMessage(m);
			return false;
		}
	}

	private synchronized void setBlockingReceiveMessage(SerializationStreamReadMessage m) {
		// Wait until the previous message was finished.
		while(this.m != null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}

		this.m = m;
		notifyAll(); // now handle this message.

		// Wait until the receiver thread finishes this message.
		while(this.m != null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}
	}

	synchronized SerializationStreamReadMessage getMessage() {
		while(m == null) {
			try {
				wait();
			} catch (Exception e) {
				// Ignore.
			}
		}

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

	public synchronized boolean setupConnection(TcpSendPortIdentifier id) { 
//		System.err.println("setupConnection"); System.err.flush();
		if (started) { 
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

	synchronized int getSequenceNr() {
		return sequenceNr++;
	}

	public ReadMessage poll() throws IbisIOException {
		while(true) {
			boolean success = false;
			synchronized(this) {
				for (int i=0; i<connectionsIndex; i++) { 
					if(connections[i].m.available() > 0) {
						success = true;
						break;
					}
				}
			}

			if(success) {
				Thread.yield();
			} else {
				return null;
			}
		}
	}

	public synchronized ReadMessage poll(ReadMessage finishMe) throws IbisIOException {
		if (finishMe != null) {
			finishMe.finish();
		}
		return poll();
	}

	public ReadMessage receive() throws IbisIOException { 
		if(upcall != null) {
			throw new IbisIOException("illegal receive");
		}

		return getMessage();
	}

	public ReadMessage receive(ReadMessage finishMe) throws IbisIOException { 
		if (finishMe != null) {
			finishMe.finish();
		}

		return receive();
	}

	public DynamicProperties properties() { 
		return null;
	}

	public ReceivePortIdentifier identifier() { 
		return ident;
	} 

	synchronized void leave(SerializationStreamConnectionHandler leaving, TcpSendPortIdentifier si, int id) {
		boolean found = false;
		for (int i=0;i<connectionsIndex; i++) { 
			if (connections[i] == leaving) { 
				connections[i] = connections[connectionsIndex-1];
				connectionsIndex--;
				found = true;
				break;
			} 
		}

		if(!found) {
			System.err.println("EEEK, connection handler not found in leave");
			System.exit(1);
		}

		TcpIbis.tcpPortHandler.releaseInput(si, id);
		notifyAll();
	}

	public synchronized void free() {
		if (DEBUG) { 
			System.err.println("TcpReceivePort.free: " + name + ": Starting");
		}

		if(m != null) {
			System.err.println("EEK: a msg is alive!");
		}

		while (connectionsIndex > 0) {
			if (upcall != null) { 
				if (DEBUG) { 
					System.err.println(name + " waiting for all connections to close (" + connectionsIndex + ")");
				}
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			} else { 
				if (DEBUG) { 
					System.err.println(name + " trying to close all connections (" + connectionsIndex + ")");
				}
				
				while (connectionsIndex > 0) { 
					for (int i=0;i<connectionsIndex;i++) { 
						if (DEBUG) { 
							System.err.println(name + " trying to close " + i);
						}
					}
					if(connectionsIndex > 0) {
						try { 
							wait(500);
						} catch (Exception e) {
							// Ignore
						}
					}
				}
			}
		}

		if (DEBUG) { 
			System.err.println(name + " all connections closed");
		}

		/* unregister with name server */
		try {
			type.freeReceivePort(name);
		} catch (Exception e) {
			// Ignore.
		}

		if (DEBUG) { 
			System.err.println(name + ":done receiveport.free");
		}
	}

	synchronized void connect(TcpSendPortIdentifier origin, InputStream in, int id) {	
		try {
//			out.println(name + " ADDING CONNECTION");			
				
			SerializationStreamConnectionHandler con = new SerializationStreamConnectionHandler(origin, this, in, id);

			if (connectionsSize == connectionsIndex) { 
				SerializationStreamConnectionHandler [] temp = new SerializationStreamConnectionHandler[2*connectionsSize];
				for (int i=0;i<connectionsIndex;i++) { 
					temp[i] = connections[i];
				}
				
				connections = temp;
				connectionsSize = 2*connectionsSize;
			} 

			connections[connectionsIndex++] = con;

//			if (upcall != null) {
				new Thread(con).start();
//			}

			connection_setup_present = false;
			notifyAll();
		} catch (Exception e) {
			System.err.println("Got exception " + e);
			e.printStackTrace();
		}
	}
}
