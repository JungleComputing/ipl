package ibis.ipl.impl.net.bytes;
import ibis.ipl.impl.net.*;

/**
 * The primitive to byte conversion driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "bytes";


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
		return new BytesInput(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws NetIbisException {
		return new BytesOutput(pt, this, up, context);
	}
}
