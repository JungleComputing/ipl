/* $Id:$ */

package ibis.ipl;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link IbisIdentifier#getLocation()}.
 * It represents a number of levels, for instance domain, hostname,
 * in that order, t.i., from coarse to detailed.
 * Should be comparable with <code>equals()</code>, so implementations
 * probably redefine <code>hashCode()</code> and <code>equals()</code>.
 */
public interface Location extends java.io.Serializable, Comparable<Location> {
    /**
     * Returns the number of levels in this location.
     * @return the number of levels.
     */
    public int levels();

    /**
     * Returns the name of the specified level.
     * @param level the specified level.
     * @return the corresponding name.
     * @exception ArrayIndexOutOfBoundsException is thrown when the specified
     * level does not correspond to a level in this location.
     */
    public String levelName(int level);

    /**
     * Returns the number of matching levels with the specified location,
     * comparing from the bottom up.
     */
    public int matchingLevels(Location l);
}
