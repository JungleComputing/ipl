package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.*;

import ibis.ipl.IbisIOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * The ID input implementation.
 */
public class IdInput extends NetInput {

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
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os, NetServiceListener nls) throws IbisIOException {
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
                                String subDriverName = getMandatoryProperty("Driver");
				subDriver = driver.getIbis().getDriver(subDriverName);
			}

			subInput = newSubInput(subDriver);
			this.subInput = subInput;
		}
		
		subInput.setupConnection(rpn, is, os, nls);
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

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                subInput.readArrayBoolean(userBuffer);
        }


	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
                subInput.readArrayByte(userBuffer);
        }


	public void readArrayChar(char [] userBuffer) throws IbisIOException {
                subInput.readArrayChar(userBuffer);
        }


	public void readArrayShort(short [] userBuffer) throws IbisIOException {
                subInput.readArrayShort(userBuffer);
        }


	public void readArrayInt(int [] userBuffer) throws IbisIOException {
                subInput.readArrayInt(userBuffer);
        }


	public void readArrayLong(long [] userBuffer) throws IbisIOException {
                subInput.readArrayLong(userBuffer);
        }


	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
                subInput.readArrayFloat(userBuffer);
        }


	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
                subInput.readArrayDouble(userBuffer);
        }



	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayBoolean(userBuffer, offset, length);
        }


	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayByte(userBuffer, offset, length);
        }


	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayChar(userBuffer, offset, length);
        }


	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayShort(userBuffer, offset, length);
        }


	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayInt(userBuffer, offset, length);
        }


	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayLong(userBuffer, offset, length);
        }


	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayFloat(userBuffer, offset, length);
        }


	public void readSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayDouble(userBuffer, offset, length);
        }

}
