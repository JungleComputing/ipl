/* $Id$ */

package ibis.impl.tcp;

import smartsockets.virtual.VirtualSocketAddress;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.StaticProperties;

public final class TcpReceivePortIdentifier implements ReceivePortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 4L;

    String name;

    StaticProperties type;

    TcpIbisIdentifier ibis;

    // Removed -- simple ports don't work in a real system -- Jason 
    // int port;
    VirtualSocketAddress sa;
    
    TcpReceivePortIdentifier(String name, StaticProperties type, TcpIbisIdentifier ibis,
            VirtualSocketAddress sa) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
        this.sa = sa;
    }
    
    public boolean equals(TcpReceivePortIdentifier other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        return (type.equals(other.type) && ibis.equals(other.ibis)
                && name.equals(other.name) && sa.equals(other.sa));
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
        return name.hashCode() + sa.hashCode();
    }

    public String name() {
        return name;
    }

    public StaticProperties type() {
        return new StaticProperties(type);
    }

    public IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(TcpRecPortIdent: name = " + name + ", type = " + type
                + ", ibis = " + ibis + "socket address = " + sa + ")");
    }
}
