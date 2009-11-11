/* $Id$ */

package ibis.ipl;

/**
 * Represents a location on which an Ibis instance runs. This is the
 * data type returned by {@link IbisIdentifier#location()}.
 * It represents a number of levels, for instance hostname, domain,
 * in that order, t.i., from detailed to coarse.
 * Level 0 represents the most detailed, level {@link #numberOfLevels()}-1
 * the most coarse.
 * <p>
 * Locations should be comparable with <code>equals()</code>, so implementations
 * probably redefine <code>hashCode()</code> and <code>equals()</code>.
 * <p>
 * Note that this interface extends {@link Comparable} and {@link
 * Iterable}. So, locations can be sorted, and the user can iterate over
 * the levels in a location.
 */
public interface Location extends java.io.Serializable, Comparable<Location>,
        Iterable<String> {
    /**
     * Returns the number of levels in this location.
     * @return
     *          the number of levels.
     */
    public int numberOfLevels();

    /**
     * Returns the name of the specified level.
     * Levels are numbered from 0 to {@link #numberOfLevels()}-1.
     * @param levelIndex
     *          the specified level.
     * @return
     *          the corresponding name.
     * @exception ArrayIndexOutOfBoundsException
     *          is thrown when the specified level does not correspond to a
     *          level in this location.
     */
    public String getLevel(int levelIndex);

    /**
     * Returns the location as a String array.
     * @return
     *          the location as a string array.
     */
    public String[] getLevels();

    /**
     * Returns the number of matching levels with the specified location,
     * comparing from coarse to detailed.
     * @param location
     *          the location to match with.
     * @return
     *          the number of matching levels.
     */
    public int numberOfMatchingLevels(Location location);

    /**
     * Returns the parent location of this location. This is a location
     * object that has the most detailed level stripped of.
     * If the object has only one level (or less), a location with 0 levels
     * is returned.
     * @return
     *          the parent location.
     */
    public Location getParent();
}
