package ibis.ipl.impl.tcp;

import ibis.ipl.SendPort;
import ibis.ipl.WriteMessage;
import ibis.ipl.DynamicProperties;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.IbisException;
import ibis.ipl.ReceivePortIdentifier;

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

final class TcpSendPort implements SendPort {

	TcpPortType type;
	TcpSendPortIdentifier ident;
	String name;
	boolean aMessageIsAlive = false;
	Sender sender;

	TcpSendPort(TcpPortType type) throws IbisException {
		this(type, null);
	}

	TcpSendPort(TcpPortType type, String name) throws IbisException {

		try { 
			
			this.name = name;
			this.type = type;
			ident = new TcpSendPortIdentifier(name, type.name(), (TcpIbisIdentifier) type.ibis.identifier());
			
			switch(type.serializationType) {
			case TcpPortType.SERIALIZATION_SUN:
				sender = new ObjectStreamSender(this);
				break;
			case TcpPortType.SERIALIZATION_MANTA:
				sender = new MantaTypedBufferStreamSender(this);
				break;
			default:
				System.out.println("EEK");
				System.exit(1);
			}

		} catch (IOException e) { 
			throw new IbisException("Could not create SendPort");
		} 
	}

	void connect(TcpReceivePortIdentifier ri, OutputStream sout, int id) throws IOException { 
		sender.connect(ri, sout, id);
	}

	public void connect(ReceivePortIdentifier receiver) throws IbisException {

		if(TcpIbis.DEBUG) {
			System.out.println(name + " connecting to " + receiver); 
		}

		/* first check the types */
		if(!type.name().equals(receiver.type())) {
			throw new IbisException("Cannot connect ports of different PortTypes");
		}

		TcpReceivePortIdentifier ri = (TcpReceivePortIdentifier) receiver;

		if (!TcpIbis.tcpPortHandler.connect(this, ri)) { 
			throw new IbisException("Could not connect");
		} 
		
		if(TcpIbis.DEBUG) {
			System.out.println(name + " connecting to " + receiver + " done"); 
		}
	}

	public void connect(ReceivePortIdentifier receiver, int timeout_millis) throws IbisException {
	    System.err.println("Implement TcpSendPort.connect(receiver, timeout)");
	}

	public WriteMessage newMessage() throws IbisException { 

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

	public DynamicProperties properties() {
		return null;
	}

	public SendPortIdentifier identifier() {
		return ident;
	}
	
	public void free() {

		if(TcpIbis.DEBUG) {
			System.out.println(type.ibis.name() + ": SendPort.free start");
		}

		sender.free();
		ident = null;

		if(TcpIbis.DEBUG) {
			System.out.println(type.ibis.name() + ": SendPort.free DONE");
		}
	}

	void release(TcpReceivePortIdentifier ri, int id) { 
		TcpIbis.tcpPortHandler.releaseOutput(ri, id);
	} 
}

