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
	 * The driver used for the 'real' output.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' output.
	 */
	private NetOutput subOutput = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param output the controlling output.
	 */
	IdOutput(StaticProperties sp,
		 NetDriver   	  driver,
		 NetOutput   	  output)
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
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		NetOutput subOutput = this.subOutput;
		
		if (subOutput == null) {
			if (subDriver == null) {
				subDriver = driver.getIbis().getDriver(getProperty("Driver"));
			}

			subOutput = subDriver.newOutput(staticProperties, this);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(rpn, is, os);

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
	public void initSend() throws IbisIOException {
		subOutput.initSend();
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
		
		subDriver = null;

		super.free();
	}
	
}
