package ibis.ipl.impl.net;

import ibis.ipl.StaticProperties;

/**
 * Provides an abstraction of a buffered network output.
 */
public abstract class NetBufferedOutput extends NetOutput {

	/**
	 * The current buffer offset after the headers of the lower layers
	 * into the payload area.
	 */
	protected int dataOffset = 0;

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
	protected NetBufferedOutput(NetPortType      portType,
			   NetDriver 	    driver,
			   NetIO  	    up,
                           String           context) {
		super(portType, driver, up, context);
	}

        protected abstract void sendByteBuffer(NetSendBuffer buffer) throws NetIbisException;

        public void initSend() throws NetIbisException {
		dataOffset = getHeadersLength();

                if (mtu != 0) {
		    if (factory == null) {
			factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
		    } else {
			factory.setMaximumTransferUnit(mtu);
		    }
		}
        }
        

	/**
	 * Sends the current buffer over the network.
	 */
	protected void flush() throws NetIbisException {
                //System.err.println("NetBufferedOutput: flush -->");
		if (buffer != null) {
                        //System.err.println(this + ": flushing buffer, "+buffer.length+" bytes");
			sendByteBuffer(buffer);
			buffer = null;
                        bufferOffset = 0;
		}
                //System.err.println("NetBufferedOutput: flush <--");
	}

	/**
	 * Allocate a new buffer.
	 *
	 * @param the preferred length. This is just a hint. The
	 * actual buffer length may differ.
	 */
	private void allocateBuffer(int length) throws NetIbisException {
		if (buffer != null) {
			buffer.free();
		}

		if (mtu != 0) {
			buffer = createSendBuffer();
		} else {
			buffer = createSendBuffer(dataOffset + length);
		}		

		buffer.length = dataOffset;
		bufferOffset = dataOffset;
		//System.err.println(this + ": allocate buffer payload=" + length + " mtu=" + mtu + " dataOffset=" + dataOffset + " buffer.length=" + buffer.length);
	}

	// TODO: ensure that send is non-blocking
	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws NetIbisException{
                super.send();
	}

	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws NetIbisException{
                //System.err.println("NetBufferedOutput: finish-->");
                super.finish();
                flush();
                //System.err.println("NetBufferedOutput: finish<--");
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws NetIbisException {
                flush();
                super.reset(doSend);
	}

        public void writeByteBuffer(NetSendBuffer b) throws NetIbisException {
                flush();
                sendByteBuffer(b);
        }


	/**
	 * Appends a byte to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param value the byte to append to the message.
	 */
	public void writeByte(byte value) throws NetIbisException {
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

	public void writeArraySliceByte(byte [] userBuffer, int offset, int length) throws NetIbisException {
		//System.err.println(this + ": write: "+offset+", "+length);
//System.err.println(this + ": offset " + offset + " length " + length + " mtu " + mtu + " buffer " + buffer + " dataOffset " + dataOffset);
		if (length == 0)
			return;
		
                if (dataOffset == 0) {
                        flush();

			// Here, the NetReceiveBuffer provides a view into a
			// pre-existing Buffer at a varying offset. For that,
			// we cannot use the BufferFactory.
                        if (mtu != 0) {
                                do {
                                        int copyLength = Math.min(mtu, length);
                                        buffer = new NetSendBuffer(userBuffer, offset, copyLength);
                                        flush();

                                        offset += copyLength;
                                        length -= copyLength;
                                } while (length != 0);
                                        
                        } else {
                                buffer = new NetSendBuffer(userBuffer, offset + length);
                                flush();
                        }

                } else {
                        while (length > 0) {
				if (buffer == null) {
					allocateBuffer(length);
				}

                                int availableLength = buffer.data.length - bufferOffset;
                                int copyLength   = Math.min(availableLength, length);

                                System.arraycopy(userBuffer, offset, buffer.data, bufferOffset, copyLength);

                                bufferOffset  	+= copyLength;
                                buffer.length  	+= copyLength;
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
