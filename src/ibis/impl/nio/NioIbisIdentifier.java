package ibis.impl.nio;

import ibis.ipl.IbisError;
import ibis.ipl.IbisIdentifier;
import ibis.util.IbisIdentifierTable;

import java.io.IOException;

public final class NioIbisIdentifier extends IbisIdentifier 
					    implements java.io.Serializable {

    //FIXME : what's this for?
    private static final long serialVersionUID = 3L;

    public NioIbisIdentifier(String name) {
	super(name);
    }

    public boolean equals(Object o) {
	if(o == null) {
	    return false;
	}
	if(o == this) { 
	    return true;
	}
	if (o instanceof NioIbisIdentifier) {
	    NioIbisIdentifier other = (NioIbisIdentifier) o;
	    return equals(other);
	}
	return false;
    }

    public boolean equals(NioIbisIdentifier other) {
	if(other == null) {
	    return false;
	}
	if(other == this) {
	    return true;
	}
	return name.equals(other.name);
    }

    public String toString() {
	return ("(NioId: " + name + ")");
    }

    public int hashCode() {
	return name.hashCode();
    }
}

