package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4 
import java.net.SocketTimeoutException;
*/
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public class IdInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param input the controlling input.
	 */
	IdInput(StaticProperties sp,
		NetDriver        driver,
		NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
	}

	/*
	 * Sets up an incoming ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                rpn,
				    ObjectInputStream 	   is,
				    ObjectOutputStream	   os)
		throws IbisIOException {
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
				subDriver = driver.getIbis().getDriver(getProperty("Driver"));
			}

			subInput = subDriver.newInput(staticProperties, this);
			this.subInput = subInput;
		}
		
		subInput.setupConnection(rpn, is, os);
		 
		int _mtu = subInput.getMaximumTransfertUnit();

		if ((mtu == 0)
		    ||
		    (mtu > _mtu)) {
			mtu = _mtu;
		}

		int _headersLength = subInput.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This ID polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		return (subInput != null) ? subInput.poll() : null;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException {

		return subInput.receiveBuffer(expectedLength);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public void receiveBuffer(NetReceiveBuffer buffer)
		throws IbisIOException {
		subInput.receiveBuffer(buffer);
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		super.release();
		subInput.release();
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		super.reset();
		subInput.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (subInput != null) {
			subInput.free();
			subInput = null;
		}

		subDriver = null;
		
		super.free();
	}
	
}
