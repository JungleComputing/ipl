package ibis.ipl;

// Be careful, classes implementing this interface should override the methods equals() and hashCode().
// note: if an IbisIdentifier is sent over the network, it is not guaranteed that the object on the remote
// side will be a new one. This allows the Ibis implementation to cache IbisIdentifiers.
public abstract class IbisIdentifier implements java.io.Serializable {
	protected String name;

        // all Ibis identifiers need an address, which is used by the name server.
	protected java.net.InetAddress address; 

	protected IbisIdentifier() {}

	protected IbisIdentifier(String name, java.net.InetAddress address) { 
		this.name = name;
		this.address = address;
	}

	public java.net.InetAddress address() {
		return address;
	}

	public String name() {
		return name;
	}
}
