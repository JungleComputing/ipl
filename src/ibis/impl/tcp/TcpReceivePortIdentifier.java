/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.StaticProperties;

public final class TcpReceivePortIdentifier implements ReceivePortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 4L;

    String name;

    StaticProperties type;

    IbisIdentifier ibis;

    int port;

    TcpReceivePortIdentifier(String name, StaticProperties type,
            IbisIdentifier ibis, int port) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
        this.port = port;
    }

    public boolean equals(TcpReceivePortIdentifier other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return (type.equals(other.type) && ibis.equals(other.ibis)
                && name.equals(other.name) && port == other.port);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof TcpReceivePortIdentifier) {
            return equals((TcpReceivePortIdentifier) other);
        }
        return false;
    }

    public int hashCode() {
        return name.hashCode() + port;
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
        return ("(TcpRecPortIdent: name = " + name + ", type = " + type
                + ", ibis = " + ibis + "port = " + port + ")");
    }
}
