package ibis.impl.nio;

import ibis.ipl.IbisError;
import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.Replacer;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.util.ArrayList;

class NioPortType extends PortType implements Config { 

    StaticProperties p;
    String name;
    NioIbis ibis;

    static final byte SERIALIZATION_SUN = 0;
    static final byte SERIALIZATION_IBIS = 1;
    static final byte SERIALIZATION_NONE = 2;

    byte serializationType;

    NioPortType(NioIbis ibis, String name, StaticProperties p) throws IbisException { 
	this.ibis = ibis;
	this.name = name;
	this.p = p;

	//copy staticproperties
	if(p == null) {
	    p = new StaticProperties();
	}

	this.p = p.combineWithUserProps();

	String ser = this.p.find("serialization");
	if(ser == null) {
	    this.p.add("serialization", "sun");
	    if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		System.err.println("Sun serialization used");
	    }
	    serializationType = SERIALIZATION_SUN;
	} else {
	    if (ser.equals("byte")) {
		if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		    System.err.println("No serialization used");
		}
		serializationType = SERIALIZATION_NONE;
	    } else if (ser.equals("sun") || ser.equals("object")) {
		if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		    System.err.println("Sun serialization used");
		}
		serializationType = SERIALIZATION_SUN;
	    } else if (ser.equals("ibis") || ser.equals("data")) {
		if(DEBUG_LEVEL >= LOW_DEBUG_LEVEL) {
		    System.err.println("Ibis serialization used");
		}
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

    //FIXME: if name == null, crash! and what if 2x anonymous?
    private boolean equals(NioPortType other) {
	return name().equals(other.name()) && ibis.equals(other.ibis);
    } 

    public boolean equals(Object other) {
	if (other == null) return false;
	if (! (other instanceof NioPortType)) return false;
	return equals((NioPortType) other);
    }

    public int hashCode() {
	return name().hashCode() + ibis.hashCode();
    }

    public StaticProperties properties() { 
	return p;
    }

    public SendPort createSendPort(String name, Replacer r, 
	    SendPortConnectUpcall cU,
	    boolean connectionAdministration) throws IOException {
	return new NioSendPort(ibis, this, name, r, connectionAdministration, cU);
    }

    public ReceivePort createReceivePort(String name, Upcall u, 
	    ReceivePortConnectUpcall cU,
	    boolean connectionAdministration)  
							throws IOException { 
	return new NioReceivePort(ibis, this, name, u, 
						connectionAdministration, cU);
    }

    public String toString() {
	return ("(NioPortType: name = " + name() + ")");
    }
}
