/* $Id$ */

package ibis.impl.tcp;

import ibis.impl.Ibis;
import ibis.impl.PortType;
import ibis.impl.ReceivePort;
import ibis.impl.SendPort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.CapabilitySet;
import ibis.ipl.Upcall;

import java.util.Properties;

import java.io.IOException;

class TcpPortType extends PortType {

    TcpPortType(Ibis ibis, CapabilitySet p, Properties tp) {
        super(ibis, p, tp);
    }

    protected SendPort doCreateSendPort(String nm, SendPortDisconnectUpcall cU,
            boolean connectionDowncalls) throws IOException {
        return new TcpSendPort(ibis, this, nm, connectionDowncalls, cU);
    }

    protected ReceivePort doCreateReceivePort(String nm, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        return new TcpReceivePort(ibis, this, nm, u, connectionDowncalls, cU);
    }
}
