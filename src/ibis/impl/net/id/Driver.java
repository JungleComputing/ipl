package ibis.ipl.impl.net.id;

import ibis.ipl.IbisException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

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
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new Id input.
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context)
		throws NetIbisException {
		return new IdInput(pt, this, up, context);
	}

	/**
	 * Creates a new Id output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new Id output.
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context)
		throws NetIbisException {
		return new IdOutput(pt, this, up, context);
	}
}
