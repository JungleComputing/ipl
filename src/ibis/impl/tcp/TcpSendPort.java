package ibis.ipl.impl.tcp;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.DynamicProperties;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReceivePortIdentifier;
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

	TcpSendPort(TcpPortType type) throws IbisIOException {
		this(type, null, null);
	}

	TcpSendPort(TcpPortType type, Replacer r) throws IbisIOException {
		this(type, r, null);
	}

	TcpSendPort(TcpPortType type, String name) throws IbisIOException {
		this(type, null, name);
	}

	TcpSendPort(TcpPortType type, Replacer r, String name) throws IbisIOException {
		try { 
			this.name = name;
			this.type = type;
			this.replacer = r;
			ident = new TcpSendPortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier());
			sender = new SerializationStreamSender(this);
		} catch (Exception e) { 
			e.printStackTrace();
			throw new IbisIOException("Could not create SendPort", e);
		}
	}

	// @@@ add sanity check: no message should be alive.
	void connect(TcpReceivePortIdentifier ri, OutputStream sout, int id) throws IOException { 
		sender.connect(ri, sout, id);
	}

	public void connect(ReceivePortIdentifier receiver) throws IbisIOException {
		if(DEBUG) {
			System.err.println("Sendport '" + name + "' connecting to " + receiver); 
		}

		/* first check the types */
		if(!type.name().equals(receiver.type())) {
			throw new IbisIOException("Cannot connect ports of different PortTypes");
		}

		TcpReceivePortIdentifier ri = (TcpReceivePortIdentifier) receiver;

		if (!TcpIbis.tcpPortHandler.connect(this, ri)) { 
			throw new IbisIOException("Could not connect");
		} 
		
		if(DEBUG) {
			System.err.println("Sendport '" + name + "' connecting to " + receiver + " done"); 
		}
	}

	public void connect(ReceivePortIdentifier receiver, int timeout_millis) throws IbisIOException {
	    System.err.println("Implement TcpSendPort.connect(receiver, timeout)");
	}

	public ibis.ipl.WriteMessage newMessage() throws IbisIOException { 
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
	
	public void free() {
		if(ASSERTS) {
			if(aMessageIsAlive) {
				System.err.println("Trying to free a sendport port while a message is alive!");
			}
		}

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free start");
		}

		sender.free();
		ident = null;

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free DONE");
		}
	}

	void release(TcpReceivePortIdentifier ri, int id) { 
		TcpIbis.tcpPortHandler.releaseOutput(ri, id);
	} 

	void reset() throws IbisIOException {
	    sender.reset();
	}
}
