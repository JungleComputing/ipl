package ibis.ipl.impl.multi;

import ibis.ipl.IbisIdentifier;
import ibis.ipl.ReceivePortIdentifier;

import java.util.HashMap;

public class MultiReceivePortIdentifier implements ReceivePortIdentifier {

    /**
     * Serial Version ID - Generated
     */
    private static final long serialVersionUID = 3918962573170503300L
    ;
    private final String name;
    private final IbisIdentifier id;

    private final HashMap<String, ReceivePortIdentifier>subIds = new HashMap<String, ReceivePortIdentifier>();

    public MultiReceivePortIdentifier(IbisIdentifier id, String name) {
        this.name = name;
        this.id = id;
    }

    public IbisIdentifier ibisIdentifier() {
        return id;
    }

    public String name() {
        return name;
    }

    ReceivePortIdentifier getSubId(String ibisName) {
        return subIds.get(ibisName);
    }

    void addSubId(String ibisName, ReceivePortIdentifier subId) {
        subIds.put(ibisName, subId);
    }
}
