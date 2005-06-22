/* $Id$ */

package ibis.impl.net;

import java.util.Hashtable;

/**
 * Provide a common superclass to both {@link ibis.impl.net.NetSendPort} and
 * {@link NetReceivePort} objects.
 */
public abstract class NetPort {

    /* ___ LESS-IMPORTANT OBJECTS ______________________________________ */

    /**
     * The {@link ibis.impl.net.NetIbis} instance.
     */
    protected NetIbis ibis = null;

    /**
     * The name of the port.
     */
    protected String name = null;

    /**
     * The type of the port.
     */
    protected NetPortType type = null;

    /**
     * Optional (fine grained) logging object.
     *
     * This logging object should be used to display code-level information
     * like function calls, args and variable values.
     */
    protected NetLog log = null;

    /**
     * Optional (coarse grained) logging object.
     *
     * This logging object should be used to display concept-level information
     * about high-level algorithmic steps (e.g. message send, new connection
     * initialization.
     */
    protected NetLog trace = null;

    /**
     * Optional (general purpose) logging object.
     *
     * This logging object should only be used temporarily for debugging
     * purpose.
     */
    protected NetLog disp = null;

    /**
     * The topmost network driver.
     */
    protected NetDriver driver = null;

    /* ___ IMPORTANT OBJECTS ___________________________________________ */

    /**
     * The table of network {@linkplain ibis.impl.net.NetConnection connections}
     * indexed by connection identification numbers.
     */
    protected Hashtable connectionTable = null;

    /**
     * Return the {@linkplain ibis.impl.net.NetPortType port type}.
     *
     * @return the {@linkplain ibis.impl.net.NetPortType port type}.
     */
    public final NetPortType getPortType() {
        return type;
    }

    public final String name() {
        return name;
    }

    public abstract void closeFromRemote(NetConnection cnx);
}

