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

import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.BufferedArrayOutputStream;
import ibis.ipl.IbisIOException;
import ibis.ipl.impl.generic.*;

final class TcpSendPort implements SendPort, Config, TcpProtocol {

	private class Conn  {
 		OutputStream out;
		TcpReceivePortIdentifier ident;
	}

	TcpPortType type;
	TcpSendPortIdentifier ident;
	String name;
	private boolean aMessageIsAlive = false;
	Replacer replacer = null;
	TcpIbis ibis;

	private OutputStreamSplitter splitter;
	DummyOutputStream dummy;
	private SerializationOutputStream out;
	private Vector receivers;
	private SerializationStreamWriteMessage message;

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

		receivers = new Vector();
		splitter = new OutputStreamSplitter();

		switch(type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
			dummy = new DummyOutputStream(new BufferedOutputStream(splitter, 60*1024));
			break;
		case TcpPortType.SERIALIZATION_IBIS:
			dummy = new DummyOutputStream(splitter);
			break;
		default:
			System.err.println("EEK, serialization type unknown");
			System.exit(1);
		}

	}

	public void connect(ReceivePortIdentifier receiver, long timeoutMillis) throws IOException {
		if(DEBUG) {
			System.err.println("Sendport " + this + " '" +  name +
							   "' connecting to " + receiver); 
		}

		/* first check the types */
		if(!type.name().equals(receiver.type())) {
			throw new PortMismatchException("Cannot connect ports of different PortTypes");
		}

		if(aMessageIsAlive) {
			throw new IOException("A message was alive while adding a new connection");
		}

		TcpReceivePortIdentifier ri = (TcpReceivePortIdentifier) receiver;

		OutputStream res = ibis.tcpPortHandler.connect(this, ri, timeoutMillis);
		if(res == null) {
			throw new ConnectionRefusedException("Could not connect");
		} 

		// we have a new receiver, now add it to our tables.
		Conn c = new Conn();
		c.ident = ri;
		c.out = res;

		if(DEBUG) {
			System.err.println(name + " adding Connection to " + ri);
		}

		if (out != null) { 
			out.writeByte(NEW_RECEIVER);
			
			if(DEBUG) {
				System.err.println(name + " Sending NEW_RECEIVER " + ri);
				out.writeObject(ri);
			}
			
			out.flush();
			out.close();
		}
		
		receivers.add(c);
		splitter.add(c.out);
		
		switch(type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
			out = new SunSerializationOutputStream(dummy);
			break;
		case TcpPortType.SERIALIZATION_IBIS:
			out = new IbisSerializationOutputStream(new BufferedArrayOutputStream(dummy));
			break;
		default:
			System.err.println("EEK, serialization type unknown");
			System.exit(1);
		}
		
		if (replacer != null) { 
			out.setReplacer(replacer);
		} 

		message = new SerializationStreamWriteMessage(this, out);

		if(DEBUG) {
			System.err.println("Sendport '" + name + "' connecting to " + receiver + " done"); 
		}
	}

	public void connect(ReceivePortIdentifier receiver) throws IOException {
		connect(receiver, 0);
	}

	public ibis.ipl.WriteMessage newMessage() throws IOException { 
		if(receivers.size() == 0) {
			throw new IbisIOException("port is not connected");
		}

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

		dummy.resetCount();
		out.writeByte(NEW_MESSAGE);
		return message;
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

		try {
			out.writeByte(CLOSE_CONNECTION);
			out.reset();
			out.flush();
			out.close();
		} catch (IOException e) {
			// System.err.println("Error in TcpSendPort.free: " + e);
			// e.printStackTrace();
		}

		for (int i=0;i<receivers.size();i++) { 
			Conn c = (Conn) receivers.get(i);
			ibis.tcpPortHandler.releaseOutput(c.ident, c.out);
		} 

		receivers = null;
		splitter = null;
		out = null;
		ident = null;

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free DONE");
		}
	}

	long getCount() {
		return dummy.getCount();
	}

	void resetCount() {
		dummy.resetCount();
	}

	public ReceivePortIdentifier[] connectedTo() {
		System.err.println("connectedTo not implemented!");
		return null;
	}
	
	public ReceivePortIdentifier[] lostConnections() {
		System.err.println(" not implemented!");
		return null;
	}

	void reset() throws IOException {
		out.writeByte(NEW_MESSAGE);
		dummy.resetCount();
	}
}
