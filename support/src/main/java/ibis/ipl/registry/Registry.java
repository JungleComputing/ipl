/*
 * Copyright 2010 Vrije Universiteit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/* $Id$ */

package ibis.ipl.registry;

import java.io.IOException;
import java.util.Properties;

import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;

/**
 * This implementation of the {@link ibis.ipl.Registry} interface defines the
 * API between an Ibis implementation and the registry. This way, an Ibis
 * implementation can dynamically load any registry implementation.
 */
public abstract class Registry implements ibis.ipl.Registry {

    /**
     * Notifies the registry that the calling Ibis instance is leaving.
     *
     * @exception IOException may be thrown when communication with the registry
     *                        fails.
     */
    public abstract void leave() throws IOException;

    /**
     * Obtains a sequence number from the registry. Each sequencer has a name, which
     * must be provided to this call.
     *
     * @param name the name of this sequencer.
     * @exception IOException may be thrown when communication with the registry
     *                        fails.
     */
    @Override
    public abstract long getSequenceNumber(String name) throws IOException;

    /**
     * Creates a registry for the specified Ibis instance.
     *
     * @param capabilities          capabilities required of this registry
     * @param handler               the handler for registry events, or
     *                              <code>null</code> if no registry events are
     *                              needed.
     * @param properties            to get some properties from, and to pass on to
     *                              the registry.
     * @param data                  the implementation dependent data in the
     *                              IbisIdentifier.
     * @param implementationVersion the identification of this Ibis implementation.
     *                              Must be identical for all Ibises in a single
     *                              pool.
     * @param tag                   the application level tag for the Ibis which is
     *                              constructing this registry.
     * @param credentials           authentication object to authenticate ibis at
     *                              registry
     * @return the registry
     * @exception Throwable can be any exception resulting from looking up the
     *                      registry constructor or the invocation attempt.
     */
    public static Registry createRegistry(IbisCapabilities capabilities, RegistryEventHandler handler, Properties properties, byte[] data,
            String implementationVersion, byte[] tag, Credentials credentials) throws Throwable {

        String registryName = properties.getProperty(IbisProperties.REGISTRY_IMPLEMENTATION);

        if (registryName == null) {
            throw new IbisConfigurationException(
                    "Could not create registry: " + "property " + IbisProperties.REGISTRY_IMPLEMENTATION + "  is not set.");
        } else if (registryName.equalsIgnoreCase("central")) {
            // shorthand for central registry
            return new ibis.ipl.registry.central.client.Registry(capabilities, handler, properties, data, implementationVersion, credentials, tag);
        } else if (registryName.equalsIgnoreCase("gossip")) {
            // shorthand for gossip registry
            return new ibis.ipl.registry.gossip.Registry(capabilities, handler, properties, data, implementationVersion, credentials, tag);
        } else if (registryName.equalsIgnoreCase("null")) {
            // shorthand for null registry
            return new ibis.ipl.registry.NullRegistry(capabilities, handler, properties, data, implementationVersion, credentials, tag);
        }

        Class<?> c = Class.forName(registryName);

        try {
            return (Registry) c
                    .getConstructor(new Class[] { IbisCapabilities.class, RegistryEventHandler.class, Properties.class, byte[].class, String.class,
                            Credentials.class, byte[].class })
                    .newInstance(new Object[] { capabilities, handler, properties, data, implementationVersion, credentials, tag });
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Returns the Ibis identifier.
     *
     * @return the ibis identifier
     */
    public abstract IbisIdentifier getIbisIdentifier();

    public abstract IbisIdentifier getRandomPoolMember();

    public abstract String[] wonElections();
}
