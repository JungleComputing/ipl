package ibis.ipl.impl.net.tcp_blk;

import ibis.ipl.impl.net.*;

/**
 * The NetIbis TCP driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "tcp_blk";


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
		return new TcpInput(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws NetIbisException {
		return new TcpOutput(pt, this, up, context);
	}
}
