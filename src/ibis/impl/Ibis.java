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
 * Base class for Ibis implementations. All Ibis implementations must
 * extend this class.
 */

public abstract class Ibis implements ibis.ipl.Ibis {

    private static final Logger logger = GetLogger.getLogger("ibis.impl.Ibis");

    /** A user-supplied resize handler, with join/leave upcalls. */
    private ResizeHandler resizeHandler;

    /**
     * Properties, as given to
     * {@link #createIbis(StaticProperties, ResizeHandler)}
     */
    protected StaticProperties requiredProps;

    /** User properties, combined with required properties. */
    protected StaticProperties combinedProps;

    private Registry registry;

    public final IbisIdentifier ident;

    private boolean i_joined = false;

    private boolean busyUpcaller = false;

    private boolean resizeUpcallerEnabled = false;

    private boolean ended = false;

    private HashMap<String, ReceivePort> receivePorts;

    /**
     * Initializes the fields of this class with the specified values.
     * @param resizeHandler the resizeHandler specified by the caller
     * of {@link ibis.ipl.IbisFactory#createIbis()}.
     * @param requiredProps properties as specified by caller of
     * {@link ibis.ipl.IbisFactory#createIbis()}.
     * @param combinedProps properties that are the result of the combination
     * of <code>requiredProps</code> and the user-specified properties.
     * Every Ibis implementation must have a public constructor with these
     * parameters.
     */
    public Ibis(ResizeHandler resizeHandler, StaticProperties requiredProps,
            StaticProperties combinedProps) throws IOException {
        this.resizeHandler = resizeHandler;
        this.requiredProps = requiredProps;
        this.combinedProps = combinedProps;
        registry = ibis.impl.Registry.loadRegistry(this);
        receivePorts = new HashMap<String, ReceivePort>();
        ident = registry.init(this, resizeHandler != null, getData());
    }

    protected abstract byte[] getData() throws IOException;

    public long getSeqno(String nm) throws IOException {
        return registry.getSeqno(nm);
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
            logger.warn("WARNING: combining ManyToOne and OneToMany in "
                    + "a port type may result in\ndeadlocks! Most systems "
                    + "don't have a working flow control when multiple\n"
                    + "senders do multicasts.");
        }
        return newPortType(p);
    }

    /**
     * See {@link ibis.ipl.Ibis#createPortType(StaticProperties)}.
     */
    protected abstract ibis.ipl.PortType newPortType(StaticProperties p)
            throws PortMismatchException;

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
        throw new IbisConfigurationException("totalNrOfIbisesInPool called but open world run");
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
    public void joined(ibis.ipl.IbisIdentifier[] joinIdent) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < joinIdent.length; i++) {
                ibis.ipl.IbisIdentifier id = joinIdent[i];
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
    public void left(IbisIdentifier[] leaveIdent) {
        if (resizeHandler != null) {
            waitForEnabled();
            for (int i = 0; i < leaveIdent.length; i++) {
                IbisIdentifier id = leaveIdent[i];
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

    protected synchronized ReceivePort findReceivePort(String name) {
        return receivePorts.get(name);
    }

    synchronized void register(ReceivePort p) {
        if (receivePorts.get(p.name) != null) {
            throw new Error("Multiple instances of receive port named " + p.name);
        }
        receivePorts.put(p.name, p);
    }

    synchronized void deRegister(ReceivePort p) {
        if (receivePorts.remove(p.name) == null) {
            throw new Error("Trying to remove unknown receiveport");
        }
    }

    protected abstract void quit();

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
}
