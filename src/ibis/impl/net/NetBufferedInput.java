package ibis.impl.net;

import ibis.ipl.ConnectionClosedException;
import ibis.ipl.IbisConfigurationException;

import java.io.IOException;

/**
 * Provides an abstraction of a buffered network input.
 */
public abstract class NetBufferedInput extends NetInput {

        protected int arrayThreshold = 0;

	/**
	 * The current buffer.
	 */
        private NetReceiveBuffer buffer          = null;

	/**
	 * The current memory block allocator.
	 */
        private NetAllocator     bufferAllocator = null;

	/**
	 * The buffer offset of the payload area.
	 */
	protected int dataOffset = 0;

	/**
	 * The current buffer offset for extracting user data.
	 */
	private int bufferOffset = 0;

        /**
         * Flag used to detect circular references in readByteBuffer default implementations.
         */
        private boolean circularCheck = false;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponing
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the input's driver.
	 * @param input the controlling input or <code>null</code> if this input is a
	 *              root input.
	 */
	protected NetBufferedInput(NetPortType      portType,
                                   NetDriver 	    driver,
                                   String           context) {
		super(portType, driver, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public NetReceiveBuffer createReceiveBuffer(int length) {
                NetReceiveBuffer b = null;

                log.in();
                if (factory == null) {
                        byte[] data = null;
                        if (bufferAllocator == null) {
                                data = new byte[length];
                        } else {
                                data = bufferAllocator.allocate();
                        }
                        b = new NetReceiveBuffer(data, length, bufferAllocator);
                } else {
                        b = (NetReceiveBuffer)createBuffer(length);
                }
                log.out();

                return b;
	}

        /**
         * Optional method for zero-copy reception.
         * Note: at least one 'receiveByteBuffer' method must be implemented.
         */
        protected void receiveByteBuffer(NetReceiveBuffer buffer) throws IOException {
                log.in();
                int offset = dataOffset;
                int length = buffer.length - offset;

                if (circularCheck) {
                        throw new IbisConfigurationException("circular reference");
		}

                circularCheck = true;
                while (length > 0) {
                        NetReceiveBuffer b = receiveByteBuffer(length);
                        int copyLength = Math.min(length, b.length);
                        System.arraycopy(b.data, 0, buffer.data, dataOffset, copyLength);
                        offset += copyLength;
                        length -= copyLength;
                        b.free();
                }
                circularCheck = false;

                log.out();
        }

        /**
         * Optional method for static buffer reception.
         * Note: at least one 'receiveByteBuffer' method must be implemented.
         */
        protected NetReceiveBuffer receiveByteBuffer(int expectedLength) throws IOException {
                log.in();
                NetReceiveBuffer b = null;

                if (circularCheck) {
                        throw new IbisConfigurationException("circular reference");
		}

                circularCheck = true;
                if (mtu != 0) {
                        b = createReceiveBuffer(mtu, 0);
                } else {
                        //b = createReceiveBuffer(dataOffset + expectedLength, 0);
                        int l = dataOffset + expectedLength;
                        b = createReceiveBuffer(l, l);
                }

                receiveByteBuffer(b);
                circularCheck = false;

                log.out();

                return b;
        }



        protected void initReceive(Integer num) throws IOException {
                log.in();
                if (mtu != 0) {
			if (bufferAllocator == null || bufferAllocator.getBlockSize() != mtu) {
				bufferAllocator = new NetAllocator(mtu);
			}
		}

                if (factory == null) {
                        factory = new NetBufferFactory(mtu, new NetReceiveBufferFactoryDefaultImpl(), bufferAllocator);
                } else {
                        factory.setMaximumTransferUnit(mtu);
                }

                dataOffset = getHeadersLength();
                log.out();
        }

        private void pumpBuffer(NetReceiveBuffer buffer) throws IOException {
                log.in();
                receiveByteBuffer(buffer);
                buffer.free();
                log.out();
        }

	private void pumpBuffer(int length) throws IOException {
                log.in();
		buffer       = receiveByteBuffer(dataOffset+length);
                if (buffer == null) {
                        throw new ConnectionClosedException("connection closed");
                }

		bufferOffset = dataOffset;
                log.out();
	}

	protected void freeBuffer() {
                log.in();
		if (buffer != null) {
			buffer.free();
			buffer       = null;
                        bufferOffset =    0;
		}
                log.out();
	}

	public void doFinish() throws IOException {
                log.in();
 		freeBuffer();
                log.out();
	}

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IOException {
                log.in();
                freeBuffer();
                NetReceiveBuffer b = receiveByteBuffer(expectedLength);
                log.out();

                return b;
        }


        public void readByteBuffer(NetReceiveBuffer b) throws IOException {
                log.in();
                freeBuffer();
                receiveByteBuffer(b);
                log.out();
        }


	public byte readByte() throws IOException {
                log.in();
		byte value = 0;
// System.err.println(this + ": Wanna read one byte...");
// Thread.dumpStack();

		if (buffer == null) {
			pumpBuffer(1);
		}

		value = buffer.data[bufferOffset++];

		if ((buffer.length - bufferOffset) == 0) {
			freeBuffer();
		}

// System.err.println(this + ": Read one byte=" + value + " = '" + (char)value + "'");
                // log.disp("OUT value = "+value);
                log.disp("OUT");
                log.out();

		return value;
	}

	public void readArray(byte [] userBuffer,
			      int     offset,
			      int     length)
		throws IOException {
                log.in();
		if (length == 0)
			return;

                if (dataOffset == 0 && length > arrayThreshold) {
                        if (buffer != null) {
                                freeBuffer();
                        }

			// Here, the NetReceiveBuffer provides a view into a
			// pre-existing Buffer at a varying offset. For that,
			// we cannot use the BufferFactory.
                        if (mtu != 0) {
                                do {
                                        int copyLength = Math.min(mtu, length);
                                        pumpBuffer(new NetReceiveBuffer(userBuffer, offset, copyLength));
                                        offset += copyLength;
                                        length -= copyLength;
                                } while (length != 0);
                        } else {
                                pumpBuffer(new NetReceiveBuffer(userBuffer, offset, length));
                        }
                } else {
                        while (length > 0) {
                                if (buffer == null) {
                                        pumpBuffer(length);
                                }

                                int bufferLength = buffer.length - bufferOffset;
                                int copyLength   = Math.min(bufferLength, length);

                                System.arraycopy(buffer.data, bufferOffset, userBuffer, offset, copyLength);

                                bufferOffset += copyLength;
                                bufferLength -= copyLength;
                                offset       += copyLength;
                                length       -= copyLength;

                                if (bufferLength == 0) {
                                        freeBuffer();
                                }
                        }
                }

		log.out();
	}

}
