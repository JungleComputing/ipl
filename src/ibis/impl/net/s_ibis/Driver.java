package ibis.ipl.impl.net.s_ibis;

import ibis.ipl.impl.net.*;

import java.io.IOException;

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
	public NetInput newInput(NetPortType pt, String context) throws IOException {
		return new SIbisInput(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new SIbisOutput(pt, this, context);
	}
}
