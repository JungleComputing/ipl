package ibis.ipl.impl.tcp;

import ibis.ipl.*;

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
	protected TcpPortType type;
	private TcpReceivePortIdentifier ident;
	private int sequenceNumber = 0;
	private int connectCount = 0;
	String name; // needed to unbind
	private SerializationStreamConnectionHandler [] connections;
	private int connectionsIndex;
	private volatile boolean stop = false;
	boolean allowUpcalls = false;
	Upcall upcall;
	ReceivePortConnectUpcall connUpcall;
	private boolean started = false;
	private boolean connection_setup_present = false;
	private SerializationStreamReadMessage m = null;
	private TcpIbis ibis;
	protected boolean shouldLeave;
	private boolean delivered = false;

	TcpReceivePort(TcpIbis ibis, TcpPortType type, String name, Upcall upcall, ReceivePortConnectUpcall connUpcall) throws IOException {
		this.type   = type;
		this.name   = name;
		this.upcall = upcall;
		this.connUpcall = connUpcall;
		this.ibis = ibis;

		connections = new SerializationStreamConnectionHandler[2];
		connectionsIndex = 0;

		int port = ibis.tcpPortHandler.register(this);
		ident = new TcpReceivePortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier(), port);
	}

	// returns:  was the message already finised?
	private boolean doUpcall(SerializationStreamReadMessage m) throws IOException {
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
		SerializationStreamReadMessage old = m;

		if(m.isFinished) {
			throw new IOException("Finish is called twice on this message, port = " + name);
		}

		m.isFinished = true;
		m = null;
		notifyAll();

		if(upcall != null) {
			startNewHandlerThread(old);
		}
	}

	private void startNewHandlerThread(SerializationStreamReadMessage old) {
		SerializationStreamConnectionHandler h = old.getHandler();
//		h.createNewMessage();
//		new Thread(h, "TCP Connection Handler").start();
		ThreadPool.createNew(h);
	}


	boolean setMessage(SerializationStreamReadMessage m) throws IOException {
		m.isFinished = false;
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

	private synchronized SerializationStreamReadMessage getMessage(long timeout) {
		while((m == null || delivered) && ! shouldLeave) {
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
			if (connUpcall != null && ! connUpcall.gotConnection(id)) {
				return false;
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
/* For some reason, the available call seems to block for sun serialziation! Check this!! --Rob @@@ 
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
*/
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

	public ReceivePortIdentifier identifier() {
		return ident;
	}

	void leave(SerializationStreamConnectionHandler leaving,
		   TcpSendPortIdentifier si, InputStream in) {
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
				System.err.println("EEEK, connection handler not found in leave");
				System.exit(1);
			}

			ibis.tcpPortHandler.releaseInput(si, in);
		}

		// Don't hold the lock when calling user upcall functions. --Rob
		if (connUpcall != null) {
			connUpcall.lostConnection(si);
		}

		synchronized(this) {
			shouldLeave = true;
			notifyAll();
		}
	}

	public synchronized void free() {
		if (DEBUG) { 
			System.err.println("TcpReceivePort.free: " + name + ": Starting");
		}

		if(m != null) {
			System.err.println(ident + "EEK: a msg is alive, port = " + name + " fin = " + m.isFinished);
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
							if(m != null) {
								System.err.println(ident + "EEK2: a msg is alive, port = " + name + " fin = " + m.isFinished);
								System.err.println("opcode = " + m.readByte());
								System.exit(1);
							}
						} catch (Exception e) {
							System.err.println("Eek3: exc: " + e);
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
			SerializationStreamConnectionHandler con = 
				new SerializationStreamConnectionHandler(origin, this, in, id);

			if (connections.length == connectionsIndex) { 
				SerializationStreamConnectionHandler [] temp = 
					new SerializationStreamConnectionHandler[2*connections.length];
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
		System.err.println("forcedClose not implemented!");
	}

	public void forcedClose(long timeoutMillis) {
		System.err.println("forcedClose not implemented!");
	}

	public SendPortIdentifier[] connectedTo() {
		System.err.println("connectedTo not implemented!");
		return null;
	}

	public SendPortIdentifier[] lostConnections() {
		System.err.println("lostConnections not implemented!");
		return null;
	}

	public SendPortIdentifier[] newConnections() {
		System.err.println("newConnections not implemented!");
		return null;
	}
}
