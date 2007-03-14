/* $Id$ */

package ibis.impl;

import ibis.impl.registry.RegistryProperties;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisProperties;
import ibis.ipl.ResizeHandler;
import ibis.ipl.CapabilitySet;
import ibis.ipl.CapabilitySet;
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
public abstract class Ibis implements ibis.ipl.Ibis,
       ibis.ipl.PredefinedCapabilities {

    /** Debugging output. */
    private static final Logger logger = Logger.getLogger("ibis.impl.Ibis");

    /** A user-supplied resize handler, with join/leave upcalls. */
    private ResizeHandler resizeHandler;

    /**
     * CapabilitySet, as derived from the capabilities passed to
     * {@link ibis.ipl.IbisFactory#createIbis(CapabilitySet, CapabilitySet
     * Properties, ResizeHandler)} and the capabilities of this ibis.
     */
    public final CapabilitySet capabilities;

    /**
     * Properties, as given to
     * {@link ibis.ipl.IbisFactory#createIbis(CapabilitySet, CapabilitySet,
     * Properties, ResizeHandler)}.
     */
    protected TypedProperties properties;

    /** The Ibis registry. */
    private final Registry registry;

    /** Identifies this Ibis instance in the registry. */
    public final IbisIdentifier ident;

    /** Set when the registry supplied the join upcall for this instance. */
    private boolean i_joined = false;

    /** Set when processing a registry upcall. */
    private boolean busyUpcaller = false;

    /** Set when registry upcalls are enabled. */
    private boolean resizeUpcallerEnabled = false;

    /** Set when {@link #end()} is called. */
    private boolean ended = false;

    /** The receiveports running on this Ibis instance. */
    private HashMap<String, ReceivePort> receivePorts;

    /** The sendports running on this Ibis instance. */
    private HashMap<String, SendPort> sendPorts;

    private final boolean closedWorld;

    private final int numInstances;

    private final HashSet<ibis.ipl.IbisIdentifier> joinedIbises;

    private final HashSet<ibis.ipl.IbisIdentifier> leftIbises;

    /**
     * Constructs an <code>Ibis</code> instance with the specified parameters.
     * @param resizeHandler the resizeHandler.
     * @param caps the capabilities.
     * @param userProperties the properties as provided by the Ibis factory.
     * @param defaultProperties the default properties of this particular
     * ibis implementation.
     */
    protected Ibis(ResizeHandler resizeHandler, CapabilitySet caps,
            Properties userProperties,Properties defaultProperties)
            throws Throwable {
        boolean needsRegistryCalls = resizeHandler != null
                || caps.hasCapability(RESIZE_DOWNCALLS);
        this.resizeHandler = resizeHandler;
        this.capabilities = caps;
        
        Log.initLog4J("ibis");

        this.properties = new TypedProperties();
        
        //bottom up add properties, starting with hard coded ones
        properties.addProperties(IbisProperties.getHardcodedProperties());
        properties.addProperties(RegistryProperties.getHardcodedProperties());
        properties.addProperties(defaultProperties);
        properties.addProperties(IbisProperties.getConfigProperties());
        properties.addProperties(userProperties);
        
        receivePorts = new HashMap<String, ReceivePort>();
        sendPorts = new HashMap<String, SendPort>();
        registry = Registry.createRegistry(this, needsRegistryCalls,
                getData());
        ident = registry.getIbisIdentifier();
        closedWorld = caps.hasCapability(WORLDMODEL_CLOSED);
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

    public Registry registry() {
        return registry;
    }

    public ibis.ipl.IbisIdentifier identifier() {
        return ident;
    }

    public ibis.ipl.PortType createPortType(CapabilitySet p, Properties tp) {
        if (p == null) {
            p = capabilities;
        } else {
            checkPortCapabilities(p);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Creating port type" + " with capabilities\n" + p);
        }
        if (p.hasCapability(CONNECTION_MANY_TO_ONE) &&
                p.hasCapability(CONNECTION_ONE_TO_MANY)) {
            logger.warn("Combining ManyToOne and OneToMany in "
                    + "a port type may result in\ndeadlocks! Most systems "
                    + "don't have a working flow control when multiple\n"
                    + "senders do multicasts.");
        }

        TypedProperties ttp = new TypedProperties(properties);

        if (tp != null) {
            ttp.addProperties(ttp);
        }

        return newPortType(p, ttp);
    }

    public ibis.ipl.PortType createPortType(CapabilitySet p) {
        return createPortType(p, null);
    }

    public Properties properties() {
        return new Properties(properties);
    }

    /**
     * This method is used to check if the capabilities for a PortType
     * match the capabilities of this Ibis.
     * @param p the capabilities for the PortType.
     * @exception IbisConfigurationException is thrown when this Ibis cannot
     * provide the capabilities requested for the PortType.
     */
    private void checkPortCapabilities(CapabilitySet p) {
        if (!p.matchCapabilities(capabilities)) {
            logger.error("Ibis capabilities: " + capabilities);
            logger.error("Port required capabilities: " + p);
            throw new IbisConfigurationException(
                "Port capabilities don't match the Ibis required capabilities");
        }
    }

    public int totalNrOfIbisesInPool() {
        if (! closedWorld) {
            throw new IbisConfigurationException(
                "totalNrOfIbisesInPool called but open world run");
        }
        return numInstances;
    }

    private synchronized void waitForEnabled() {
        while (! resizeUpcallerEnabled) {
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
     * Notifies this Ibis instance that other Ibis instances have
     * joined the run. Called by the registry.
     * @param joinIdents the Ibis {@linkplain ibis.ipl.IbisIdentifier
     * identifiers} of the Ibis instances joining the run.
     */
    public void joined(ibis.ipl.IbisIdentifier[] joinIdents) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < joinIdents.length; i++) {
                ibis.ipl.IbisIdentifier id = joinIdents[i];
                resizeHandler.joined(id);
                if (id.equals(this.ident)) {
                    synchronized(this) {
                        i_joined = true;
                        notifyAll();
                    }
                }
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
        if (joinedIbises != null) {
            synchronized(this) {
                for (int i = 0; i < joinIdents.length; i++) {
                    joinedIbises.add(joinIdents[i]);
                }
            }
        }
    }

    /**
     * Notifies this Ibis instance that other Ibis instances have
     * left the run. Called by the Registry.
     * @param leaveIdents the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances leaving the run.
     */
    public void left(IbisIdentifier[] leaveIdents) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < leaveIdents.length; i++) {
                IbisIdentifier id = leaveIdents[i];
                resizeHandler.left(id);
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
        if (leftIbises != null) {
            synchronized(this) {
                for (int i = 0; i < leaveIdents.length; i++) {
                    leftIbises.add(leaveIdents[i]);
                }
            }
        }
    }

    /**
     * Notifies this Ibis instance that other Ibis instances have died.
     * Called by the registry.
     * @param corpses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that died.
     */
    public void died(IbisIdentifier[] corpses) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < corpses.length; i++) {
                IbisIdentifier id = corpses[i];
                resizeHandler.died(id);
            }
            synchronized(this) {
                busyUpcaller = false;
            }
        }
    }

    /**
     * Notifies this Ibis instance that some Ibis instances are requested
     * to leave. Called by the registry.
     * @param ibisses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that are requested to leave.
     */
    public void mustLeave(IbisIdentifier[] ibisses) {
        if (resizeHandler != null) {
            waitForEnabled();
            resizeHandler.mustLeave(ibisses);
            synchronized(this) {
                busyUpcaller = false;
            }
        }
    }

    public synchronized void enableResizeUpcalls() {
        resizeUpcallerEnabled = true;
        notifyAll();

        if (resizeHandler != null && !i_joined) {
            while (!i_joined) {
                try {
                    wait();
                } catch (Exception e) {
                    /* ignore */
                }
            }
        }
    }

    public synchronized void enableResizeUpcallsNonBlocking() {
        resizeUpcallerEnabled = true;
        notifyAll();
    }

    public synchronized void disableResizeUpcalls() {
        while (busyUpcaller) {
            try {
                wait();
            } catch(Exception e) {
                // nothing
            }
        }
        resizeUpcallerEnabled = false;
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

    /**
     * See {@link ibis.ipl.Ibis#createPortType(CapabilitySet, Properties)}.
     */
    protected abstract ibis.ipl.PortType newPortType(CapabilitySet p,
            Properties attrib);
}
