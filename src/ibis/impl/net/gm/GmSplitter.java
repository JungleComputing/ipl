package ibis.ipl.impl.net.gm;

import ibis.ipl.impl.net.*;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Provides a GM-specific multiple network output splitter.
 */
public final class GmSplitter extends NetSplitter {


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GmSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();

		NetOutput no = new GmOutput(type, driver, null);
		setupConnection(cnx, cnx.getNum(), no);

		int _mtu = no.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = no.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}

                log.out();
	}

}
