package ibis.ipl.impl.net.bytes;

import ibis.ipl.impl.net.*;

/**
 * The ID input implementation.
 */
public final class BytesInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

        /*
         * 
         */
        private NetAllocator a2 = new NetAllocator(2, 1024);
        private NetAllocator a4 = new NetAllocator(4, 1024);
        private NetAllocator a8 = new NetAllocator(8, 1024);

        /**
         * Pre-allocation threshold.
         * Note: must be a multiple of 8.
         */
        private int          anThreshold = 8 * 256;
        private NetAllocator an = null;
        private volatile Thread activeUpcallThread = null;

	BytesInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws NetIbisException {
		super(pt, driver, up, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
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

	public synchronized Integer poll(boolean block) throws NetIbisException {
                if (activeNum != null) {
                        throw new Error("invalid call");
                }

                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll(block);
                if (result != null) {
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                }
		return result;
	}
	
        public void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                synchronized(this) {
                        while (activeNum != null) {
                                try {
                                        wait();
                                } catch (InterruptedException e) {
                                        throw new NetIbisInterruptedException(e);
                                }
                        }
                        
                        if (spn == null) {
                                throw new Error("invalid connection num");
                        }
                        
                        activeNum = spn;
                        activeUpcallThread = Thread.currentThread();
                }

                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();

                upcallFunc.inputUpcall(this, spn);

                synchronized(this) {
                        if (activeNum == spn && activeUpcallThread == Thread.currentThread()) {
                                activeNum = null;
                                activeUpcallThread = null;
                                notifyAll();
                        }
                }
        }

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws NetIbisException {
		super.finish();
		subInput.finish();
                synchronized(this) {
                        activeNum = null;
                        activeUpcallThread = null;
                        notifyAll();
                }
                
	}

        public synchronized void close(Integer num) throws NetIbisException {
                if (subInput != null) {
                        subInput.close(num);
                }
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
	

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                return subInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                subInput.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws NetIbisException {
                return NetConvert.byte2boolean(subInput.readByte());
        }
        

	public byte readByte() throws NetIbisException {
                return subInput.readByte();
        }
        

	public char readChar() throws NetIbisException {
                char value = 0;

                byte [] b = a2.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readChar(b);
                a2.free(b);

                return value;
        }


	public short readShort() throws NetIbisException {
                short value = 0;

                byte [] b = a2.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readShort(b);
                a2.free(b);

                return value;
        }


	public int readInt() throws NetIbisException {
                int value = 0;

                byte [] b = a4.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readInt(b);
                a4.free(b);

                return value;
        }


	public long readLong() throws NetIbisException {
                long value = 0;

                byte [] b = a8.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readLong(b);
                a8.free(b);

                return value;
        }

	
	public float readFloat() throws NetIbisException {
                float value = 0;

                byte [] b = a4.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readFloat(b);
                a4.free(b);

                return value;
        }


	public double readDouble() throws NetIbisException {
                double value = 0;

                byte [] b = a8.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readDouble(b);
                a8.free(b);

                return value;
        }


	public String readString() throws NetIbisException {
                return subInput.readString();
        }


	public Object readObject() throws NetIbisException {
                return subInput.readObject();
        }

	public void readArraySliceBoolean(boolean [] ub, int o, int l) throws NetIbisException {
                if (l <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l);
                        NetConvert.readArraySliceBoolean(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceBoolean(b, ub, o, l);
                }
        }


	public void readArraySliceByte(byte [] ub, int o, int l) throws NetIbisException {
                subInput.readArraySliceByte(ub, o, l);
        }


	public void readArraySliceChar(char [] ub, int o, int l) throws NetIbisException {
                final int f = 2;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceChar(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceChar(b, ub, o, l);
                }
        }


	public void readArraySliceShort(short [] ub, int o, int l) throws NetIbisException {
                final int f = 2;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceShort(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceShort(b, ub, o, l);
                }
        }


	public void readArraySliceInt(int [] ub, int o, int l) throws NetIbisException {
                final int f = 4;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceInt(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceInt(b, ub, o, l);
                }
        }


	public void readArraySliceLong(long [] ub, int o, int l) throws NetIbisException {
                final int f = 8;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceLong(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceLong(b, ub, o, l);
                }
        }


	public void readArraySliceFloat(float [] ub, int o, int l) throws NetIbisException {
                final int f = 4;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceFloat(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceFloat(b, ub, o, l);
                }
        }


	public void readArraySliceDouble(double [] ub, int o, int l) throws NetIbisException {
                final int f = 8;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readArraySliceByte(b, 0, l*f);
                        NetConvert.readArraySliceDouble(b, ub, o, l);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*l];
                        subInput.readArrayByte(b);
                        NetConvert.readArraySliceDouble(b, ub, o, l);
                }
        }

	public void readArraySliceObject(Object [] ub, int o, int l) throws NetIbisException {
                for (int i = 0; i < l; i++) {
                        ub[o+i] = readObject();
                }
        }       
}
