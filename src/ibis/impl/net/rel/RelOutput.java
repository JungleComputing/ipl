package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The REL output implementation.
 */
public final class RelOutput extends NetBufferedOutput {

	/**
	 * The driver used for the 'real' output.
	 */
	private NetDriver subDriver = null;

	/**
	 * The communication output.
	 */
	private NetOutput subOutput = null;

	/**
	 * The acknowledgement input.
	 */
	private NetInput subInput = null;

        private long     sequenceNumber = 0;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the REL driver instance.
	 * @param output the controlling output.
	 */
	RelOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);

		// The length of the header expressed in bytes
		headerLength = 8;
	}

	/*
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
                /* Main connection */
		NetOutput subOutput = this.subOutput;
		
		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(rpn, is, os, nls);

		int _mtu = subOutput.getMaximumTransfertUnit();
		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

 		int _headersLength = subOutput.getHeadersLength();
 
 		if (headerOffset < _headersLength) {
 			headerOffset = _headersLength;
 		}

                /* Reverse connection */
		NetInput subInput = this.subInput;
                if (subInput == null) {
                        subInput = newSubInput(subDriver);
			this.subInput = subInput;
                }

		subInput.setupConnection(new Integer(-1), is, os, nls);
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IbisIOException {
                super.initSend();
		subOutput.initSend();
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendByteBuffer(NetSendBuffer b) throws IbisIOException {
                NetConvert.writeLong(sequenceNumber++, b.data, headerOffset);
                subOutput.writeByteBuffer(b);
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		super.finish();
		subOutput.finish();
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
