/* $Id$ */

package ibis.impl.net.ser;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

public final class Driver extends NetDriver {

    /**
     * The driver name.
     */
    private final String name = "ser";

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
            NetInputUpcall inputUpcall) throws IOException {
        return new SSerInput(pt, this, context, inputUpcall);
    }

    public NetOutput newOutput(NetPortType pt, String context)
            throws IOException {
        return new SSerOutput(pt, this, context);
    }
}
