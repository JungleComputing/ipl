package ibis.ipl.impl.tcp;

import java.io.IOException;

import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.Upcall;
import ibis.ipl.IbisException;
import ibis.ipl.IbisError;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortConnectUpcall;
import ibis.io.Replacer;
import java.util.ArrayList;
import ibis.ipl.ReadMessage;

class TcpPortType implements PortType, Config { 

	StaticProperties p;
	String name;
	TcpIbis ibis;
	
	static final byte SERIALIZATION_SUN = 0;
	static final byte SERIALIZATION_IBIS = 1;

	byte serializationType = SERIALIZATION_SUN;

	private ArrayList receivePorts = new ArrayList();

	TcpPortType(TcpIbis ibis, String name, StaticProperties p) throws IbisException { 
		this.ibis = ibis;
		this.name = name;
		this.p = p;

		String ser = p.find("Serialization");
		if(ser == null) {
			p.add("Serialization", "sun");
			serializationType = SERIALIZATION_SUN;
		} else {
			if (ser.equals("sun")) {
				serializationType = SERIALIZATION_SUN;
//				System.err.println("serializationType = SERIALIZATION_SUN");
			} else if (ser.equals("ibis")) {

//				System.err.println("serializationType = SERIALIZATION_IBIS");
				serializationType = SERIALIZATION_IBIS;
			} else if (ser.equals("manta")) {
				// backwards compatibility ...

//				System.err.println("serializationType = SERIALIZATION_IBIS");
				serializationType = SERIALIZATION_IBIS;
			} else {
				throw new IbisException("Unknown Serialization type " + ser);
			}
		}
	} 

	public String name() { 
		if (name != null) {
			return name;
		}
		return "__anonymous__";
	} 

	private boolean equals(TcpPortType other) {
		return name().equals(other.name()) && ibis.equals(other.ibis);
	} 

	public boolean equals(Object other) {
		if (other == null) return false;
		if (! (other instanceof TcpPortType)) return false;
		return equals((TcpPortType) other);
	}

	public int hashCode() {
	    return name().hashCode() + ibis.hashCode();
	}

	public StaticProperties properties() { 
		return p;
	}

	public SendPort createSendPort() throws IOException {
		SendPort s;

		s = new TcpSendPort(ibis, this);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(Replacer r) throws IOException {
		SendPort s;

		s = new TcpSendPort(ibis, this, r);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String portname, Replacer r) throws IOException {
		SendPort s;

		s = new TcpSendPort(ibis, this, r, portname);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String portname) throws IOException {
		SendPort s;

		s = new TcpSendPort(ibis, this, portname);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport '" + portname + "' created of of type '" + this.name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String name, SendPortConnectUpcall cU) throws IOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}


	public SendPort createSendPort(ibis.io.Replacer r, SendPortConnectUpcall cU) throws IOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}


	public SendPort createSendPort(String name, ibis.io.Replacer r, SendPortConnectUpcall cU) throws IOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}

	public ReceivePort createReceivePort(String name) throws IOException {
	    return createReceivePort(name, null, null);
	}

	public ReceivePort createReceivePort(String name, Upcall u)  throws IOException { 
	    return createReceivePort(name, u, null);

	}

	public ReceivePort createReceivePort(String name, ReceivePortConnectUpcall cU) throws IOException {
	    return createReceivePort(name, null, cU);
	}


	public ReceivePort createReceivePort(String name, Upcall u, ReceivePortConnectUpcall cU)  throws IOException { 
		TcpReceivePort p = new TcpReceivePort(ibis, this, name, u, cU);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Receiveport created name = '" + name() + "'");
		}

		ibis.tcpReceivePortNameServerClient.bind(name, p);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Receiveport bound in registry, name = '" + name() + "'");
		}

		synchronized(this) {
			receivePorts.add(p);
		}

		return p;
	}

	void freeReceivePort(String name) throws IOException {
		synchronized(this) {
			for(int i=0; i<receivePorts.size(); i++) {
				ReceivePort rp = (ReceivePort) receivePorts.get(i);
				if(rp.equals(name)) {
					receivePorts.remove(i);
					ibis.tcpReceivePortNameServerClient.unbind(name);
					return;
				}
			}

			throw new IbisError("Internal error in TcpPortType: could not fine port with name: " + name);
		}
	}

	public String toString() {
		return ("(TcpPortType: name = " + name() + ")");
	}

	/* Only used interally to implement ibis.poll.
	   Poll all receiveports. */
	ReadMessage poll() throws IOException {
		Object[] a;

		synchronized(this) {
			a = receivePorts.toArray();
		}
		
		for(int i=0; i<a.length; i++) {
			TcpReceivePort r = (TcpReceivePort) a[i];
			ReadMessage m = r.poll(); // all exceptions are just forwared
			if(m != null) return m;
		}

		return null;
	}
}
