package ibis.ipl.impl.net.s_sun;

import ibis.ipl.impl.net.*;

public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "s_ibis";


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context) throws NetIbisException {
		return new SSunInput(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws NetIbisException {
		return new SSunOutput(pt, this, up, context);
	}
}
