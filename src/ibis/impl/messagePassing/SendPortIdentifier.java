package ibis.ipl.impl.messagePassing;

final class SendPortIdentifier implements ibis.ipl.SendPortIdentifier,
    java.io.Serializable {

    String name;
    String type;
    int cpu;
    int port;
    IbisIdentifier ibisId;


    SendPortIdentifier(String name, String type) {
	synchronized (ibis.ipl.impl.messagePassing.Ibis.myIbis) {
	    port = ibis.ipl.impl.messagePassing.Ibis.myIbis.sendPort++;
	}
	this.name = name;
	this.type = type;
	this.ibisId = (IbisIdentifier)ibis.ipl.impl.messagePassing.Ibis.myIbis.identifier();
	cpu = ibis.ipl.impl.messagePassing.Ibis.myIbis.myCpu;
    }

    SendPortIdentifier(String name, String type, String ibisId,
			    int cpu, int port) {
	this.name = name;
	this.type = type;
// System.err.println("SendPortIdentifier.<ctor>: Lookup ibisId " + ibisId);
	this.ibisId = ibis.ipl.impl.messagePassing.Ibis.myIbis.lookupIbis(ibisId);
	this.cpu = cpu;
	this.port = port;
    }

    public boolean equals(ibis.ipl.SendPortIdentifier other) {
	    if (other == this) return true;
	    
	    if (other instanceof SendPortIdentifier) {
		    SendPortIdentifier o = (SendPortIdentifier)other;
		    return cpu == o.cpu && port == o.port;
	    }
	    
	    return false;
    }

    public String name() {
	if (name != null) {
	    return name;
	}

	return "anonymous";
    }

    public String type() {
	return type;
    }

    public ibis.ipl.IbisIdentifier ibis() {
	return ibisId;
    }

    public String toString() {
	return ("(SendPortIdent: name \"" + name + "\" type \"" + type +
		"\" cpu " + cpu + " port " + port + ")");
    }
}
