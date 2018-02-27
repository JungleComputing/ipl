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
package ibis.ipl.registry;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.Ibis;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;

/**
 *
 * Registry implementation that does nothing. Throws an Exception most calls.
 *
 */
public final class NullRegistry extends ibis.ipl.registry.Registry {

    private final IbisIdentifier identifier;

    @Override
    public long getSequenceNumber(String name) throws IOException {
        throw new IbisConfigurationException(
                "sequence numbers not supported by NullRegistry");
    }

    /**
     * Creates a Null Registry.
     *
     * @param capabilities
     *            the required capabilities
     * @param handler
     *            registry handler to pass events to.
     * @param properties
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @param tag
     *            A tag provided by the application for this ibis instance.
     * @param implementationVersion
     *            the identification of this ibis implementation, including
     *            version, class and such. Must be identical for all ibises in a
     *            single pool.
     * @param credentials
     *            the credentials
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public NullRegistry(IbisCapabilities capabilities,
            RegistryEventHandler handler, Properties properties, byte[] data,
            String implementationVersion, Credentials credentials, byte[] tag)
            throws IOException {

        if (handler != null) {
            throw new IbisConfigurationException(
                    "upcalls not supported by NullRegistry");
        }

        String id = properties.getProperty(Ibis.ID_PROPERTY);
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        Location location = Location.defaultLocation(properties, null);

        String pool = properties.getProperty(IbisProperties.POOL_NAME);

        identifier = new IbisIdentifier(id, data, null, location, pool, tag);
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

    public ibis.ipl.IbisIdentifier elect(String election, long timeoutMillis)
            throws IOException {
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

    @Override
    public String[] wonElections() {
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

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "no properties supported by null registry");
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "no properties supported by null registry");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "no properties supported by null registry");
    }

    public void printManagementProperties(PrintStream stream) {
        // NOTHING
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

    @Override
    public IbisIdentifier getRandomPoolMember() {
        return null;
    }

    @Override
    public void addTokens(String name, int count) throws IOException {
        throw new IbisConfigurationException(
                "tokens not supported by NullRegistry");
    }

    @Override
    public String getToken(String name) throws IOException {
        throw new IbisConfigurationException(
                "tokens not supported by NullRegistry");
    }
}
