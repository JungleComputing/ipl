package ibis.impl.net.gm;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetOutput;
import ibis.impl.net.NetPortType;
import ibis.impl.net.NetSplitter;

import java.io.IOException;

/**
 * Provides a GM-specific multiple network output splitter.
 */
public final class GmSplitter extends NetSplitter {


	/**
	 * Constructor.
	 *
	 * @param pt the {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param driver the driver of this poller.
	 * @param context the context.
	 */
	public GmSplitter(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

	public synchronized void setupConnection(NetConnection cnx) throws IOException {
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

		writeBufferedSupported = true;

// System.err.println(this + ": " + cnx.getServiceLink() + ": established connection");
                log.out();
	}

}
