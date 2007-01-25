/* $Id: SendPortIdentifier.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.StaticProperties;

/**
 * Implementation of the <code>SendPortIdentifier</code> interface.
 * This class can be extended by Ibis implementations.
 */
public class SendPortIdentifier implements ibis.ipl.SendPortIdentifier,
        java.io.Serializable {

    /** The properties of the corresponding sendport. */
    protected StaticProperties type;

    /** The name of the corresponding sendport. */
    protected String name;

    /** The IbisIdentifier of the Ibis instance that created the sendport. */
    protected IbisIdentifier ibis;

    /**
     * Constructor, initializing the fields with the specified parameters.
     * @param name the name of the sendport.
     * @param type the properties of the sendport.
     * @param ibis the Ibis instance that created the sendport.
     */
    public SendPortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    private boolean equals(SendPortIdentifier other) {
        if (other == this) {
            return true;
        }
        return name.equals(other.name) && ibis.equals(other.ibis)
                && type.equals(other.type);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof SendPortIdentifier) {
            return equals((SendPortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public final String name() {
        return name;
    }

    public final StaticProperties type() {
        return new StaticProperties(type);
    }

    public IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(SendPortIdentifier: name = " + name + ", type = " + type
                + ", ibis = " + ibis + ")");
    }
}
