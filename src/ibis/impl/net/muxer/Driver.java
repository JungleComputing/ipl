package ibis.impl.net.muxer;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import ibis.io.Conversion;

import java.io.IOException;

/**
 * The NetIbis UDP Multiplexer driver.
 */
public final class Driver extends NetDriver {

	final static boolean DEBUG = false; // true;
	final static boolean DEBUG_HUGE = false; // DEBUG;
	final static boolean STATISTICS = false;

	final static boolean	PACKET_SEQNO = false; // true;
	final static int	KEY_OFFSET = 0;
	final static int	SEQNO_OFFSET = Conversion.INT_SIZE;
	final static int	HEADER_SIZE = PACKET_SEQNO ?
						SEQNO_OFFSET + Conversion.LONG_SIZE :
						SEQNO_OFFSET;

	/**
	 * The driver name.
	 */
	private final String name = "muxer";

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
	public NetInput newInput(NetPortType pt, String context)
		throws IOException {
		return new Demuxer(pt, this, context);
	}

	/**
	 * Creates a new UDP output.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.impl.net.NetSendPort NetSendPort}.
	 * @return The new UDP output.
	 */
	public NetOutput newOutput(NetPortType pt, String context)
		throws IOException {
		return new Muxer(pt, this, context);
	}
}
