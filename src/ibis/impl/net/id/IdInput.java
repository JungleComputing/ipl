package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.*;


/**
 * The ID input implementation.
 */
public final class IdInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param input the controlling input.
	 */
	IdInput(NetPortType pt, NetDriver driver, NetIO up, String context)
		throws NetIbisException {
		super(pt, driver, up, context);
	}

	/*
	 * Sets up an incoming ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public synchronized void setupConnection(NetConnection cnx) throws NetIbisException {
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver);
			this.subInput = subInput;
		}
		
                if (upcallFunc != null) {
                        subInput.setupConnection(cnx, this);
                } else {
                        subInput.setupConnection(cnx, null);
                }
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                activeNum = spn;

                // Note: the IdInput instance is bypassed during upcall reception
                upcallFunc.inputUpcall(input, spn);

                activeNum = null;
        }


	/**
	 * {@inheritDoc}
	 *
	 * <BR><B>Note</B>: This ID polling implementation uses the
	 * {@link java.io.InputStream#available()} function to test whether at least one
	 * data byte may be extracted without blocking.
	 *
	 * @return {@inheritDoc}
	 */
	public Integer poll() throws NetIbisException {
                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll();
                if (result != null) {
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                }

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
		subInput.finish();
		super.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws NetIbisException {
		if (subInput != null) {
			subInput.free();
		}

		super.free();
	}
	
        public synchronized void close(Integer num) throws NetIbisException {
                if (subInput != null) {
			subInput.close(num);
			subInput = null;
		}
        }
        

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                return subInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                subInput.readByteBuffer(buffer);
        }


	public boolean readBoolean() throws NetIbisException {
                return subInput.readBoolean();
        }
        

	public byte readByte() throws NetIbisException {
                return subInput.readByte();
        }
        

	public char readChar() throws NetIbisException {
                return subInput.readChar();
        }


	public short readShort() throws NetIbisException {
                return subInput.readShort();
        }


	public int readInt() throws NetIbisException {
                return subInput.readInt();
        }


	public long readLong() throws NetIbisException {
                return subInput.readLong();
        }

	
	public float readFloat() throws NetIbisException {
                return subInput.readFloat();
        }


	public double readDouble() throws NetIbisException {
                return subInput.readDouble();
        }


	public String readString() throws NetIbisException {
                return (String)subInput.readString();
        }


	public Object readObject() throws NetIbisException {
                return subInput.readObject();
        }


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceBoolean(b, o, l);
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceByte(b, o, l);
        }


	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceChar(b, o, l);
        }


	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceShort(b, o, l);
        }


	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceInt(b, o, l);
        }


	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceLong(b, o, l);
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceFloat(b, o, l);
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                subInput.readArraySliceObject(b, o, l);
        }

}
