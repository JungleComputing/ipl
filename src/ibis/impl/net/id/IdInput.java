package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

/* Only for java >= 1.4 
import java.net.SocketTimeoutException;
*/
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;


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
	IdInput(StaticProperties sp,
		NetDriver        driver,
		NetInput         input)
		throws IbisIOException {
		super(sp, driver, input);
	}

	/*
	 * Sets up an incoming ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer                rpn,
				    ObjectInputStream 	   is,
				    ObjectOutputStream	   os)
		throws IbisIOException {
		NetInput subInput = this.subInput;
		if (subInput == null) {
			if (subDriver == null) {
				subDriver = driver.getIbis().getDriver(getProperty("Driver"));
			}

			subInput = subDriver.newInput(staticProperties, this);
			this.subInput = subInput;
		}
		
		subInput.setupConnection(rpn, is, os);
		 
		int _mtu = subInput.getMaximumTransfertUnit();

		if ((mtu == 0)
		    ||
		    (mtu > _mtu)) {
			mtu = _mtu;
		}

		int _headersLength = subInput.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}
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
		return (subInput != null) ? subInput.poll() : null;
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
