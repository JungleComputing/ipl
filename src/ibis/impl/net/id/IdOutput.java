package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

/**
 * The ID output implementation.
 */
public class IdOutput extends NetOutput {

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer                  rpn 	   = null;

	/**
	 * The communication input stream.
	 *
	 * Note: this stream is not really needed but may be used for debugging
	 *       purpose.
	 */
	private InputStream  	         idIs	   = null;

	/**
	 * The communication output stream.
	 */
	private OutputStream 	         idOs	   = null;

	/**
	 * The driver used for the 'real' output.
	 */
	private NetDriver                subDriver = null;

	/**
	 * The 'real' output.
	 */
	private NetOutput                subOutput = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param output the controlling output.
	 */
	IdOutput(StaticProperties sp,
		  NetDriver   	   driver,
		  NetOutput   	   output)
		throws IbisIOException {
		super(sp, driver, output);
	}

	/*
	 * Sets up an outgoing ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                  rpn,
				    ObjectInputStream 	     is,
				    ObjectOutputStream	     os)
		throws IbisIOException {
		if (this.subOutput != null)
			__.abort__("already connected");

		this.rpn = rpn;

		if (subDriver == null) {
			subDriver = driver.getIbis().getDriver(getProperty("Driver"));
		}

		NetOutput subOutput = subDriver.newOutput(staticProperties, this);
		subOutput.setupConnection(rpn, is, os);
		this.subOutput = subOutput;

		int _mtu = subOutput.getMaximumTransfertUnit();

		if ((mtu == 0)
		    ||
		    (mtu > _mtu)) {
			mtu = _mtu;
		}

		int _headersLength = subOutput.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendBuffer(NetSendBuffer b) throws IbisIOException {
		subOutput.sendBuffer(b);
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		subOutput.release();
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		subOutput.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (subOutput != null) {
			subOutput.free();
			subOutput = null;
		}
		
		rpn       = null;
		idIs      = null;
		idOs      = null;
		subDriver = null;

		super.free();
	}
	
}
