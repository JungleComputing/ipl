package ibis.ipl.impl.net;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

/**
 * Provides an abstraction of a buffered network output.
 */
public abstract class NetBufferedOutput extends NetOutput {
	/**
	 * The current memory block allocator.
	 */
	private NetAllocator bufferAllocator = null;

	/**
	 * The current buffer offset of the payload area.
	 */
	private int dataOffset = 0;

	/**
	 * The current buffer offset for appending user data.
	 */
	private int bufferOffset = 0;

	/**
	 * The current buffer.
	 */
	private NetSendBuffer buffer = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponding
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the output's driver.
	 * @param output the controlling output or <code>null</code>
	 *               if this output is a root output.
	 */
	protected NetBufferedOutput(StaticProperties staticProperties,
                                    NetDriver 	     driver,
                                    NetOutput 	     output) {
		super(staticProperties, driver, output);
	}

        protected abstract void writeByteBuffer(NetSendBuffer buffer) throws IbisIOException;

        public void initSend() throws IbisIOException {
                if (mtu != 0) {
			if (bufferAllocator == null || bufferAllocator.getBlockSize() != mtu) {
				bufferAllocator = new NetAllocator(mtu);
			}
		}
                
		dataOffset = getHeadersLength();
        }
        

	/**
	 * Sends the current buffer over the network.
	 */
	private void flush() throws IbisIOException {
                //System.err.println("NetBufferedOutput: flush -->");
		if (buffer != null) {
                        //System.err.println("NetBufferedOutput: flushing buffer");
			writeByteBuffer(buffer);
			buffer.free();
			buffer = null;
		}

		bufferOffset = 0;
                //System.err.println("NetBufferedOutput: flush <--");
	}

	/**
	 * Allocate a new buffer.
	 *
	 * @param the preferred length. This is just a hint. The
	 * actual buffer length may differ.
	 */
	private void allocateBuffer(int length) {
		if (buffer != null) {
			buffer.free();
		}
		
		if (bufferAllocator != null) {
			buffer = new NetSendBuffer(bufferAllocator.allocate(), dataOffset, bufferAllocator);
		} else {
			if (mtu != 0) {
				length = mtu;
			} else {
				length += dataOffset;
			}		
			buffer = new NetSendBuffer(new byte[length], dataOffset);
		}
		
		bufferOffset = dataOffset;
	}

	// TODO: ensure that send is non-blocking
	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws IbisIOException{
                super.send();
	}

	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IbisIOException{
                super.finish();
                flush();
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws IbisIOException {
                flush();
                super.reset(doSend);
	}

	/**
	 * Appends a byte to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param value the byte to append to the message.
	 */
	public void writeByte(byte value) throws IbisIOException {
		//System.err.println("writebyte -->");
		if (buffer == null) {
			allocateBuffer(1);
		}

		buffer.data[bufferOffset] = value;
		buffer.length++;
		bufferOffset++;

		if (bufferOffset >= buffer.data.length) {
			flush();
		}
		//System.err.println("writebyte <--");
	}

	/**
	 * Appends a byte array to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param userBuffer the byte array to append to the message.
	 */
	public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
		writeSubArrayByte(userBuffer, 0, userBuffer.length);
	}

	public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
		//System.err.println("write: "+offset+", "+length);
		if (length == 0)
			return;
		
                if (dataOffset == 0) {
                        if (buffer != null) {
                                flush();
                        }

                        if (mtu != 0) {
                                int base = offset;

                                do {
                                        int copyLength = Math.min(mtu, length);
                                        buffer = new NetSendBuffer(userBuffer, base, copyLength);
                                        flush();

                                        base   += copyLength;
                                        length -= copyLength;
                                } while (length != 0);
                                        
                        } else {
                                buffer = new NetSendBuffer(userBuffer, offset, length);
                                flush();
                        }
                } else {
                        if (buffer != null) {
                                int availableLength = buffer.data.length - bufferOffset;
                                int copyLength      = Math.min(availableLength, length);

                                System.arraycopy(userBuffer, offset, buffer.data, bufferOffset, copyLength);

                                bufferOffset  	 += copyLength;
                                buffer.length 	 += copyLength;
                                availableLength  -= copyLength;
                                offset        	 += copyLength;
                                length        	 -= copyLength;

                                if (availableLength == 0) {
                                        flush();
                                }
                        }
		
                        while (length > 0) {
                                allocateBuffer(length);

                                int availableLength = buffer.data.length - bufferOffset;
                                int copyLength   = Math.min(availableLength, length);

                                System.arraycopy(userBuffer, offset, buffer.data, bufferOffset, copyLength);

                                bufferOffset  	+= copyLength;
                                buffer.length 	+= copyLength;
                                availableLength -= copyLength;
                                offset        	+= copyLength;
                                length        	-= copyLength;

                                if (availableLength == 0) {
                                        flush();
                                }
                        }
                }
                
		//System.err.println("write: "+offset+", "+length+": ok");
	}
}
