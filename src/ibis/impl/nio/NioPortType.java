package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.Dissipator;
import ibis.io.IbisSerializationInputStream;
import ibis.io.IbisSerializationOutputStream;
import ibis.io.NoSerializationInputStream;
import ibis.io.NoSerializationOutputStream;
import ibis.io.SerializationInputStream;
import ibis.io.SerializationOutputStream;
import ibis.io.SunSerializationInputStream;
import ibis.io.SunSerializationOutputStream;
import ibis.ipl.IbisError;
import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;
import java.util.Properties;

class NioPortType extends PortType implements Config { 

    StaticProperties p;
    String name;
    NioIbis ibis;

    static final byte SERIALIZATION_BYTE = 0;
    static final byte SERIALIZATION_DATA = 1;
    static final byte SERIALIZATION_SUN =  2;
    static final byte SERIALIZATION_IBIS = 3;
    static final byte SERIALIZATION_IPL_IBIS = 4;

    static final String[] SERIALIZATION_NAMES = {"Byte", 
						 "Data", 
						 "Sun", 
						 "Nio Ibis",
						 "Ipl Ibis"};

    byte serializationType;

    static final byte IMPLEMENTATION_BLOCKING = 0;
    static final byte IMPLEMENTATION_NON_BLOCKING = 1;
    static final byte IMPLEMENTATION_THREAD = 2;

    static final String[] IMPLEMENTATION_NAMES = {"Blocking", 
						  "Non Blocking",
						  "Thread"};

    byte sendPortImplementation;
    byte receivePortImplementation;

    final boolean numbered;
    final boolean oneToMany;
    final boolean manyToOne;

    NioPortType(NioIbis ibis, String name, StaticProperties p) throws IbisException { 
	this.ibis = ibis;
	this.name = name;
	this.p = p;

	Properties systemProperties = System.getProperties();

	oneToMany = p.isProp("Communication", "OneToMany");
	manyToOne = p.isProp("Communication", "ManyToOne");

	if (systemProperties.getProperty(NioIbis.s_numbered) != null) {
	    numbered = true;
	} else {
	    numbered = p.isProp("Communication", "Numbered");
	}

	String ser = this.p.find("Serialization");
	if (ser == null) {
	    this.p = new StaticProperties(p);
	    this.p.add("Serialization", "sun");
	}

	if (p.isProp("Serialization", "byte")) {
	    serializationType = SERIALIZATION_BYTE;
	    if(numbered) {
		throw new IbisException("Numbered communication is not"
			+ " supported on byte serialization streams");
	    }
	} else if (p.isProp("Serialization", "data")) {
	    serializationType = SERIALIZATION_DATA;
	} else if (p.isProp("Serialization", "sun") 
		|| p.isProp("Serialization", "object")) {
	    serializationType = SERIALIZATION_SUN;
	} else if (p.isProp("Serialization", "ibis")) {
	    serializationType = SERIALIZATION_IBIS;
	} else if (p.isProp("Serialization", "iplibis")) {
	    serializationType = SERIALIZATION_IPL_IBIS;
	} else {
	    throw new IbisException("Unknown Serialization type");
	}



	String globalSPI = systemProperties.getProperty(NioIbis.s_spi);
	String typeSPI = systemProperties.getProperty(NioIbis.s_spi + "." + name);

	if (typeSPI != null) {
	    if (typeSPI.equalsIgnoreCase("Blocking")) {
		sendPortImplementation = IMPLEMENTATION_BLOCKING;
	    } else if (typeSPI.equalsIgnoreCase("NonBlocking")) {
		sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
	    } else if (typeSPI.equalsIgnoreCase("Thread")) {
		sendPortImplementation = IMPLEMENTATION_THREAD;
	    } else {
		throw new IbisException("unknown value \"" + typeSPI 
			+ "\" for sendport implementation");
	    }
	} else if (globalSPI != null) {
	    if (globalSPI.equalsIgnoreCase("Blocking")) {
		sendPortImplementation = IMPLEMENTATION_BLOCKING;
	    } else if (globalSPI.equalsIgnoreCase("NonBlocking")) {
		sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
	    } else if (globalSPI.equalsIgnoreCase("Thread")) {
		sendPortImplementation = IMPLEMENTATION_THREAD;
	    } else {
		throw new IbisException("unknown value \"" + typeSPI
			+ "\" for sendport implementation");
	    }
	} else if (oneToMany) {
	    sendPortImplementation = IMPLEMENTATION_NON_BLOCKING;
	} else {
	    sendPortImplementation = IMPLEMENTATION_BLOCKING;
	}

	String globalRPI = systemProperties.getProperty(NioIbis.s_rpi);
	String typeRPI = systemProperties.getProperty(NioIbis.s_rpi + "." + name); 

	if (typeRPI != null) {
	    if (typeRPI.equalsIgnoreCase("Blocking")) {
		receivePortImplementation = IMPLEMENTATION_BLOCKING;
	    } else if (typeRPI.equalsIgnoreCase("NonBlocking")) {
		receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
	    } else if (typeRPI.equalsIgnoreCase("Thread")) {
		receivePortImplementation = IMPLEMENTATION_THREAD;
	    } else {
		throw new IbisException("unknown value \"" + typeRPI
			+ "\" for receiveport implementation");
	    }
	} else if (globalRPI != null) {
	    if (globalRPI.equalsIgnoreCase("Blocking")) {
		receivePortImplementation = IMPLEMENTATION_BLOCKING;
	    } else if (globalRPI.equalsIgnoreCase("NonBlocking")) {
		receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
		} else if (globalRPI.equalsIgnoreCase("Thread")) {
		    receivePortImplementation = IMPLEMENTATION_THREAD;
		} else {
		    throw new IbisException("unknown value \"" + globalRPI
			    + "\" for receiveport implementation");
		}
	} else if (manyToOne) {
	    receivePortImplementation = IMPLEMENTATION_NON_BLOCKING;
	} else {
	    receivePortImplementation = IMPLEMENTATION_BLOCKING;
	}

	if (p.find("verbose") != null)  {
		    System.out.println( "serialization = " 
		    + SERIALIZATION_NAMES[serializationType] 
		    + "\nsend port implementation = "
		    + IMPLEMENTATION_NAMES[sendPortImplementation]
		    + "\nreceive port implementation = "
		    + IMPLEMENTATION_NAMES[receivePortImplementation]
		    + "\nnumbered = " + numbered
		    + "\n");
	}
    } 

    public String name() { 
	return name;
    } 

    private boolean equals(NioPortType other) {
	return name.equals(other.name) && ibis.equals(other.ibis);
    } 

    public boolean equals(Object other) {
	if (other == null) return false;
	if (! (other instanceof NioPortType)) return false;
	return equals((NioPortType) other);
    }

    public int hashCode() {
	return name.hashCode() + ibis.hashCode();
    }

    public StaticProperties properties() { 
	return p;
    }

    public SendPort createSendPort(String name,
				   SendPortConnectUpcall cU,
				   boolean connectionAdministration)
	    throws IOException {
	return new NioSendPort(ibis, this, name, connectionAdministration, cU);
    }

    public ReceivePort createReceivePort(String name,
					 Upcall u,
					 ReceivePortConnectUpcall cU,
					 boolean connectionAdministration) 
	throws IOException {

	switch(receivePortImplementation) {
	    case IMPLEMENTATION_BLOCKING:
		return new BlockingChannelNioReceivePort(ibis, this, name, 
			u, connectionAdministration, cU);
	    case IMPLEMENTATION_NON_BLOCKING:
		return new NonBlockingChannelNioReceivePort(ibis, this, 
			name, u, connectionAdministration, cU);
	    case IMPLEMENTATION_THREAD:
		return new ThreadNioReceivePort(ibis, this, name, u,
			connectionAdministration, cU);
	    default:
		throw new IbisError("unknown receiveport implementation"
			+ " type " + receivePortImplementation);
	}
    }

    public String toString() {
	return ("(NioPortType: name = " + name + ")");
    }

    SerializationOutputStream createSerializationOutputStream(Accumulator out) 
	    throws IOException {
	SerializationOutputStream result;

	switch(serializationType) {
	    case SERIALIZATION_BYTE:
		return new NoSerializationOutputStream(out);
	    case SERIALIZATION_DATA:
		return new NioDataSerializationOutputStream(out);
	    case SERIALIZATION_SUN:
		return new SunSerializationOutputStream(out);
	    case SERIALIZATION_IBIS:
		return new NioIbisSerializationOutputStream(out);
	    case SERIALIZATION_IPL_IBIS:
		return new IbisSerializationOutputStream(out);
	    default:
		throw new IbisError("EEK! Unknown serialization type");
	}
    }

    SerializationInputStream createSerializationInputStream(Dissipator in) 
	    throws IOException {
	SerializationOutputStream result;

	switch(serializationType) {
	    case SERIALIZATION_BYTE:
		return new NoSerializationInputStream(in);
	    case SERIALIZATION_DATA:
		return new NioDataSerializationInputStream(in);
	    case SERIALIZATION_SUN:
		return new SunSerializationInputStream(in);
	    case SERIALIZATION_IBIS:
		return new NioIbisSerializationInputStream(in);
	    case SERIALIZATION_IPL_IBIS:
		return new IbisSerializationInputStream(in);
	    default:
		throw new IbisError("EEK! Unknown serialization type");
	}
    }
}
