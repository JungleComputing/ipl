package ibis.impl.messagePassing;

import ibis.ipl.IbisException;
import ibis.ipl.Replacer;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;

import java.io.IOException;

public class PortType extends ibis.ipl.PortType {

    private StaticProperties p;
    private String name;

    public static final byte SERIALIZATION_NONE = 0;
    public static final byte SERIALIZATION_SUN = 1;
    public static final byte SERIALIZATION_IBIS = 2;

    public byte serializationType = SERIALIZATION_SUN;

    PortType(String name,
	     StaticProperties p) throws IbisException {
	this.name = name;
	this.p = p;

	String ser = p.find("Serialization");
	if (ser == null) {
	    this.p = new StaticProperties(p);
	    this.p.add("Serialization", "bytes");
	    serializationType = SERIALIZATION_NONE;
	} else if (ser.equals("object")) {
	    serializationType = SERIALIZATION_SUN;
	} else if (ser.equals("byte")) {
	    serializationType = SERIALIZATION_NONE;
	} else if (ser.equals("sun")) {
	    serializationType = SERIALIZATION_SUN;
	} else if (ser.equals("manta")) {
	    // For backwards compatibility ...
	    serializationType = SERIALIZATION_IBIS;
	} else if (ser.equals("ibis") || ser.equals("data")) {
	    serializationType = SERIALIZATION_IBIS;
	} else {
	    throw new IbisException("Unknown Serialization type " + ser);
	}
    }

    public String name() {
	if (name != null) {
	    return name;
	}
	return "__anonymous__";
    }

    public boolean equals(Object other) {
	if (other == null) {
	    return false;
	}
	if (!(other instanceof PortType))
	    return false;

	PortType temp = (PortType)other;

	return name().equals(temp.name());
    }

    public int hashCode() {
	return name.hashCode();
    }

    public StaticProperties properties() {
	return p;
    }

    public ibis.ipl.SendPort createSendPort(String portname, Replacer r, SendPortConnectUpcall cU, 
					    boolean connectionAdministration) throws IOException {

	if (cU != null) {
	    System.err.println(this + ": createSendPort with ConnectUpcall. UNIMPLEMENTED");
	    connectionAdministration = true;
	}

	if (connectionAdministration) {
	    System.err.println(this + ": createSendPort with ConnectionAdministration UNIMPLEMENTED");
	}

	SendPort s;

	switch (serializationType) {
        case PortType.SERIALIZATION_NONE:
// System.err.println("MSG: NO SER, name = " + portname);
	    s = new SendPort(this, portname, new OutputConnection());
	    break;

	case PortType.SERIALIZATION_SUN:
// System.err.println("MSG: SUN SER, name = " + portname);
	    s = new SerializeSendPort(this, portname, new OutputConnection(), r);
	    break;

	case PortType.SERIALIZATION_IBIS:
// System.err.println("MSG: IBIS SER, name = " + portname);
	    s = new IbisSendPort(this, portname, new OutputConnection(), r);
	    break;

	default:
	    throw new Error("No such serialization type " + serializationType);
	}

	if (Ibis.DEBUG) {
	    System.out.println(Ibis.myIbis.name() + ": Sendport " + portname +
				" created of of type '" + this + "'" +
				" cpu " + s.ident.cpu +
				" port " + s.ident.port);
	}
	
	return s;
    }


    public ibis.ipl.ReceivePort createReceivePort(
					String name,
					ibis.ipl.Upcall u,
					ibis.ipl.ReceivePortConnectUpcall cU,
					boolean connectionAdministration)
	    throws IOException {

	ReceivePort p = new ReceivePort(this, name, u, cU, connectionAdministration);

	if (Ibis.DEBUG) {
	    System.out.println(Ibis.myIbis.name() + ": Receiveport created of type '" +
			       this.name + "', name = '" + name + "'" +
			       " id " + p.identifier());
	}

	if (Ibis.DEBUG) {
	    System.out.println(Ibis.myIbis.name() +
			       ": Receiveport bound in registry, type = '" +
			       this.name + "', name = '" + name + "'");
	}

	return p;
    }

    void freeReceivePort(String name) throws IOException {
	((Registry)Ibis.myIbis.registry()).unbind(name);
    }

    public String toString() {
	return ("(PortType: name = " + name + ")");
    }

}
