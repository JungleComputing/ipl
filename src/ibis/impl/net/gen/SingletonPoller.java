package ibis.impl.net.gen;

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
public class SingletonPoller extends GenPoller {


    /**
     * The driver used for the inputs.
     */
    protected NetDriver		subDriver   = null;

    /**
     * The subInput
     */
    protected NetInput		subInput;

    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	this(pt, driver, context, true, inputUpcall);
    }

    public SingletonPoller(NetPortType pt,
			   NetDriver driver,
			   String context,
			   boolean decouplePoller,
			   NetInputUpcall inputUpcall)
	    throws IOException {
	super(pt, driver, context, decouplePoller, inputUpcall);
// System.err.println(this + ": hah, I live");
    }


    /**
     * {@inheritDoc}
     */
    public boolean readBufferedSupported() {
	return subInput.readBufferedSupported();
    }


    /**
     * Actually establish a connection with a remote port.
     *
     * @param cnx the connection attributes.
     * @exception IOException if the connection setup fails.
     */
    public synchronized void setupConnection(NetConnection cnx)
	    throws IOException {
	log.in();

	if (subDriver == null) {
	    String subDriverName = getMandatoryProperty("Driver");
	    subDriver = driver.getIbis().getDriver(subDriverName);
	}

	subInput = newSubInput(subDriver, null);

	if (decouplePoller) {
	    /*
	     * If our subclass is a multiplexer, it starts all necessary
	     * upcall threads. Then we do not want an upcall thread in
	     * this class.
	     */
	} else {
// System.err.println(this + ": start upcall thread");
	    startUpcallThread();
	}

	subInput.setupConnection(cnx);

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
