package ibis.ipl.impl.net;

/**
 * Provide a generic interface for the low level network drivers.
 *
 * Each non-abstract subclass of this class should be named <CODE>Driver</CODE>
 * and placed in a sub-package/sub-directory of {@link ibis.ipl.impl.net} to allow
 * for the dynamic driver loading mechanism to find the driver properly.
 */
public abstract class NetDriver {

	/**
	 * Reference the owning {@link NetIbis} instance.
	 */
	protected NetIbis ibis = null;


	/**
	 * Constructor.
	 */
	public NetDriver() {
	}


	/**
	 * Constructor.
	 *
	 * @param NetIbis the owning {@link NetIbis} instance.
	 */
	public NetDriver(NetIbis ibis) {
		this.ibis = ibis;
	}


	/**
	 * Set the owning {@link NetIbis}
	 *
	 * @param ibis the owning {@link NetIbis}
	 */
	void setIbis(NetIbis ibis) {
	    this.ibis = ibis;
	}


	/**
	 * Return the owning {@link NetIbis} instance.
	 *
	 * @return the {@link NetIbis} instance which loaded this driver.
	 */
	public NetIbis getIbis() {
		return ibis;
	}

	/**
	 * Return the name of the driver.
	 *
	 * Note: the name of the driver should preferably be equals to the suffix
	 * of the driver's package name.
	 *
	 * @param return the driver's name.
	 */
	public abstract String getName();

	/**
	 * Create a new {@link NetInput} according to the
	 * {@linkplain NetPortType port type} settings and the context {@linkplain String string}.
	 *
	 * @param pt the port type.
         * @param context the context string.
	 *
	 * @exception NetIbisException if the input creation fail.
	 */
	public abstract NetInput newInput(NetPortType pt, String context)
		throws NetIbisException;

	/**
	 * Create a new {@link NetOutput} according to the
	 * {@linkplain NetPortType port type} settings and the context {@linkplain String string}.
	 *
	 * @param pt the port type.
         * @param context the context string.
	 *
	 * @exception NetIbisException if the output creation fail.
	 */
	public abstract NetOutput newOutput(NetPortType pt, String context)
		throws NetIbisException;
}
