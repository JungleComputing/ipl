package ibis.impl.net.id;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

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
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
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
	 * @param pt the input's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @param inputUpcall the input upcall for upcall receives, or
	 *        <code>null</code> for downcall receives
	 * @return The new Id input.
	 */
	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall)
		throws IOException {
		return new IdInput(pt, this, context, inputUpcall);
	}

	/**
	 * Creates a new Id output.
	 *
	 * @param pt the output's {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param context the context.
	 * @return The new Id output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
		return new IdOutput(pt, this, context);
	}
}
