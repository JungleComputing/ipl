package ibis.ipl.impl.net;

/**
 * Provides a generic interface for the low level network drivers.
 *
 * Each non-abstract subclass of this class should be named <CODE>Driver</CODE>
 * and placed in a sub-package/sub-directory of {@link ibis.ipl.impl.net} to allow
 * for the dynamic driver loading mechanism to find the driver properly.
 */
public abstract class NetDriver {
	
	/**
	 * The reference to the owning {@link NetIbis} instance.
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
	 * Returns the owning {@link NetIbis} instance.
	 *
	 * @return the {@link NetIbis} instance which loaded this driver.
	 */
	public NetIbis getIbis() {
		return ibis;
	}

	/**
	 * Returns the name of the driver.
	 *
	 * Note: the name of the driver should preferably be equals to the suffix
	 * of the driver's package name.
	 *
	 * @param return the driver's name.
	 */
	public abstract String getName();
	
	/**
	 * Creates a new {@link NetInput} according to the
	 * {@linkplain StaticProperties properties}.
	 *
	 * @param sp The requested properties.
	 * @param input The optional controlling {@linkplain NetInput input}
	 *              or <CODE>null</CODE> if the new input is directly 
	 *              controlled by the {@linkplain NetReceivePort receive port}.
	 *
	 * @exception NetIbisException if the input creation fail.
	 */
	public abstract NetInput newInput(NetPortType pt, NetIO up, String context)
		throws NetIbisException;

	/**
	 * Creates a new {@link NetOutput} according to the
	 * {@linkplain StaticProperties properties}.
	 *
	 * @param sp The requested properties.
	 * @param output The optional controlling {@linkplain NetOutput output}
	 *              or <CODE>null</CODE> if the new output is directly 
	 *              controlled by the {@linkplain NetSendPort send port}.
	 *
	 * @exception NetIbisException if the output creation fail.
	 */
	public abstract NetOutput newOutput(NetPortType pt, NetIO up, String context)
		throws NetIbisException;
}
