package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Provides a generic multiple network output poller.
 */
public final class GenSplitter extends NetSplitter {

	/**
	 * The driver used for the outputs.
	 */
	private NetDriver subDriver = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GenSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
	}

	/**
	 * Adds a new input to the output set.
	 *
	 * The MTU and the header offset is updated by this function.
	 *
	 * @param output the output.
	 */
	private void addOutput(Integer rpn, NetOutput output) {
                log.in();
		int _mtu = output.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = output.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}

		outputMap.put(rpn, output);
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
		if (subDriver == null) {
			String subDriverName = getProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetOutput no = newSubOutput(subDriver);
		super.setupConnection(cnx, cnx.getNum(), no);

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

        protected Object getKey(Integer num) {
                return num;
        }

        public synchronized void closeConnection(Integer num) throws NetIbisException {
                NetOutput output = (NetOutput)outputMap.get(num);
                if (output != null) {
                        output.close(num);
                }
        }
}
