package ibis.ipl;

// Be careful, classes implementing this interface should override the methods
// equals() and hashCode().
// note: if an IbisIdentifier is sent over the network, it is not guaranteed
// that the object on the remote side will be a new one. This allows the Ibis
// implementation to cache IbisIdentifiers.
public abstract class IbisIdentifier implements java.io.Serializable {
	protected String name;

	/**
	 * This field is used to indicate to which virtual or physical cluster
	 * this ibis belongs. It is set according to the 'cluster' System
	 * property. Nodes which don't have that property set will get
	 * unknown@<all bytes of IP except the last in hex> at cluster. This
	 * way all nodes in the same 'network' automagically get the same cluster
	 * property. We should of course use netmasks and deal with ipv6...
	 */
	protected String cluster;

    // all Ibis identifiers need an address, which is used by the name server.
	protected java.net.InetAddress address; 

	protected IbisIdentifier() {}

	protected IbisIdentifier(String name, java.net.InetAddress address) { 
		this.name = name;
		this.address = address;
		init_cluster();
	}

	protected void init_cluster() {
		cluster = System.getProperty("cluster");
		if (cluster == null) {
			cluster = "unknown@";

			byte[] bytes = address.getAddress();
			for (int i = 0; i < bytes.length - 1; i++) {
				cluster += Integer.toHexString(bytes[i]);
			}
		}
	}

	public java.net.InetAddress address() {
		return address;
	}

	public String name() {
		return name;
	}

	public String cluster() {
		return cluster;
	}
}
