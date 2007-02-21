/* $Id$ */

package ibis.ipl;

/**
 * Uniquely identifies an Ibis on the network. Should be comparable with
 * <code>equals()</code>, so implementations probably redefine
 * <code>hashCode()</code> and <code>equals()</code>.
 * When two IbisIdentifiers compare equal, they identify the same Ibis
 * instance.
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
}
