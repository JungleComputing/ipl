package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


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
		throws IbisIOException {
		super(pt, driver, up, context);
	}

	/*
	 * Sets up an incoming ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
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

        public synchronized void inputUpcall(NetInput input, Integer spn) {
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
	
	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		subInput.finish();
		super.finish();
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
                return subInput.readBoolean();
        }
        

	public byte readByte() throws IbisIOException {
                return subInput.readByte();
        }
        

	public char readChar() throws IbisIOException {
                return subInput.readChar();
        }


	public short readShort() throws IbisIOException {
                return subInput.readShort();
        }


	public int readInt() throws IbisIOException {
                return subInput.readInt();
        }


	public long readLong() throws IbisIOException {
                return subInput.readLong();
        }

	
	public float readFloat() throws IbisIOException {
                return subInput.readFloat();
        }


	public double readDouble() throws IbisIOException {
                return subInput.readDouble();
        }


	public String readString() throws IbisIOException {
                return (String)subInput.readString();
        }


	public Object readObject() throws IbisIOException {
                return subInput.readObject();
        }


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceBoolean(b, o, l);
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceByte(b, o, l);
        }


	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceChar(b, o, l);
        }


	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceShort(b, o, l);
        }


	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceInt(b, o, l);
        }


	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceLong(b, o, l);
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceFloat(b, o, l);
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                subInput.readArraySliceObject(b, o, l);
        }

}
