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
	
	private boolean started = false;
	private boolean connection_setup_present = false;

	TcpReceivePort(TcpPortType type, String name) throws IbisIOException {
		this(type, name, null);
	}

	TcpReceivePort(TcpPortType type, String name, Upcall upcall) throws IbisIOException {
		this.type   = type;
		this.name   = name;
		this.upcall = upcall;

		connections = new ConnectionHandler[2];
		connectionsSize = 2;
		connectionsIndex = 0;

		int port = TcpIbis.tcpPortHandler.register(this);
		ident = new TcpReceivePortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier(), port);
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
		allowUpcalls = true;
		notifyAll();
	}

	public synchronized void disableUpcalls() {
		allowUpcalls = false;
	}

	synchronized int getSequenceNr() {
		return sequenceNr++;
	}

	private synchronized ReadMessage doPoll() throws IbisIOException {
		ReadMessage m = null;
//		System.err.println("polling, connections = " + connectionsIndex);
		while (aMessageIsAlive) { 
			try { 
				wait();
			} catch (InterruptedException e) {
				// ignore.
			} 
		}
		
		for (int i=0; i<connectionsIndex; i++) { 
			m = connections[i].poll();
			
			if (m != null) { 
				aMessageIsAlive = true;
//				System.err.println(ident + ": polling DONE, returned m");
//				new Exception().printStackTrace();
				return m;
			}
		}

//		System.err.println("polling DONE, no m");
		return m;
	}

	public synchronized ReadMessage poll() throws IbisIOException {
		if(upcall != null) {
			Thread.yield();
			return null;
		}

		return doPoll();
	}

	public synchronized ReadMessage poll(ReadMessage finishMe) throws IbisIOException {
		if (finishMe != null) {
			finishMe.finish();
		}
		return poll();
	}

	public ReadMessage receive() throws IbisIOException { 
		ReadMessage m = null;

		while (m == null) {
			m = doPoll();
			if (m != null) { 
				break;
			} 
			Thread.yield();
		}
		
		return m;
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
//		if (TcpIbis.DEBUG) { 
			System.err.println("TcpReceivePort.free: " + name + ": Starting");
//		}

			if(aMessageIsAlive) {
				System.err.println("EEK: a msg is alive!");
			}

		while (connectionsIndex > 0) {

			if (upcall != null) { 
//				if (TcpIbis.DEBUG) { 
					System.err.println(name + " waiting for all connections to close (" + connectionsIndex + ")");
//				}
				try {
					wait();
				} catch (Exception e) {
					// Ignore.
				}
			} else { 
//				if (TcpIbis.DEBUG) { 
					System.err.println(name + " trying to close all connections (" + connectionsIndex + ")");
//				}
				
				while (connectionsIndex > 0) { 
				
					for (int i=0;i<connectionsIndex;i++) { 
//						if (TcpIbis.DEBUG) { 
							System.err.println(name + " trying to close " + i);
//						}
						
						try {
							ReadMessage m = connections[i].poll();
							if(m != null) {
								// o my, a message was in the pipe while this free was issued!
								System.err.println("EEK, message in pipe during free, this is a bug in your code: the sender and receiver do not agree to close the channel");
								return;
//								System.exit(1);

							}
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

//				if (TcpIbis.DEBUG) { 
					System.err.println(name + " all connections closed");
//				}

		/* unregister with name server */
		try {
			type.freeReceivePort(name);
		} catch (Exception e) {
			// Ignore.
		}

//		if (TcpIbis.DEBUG) { 
			System.err.println(name + ":done receiveport.free");
//		}
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
				System.err.println("EEK: serialization type unknown");
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
			System.err.println("Got exception " + e);
			e.printStackTrace();
		} 
	}
}
