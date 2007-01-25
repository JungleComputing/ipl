/* $Id: ReceivePortIdentifier.java 4910 2006-12-13 09:01:33Z ceriel $ */

package ibis.impl;

import ibis.ipl.StaticProperties;

/**
 * Implementation of the <code>ReceivePortIdentifier</code> interface.
 * This class can be extended by Ibis implementations.
 */
public class ReceivePortIdentifier implements ibis.ipl.ReceivePortIdentifier,
        java.io.Serializable {

    /** The name of the corresponding receiveport. */
    protected final String name;

    /** The properties of the corresponding receiveport. */
    protected final StaticProperties type;

    /** The IbisIdentifier of the Ibis instance that created the receiveport. */
    protected final IbisIdentifier ibis;

    /**
     * Constructor, initializing the fields with the specified parameters.
     * @param name the name of the receiveport.
     * @param type the properties of the receiveport.
     * @param ibis the Ibis instance that created the receiveport.
     */
    public ReceivePortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    private boolean equals(ReceivePortIdentifier other) {
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
        if (other instanceof ReceivePortIdentifier) {
            return equals((ReceivePortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public String name() {
        return name;
    }

    public StaticProperties type() {
        return new StaticProperties(type);
    }

    public ibis.ipl.IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(ReceivePortIdentifier: name = " + name + ", type = " + type
                + ", ibis = " + ibis);
    }
}
