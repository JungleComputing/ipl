/* $Id$ */

package ibis.ipl;

/**
 * Identifies an Ibis on the network. Should be comparable with
 * <code>equals()</code>, so implementations probably redefine
 * <code>hashCode()</code> and <code>equals()</code>.
 */
public interface IbisIdentifier extends java.io.Serializable,
       Comparable<IbisIdentifier> {
    /**
     * Returns the {@link Location} of this Ibis instance.
     * @return the location.
     */
    public Location getLocation();

    /**
     * Returns the name of the pool that identifies the run to which
     * this Ibis instance belongs with the Ibis registry.
     * @return the poolname.
     */
    public String getPool();

    /**
     * Returns something that could represent a cluster name. This is a
     * concatenation of all location level names but the last.
     * @return the cluster.
     */
    public String cluster();
}
