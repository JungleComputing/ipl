package ibis.ipl.server;

import ibis.ipl.IbisIdentifier;

import java.util.Map;

public interface RegistryServiceInterface {

    public abstract String getServiceName();

    public abstract Map<String, String> getStats();

    public abstract IbisIdentifier[] getMembers(String poolName);

}