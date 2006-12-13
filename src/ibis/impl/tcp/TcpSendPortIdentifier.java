/* $Id$ */

package ibis.impl.tcp;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;

public final class TcpSendPortIdentifier implements SendPortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 5L;

    StaticProperties type;

    String name;

    IbisIdentifier ibis;

    TcpSendPortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    public boolean equals(TcpSendPortIdentifier other) {

        if (other == null) {
            return false;
        }
        return (type.equals(other.type) && ibis.equals(other.ibis)
                && name.equals(other.name));
    }

    public int hashCode() {
        return type.hashCode() + name.hashCode() + ibis.hashCode();
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof TcpSendPortIdentifier) {
            return equals((TcpSendPortIdentifier) other);
        }
        return false;
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
        return ("(TcpSendPortIdent: name = " + name + ", type = " + type
                + ", ibis = " + ibis + ")");
    }
}
