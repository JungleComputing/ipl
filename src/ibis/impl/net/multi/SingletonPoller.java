package ibis.impl.net.multi;

import ibis.impl.net.NetConnection;
import ibis.impl.net.NetDriver;
import ibis.impl.net.NetInput;
import ibis.impl.net.NetInputUpcall;
import ibis.impl.net.NetPortType;

import java.io.IOException;

/**
 * A specialized Poller for the case where we can derive statically from
 * the PortType Properties that there is at most one input.
 */
public class SingletonPoller extends MultiPoller {


    /**
     * The driver used for the inputs.
     */
    protected NetDriver		subDriver   = null;

    /**
     * The subInput
     */
    protected NetInput		subInput;
    protected Lane		singleLane;

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	this(pt, driver, context, true, inputUpcall);
    }

    /**
     * Constructor.
     *
     * @param pt      the port type.
     * @param driver  the driver of this poller.
     * @param context the context string.
     * @param decouplePoller en/disable decoupled message delivery in this class
     * @param inputUpcall the input upcall for upcall receives, or
     *        <code>null</code> for downcall receives
     */
    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   boolean decouplePoller,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	super(pt, driver, context, decouplePoller, inputUpcall);
    }


    /**
     * {@inheritDoc}
     */
    public boolean readBufferedSupported() {
	return subInput.readBufferedSupported();
    }


	protected NetInput newPollerSubInput(Object key, ReceiveQueue q)
	    	throws IOException {
	    NetInput ni = q.getInput();

	    if (ni != null) {
		return ni;
	    }

	    Lane      lane          = (Lane)key;
	    String    subContext    = lane.subContext;
	    String    subDriverName = getProperty(subContext, "Driver");
	    NetDriver subDriver     = driver.getIbis().getDriver(subDriverName);

	    return newSubInput(subDriver, subContext, null);
	}



    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws IOException {

	super.setupConnection(cnx);

	singleLane   = (Lane)laneTable.get(cnx.getNum());
	mtu          = singleLane.mtu;
	headerOffset = singleLane.headerLength;
	subInput     = singleLane.queue.getInput();

	log.out();
    }


    public Integer doPoll(boolean block) throws IOException {
	log.in();
	if (subInput == null) {
	    return null;
	}

	Integer spn = subInput.poll(block);

	log.out();

	return spn;
    }


    /**
     * {@inheritDoc}
     */
    public void doFinish() throws IOException {
	log.in();
// rcveTimer.stop();
	subInput.finish();
	log.out();
    }


    /**
     * {@inheritDoc}
     */
    public void doFree() throws IOException {
	log.in();
	if (subInput != null) {
	    subInput.free();
	}
	log.out();
    }


    protected synchronized void doClose(Integer num) throws IOException {
	log.in();
	if (subInput != null) {
	    subInput.close(num);
	    subInput = null;
	}
	log.out();
    }


    protected NetInput activeInput() throws IOException {
	return subInput;
    }

}
