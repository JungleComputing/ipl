package ibis.ipl.impl.registry.gossip;

import java.io.IOException;
import java.util.UUID;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.util.TypedProperties;

class Pool extends Thread {

    private final String name;

    private final CommunicationHandler commHandler;

    private final IbisIdentifier ibisIdentifier;

    public Pool(IbisCapabilities capabilities, TypedProperties properties,
            byte[] ibisData, Registry registry) throws IOException {
        UUID id = UUID.randomUUID();

        name = properties.getProperty(IbisProperties.POOL_NAME);

        if (name == null) {
            throw new IbisConfigurationException(
                    "cannot initialize registry, property "
                            + IbisProperties.POOL_NAME + " is not specified");
        }

        commHandler = new CommunicationHandler(properties, this);

        Location location = Location.defaultLocation(properties);

        ibisIdentifier =
                new IbisIdentifier(id.toString(), ibisData, commHandler
                        .getAddress().toBytes(), location, name);

    }
    
    /* (non-Javadoc)
     * @see java.lang.Thread#start()
     */
    @Override
    public synchronized void start() {
        // TODO Auto-generated method stub
        super.setDaemon(true);
        super.start();
        
        commHandler.start();

    }

    public boolean isStopped() {
        // TODO Auto-generated method stub
        return false;
    }

    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) {
        // TODO Auto-generated method stub
        return null;
    }

    public IbisIdentifier elect(String electionName) {
        // TODO Auto-generated method stub
        return null;
    }

    public IbisIdentifier getIbisIdentifier() {
        // TODO Auto-generated method stub
        return ibisIdentifier;
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibisIdentifier2) {
        // TODO Auto-generated method stub
        
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibisIdentifier2) {
        // TODO Auto-generated method stub
        
    }

    public void signal(String signal, ibis.ipl.IbisIdentifier[] ibisIdentifiers) {
        // TODO Auto-generated method stub
        
    }

    public void leave() {
        // TODO Auto-generated method stub
        
    }

}
