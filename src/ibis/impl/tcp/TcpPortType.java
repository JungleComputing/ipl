/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.PortType;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;

class TcpPortType extends PortType {

    TcpIbis ibis;

    TcpPortType(TcpIbis ibis, StaticProperties p)
            throws PortMismatchException {
        super(p);
        this.ibis = ibis;
    }

    protected SendPort doCreateSendPort(String nm, SendPortConnectUpcall cU,
            boolean connectionAdministration) throws IOException {
        return new TcpSendPort(ibis, this, nm, connectionAdministration, cU);
    }

    protected ReceivePort doCreateReceivePort(String nm, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionAdministration)
            throws IOException {
        return new TcpReceivePort(ibis, this, nm, u, connectionAdministration,
                cU);
    }
}
