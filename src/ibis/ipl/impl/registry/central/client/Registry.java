package ibis.ipl.impl.registry.central.client;

import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.impl.IbisIdentifier;
import ibis.ipl.impl.registry.RemoteException;
import ibis.ipl.impl.registry.central.Event;
import ibis.ipl.impl.registry.central.Protocol;
import ibis.ipl.impl.registry.central.RegistryProperties;
import ibis.ipl.impl.registry.statistics.Statistics;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Central registry.
 */
public final class Registry extends ibis.ipl.impl.Registry {

    private static final Logger logger = Logger.getLogger(Registry.class);

    // A thread that forwards the events to the user event handler
    private final Upcaller upcaller;

    private final Statistics statistics;

    // Handles incoming and outgoing communication with other registries and
    // the server.
    private final CommunicationHandler communicationHandler;

    // client-side representation of the Pool the local Ibis is in.
    private final Pool pool;

    private final IbisIdentifier identifier;

    private final IbisCapabilities capabilities;

    // data structures that the user can poll

    private final ArrayList<ibis.ipl.IbisIdentifier> joinedIbises;

    private final ArrayList<ibis.ipl.IbisIdentifier> leftIbises;

    private final ArrayList<ibis.ipl.IbisIdentifier> diedIbises;

    private final ArrayList<String> signals;

    /**
     * Creates a Central Registry.
     * 
     * @param eventHandler
     *            Registry handler to pass events to.
     * @param userProperties
     *            properties of this registry.
     * @param data
     *            Ibis implementation data to attach to the IbisIdentifier.
     * @param ibisImplementationIdentifier
     *            the identification of this ibis implementation, including
     *            version, class and such. Must be identical for all ibisses in
     *            a single pool.
     * @throws IOException
     *             in case of trouble.
     * @throws IbisConfigurationException
     *             In case invalid properties were given.
     */
    public Registry(IbisCapabilities capabilities,
            RegistryEventHandler eventHandler, Properties userProperties,
            byte[] data, String ibisImplementationIdentifier)
            throws IbisConfigurationException, IOException {
        logger.debug("creating central registry");

        this.capabilities = capabilities;

        TypedProperties properties =
            RegistryProperties.getHardcodedProperties();
        properties.addProperties(userProperties);

        if ((capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_UNRELIABLE) || capabilities.hasCapability(IbisCapabilities.MEMBERSHIP_TOTALLY_ORDERED))
                && eventHandler == null) {
            joinedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            leftIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
            diedIbises = new ArrayList<ibis.ipl.IbisIdentifier>();
        } else {
            joinedIbises = null;
            leftIbises = null;
            diedIbises = null;
        }

        if (capabilities.hasCapability(IbisCapabilities.SIGNALS)
                && eventHandler == null) {
            signals = new ArrayList<String>();
        } else {
            signals = null;
        }

        if (eventHandler != null) {
            upcaller = new Upcaller(eventHandler);
        } else {
            upcaller = null;
        }

        if (properties.getBooleanProperty(RegistryProperties.STATISTICS)) {
            statistics = new Statistics(Protocol.OPCODE_NAMES);
            logger.debug("statistics: on");
        } else {
            statistics = null;
            logger.debug("statistics: off");
        }

        pool = new Pool(capabilities, properties, this, statistics);

        try {

            communicationHandler =
                new CommunicationHandler(properties, pool, statistics);

            identifier =
                communicationHandler.join(data, ibisImplementationIdentifier);

            communicationHandler.bootstrap();

        } catch (RemoteException e) {
            // error caused by server "complaining"
            throw new IbisConfigurationException(e.getMessage());
        }

        //start writing statistics
        if (statistics != null) {
            statistics.setID(identifier.getID(), pool.getName());
            statistics.startWriting(properties.getIntProperty(RegistryProperties.STATISTICS_INTERVAL) * 1000);
        }

        logger.debug("registry for " + identifier + " initiated");
    }

    @Override
    public IbisIdentifier getIbisIdentifier() {
        return identifier;
    }

    public IbisIdentifier elect(String electionName) throws IOException {
        return elect(electionName, 0);

    }

    public IbisIdentifier elect(String electionName, long timeoutMillis)
            throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do election, registry already stopped");
        }

        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)
                && !capabilities.hasCapability(IbisCapabilities.ELECTIONS_STRICT)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        IbisIdentifier result = pool.getElectionResult(electionName, -1);

        if (result == null) {
            result = communicationHandler.elect(electionName, timeoutMillis);
        }

        return result;
    }

    public IbisIdentifier getElectionResult(String election) throws IOException {
        return getElectionResult(election, 0);
    }

    public IbisIdentifier getElectionResult(String electionName,
            long timeoutMillis) throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do getElectionResult, registry already stopped");
        }

        if (!capabilities.hasCapability(IbisCapabilities.ELECTIONS_UNRELIABLE)
                && !capabilities.hasCapability(IbisCapabilities.ELECTIONS_STRICT)) {
            throw new IbisConfigurationException(
                    "No election support requested");
        }

        logger.debug("getting election result for: \"" + electionName + "\"");

        return pool.getElectionResult(electionName, timeoutMillis);
    }

    public void maybeDead(ibis.ipl.IbisIdentifier ibisIdentifier)
            throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do maybeDead, registry already stopped");
        }

        communicationHandler.maybeDead(ibisIdentifier);
    }

    public void assumeDead(ibis.ipl.IbisIdentifier ibisIdentifier)
            throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot do assumeDead, registry already stopped");
        }

        communicationHandler.assumeDead(ibisIdentifier);
    }

    public void signal(String signal,
            ibis.ipl.IbisIdentifier... ibisIdentifiers) throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot send signals, registry already stopped");
        }

        if (!capabilities.hasCapability(IbisCapabilities.SIGNALS)) {
            throw new IbisConfigurationException("No string support requested");
        }

        logger.debug("telling " + ibisIdentifiers.length
                + " ibisses a string: " + signal);

        communicationHandler.signal(signal, ibisIdentifiers);
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] retval =
            joinedIbises.toArray(new ibis.ipl.IbisIdentifier[joinedIbises.size()]);
        joinedIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval =
            leftIbises.toArray(new ibis.ipl.IbisIdentifier[leftIbises.size()]);
        leftIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] diedIbises() {
        if (diedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }

        ibis.ipl.IbisIdentifier[] retval =
            diedIbises.toArray(new ibis.ipl.IbisIdentifier[diedIbises.size()]);
        diedIbises.clear();
        return retval;
    }

    public synchronized String[] receivedSignals() {
        if (signals == null) {
            throw new IbisConfigurationException(
                    "Registry downcalls not configured");
        }

        String[] retval = signals.toArray(new String[signals.size()]);
        signals.clear();
        return retval;
    }

    public int getPoolSize() {
        if (!pool.isClosedWorld()) {
            throw new IbisConfigurationException(
                    "getPoolSize called but open world run");
        }

        return pool.getSize();
    }

    public void waitUntilPoolClosed() {
        if (!pool.isClosedWorld()) {
            throw new IbisConfigurationException(
                    "waitForAll called but open world run");
        }

        pool.waitUntilPoolClosed();
    }

    public void enableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.enableEvents();
    }

    public void disableEvents() {
        if (upcaller == null) {
            throw new IbisConfigurationException("Registry not configured to "
                    + "produce events");
        }

        upcaller.disableEvents();
    }

    @Override
    public long getSequenceNumber(String name) throws IOException {
        if (pool.isStopped()) {
            throw new IOException(
                    "cannot send signals, registry already stopped");
        }

        return communicationHandler.getSeqno(name);
    }

    @Override
    public void leave() throws IOException {
        if (pool.isStopped()) {
            throw new IOException("cannot leave, registry already stopped");
        }

        communicationHandler.leave();
        
        if (statistics != null) {
            statistics.write();
            statistics.end();
        }
    }

    /**
     * Handles incoming user events.
     */
    synchronized void handleEvent(Event event) {
        logger.debug("new event passed to user: " + event);

        // generate an upcall for this event
        if (upcaller != null) {
            upcaller.newEvent(event);
        }

        switch (event.getType()) {
        case Event.JOIN:
            if (joinedIbises != null) {
                joinedIbises.add(event.getFirstIbis());
            }
            break;
        case Event.LEAVE:
            if (leftIbises != null) {
                leftIbises.add(event.getFirstIbis());
            }
            break;
        case Event.DIED:
            if (leftIbises != null) {
                leftIbises.add(event.getFirstIbis());
            }
            break;
        case Event.SIGNAL:
            if (signals != null) {
                // see if this string is send to us.
                for (IbisIdentifier ibis : event.getIbises()) {
                    if (ibis.equals(identifier)) {
                        signals.add(event.getDescription());
                        break;
                    }
                }
            }
            break;
        case Event.ELECT:
        case Event.UN_ELECT:
        case Event.POOL_CLOSED:
            // NOT HANDLED HERE
            break;
        default:
            logger.error("unknown event type in registry: " + event);
        }
    }

    public Map<String, String> managementProperties() {
        // TODO: add some statistics
        return new HashMap<String, String>();
    }

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        String result = managementProperties().get(key);

        if (result == null) {
            throw new NoSuchPropertyException(key + " is not a valid property");
        }
        return result;
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "central registry does not have any properties that can be set");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        throw new NoSuchPropertyException(
                "central registry does not have any properties that can be set");
    }
    
    public void printManagementProperties(PrintStream stream) {
        //NOTHING
    }
}
