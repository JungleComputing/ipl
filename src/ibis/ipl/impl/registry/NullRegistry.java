package ibis.ipl.impl.registry;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
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
    public long getSequenceNumber(String name) throws IOException {
        throw new IbisConfigurationException(
                "sequence numbers not supported by NullRegistry");
    }

    /**
     * Creates a Null Registry.
     * 
     * @param handler
     *                registry handler to pass events to.
     * @param props
     *                properties of this registry.
     * @param data
     *                Ibis implementation data to attach to the IbisIdentifier.
     * @param ibisImplementationIdentifier
     *                the identification of this ibis implementation, including
     *                version, class and such. Must be identical for all ibisses
     *                in a single pool.
     * @throws IOException
     *                 in case of trouble.
     * @throws IbisConfigurationException
     *                 In case invalid properties were given.
     */
    public NullRegistry(IbisCapabilities caps, RegistryEventHandler handler,
            Properties props, byte[] data, String ibisImplementationIdentifier)
            throws IOException {

        if (handler != null) {
            throw new IbisConfigurationException(
                    "upcalls not supported by NullRegistry");
        }

        UUID id = UUID.randomUUID();

        Location location = Location.defaultLocation(props, null);

        String pool = props.getProperty(IbisProperties.POOL_NAME);

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

    public ibis.ipl.IbisIdentifier elect(String election, long timeoutMillis) throws IOException {
        throw new IbisConfigurationException(
                "elections not supported by NullRegistry");
    }
    
    public ibis.ipl.IbisIdentifier getElectionResult(String election)
            throws IOException {
        throw new IbisConfigurationException(
                "elections not supported by NullRegistry");
    }

    public ibis.ipl.IbisIdentifier getElectionResult(String election,
            long timeoutMillis) throws IOException {
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

    public ibis.ipl.IbisIdentifier[] diedIbises() {
        throw new IbisConfigurationException(
                "died not supported by NullRegistry");
    }

    public ibis.ipl.IbisIdentifier[] joinedIbises() {
        throw new IbisConfigurationException(
                "joins not supported by NullRegistry");
    }

    public ibis.ipl.IbisIdentifier[] leftIbises() {
        throw new IbisConfigurationException(
                "leaves not supported by NullRegistry");
    }

    public String[] receivedSignals() {
        throw new IbisConfigurationException(
                "signals not supported by NullRegistry");
    }

    public void disableEvents() {
        // empty ?
    }

    public void enableEvents() {
        // empty ?
    }

    public int getPoolSize() {
        throw new IbisConfigurationException(
                "pool size not supported by NullRegistry");
    }
    
    public String getPoolName() {
        return identifier.poolName();
    }

    public void waitUntilPoolClosed() {
        throw new IbisConfigurationException(
                "waitUntilPoolClosed not supported by NullRegistry");
    }
    
    public boolean isClosed() {
        throw new IbisConfigurationException(
        "closed world not supported by NullRegistry");
    }

    public Map<String, String> managementProperties() {
        return new HashMap<String, String>();
    }

    public String getManagementProperty(String key) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("no properties supported by null registry");
    }

    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("no properties supported by null registry");
    }

    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("no properties supported by null registry");
    }

    public void printManagementProperties(PrintStream stream) {
        //NOTHING
    }

    public boolean hasTerminated() {
        throw new IbisConfigurationException(
        "termination not supported by NullRegistry");
    }

    public void terminate() throws IOException {
        throw new IbisConfigurationException(
        "termination not supported by NullRegistry");
    }

    public IbisIdentifier waitUntilTerminated() {
        throw new IbisConfigurationException(
        "termination not supported by NullRegistry");
    }
}
