package ibis.impl.net.tcp_blk;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * The NetIbis TCP driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	private static final String prefix = "ibis.net.tcp_blk.";

	static final String tcpblk_rdah = prefix + "read_ahead";

	private static final String[] properties = {
		tcpblk_rdah
	};

	static {
		ibis.util.TypedProperties.checkProperties(prefix, properties, null);
	}

	/**
	 * The driver name.
	 */
	private final String name = "tcp_blk";


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link NetIbis} instance.
	 */
	public Driver(NetIbis ibis) {
		super(ibis);
	}

	public String getName() {
		return name;
	}

	public NetInput newInput(NetPortType pt, String context, NetInputUpcall inputUpcall) throws IOException {
                //System.err.println("new tcp input");
		return new TcpInput(pt, this, context, inputUpcall);
	}

	public NetOutput newOutput(NetPortType pt, String context) throws IOException {
                //System.err.println("new tcp output");
		return new TcpOutput(pt, this, context);
	}
}
