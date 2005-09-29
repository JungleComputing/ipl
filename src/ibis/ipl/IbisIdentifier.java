/* $Id$ */

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
     * @return <code>true</code> if equal, <code>false</code> if not.
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (! o.getClass().equals(getClass())) {
            return false;
        }
        return name.equals(((IbisIdentifier) o).name);
    }

    /**
     * Computes the hashcode.
     * @return the hashcode.
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Initializes the <code>cluster</code> field.
     */
    protected void init_cluster() {
        cluster = System.getProperty("ibis.pool.cluster");
        if (cluster == null) {
            // Backwards compatibility, will be deprecated.
            cluster = System.getProperty("cluster");
        }
        if (cluster == null) {
            cluster = "unknown";
        }
    }

    /**
     * Returns a string identifying the Ibis instance to which this
     * <code>IbisIdentifier</code> refers. This method can be overridden
     * by Ibis implementations to add more information for debugging prints,
     * and should not be used to create names for receive ports. For that,
     * {@link #name()} should be used.
     * @return a string representation of this IbisIdentifier.
     */
    public String toString() {
        return "(IbisId: " + name + ")";
    }

    /**
     * Returns a name uniquely identifying the Ibis instance to which this
     * <code>IbisIdentifier</code> refers.
     * @return the name of the Ibis instance.
     */
    public final String name() {
        return name;
    }

    /**
     * Returns the name of the virtual or physical cluster this Ibis
     * belongs to. If not set in the System property, it is "unknown".
     * @return the cluster name.
     */
    public final String cluster() {
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
