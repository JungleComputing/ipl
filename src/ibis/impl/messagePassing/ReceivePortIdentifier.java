package ibis.ipl.impl.messagePassing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;

import ibis.ipl.IbisIOException;

final class ReceivePortIdentifier
	implements ibis.ipl.ReceivePortIdentifier,
		   java.io.Serializable {

    String name;
    String type;
    int cpu;
    int port;
    ibis.ipl.IbisIdentifier ibisIdentifier;
    transient byte[] serialForm;


    ReceivePortIdentifier(String name, String type)
	    throws IbisIOException {

	synchronized (Ibis.myIbis) {
	    port = Ibis.myIbis.receivePort++;
	}
	cpu = Ibis.myIbis.myCpu;
	this.name = name;
	this.type = type;
	ibisIdentifier = Ibis.myIbis.identifier();
	makeSerialForm();
    }


    private void makeSerialForm() throws IbisIOException {
	serialForm = SerializeBuffer.writeObject(this);
    }


    byte[] getSerialForm() throws IbisIOException {
	if (serialForm == null) {
	    makeSerialForm();
	}
	return serialForm;
    }


    public boolean equals(ibis.ipl.ReceivePortIdentifier other) {
	if (other == this) return true;

	if (!(other instanceof ReceivePortIdentifier)) {
	    return false;
	}

	if(other instanceof ReceivePortIdentifier) {
		ReceivePortIdentifier temp = (ReceivePortIdentifier)other;
		return (cpu == temp.cpu && port == temp.port);
	}

	return false;
    }


    //gosia
    public int hashCode() {
	return name.hashCode() + type.hashCode() + cpu + port;
    }


    public String name() {
	return name;
    }


    public String type() {
	return type;
    }


    public ibis.ipl.IbisIdentifier ibis() {
	return ibisIdentifier;
    }


    public String toString() {
	return ("(RecPortIdent: name \"" + name + "\" type \"" + type +
		"\" cpu " + cpu + " port " + port + " ibis \"" + ibisIdentifier.name() + "\")");
    }

}
