package ibis.ipl.impl.net;

import ibis.io.ArrayOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * The ID output implementation.
 */
public abstract class NetSerializedOutput extends NetOutput {

	/**
	 * The driver used for the 'real' output.
	 */
	protected NetDriver subDriver = null;

	/**
	 * The 'real' output.
	 */
	protected NetOutput subOutput = null;

        private SerializationOutputStream outputSerializationStream = null;

        private boolean needFlush = false;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param output the controlling output.
	 */
	public NetSerializedOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
		NetOutput subOutput = this.subOutput;
                try {
		
		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(rpn, is, os, nls);

		int _mtu = subOutput.getMaximumTransfertUnit();
		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}
 
 		int _headersLength = subOutput.getHeadersLength();
 
 		if (headerOffset < _headersLength) {
 			headerOffset = _headersLength;
 		}

                /*
                 * Need to re-initialize the serialization stream state
                 * in order to ensure consistency between multiple receivers.
                 */
                outputSerializationStream = null;
		} catch (Exception e) {
                        throw new Error(e);
                }
	}

        public abstract SerializationOutputStream newSerializationOutputStream() throws IbisIOException;
        

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IbisIOException {
                super.initSend();
                subOutput.initSend();
                if (outputSerializationStream == null) {
                        subOutput.writeByte((byte)1);
                        outputSerializationStream = newSerializationOutputStream();
                } else {
                        subOutput.writeByte((byte)0);
                }                
	}

        private void flushStream() throws java.io.IOException {
                if (needFlush) {
                        outputSerializationStream.flush();
                        needFlush = false;
                }
        }
        

        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws IbisIOException {
	    try {
                flushStream();
                outputSerializationStream.reset();
                super.finish();
                subOutput.finish();
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
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
	    try {
                flushStream();
                subOutput.writeByteBuffer(buffer);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeBoolean(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeByte(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }
        
        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeChar(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeShort(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeInt(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeLong(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeFloat(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeDouble(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     value             The string value to write.
	 */
        public void writeString(String value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeObject(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeObject(value);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceBoolean(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceByte(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceChar(char [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceChar(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceShort(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceInt(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceLong(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceFloat(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceDouble(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }	

        public void writeArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
	    try {
                needFlush = true;
                outputSerializationStream.writeArraySliceObject(b, o, l);
	    } catch(java.io.IOException e) {
		throw new IbisIOException("got exception", e);
	    }
        }	

}
