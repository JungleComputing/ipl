package ibis.ipl.impl.tcp;

import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.Upcall;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortConnectUpcall;
import ibis.io.Replacer;

class TcpPortType implements PortType, Config { 

	StaticProperties p;
	String name;
	TcpIbis ibis;
	
	static final byte SERIALIZATION_SUN = 0;
	static final byte SERIALIZATION_IBIS = 1;

	byte serializationType = SERIALIZATION_SUN;

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
		return name;
	} 

	public boolean equals(PortType other) {
		if(!(other instanceof TcpPortType)) return false;

		TcpPortType temp = (TcpPortType) other;

		return name.equals(temp.name);
	} 

	public StaticProperties properties() { 
		return p;
	}

	public SendPort createSendPort() throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(Replacer r) throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this, r);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String portname, Replacer r) throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this, r, portname);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String portname) throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this, portname);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Sendport '" + portname + "' created of of type '" + this.name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String name, SendPortConnectUpcall cU) throws IbisIOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}


	public SendPort createSendPort(ibis.io.Replacer r, SendPortConnectUpcall cU) throws IbisIOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}


	public SendPort createSendPort(String name, ibis.io.Replacer r, SendPortConnectUpcall cU) throws IbisIOException {
		System.err.println("Must implement createSendPort(..., ReceivePortConnectUpcall)");
		return null;
	}

	public ReceivePort createReceivePort(String name) throws IbisIOException {
	    return createReceivePort(name, null, null);
	}

	public ReceivePort createReceivePort(String name, Upcall u)  throws IbisIOException { 
	    return createReceivePort(name, u, null);

	}

	public ReceivePort createReceivePort(String name, ReceivePortConnectUpcall cU) throws IbisIOException {
	    return createReceivePort(name, null, cU);
	}


	public ReceivePort createReceivePort(String name, Upcall u, ReceivePortConnectUpcall cU)  throws IbisIOException { 
		TcpReceivePort p = new TcpReceivePort(this, name, u, cU);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Receiveport created of type '" + this.name + "', name = '" + name + "'");
		}

		ibis.tcpReceivePortNameServerClient.bind(name, p);

		if(DEBUG) {
			System.out.println(ibis.name() + ": Receiveport bound in registry, type = '" + this.name + "', name = '" + name + "'");
		}

		return p;
	}

	void freeReceivePort(String name) throws IbisIOException {
		ibis.tcpReceivePortNameServerClient.unbind(name);
	}

	public String toString() {
		return ("(TcpPortType: name = " + name + ")");
	}
}
