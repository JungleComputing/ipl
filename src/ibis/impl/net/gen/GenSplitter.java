package ibis.ipl.impl.net.gen;

import ibis.ipl.IbisIOException;

import ibis.ipl.impl.net.*;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Iterator;
import java.util.Vector;

/**
 * Provides a generic multiple network output poller.
 */
public class GenSplitter extends NetOutput {

	// These fields are 'protected' instead of 'private' to allow the
	// class to be used as a base class for other splitters.

	/**
	 * The set of outputs.
	 */
	protected Vector    outputVector = null;

	/**
	 * The set of incoming TCP service connections
	 */
	protected Vector    isVector     = null;

	/**
	 * The set of outgoing TCP service connections
	 */
	protected Vector    osVector     = null;

	/**
	 * The driver used for the outputs.
	 */
	protected NetDriver subDriver    = null;


	/**
	 * Constructor.
	 *
	 * @param staticProperties the port's properties.
	 * @param driver the driver of this poller.
	 * @param output  the controlling output.
	 */
	public GenSplitter(NetPortType pt, NetDriver driver, NetIO up, String context) throws IbisIOException {
		super(pt, driver, up, context);
		outputVector = new Vector();
		isVector     = new Vector();
		osVector     = new Vector();
	}

	/**
	 * Adds a new input to the output set.
	 *
	 * The MTU and the header offset is updated by this function.
	 *
	 * @param output the output.
	 */
	private void addOutput(Integer   rpn,
			       NetOutput output) {
		int _mtu = output.getMaximumTransfertUnit();

		if (mtu == 0  ||  mtu > _mtu) {
			mtu = _mtu;
		}

		int _headersLength = output.getHeadersLength();

		if (headerOffset < _headersLength) {
			headerOffset = _headersLength;
		}

		outputVector.add(output);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setupConnection(Integer rpn, ObjectInputStream is, ObjectOutputStream os) throws IbisIOException {
		if (subDriver == null) {
			String subDriverName = getProperty("Driver");
                        subDriver = driver.getIbis().getDriver(subDriverName);
		}
		
		NetOutput no = newSubOutput(subDriver);
		
		no.setupConnection(rpn, is, os);
		addOutput(rpn, no);
	}

	/**
	 * {@inheritDoc}
	 */
	public void initSend() throws IbisIOException {
                super.initSend();
                
		Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.initSend();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() throws IbisIOException {
                super.finish();
		Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.finish();
		} while (i.hasNext());
	}

	/**
	 * {@inheritDoc}
	 */
	public void free()
		throws IbisIOException {
		if (outputVector != null) {
			Iterator i = outputVector.listIterator();

			while (i.hasNext()) {
				NetOutput no = (NetOutput)i.next();
				no.free();
			}
			outputVector = null;
		}
		
		isVector     = null;
		osVector     = null;

		super.free();
	}		

        public void writeByteBuffer(NetSendBuffer buffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByteBuffer(buffer);
		} while (i.hasNext());
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeBoolean(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public void writeByte(byte value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeByte(value);
		} while (i.hasNext());
        }
        
        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeChar(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeShort(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeInt(value);
		} while (i.hasNext());
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeLong(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeFloat(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeDouble(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeString(String value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeString(value);
		} while (i.hasNext());
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeObject(value);
		} while (i.hasNext());
        }

        public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayBoolean(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayByte(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayChar(char [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayChar(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayShort(short [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayShort(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayInt(int [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayInt(userBuffer);
		} while (i.hasNext());
        }


        public void writeArrayLong(long [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayLong(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayFloat(float [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayFloat(userBuffer);
		} while (i.hasNext());
        }

        public void writeArrayDouble(double [] userBuffer) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeArrayDouble(userBuffer);
		} while (i.hasNext());
        }

        public void writeSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayBoolean(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayByte(userBuffer, offset, length);
		} while (i.hasNext());
        }
        public void writeSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayChar(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayShort(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayInt(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayLong(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayFloat(userBuffer, offset, length);
		} while (i.hasNext());
        }

        public void writeSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                Iterator i = outputVector.listIterator();
		do {
			NetOutput no = (NetOutput)i.next();
			no.writeSubArrayDouble(userBuffer, offset, length);
		} while (i.hasNext());
        }	
}
