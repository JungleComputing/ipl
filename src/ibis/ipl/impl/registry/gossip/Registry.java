package ibis.ipl.impl.registry.gossip;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Registry extends ibis.ipl.impl.Registry {
    
    
    private final IbisIdentifier identifier;

    /**
     * Creates a Gossip Registry.
     * 
     * @param eventHandler
     *                Registry handler to pass events to.
     * @param userProperties
     *                properties of this registry.
     * @param data
     *                Ibis implementation data to attach to the IbisIdentifier.
     * @param ibisImplementationIdentifier the identification of this ibis 
     * implementation, including version, class and such. Must be identical
     * for all Ibisses in a single pool.
     * @throws IOException
     *                 in case of trouble.
     * @throws IbisConfigurationException
     *                 In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities,
            RegistryEventHandler eventHandler, Properties userProperties,
            byte[] data, String ibisImplementationIdentifier) throws IbisConfigurationException, IOException,
            IbisConfigurationException {
        identifier = null;
        
    }
    
    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        throw new IOException("Gossip registry does not support sequence numbers");
    }

    @Override
    public void leave() throws IOException {
        //TODO: send leave to all nodes we know
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibisIdentifier)
            throws IOException {
        //TODO: create "dead" event for this ibis
    }

    public ibis.ipl.IbisIdentifier[] diedIbises() {
        // TODO Auto-generated method stub
        return null;
    }

    public void disableEvents() {
        // TODO Auto-generated method stub

    }

    public ibis.ipl.IbisIdentifier elect(String electionName)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public void enableEvents() {
        // TODO Auto-generated method stub

    }

    public ibis.ipl.IbisIdentifier getElectionResult(String electionName)
            throws IOException {
        return getElectionResult(electionName, 0);
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String electionName, long timeoutMillis)
            throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    public int getPoolSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    public ibis.ipl.IbisIdentifier[] joinedIbises() {
        // TODO Auto-generated method stub
        return null;
    }

    public ibis.ipl.IbisIdentifier[] leftIbises() {
        // TODO Auto-generated method stub
        return null;
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibisIdentifier)
            throws IOException {
        // TODO Auto-generated method stub

    }

    public String[] receivedSignals() {
        // TODO Auto-generated method stub
        return null;
    }

    public void signal(String signal,
            ibis.ipl.IbisIdentifier... ibisIdentifiers) throws IOException {
        // TODO Auto-generated method stub

    }

    public void waitUntilPoolClosed() {
        // TODO Auto-generated method stub

    }

    public Map<String, String> managementProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getManagementProperty(String key) throws NoSuchPropertyException {
        // TODO Auto-generated method stub
        return null;
    }

    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        // TODO Auto-generated method stub
        
    }

    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        // TODO Auto-generated method stub
        
    }

}
