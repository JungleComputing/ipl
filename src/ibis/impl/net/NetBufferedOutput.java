package ibis.impl.net;

import java.io.IOException;

/**
 * Provides an abstraction of a buffered network output.
 */
public abstract class NetBufferedOutput extends NetOutput implements NetBufferedOutputSupport {

        protected int arrayThreshold = 0;

	/**
	 * The current buffer offset after the headers of the lower layers
	 * into the payload area.
	 */
	protected int dataOffset = 0;

	/**
	 * The current buffer offset for appending user data.
	 */
	// Use buffer.length. bufferOffset is superfluous.
	// private int bufferOffset = 0;

	/**
	 * The current buffer.
	 */
	private NetSendBuffer buffer = null;

	/**
	 * @param portType the {@link ibis.impl.net.NetPortType NetPortType}.
	 * @param driver the driver.
	 * @param context the context.
	 */
	protected NetBufferedOutput(NetPortType portType,
                                    NetDriver 	driver,
                                    String      context) {
		super(portType, driver, context);
	}

        protected abstract void sendByteBuffer(NetSendBuffer buffer) throws IOException;

        public void initSend() throws IOException {
                log.in();
                stat.begin();
		super.initSend();	// Need this for GM, what would it break RFHH?
		dataOffset = getHeadersLength();

                //if (mtu != 0) {
                if (factory == null) {
                        factory = new NetBufferFactory(mtu, new NetSendBufferFactoryDefaultImpl());
                } else {
                        mtu = Math.min(mtu, factory.getMaximumTransferUnit());
                        factory.setMaximumTransferUnit(mtu);
                }
                        //}
                log.out();
        }


	/**
	 * Sends the current buffer over the network.
	 */
	public void flushBuffer() throws IOException {
                log.in();
// System.err.println(this + ": in flushBuffer(), buffer " + buffer);
// Thread.dumpStack();
		if (buffer != null) {
// System.err.println(this + ": in flushBuffer(), buffer.length " + buffer.length + " bufferOffset " + buffer.length);
                        stat.addBuffer(buffer.length);
			sendByteBuffer(buffer);
			buffer = null;
			// bufferOffset = 0x654321;
		}
                log.out();
	}

	/**
	 * Allocate a new buffer.
	 *
	 * @param length the preferred length. This is just a hint. The
	 * actual buffer length may differ.
	 */
	private void allocateBuffer(int length) throws IOException {
                log.in();
		if (buffer != null) {
			buffer.free();
		}

		if (mtu != 0) {
			buffer = createSendBuffer();
		} else {
			buffer = createSendBuffer(dataOffset + length);
		}
// System.err.println(this + ": allocated new send buffer " + buffer + " size " + buffer.length + " data size " + buffer.data.length + " dataOffset " + dataOffset);
// Thread.dumpStack();

		buffer.length = dataOffset;
		// bufferOffset = dataOffset;
                log.out();
	}

	/**
	 * Sends what remains to be sent.
	 */
	public int send() throws IOException{
                log.in();
                int retval = super.send();
                log.out();
		return retval;
	}

	/**
	 * Unconditionaly completes the message transmission and
	 * releases the send port.
	 */
	public void reset() throws IOException {
		log.in();
		flushBuffer();
		super.reset();
		log.out();
	}

	/**
	 * Completes the message transmission and releases the send port.
	 */
	public long finish() throws IOException{
                log.in();
                super.finish();
                flushBuffer();
                stat.end();
                log.out();
		// TODO: return byte count of message.
		return 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public void sync(int ticket) throws IOException {
                log.in();
                flushBuffer();
                super.sync(ticket);
                log.out();
	}

        public void writeByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
                flushBuffer();
                stat.addBuffer(b.length);
                sendByteBuffer(b);
                log.out();
        }


	/**
	 * Appends a byte to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param value the byte to append to the message.
	 */
	public void writeByte(byte value) throws IOException {
		log.in();
                // log.disp("IN value = "+value);
                log.disp("IN");
// System.err.println(this + ": Write one byte=" + value + " = '" + (char)value + "'");
// Thread.dumpStack();

		if (buffer == null) {
			allocateBuffer(1);
		}

		// buffer.data[bufferOffset] = value;
		buffer.data[buffer.length] = value;
		buffer.length++;
		// bufferOffset++;

// System.err.println("bufferOffset " + buffer.length + " buffer.data.length " + buffer.data.length);
		if (buffer.length >= buffer.data.length) {
			flushBuffer();
		}
		log.out();
	}

	public boolean writeBufferedSupported() {
	    return true;
	}

	public void writeBuffered(byte[] userBuffer, int offset, int length)
		throws IOException {
	    log.in();
// System.err.print(this + ": Write buffer[" + length + "] = ("); for (int i = offset, n = Math.min(offset + length, offset + 32); i < n; i++) System.err.print("0x" + Integer.toHexString(userBuffer[i] & 0xFF) + " "); System.err.println(")");
// System.err.println("dataOffset " + dataOffset + " arrayThreshold " + arrayThreshold + " mtu " + mtu + " buffer " + buffer + " buffer.length " + (buffer != null ? buffer.length : -1));
// Thread.dumpStack();
	    if (length == 0) {
		return;
	    }

	    // int mtu = 1 * 1024;

	    if (dataOffset != 0 || length <= arrayThreshold) {

		while (length > 0) {
		    if (buffer == null) {
			allocateBuffer(length);
		    }

		    int availableLength = buffer.data.length - buffer.length;
		    int copyLength      = Math.min(availableLength, length);
// System.err.println("Now copy " + copyLength + " into the buffer");

		    System.arraycopy(userBuffer, offset,
				     buffer.data, buffer.length, copyLength);

		    // bufferOffset    += copyLength;
		    buffer.length   += copyLength;
		    availableLength -= copyLength;
		    offset          += copyLength;
		    length          -= copyLength;

		    if (availableLength == 0) {
			flushBuffer();
		    }
		}

	    } else if (buffer != null
			// && length <= buffer.data.length - bufferOffset
			&& length <= buffer.data.length - buffer.length
			) {

// System.err.println("Take the copy path.. bufferOffset " + buffer.length + " buffer data length " + buffer.data.length);
		System.arraycopy(userBuffer, offset,
				 // buffer.data, bufferOffset,
				 buffer.data, buffer.length,
				 length);
		buffer.length += length;
		// bufferOffset += length;

	    } else if (mtu != 0) {

		flushBuffer();
		// Here, the NetReceiveBuffer provides a view into a
		// pre-existing Buffer at a varying offset. For that,
		// we cannot use the BufferFactory.
		do {
		    int copyLength = Math.min(mtu, length);
		    buffer = new NetSendBuffer(userBuffer, offset, copyLength);
// System.err.println(this + ": mtu/created new buffer " + buffer + " length " + copyLength);
// Thread.dumpStack();

		    flushBuffer();
		    offset += copyLength;
		    length -= copyLength;
		} while (length != 0);

	    } else {

		flushBuffer();
		buffer = new NetSendBuffer(userBuffer, offset + length);
// System.err.println(this + ": created new buffer " + buffer);
// Thread.dumpStack();
	    }

	    log.out();
	}

	public void writeArray(byte [] userBuffer, int offset, int length) throws IOException {
	    writeBuffered(userBuffer, offset, length);
        }

	protected int available() {
	    // return bufferOffset;
	    return buffer == null ? 0 : buffer.length;
	}
}
