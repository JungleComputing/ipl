package ibis.ipl.impl.net.tcp;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIbis;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetOutput;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis TCP driver.
 */
public class Driver extends NetDriver {

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
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new TCP input.
	 */
	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
		return new TcpInput(sp, this, input);
	}

	/**
	 * Creates a new TCP output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new TCP output.
	 */
	public NetOutput newOutput(StaticProperties sp,
				   NetOutput	    output)
		throws IbisIOException {
		return new TcpOutput(sp, this, output);
	}
}
