package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.*;


/**
 * The ID output implementation.
 */
public final class IdOutput extends NetOutput {

	/**
	 * The driver used for the 'real' output.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' output.
	 */
	private NetOutput subOutput = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the output's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 */
	IdOutput(NetPortType pt, NetDriver driver, String context) throws NetIbisException {
		super(pt, driver, context);
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

		if ((mtu == 0)
		    ||
		    (mtu > _mtu)) {
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
                super.initSend();
		subOutput.initSend();
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
		subOutput.finish();
		super.finish();
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

	public synchronized void close(Integer num) throws NetIbisException {
		if (subOutput != null) {
			subOutput.close(num);
			subOutput = null;
		}		
	}

        public void writeByteBuffer(NetSendBuffer buffer) throws NetIbisException {
                subOutput.writeByteBuffer(buffer);
        }

        /**
	 * Writes a boolean v to the message.
	 * @param     v             The boolean v to write.
	 */
        public void writeBoolean(boolean v) throws NetIbisException {
                subOutput.writeBoolean(v);
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
                subOutput.writeChar(v);
        }

        /**
	 * Writes a short v to the message.
	 * @param     v             The short v to write.
	 */
        public void writeShort(short v) throws NetIbisException {
                subOutput.writeShort(v);
        }

        /**
	 * Writes a int v to the message.
	 * @param     v             The int v to write.
	 */
        public void writeInt(int v) throws NetIbisException {
                subOutput.writeInt(v);
        }


        /**
	 * Writes a long v to the message.
	 * @param     v             The long v to write.
	 */
        public void writeLong(long v) throws NetIbisException {
                subOutput.writeLong(v);
        }

        /**
	 * Writes a float v to the message.
	 * @param     v             The float v to write.
	 */
        public void writeFloat(float v) throws NetIbisException {
                subOutput.writeFloat(v);
        }

        /**
	 * Writes a double v to the message.
	 * @param     v             The double v to write.
	 */
        public void writeDouble(double v) throws NetIbisException {
                subOutput.writeDouble(v);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     v             The object v to write.
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

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceBoolean(b, o, l);
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceByte(b, o, l);
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceChar(b, o, l);
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceShort(b, o, l);
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceInt(b, o, l);
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceLong(b, o, l);
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceFloat(b, o, l);
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceDouble(b, o, l);
        }	

        public void writeArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                subOutput.writeArraySliceObject(b, o, l);
        }	
}
