package ibis.impl.net.gen;

import ibis.impl.net.*;

import java.io.IOException;

/**
 * Provides a generic multiple network output poller.
 */
public final class GenSplitter extends NetSplitter {


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GenSplitter(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

}
