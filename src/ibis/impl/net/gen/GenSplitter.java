package ibis.ipl.impl.net.gen;

import ibis.ipl.impl.net.*;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Provides a generic multiple network output poller.
 */
public final class GenSplitter extends NetOutput {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected HashMap   outputMap = null;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 */
	public GenSplitter(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
		outputMap = new HashMap();
	}

	/**
	 * Adds a new input to the output set.
	 *
	 * The MTU and the header offset is updated by this function.
	 *
	 * @param output the output.
	 */
	private void addOutput(Integer   rpn,
			       NetOutput output) {
		int _mtu = output.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = output.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}

		outputMap.put(rpn, output);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
		if (subDriver == null) {
			String subDriverName = getProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}
                
		NetOutput no = newSubOutput(subDriver);
		no.setupConnection(cnx);
		addOutput(cnx.getNum(), no);
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws NetIbisException {
                super.initSend();
                
		Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.initSend();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
                super.finish();
		Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws NetIbisException {
		if (outputMap != null) {
			Iterator i = outputMap.values().iterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
                                i.remove();
			}
		}
		
		super.free();
	}		

        public synchronized void close(Integer num) throws NetIbisException {
                if (outputMap != null) {
                        NetOutput no = (NetOutput)outputMap.get(num);
                        no.close(num);
                        outputMap.remove(num);
                }
        }
        


        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByteBuffer(buffer);
		} while (i.hasNext());
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeBoolean(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByte(v);
		} while (i.hasNext());
        }
        
        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeChar(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeShort(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeInt(v);
		} while (i.hasNext());
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeLong(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeFloat(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeDouble(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeString(String v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeString(v);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeObject(v);
		} while (i.hasNext());
        }

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceBoolean(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceByte(b, o, l);
		} while (i.hasNext());
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceChar(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceShort(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceInt(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceLong(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceFloat(b, o, l);
		} while (i.hasNext());
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceDouble(b, o, l);
		} while (i.hasNext());
        }	

        public void writeArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                Iterator i = outputMap.values().iterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArraySliceObject(b, o, l);
		} while (i.hasNext());
        }	
}
