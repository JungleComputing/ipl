package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetIbis;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetPoller;
import ibis.ipl.impl.net.NetReceivePortIdentifier;
import ibis.ipl.impl.net.NetSplitter;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The generic splitter/poller virtual driver.
 */
public class Driver extends NetDriver {

	/**
	 * The driver name.
	 */
	private final String name = "gen";
	
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
	 * Creates a new generic poller.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new generic poller.
	 */
	public NetInput newInput(StaticProperties sp,
				 NetInput	  input)
		throws IbisIOException {
		return new NetPoller(sp, this, input);
	}

	/**
	 * Creates a new generic splitter.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new generic splitter.
	 */
	public NetOutput newOutput(StaticProperties sp,
				   NetOutput        output)
		throws IbisIOException {
		return new NetSplitter(sp, this, output);
	}
}
