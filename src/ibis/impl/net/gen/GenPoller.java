package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;

import java.net.InetAddress;
import java.net.Socket;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network input poller.
 */
public class GenPoller extends NetInput {

	/**
	 * The set of inputs.
	 */
	protected Vector    inputVector = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector    = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector    = null;

	/**
	 * The driver used for the inputs.
	 */
	protected NetDriver subDriver   = null;

	/**
	 * The input that was last sucessfully polled, or <code>null</code>.
	 */
	protected NetInput  activeInput = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param input  the controlling input.
	 */
	public GenPoller(StaticProperties staticProperties,
			 NetDriver 	  driver,
			 NetInput  	  input) {
		super(staticProperties, driver, input);
		inputVector = new Vector();
		isVector    = new Vector();
		osVector    = new Vector();
	}

	/**
	 * Adds a new input to the input set.
	 *
	 * @param input the input.
	 */
	private void addInput(NetInput input) {
		inputVector.add(input);
	}

	/**
	 * Setup a new input.
	 *
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer               rpn,
				    ObjectInputStream     is,
				    ObjectOutputStream    os)
		throws IbisIOException {
		if (subDriver == null) {
			subDriver = driver.getIbis().getDriver(getProperty("Driver"));
		}
		
		NetInput ni = subDriver.newInput(staticProperties, this);

		ni.setupConnection(rpn, is, os);
		addInput(ni);
	}

	/**
	 * Polls the inputs.
	 *
	 * {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {		
		activeInput = null;
		activeNum   = null;
		
		Iterator i = inputVector.listIterator();

	polling_loop:
		while (i.hasNext()) {
			NetInput ni  = (NetInput)i.next();
			Integer  num = null;
			
			if ((num = ni.poll()) != null) {
				activeInput = ni;
				activeNum   = num;
				break polling_loop;
			}
		}
		
		return activeNum;
	}

	/**
	 * Receive a buffer over the active input.
	 *
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException {
		return activeInput.receiveBuffer(expectedLength);
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		super.release();
		activeInput.release();
		activeInput = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		super.reset();
		activeInput.reset();
		activeInput = null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws IbisIOException {
		if (inputVector != null) {
			Iterator i = inputVector.listIterator();

			while (i.hasNext()) {
				NetInput ni = (NetInput)i.next();
				ni.free();
			}
		}
		
		inputVector = null;
		isVector    = null;
		osVector    = null;
		subDriver   = null;
		activeInput =  null;

		super.free();
	}
} 
