package ibis.ipl.impl.tcp;

import ibis.ipl.PortType;
import ibis.ipl.StaticProperties;
import ibis.ipl.SendPort;
import ibis.ipl.ReceivePort;
import ibis.ipl.Upcall;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ConnectUpcall;
import ibis.ipl.Replacer;

class TcpPortType implements PortType { 

	StaticProperties p;
	String name;
	TcpIbis ibis;
	
	static final byte SERIALIZATION_SUN = 0;
	static final byte SERIALIZATION_MANTA = 1;

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
			} else if (ser.equals("manta")) {

//				System.err.println("serializationType = SERIALIZATION_MANTA");
				serializationType = SERIALIZATION_MANTA;
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

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(Replacer r) throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this, r);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Sendport created of of type '" + name + "'");
		}

		return s;
	}

	public SendPort createSendPort(String portname) throws IbisIOException {
		SendPort s;

		s = new TcpSendPort(this, portname);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Sendport " + name + " created of of type '" + this.name + "'");
		}

		return s;
	}

	public ReceivePort createReceivePort(String name) throws IbisIOException {

		TcpReceivePort p = new TcpReceivePort(this, name);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Receiveport created of type '" + this.name + "', name = '" + name + "'");
		}

		ibis.tcpReceivePortNameServerClient.bind(name, p);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Receiveport bound in registry, type = '" + this.name + "', name = '" + name + "'");
		}

		return p;
	}

	public ReceivePort createReceivePort(String name, Upcall u)  throws IbisIOException { 

		TcpReceivePort p = new TcpReceivePort(this, name, u);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Receiveport created of type '" + this.name + "', name = '" + name + "'");
		}

		ibis.tcpReceivePortNameServerClient.bind(name, p);

		if(TcpIbis.DEBUG) {
			System.out.println(ibis.name() + ": Receiveport bound in registry, type = '" + this.name + "', name = '" + name + "'");
		}

		return p;
	}

	public ReceivePort createReceivePort(String name, ConnectUpcall cU) throws IbisIOException {

		System.err.println("Must implement createReceivePort(..., ConnectUpcall) RFHH");

		return null;
	}

	public ReceivePort createReceivePort(String name, Upcall u, ConnectUpcall cU)  throws IbisIOException { 

		System.err.println("Must implement createReceivePort(..., ConnectUpcall) RFHH");

		return null;
	}

	void freeReceivePort(String name) throws IbisIOException {
		ibis.tcpReceivePortNameServerClient.unbind(name);
	}

	public String toString() {
		return ("(TcpPortType: name = " + name + ")");
	}
}
