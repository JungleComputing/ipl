/* $Id$ */

package ibis.impl;

import ibis.ipl.IbisConfigurationException;

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
     * @param needsUpcalls set when the registry must provide for connection
     * upcalls.
     * @param data the implementation dependent data in the IbisIdentifier.
     * @exception Throwable can be any exception resulting from looking up
     * the registry constructor or the invocation attempt.
     */
    public static Registry loadRegistry(Ibis ibis, String registryName,
            boolean needsUpcalls, byte[] data) throws Throwable {
        Registry res = null;
        Class c;

        c = Class.forName(registryName);

        try {
            return (Registry) c.getConstructor(new Class[] {
                    Ibis.class, boolean.class, byte[].class}).newInstance(
                        new Object[] {ibis, needsUpcalls, data});
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Returns the Ibis identifier.
     */
    public abstract IbisIdentifier getIbisIdentifier();
}
