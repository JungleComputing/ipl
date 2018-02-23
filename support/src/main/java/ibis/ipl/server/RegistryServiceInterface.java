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
package ibis.ipl.server;

import java.io.IOException;
import java.util.Map;

import ibis.ipl.IbisIdentifier;

/**
 * Interface to the registry. Mostly for getting information on the members of
 * each pool from the registry.
 * 
 * @ibis.experimental
 */
public interface RegistryServiceInterface {

    /**
     * Obtains a list of pools from this registry service.
     * @return a list of pool names.
     * @throws IOException is thrown in case of communication troubles.
     */
    public String[] getPools() throws IOException;

    /**
     * Obtains a map mapping pool names to pool sizes from this registry service.
     * @return a map mapping pool names to pool sizes.
     * @throws IOException is thrown in case of communication troubles.
     */
    public Map<String, Integer> getPoolSizes() throws IOException;

    /**
     * Obtains a list of locations of members of a specified pool.
     * @param poolName the specified pool.
     * @return a list of locations of members of this pool.
     * @throws IOException is thrown in case of communication troubles.
     */
    public String[] getLocations(String poolName) throws IOException;

    /**
     * Obtains the current members of a specified pool.
     * @param poolName the specified pool.
     * @return a list containing the current members of the specified pool.
     * @throws IOException is thrown in case of communication troubles.
     */
    public IbisIdentifier[] getMembers(String poolName) throws IOException;

}