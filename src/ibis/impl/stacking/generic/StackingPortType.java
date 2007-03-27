package ibis.impl.stacking.generic;

import java.io.IOException;
import java.util.Properties;

import ibis.impl.Ibis;
import ibis.impl.PortType;
import ibis.ipl.CapabilitySet;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortConnectUpcall;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortDisconnectUpcall;
import ibis.ipl.Upcall;

public class StackingPortType extends PortType {
    
    ibis.ipl.PortType base;
    
    public StackingPortType(Ibis ibis, CapabilitySet p, Properties tp) {
        super(ibis, p, tp);
        base = ((StackingIbis)ibis).ibis.createPortType(p, tp);
    }

    @Override
    protected ReceivePort doCreateReceivePort(String name, Upcall u,
            ReceivePortConnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        return new StackingReceivePort(this, name, u, cU, connectionDowncalls);
    }

    @Override
    protected SendPort doCreateSendPort(String name,
            SendPortDisconnectUpcall cU, boolean connectionDowncalls)
            throws IOException {
        return new StackingSendPort(this, name, cU, connectionDowncalls);
    }

}
