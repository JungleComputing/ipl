package ibis.ipl.impl.multi;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.SendPortIdentifier;

public class MultiSendPortIdentifier implements SendPortIdentifier {

    /**
     * Serial Version UID - Generated
     */
    private static final long serialVersionUID = -1400675486608710003L;

    private final IbisIdentifier id;
    private final String name;

    public MultiSendPortIdentifier(IbisIdentifier identifier, String name) {
        this.id = identifier;
        this.name = name;
    }

    public IbisIdentifier ibisIdentifier() {
        return id;
    }

    public String name() {
        return name;
    }

}
