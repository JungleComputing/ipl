package ibis.ipl.impl.net.pipe;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetBank;
import ibis.ipl.impl.net.NetBufferedInput;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;

import java.util.Hashtable;

/**
 * The PIPE input implementation.
 */
public class PipeInput extends NetBufferedInput {

	/**
	 * Default MTU of the pipe.
	 */
	private int              defaultMtu = 512;

	/**
	 * The peer {@link ibis.ipl.impl.net.NetSendPort NetSendPort}
	 * local number.
	 */
	private Integer          rpn       = null;

	/**
	 * The communication input stream.
	 */
	private PipedInputStream pipeIs    = null;

	/**
	 * The buffer block allocator.
	 */
	private NetAllocator     allocator = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the PIPE driver instance.
	 * @param input the controlling input.
	 */
	PipeInput(StaticProperties sp,
		 NetDriver        driver,
		 NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
		headerLength = 4;
	}

	/*
	 * Sets up an incoming PIPE connection.
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
		mtu      = defaultMtu;
		 
		try {
			pipeIs = new PipedInputStream();
			
			NetBank bank = driver.getIbis().getBank();
			Long    key  = bank.getUniqueKey();
			bank.put(key, pipeIs);
			
			Hashtable info = new Hashtable();
			info.put("pipe_mtu",       new Integer(mtu));
			info.put("pipe_istream_key", key);
			key = null;

			sendInfoTable(os, info);
			int ack = is.read();
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		allocator = new NetAllocator(mtu);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This PIPE polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws IbisIOException {
		activeNum = null;

		if (rpn == null) {
			return null;
		}

		try {
			if (pipeIs.available() > 0) {
				activeNum = rpn;
                                initReceive();
			}
		} catch (IOException e) {
			throw new IbisIOException(e);
		} 

		return activeNum;
	}	


	/*
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: this function may block if the expected data is not there.
	 *
	 * @return {@inheritDoc}
        */
	public NetReceiveBuffer readByteBuffer(int expectedLength)
		throws IbisIOException {
		byte [] b = allocator.allocate();
		int     l = 0;
		
		try {
			int offset = 0;
			do {
				while (pipeIs.available() < 4) {
					Thread.currentThread().yield();
				}
				
				offset += pipeIs.read(b, offset, 4);
			} while (offset < 4);

			l = readInt(b, 0);
			
			do {
				while (pipeIs.available() < (l - offset)) {
					Thread.currentThread().yield();
				}
				
				offset += pipeIs.read(b, offset, l - offset);
			} while (offset < l);
		} catch (Exception e) {
			throw new IbisIOException(e);
		}
		
		return new NetReceiveBuffer(b, l, allocator);
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		try {
			if (pipeIs != null) {
				pipeIs.close();
				pipeIs = null;
			}

			rpn = null;
		}
		catch (Exception e) {
			throw new IbisIOException(e);
		}

		super.free();
	}
}
