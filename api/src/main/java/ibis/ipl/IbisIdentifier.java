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
 * An IbisIdentifier uniquely identifies an Ibis instance on the network.
 * <p>
 * IbisIdentifiers should be comparable with <code>equals()</code>, so
 * implementations probably redefine <code>hashCode()</code> and
 * <code>equals()</code>. When two IbisIdentifiers compare equal, they identify
 * the same Ibis instance.
 * <p>
 * IbisIdentifiers also implement the {@link Comparable} interface, which means
 * they can be sorted.
 */
public interface IbisIdentifier extends java.io.Serializable, Comparable<IbisIdentifier> {
    /**
     * Returns the {@link Location} of this Ibis instance.
     *
     * @return the location.
     */
    public Location location();

    /**
     * Returns the name of the pool to which this Ibis instance belongs.
     *
     * @return the pool name.
     */
    public String poolName();

    /**
     * Returns a name uniquely identifying the Ibis instance to which this
     * IbisIdentifier refers. Names are only unique within a single Ibis pool.
     *
     * @return a name.
     */
    public String name();

    /**
     * Returns the application tag provided when the Ibis instance with this
     * identifier was constructed or null if none was provided. Applications should
     * try to keep this as short as possible since these will be sent over the
     * network many times.
     *
     * @return a tag.
     * @throws RuntimeException if the current VM does not support UTF-8 encoding
     *                          for strings.
     */
    public String tagAsString();

    /**
     * Returns the application tag provided when the Ibis instance with this
     * identifier was constructed or null if none was provided. Applications should
     * try to keep this as short as possible since these will be sent over the
     * network many times.
     *
     * @return a tag.
     */
    public byte[] tag();

    /**
     * Returns a human-readable but not necessarily unique string identifying the
     * Ibis instance to which this IbisIdentifier refers. This method can be used
     * for debugging prints.
     *
     * @return a string representation of this IbisIdentifier.
     */
    @Override
    public String toString();
}
