package ibis.ipl.impl.net.bytes;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

	BytesInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
                an = new NetAllocator(anThreshold);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer spn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
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
                        subInput.setupConnection(spn, is, os, nls, this);
                } else {
                        subInput.setupConnection(spn, is, os, nls, null);
                }
	}

	public Integer poll() throws IbisIOException {
                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll();
                if (result != null) {
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                }
		return result;
	}
	
        public void inputUpcall(NetInput input, Integer spn) {
                activeNum = spn;
                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();
                upcallFunc.inputUpcall(this, spn);
                activeNum = null;
        }

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		super.finish();
		subInput.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (subInput != null) {
			subInput.free();
			subInput = null;
		}

		subDriver = null;
		
		super.free();
	}
	

        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                return subInput.readByteBuffer(expectedLength);
        }       

        public void readByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                subInput.readByteBuffer(buffer);
        }

	public boolean readBoolean() throws IbisIOException {
                return NetConvert.byte2boolean(subInput.readByte());
        }
        

	public byte readByte() throws IbisIOException {
                return subInput.readByte();
        }
        

	public char readChar() throws IbisIOException {
                char value = 0;

                byte [] b = a2.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readChar(b);
                a2.free(b);

                return value;
        }


	public short readShort() throws IbisIOException {
                short value = 0;

                byte [] b = a2.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readShort(b);
                a2.free(b);

                return value;
        }


	public int readInt() throws IbisIOException {
                int value = 0;

                byte [] b = a4.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readInt(b);
                a4.free(b);

                return value;
        }


	public long readLong() throws IbisIOException {
                long value = 0;

                byte [] b = a8.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readLong(b);
                a8.free(b);

                return value;
        }

	
	public float readFloat() throws IbisIOException {
                float value = 0;

                byte [] b = a4.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readFloat(b);
                a4.free(b);

                return value;
        }


	public double readDouble() throws IbisIOException {
                double value = 0;

                byte [] b = a8.allocate();
                subInput.readArrayByte(b);
                value = NetConvert.readDouble(b);
                a8.free(b);

                return value;
        }


	public String readString() throws IbisIOException {
                return subInput.readString();
        }


	public Object readObject() throws IbisIOException {
                return subInput.readObject();
        }

	public void readArraySliceBoolean(boolean [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceByte(byte [] ub, int o, int l) throws IbisIOException {
                subInput.readArraySliceByte(ub, o, l);
        }


	public void readArraySliceChar(char [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceShort(short [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceInt(int [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceLong(long [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceFloat(float [] ub, int o, int l) throws IbisIOException {
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


	public void readArraySliceDouble(double [] ub, int o, int l) throws IbisIOException {
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

	public void readArraySliceObject(Object [] ub, int o, int l) throws IbisIOException {
                for (int i = 0; i < l; i++) {
                        ub[o+i] = readObject();
                }
        }       
}
