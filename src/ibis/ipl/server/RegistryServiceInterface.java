package ibis.ipl.server;

import java.io.IOException;
import java.util.Map;

import ibis.ipl.IbisIdentifier;

public interface RegistryServiceInterface {

    public String[] getPools() throws IOException;
    
    public Map<String, Integer> getPoolSizes() throws IOException;
    
    public String[] getLocations(String poolName) throws IOException;

    public IbisIdentifier[] getMembers(String poolName) throws IOException;

}