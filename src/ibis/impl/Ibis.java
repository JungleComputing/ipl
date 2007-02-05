/* $Id: Ibis.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.ipl.IbisConfigurationException;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ResizeHandler;
import ibis.ipl.StaticProperties;
import ibis.util.GetLogger;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * This implementation of the {@link ibis.ipl.Ibis} interface 
 * is a base class, to be extended by specific Ibis implementations.
 */
public abstract class Ibis implements ibis.ipl.Ibis {

    /** Debugging output. */
    private static final Logger logger = GetLogger.getLogger("ibis.impl.Ibis");

    /** A user-supplied resize handler, with join/leave upcalls. */
    private ResizeHandler resizeHandler;

    /**
     * Properties, as given to
     * {@link ibis.ipl.IbisFactory#createIbis(StaticProperties, ResizeHandler)}.
     */
    protected StaticProperties requiredProps;

    /** User properties, combined with required properties. */
    protected StaticProperties combinedProps;

    /** The Ibis registry. */
    private final Registry registry;

    /** Identifies this Ibis instance in the registry. */
    protected final IbisIdentifier ident;

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

    /**
     * Constructs an <code>Ibis</code> instance with the specified parameters.
     * @param resizeHandler the resizeHandler specified by the caller of
     * {@link ibis.ipl.IbisFactory#createIbis(StaticProperties, ResizeHandler)}.
     * @param requiredProps properties as specified by caller of
     * {@link ibis.ipl.IbisFactory#createIbis(StaticProperties, ResizeHandler)}.
     * @param combinedProps properties that are the result of the combination
     * of <code>requiredProps</code> and the user-specified properties.
     * Every Ibis implementation must have a public constructor with these
     * parameters.
     */
    protected Ibis(ResizeHandler resizeHandler, StaticProperties requiredProps,
            StaticProperties combinedProps) throws IOException {
        this.resizeHandler = resizeHandler;
        this.requiredProps = requiredProps;
        this.combinedProps = combinedProps;
        // TODO: fix props
        String registryName = System.getProperty("ibis.registry.impl");
        if (registryName == null) {
            registryName = "ibis.impl.registry.tcp.NameServerClient";
        }
        registry = Registry.loadRegistry(this, registryName);
        receivePorts = new HashMap<String, ReceivePort>();
        sendPorts = new HashMap<String, SendPort>();
        ident = registry.init(this, resizeHandler != null, getData());
    }

    public Registry registry() {
        return registry;
    }

    public ibis.ipl.IbisIdentifier identifier() {
        return ident;
    }

    public ibis.ipl.PortType createPortType(StaticProperties p)
            throws PortMismatchException {
        if (p == null) {
            p = combinedProps;
        } else {
            /*
             * The properties given as parameter have preference.
             * It is not clear to me if the user properties should have
             * preference here. The user could say that he wants Ibis
             * serialization, but the parameter could say: sun serialization.
             * On the other hand, the parameter could just say: object
             * serialization, in which case the user specification is
             * more specific.
             * The {@link StaticProperties#combine} method should deal
             * with that.
             */
            p = new StaticProperties(combinedProps.combine(p));
            // Select the properties that are significant for the port type.
            StaticProperties portProps = new StaticProperties();
            String prop = p.find("communication");
            if (prop != null) {
                portProps.add("communication", prop);
            }
            prop = p.find("serialization");
            if (prop != null) {
                portProps.add("serialization", prop);
            }
            prop = p.find("serialization.replacer");
            if (prop != null) {
                portProps.addLiteral("serialization.replacer", prop);
            }
            checkPortProperties(portProps);
            p = portProps;
        }
        logger.info("Creating port type" + " with properties\n" + p);
        if (p.isProp("communication", "manytoone") &&
                p.isProp("communication", "onetomany")) {
            logger.warn("Combining ManyToOne and OneToMany in "
                    + "a port type may result in\ndeadlocks! Most systems "
                    + "don't have a working flow control when multiple\n"
                    + "senders do multicasts.");
        }
        return newPortType(p);
    }

    /**
     * This method is used to check if the properties for a PortType
     * match the properties of this Ibis.
     * @param p the properties for the PortType.
     * @exception PortMismatchException is thrown when this Ibis cannot provide
     * the properties requested for the PortType.
     */
    private void checkPortProperties(StaticProperties p)
            throws PortMismatchException {
        if (!p.matchProperties(requiredProps)) {
            logger.error("Ibis required properties: " + requiredProps);
            logger.error("Port required properties: " + p);
            throw new PortMismatchException(
                    "Port properties don't match the Ibis required properties");
        }
    }

    public int totalNrOfIbisesInPool() {
        if (combinedProps.isProp("worldmodel", "closed")) {
            return TypedProperties.intProperty("ibis.pool.total_hosts");
        }
        throw new IbisConfigurationException(
                "totalNrOfIbisesInPool called but open world run");
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

    /**
     * Notifies this Ibis instance that other Ibis instances have
     * joined the run.
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
    }

    /**
     * Notifies this Ibis instance that other Ibis instances have
     * left the run.
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
    }

    /**
     * Notifies this Ibis instance that other Ibis instances have died.
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
     * to leave.
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

    public StaticProperties properties() {
        return ibis.ipl.IbisFactory.staticProperties(this.getClass().getName());
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

    public void printStatistics() { 
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
    // Protected methods, may called by Ibis implementations.
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /**
     * Returns the receiveport with the specified name, or <code>null</code>
     * if not present.
     * @param name the name of the receiveport.
     * @return the receiveport.
     */
    protected synchronized ReceivePort findReceivePort(String name) {
        return receivePorts.get(name);
    }

    /**
     * Returns the sendport with the specified name, or <code>null</code>
     * if not present.
     * @param name the name of the sendport.
     * @return the sendport.
     */
    protected synchronized SendPort findSendPort(String name) {
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
     * See {@link ibis.ipl.Ibis#createPortType(StaticProperties)}.
     */
    protected abstract ibis.ipl.PortType newPortType(StaticProperties p)
            throws PortMismatchException;
}
