/* $Id$ */

package ibis.ipl;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link IbisIdentifier#getLocation()}.
 * It represents a number of levels, for instance domain, hostname,
 * in that order, t.i., from coarse to detailed.
 * Should be comparable with <code>equals()</code>, so implementations
 * probably redefine <code>hashCode()</code> and <code>equals()</code>.
 */
public interface Location extends java.io.Serializable, Comparable<Location>,
        Iterable<String> {
    /**
     * Returns the number of levels in this location.
     * @return the number of levels.
     */
    public int numberOfLevels();

    /**
     * Returns the name of the specified level.
     * @param levelIndex the specified level.
     * @return the corresponding name.
     * @exception ArrayIndexOutOfBoundsException is thrown when the specified
     * level does not correspond to a level in this location.
     */
    public String getLevel(int levelIndex);

    /**
     * Returns the location as a String array.
     * @return the location as a string array.
     */
    public String[] getLevels();

    /**
     * Returns the number of matching levels with the specified location,
     * comparing from coarse to detailed.
     * @param location the location to match with.
     * @return the number of matching levels.
     */
    public int numberOfMatchingLevels(Location location);

    /**
     * Returns something that could represent a cluster name. This is a
     * concatenation of all location level names but the last.
     * @return the cluster.
     */
    public String getCluster();
}
