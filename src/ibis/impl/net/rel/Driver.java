package ibis.ipl.impl.net.rel;

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
 * The NetIbis 'reliability' driver.
 *
 * This driver is an example of a virtual driver. It just pass data untouched.
 * The goal of this driver is to provide a starting point for implementing
 * other virtual drives.
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
	 * Returns the name of the driver.
	 *
	 * @return The driver name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Creates a new Rel input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new Rel input.
	 */
	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
		return new RelInput(sp, this, input);
	}

	/**
	 * Creates a new Rel output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new Rel output.
	 */
	public NetOutput newOutput(StaticProperties sp,
				   NetOutput	    output)
		throws IbisIOException {
		return new RelOutput(sp, this, output);
	}
}
