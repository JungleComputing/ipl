package ibis.impl.registry;

import ibis.impl.IbisIdentifier;
import ibis.impl.Location;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.RegistryEventHandler;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public final class NullRegistry extends ibis.impl.Registry {
    
    private final IbisIdentifier identifier;

    @Override
    public long getSeqno(String name) throws IOException {
        throw new IOException("sequence numbers not supported by NullRegistry");
    }

    public NullRegistry(RegistryEventHandler handler, Properties props,
            byte[] data) throws IOException, IbisConfigurationException {
        if (handler != null) {
            throw new IOException("upcalls not supported by NullRegistry");
        }
        
        //FIXME: use real UUID generator (from smartsockets?)
        UUID id = UUID.randomUUID();
        
        Location location = Location.defaultLocation();

        String pool = props.getProperty(RegistryProperties.POOL);
        
        identifier = new IbisIdentifier(id.toString(), data, null, location, pool);
    }

    @Override
    protected void leave() throws IOException {
        //NOTHING
    }

    public void dead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        //NOTHING
    }

    public ibis.ipl.IbisIdentifier elect(String election) throws IOException {
        throw new IOException("elections not supported by NullRegistry");
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election) throws IOException {
        throw new IOException("elections not supported by NullRegistry");
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        //NOTHING
    }

    public void signal(String string, ibis.ipl.IbisIdentifier... ibisses) throws IOException {
        throw new IOException("signals not supported by NullRegistry");
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

}
