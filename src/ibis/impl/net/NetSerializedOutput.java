package ibis.ipl.impl.net;

import ibis.io.ArrayOutputStream;
import ibis.io.SerializationOutputStream;

import ibis.ipl.Replacer;

import ibis.ipl.impl.net.*;

import java.io.IOException;

/**
 * The ID output implementation.
 */
public abstract class NetSerializedOutput extends NetOutput {

	/**
	 * The driver used for the 'real' output.
	 */
	protected NetDriver                 subDriver = null;

	/**
	 * The 'real' output.
	 */
	protected NetOutput                 subOutput = null;

        private   SerializationOutputStream oss       = null;

        private   Replacer                  replacer  = null;

        private   boolean                   needFlush = false;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param output the controlling output.
	 */
	public NetSerializedOutput(NetPortType pt, NetDriver driver, String context) throws IOException {
		super(pt, driver, context);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws IOException {
		NetOutput subOutput = this.subOutput;

		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

                replacer = cnx.getReplacer();

		subOutput.setupConnection(cnx);

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
                oss = null;
	}

        public abstract SerializationOutputStream newSerializationOutputStream() throws IOException;


	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IOException {
                super.initSend();
                subOutput.initSend();
                if (oss == null) {
                        subOutput.writeByte((byte)1);
                        oss = newSerializationOutputStream();
                        if (replacer != null) oss.setReplacer(replacer);
                } else {
                        subOutput.writeByte((byte)0);
			oss.reset();
                }
	}

        private void flushStream() throws IOException {
                if (needFlush) {
                        oss.flush();
                        needFlush = false;
                }
        }


        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems.
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws IOException {
	    super.finish();
	    flushStream();
	    subOutput.finish();
        }

        public synchronized void close(Integer num) throws IOException {
                if (subOutput != null) {
                        subOutput.close(num);
                }
        }


	/**
	 * {@inheritDoc}
	 */
	public void free() throws IOException {
		if (subOutput != null) {
			subOutput.free();
		}

		super.free();
	}

        public void writeByteBuffer(NetSendBuffer buffer) throws IOException {
	    flushStream();
	    subOutput.writeByteBuffer(buffer);
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IOException {
	    needFlush = true;
	    oss.writeBoolean(value);
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte value) throws IOException {
	    needFlush = true;
	    oss.writeByte(value);
        }

        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IOException {
	    needFlush = true;
	    oss.writeChar(value);
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IOException {
	    needFlush = true;
	    oss.writeShort(value);
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IOException {
	    needFlush = true;
	    oss.writeInt(value);
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IOException {
	    needFlush = true;
	    oss.writeLong(value);
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IOException {
	    needFlush = true;
	    oss.writeFloat(value);
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IOException {
	    needFlush = true;
	    oss.writeDouble(value);
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     value             The string value to write.
	 */
        public void writeString(String value) throws IOException {
	    needFlush = true;
	    oss.writeObject(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IOException {
	    needFlush = true;
	    oss.writeObject(value);
        }

        public void writeArray(boolean [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(byte [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(char [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(short [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(int [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(long [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(float [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(double [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

        public void writeArray(Object [] b, int o, int l) throws IOException {
	    needFlush = true;
	    oss.writeArray(b, o, l);
        }

}
