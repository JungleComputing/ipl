package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.util.Hashtable;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

/**
 * Provides an abstraction of a network input.
 */
public abstract class NetInput extends NetIO {
	/**
	 * Active {@linkplain NetSendPort send port} number or <code>null</code> if
	 * no send port is active.
	 */
	protected Integer activeNum = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponing
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the input's driver.
	 * @param input the controlling input or <code>null</code> if this input is a
	 *              root input.
	 */
	protected NetInput(StaticProperties staticProperties,
			   NetDriver 	    driver,
			   NetInput  	    input) {
		super(staticProperties, driver, input);
	}

	/**
	 * Blockingly receives a buffer.
	 *
	 * @param expectedLength the number of bytes the caller
	 * would like to receive. It is only a hint, not a hard
	 * constraint. It is needed for drivers that do not have a
	 * MaximumTransfertUnit, in order to determine the size of the
	 * buffer that should be allocated for reception.
	 *
	 * @exception IbisIOException if the reception fails.
	 */
	public abstract NetReceiveBuffer receiveBuffer(int expectedLength)
		throws IbisIOException;

	/**
	 * Unblockingly tests for incoming data.
	 *
	 * Note: if <code>poll</code> is called again immediately
	 * after a successful poll without extracting the message and
	 * {@linkplain #release releasing} the input, the result is
	 * undefined and data might get lost. Use {@link
	 * #getActiveSendPortNum} instead.
	 *
	 * @return the send port's corresponding integer or <code>null</code> if
	 * no data is available.
	 * @exception IbisIOException if the polling fails (!= the
	 * polling is unsuccessful).
	 */
	public abstract Integer poll() throws IbisIOException;

	/**
	 * Returns the active send port related integer or <code>null</code> if
	 * no send port is active.
	 *
	 * @return the send port's corresponding local integer or <code>null</code> if
	 * no send port is active.
	 */
	public final Integer getActiveSendPortNum() {
		return activeNum;
	}

	/**
	 * Releases this input after a message has been extracted.
	 *
	 * Node: methods redefining this one should call it at the beginning.
	 */
	public void release() {
		activeNum = null;
	}

	/**
	 * Releases this input after a message has been extracted.
	 *
	 * Node: methods redefining this one should call it at the beginning.
	 */
	public void reset() {
		activeNum = null;
	}

	/**
	 * Closes this input.
	 *
	 * Node: methods redefining this one should call it at the end.
	 */
	public void free() 
		throws IbisIOException {
		activeNum = null;
		super.free();
	}

	/**
	 * Finalizes this input.
	 *
	 * Node: methods redefining this one should call it at the end.
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}
	
}
