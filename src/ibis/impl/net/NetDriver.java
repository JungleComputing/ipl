/* $Id$ */

package ibis.impl.net;

import java.io.IOException;

/**
 * Provide a generic interface for the low level network drivers.
 *
 * Each non-abstract subclass of this class should be named <CODE>Driver</CODE>
 * and placed in a sub-package/sub-directory of {@link ibis.impl.net} to allow
 * for the dynamic driver loading mechanism to find the driver properly.
 */
public abstract class NetDriver {

    /**
     * The owning {@link ibis.impl.net.NetIbis}.
     */
    private NetIbis ibis = null;

    /**
     * Constructor.
     */
    public NetDriver() {
        // No-args constructor required by Java
    }

    /**
     * Constructor.
     *
     * @param ibis the owning Ibis instance.
     */
    public NetDriver(NetIbis ibis) {
        this.ibis = ibis;
    }

    /**
     * Set the owning {@link ibis.impl.net.NetIbis}
     *
     * @param ibis the owning {@link ibis.impl.net.NetIbis}
     */
    void setIbis(NetIbis ibis) {
        this.ibis = ibis;
    }

    /**
     * Return the owning Ibis instance.
     *
     * @return the Ibis instance which loaded this driver.
     */
    public NetIbis getIbis() {
        return ibis;
    }

    /**
     * Return the name of the driver.
     *
     * Note: the name of the driver should preferably be equal to the suffix
     * of the driver's package name.
     *
     * @return The driver's name.
     */
    public abstract String getName();

    /**
     * Create a new {@link ibis.impl.net.NetInput} according to the
     * {@linkplain ibis.impl.net.NetPortType port type} settings and the
     * context {@linkplain String string}.
     *
     * @param pt the port type.
     * @param context the context string.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     *
     * @exception IOException if the input creation fail.
     */
    public abstract NetInput newInput(NetPortType pt, String context,
            NetInputUpcall inputUpcall) throws IOException;

    /**
     * Create a new {@link ibis.impl.net.NetOutput} according to the
     * {@linkplain ibis.impl.net.NetPortType port type} settings and the
     * context {@linkplain String string}.
     *
     * @param pt the port type.
     * @param context the context string.
     *
     * @exception IOException if the output creation fail.
     */
    public abstract NetOutput newOutput(NetPortType pt, String context)
            throws IOException;
}
