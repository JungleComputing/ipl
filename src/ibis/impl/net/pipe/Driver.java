package ibis.ipl.impl.net.pipe;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis pipe-based loopback driver.
 */
public class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "pipe";


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
	 * Creates a new PIPE input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new PIPE input.
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
		return new PipeInput(pt, this, up, context);
	}

	/**
	 * Creates a new PIPE output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new PIPE output.
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
		return new PipeOutput(pt, this, up, context);
	}
}
