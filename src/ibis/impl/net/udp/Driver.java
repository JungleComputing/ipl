package ibis.impl.net.udp;

import ibis.impl.net.*;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.DatagramSocket;

/**
 * The NetIbis UDP driver.
 */
public final class Driver extends NetDriver {

	final static boolean DEBUG = false; // true;
	final static boolean STATISTICS = false;

	/**
	 * The driver name.
	 */
	private final String name = "udp";

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
	 * {@link ibis.impl.net.NetReceivePort NetReceivePort}.
	 * @return The new UDP input.
	 */
	public NetInput newInput(NetPortType pt, String context) throws IOException {
		return new UdpInput(pt, this, context);
	}

	/**
	 * Creates a new UDP output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @return The new UDP output.
	 */
	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
		return new UdpOutput(pt, this, context);
	}
}
