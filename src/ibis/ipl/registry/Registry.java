/* $Id$ */

package ibis.ipl.registry;

import ibis.io.Conversion;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisProperties;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the {@link ibis.ipl.Registry} interface defines the
 * API between an Ibis implementation and the registry. This way, an Ibis
 * implementation can dynamically load any registry implementation.
 */
public abstract class Registry implements ibis.ipl.Registry {

    private static final Logger logger = LoggerFactory
            .getLogger(Registry.class);

 

    /**
     * Notifies the registry that the calling Ibis instance is leaving.
     * 
     * @exception IOException
     *                may be thrown when communication with the registry fails.
     */
    public abstract void leave() throws IOException;

    /**
     * Obtains a sequence number from the registry. Each sequencer has a name,
     * which must be provided to this call.
     * 
     * @param name
     *            the name of this sequencer.
     * @exception IOException
     *                may be thrown when communication with the registry fails.
     */
    public abstract long getSequenceNumber(String name) throws IOException;

    /**
     * Creates a registry for the specified Ibis instance.
     * 
     * @param handler
     *            the handler for registry events, or <code>null</code> if no
     *            registry events are needed.
     * @param properties
     *            to get some properties from, and to pass on to the registry.
     * @param data
     *            the implementation dependent data in the IbisIdentifier.
     * @param version
     *            the identification of this Ibis implementation. Must be
     *            identical for all Ibises in a single pool.
     * @exception Throwable
     *                can be any exception resulting from looking up the
     *                registry constructor or the invocation attempt.
     */
    public static Registry createRegistry(IbisCapabilities caps,
            RegistryEventHandler handler, Properties properties, byte[] data,
            byte[] version) throws Throwable {

        String registryName = properties
                .getProperty(IbisProperties.REGISTRY_IMPLEMENTATION);

        if (registryName == null) {
            throw new IbisConfigurationException("Could not create registry: "
                    + "property " + IbisProperties.REGISTRY_IMPLEMENTATION
                    + "  is not set.");
        } else if (registryName.equalsIgnoreCase("central")) {
            // shorthand for central registry
            return new ibis.ipl.registry.central.client.Registry(caps, handler, properties, data, version);
        } else if (registryName.equalsIgnoreCase("gossip")) {
            // shorthand for gossip registry
            return new ibis.ipl.registry.gossip.Registry(caps, handler, properties, data, version);
        }

        Class<?> c = Class.forName(registryName);

        try {
            return (Registry) c.getConstructor(
                new Class[] { IbisCapabilities.class,
                        RegistryEventHandler.class, Properties.class,
                        byte[].class, byte[].class }).newInstance(
                new Object[] { caps, handler, properties, data, version });
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Returns the Ibis identifier.
     */
    public abstract IbisIdentifier getIbisIdentifier();
}
