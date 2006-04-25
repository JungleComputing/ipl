/* $Id$ */

package ibis.impl.tcp;

import ibis.connect.virtual.VirtualSocketAddress;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

public final class TcpReceivePortIdentifier implements ReceivePortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 4L;

    String name;

    String type;

    TcpIbisIdentifier ibis;

    // Removed -- simple ports don't work in a real system -- Jason 
    // int port;
    VirtualSocketAddress sa;
    
    TcpReceivePortIdentifier(String name, String type, TcpIbisIdentifier ibis,
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
        /*
        System.out.println("TcpReceivePortIdentifier.equals: ");
        System.out.println("type: " + (type().equals(other.type())));
        System.out.println("ibis: " + (ibis.equals(other.ibis)));
        System.out.println("name: " + (name().equals(other.name())));
        System.out.println("sa  : " + (sa.equals(other.sa)));
        */
        return (type().equals(other.type()) && ibis.equals(other.ibis)
                && name().equals(other.name()) && sa.equals(other.sa));
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

    //gosia
    public int hashCode() {
        return name().hashCode() + sa.hashCode();
    }

    //end gosia

    public String name() {
        return name;
    }

    public String type() {
        if (type != null) {
            return type;
        }
        return "__notype__";
    }

    public IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(TcpRecPortIdent: name = " + name + ", type = " + type
                + ", ibis = " + ibis + "socket address = " + sa + ")");
    }
}
