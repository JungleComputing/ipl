package ibis.impl.nio;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

public final class NioSendPortIdentifier implements SendPortIdentifier, java.io.Serializable { 
    private static final long serialVersionUID = 5L;

    String type;
    String name;
    NioIbisIdentifier ibis;

    NioSendPortIdentifier(String name, String type, NioIbisIdentifier ibis) {
	this.name = name;
	this.type = type;
	this.ibis = ibis;
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
	if (type != null) {
	    return type;
	}
	return "__notype__";
    }

    public IbisIdentifier ibis() {
	return ibis;
    }

    public String toString() {
	return ("(NioSendPortIdent: name = " + name() + ", type = " + type() + ", ibis = " + ibis + ")");
    }
}
