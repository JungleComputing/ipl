package ibis.ipl.impl.net.tcp_plain;
import ibis.ipl.IbisIOException;
import ibis.ipl.impl.net.*;

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
	public NetInput newInput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new TcpInput(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new TcpOutput(pt, this, up, context);
	}
}
