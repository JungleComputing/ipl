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

	subInput.setupConnection(cnx);

	mtu = subInput.getMaximumTransfertUnit();
	headerOffset = subInput.getHeadersLength();
// System.err.println(this + ": my subInput " + subInput + " mtu " + mtu);

	log.out();
    }


    public void startReceive() throws IOException {
	subInput.startReceive();
    }

    public void switchToUpcallMode(NetInputUpcall inputUpcall)
	    throws IOException {
	installUpcallFunc(inputUpcall);
	subInput.switchToUpcallMode(this);
    }

    public boolean pollIsInterruptible() throws IOException {
	return subInput.pollIsInterruptible();
    }

    public void setInterruptible(boolean interruptible)
	    throws IOException {
	subInput.setInterruptible(interruptible);
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


    public void doFinish() throws IOException {
	log.in();
// rcveTimer.stop();
	subInput.finish();
	log.out();
    }


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
