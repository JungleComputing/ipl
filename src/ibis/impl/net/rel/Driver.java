package ibis.ipl.impl.net.rel;
import ibis.ipl.impl.net.*;
import ibis.ipl.IbisIOException;

/**
 * The NetIbis 'reliability' driver.
 */
public class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "rel";

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
		return new RelInput(pt, this, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context) throws IbisIOException {
		return new RelOutput(pt, this, up, context);
	}
}
