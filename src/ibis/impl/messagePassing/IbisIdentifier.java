package ibis.ipl.impl.messagePassing;

import ibis.io.MantaInputStream;
import ibis.io.MantaOutputStream;


// the implements should be unnecessary, but the IOGenerator does not 
// see that the super class implents it, and rewrites the bytecode.
final class IbisIdentifier
	extends ibis.ipl.IbisIdentifier
	implements java.io.Serializable, ibis.io.Serializable
{
    int cpu;

    IbisIdentifier(String name, int cpu) {
	    try {
		    this.address = java.net.InetAddress.getLocalHost();
	    } catch (java.net.UnknownHostException e) {
		    // this should not happen.
		    System.err.println("EEk: could not get my own IP address: " + e);
		    System.exit(1);
	    }
//	    this.name = name + "@" + cpu;
	    this.name = new String(name);
	    this.cpu  = cpu;
    }


    public IbisIdentifier(MantaInputStream stream) throws ibis.ipl.IbisIOException {
	stream.addObjectToCycleCheck(this);
	int handle = stream.readInt();
	if(handle < 0) {
	    try {
		address = java.net.InetAddress.getByName(stream.readUTF()); // this does not do a real lookup
	    } catch (Exception e) {
		System.err.println("EEK, could not create an inet address from a IP address. This should not happen");
		System.exit(1);
	    }
	    name = stream.readUTF();
	    cpu = stream.readInt();
	    Ibis.globalIbis.identTable.addIbis(stream, -handle, this);
	} else {
	    IbisIdentifier ident = (IbisIdentifier) Ibis.globalIbis.identTable.getIbis(stream, handle);
	    address = ident.address;
	    name = ident.name;
	    cpu = ident.cpu;
	}
    }


    // Compare ranks here, much faster. This is method critical for Satin. --Rob
    public boolean equals(ibis.ipl.impl.messagePassing.IbisIdentifier other) {
manta.runtime.RuntimeSystem.DebugMe(this, other);
	return cpu == other.cpu;
    }

    public boolean equals(Object o) {
	if(o == this) return true;

	if (o instanceof ibis.ipl.impl.messagePassing.IbisIdentifier) {
	    ibis.ipl.impl.messagePassing.IbisIdentifier other = (ibis.ipl.impl.messagePassing.IbisIdentifier)o;
	    // there is only one messagePassing.Ibis per cpu, so this should be ok
	    return cpu == other.cpu;
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

    public final void generated_WriteObject(MantaOutputStream stream) throws ibis.ipl.IbisIOException {
	int handle = Ibis.globalIbis.identTable.getHandle(stream, this);
	stream.writeInt(handle);
	if(handle < 0) { // First time, send it.
	    stream.writeUTF(address.getHostAddress());
	    stream.writeUTF(name);
	    stream.writeInt(cpu);
	}
    }
}
