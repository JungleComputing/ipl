package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.Hashtable;

/**
 * The PIPE output implementation.
 */
public class PipeOutput extends NetBufferedOutput {

	/**
	 * The peer {@link ibis.ipl.impl.net.NetReceivePort NetReceivePort}
	 * local number.
	 */
	private Integer           rpn    = null;

	/**
	 * The communication output stream.
	 */
	private PipedOutputStream pipeOs = null;


	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the PIPE driver instance.
	 * @param output the controlling output.
	 */
	PipeOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
		headerLength = 4;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
		this.rpn = rpn;
	
		try {
			Hashtable info = receiveInfoTable(is);
			mtu  	       = ((Integer) info.get("pipe_mtu")).intValue();
			NetBank          bank   = driver.getIbis().getBank();
			Long             key    = (Long)info.get("pipe_istream_key");
			PipedInputStream pipeIs = (PipedInputStream)bank.discardKey(key);

			pipeOs = new PipedOutputStream(pipeIs);
			os.write(1); // Connection ack
			os.flush();			
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	public void sendByteBuffer(NetSendBuffer b) throws IbisIOException {
		try {
			NetConvert.writeInt(b.length, b.data, 0);
			pipeOs.write(b.data, 0, b.length);
			Thread.currentThread().yield();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (pipeOs != null) {
				pipeOs.close();
				pipeOs = null;
			}
		
			rpn = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}

}
