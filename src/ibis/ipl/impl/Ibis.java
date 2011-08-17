/* $Id$ */

package ibis.ipl.impl;

import ibis.io.IbisIOException;
import ibis.ipl.Credentials;
import ibis.ipl.IbisCapabilities;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisCreationFailedException;
import ibis.ipl.IbisFactory;
import ibis.ipl.IbisProperties;
import ibis.ipl.IbisStarter;
import ibis.ipl.MessageUpcall;
import ibis.ipl.NoSuchPropertyException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.registry.Registry;
import ibis.ipl.support.management.ManagementClient;
import ibis.ipl.support.vivaldi.Coordinates;
import ibis.ipl.support.vivaldi.VivaldiClient;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation of the {@link ibis.ipl.Ibis} interface is a base class,
 * to be extended by specific Ibis implementations.
 */
public abstract class Ibis implements ibis.ipl.Ibis // , IbisMBean
{
    // property to uniquely identify an Ibis locally, even when it has not
    // joined the registry yet
    public static final String ID_PROPERTY = "ibis.local.id";

    /** Debugging output. */
    private static final Logger logger = LoggerFactory
            .getLogger("ibis.ipl.impl.Ibis");

    /** The IbisCapabilities as specified by the user. */
    private final IbisCapabilities capabilities;

    /** List of port types given by the user */
    private final PortType[] portTypes;

    private final IbisStarter starter;

    /**
     * Properties, as given to
     * {@link ibis.ipl.IbisFactory#createIbis(IbisCapabilities, Properties, boolean, RegistryEventHandler, PortType...)}
     * .
     */
    protected TypedProperties properties;

    /** The Ibis registry. */
    private final Registry registry;

    /** Management Client */
    private final ManagementClient managementClient;

    /** Vivaldi Client */
    private final VivaldiClient vivaldiClient;

    /** Identifies this Ibis instance in the registry. */
    public final IbisIdentifier ident;

    /** Set when {@link #end()} is called. */
    private boolean ended = false;

    /** The receiveports running on this Ibis instance. */
    private HashMap<String, ReceivePort> receivePorts;

    /** The sendports running on this Ibis instance. */
    private HashMap<String, SendPort> sendPorts;
    
    private HashMap<ibis.ipl.IbisIdentifier, Long> sentBytesPerIbis = null;
    
    private HashMap<ibis.ipl.IbisIdentifier, Long> receivedBytesPerIbis = null;

    /** Counter for allocating names for anonymous sendports. */
    private static int send_counter = 0;

    /** Counter for allocating names for anonymous receiveports. */
    private static int receive_counter = 0;

    /** Total number of messages send by closed send ports */
    private long outgoingMessageCount = 0;

    /** Total number of messages received by closed receive ports */
    private long incomingMessageCount = 0;

    /** Total number of bytes written to messages closed send ports */
    private long bytesWritten = 0;

    /** Total number of bytes send by closed send ports */
    private long bytesSent = 0;

    /** Total number of bytes read by closed receive ports */
    private long bytesReceived = 0;

    /** Total number of bytes read from messages (for closed received ports) */
    private long bytesRead = 0;

    /**
     * Version, consisting of both the generic implementation version, and the
     * "actual" implementation version.
     */
    private String getImplementationVersion() throws Exception {
        String genericVersion = Ibis.class.getPackage()
                .getImplementationVersion();

        // --Roelof on android the implementation version from the manifest gets
        // overwritten with a default implementation version of "0.0". This is
        // not the value we're searching for.
        if (genericVersion == null || genericVersion.equals("0.0")) {
            // try to get version from IPL_MANIFEST properties
            genericVersion = IbisFactory
                    .getManifestProperty("implementation.version");
        }

        logger.debug("Version of Generic Ibis = " + genericVersion);

        if (genericVersion == null
                || starter.getImplementationVersion() == null) {
            throw new Exception("cannot get version for ibis");
        }

        return genericVersion + starter.getImplementationVersion();
    }

    /**
     * Constructs an <code>Ibis</code> instance with the specified parameters.
     * 
     * @param registryHandler
     *            the registryHandler.
     * @param capabilities
     *            the capabilities.
     * @param applicationTag
     *            an application level tag for this Ibis instance
     * @param portTypes
     *            the port types requested for this ibis implementation.
     * @param userProperties
     *            the properties as provided by the Ibis factory.
     */
    protected Ibis(RegistryEventHandler registryHandler,
            IbisCapabilities capabilities, Credentials credentials,
            byte[] applicationTag, PortType[] portTypes,
            Properties userProperties, IbisStarter starter)
            throws IbisCreationFailedException {

        if (capabilities == null) {
            throw new IbisConfigurationException("capabilities not specified");
        }

        this.capabilities = capabilities;
        this.portTypes = portTypes;
        this.starter = starter;

        this.properties = new TypedProperties();

        // bottom up add properties, starting with hard coded ones
        properties.addProperties(IbisProperties.getHardcodedProperties());
        properties.addProperties(userProperties);

        // set unique ID for this Ibis.
        properties.setProperty(ID_PROPERTY, UUID.randomUUID().toString());

        if (logger.isDebugEnabled()) {
            logger.debug("Ibis constructor: properties = " + properties);
        }

        receivePorts = new HashMap<String, ReceivePort>();
        sendPorts = new HashMap<String, SendPort>();

        if (registryHandler != null) {
            // Only install wrapper if user actually has an event handler.
            // Otherwise, registry downcalls won't work. There needs to be another
            // way to let an Ibis know of died Ibises. --Ceriel
            registryHandler = new RegistryEventHandlerWrapper(registryHandler, this);
        }
        try {
            registry = Registry.createRegistry(this.capabilities,
                    registryHandler, properties, getData(),
                    getImplementationVersion(), applicationTag, credentials);
        } catch (IbisConfigurationException e) {
            throw e;
        } catch (Throwable e) {
            throw new IbisCreationFailedException("Could not create registry",
                    e);
        }

        ident = registry.getIbisIdentifier();

        if (properties.getBooleanProperty("ibis.vivaldi")) {
            try {
                vivaldiClient = new VivaldiClient(properties, registry);
            } catch (Exception e) {
                throw new IbisCreationFailedException(
                        "Could not create vivaldi client", e);
            }
        } else {
            vivaldiClient = null;
        }
        
        if (properties.getBooleanProperty("ibis.bytescount")) {
            sentBytesPerIbis = new HashMap<ibis.ipl.IbisIdentifier, Long>();
            receivedBytesPerIbis = new HashMap<ibis.ipl.IbisIdentifier, Long>();
        }

        if (properties.getBooleanProperty("ibis.managementclient")) {
            try {
                managementClient = new ManagementClient(properties, this);
            } catch (Throwable e) {
                throw new IbisCreationFailedException(
                        "Could not create management client", e);
            }
        } else {
            managementClient = null;
        }

        /*
         * // add bean to JMX try { MBeanServer mbs =
         * ManagementFactory.getPlatformMBeanServer(); ObjectName name = new
         * ObjectName("ibis.ipl.impl:type=Ibis"); mbs.registerMBean(this, name);
         * } catch (Exception e) { logger.warn("cannot registry MBean", e); }
         */
    }
    
    void died(ibis.ipl.IbisIdentifier corpse) {
	killConnections(corpse);
    }
    
    void left(ibis.ipl.IbisIdentifier leftIbis) {
	killConnections(leftIbis);
    }
    
    protected void killConnections(ibis.ipl.IbisIdentifier corpse) {
	SendPort[] sps;
	ReceivePort[] rps;
    
	synchronized(this) {
	    sps = sendPorts.values().toArray(new SendPort[sendPorts.size()]);
	    rps = receivePorts.values().toArray(new ReceivePort[receivePorts.size()]);
	}
	for (SendPort s : sps) {
	    try {
		s.killConnectionsWith(corpse);
	    } catch (Throwable e) {
		if (logger.isDebugEnabled()) {
		    logger.debug("Got exception from killConnectionsWith", e);
		    
		}
	    }
	}
	for (ReceivePort p : rps) {
	    try {
		p.killConnectionsWith(corpse);
	    } catch (Throwable e) {
		if (logger.isDebugEnabled()) {
		    logger.debug("Got exception from killConnectionsWith", e);
		    
		}
	    }
	}
    }

    /**
     * Returns the current Ibis version.
     * 
     * @return the ibis version.
     */
    public String getVersion() {
        return starter.getNickName() + "-" + starter.getIplVersion();
    }

    public ibis.ipl.Registry registry() {
        return registry;
    }

    public ibis.ipl.IbisIdentifier identifier() {
        return ident;
    }

    public Properties properties() {
        return new Properties(properties);
    }

    public void end() throws IOException {
        synchronized (this) {
            if (ended) {
                return;
            }
            ended = true;
        }

        try {
            registry.leave();
        } catch (Throwable e) {
            throw new IbisIOException("Registry: leave failed ", e);
        }

        if (managementClient != null) {
            managementClient.end();
        }

        if (vivaldiClient != null) {
            vivaldiClient.end();
        }

        quit();
    }

    public void poll() throws IOException {
        // Default has empty implementation.
    }

    synchronized void register(ReceivePort p) throws IOException {
        if (receivePorts.get(p.name) != null) {
            throw new IOException("Multiple instances of receiveport named "
                    + p.name);
        }
        receivePorts.put(p.name, p);
    }

    synchronized void deRegister(ReceivePort p) {
        if (receivePorts.remove(p.name) != null) {
            // add statistics for this receive port to "total" statistics
            incomingMessageCount += p.getMessageCount();
            bytesReceived += p.getBytesReceived();
            bytesRead += p.getBytesRead();
        }
    }

    synchronized void register(SendPort p) throws IOException {
        if (sendPorts.get(p.name) != null) {
            throw new IOException("Multiple instances of sendport named "
                    + p.name);
        }
        sendPorts.put(p.name, p);
    }

    synchronized void deRegister(SendPort p) {
        if (sendPorts.remove(p.name) != null) {
            // add statistics for this sendport to "total" statistics
            outgoingMessageCount += p.getMessageCount();
            bytesSent += p.getBytesSent();
            bytesWritten += p.getBytesWritten();
        }
    }
    
    synchronized void addSentPerIbis(long cnt, ibis.ipl.ReceivePortIdentifier[] idents) {
        if (sentBytesPerIbis == null) {
            return;
        }
        for (ibis.ipl.ReceivePortIdentifier rp : idents) {
            ibis.ipl.IbisIdentifier i = rp.ibisIdentifier();
            Long oldval = sentBytesPerIbis.get(i);
            if (oldval != null) {
                cnt += oldval.longValue();
            }
            sentBytesPerIbis.put(i, new Long(cnt));
        }
    }
    
    
    synchronized void addReceivedPerIbis(long cnt, ibis.ipl.SendPortIdentifier[] idents) {
        if (receivedBytesPerIbis == null) {
            return;
        }
        for (ibis.ipl.SendPortIdentifier sp : idents) {
            ibis.ipl.IbisIdentifier i = sp.ibisIdentifier();
            Long oldval = receivedBytesPerIbis.get(i);
            if (oldval != null) {
                cnt += oldval.longValue();
            }
            receivedBytesPerIbis.put(i, new Long(cnt));
        }
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Public methods, may called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Returns the receiveport with the specified name, or <code>null</code> if
     * not present.
     * 
     * @param name
     *            the name of the receiveport.
     * @return the receiveport.
     */
    public synchronized ReceivePort findReceivePort(String name) {
        return receivePorts.get(name);
    }

    /**
     * Returns the sendport with the specified name, or <code>null</code> if not
     * present.
     * 
     * @param name
     *            the name of the sendport.
     * @return the sendport.
     */
    public synchronized SendPort findSendPort(String name) {
        return sendPorts.get(name);
    }

    public ReceivePortIdentifier createReceivePortIdentifier(String name,
            IbisIdentifier id) {
        return new ReceivePortIdentifier(name, id);
    }

    public SendPortIdentifier createSendPortIdentifier(String name,
            IbisIdentifier id) {
        return new SendPortIdentifier(name, id);
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected methods, to be implemented by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Implementation-dependent part of the {@link #end()} implementation.
     */
    protected abstract void quit();

    /**
     * This method should provide the implementation-dependent data of the Ibis
     * identifier for this Ibis instance. This method gets called from the Ibis
     * constructor.
     * 
     * @exception IOException
     *                may be thrown in case of trouble.
     * @return the implementation-dependent data, as a byte array.
     */
    protected abstract byte[] getData() throws IOException;

    public ibis.ipl.SendPort createSendPort(PortType tp) throws IOException {
        return createSendPort(tp, null, null, null);
    }

    public ibis.ipl.SendPort createSendPort(PortType tp, String name)
            throws IOException {
        return createSendPort(tp, name, null, null);
    }

    private void matchPortType(PortType tp) {
        boolean matched = false;
        for (PortType p : portTypes) {
            if (tp.equals(p)) {
                matched = true;
            }
        }
        if (!matched) {
            throw new IbisConfigurationException("PortType \"" + tp
                    + "\" not specified when creating this Ibis instance");
        }
    }

    public ibis.ipl.SendPort createSendPort(PortType tp, String name,
            SendPortDisconnectUpcall cU, Properties properties)
            throws IOException {

        if (tp.hasCapability(PortType.CONNECTION_ULTRALIGHT)) {
            if (tp.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "Ultralight connections to not support connection upcalls");
            }

            if (tp.hasCapability(PortType.COMMUNICATION_RELIABLE)) {
                throw new IbisConfigurationException(
                        "Ultralight connections do not support reliability");
            }

            if (tp.hasCapability(PortType.COMMUNICATION_FIFO)) {
                throw new IbisConfigurationException(
                        "Ultralight connections do not support FIFO message ordering");
            }
        }

        if (cU != null) {
            if (!tp.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (name == null) {
            synchronized (this.getClass()) {
                name = "anonymous send port " + send_counter++;
            }
        }

        matchPortType(tp);

        return doCreateSendPort(tp, name, cU, properties);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of the specified port type.
     * 
     * @param tp
     *            the port type.
     * @param name
     *            the name of this sendport.
     * @param cU
     *            object implementing the
     *            {@link SendPortDisconnectUpcall#lostConnection(ibis.ipl.SendPort, ReceivePortIdentifier, Throwable)}
     *            method.
     * @param properties
     *            the port properties.
     * @return the new sendport.
     * @exception java.io.IOException
     *                is thrown when the port could not be created.
     */
    protected abstract ibis.ipl.SendPort doCreateSendPort(PortType tp,
            String name, SendPortDisconnectUpcall cU, Properties properties)
            throws IOException;

    public ibis.ipl.ReceivePort createReceivePort(PortType tp, String name)
            throws IOException {
        return createReceivePort(tp, name, null, null, null);
    }

    public ibis.ipl.ReceivePort createReceivePort(PortType tp, String name,
            MessageUpcall u) throws IOException {
        return createReceivePort(tp, name, u, null, null);
    }

    public ibis.ipl.ReceivePort createReceivePort(PortType tp, String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(tp, name, null, cU, null);
    }

    public ibis.ipl.ReceivePort createReceivePort(PortType tp, String name,
            MessageUpcall u, ReceivePortConnectUpcall cU, Properties properties)
            throws IOException {

        if (tp.hasCapability(PortType.CONNECTION_ULTRALIGHT)) {
            if (tp.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "Ultralight connections to not support connection upcalls");
            }

            if (tp.hasCapability(PortType.COMMUNICATION_RELIABLE)) {
                throw new IbisConfigurationException(
                        "Ultralight connections do not support reliability");
            }

            if (tp.hasCapability(PortType.COMMUNICATION_FIFO)) {
                throw new IbisConfigurationException(
                        "Ultralight connections do not support FIFO message ordering");
            }
        }

        if (cU != null) {
            if (!tp.hasCapability(PortType.CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (u != null) {
            if (!tp.hasCapability(PortType.RECEIVE_AUTO_UPCALLS)
                    && !tp.hasCapability(PortType.RECEIVE_POLL_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no message upcalls requested for this port type");
            }
        } else {
            if (!tp.hasCapability(PortType.RECEIVE_EXPLICIT)) {
                throw new IbisConfigurationException(
                        "no explicit receive requested for this port type");
            }
        }
        if (name == null) {
            synchronized (this.getClass()) {
                name = "anonymous receive port " + receive_counter++;
            }
        }
        matchPortType(tp);
        return doCreateReceivePort(tp, name, u, cU, properties);
    }

    /**
     * Creates a named {@link ibis.ipl.ReceivePort} of the specified port type,
     * with upcall based communication. New connections will not be accepted
     * until {@link ibis.ipl.ReceivePort#enableConnections()} is invoked. This
     * is done to avoid upcalls during initialization. When a new connection
     * request arrives, or when a connection is lost, a ConnectUpcall is
     * performed.
     * 
     * @param tp
     *            the port type.
     * @param name
     *            the name of this receiveport.
     * @param u
     *            the upcall handler.
     * @param cU
     *            object implementing <code>gotConnection</code>() and
     *            <code>lostConnection</code>() upcalls.
     * @param properties
     *            the port properties.
     * @return the new receiveport.
     * @exception java.io.IOException
     *                is thrown when the port could not be created.
     */
    protected abstract ibis.ipl.ReceivePort doCreateReceivePort(PortType tp,
            String name, MessageUpcall u, ReceivePortConnectUpcall cU,
            Properties properties) throws IOException;

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Protected management methods, can be overriden/used in implementations
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    public String getManagementProperty(String key)
            throws NoSuchPropertyException {
        String result = managementProperties().get(key);

        if (result == null) {
            throw new NoSuchPropertyException("property \"" + key
                    + "\" not found");
        }

        return result;
    }

    public synchronized long getOutgoingMessageCount() {
        long outgoingMessageCount = this.outgoingMessageCount;

        // also add numbers for current send ports
        for (SendPort sendPort : sendPorts.values()) {
            outgoingMessageCount += sendPort.getMessageCount();
        }

        return outgoingMessageCount;
    }

    public synchronized long getBytesSent() {
        long bytesSend = this.bytesSent;

        // also add numbers for current send ports
        for (SendPort sendPort : sendPorts.values()) {
            bytesSend += sendPort.getBytesSent();
        }

        return bytesSend;
    }

    public synchronized long getBytesWritten() {
        long bytesWritten = this.bytesWritten;

        // also add numbers for current send ports
        for (SendPort sendPort : sendPorts.values()) {
            bytesWritten += sendPort.getBytesWritten();
        }

        return bytesWritten;
    }

    public synchronized long getIncomingMessageCount() {
        long incomingMessageCount = this.incomingMessageCount;

        // also add numbers for current receive ports
        for (ReceivePort receivePort : receivePorts.values()) {
            incomingMessageCount += receivePort.getMessageCount();
        }
        return incomingMessageCount;
    }

    public synchronized long getBytesReceived() {
        long bytesReceived = this.bytesReceived;
        // also add numbers for current receive ports
        for (ReceivePort receivePort : receivePorts.values()) {
            bytesReceived += receivePort.getBytesReceived();
        }

        return bytesReceived;
    }

    public synchronized long getBytesRead() {
        long bytesRead = this.bytesRead;
        // also add numbers for current receive ports
        for (ReceivePort receivePort : receivePorts.values()) {
            bytesRead += receivePort.getBytesReceived();
        }

        return bytesRead;
    }

    /**
     * @ibis.experimental
     */
    public synchronized ibis.ipl.IbisIdentifier[] connectedTo() {
        HashSet<ibis.ipl.IbisIdentifier> result = new HashSet<ibis.ipl.IbisIdentifier>();

        Collection<SendPort> ports = sendPorts.values();

        for (SendPort sendPort : ports) {
            ibis.ipl.ReceivePortIdentifier[] receivePorts = sendPort
                    .connectedTo();

            for (ibis.ipl.ReceivePortIdentifier receivePort : receivePorts) {
                result.add(receivePort.ibisIdentifier());
            }

        }
        return result.toArray(new ibis.ipl.IbisIdentifier[0]);
    }

    /**
     * @ibis.experimental
     */
    public Coordinates getVivaldiCoordinates() {
        if (vivaldiClient == null) {
            return null;
        }

        return vivaldiClient.getCoordinates();
    }
    
    /**
     * @ibis.experimental
     */
    public synchronized Map<ibis.ipl.IbisIdentifier, Long> getSentBytesPerIbis() {
        if (sentBytesPerIbis == null) {
            return null;
        }
        return new HashMap<ibis.ipl.IbisIdentifier, Long>(sentBytesPerIbis);
    }
    
    /**
     * @ibis.experimental
     */
    public synchronized Map<ibis.ipl.IbisIdentifier, Long> getReceivedBytesPerIbis() {
        if (receivedBytesPerIbis == null) {
            return null;
        }
        return new HashMap<ibis.ipl.IbisIdentifier, Long>(receivedBytesPerIbis);
    }
    
    /**
     * @ibis.experimental
     */
    public String[] wonElections() {
        return registry.wonElections();
    }
        
    /**
     * @ibis.experimental
     */
    public synchronized Map<ibis.ipl.IbisIdentifier, Set<String>> getReceiverConnectionTypes() {
        Map<ibis.ipl.IbisIdentifier, Set<String>> result = new HashMap<ibis.ipl.IbisIdentifier, Set<String>>();
        for (ReceivePort port : receivePorts.values()) {
            Map<IbisIdentifier, Set<String>> p = port.getConnectionTypes();
            for (Entry<IbisIdentifier, Set<String>> entry : p.entrySet()) {
                Set<String> r = result.get(entry.getKey());
                if (r == null) {
                    r = new HashSet<String>();
                }
                r.addAll(entry.getValue());
                result.put(entry.getKey(), r);
            }
        }       
        return result;
    }
    
    /**
     * @ibis.experimental
     */
    public synchronized Map<ibis.ipl.IbisIdentifier, Set<String>> getSenderConnectionTypes() {
        Map<ibis.ipl.IbisIdentifier, Set<String>> result = new HashMap<ibis.ipl.IbisIdentifier, Set<String>>();
        for (SendPort port : sendPorts.values()) {
            Map<IbisIdentifier, Set<String>> p = port.getConnectionTypes();
            for (Entry<IbisIdentifier, Set<String>> entry : p.entrySet()) {
                Set<String> r = result.get(entry.getKey());
                if (r == null) {
                    r = new HashSet<String>();
                }
                r.addAll(entry.getValue());
                result.put(entry.getKey(), r);
            }
        }        
        return result;
    }

    public synchronized Map<String, String> managementProperties() {
        Map<String, String> result = new HashMap<String, String>();

        // put gathered statistics in the map
        result.put("outgoingMessageCount", "" + getOutgoingMessageCount());
        result.put("bytesWritten", "" + getBytesWritten());
        result.put("bytesSent", "" + getBytesSent());
        result.put("incomingMessageCount", "" + getIncomingMessageCount());
        result.put("bytesReceived", "" + getBytesReceived());
        result.put("bytesRead", "" + getBytesRead());

        return result;
    }

    public void printManagementProperties(PrintStream stream) {
        stream.format("Messages Sent: %d\n", getOutgoingMessageCount());

        double mbWritten = getBytesWritten() / 1024.0 / 1024.0;
        stream.format("Data written to messages: %.2f Mb\n", mbWritten);

        double mbSent = getBytesSent() / 1024.0 / 1024.0;
        stream.format("Data sent out on network: %.2f Mb\n", mbSent);

        stream.format("Messages Received: %d\n", getIncomingMessageCount());

        double mbReceived = getBytesReceived() / 1024.0 / 1024.0;
        stream.format("Data received from network: %.2f Mb\n", mbReceived);

        double mbRead = getBytesRead() / 1024.0 / 1024.0;
        stream.format("Data read from messages: %.2f Mb\n", mbRead);

        stream.flush();
    }

    public void setManagementProperties(Map<String, String> properties)
            throws NoSuchPropertyException {
        // override if an Ibis _can_ set properties
        throw new NoSuchPropertyException("cannot set any properties");
    }

    public void setManagementProperty(String key, String value)
            throws NoSuchPropertyException {
        // override if an Ibis _can_ set properties
        throw new NoSuchPropertyException("cannot set any properties");
    }

    // jmx function
    public String getIdentifier() {
        return ident.toString();
    }
}
