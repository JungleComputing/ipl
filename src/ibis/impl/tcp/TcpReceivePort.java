package ibis.ipl.impl.tcp;

import ibis.ipl.ReceivePort;
import ibis.ipl.ReadMessage;
import ibis.ipl.DynamicProperties;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.IbisIOException;
import ibis.ipl.Upcall;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedInputStream;

import ibis.io.*;
import java.io.*;
import java.util.ArrayList;

final class TcpReceivePort implements ReceivePort, TcpProtocol {

	private int sequenceNr = 0;
	private TcpPortType type;
	private TcpReceivePortIdentifier ident;
	private int sequenceNumber = 0;
	private int connectCount = 0;
	String name; // needed to unbind

	private ConnectionHandler [] connections;
	private int connectionsSize;
	private int connectionsIndex;

	private volatile boolean stop = false;

	boolean aMessageIsAlive = false;
	boolean allowUpcalls = false;
	Upcall upcall;	
	
	/* We don't want to hold the port lock during upcalls, so create a seperate object */
	Object upcallLock = new Object();
	
	private boolean started = false;
	private boolean connection_setup_present = false;

	TcpReceivePort(TcpPortType type, String name) throws IbisIOException {
		this(type, name, null);
	}

	TcpReceivePort(TcpPortType type, String name, Upcall upcall) throws IbisIOException {
		this.type   = type;
		this.name   = name;
		this.upcall = upcall;

		ident = new TcpReceivePortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier());

		connections = new ConnectionHandler[2];
		connectionsSize = 2;
		connectionsIndex = 0;

		TcpIbis.tcpPortHandler.register(this);
	}

	synchronized void doUpcall(ReadMessage m) { 
		while (!allowUpcalls || aMessageIsAlive) {
			try {
				wait();
			} catch(InterruptedException e) {
				// Ignore.
			}
		}
		upcall.upcall(m);

		while (aMessageIsAlive) {
			try {
				wait();
			} catch(InterruptedException e) {
				// Ignore.
			}
		}		
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

		if (started) { 
			connection_setup_present = true;
			notifyAll();
			return true;
		} else {
			return false;
		}
	} 


	public synchronized void enableUpcalls() {
		synchronized(upcallLock) {
			allowUpcalls = true;
			upcallLock.notifyAll();
		}
	}

	public synchronized void disableUpcalls() {
		synchronized(upcallLock) {
			allowUpcalls = false;
		}
	}

	synchronized int getSequenceNr() {
		return sequenceNr++;
	}

	private synchronized ReadMessage doPoll() throws IbisIOException { 

		ReadMessage m = null;

		while (aMessageIsAlive) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore.
			} 
		}
		
		for (int i=0;i<connectionsIndex;i++) { 
			m = connections[i].poll();
			
			if (m != null) { 
				aMessageIsAlive = true;
				return m;
			}
		}

		return m;		
	}

	public ReadMessage receive() throws IbisIOException { 

		ReadMessage m = null;

		while (m == null) {		
			m = doPoll();				
			Thread.yield();
		}
		
		return m;
	}

	public DynamicProperties properties() { 
		return null;
	}

	public ReceivePortIdentifier identifier() { 
		return ident;
	} 

	synchronized void leave(ConnectionHandler leaving, TcpSendPortIdentifier si, int id) {

		for (int i=0;i<connectionsIndex;i++) { 
			if (connections[i] == leaving) { 
				connections[i] = connections[connectionsIndex-1];
				connectionsIndex--;
				break;
			} 
		}

		TcpIbis.tcpPortHandler.releaseInput(si, id);
		notifyAll();
	}

	public synchronized void free() {
		
		if (TcpIbis.DEBUG) { 
			System.out.println("TcpReceive{ort.free: " + name + ": Starting");
		}

		while (connectionsIndex > 0) {

			if (upcall != null) { 
				if (TcpIbis.DEBUG) { 
					System.out.println(name + " waiting for all connections to close (" + connectionsIndex + ")");
				}
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			} else { 
				if (TcpIbis.DEBUG) { 
					System.out.println(name + " trying to close all connections (" + connectionsIndex + ")");
				}
				
				while (connectionsIndex > 0) { 
				
					for (int i=0;i<connectionsIndex;i++) { 
						if (TcpIbis.DEBUG) { 
							System.out.println(name + " trying to close " + i);
						}
						
						try { 
							connections[i].poll();						
						} catch (IbisIOException e) {
							// Ignore
						}
					}
					if(connectionsIndex > 0) {
						try { 
							Thread.sleep(1);
						}	catch (Exception e) {
							// Ignore
						}
					}			
				}
			}
		}

		/* unregister with name server */
		try {
			type.freeReceivePort(name);
		} catch (Exception e) {
				// Ignore.
		}

		if (TcpIbis.DEBUG) { 
			System.out.println(name + ":done receiveport.free");
		}
	}

	synchronized void connect(TcpSendPortIdentifier origin, InputStream in, int id) {	

		try { 
//			out.println(name + " ADDING CONNECTION");			
				
			ConnectionHandler con = null;

			switch(type.serializationType) {
			case TcpPortType.SERIALIZATION_SUN:
				con = new ObjectStreamConnectionHandler(origin, this, in, id);
				break;
			case TcpPortType.SERIALIZATION_MANTA:
				con = new MantaTypedBufferStreamConnectionHandler(origin, this, in, id);
				break;
			default:
				System.out.println("EEK");
				System.exit(1);
			}

			if (connectionsSize == connectionsIndex) { 
				
				ConnectionHandler [] temp = new ConnectionHandler[2*connectionsSize];
				for (int i=0;i<connectionsIndex;i++) { 
					temp[i] = connections[i];
				}
				
				connections = temp;
				connectionsSize = 2*connectionsSize;
			} 
			
			connections[connectionsIndex++] = con;

			if (upcall != null) {
				new Thread(con).start();
			}			

			connection_setup_present = false;
			notifyAll();

		} catch (Exception e) { 
			System.out.println("Got exception " + e);
			e.printStackTrace();
		} 
	}
}
