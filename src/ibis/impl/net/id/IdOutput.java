package ibis.ipl.impl.net.id;

import ibis.ipl.impl.net.__;
import ibis.ipl.impl.net.NetDriver;
import ibis.ipl.impl.net.NetOutput;
import ibis.ipl.impl.net.NetSendBuffer;

import ibis.ipl.IbisIOException;
import ibis.ipl.StaticProperties;

import java.net.Socket;
import java.net.InetAddress;
import java.net.SocketException;

import java.io.ObjectInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;

/**
 * The ID output implementation.
 */
public class IdOutput extends NetOutput {

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
	 * @param output the controlling output.
	 */
	IdOutput(StaticProperties sp,
		 NetDriver   	  driver,
		 NetOutput   	  output)
		throws IbisIOException {
		super(sp, driver, output);
	}

	/*
	 * Sets up an outgoing ID connection.
	 *
	 * @param rpn {@inheritDoc}
	 * @param is {@inheritDoc}
	 * @param os {@inheritDoc}
	 */
	public void setupConnection(Integer            rpn,
				    ObjectInputStream  is,
				    ObjectOutputStream os)
		throws IbisIOException {
		NetOutput subOutput = this.subOutput;
		
		if (subOutput == null) {
			if (subDriver == null) {
				subDriver = driver.getIbis().getDriver(getProperty("Driver"));
			}

			subOutput = subDriver.newOutput(staticProperties, this);
			this.subOutput = subOutput;
		}

		subOutput.setupConnection(rpn, is, os);

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
	public void initSend() throws IbisIOException {
                super.initSend();
		subOutput.initSend();
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
		subOutput.finish();
		super.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	public void free() throws IbisIOException {
		if (subOutput != null) {
			subOutput.free();
			subOutput = null;
		}
		
		subDriver = null;

		super.free();
	}

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IbisIOException {
                subOutput.writeBoolean(value);
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte value) throws IbisIOException {
                subOutput.writeByte(value);
        }
        
        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IbisIOException {
                subOutput.writeChar(value);
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IbisIOException {
                subOutput.writeShort(value);
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IbisIOException {
                subOutput.writeInt(value);
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IbisIOException {
                subOutput.writeLong(value);
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IbisIOException {
                subOutput.writeFloat(value);
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IbisIOException {
                subOutput.writeDouble(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeString(String value) throws IbisIOException {
                subOutput.writeString(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IbisIOException {
                subOutput.writeObject(value);
        }

        public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                subOutput.writeArrayBoolean(userBuffer);
        }

        public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
                subOutput.writeArrayByte(userBuffer);
        }

        public void writeArrayChar(char [] userBuffer) throws IbisIOException {
                subOutput.writeArrayChar(userBuffer);
        }

        public void writeArrayShort(short [] userBuffer) throws IbisIOException {
                subOutput.writeArrayShort(userBuffer);
        }

        public void writeArrayInt(int [] userBuffer) throws IbisIOException {
                subOutput.writeArrayInt(userBuffer);
        }


        public void writeArrayLong(long [] userBuffer) throws IbisIOException {
                subOutput.writeArrayLong(userBuffer);
        }

        public void writeArrayFloat(float [] userBuffer) throws IbisIOException {
                subOutput.writeArrayFloat(userBuffer);
        }

        public void writeArrayDouble(double [] userBuffer) throws IbisIOException {
                subOutput.writeArrayDouble(userBuffer);
        }

        public void writeSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayBoolean(userBuffer, offset, length);
        }

        public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayByte(userBuffer, offset, length);
        }
        public void writeSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayChar(userBuffer, offset, length);
        }

        public void writeSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayShort(userBuffer, offset, length);
        }

        public void writeSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayInt(userBuffer, offset, length);
        }

        public void writeSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayLong(userBuffer, offset, length);
        }

        public void writeSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayFloat(userBuffer, offset, length);
        }

        public void writeSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                subOutput.writeSubArrayDouble(userBuffer, offset, length);
        }	
}
