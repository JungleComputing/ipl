package ibis.ipl;

/**
   Be careful, classes implementing this abstact class should override the methods
   equals() and hashCode().
   note: if an IbisIdentifier is sent over the network, it is not guaranteed
   that the object on the remote side will be a new one. This allows the Ibis
   implementation to cache IbisIdentifiers.
**/
public abstract class IbisIdentifier implements java.io.Serializable {
	protected String name;

	/**
	 * This field is used to indicate to which virtual or physical cluster
	 * this ibis belongs. It is set according to the 'cluster' System
	 * property. Nodes which don't have that property set will get
	 * "unknown".
	 */
	protected String cluster;

	protected IbisIdentifier() {
		throw new IbisError("No arg constructor of IbisIdentifier, this should not happen.");
	}

	protected IbisIdentifier(String name) { //, java.net.InetAddress address) { 
		this.name = name;
		init_cluster();
	}

	public boolean equals(Object o) {
		throw new IbisError("Subclasses of IbisIdentifier should override equals");
	}

	public int hashCode() {
		throw new IbisError("Subclasses of IbisIdentifier should override hashCode");
	}

	protected void init_cluster() {
		cluster = System.getProperty("cluster");
		if (cluster == null) {
			cluster = "unknown";
		}
	}

	public String name() {
		return name;
	}

	public String cluster() {
		return cluster;
	}
}
