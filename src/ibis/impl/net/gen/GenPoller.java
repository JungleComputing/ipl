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
	 * The set of inputs.
	 */

	/**
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;


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
		if (subDriver == null) {
			String subDriverName = getMandatoryProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}

		NetInput ni = newSubInput(subDriver);

		super.setupConnection(cnx, cnx.getNum(), ni);
	}


	/**
	 * {@inheritDoc}
	 */
	protected void selectInput(Integer spn) throws NetIbisClosedException {
	    System.err.println(this + ": selectInput - setting activeQueue, spn = "+spn);
Thread.dumpStack();
	    activeQueue = (ReceiveQueue)inputTable.get(spn);
	    if (activeQueue == null) {
		    //System.err.println("GenPoller: inputUpcall - setting activeQueue - input closed, spn = "+spn);
		    throw new NetIbisClosedException("connection "+spn+" closed");
	    }
	    System.err.println(this + ": selectInput - setting activeQueue - ok, spn = "+spn);
	}


	/**
	 * {@inheritDoc}
	 */
	protected void selectConnection(ReceiveQueue ni) {
	    NetInput    input = ni.input;

	    mtu = input.getMaximumTransfertUnit();
	    headerOffset = input.getHeadersLength();
	}


	/**
	 * {@inheritDoc}
	 *
	public void finish() throws NetIbisException {
                // System.err.println("GenPoller: finish-->");
System.err.println(this + ": finish msg");
                synchronized(this) {
			activeQueue.finish();
                }
		super.finish();
                //System.err.println("GenPoller: finish<--");
	}
	*/

}
