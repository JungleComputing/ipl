package ibis.ipl.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.io.Replacer;

public class PortType implements ibis.ipl.PortType {

    ibis.ipl.StaticProperties p;
    String name;
    ibis.ipl.impl.messagePassing.Ibis myIbis;

    public static final byte SERIALIZATION_NONE = 0;
    public static final byte SERIALIZATION_SUN = 1;
    public static final byte SERIALIZATION_IBIS = 2;

    public byte serializationType = SERIALIZATION_SUN;

    PortType(ibis.ipl.impl.messagePassing.Ibis myIbis, String name,
		  ibis.ipl.StaticProperties p) throws IbisException {
	this.myIbis = myIbis;
	this.name = name;
	this.p = p;

	String ser = p.find("Serialization");
	if (ser == null) {
	    p.add("Serialization", "sun");
	    serializationType = SERIALIZATION_SUN;
	} else if (ser.equals("none")) {
	    serializationType = SERIALIZATION_NONE;
	} else if (ser.equals("sun")) {
	    serializationType = SERIALIZATION_SUN;
	} else if (ser.equals("ibis")) {
	    // For backwards compatibility ...
	    serializationType = SERIALIZATION_IBIS;
	} else if (ser.equals("ibis")) {
	    serializationType = SERIALIZATION_IBIS;
	} else {
	    throw new IbisException("Unknown Serialization type " + ser);
	}
    }

    public String name() {
	return name;
    }

    public boolean equals(ibis.ipl.PortType other) {
	if (!(other instanceof PortType))
	    return false;

	PortType temp = (PortType)other;

	return name.equals(temp.name);
    }

    public ibis.ipl.StaticProperties properties() {
	return p;
    }

    public ibis.ipl.SendPort createSendPort() throws IbisIOException {
	SendPort s;

	s = ibis.ipl.impl.messagePassing.Ibis.myIbis.createSendPort(this);

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(myIbis.name() + ": Sendport created of of type '" +
			       name + "'" + " cpu " + s.ident.cpu + " port " + s.ident.port);
	}

	return s;
    }

    public ibis.ipl.SendPort createSendPort(Replacer r) throws IbisIOException {
	SendPort s;

	s = ibis.ipl.impl.messagePassing.Ibis.myIbis.createSendPort(this, r);

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(myIbis.name() + ": Sendport created of of type '" +
			       name + "'" + " cpu " + s.ident.cpu + " port " + s.ident.port);
	}

	return s;
    }

    public ibis.ipl.SendPort createSendPort(String portname) throws IbisIOException {
	SendPort s;

	s = ibis.ipl.impl.messagePassing.Ibis.myIbis.createSendPort(this, portname);

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(myIbis.name() + ": Sendport " + name +
			       " created of of type '" + this.name + "'");
	}

	return s;
    }

    public ibis.ipl.SendPort createSendPort(String portname, Replacer r) throws IbisIOException {
	    SendPort s;
	    s = ibis.ipl.impl.messagePassing.Ibis.myIbis.createSendPort(this, portname, r);

	    if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
		    System.out.println(myIbis.name() + ": Sendport " + name + " created of of type '" +
				       name + "'" + " cpu " + s.ident.cpu + " port " + s.ident.port);
	    }
	    
	    return s;
    }

    public ibis.ipl.ReceivePort createReceivePort(String name)
	    throws IbisIOException {
	return createReceivePort(name, null, null);
    }

    public ibis.ipl.ReceivePort createReceivePort(
					String name,
					ibis.ipl.Upcall u)
					    throws IbisIOException {
	return createReceivePort(name, u, null);
    }

    public ibis.ipl.ReceivePort createReceivePort(
					String name,
					ibis.ipl.ConnectUpcall cU)
					    throws IbisIOException {
	return createReceivePort(name, null, cU);
    }

    public ibis.ipl.ReceivePort createReceivePort(
					String name,
					ibis.ipl.Upcall u,
					ibis.ipl.ConnectUpcall cU)
					    throws IbisIOException {

	ReceivePort p = new ReceivePort(this, name, u, cU);

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(myIbis.name() + ": Receiveport created of type '" +
			       this.name + "', name = '" + name + "'" +
			       " cpu " + p.ident.cpu + " port " + p.ident.port);
	}

	if (ibis.ipl.impl.messagePassing.Ibis.DEBUG) {
	    System.out.println(myIbis.name() +
			       ": Receiveport bound in registry, type = '" +
			       this.name + "', name = '" + name + "'");
	}

	return p;
    }

    void freeReceivePort(String name) throws IbisIOException {
	((Registry)myIbis.registry()).unbind(name);
    }

    public String toString() {
	return ("(PortType: name = " + name + ")");
    }

}
