package ibis.impl.net.tcp;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

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
	 * @param pt the input's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @param inputUpcall the input upcall for upcall receives, or
	 *        <code>null</code> for downcall receives
	 * @return The new TCP input.
	 */
	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall)
		throws IOException {
		return new TcpInput(pt, this, context, inputUpcall);
	}

	/**
	 * Creates a new TCP output.
	 *
	 * @param pt the output's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new TCP output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
		return new TcpOutput(pt, this, context);
	}
}
