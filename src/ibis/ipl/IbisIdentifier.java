/* $Id$ */

package ibis.ipl;

/**
 * Identifies an Ibis on the network. Should be comparable with
 * <code>equals()</code>, so implementations probably redefine
 * <code>hashCode()</code> and <code>equals()</code>.
 */
public interface IbisIdentifier extends java.io.Serializable {
    /**
     * Returns the name of the virtual or physical cluster this Ibis
     * instance belongs to.
     * @return the cluster name.
     */
    public String cluster();

    /**
     * Returns the name of the pool that identifies the run to which
     * this Ibis instance belongs with the Ibis registry.
     * @return the poolname.
     */
    public String getPool();
}
