package ibis.ipl.impl.net.multi;
import ibis.ipl.impl.net.*;
import ibis.ipl.IbisIOException;

/**
 * The multieric splitter/poller virtual driver.
 */
public class Driver extends NetDriver {

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
	public NetInput newInput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new MultiPoller(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new MultiSplitter(pt, this, up, context);
	}
}
