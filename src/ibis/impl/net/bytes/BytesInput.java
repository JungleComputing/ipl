package ibis.ipl.impl.net.bytes;

import ibis.io.ArrayInputStream;
import ibis.io.MantaInputStream;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetAllocator;
import ibis.ipl.impl.net.NetConvert;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetInput;
import ibis.ipl.impl.net.NetReceiveBuffer;
import ibis.ipl.impl.net.NetSendPortIdentifier;

import ibis.io.Conversion;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

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
public class BytesInput extends NetInput {

	/**
	 * The driver used for the 'real' input.
	 */
	private NetDriver subDriver = null;

	/**
	 * The 'real' input.
	 */
	private NetInput  subInput  = null;

        /**
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

	/**
	 * Constructor.
	 *
	 * @param sp the properties of the input's 
	 * {@link ibis.ipl.impl.net.NetSendPort NetSendPort}.
	 * @param driver the ID driver instance.
	 * @param input the controlling input.
	 */
	BytesInput(StaticProperties sp,
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
                if (subInput == null)
                        return null;
                
                Integer result = subInput.poll();
		return result;
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

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;

                if (l <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l);
                        NetConvert.readArrayBoolean(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayBoolean(b, userBuffer);
                }
        }


	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
                subInput.readArrayByte(userBuffer);
        }


	public void readArrayChar(char [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 2;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayChar(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayChar(b, userBuffer);
                }
        }


	public void readArrayShort(short [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 2;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayShort(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayShort(b, userBuffer);
                }
        }


	public void readArrayInt(int [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 4;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayInt(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayInt(b, userBuffer);
                }
        }


	public void readArrayLong(long [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 8;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayLong(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayLong(b, userBuffer);
                }
        }


	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 4;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayFloat(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayFloat(b, userBuffer);
                }
        }


	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
                int l = userBuffer.length;
                final int f = 8;

                if (l*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, l*f);
                        NetConvert.readArrayDouble(b, userBuffer);
                        an.free(b);
                } else {
                        byte [] b = new byte[l*f];
                        subInput.readArrayByte(b);
                        NetConvert.readArrayDouble(b, userBuffer);
                }
        }



	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                if (length <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length);
                        NetConvert.readSubArrayBoolean(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayBoolean(b, userBuffer, offset, length);
                }
        }


	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                subInput.readSubArrayByte(userBuffer, offset, length);
        }


	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 2;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayChar(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayChar(b, userBuffer, offset, length);
                }
        }


	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 2;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayShort(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayShort(b, userBuffer, offset, length);
                }
        }


	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 4;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayInt(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayInt(b, userBuffer, offset, length);
                }
        }


	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 8;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayLong(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayLong(b, userBuffer, offset, length);
                }
        }


	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 4;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayFloat(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayFloat(b, userBuffer, offset, length);

                }
        }


	public void readSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                final int f = 8;

                if (length*f <= anThreshold) {
                        byte [] b = an.allocate();
                        subInput.readSubArrayByte(b, 0, length*f);
                        NetConvert.readSubArrayDouble(b, userBuffer, offset, length);
                        an.free(b);
                } else {
                        byte [] b = new byte[f*length];
                        subInput.readArrayByte(b);
                        NetConvert.readSubArrayDouble(b, userBuffer, offset, length);
                }
        }
}
