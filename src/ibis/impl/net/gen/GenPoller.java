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
	 *
	 * Call this synchronized (this)
	 */
	protected void selectConnection(ReceiveQueue rq) {
                log.in();
                NetInput    input = rq.input();
                log.disp("1");
                mtu = input.getMaximumTransfertUnit();
                log.disp("2");
                headerOffset = input.getHeadersLength();
                log.out();
	}

        /**
         * {@inheritDoc}
         */
        public synchronized void closeConnection(ReceiveQueue rq, Integer num) throws NetIbisException {
                //
        }

}
