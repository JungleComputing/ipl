package ibis.impl.tcp;

import ibis.io.BufferedArrayOutputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.DataSerializationOutputStream;
import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.io.NoSerializationOutputStream;
import ibis.ipl.ConnectionRefusedException;
import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisError;
import ibis.ipl.IbisIOException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.Replacer;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.SendPortIdentifier;
import ibis.util.DummyOutputStream;
import ibis.util.OutputStreamSplitter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

final class TcpSendPort implements SendPort, Config, TcpProtocol {

	private static class Conn  {
 		OutputStream out;
		TcpReceivePortIdentifier ident;
	}

	private TcpPortType type;
	private TcpSendPortIdentifier ident;
	private String name;
	private boolean aMessageIsAlive = false;
	private TcpIbis ibis;
	private OutputStreamSplitter splitter;
	DummyOutputStream dummy;
	private SerializationOutputStream out;
	private ArrayList receivers = new ArrayList();
	private TcpWriteMessage message;
	private boolean connectionAdministration = false;
	private SendPortConnectUpcall connectUpcall = null;
	private ArrayList lostConnections = new ArrayList();
	private Replacer replacer;
	long count;

	TcpSendPort(TcpIbis ibis, TcpPortType type, String name,
		    boolean connectionAdministration, SendPortConnectUpcall cU)
		throws IOException {

		this.name = name;
		this.type = type;
		this.ibis = ibis;
		this.connectionAdministration = connectionAdministration;
		this.connectUpcall = cU;
		if(cU != null) connectionAdministration = true;

		ident = new TcpSendPortIdentifier(name, type.name(), 
						  (TcpIbisIdentifier) type.ibis.identifier());

                // if we keep administration, close connections when exception occurs.
		splitter = new OutputStreamSplitter(connectionAdministration); 

		switch(type.serializationType) {
		case TcpPortType.SERIALIZATION_SUN:
			dummy = new DummyOutputStream(new BufferedOutputStream(splitter, 60*1024));
			break;
		case TcpPortType.SERIALIZATION_NONE:
			dummy = new DummyOutputStream(new BufferedOutputStream(splitter, 60*1024));
			break;
		case TcpPortType.SERIALIZATION_IBIS:
		case TcpPortType.SERIALIZATION_DATA:
			dummy = new DummyOutputStream(splitter);
			break;
		default:
			System.err.println("EEK, serialization type unknown");
			System.exit(1);
		}
	}

	/**
	 * {@inheritDoc}
	 **/
	public long getCount() {
		return count;
	}

	/**
	 * {@inheritDoc}
	 **/
	public void resetCount() {
		count = 0;
	}


	public synchronized void connect(ReceivePortIdentifier receiver, long timeoutMillis) throws IOException {
		/* first check the types */
		if(!type.name().equals(receiver.type())) {
			throw new PortMismatchException("Cannot connect ports of different PortTypes");
		}

		if(aMessageIsAlive) {
			throw new IOException("A message was alive while adding a new connection");
		}

		if(DEBUG) {
			System.err.println("Sendport " + this + " '" +  name +
							   "' connecting to " + receiver); 
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
			if (replacer != null) out.setReplacer(replacer);
			break;
		case TcpPortType.SERIALIZATION_NONE:
			out = new NoSerializationOutputStream(dummy);
			break;
		case TcpPortType.SERIALIZATION_IBIS:
			out = new IbisSerializationOutputStream(new BufferedArrayOutputStream(dummy));
			if (replacer != null) out.setReplacer(replacer);
			break;
		case TcpPortType.SERIALIZATION_DATA:
			out = new DataSerializationOutputStream(new BufferedArrayOutputStream(dummy));
			break;
		default:
			System.err.println("EEK, serialization type unknown");
			System.exit(1);
		}
		
		message = new TcpWriteMessage(this, out, connectionAdministration);

		if(DEBUG) {
			System.err.println("Sendport '" + name + "' connecting to " + receiver + " done"); 
		}
	}

	public void setReplacer(Replacer r) {
		replacer = r;
		if (out != null) out.setReplacer(r);
	}

	public void connect(ReceivePortIdentifier receiver) throws IOException {
		connect(receiver, 0);
	}

	public void disconnect(ReceivePortIdentifier receiver) throws IOException {
		/* Niels: TODO! */
	}

	public ibis.ipl.WriteMessage newMessage() throws IOException { 
		TcpWriteMessage res = null;

		synchronized(this) {
			if(receivers.size() == 0) {
				throw new IbisIOException("port is not connected");
			}

			while(aMessageIsAlive) {
				try {
					wait();
				} catch(InterruptedException e) {
					// Ignore.
				}
			}
			
			aMessageIsAlive = true;

			res = message;
		}

		out.writeByte(NEW_MESSAGE);
		return res;
	}

	synchronized void finishMessage() {
		aMessageIsAlive = false;
		notifyAll();
	}

	public DynamicProperties properties() {
		return DynamicProperties.NoDynamicProperties;
	}

	public String name() {
		return name;
	}

	public SendPortIdentifier identifier() {
		return ident;
	}
	
	public void close() throws IOException {
		if(aMessageIsAlive) {
			throw new IOException("Trying to free a sendport port while a message is alive!");
		}

		if(ident == null) {
			throw new IbisError("Port already freed");
		}

		if(DEBUG) {
			System.err.println(type.ibis.name() + ": SendPort.free start");
		}

		try {
			if(out != null) {
				out.writeByte(CLOSE_CONNECTION);
				out.reset();
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			// System.err.println("Error in TcpSendPort.free: " + e);
			// e.printStackTrace();
		}

		for (int i=0; i<receivers.size(); i++) { 
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

	public synchronized ReceivePortIdentifier[] connectedTo() {
		Conn[] connections = (Conn[]) receivers.toArray();
		ReceivePortIdentifier[] res = new ReceivePortIdentifier[connections.length];
		for(int i=0; i<res.length; i++) {
			res[i] = connections[i].ident;
		}

		return res;
	}
	
	// called by the writeMessage
	// the stream has already been removed from the splitter.
	// we must remove it from our receivers table and inform the user.
	void lostConnection(OutputStream s, Exception cause, boolean report) {
		TcpReceivePortIdentifier rec = null;

		if(DEBUG) {
			System.out.println("sendport " + name + " lost connection!");
		}

		synchronized (this) {
			for (int i=0;i<receivers.size();i++) { 
				Conn c = (Conn) receivers.get(i);
				if(c.out == s) {
					receivers.remove(i);
					rec = c.ident;
					break;
				}
			}

			if(rec == null) {
				throw new IbisError("could not find connection in lostConnection");
			}
			if(report && connectUpcall == null) {
				lostConnections.add(rec);
				return;
			}
		}

		if(report) {
			// don't hold lock during upcall
			connectUpcall.lostConnection(this, rec, cause);
		}
	}

	public synchronized ReceivePortIdentifier[] lostConnections() {
		return (ReceivePortIdentifier[]) lostConnections.toArray();
	}

	// called from writeMessage
	void reset() throws IOException {
		out.writeByte(NEW_MESSAGE);
	}
}
