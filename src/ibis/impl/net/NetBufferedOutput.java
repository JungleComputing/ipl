package ibis.ipl.impl.net;

import ibis.ipl.StaticProperties;

import java.io.IOException;

/**
 * Provides an abstraction of a buffered network output.
 */
public abstract class NetBufferedOutput extends NetOutput {

        protected int arrayThreshold = 0;

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
                        factory.setMaximumTransferUnit(mtu);
                }
                        //}
                log.out();
        }


	/**
	 * Sends the current buffer over the network.
	 */
	protected void flush() throws IOException {
                log.in();
// System.err.println(this + ": in flush(), buffer " + buffer);
		if (buffer != null) {
                        stat.addBuffer(buffer.length);
			sendByteBuffer(buffer);
			buffer = null;
                        bufferOffset = 0;
		}
                log.out();
	}

	/**
	 * Allocate a new buffer.
	 *
	 * @param the preferred length. This is just a hint. The
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
// System.err.println(this + ": allocated new buffer " + buffer);
// Thread.dumpStack();

		buffer.length = dataOffset;
		bufferOffset = dataOffset;
                log.out();
	}

	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws IOException{
                log.in();
                super.send();
                log.out();
	}

	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IOException{
                log.in();
                super.finish();
                flush();
                stat.end();
                log.out();
	}

	/**
	 * Unconditionaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws IOException {
                log.in();
                flush();
                super.reset(doSend);
                log.out();
	}

        public void writeByteBuffer(NetSendBuffer b) throws IOException {
                log.in();
                flush();
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
// System.err.println("Write one byte=" + value + " = '" + (char)value + "'");
// Thread.dumpStack();

		if (buffer == null) {
			allocateBuffer(1);
		}

		buffer.data[bufferOffset] = value;
		buffer.length++;
		bufferOffset++;

// System.err.println("bufferOffset " + bufferOffset + " buffer.data.length " + buffer.data.length);
		if (bufferOffset >= buffer.data.length) {
			flush();
		}
		log.out();
	}

	public void writeArray(byte [] userBuffer, int offset, int length) throws IOException {
                log.in();
		if (length == 0)
			return;

                if (dataOffset == 0 && length > arrayThreshold) {
                        flush();

			// Here, the NetReceiveBuffer provides a view into a
			// pre-existing Buffer at a varying offset. For that,
			// we cannot use the BufferFactory.
                        if (mtu != 0) {
                                do {
                                        int copyLength = Math.min(mtu, length);
                                        buffer = new NetSendBuffer(userBuffer, offset, copyLength);
// System.err.println(this + ": created new buffer " + buffer);
// Thread.dumpStack();
                                        flush();

                                        offset += copyLength;
                                        length -= copyLength;
                                } while (length != 0);

                        } else {
                                buffer = new NetSendBuffer(userBuffer, offset + length);
// System.err.println(this + ": created new buffer " + buffer);
// Thread.dumpStack();
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
                log.out();
        }
}
