package ibis.ipl.impl.net.nio;
import ibis.ipl.IbisIOException;
import ibis.ipl.impl.net.*;

/**
 * The NetIbis NIO TCP driver
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "nio";


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
	public NetInput newInput(NetPortType pt, String context) 
						throws NetIbisException {
		return new NioInput(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context) 
						throws NetIbisException {
		return new NioOutput(pt, this, context);
	}
}
