package ibis.impl.nio;

import ibis.io.Accumulator;
import ibis.io.Dissipator;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

import java.io.IOException;

public final class NioSendPortIdentifier implements SendPortIdentifier, java.io.Serializable { 
    private static final long serialVersionUID = 5L;

    String name;
    String type;
    NioIbisIdentifier ibis;

    NioSendPortIdentifier(String name, String type, NioIbisIdentifier ibis) {
	this.name = name;
	this.type = type;
	this.ibis = ibis;
    }

    NioSendPortIdentifier(Dissipator in) throws IOException {
	int nameLength;
	int typeLength;
	byte[] nameBytes;
	byte[] typeBytes;

	nameLength = in.readInt();
	nameBytes = new byte[nameLength];
	in.readArray(nameBytes, 0, nameLength);
	name = new String(nameBytes, "UTF-8");

	typeLength = in.readInt();
	typeBytes = new byte[typeLength];
	in.readArray(typeBytes, 0, typeLength);
	type = new String(typeBytes, "UTF-8");

	ibis = new NioIbisIdentifier(in);
    }

    public boolean equals(NioSendPortIdentifier other) {
	if (other == null) { 
	    return false;
	} else { 
	    return (type().equals(other.type()) 
		    && ibis.equals(other.ibis) && name().equals(other.name()));
	}
    }

    public int hashCode() {
	return type().hashCode() + name().hashCode() + ibis.hashCode();
    }

    public boolean equals(Object other) { 
	if (other == null) { 
	    return false;
	}
	if (other instanceof NioSendPortIdentifier) {			
	    return equals((NioSendPortIdentifier) other);
	} else { 
	    return false;
	}
    } 

    public final String name() {
	return name;
    }

    public final String type() {
	return type;
    }

    public IbisIdentifier ibis() {
	return ibis;
    }

    public String toString() {
	return name;
    }

    public void writeTo(Accumulator out) throws IOException {
	byte[] nameBytes;
	byte[] typeBytes;

	nameBytes = name.getBytes("UTF-8");
	out.writeInt(nameBytes.length);
	out.writeArray(nameBytes, 0, nameBytes.length);

	typeBytes = type.getBytes("UTF-8");
	out.writeInt(typeBytes.length);
	out.writeArray(typeBytes, 0, typeBytes.length);

	ibis.writeTo(out);
    }
}
