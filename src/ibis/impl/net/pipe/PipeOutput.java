package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetBank;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import java.util.Hashtable;

/**
 * The PIPE output implementation.
 */
public class PipeOutput extends NetOutput {

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
	PipeOutput(StaticProperties sp,
		   NetDriver   	    driver,
		   NetOutput   	    output)
		throws IbisIOException {
		super(sp, driver, output);
		headerLength = 4;
	}

	/*
	 * Sets up an outgoing PIPE connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		this.rpn = rpn;
	
		try {
			Hashtable info = null;
			info 	       = receiveInfoTable(is);
			mtu  	       = ((Integer) info.get("pipe_mtu")).intValue();

			NetBank          bank   = driver.getIbis().getBank();
			Long             key  = (Long)info.get("pipe_istream_key");
			PipedInputStream pipeIs = (PipedInputStream)bank.discardKey(key);

			pipeOs = new PipedOutputStream(pipeIs);
			os.write(1); // Connection ack
			os.flush();			
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void sendBuffer(NetSendBuffer b) throws IbisIOException {
		try {
			writeInt(b.data, 0, b.length);
			pipeOs.write(b.data, 0, b.length);
			Thread.currentThread().yield();
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 
	}

	/**
	 * {@inheritDoc}
	 */
	public void release() {
		// nothing
	}

	/**
	 * {@inheritDoc}
	 */
	public void reset() {
		// nothing
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
