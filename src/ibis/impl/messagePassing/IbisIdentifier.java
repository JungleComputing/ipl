package ibis.ipl.impl.messagePassing;


// Make this final, make inlining possible
final class IbisIdentifier extends ibis.ipl.IbisIdentifier implements java.io.Serializable
{

    String name;
    int cpu;

    IbisIdentifier(String name, int cpu) {
	    try {
		    this.address = java.net.InetAddress.getLocalHost();
	    } catch (java.net.UnknownHostException e) {
		    // this should not happen.
		    System.err.println("EEk: could not get my own IP address: " + e);
		    System.exit(1);
	    }
	    this.name = name;// + "@" + cpu;
	    this.cpu  = cpu;
    }

    // Compare ranks here, much faster. This is method critical for Satin. --Rob
    public boolean equals(ibis.ipl.impl.messagePassing.IbisIdentifier other) {
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
	return ("(IbisIdent: name = " + name + ")");
    }

    public String name() {
	return name;
    }

    public int hashCode() {
	return name.hashCode();
    }
}
