/* $Id$ */

package ibis.impl.net;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

/**
 * Provides an identifier for a {@link ibis.impl.net.NetSendPort}.
 */
public final class NetSendPortIdentifier implements SendPortIdentifier,
        java.io.Serializable {

    /**
     * SendPort type name.
     */
    private String type = null;

    /**
     * SendPort name.
     */
    private String name = null;

    /**
     * Ibis instance identifier.
     */
    private NetIbisIdentifier ibis = null;

    /**
     * Constructor.
     *
     * @param name the name of the SendPort.
     * @param type the type name.
     * @param ibis the Ibis instance identifier.
     */
    public NetSendPortIdentifier(String name, String type,
            NetIbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    /**
     * Specific equality test.
     *
     * @return The equality result.
     */
    public boolean equals(NetSendPortIdentifier other) {

    	if (other == null) {
    		return false;
    	}
    	return (type().equals(other.type()) && ibis.equals(other.ibis)
    			&& name().equals(other.name()));
    }

    /**
     * Generic equality test.
     *
     * @return The equality result.
     */
    public boolean equals(Object other) {
        if (other instanceof NetSendPortIdentifier) {
        	return equals((NetSendPortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name().hashCode() + ibis.hashCode() + type().hashCode();
    }

    /**
     * Returns the SendPort type name.
     *
     * @return The type name.
     */
    public String type() {
        if (type != null) {
            return type;
        }
        return "__notype__";
    }

    /**
     * Returns the SendPort name.
     *
     * @return The name.
     */
    public String name() {
        return name;
    }

    /**
     * Returns the Ibis instance identifier.
     *
     * @return The Ibis identifier.
     */
    public IbisIdentifier ibis() {
        return ibis;
    }
}
