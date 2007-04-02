/* $Id$ */

package ibis.impl;

import ibis.impl.registry.RegistryProperties;
import ibis.ipl.CapabilitySet;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.RegistryEventHandler;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Upcall;
import ibis.util.Log;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * This implementation of the {@link ibis.ipl.Ibis} interface 
 * is a base class, to be extended by specific Ibis implementations.
 */
public abstract class Ibis extends Managable implements ibis.ipl.Ibis,
       RegistryEventHandler, ibis.ipl.PredefinedCapabilities {

    /** Debugging output. */
    private static final Logger logger = Logger.getLogger("ibis.impl.Ibis");

    /** A user-supplied registry handler, with join/leave upcalls. */
    protected final RegistryEventHandler registryHandler;

    /**
     * CapabilitySet, as derived from the capabilities passed to
     * {@link ibis.ipl.IbisFactory#createIbis(CapabilitySet, CapabilitySet
     * Properties, RegistryEventHandler)} and the capabilities of this ibis.
     */
    public final CapabilitySet capabilities;

    /**
     * Properties, as given to
     * {@link ibis.ipl.IbisFactory#createIbis(CapabilitySet, CapabilitySet,
     * Properties, RegistryEventHandler)}.
     */
    protected TypedProperties properties;

    /** The Ibis registry. */
    private final Registry registry;

    /** Identifies this Ibis instance in the registry. */
    public final IbisIdentifier ident;

    /** Set when processing a registry upcall. */
    private boolean busyUpcaller = false;

    /** Set when registry upcalls are enabled. */
    private boolean registryUpcallerEnabled = false;

    /** Set when {@link #end()} is called. */
    private boolean ended = false;

    /** The receiveports running on this Ibis instance. */
    private HashMap<String, ReceivePort> receivePorts;

    /** The sendports running on this Ibis instance. */
    private HashMap<String, SendPort> sendPorts;

    private int nJoins;

    private final boolean closedWorld;

    private final int numInstances;

    private final HashSet<ibis.ipl.IbisIdentifier> joinedIbises;

    private final HashSet<ibis.ipl.IbisIdentifier> leftIbises;

    /** Counter for allocating names for anonymous sendports. */
    private static int send_counter = 0;

    /** Counter for allocating names for anonymous receiveports. */
    private static int receive_counter = 0;

    /**
     * Constructs an <code>Ibis</code> instance with the specified parameters.
     * @param registryHandler the registryHandler.
     * @param caps the capabilities.
     * @param userProperties the properties as provided by the Ibis factory.
     * @param defaultProperties the default properties of this particular
     * ibis implementation.
     */
    protected Ibis(RegistryEventHandler registryHandler, CapabilitySet caps,
            Properties userProperties,Properties defaultProperties) {
        closedWorld = caps.hasCapability(WORLDMODEL_CLOSED);
        boolean needsRegistryCalls = registryHandler != null
                || caps.hasCapability(RESIZE_DOWNCALLS)
                || closedWorld;
        this.registryHandler = registryHandler;
        this.capabilities = caps;
        
        Log.initLog4J("ibis");

        this.properties = new TypedProperties();
        
        //bottom up add properties, starting with hard coded ones
        properties.addProperties(IbisProperties.getHardcodedProperties());
        properties.addProperties(RegistryProperties.getHardcodedProperties());
        properties.addProperties(defaultProperties);
        properties.addProperties(IbisProperties.getConfigProperties());
        properties.addProperties(userProperties);
        
        if (logger.isDebugEnabled()) {
            logger.debug("Ibis constructor: properties = " + properties);
        }
        
        receivePorts = new HashMap<String, ReceivePort>();
        sendPorts = new HashMap<String, SendPort>();

        registry = initializeRegistry(needsRegistryCalls ? this : null);

        ident = registry.getIbisIdentifier();
        if (closedWorld) {
            try {
                numInstances = this.properties.getIntProperty(
                        "ibis.pool.total_hosts");
            } catch(NumberFormatException e) {
                throw new IbisConfigurationException("Could not get number of "
                        + "instances", e);
            }
        } else {
            numInstances = -1;
        }
        if (caps.hasCapability(RESIZE_DOWNCALLS)) {
            joinedIbises = new HashSet<ibis.ipl.IbisIdentifier>();
            leftIbises = new HashSet<ibis.ipl.IbisIdentifier>();
        } else {
            joinedIbises = null;
            leftIbises = null;
        }
    }

    protected Registry initializeRegistry(RegistryEventHandler handler) {
        try {
            return Registry.createRegistry(handler, properties, getData());
        } catch(Throwable e) {
            throw new IbisConfigurationException("Coulld not create registry",
                    e);
        }
    }

    public synchronized void waitForAll() {
        if (! closedWorld) {
            throw new IbisConfigurationException("waitForAll() called but not "
                    + "closed world");
        }
        if (registryHandler != null && ! registryUpcallerEnabled) {
            throw new IbisConfigurationException("waitForAll() called but "
                    + "registry events not enabled yet");
        }
        while (nJoins < numInstances) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
        }
    }

    public Registry registry() {
        return registry;
    }

    public ibis.ipl.IbisIdentifier identifier() {
        return ident;
    }

    public Properties properties() {
        return new Properties(properties);
    }

    public int totalNrOfIbisesInPool() {
        if (! closedWorld) {
            throw new IbisConfigurationException(
                "totalNrOfIbisesInPool called but open world run");
        }
        return numInstances;
    }

    private synchronized void waitForEnabled() {
        while (! registryUpcallerEnabled) {
            try {
                wait();
            } catch(Exception e) {
                // ignored
            }
        }
        busyUpcaller = true;
    }

    public synchronized ibis.ipl.IbisIdentifier[] joinedIbises() {
        if (joinedIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = joinedIbises.toArray(
                new ibis.ipl.IbisIdentifier[joinedIbises.size()]);
        joinedIbises.clear();
        return retval;
    }

    public synchronized ibis.ipl.IbisIdentifier[] leftIbises() {
        if (leftIbises == null) {
            throw new IbisConfigurationException(
                    "Resize downcalls not configured");
        }
        ibis.ipl.IbisIdentifier[] retval = leftIbises.toArray(
                new ibis.ipl.IbisIdentifier[leftIbises.size()]);
        leftIbises.clear();
        return retval;
    }

    /**
     * Notifies this Ibis instance that another Ibis instance has
     * joined the run. Called by the registry.
     * @param joinIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier
     * identifier} of the Ibis instance joining the run.
     */
    public void joined(ibis.ipl.IbisIdentifier joinIdent) {
        if (closedWorld) {
            synchronized(this) {
                nJoins++;
                if (nJoins > numInstances) {
                    return;
                }
                if (nJoins == numInstances) {
                    notifyAll();
                }
            }
        }
        if (registryHandler != null) {
            waitForEnabled();
            registryHandler.joined(joinIdent);
            synchronized(this) {
                busyUpcaller = false;
            }
        }
        if (joinedIbises != null) {
            synchronized(this) {
                joinedIbises.add(joinIdent);
             }
        }
    }

    /**
     * Notifies this Ibis instance that another Ibis instance has
     * left the run. Called by the Registry.
     * @param leaveIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifier} of the Ibis instance leaving the run.
     */
    public void left(ibis.ipl.IbisIdentifier leaveIdent) {
        if (registryHandler != null) {
            waitForEnabled();
            registryHandler.left(leaveIdent);
            synchronized(this) {
                busyUpcaller = false;
            }
        }
        if (leftIbises != null) {
            synchronized(this) {
                leftIbises.add(leaveIdent);
            }
        }
    }

    /**
     * Notifies this Ibis instance that another Ibis instance has died.
     * Called by the registry.
     * @param corpse the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifier} of the Ibis instance that died.
     */
    public void died(ibis.ipl.IbisIdentifier corpse) {
        if (registryHandler != null) {
            waitForEnabled();
            registryHandler.died(corpse);
            synchronized(this) {
                busyUpcaller = false;
            }
        }
        if (leftIbises != null) {
            synchronized(this) {
                leftIbises.add(corpse);
            }
        }
    }

    /**
     * Notifies this Ibis instance that some signal arrived.
     * Called by the registry.
     * @param signal the signal.
     */
    public void gotSignal(String signal) {
        if (registryHandler != null) {
            waitForEnabled();
            registryHandler.gotSignal(signal);
            synchronized(this) {
                busyUpcaller = false;
            }
        } 
    }

    public synchronized void enableRegistryEvents() {
        registryUpcallerEnabled = true;
        notifyAll();
    }

    public synchronized void disableRegistryEvents() {
        while (busyUpcaller) {
            try {
                wait();
            } catch(Exception e) {
                // nothing
            }
        }
        registryUpcallerEnabled = false;
    }

    public CapabilitySet capabilities() {
        return capabilities;
    }

    /**
     * Returns the current Ibis version.
     * @return the ibis version.
     */
    public String getVersion() {
        InputStream in
            = ClassLoader.getSystemClassLoader().getResourceAsStream("VERSION");
        String version = "Unknown Ibis Version ID";
        if (in != null) {
            byte[] b = new byte[512];
            int l = 0;
            try {
                l = in.read(b);
            } catch (Exception e) {
                // Ignored
            }
            if (l > 0) {
                version = "Ibis Version ID " + new String(b, 0, l);
            }
        }
        return version + ", implementation = " + this.getClass().getName();
    }

    public void printStatistics(java.io.PrintStream out) { 
        // default is empty
    }

    public void end() {
        synchronized (this) {
            if (ended) {
                return;
            }
            ended = true;
        }
        try {
            registry.leave();
        } catch (Exception e) {
            throw new RuntimeException("Registry: leave failed ", e);
        }
        quit();
    }

    public void poll() {
        // Default has empty implementation.
    }

    synchronized void register(ReceivePort p) {
        if (receivePorts.get(p.name) != null) {
            throw new Error("Multiple instances of receiveport named "
                    + p.name);
        }
        receivePorts.put(p.name, p);
    }

    synchronized void deRegister(ReceivePort p) {
        if (receivePorts.remove(p.name) == null) {
            throw new Error("Trying to remove unknown receiveport");
        }
    }

    synchronized void register(SendPort p) {
        if (sendPorts.get(p.name) != null) {
            throw new Error("Multiple instances of sendport named " + p.name);
        }
        sendPorts.put(p.name, p);
    }

    synchronized void deRegister(SendPort p) {
        if (sendPorts.remove(p.name) == null) {
            throw new Error("Trying to remove unknown sendport");
        }
    }

    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    // Public methods, may called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Returns the receiveport with the specified name, or <code>null</code>
     * if not present.
     * @param name the name of the receiveport.
     * @return the receiveport.
     */
    public synchronized ReceivePort findReceivePort(String name) {
        return receivePorts.get(name);
    }

    /**
     * Returns the sendport with the specified name, or <code>null</code>
     * if not present.
     * @param name the name of the sendport.
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
     * This method should provide the implementation-dependent data of
     * the Ibis identifier for this Ibis instance. This method gets called
     * from the Ibis constructor.
     * @exception IOException may be thrown in case of trouble.
     * @return the implementation-dependent data, as a byte array.
     */
    protected abstract byte[] getData() throws IOException;

    public ibis.ipl.SendPort createSendPort(CapabilitySet tp)
            throws IOException {
        return createSendPort(tp, null, null, false);
    }

    public ibis.ipl.SendPort createSendPort(CapabilitySet tp, String name)
            throws IOException {
        return createSendPort(tp, name, null, false);
    }

    public ibis.ipl.SendPort createSendPort(CapabilitySet tp,
            boolean connectionDowncalls) throws IOException {
        return createSendPort(tp, null, null, connectionDowncalls);
    }

    public ibis.ipl.SendPort createSendPort(CapabilitySet tp, String name,
            boolean connectionDowncalls) throws IOException {
        return createSendPort(tp, name, null, connectionDowncalls);
    }

    public ibis.ipl.SendPort createSendPort(CapabilitySet tp, String name,
            SendPortDisconnectUpcall cU) throws IOException {
        return createSendPort(tp, name, cU, false);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of the specified port type.
     *
     * @param tp the port type.
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortDisconnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Throwable)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    private ibis.ipl.SendPort createSendPort(CapabilitySet tp, String name,
            SendPortDisconnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        if (cU != null) {
            if (! capabilities.hasCapability(CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!capabilities.hasCapability(CONNECTION_DOWNCALLS)) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (name == null) {
            synchronized(this.getClass()) {
                name = "anonymous send port " + send_counter++;
            }
        }

        return doCreateSendPort(tp, name, cU, connectionDowncalls);
    }

    /**
     * Creates a {@link ibis.ipl.SendPort} of the specified port type.
     *
     * @param tp the port type.
     * @param name the name of this sendport.
     * @param cU object implementing the
     * {@link SendPortDisconnectUpcall#lostConnection(ibis.ipl.SendPort,
     * ReceivePortIdentifier, Throwable)} method.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections
     * downcall.
     * @return the new sendport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.SendPort doCreateSendPort(CapabilitySet tp,
            String name, SendPortDisconnectUpcall cU,
            boolean connectionDowncalls) throws IOException;

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name)
            throws IOException {
        return createReceivePort(tp, name, null, null, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            boolean connectionDowncalls) throws IOException {
        return createReceivePort(tp, name, null, null, connectionDowncalls);
    }

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            Upcall u) throws IOException {
        return createReceivePort(tp, name, u, null, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            Upcall u, boolean connectionDowncalls) throws IOException {
        return createReceivePort(tp, name, u, null, connectionDowncalls);
    }

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(tp, name, null, cU, false);
    }

    public ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            Upcall u, ReceivePortConnectUpcall cU) throws IOException {
        return createReceivePort(tp, name, u, cU, false);
    }

    /** 
     * Creates a named {@link ibis.ipl.ReceivePort} of the specified
     * port type, with upcall based communication.
     * New connections will not be accepted until
     * {@link ibis.ipl.ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param tp the port type.
     * @param name the unique name of this receiveport (or <code>null</code>,
     *    in which case the port is created anonymously and is not bound
     *    in the registry).
     * @param u the upcall handler.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     * @exception ibis.ipl.IbisConfigurationException is thrown when the port
     * type does not match what is required here.
     */
    private ibis.ipl.ReceivePort createReceivePort(CapabilitySet tp, String name,
            Upcall u, ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        CapabilitySet p = capabilities;
        if (cU != null) {
            if (!p.hasCapability(CONNECTION_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no connection upcalls requested for this port type");
            }
        }
        if (connectionDowncalls) {
            if (!p.hasCapability(CONNECTION_DOWNCALLS)) {
                throw new IbisConfigurationException(
                        "no connection downcalls requested for this port type");
            }
        }
        if (u != null) {
            if (!p.hasCapability(RECEIVE_AUTO_UPCALLS)
                    && !p.hasCapability(RECEIVE_POLL_UPCALLS)) {
                throw new IbisConfigurationException(
                        "no message upcalls requested for this port type");
            }
        } else {
            if (!p.hasCapability(RECEIVE_EXPLICIT)) {
                throw new IbisConfigurationException(
                        "no explicit receive requested for this port type");
            }
        }
        if (name == null) {
            synchronized(this.getClass()) {
                name = "anonymous receive port " + receive_counter++;
            }
        }

        return doCreateReceivePort(tp, name, u, cU, connectionDowncalls);
    }

    /** 
     * Creates a named {@link ibis.ipl.ReceivePort} of the specified port type,
     * with upcall based communication.
     * New connections will not be accepted until
     * {@link ibis.ipl.ReceivePort#enableConnections()} is invoked.
     * This is done to avoid upcalls during initialization.
     * When a new connection request arrives, or when a connection is lost,
     * a ConnectUpcall is performed.
     *
     * @param tp the port type.
     * @param name the name of this receiveport.
     * @param u the upcall handler.
     * @param cU object implementing <code>gotConnection</code>() and
     * <code>lostConnection</code>() upcalls.
     * @param connectionDowncalls set when this port must keep
     * connection administration to support the lostConnections and
     * newConnections downcalls.
     * @return the new receiveport.
     * @exception java.io.IOException is thrown when the port could not be
     * created.
     */
    protected abstract ibis.ipl.ReceivePort doCreateReceivePort(
            CapabilitySet tp, String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
        throws IOException;
}
