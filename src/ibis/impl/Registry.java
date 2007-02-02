/* $Id: Registry.java 4880 2006-12-08 09:06:32Z ceriel $ */

package ibis.impl;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.StaticProperties;

import java.io.IOException;
import java.util.Properties;

/** 
 * This implementation of the {@link ibis.ipl.Registry} interface
 * defines the API between an Ibis implementation and the registry.
 * This way, an Ibis implementation can dynamically load any registry
 * implementaton.
 */
public abstract class Registry implements ibis.ipl.Registry {

    /**
     * Notifies the registry that the calling Ibis instance is leaving.
     * @exception IOException may be thrown when communication with the
     * registry fails.
     */
    protected abstract void leave() throws IOException;

    /**
     * Used internally to initialize the registry.
     * @param ibis the Ibis instance.
     * @param needsUpcalls set when the registry must provide for connection
     * upcalls.
     * @param data the implementation dependent data in the IbisIdentifier.
     * @return the IbisIdentifier as initialized by the registry.
     * @exception IOException may be thrown when communication with the
     * registry fails.
     * @exception IbisConfigurationException may be thrown when the Ibis
     * instance is not configured for connection upcalls, but
     * <code>needsUpcalls</code> is set.
     */
    protected abstract IbisIdentifier init(Ibis ibis, boolean needsUpcalls,
            byte[] data) throws IOException, IbisConfigurationException;

    /**
     * Obtains a sequence number from the registry.
     * Each sequencer has a name, which must be provided to this call.
     * @param name the name of this sequencer.
     * @exception IOException may be thrown when communication with the
     * registry fails.
     */
    public abstract long getSeqno(String name) throws IOException;

    /**
     * Creates a registry for the specified Ibis instance.
     * @param ibis the Ibis instance.
     * @param registryName the class name of the registry implementation.
     * @exception IllegalArgumentException may be thrown if the registry
     * could not be created for some reason.
     */
    public static Registry loadRegistry(Ibis ibis, String registryName)
            throws IllegalArgumentException {
        Registry res = null;
        Class c;

        try {
            c = Class.forName(registryName);
        } catch (ClassNotFoundException t) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis registry " + t);
        }

        try {
            res = (Registry) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis registry " + e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException(
                    "Could not initialize Ibis registry " + e2);
        }

        return res;
    }
}
