package ibis.impl.net.pipe;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The NetIbis pipe-based loopback driver.
 */
public final class Driver extends NetDriver {

    /**
     * The driver name.
     */
    private final String name = "pipe";

    /**
     * Constructor.
     *
     * @param ibis the {@link ibis.impl.net.NetIbis} instance.
     */
    public Driver(NetIbis ibis) {
        super(ibis);
    }

    /**
     * Returns the name of the driver.
     *
     * @return The driver name.
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a new PIPE input.
     *
     * @param pt the input's {@link ibis.impl.net.NetPortType NetPortType}.
     * @param context the context.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     * @return The new PIPE input.
     */
    public NetInput newInput(NetPortType pt, String context,
            NetInputUpcall inputUpcall) throws IOException {
        return new PipeInput(pt, this, context, inputUpcall);
    }

    /**
     * Creates a new PIPE output.
     *
     * @param pt the output's {@link ibis.impl.net.NetPortType NetPortType}.
     * @param context the context.
     * @return The new PIPE output.
     */
    public NetOutput newOutput(NetPortType pt, String context)
            throws IOException {
        return new PipeOutput(pt, this, context);
    }
}