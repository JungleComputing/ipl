/* $Id: Ibis.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.ipl.StaticProperties;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ResizeHandler;

import ibis.util.ClassLister;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Base class for Ibis implementations. All Ibis implementations must
 * extend this class.
 */

public abstract class Ibis extends ibis.ipl.Ibis {

    /** A user-supplied resize handler, with join/leave upcalls. */
    protected ResizeHandler resizeHandler;

    /**
     * Properties, as given to
     * {@link #createIbis(StaticProperties, ResizeHandler)}
     */
    protected StaticProperties requiredProps;

    /** User properties, combined with required properties. */
    protected StaticProperties combinedProps;

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
            StaticProperties combinedProps) {
        this.resizeHandler = resizeHandler;
        this.requiredProps = requiredProps;
        this.combinedProps = combinedProps;
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
            p.add("worldmodel", ""); // not significant for port type,
            // and may conflict with the ibis prop.
            checkPortProperties(p);
        }
        if (combinedProps.find("verbose") != null) {
            System.out.println("Creating port type"
                    + " with properties\n" + p);
        }
        if (p.isProp("communication", "manytoone") &&
                p.isProp("communication", "onetomany")) {
            System.err.println("WARNING: combining ManyToOne and OneToMany in "
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
            System.err.println("Ibis required properties: " + requiredProps);
            System.err.println("Port required properties: " + p);
            throw new PortMismatchException(
                    "Port properties don't match the Ibis required properties");
        }
    }

    public int totalNrOfIbisesInPool() {
        if (combinedProps.isProp("worldmodel", "closed")) {
            return TypedProperties.intProperty("ibis.pool.total_hosts");
        }
        return -1;
    }

    /**
     * Notifies this Ibis instance that other Ibis instances have
     * joined the run.
     * @param joinIdents the Ibis {@linkplain ibis.ipl.IbisIdentifier
     * identifiers} of the Ibis instances joining the run.
     */
    public abstract void joined(ibis.ipl.IbisIdentifier[] joinIdents);

    /**
     * Notifies this Ibis instance that other Ibis instances have
     * left the run.
     * @param leaveIdents the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances leaving the run.
     */
    public abstract void left(ibis.ipl.IbisIdentifier[] leaveIdents);

    /**
     * Notifies this Ibis instance that other Ibis instances have died.
     * @param corpses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that died.
     */
    public abstract void died(ibis.ipl.IbisIdentifier[] corpses);

    /**
     * Notifies this Ibis instance that some Ibis instances are requested
     * to leave.
     * @param ibisses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that are requested to leave.
     */
    public abstract void mustLeave(ibis.ipl.IbisIdentifier[] ibisses);
}
