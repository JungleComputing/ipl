/* $Id: IbisIdentifier.java 4893 2006-12-08 15:15:12Z ceriel $ */

package ibis.impl;

/**
 * Identifies an Ibis instance on the network.
 */
public final class IbisIdentifier implements ibis.ipl.IbisIdentifier {
    /**
     * This field is used to indicate to which virtual or physical cluster
     * this ibis belongs.
     */
    private final String cluster;

    /**
     * Numbering of Ibis instances, provided by the registry.
     */
    private final int joinId;

    /**
     * Extra data for implementation.
     */
    private final byte[] data;

    /**
     * Constructs an <code>IbisIdentifier</code>.
     */
    public IbisIdentifier(int id, byte[] data, String cluster) {
        this.joinId = id;
        this.data = data;
        this.cluster = cluster;
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

        IbisIdentifier other = (IbisIdentifier) o;
        return other.joinId == joinId;
    }

    /**
     * Computes the hashcode.
     * @return the hashcode.
     */
    public int hashCode() {
        return joinId;
    }

    /**
     * Initializes the <code>cluster</code> field.
     */
    public static String getCluster() {
        String cluster = System.getProperty("ibis.pool.cluster");
        if (cluster == null) {
            // Backwards compatibility, will be deprecated.
            cluster = System.getProperty("cluster");
        }
        if (cluster == null) {
            cluster = "unknown";
        }
        return cluster;
    }

    /**
     * Returns a string identifying the Ibis instance to which this
     * <code>IbisIdentifier</code> refers. This method can be overridden
     * by Ibis implementations to add more information for debugging prints.
     * @return a string representation of this IbisIdentifier.
     */
    public String toString() {
        return "(IbisNo " + joinId + ")";
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
     * Obtains the implementation dependant data.
     * @return the data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Obtains the Id of this Ibis instance.
     */
    public int getId() {
        return joinId;
    }
}
