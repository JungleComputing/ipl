package ibis.ipl.impl.registry.gossip;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;

import java.io.IOException;
import java.util.Properties;

public class Registry extends ibis.ipl.impl.Registry {
    
    
    private final IbisIdentifier identifier;

    /**
     * Creates a Gossip Registry.
     * 
     * @param handler
     *            registry handler to pass events to.
     * @param userProperties
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public Registry(IbisCapabilities caps, RegistryEventHandler handler,
            Properties userProperties, byte[] data) {
        identifier = null;
        
        
    }
    
    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    @Override
    public long getSeqno(String name) throws IOException {
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

    public void waitForAll() {
        // TODO Auto-generated method stub

    }

}
