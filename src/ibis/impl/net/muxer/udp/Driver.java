package ibis.ipl.impl.net.muxer.udp;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis Muxer/UDP driver.
 */
public final class Driver extends NetDriver {

	final static boolean DEBUG = false;
	final static boolean STATISTICS = false;

	/**
	 * The driver name.
	 */
	private final String name = "muxer.udp";

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
	 * Creates a new UDP input.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}.
	 * @param input the controlling input.
	 * @return The new UDP input.
	 */
	public NetInput newInput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
		return new UdpMuxInput(pt, this, up, context);
	}

	/**
	 * Creates a new UDP output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param output the controlling output.
	 * @return The new UDP output.
	 */
	public NetOutput newOutput(NetPortType pt, NetIO up, String context)
		throws IbisIOException {
		return new UdpMuxOutput(pt, this, up, context);
	}
}
