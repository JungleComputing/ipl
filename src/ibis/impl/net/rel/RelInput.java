package ibis.ipl.impl.net.rel;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The REL input implementation.
 */
public class RelInput extends NetBufferedInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The communication input.
	 */
	private NetInput  subInput  = null;

	/**
	 * The acknowledgement output.
	 */
	private NetOutput subOutput = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the REL driver instance.
	 * @param input the controlling input.
	 */
	RelInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);

		// The length of the header expressed in bytes
		headerLength = 8;
	}

	/*
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os) throws IbisIOException {

                /* Main connection */
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver);
			this.subInput = subInput;
		}
		
		subInput.setupConnection(rpn, is, os);

                /* Reverse connection */
		NetOutput subOutput = this.subOutput;
                if (subOutput == null) {
                        subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
                }

		subOutput.setupConnection(new Integer(-1), is, os);
	}

	/**
	 * {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll();
                if (result != null) {
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                        initReceive();
                }

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
	 */
	public NetReceiveBuffer receiveByteBuffer(int expectedLength) throws IbisIOException {
                NetReceiveBuffer b = subInput.readByteBuffer(expectedLength);
                long seq = NetConvert.readLong(b.data, headerOffset);
                return b;
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		super.finish();
		subInput.finish();
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
