package ibis.ipl.impl.registry;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

/**
 * 
 * Registry implementation that does nothing. Throws an Exception most calls.
 * 
 */
public final class NullRegistry extends ibis.ipl.impl.Registry {

    private final IbisIdentifier identifier;

    @Override
    public long getSeqno(String name) throws IOException {
        throw new IbisConfigurationException(
                "sequence numbers not supported by NullRegistry");
    }

    /**
     * Creates a Null Registry.
     * 
     * @param handler
     *            registry handler to pass events to.
     * @param props
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public NullRegistry(RegistryEventHandler handler, Properties props,
            byte[] data) throws IOException {
        if (handler != null) {
            throw new IbisConfigurationException(
                    "upcalls not supported by NullRegistry");
        }

        // FIXME: use real UUID generator (from smartsockets?)
        UUID id = UUID.randomUUID();

        Location location = Location.defaultLocation();

        String pool = props.getProperty(RegistryProperties.POOL);

        identifier = new IbisIdentifier(id.toString(), data, null, location,
                pool);
    }

    @Override
    public void leave() throws IOException {
        // NOTHING
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        // NOTHING
    }

    public ibis.ipl.IbisIdentifier elect(String election) throws IOException {
        throw new IbisConfigurationException(
                "elections not supported by NullRegistry");
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election)
            throws IOException {
        throw new IbisConfigurationException(
                "elections not supported by NullRegistry");
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibis) throws IOException {
        // NOTHING
    }

    public void signal(String string, ibis.ipl.IbisIdentifier... ibisses)
            throws IOException {
        throw new IbisConfigurationException(
                "signals not supported by NullRegistry");
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

}
