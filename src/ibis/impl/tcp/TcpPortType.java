/* $Id$ */

package ibis.impl.tcp;

import ibis.ipl.IbisException;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;

class TcpPortType extends PortType implements Config {

    StaticProperties p;

    String name;

    TcpIbis ibis;

    boolean numbered;

    String ser;

    TcpPortType(TcpIbis ibis, String name, StaticProperties p)
            throws IbisException {
        this.ibis = ibis;
        this.name = name;
        this.p = p;
        numbered = p.isProp("communication", "Numbered");

        ser = p.find("Serialization");

        if (ser == null) ser = "sun";

        if (ser.equals("byte") && numbered) {
            throw new IbisException(
                    "Numbered communication is not supported on byte "
                    + "serialization streams");
        }
    }

    public String name() {
        return name;
    }

    private boolean equals(TcpPortType other) {
        return name.equals(other.name) && ibis.equals(other.ibis);
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof TcpPortType)) {
            return false;
        }
        return equals((TcpPortType) other);
    }

    public int hashCode() {
        return name.hashCode() + ibis.hashCode();
    }

    public StaticProperties properties() {
        return p;
    }

    public SendPort createSendPort(String nm, SendPortConnectUpcall cU,
            boolean connectionAdministration) {
        return new TcpSendPort(ibis, this, nm, connectionAdministration, cU);
    }

    public ReceivePort createReceivePort(String nm, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionAdministration)
            throws IOException {
        TcpReceivePort prt = new TcpReceivePort(ibis, this, nm, u,
                connectionAdministration, cU);

        if (DEBUG) {
            System.out.println(ibis.identifier()
                    + ": Receiveport created name = '" + prt.name() + "'");
        }

        return prt;
    }

    public String toString() {
        return ("(TcpPortType: name = " + name + ")");
    }
}
