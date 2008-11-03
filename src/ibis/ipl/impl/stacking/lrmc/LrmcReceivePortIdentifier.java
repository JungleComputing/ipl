package ibis.ipl.impl.stacking.lrmc;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

class LrmcReceivePortIdentifier implements ReceivePortIdentifier {

    private static final long serialVersionUID = 1L;

    IbisIdentifier ibis;
    String name;

    LrmcReceivePortIdentifier(IbisIdentifier ibis, String name) {
        this.ibis = ibis;
        this.name = name;
    }

    public IbisIdentifier ibisIdentifier() {
        return ibis;
    }

    public String name() {
        return name;
    }
}
