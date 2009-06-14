package ibis.ipl.server;

import ibis.ipl.management.ManagementService;
import ibis.smartsockets.direct.DirectSocketAddress;

public interface ServerInterface {

    public RegistryServiceInterface getRegistryService();

    public ManagementService getManagementService();

    /**
     * Returns the address of this server as a string
     */
    public String getAddress();

    /**
     * Returns the names of all user services currently in this server
     */
    public String[] getServiceNames();

    /**
     * Returns the addresses of all hubs known to this server
     */
    public String[] getHubs();

    /**
     * Tell the server about some hubs
     */
    public void addHubs(DirectSocketAddress... hubAddresses);

    /**
     * Tell the server about some hubs
     */
    public void addHubs(String... hubAddresses);

    /**
     * Stops all services. May wait until the services are idle.
     * 
     * @param timeout
     *            timeout for ending all services in Milliseconds. 0 == wait
     *            forever, -1 == no not wait.
     */
    public void end(long timeout);

}