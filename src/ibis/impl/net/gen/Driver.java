package ibis.ipl.impl.net.gen;
import ibis.ipl.impl.net.*;

/**
 * The generic splitter/poller virtual driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "gen";
	
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
		return new GenPoller(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws NetIbisException {
		return new GenSplitter(pt, this, up, context);
	}
}
