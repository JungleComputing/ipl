package ibis.ipl;

/**
 * Identifies an Ibis on the network.
 * Be careful, classes implementing this abstract class should override
 * the methods <code>equals</code>() and <code>hashCode</code>().
 * note: if an <code>IbisIdentifier</code> is sent over the network,
 * it is not guaranteed that the object on the remote side will be a new one.
 * This allows the Ibis implementation to cache <code>IbisIdentifier</code>s.
 */
public abstract class IbisIdentifier implements java.io.Serializable {

    /** The name of this Ibis, given to it when it was created. */
    protected String name;

    /**
     * This field is used to indicate to which virtual or physical cluster
     * this ibis belongs. It is set according to the 'cluster' System
     * property. Nodes which don't have that property set will get
     * "unknown".
     */
    protected String cluster;

    /**
     * Parameter-less constructor should not happen, therefore private.
     */
    private IbisIdentifier() {
	throw new IbisError("No args constructor of IbisIdentifier");
    }

    /**
     * Constructs an <code>IbisIdentifier</code>.
     * @param name the name of this ibis.
     */
    protected IbisIdentifier(String name) { //, java.net.InetAddress address) { 
	this.name = name;
	init_cluster();
    }

    /**
     * Compares two Ibis identifiers.
     * Note: should be redefined by all Ibis implementations.
     * @exception IbisError is thrown when this method is not redefined
     * by the ibis implementation.
     */
    public boolean equals(Object o) {
	throw new IbisError("IbisIdentifier subclass must override equals");
    }

    /**
     * Computes the hashcode.
     * Note: should be redefined by all Ibis implementations.
     * @return the hashcode.
     * @exception IbisError is thrown when this method is not redefined
     * by the ibis implementation.
     */
    public int hashCode() {
	throw new IbisError("IbisIdentifier subclass must override hashCode");
    }

    /**
     * Initializes the <code>cluster</code> field.
     */
    protected void init_cluster() {
	cluster = System.getProperty("cluster");
	if (cluster == null) {
	    cluster = "unknown";
	}
    }

    /**
     * Returns the name of this Ibis.
     * @return the Ibis name.
     */
    public String name() {
	return name;
    }

    /**
     * Returns the name of the virtual or physical cluster this Ibis
     * belongs to. If not set in the System property, it is "unknown".
     * @return the cluster name.
     */
    public String cluster() {
	return cluster;
    }

    /**
     * Releases any resources held by the Ibis identifier.
     * The default does nothing.
     */
    public void free() {
    	/* do nothing */
    }
}
