package ibis.impl.net.bytes;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.util.TypedProperties;

/**
 * The primitive to byte conversion driver.
 */
public final class Driver extends NetDriver {

    static final String prefix = "ibis.net.bytes.";

    static final String bytes_mtu = prefix + "mtu";

    private static final String[] properties = { bytes_mtu };

    static {
        TypedProperties.checkProperties(prefix, properties, null);
    }

    /**
     * The driver name.
     */
    private final String name = "bytes";

    /**
     * Constructor.
     *
     * @param ibis the {@link ibis.impl.net.NetIbis} instance.
     */
    public Driver(NetIbis ibis) {
        super(ibis);
    }

    public String getName() {
        return name;
    }

    public NetInput newInput(NetPortType pt, String context,
            NetInputUpcall inputUpcall) {
        return new BytesInput(pt, this, context, inputUpcall);
    }

    public NetOutput newOutput(NetPortType pt, String context) {
        return new BytesOutput(pt, this, context);
    }
}