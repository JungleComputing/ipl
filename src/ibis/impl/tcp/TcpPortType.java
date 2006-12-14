/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.PortType;
import ibis.impl.PortType;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;

class TcpPortType extends PortType implements Config {

    TcpIbis ibis;

    TcpPortType(TcpIbis ibis, StaticProperties p)
            throws PortMismatchException {
        super(p);
        this.ibis = ibis;
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
            System.out.println(ibis.ident
                    + ": Receiveport created name = '" + prt.name() + "'");
        }

        return prt;
    }
}
