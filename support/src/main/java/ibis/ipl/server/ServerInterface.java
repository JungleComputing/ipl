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

import ibis.smartsockets.direct.DirectSocketAddress;

/**
 * Interface to the Ibis server. This can be both locally to the actual server
 * object, or remotely through a {@link ServerConnection}.
 * 
 * @ibis.experimental
 */
public interface ServerInterface {

    /**
     * Returns the address of this server as a string.
     */
    public String getAddress() throws IOException;

    /**
     * Returns the names of all user services currently in this server.
     */
    public String[] getServiceNames() throws IOException;

    /**
     * Returns the addresses of all hubs known to this server.
     */
    public String[] getHubs() throws IOException;

    /**
     * Tell the server about some hubs.
     */
    public void addHubs(DirectSocketAddress... hubAddresses) throws IOException;

    /**
     * Tell the server about some hubs.
     */
    public void addHubs(String... hubAddresses) throws IOException;

    /**
     * Stops all services. May wait until the services are idle.
     * 
     * @param timeout
     *            timeout for ending all services in milliseconds. 0 == wait
     *            forever, -1 == not wait.
     */
    public void end(long timeout) throws IOException;

    /**
     * Obtains the registry service interface from this Server.
     * @return the registry service interface.
     */
    public RegistryServiceInterface getRegistryService();

    /**
     * Obtains the management service interface from this Server.
     * @return the management service interface.
     */
    public ManagementServiceInterface getManagementService();
}