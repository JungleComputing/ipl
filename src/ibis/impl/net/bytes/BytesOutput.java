package ibis.ipl.impl.net.bytes;

import ibis.ipl.impl.net.*;

/**
 * The byte conversion output implementation.
 */
public final class BytesOutput extends NetOutput {

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
	BytesOutput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
		super(pt, driver, up, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
		NetOutput subOutput = this.subOutput;
		
		if (subOutput == null) {
			if (subDriver == null) {
                                String subDriverName = getProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subOutput = newSubOutput(subDriver);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(cnx);

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
	public void initSend() throws NetIbisException {
		subOutput.initSend();
                super.initSend();
	}

        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws NetIbisException{
                super.finish();
                subOutput.finish();
        }

        public synchronized void close(Integer num) throws NetIbisException {
		if (subOutput != null) {
                        subOutput.close(num);
                }
        }

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		if (subOutput != null) {
			subOutput.free();
		}

		super.free();
	}        

        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                subOutput.writeByteBuffer(buffer);
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                subOutput.writeByte(NetConvert.boolean2byte(v));
        }

        /**
	 * Writes a byte v to the message.
	 * @param     v             The byte v to write.
	 */
        public void writeByte(byte v) throws NetIbisException {
                subOutput.writeByte(v);                
        }
        
        /**
	 * Writes a char v to the message.
	 * @param     v             The char v to write.
	 */
        public void writeChar(char v) throws NetIbisException {
                byte [] b = a2.allocate();
                NetConvert.writeChar(v, b);
                subOutput.writeArrayByte(b);
                a2.free(b);
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                byte [] b = a2.allocate();
                NetConvert.writeShort(v, b);
                subOutput.writeArrayByte(b);
                a2.free(b);                
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                byte [] b = a4.allocate();
                NetConvert.writeInt(v, b);
                subOutput.writeArrayByte(b);
                a4.free(b);                
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                byte [] b = a8.allocate();
                NetConvert.writeLong(v, b);
                subOutput.writeArrayByte(b);
                a8.free(b);                
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                byte [] b = a4.allocate();
                NetConvert.writeFloat(v, b);
                subOutput.writeArrayByte(b);
                a4.free(b);                
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                byte [] b = a8.allocate();
                NetConvert.writeDouble(v, b);
                subOutput.writeArrayByte(b);
                a8.free(b);                
        }

        /**
	 * Writes a Serializable string to the message.
         * Note: uses writeObject to send the string.
	 * @param     v             The string v to write.
	 */
        public void writeString(String v) throws NetIbisException {
                subOutput.writeString(v);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
	 */
        public void writeObject(Object v) throws NetIbisException {
                subOutput.writeObject(v);                
        }


        public void writeArraySliceBoolean(boolean [] ub, int o, int l) throws NetIbisException {
                if (l <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceBoolean(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceBoolean(ub, o, l));
                }                
        }

        public void writeArraySliceByte(byte [] ub, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceByte(ub, o, l);                
        }

        public void writeArraySliceChar(char [] ub, int o, int l) throws NetIbisException {
                final int f = 2;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceChar(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceChar(ub, o, l));
                }
        }

        public void writeArraySliceShort(short [] ub, int o, int l) throws NetIbisException {
                final int f = 2;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceShort(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceShort(ub, o, l));
                }
        }

        public void writeArraySliceInt(int [] ub, int o, int l) throws NetIbisException {
                final int f = 4;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceInt(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceInt(ub, o, l));
                }
        }

        public void writeArraySliceLong(long [] ub, int o, int l) throws NetIbisException {
                final int f = 8;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceLong(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceLong(ub, o, l));
                }
        }

        public void writeArraySliceFloat(float [] ub, int o, int l) throws NetIbisException {
                final int f = 4;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceFloat(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceFloat(ub, o, l));
                }
        }

        public void writeArraySliceDouble(double [] ub, int o, int l) throws NetIbisException {
                final int f = 8;

                if ((l*f) <= anThreshold) {
                        byte [] b = an.allocate();
                        NetConvert.writeArraySliceDouble(ub, o, l, b);
                        subOutput.writeArraySliceByte(b, 0, l*f);
                        an.free(b);
                } else {
                        subOutput.writeArrayByte(NetConvert.writeArraySliceDouble(ub, o, l));
                }
        }	

        public void writeArraySliceObject(Object [] ub, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        writeObject(ub[o+i]);
                }
        }
}
