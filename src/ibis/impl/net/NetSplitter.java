package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.Socket;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.InetAddress;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network output poller.
 */
public class NetSplitter extends NetOutput {

	/**
	 * The set of outputs.
	 */
	protected Vector    outputVector = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector     = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector     = null;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver    = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output  the controlling output.
	 */
	public NetSplitter (StaticProperties staticProperties,
			    NetDriver 	     driver,
			    NetOutput 	     output) {
		super(staticProperties, driver, output);
		outputVector = new Vector();
		isVector     = new Vector();
		osVector     = new Vector();
	}

	// TODO: the target nodes of the corresponding 
	// sendPort should be told about any change of
	// the header offset
	/**
	 * Adds a new input to the output set.
	 *
	 * The MTU and the header offset is updated by this function.
	 *
	 * @param output the output.
	 */
	private void addOutput(Integer   rpn,
			       NetOutput output) {
		int _mtu = output.getMaximumTransfertUnit();

		if ((mtu == 0)
		    ||
		    (mtu > _mtu)) {
			mtu = _mtu;
		}

		outputVector.add(output);

		int _headersLength = output.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}
	}

	/**
	 * Setup a new output.
	 *
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer                  rpn,
				    ObjectInputStream        is,
				    ObjectOutputStream       os)
		throws IbisIOException {
		if (subDriver == null) {
			subDriver = driver.getIbis().getDriver(getProperty("Driver"));
		}
		
		NetOutput no = subDriver.newOutput(staticProperties, this);
		
		no.setupConnection(rpn, is, os);
		addOutput(rpn, no);
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		Iterator i = outputVector.listIterator();
		while (i.hasNext()) {
			NetOutput no = (NetOutput)i.next();
			no.release();
		}		
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		Iterator i = outputVector.listIterator();
		while (i.hasNext()) {
			NetOutput no = (NetOutput)i.next();
			no.reset();
		}		
	}

	/**
	 * Send a buffer to each peer receive port.
	 */
	public void sendBuffer(NetSendBuffer b)
		throws IbisIOException {
		
		Iterator i = outputVector.listIterator();

		while (i.hasNext()) {
			NetOutput no = (NetOutput)i.next();
			no.sendBuffer(b);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws IbisIOException {
		if (outputVector != null) {
			Iterator i = outputVector.listIterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
			}
		}
		
		outputVector = null;
		isVector     = null;
		osVector     = null;

		super.free();
	}		

}
