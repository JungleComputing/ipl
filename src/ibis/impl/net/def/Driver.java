package ibis.ipl.impl.net.def;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis DEF driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "def";


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
		return new DefInput(pt, this, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws NetIbisException {
		return new DefOutput(pt, this, context);
	}
}
