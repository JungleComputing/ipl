package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

/**
 * Provides a generic multiple network input poller.
 */
public final class GenPoller extends NetPoller {

	/**
	 * The driver used for the inputs.
	 */
	private NetDriver subDriver = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GenPoller(NetPortType pt, NetDriver driver, String context)
		throws NetIbisException {
		super(pt, driver, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
                log.in();
		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetInput ni = newSubInput(subDriver);

		super.setupConnection(cnx, cnx.getNum(), ni);
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void selectInput(Integer spn) throws NetIbisClosedException {
                log.in();
                activeQueue = (ReceiveQueue)inputTable.get(spn);
                if (activeQueue == null) {
                        log.disp("setting activeQueue - input closed, spn = "+spn);
                        throw new NetIbisClosedException("connection "+spn+" closed");
                }
                log.disp("setting activeQueue - ok, spn = "+spn);
                log.out();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void selectConnection(ReceiveQueue ni) {
                log.in();
                NetInput    input = ni.input;

                mtu = input.getMaximumTransfertUnit();
                headerOffset = input.getHeadersLength();
                log.out();
	}
}
