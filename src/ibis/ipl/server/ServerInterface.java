package ibis.ipl.server;

import java.io.IOException;

import ibis.smartsockets.direct.DirectSocketAddress;

/**
 * Interface to the Ibis server. This can be both locally to the actual server
 * object, or remotely though a {@link ServerConnection}
 * 
 * @ibis.experimental
 */
public interface ServerInterface {

    /**
     * Returns the address of this server as a string
     */
    public String getAddress() throws IOException;

    /**
     * Returns the names of all user services currently in this server
     */
    public String[] getServiceNames() throws IOException;

    /**
     * Returns the addresses of all hubs known to this server
     */
    public String[] getHubs() throws IOException;

    /**
     * Tell the server about some hubs
     */
    public void addHubs(DirectSocketAddress... hubAddresses) throws IOException;

    /**
     * Tell the server about some hubs
     */
    public void addHubs(String... hubAddresses) throws IOException;

    /**
     * Stops all services. May wait until the services are idle.
     * 
     * @param timeout
     *            timeout for ending all services in Milliseconds. 0 == wait
     *            forever, -1 == no not wait.
     */
    public void end(long timeout) throws IOException;

    public RegistryServiceInterface getRegistryService();

    public ManagementServiceInterface getManagementService();
}