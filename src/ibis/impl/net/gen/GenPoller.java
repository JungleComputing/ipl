package ibis.impl.net.gen;

import ibis.impl.net.*;

import java.io.IOException;

/**
 * Provides a generic multiple network input poller.
 */
public final class GenPoller extends NetPoller {

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GenPoller(NetPortType pt, NetDriver driver, String context)
		throws IOException {
		super(pt, driver, context);
	}
}
