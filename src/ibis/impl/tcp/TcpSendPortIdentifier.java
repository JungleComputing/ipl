package ibis.impl.tcp;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

public final class TcpSendPortIdentifier implements SendPortIdentifier,
        java.io.Serializable {
    private static final long serialVersionUID = 5L;

    String type;

    String name;

    TcpIbisIdentifier ibis;

    TcpSendPortIdentifier(String name, String type, TcpIbisIdentifier ibis) {
        this.name = name;
        this.type = type;
        this.ibis = ibis;
    }

    public boolean equals(TcpSendPortIdentifier other) {

        if (other == null) {
            return false;
        }
        return (type().equals(other.type()) && ibis.equals(other.ibis)
                && name().equals(other.name()));
    }

    public int hashCode() {
        return type().hashCode() + name().hashCode() + ibis.hashCode();
    }

    public boolean equals(Object other) {
        if (other == null)
            return false;
        if (other instanceof TcpSendPortIdentifier) {
            return equals((TcpSendPortIdentifier) other);
        }
        return false;
    }

    public final String name() {
        return name;
    }

    public final String type() {
        if (type != null) {
            return type;
        }
        return "__notype__";
    }

    public IbisIdentifier ibis() {
        return ibis;
    }

    public String toString() {
        return ("(TcpSendPortIdent: name = " + name() + ", type = " + type()
                + ", ibis = " + ibis + ")");
    }
}
