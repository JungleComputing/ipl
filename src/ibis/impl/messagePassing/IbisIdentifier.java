package ibis.impl.messagePassing;

import ibis.impl.util.IbisIdentifierTable;
import ibis.io.Conversion;

import java.io.IOException;
import java.io.StreamCorruptedException;

/**
 * messagePassing IbisIdentifier.
 * Assumes closed world, so CPUs can be simply ranked.
 */
// Make this final, make inlining possible
final class IbisIdentifier
	extends ibis.ipl.IbisIdentifier
	implements java.io.Serializable {

    private int cpu;
    private transient byte[] serialForm;
    private static IbisIdentifierTable cache = new IbisIdentifierTable();

    IbisIdentifier(String name, int cpu) throws IOException {
	super(name);
	this.cpu  = cpu;
	makeSerialForm();
    }


    static IbisIdentifier createIbisIdentifier(byte[] serialForm)
	    throws IOException {
	IbisIdentifier id;
	try {
	    id = (IbisIdentifier) Conversion.byte2object(serialForm);
	} catch (ClassNotFoundException e) {
	    throw new StreamCorruptedException("serialForm corrupted " + e);
	}

	id.serialForm = serialForm;

	return id;
    }


    // no need to serialize super class fields, this is done automatically
    // We handle the address field special.
    // Do not do a writeObject on it (or a defaultWriteObject of the current object),
    // because InetAddress might not be rewritten as it is in the classlibs --Rob
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	int handle = cache.getHandle(out, this);
	out.writeInt(handle);
	// Rob, somehow you should tell the partner the CPU number
	// the first time. It is not part of the superclass so you
	// must do it yourself.
	if (handle < 0) {
	    out.defaultWriteObject();
	}
    }

    // no need to serialize super class fields, this is done automatically
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	int handle = in.readInt();
	if (handle < 0) {
	    in.defaultReadObject();
	    cache.addIbis(in, -handle, this);
	} else {
	    IbisIdentifier ident = (IbisIdentifier)cache.getIbis(in, handle);
	    name = ident.name;
	    cluster = ident.cluster;
	    cpu = ident.cpu;
	}
    }

    public void free() {
	cache.removeIbis(this);
    }


    private void makeSerialForm() throws IOException {
	serialForm = Conversion.object2byte(this);
    }


    byte[] getSerialForm() throws IOException {
	if (serialForm == null) {
	    makeSerialForm();
	}
	return serialForm;
    }

    // Compare ranks here, much faster. This is method critical for Satin. --Rob
    public boolean equals(IbisIdentifier other) {
	return (cpu == other.cpu);
    }

    public boolean equals(Object o) {
	if (o == this) return true;

	if (o instanceof IbisIdentifier) {
	    IbisIdentifier other = (IbisIdentifier)o;
	    // there is only one messagePassing.Ibis per cpu, so this should be ok
	    return (cpu == other.cpu);
	}
	return false;
    }

    public String toString() {
	return ("(IbisIdent: name = \"" + name + "\")");
    }

    public String name() {
	return name;
    }

    public int hashCode() {
	return name.hashCode();
    }

    int getCPU() {
	return cpu;
    }
}
