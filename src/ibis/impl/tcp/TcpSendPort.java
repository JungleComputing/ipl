package ibis.ipl.impl.tcp;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.DynamicProperties;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ConnectionTimedOutException;
import ibis.io.Replacer;

import java.util.Vector;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;

import ibis.io.*;

final class TcpSendPort implements SendPort, Config {
	TcpPortType type;
	TcpSendPortIdentifier ident;
	String name;
	private boolean aMessageIsAlive = false;
	SerializationStreamSender sender;
	Replacer replacer = null;
	TcpIbis ibis;

	TcpSendPort(TcpIbis ibis, TcpPortType type) throws IOException {
		this(ibis, type, null, null);
	}

	TcpSendPort(TcpIbis ibis, TcpPortType type, Replacer r) throws IOException {
		this(ibis, type, r, null);
	}

	TcpSendPort(TcpIbis ibis, TcpPortType type, String name) throws IOException {
		this(ibis, type, null, name);
	}

	TcpSendPort(TcpIbis ibis, TcpPortType type, Replacer r, String name) throws IOException {
		this.name = name;
		this.type = type;
		this.replacer = r;
		this.ibis = ibis;
		ident = new TcpSendPortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier());
		sender = new SerializationStreamSender(this);
	}

	// called by ConnectionHandler
	void connect(TcpReceivePortIdentifier ri, OutputStream sout) throws IOException {
		if(aMessageIsAlive) {
			throw new IOException("A message was alive while adding a new connection");
		}

		sender.connect(ri, sout);
	}

	public void connect(ReceivePortIdentifier receiver) throws IOException {
		if(DEBUG) {
			System.err.println("Sendport " + this + " '" +  name +
							   "' connecting to " + receiver); 
		}

		/* first check the types */
		if(!type.name().equals(receiver.type())) {
			throw new PortMismatchException("Cannot connect ports of different PortTypes");
		}

		TcpReceivePortIdentifier ri = (TcpReceivePortIdentifier) receiver;

		if (!ibis.tcpPortHandler.connect(this, ri)) { 
			throw new ConnectionRefusedException("Could not connect");
		} 
		
		if(DEBUG) {
			System.err.println("Sendport '" + name + "' connecting to " + receiver + " done"); 
		}
	}

	public void connect(ReceivePortIdentifier receiver, int timeout_millis) throws IOException {
		long start = System.currentTimeMillis();
		boolean success = false;
		do {
			if (timeout_millis > 0 &&
				System.currentTimeMillis() > start + timeout_millis) {
				throw new ConnectionTimedOutException("could not connect");
			}
			try {
				connect(receiver);
				success = true;
			} catch (IOException e) {
				int timeLeft = (int)(start + timeout_millis - System.currentTimeMillis());
				try {
					if (timeLeft > 0) Thread.sleep(Math.min(timeLeft, 500));
				} catch (InterruptedException e2) {
					// ignore               
				}
			}
		} while (!success);
	}

	public ibis.ipl.WriteMessage newMessage() throws IOException { 
		synchronized(this) {
			while(aMessageIsAlive) {
				try {
					wait();
				} catch(InterruptedException e) {
					// Ignore.
				}
			}
			
			aMessageIsAlive = true;
		}
		return sender.newMessage();
	}

	synchronized void finishMessage() {
		aMessageIsAlive = false;
		notifyAll();
	}

	public DynamicProperties properties() {
		return null;
	}

	public SendPortIdentifier identifier() {
		return ident;
	}
	
	public void free() throws IOException {
		if(aMessageIsAlive) {
			throw new IOException("Trying to free a sendport port while a message is alive!");
		}

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free start");
		}

		if(sender != null) {
			sender.free();
		}
		ident = null;

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free DONE");
		}
	}

	void release(TcpReceivePortIdentifier ri, OutputStream out) {
		ibis.tcpPortHandler.releaseOutput(ri, out);
	}

	void reset() throws IOException {
	    sender.reset();
	}

	public ReceivePortIdentifier[] connectedTo() {
		System.err.println("connectedTo not implemented!");
		return null;
	}
	
	public ReceivePortIdentifier[] lostConnections() {
		System.err.println(" not implemented!");
		return null;
	}
}
