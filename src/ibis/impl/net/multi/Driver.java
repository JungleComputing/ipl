package ibis.ipl.impl.net.multi;
import ibis.ipl.impl.net.*;

/**
 * The multieric splitter/poller virtual driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "multi";
	
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
	public NetInput newInput(NetPortType pt, String context) throws NetIbisException {
		return new MultiPoller(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws NetIbisException {
		return new MultiSplitter(pt, this, context);
	}
}
