package ibis.ipl.impl.net.bytes;

import ibis.io.ArrayOutputStream;
import ibis.io.MantaOutputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The byte conversion output implementation.
 */
public class BytesOutput extends NetOutput {

	/**
	 * The driver used for the 'real' output.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' output.
	 */
        private NetOutput subOutput = null;

        private NetAllocator a2 = new NetAllocator(2, 1024);
        private NetAllocator a4 = new NetAllocator(4, 1024);
        private NetAllocator a8 = new NetAllocator(8, 1024);

        /**
         * Pre-allocation threshold.
         * Note: must be a multiple of 8.
         */
        private int          anThreshold = 8 * 256;
        private NetAllocator an = null;
        

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param output the controlling output.
	 */
	BytesOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os) throws IbisIOException {
		NetOutput subOutput = this.subOutput;
		
		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(rpn, is, os);

		int _mtu = subOutput.getMaximumTransfertUnit();
		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}
 
 		int _headersLength = subOutput.getHeadersLength();
 
 		if (headerOffset < _headersLength) {
 			headerOffset = _headersLength;
 		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IbisIOException {
		subOutput.initSend();
                super.initSend();
	}

        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws IbisIOException{
                super.finish();
                subOutput.finish();
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (subOutput != null) {
			subOutput.free();
			subOutput = null;
		}
		
		subDriver = null;

		super.free();
	}        

        public void writeByteBuffer(NetSendBuffer buffer) throws IbisIOException {
                subOutput.writeByteBuffer(buffer);
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IbisIOException {
                subOutput.writeByte(NetConvert.boolean2byte(value));
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte value) throws IbisIOException {
                subOutput.writeByte(value);                
        }
        
        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IbisIOException {
                byte [] b = a2.allocate();
                NetConvert.writeChar(value, b);
                subOutput.writeArrayByte(b);
                a2.free(b);
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IbisIOException {
                byte [] b = a2.allocate();
                NetConvert.writeShort(value, b);
                subOutput.writeArrayByte(b);
                a2.free(b);                
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IbisIOException {
                byte [] b = a4.allocate();
                NetConvert.writeInt(value, b);
                subOutput.writeArrayByte(b);
                a4.free(b);                
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IbisIOException {
                byte [] b = a8.allocate();
                NetConvert.writeLong(value, b);
                subOutput.writeArrayByte(b);
                a8.free(b);                
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IbisIOException {
                byte [] b = a4.allocate();
                NetConvert.writeFloat(value, b);
                subOutput.writeArrayByte(b);
                a4.free(b);                
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IbisIOException {
                byte [] b = a8.allocate();
                NetConvert.writeDouble(value, b);
                subOutput.writeArrayByte(b);
                a8.free(b);                
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     value             The string value to write.
	 */
        public void writeString(String value) throws IbisIOException {
                subOutput.writeString(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IbisIOException {
                subOutput.writeObject(value);                
        }

        public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                if (userBuffer.length <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayBoolean(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayBoolean(userBuffer));
                }                
        }

        public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
                subOutput.writeArrayByte(userBuffer);
        }

        public void writeArrayChar(char [] userBuffer) throws IbisIOException {
                final int f = 2;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayChar(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayChar(userBuffer));
                }                
        }

        public void writeArrayShort(short [] userBuffer) throws IbisIOException {
                final int f = 2;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayShort(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayShort(userBuffer));
                }                
        }

        public void writeArrayInt(int [] userBuffer) throws IbisIOException {
                final int f = 4;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayInt(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayInt(userBuffer));
                }                
        }

        public void writeArrayLong(long [] userBuffer) throws IbisIOException {
                final int f = 8;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayLong(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayLong(userBuffer));
                }                
        }

        public void writeArrayFloat(float [] userBuffer) throws IbisIOException {
                final int f = 4;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayFloat(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayFloat(userBuffer));
                }                
        }

        public void writeArrayDouble(double [] userBuffer) throws IbisIOException {
                final int f = 8;

                if (userBuffer.length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArrayDouble(userBuffer, b);
                        subOutput.writeSubArrayByte(b, 0, userBuffer.length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArrayDouble(userBuffer));
                }                
        }

        public void writeSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                if (length <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayBoolean(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayBoolean(userBuffer, offset, length));
                }                
        }

        public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayByte(userBuffer, offset, length);                
        }

        public void writeSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 2;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayChar(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayChar(userBuffer, offset, length));
                }
        }

        public void writeSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 2;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayShort(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayShort(userBuffer, offset, length));
                }
        }

        public void writeSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 4;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayInt(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayInt(userBuffer, offset, length));
                }
        }

        public void writeSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 8;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayLong(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayLong(userBuffer, offset, length));
                }
        }

        public void writeSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 4;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayFloat(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayFloat(userBuffer, offset, length));
                }
        }

        public void writeSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 8;

                if ((length*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeSubArrayDouble(userBuffer, offset, length, b);
                        subOutput.writeSubArrayByte(b, 0, length*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeSubArrayDouble(userBuffer, offset, length));
                }
        }	

}
