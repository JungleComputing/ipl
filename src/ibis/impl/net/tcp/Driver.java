package ibis.impl.net.tcp;

import ibis.impl.net.*;

import java.io.IOException;

/**
 * The NetIbis TCP driver.
 */
public final class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "tcp";


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
	 * Creates a new TCP input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new TCP input.
	 */
	public NetInput newInput(NetPortType pt, String context)
		throws IOException {
		return new TcpInput(pt, this, context);
	}

	/**
	 * Creates a new TCP output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new TCP output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
		return new TcpOutput(pt, this, context);
	}
}
