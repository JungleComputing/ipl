package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.util.Iterator;
import java.util.HashMap;

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
	public GenSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
	}

}
