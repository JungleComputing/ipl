package ibis.ipl.impl.net;

import ibis.io.ArrayInputStream;
import ibis.io.SerializationInputStream;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;


/**
 * The ID input implementation.
 */
public abstract class NetSerializedInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	protected NetDriver        subDriver        = null;
       
	/**       
	 * The 'real' input.       
	 */       
	protected NetInput         subInput         = null;
        private SerializationInputStream inputSerializationStream = null;
	private Hashtable        streamTable = null;

	public NetSerializedInput(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
                streamTable = new Hashtable();
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

        public abstract SerializationInputStream newSerializationInputStream() throws IbisIOException;
        

	public void initReceive() throws IbisIOException {
                byte b = subInput.readByte();

                if (b != 0) {
                        inputSerializationStream = newSerializationInputStream();
                        streamTable.put(activeNum, inputSerializationStream);
                } else {
                        inputSerializationStream = (SerializationInputStream)streamTable.get(activeNum);
                }
                
	}

        public void inputUpcall(NetInput input, Integer spn) {
                activeNum = spn;
                mtu          = subInput.getMaximumTransfertUnit();
                headerOffset = subInput.getHeadersLength();
                try {
                        initReceive();
                } catch (Exception e) {
                        throw new Error(e);
                }
                upcallFunc.inputUpcall(this, spn);
                activeNum = null;
        }

	public Integer poll() throws IbisIOException {
                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll();
                if (result != null) {
                        activeNum = result;
                        mtu          = subInput.getMaximumTransfertUnit();
                        headerOffset = subInput.getHeadersLength();
                        initReceive();
                }

		return result;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		//System.err.println("SSerializationInput: finish-->");
                //inputSerializationStream.close();
                inputSerializationStream = null;
		super.finish();
		subInput.finish();
                activeNum = null;
		//System.err.println("SSerializationInput: finish<--");
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
                return inputSerializationStream.readBoolean();
        }
        

	public byte readByte() throws IbisIOException {
                return inputSerializationStream.readByte();
        }
        

	public char readChar() throws IbisIOException {
                return inputSerializationStream.readChar();
        }


	public short readShort() throws IbisIOException {
                return inputSerializationStream.readShort();
        }


	public int readInt() throws IbisIOException {
                return inputSerializationStream.readInt();
        }


	public long readLong() throws IbisIOException {
                return inputSerializationStream.readLong();
        }

	
	public float readFloat() throws IbisIOException {
                return inputSerializationStream.readFloat();
        }


	public double readDouble() throws IbisIOException {
                return inputSerializationStream.readDouble();
        }


	public String readString() throws IbisIOException {
                return (String)inputSerializationStream.readObject();
        }


	public Object readObject() throws IbisIOException {
                return inputSerializationStream.readObject();
        }

	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceBoolean(b, o, l);
        }


	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceByte(b, o, l);
        }


	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceChar(b, o, l);
        }


	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceShort(b, o, l);
        }


	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceInt(b, o, l);
        }


	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceLong(b, o, l);
        }


	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceFloat(b, o, l);
        }


	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                inputSerializationStream.readArraySliceObject(b, o, l);
        }

}
