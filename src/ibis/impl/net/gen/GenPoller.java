package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.IOException;

import java.util.Hashtable;

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
