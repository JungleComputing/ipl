package ibis.ipl.impl.messagePassing;

import java.io.IOException;

import ibis.ipl.IbisException;

final class SendPortIdentifier implements ibis.ipl.SendPortIdentifier,
    java.io.Serializable {

    String name;
    String type;
    int cpu;
    int port;
    IbisIdentifier ibisId;
    transient byte[] serialForm;


    SendPortIdentifier(String name, String type) {
	synchronized (Ibis.myIbis) {
	    port = Ibis.myIbis.sendPort++;
	}
	this.name = name;
	this.type = type;
	this.ibisId = (IbisIdentifier)Ibis.myIbis.identifier();
	cpu = Ibis.myIbis.myCpu;
	makeSerialForm();
    }


    private void makeSerialForm() {
	try {
	    serialForm = SerializeBuffer.writeObject(this);
	} catch (IOException e) {
	    throw new Error("Cannot serialize myself", e);
	}
    }


    byte[] getSerialForm() {
	if (serialForm == null) {
	    makeSerialForm();
	}
	return serialForm;
    }


    public boolean equals(Object other) {
	if (other == this) return true;
	if (other == null) return false;
	    
	if (other instanceof SendPortIdentifier) {
	    SendPortIdentifier o = (SendPortIdentifier)other;
	    return cpu == o.cpu && port == o.port && ibisId.equals(o.ibisId) &&
		   name().equals(o.name()) && type().equals(o.type());
	}
	    
	return false;
    }

    public int hashCode() {
	return name().hashCode() + type().hashCode() + ibisId.hashCode() + cpu + port;
    }

    public String name() {
	if (name != null) {
	    return name;
	}

	return "__anonymous__";
    }

    public String type() {
	if (type != null) {
	    return type;
	}
	return "__notype__";
    }

    public ibis.ipl.IbisIdentifier ibis() {
	return ibisId;
    }

    public String toString() {
	return ("(SendPortIdent: name \"" + name() + "\" type \"" + type() +
		"\" cpu " + cpu + " port " + port + ")");
    }
}
