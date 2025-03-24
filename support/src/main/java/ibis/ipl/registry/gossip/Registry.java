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
package ibis.ipl.registry.gossip;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.Location;
import ibis.ipl.registry.statistics.Statistics;
import ibis.util.ThreadPool;
import ibis.util.TypedProperties;

public class Registry extends ibis.ipl.registry.Registry implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(Registry.class);

    private final IbisCapabilities capabilities;

    private final TypedProperties properties;

    private final String poolName;

    private final IbisIdentifier identifier;

    private final Statistics statistics;

    private final MemberSet members;

    private final ElectionSet elections;

    private final CommunicationHandler commHandler;

    private final Upcaller upcaller;

    // data structures the user can poll

    private final ArrayList<IbisIdentifier> joinedIbises;

    private final ArrayList<IbisIdentifier> leftIbises;

    private final ArrayList<IbisIdentifier> diedIbises;

    private final ArrayList<String> signals;

    private boolean stopped;

    /**
     * Creates a Gossip Registry.
     *
     * @param capabilities          capabilities required of this registry
     * @param eventHandler          Registry handler to pass events to.
     * @param userProperties        properties of this registry.
     * @param ibisData              Ibis implementation data to attach to the
     *                              IbisIdentifier.
     * @param credentials           credentials used for authenticating this ibis at
     *                              the server
     * @param implementationVersion the identification of this ibis implementation,
     *                              including version, class and such. Must be
     *                              identical for all Ibises in a single poolName.
     * @param applicationTag        A tag provided by the application constructing
     *                              this ibis.
     * @throws IOException                in case of trouble.
     * @throws IbisConfigurationException In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities, RegistryEventHandler eventHandler, Properties userProperties, byte[] ibisData,
            String implementationVersion, Credentials credentials, byte[] applicationTag)
            throws IbisConfigurationException, IOException, IbisConfigurationException {
        this.capabilities = capabilities;

        if (capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED)) {
            throw new IbisConfigurationException("gossip registry does not support totally ordered membership");
        }

        if (capabilities.hasCapability(IbisCapabilities.CLOSED_WORLD)) {
            throw new IbisConfigurationException("gossip registry does not support closed world");
        }

        if (capabilities.hasCapability(IbisCapabilities.ELECTIONS_STRICT)) {
            throw new IbisConfigurationException("gossip registry does not support strict elections");
        }

        if (capabilities.hasCapability(IbisCapabilities.TERMINATION)) {
            throw new IbisConfigurationException("gossip registry does not support termination");
        }

        properties = RegistryProperties.getHardcodedProperties();
        properties.addProperties(userProperties);

        if ((capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE)) && eventHandler == null) {
            joinedIbises = new ArrayList<>();
            leftIbises = new ArrayList<>();
            diedIbises = new ArrayList<>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
        }

        if (capabilities.hasCapability(IbisCapabilities.SIGNALS) && eventHandler == null) {
            signals = new ArrayList<>();
        } else {
            signals = null;
        }

        if (eventHandler != null) {
            upcaller = new Upcaller(eventHandler);
        } else {
            upcaller = null;
        }

        UUID id = UUID.randomUUID();

        poolName = properties.getProperty(IbisProperties.POOL_NAME);

        if (poolName == null) {
            throw new IbisConfigurationException("cannot initialize registry, property " + IbisProperties.POOL_NAME + " is not specified");
        }

        Location location = Location.defaultLocation(properties, null);

        if (properties.getBooleanProperty(RegistryProperties.STATISTICS)) {
            statistics = new Statistics(Protocol.OPCODE_NAMES);
            statistics.setID(id.toString() + "@" + location.toString(), poolName);

            long interval = properties.getIntProperty(RegistryProperties.STATISTICS_INTERVAL) * 1000;

            statistics.startWriting(interval);
        } else {
            statistics = null;
        }

        members = new MemberSet(properties, this, statistics);
        elections = new ElectionSet(properties, this);

        commHandler = new CommunicationHandler(properties, this, members, elections, statistics);

        identifier = new IbisIdentifier(id.toString(), ibisData, commHandler.getAddress().toBytes(), location, poolName, applicationTag);

        commHandler.start();
        members.start();

        boolean printMembers = properties.getBooleanProperty(RegistryProperties.PRINT_MEMBERS);

        if (printMembers) {
            new MemberPrinter(members);
        }

        ThreadPool.createNew(this, "pool management thread");

        if (logger.isDebugEnabled()) {
            logger.debug("registry for " + identifier + " initiated");
        }
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    CommunicationHandler getCommHandler() {
        return commHandler;
    }

    @Override
    public IbisIdentifier elect(String electionName) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException("No election support requested");
        }

        IbisIdentifier[] candidates = elections.elect(electionName);

        return members.getFirstLiving(candidates);
    }

    @Override
    public IbisIdentifier elect(String electionName, long timeoutMillis) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException("No election support requested");
        }

        IbisIdentifier[] candidates = elections.elect(electionName, timeoutMillis);

        return members.getFirstLiving(candidates);

    }

    @Override
    public IbisIdentifier getElectionResult(String election) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException("No election support requested");
        }

        IbisIdentifier[] candidates = elections.getElectionResult(election);

        return members.getFirstLiving(candidates);

    }

    @Override
    public String[] wonElections() {
        ArrayList<String> result = new ArrayList<>();
        synchronized (elections) {
            for (Election e : elections) {
                if (e.getWinner().equals(identifier)) {
                    result.add(e.getName());
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public IbisIdentifier getElectionResult(String electionName, long timeoutMillis) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)) {
            throw new IbisConfigurationException("No election support requested");
        }

        IbisIdentifier[] candidates = elections.getElectionResult(electionName, timeoutMillis);

        return members.getFirstLiving(candidates);

    }

    @Override
    public void maybeDead(ibis.ipl.IbisIdentifier suspect) throws IOException {
        try {
            members.maybeDead((IbisIdentifier) suspect);
        } catch (ClassCastException e) {
            logger.error("illegal ibis identifier given: " + e);
        }
    }

    @Override
    public void assumeDead(ibis.ipl.IbisIdentifier deceased) throws IOException {
        try {
            members.assumeDead((IbisIdentifier) deceased);
        } catch (ClassCastException e) {
            logger.error("illegal ibis identifier given: " + e);
        }

    }

    @Override
    public void signal(String signal, ibis.ipl.IbisIdentifier... ibisIdentifiers) throws IOException {
        if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            throw new IbisConfigurationException("No string support requested");
        }

        try {
            IbisIdentifier[] implIdentifiers = (IbisIdentifier[]) ibisIdentifiers;

            commHandler.sendSignals(signal, implIdentifiers);

        } catch (ClassCastException e) {
            throw new IOException("wrong type of identifiers given: " + e);
        }
    }

    @Override
    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException("Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result = joinedIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        joinedIbises.clear();
        return result;
    }

    @Override
    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException("Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] result = leftIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        leftIbises.clear();
        return result;
    }

    @Override
    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException("Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] result = diedIbises.toArray(new ibis.ipl.IbisIdentifier[0]);
        diedIbises.clear();
        return result;
    }

    @Override
    public synchronized String[] receivedSignals() {
        if (signals == null) {
            throw new IbisConfigurationException("Registry downcalls not configured");
        }

        String[] result = signals.toArray(new String[0]);
        signals.clear();
        return result;
    }

    @Override
    public int getPoolSize() {
        throw new IbisConfigurationException("getPoolSize not supported by gossip registry");
    }

    @Override
    public synchronized void waitUntilPoolClosed() {
        throw new IbisConfigurationException("waitForAll not supported by gossip registry");

    }

    @Override
    public void enableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to " + "produce events");
        }

        upcaller.enableEvents();
    }

    @Override
    public void disableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to " + "produce events");
        }

        upcaller.disableEvents();
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        throw new IbisConfigurationException("Sequence numbers not supported by" + " gossip registry");
    }

    @Override
    public Map<String, String> managementProperties() {
        // no properties (as of yet)
        return new HashMap<>();
    }

    @Override
    public String getManagementProperty(String key) throws NoSuchPropertyException {
        String result = managementProperties().get(key);

        if (result == null) {
            throw new NoSuchPropertyException(key + " is not a valid property");
        }
        return result;
    }

    @Override
    public void setManagementProperties(Map<String, String> properties) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("gossip registry does not have any properties that can be set");
    }

    @Override
    public void setManagementProperty(String key, String value) throws NoSuchPropertyException {
        throw new NoSuchPropertyException("gossip registry does not have any properties that can be set");
    }

    @Override
    public void printManagementProperties(PrintStream stream) {
        // NOTHING
    }

    // functions called by pool to tell the registry an event has occured

    synchronized void ibisJoined(IbisIdentifier ibis) {
        if (joinedIbises != null) {
            joinedIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisJoined(ibis);
        }
    }

    synchronized void ibisLeft(IbisIdentifier ibis) {
        if (leftIbises != null) {
            leftIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisLeft(ibis);
        }
    }

    synchronized void ibisDied(IbisIdentifier ibis) {
        if (diedIbises != null) {
            diedIbises.add(ibis);
        }

        if (upcaller != null) {
            upcaller.ibisDied(ibis);
        }
    }

    synchronized void signal(String signal, IbisIdentifier source) {
        if (signals != null) {
            signals.add(signal);
        }

        if (upcaller != null) {
            upcaller.signal(signal, source);
        }
    }

    synchronized void electionResult(String name, IbisIdentifier winner) {
        if (upcaller != null) {
            upcaller.electionResult(name, winner);
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public String getPoolName() {
        return poolName;
    }

    @Override
    public void leave() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("leaving: setting stopped state");
        }
        synchronized (this) {
            stopped = true;
            notifyAll();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("leaving: telling pool we are leaving");
        }
        members.leave(identifier);
        members.leave();

        // logger.debug("leaving: broadcasting leave");
        commHandler.broadcastLeave();

        if (logger.isDebugEnabled()) {
            logger.debug("leaving: writing statistics");
        }
        if (statistics != null) {
            statistics.write();
            statistics.end();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("leaving: done!");
        }
    }

    @Override
    public void run() {
        long interval = properties.getIntProperty(RegistryProperties.GOSSIP_INTERVAL) * 1000;

        while (!isStopped()) {
            commHandler.gossip();

            int timeout = (int) (Math.random() * interval);
            synchronized (this) {
                try {
                    wait(timeout);
                } catch (InterruptedException e) {
                    // IGNORE
                }
            }
        }

    }

    @Override
    public boolean hasTerminated() {
        throw new IbisConfigurationException("gossip registry does not support termination");
    }

    @Override
    public boolean isClosed() {
        throw new IbisConfigurationException("gossip registry does not support closed world");
    }

    @Override
    public void terminate() throws IOException {
        throw new IbisConfigurationException("gossip registry does not support termination");
    }

    @Override
    public ibis.ipl.IbisIdentifier waitUntilTerminated() {
        throw new IbisConfigurationException("gossip registry does not support termination");
    }

    @Override
    public IbisIdentifier getRandomPoolMember() {
        Member[] random = members.getRandomMembers(1);

        if (random.length == 1) {
            return random[0].getIdentifier();
        } else {
            return null;
        }
    }

    @Override
    public void addTokens(String name, int count) throws IOException {
        throw new IbisConfigurationException("tokens not supported by gossip registry");
    }

    @Override
    public String getToken(String name) throws IOException {
        throw new IbisConfigurationException("tokens not supported by gossip registry");
    }

}
