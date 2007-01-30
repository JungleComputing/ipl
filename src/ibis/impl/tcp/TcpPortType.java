/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.Ibis;
import ibis.impl.PortType;
import ibis.impl.ReceivePort;
import ibis.impl.SendPort;
import ibis.ipl.PortMismatchException;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortConnectUpcall;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.io.IOException;

class TcpPortType extends PortType {

    TcpPortType(Ibis ibis, StaticProperties p) throws PortMismatchException {
        super(ibis, p);
    }

    protected SendPort doCreateSendPort(String nm, SendPortConnectUpcall cU,
            boolean connectionDowncalls) throws IOException {
        return new TcpSendPort(ibis, this, nm, connectionDowncalls, cU);
    }

    protected ReceivePort doCreateReceivePort(String nm, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        return new TcpReceivePort(ibis, this, nm, u, connectionDowncalls, cU);
    }
}
