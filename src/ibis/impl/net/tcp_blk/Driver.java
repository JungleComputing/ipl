package ibis.impl.net.tcp_blk;

import java.net.Socket;

import ibis.ipl.IbisIdentifier;

import ibis.impl.net.NetDriver;
import ibis.impl.net.NetIbis;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.util.TypedProperties;

import java.io.IOException;

/**
 * The NetIbis TCP driver with pipelined block transmission.
 */
public final class Driver extends NetDriver {

	private static final String prefix = "ibis.net.tcp_blk.";

	static final String tcpblk_rdah = prefix + "read_ahead";
	static final String tcpblk_timeout = prefix + "timeout";
	static final String tcpblk_cache_v = prefix + "cache.verbose";
	static final String tcpblk_cache_enable = prefix + "cache";

	private static final String[] properties = {
		tcpblk_rdah,
		tcpblk_timeout,
		tcpblk_cache_v,
		tcpblk_cache_enable
	};

	static {
		TypedProperties.checkProperties(prefix, properties, null);
	}

	/**
	 * The driver name.
	 */
	private final String name = "tcp_blk";

	private TcpConnectionCache connectionCache = new TcpConnectionCache();

	boolean cacheInput(IbisIdentifier ibis, Socket socket) {
	    return connectionCache.cacheInput(ibis, socket);
	}

	boolean cacheOutput(IbisIdentifier ibis, Socket socket) {
	    return connectionCache.cacheOutput(ibis, socket);
	}

	Socket getCachedInput(IbisIdentifier ibis, int port) {
	    return connectionCache.getCachedInput(ibis, port);
	}

	Socket getCachedOutput(IbisIdentifier ibis) {
	    return connectionCache.getCachedOutput(ibis);
	}


	/**
	 * Constructor.
	 *
	 * @param ibis the {@link ibis.impl.net.NetIbis} instance.
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
