package ibis.ipl.impl.net.s_sun;

import ibis.ipl.impl.net.*;

import java.io.IOException;

public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "s_sun";


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
	public NetInput newInput(NetPortType pt, String context) throws IOException {
		return new SSunInput(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new SSunOutput(pt, this, context);
	}
}
