package ibis.ipl.impl.net;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

/**
 * Provides an abstraction of a buffered network input.
 */
public abstract class NetBufferedInput extends NetInput {

	/**
	 * The current buffer.
	 */
        private NetReceiveBuffer buffer          = null;

	/**
	 * The current memory block allocator.
	 */
        private NetAllocator     bufferAllocator = null;

	/**
	 * The current buffer offset of the payload area.
	 */
	private int dataOffset = 0;

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
	protected NetBufferedInput(StaticProperties staticProperties,
			   NetDriver 	    driver,
			   NetInput  	    input) {
		super(staticProperties, driver, input);
	}

        /**
         * Optional method for zero-copy reception.
         * Note: at least one 'readByteBuffer' method must be implemented.
         */
        protected void readByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                int offset = buffer.base;
                int length = buffer.length - offset;

                if (circularCheck)
                        throw new IbisIOException("circular reference");

                circularCheck = true;
                while (length > 0) {
                        NetReceiveBuffer b = readByteBuffer(length);
                        int copyLength = Math.min(length, b.length);
                        System.arraycopy(b.data, 0, buffer.data, buffer.base, copyLength);
                        offset += copyLength;
                        length -= copyLength;
                        b.free();
                }
                circularCheck = false;
        }

        /**
         * Optional method for static buffer reception.
         * Note: at least one 'readByteBuffer' method must be implemented.
         */
        protected NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                NetReceiveBuffer b = null;
                
                if (circularCheck)
                        throw new IbisIOException("circular reference");

                circularCheck = true;
                if (mtu != 0) {
                        b = new NetReceiveBuffer(bufferAllocator.allocate(), 0, bufferAllocator);
                } else {
                        b = new NetReceiveBuffer(new byte[expectedLength], 0);
                }
                
                readByteBuffer(b);
                circularCheck = false;
                return b;
        }
        
                
        
        protected void initReceive() {
                //System.err.println("initReceive -->");
                if (mtu != 0) {
			if (bufferAllocator == null || bufferAllocator.getBlockSize() != mtu) {
				bufferAllocator = new NetAllocator(mtu);
			}
		}
  
                dataOffset = getHeadersLength();
                //System.err.println("initReceive <--");
        }

        private void pumpBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                readByteBuffer(buffer);
                buffer.free();
        }

	private void pumpBuffer(int length) throws IbisIOException {
		buffer       = readByteBuffer(length);
		bufferOffset = dataOffset;
	}

	private void freeBuffer() {
		if (buffer != null) {
			buffer.free();
			buffer = null;
		}
		
		bufferOffset =    0;
	}

	public void finish() throws IbisIOException {
                super.finish();
 		freeBuffer();
	}

	public byte readByte() throws IbisIOException {
                //System.err.println("readbyte -->");
		byte value = 0;

		if (buffer == null) {
			pumpBuffer(1);
		}

		value = buffer.data[bufferOffset++];

		if ((buffer.length - bufferOffset) == 0) {
			freeBuffer();
		}
		
                //System.err.println("readbyte <--");

		return value;
	}
	
	public void readArrayByte(byte [] userBuffer)
		throws IbisIOException {
		readSubArrayByte(userBuffer, 0, userBuffer.length);
	}

	public void readSubArrayByte(byte [] userBuffer,
				     int     offset,
				     int     length)
		throws IbisIOException {
		//System.err.println("read: "+offset+", "+length);
		if (length == 0)
			return;

                if (dataOffset == 0) {
                        if (buffer != null) {
                                freeBuffer();
                        }

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
                        if (buffer != null) {
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

                        while (length > 0) {
                                pumpBuffer(length);

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
                
		//System.err.println("read: "+offset+", "+length+": ok");
	}
}
