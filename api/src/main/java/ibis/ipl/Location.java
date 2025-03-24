/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl;

/**
 * Represents a location on which an Ibis instance runs. This is the data type
 * returned by {@link IbisIdentifier#location()}. It represents a number of
 * levels, for instance hostname, domain, in that order, t.i., from detailed to
 * coarse. Level 0 represents the most detailed, level
 * {@link #numberOfLevels()}-1 the most coarse.
 * <p>
 * Locations should be comparable with <code>equals()</code>, so implementations
 * probably redefine <code>hashCode()</code> and <code>equals()</code>.
 * <p>
 * The location can be set using the {@link IbisProperties#LOCATION} property.
 * The <code>%HOSTNAME%</code> sequence is replaced by the hostname, the
 * <code>%DOMAIN%</code> sequence is replaced by the domain, where each "level"
 * in the domain name is a separate level in the location, and
 * <code>%FLAT_DOMAIN%</code> sequence is replaced by the domain, as a single
 * level in the location. <code>%PID%</code> is replaced by the process id, if
 * available, or <code>-1</code>. Unrecognized sequences are left untouched.
 * <p>
 * The default location is specified below, in {@link #DEFAULT_LOCATION}.
 * <p>
 * Note that this interface extends {@link Comparable} and {@link Iterable}. So,
 * locations can be sorted, and the user can iterate over the levels in a
 * location.
 */
public interface Location extends java.io.Serializable, Comparable<Location>, Iterable<String> {

    /**
     * Default location format.
     */
    public static final String DEFAULT_LOCATION = "%HOSTNAME%@%DOMAIN%";

    /**
     * Returns the number of levels in this location.
     *
     * @return the number of levels.
     */
    public int numberOfLevels();

    /**
     * Returns the name of the specified level. Levels are numbered from 0 to
     * {@link #numberOfLevels()}-1.
     *
     * @param levelIndex the specified level.
     * @return the corresponding name.
     * @exception ArrayIndexOutOfBoundsException is thrown when the specified level
     *                                           does not correspond to a level in
     *                                           this location.
     */
    public String getLevel(int levelIndex);

    /**
     * Returns the location as a String array.
     *
     * @return the location as a string array.
     */
    public String[] getLevels();

    /**
     * Returns the number of matching levels with the specified location, comparing
     * from coarse to detailed.
     *
     * @param location the location to match with.
     * @return the number of matching levels.
     */
    public int numberOfMatchingLevels(Location location);

    /**
     * Returns the parent location of this location. This is a location object that
     * has the most detailed level stripped of. If the object has only one level (or
     * less), a location with 0 levels is returned.
     *
     * @return the parent location.
     */
    public Location getParent();
}
