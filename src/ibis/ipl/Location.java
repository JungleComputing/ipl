/* $Id$ */

// FIXME docs: levels run from 0 - #levels-1 or from 1? --Rob
// FIXME docs: what is the ordering of level numbers? domain == 0 or size-1 ?

package ibis.ipl;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link IbisIdentifier#location()}.
 * It represents a number of levels, for instance hostname, domain,
 * in that order, t.i., from detailed to coarse.
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
     * Returns the parent location of this location. This is a location
     * object that has the most detailed level stripped of.
     * If the object one level (or less), a location with 0 levels
     * is returned.
     * @return the parent location.
     */
    public Location getParent();
}
