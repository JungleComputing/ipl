/* $Id$ */

package ibis.impl.net.tcp_plain;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The NetIbis TCP driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

    /**
     * The driver name.
     */
    private final String name = "tcp_plain";

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
        return new TcpInput(pt, this, context, inputUpcall);
    }

    public NetOutput newOutput(NetPortType pt, String context)
            throws IOException {
        return new TcpOutput(pt, this, context);
    }
}
