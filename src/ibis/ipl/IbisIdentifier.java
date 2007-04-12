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
    public Location location();

    /**
     * Returns the name of the pool to which
     * this Ibis instance belongs.
     * @return the poolname.
     */
    public String poolName();
    
    /**
     * Returns a name uniquely identifying the Ibis instance to which
     * this IbisIdentifier refers. Names are only unique within a
     * single Ibis pool.
     * @return a name.
     */
    public String name();

    /**
     * Returns a human readable but not neccesarily unique string
     * identifying the Ibis instance to which this IbisIdentifier
     * refers. This method can be used for debugging prints.
     * @return a string representation of this IbisIdentifier.
     */
    public String toString();
}
