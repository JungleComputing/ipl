/* $Id$ */

package ibis.ipl;

/**
 * Identifies an Ibis on the network. Implementations probably redefine
 * hashCode() and equals().
 */
public interface IbisIdentifier extends java.io.Serializable {
    /**
     * Returns the name of the virtual or physical cluster this Ibis
     * instance belongs to.
     * @return the cluster name.
     */
    public String cluster();
}
