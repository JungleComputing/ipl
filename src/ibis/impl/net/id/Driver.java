package ibis.impl.net.id;

import ibis.impl.net.*;

import java.io.IOException;

/**
 * The NetIbis 'identity' dummy driver.
 *
 * This driver is an example of a virtual driver. It just pass data untouched.
 * The goal of this driver is to provide a starting point for implementing
 * other virtual drives.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "id";


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}	

	/**
	 * Returns the name of the driver.
	 *
	 * @return The driver name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates a new Id input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.impl.net.NetReceivePort NetReceivePort}.
	 * @return The new Id input.
	 */
	public NetInput newInput(NetPortType pt, String context)
		throws IOException {
		return new IdInput(pt, this, context);
	}

	/**
	 * Creates a new Id output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @return The new Id output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
		return new IdOutput(pt, this, context);
	}
}
