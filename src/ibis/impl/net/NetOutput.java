package ibis.impl.net;

import ibis.ipl.WriteMessage;
import ibis.ipl.IbisConfigurationException;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
 * Provide an abstraction of a network output.
 */
public abstract class NetOutput extends NetIO implements WriteMessage {


	/**
	 * Check to not _finish twice
	 */
	private boolean		finished;


	/**
	 * Constructor.
	 *
	 * @param portType the port {@link NetPortType} type.
	 * @param driver the driver.
	 * @param context the context.
	 */
	protected NetOutput(NetPortType      portType,
                            NetDriver 	     driver,
                            String           context) {
		super(portType, driver, context);
		// factory = new NetBufferFactory(new NetSendBufferFactoryDefaultImpl());
		finished = false;
	}

        /**
         * Prepare the output for a new message transmission.
         */
	public void initSend() throws IOException {
// System.err.println(this + ": in initSend");
        }

	public boolean writeBufferedSupported() {
	    return false;
	}

	public void writeBuffered(byte[] data, int offset, int length)
		throws IOException {
	    throw new IOException("write buffered byte array not supported");
	}


        /* WriteMessage Interface */                

        /**
         * Unimplemented.
         *
         * Does nothing.
         *
	 * @exception IOException an error occurred 
	 **/
        public void send() throws IOException {
                //
        }


	/**
	 * Completes the message transmission.
	 *
	 * Note: if it is detected that the message is actually empty,
	 * a single byte is forced to be sent over the network.
	 */
	private void _finish() throws IOException {
	    log.in();
	    finished = true;
	    log.out();
	}

        /**
         * Completes the current outgoing message.
         *
         * @exception IOException in case of trouble.
         */
        public void finish() throws IOException{
		if (! finished) {
		    _finish();
		}
		finished = false;
        }

        /**
         * Reset the output state.
         *
         * The full reset functionality is not implemented. When {#doSend} is true, it is equivalent to the {@link #finish} method. Otherwise, an {@link Error error} is thrown.
         *
         * @param doSend if <code>true</code> 
         * @exception IOException in case of trouble.
         */
        public void reset(boolean doSend) throws IOException {
		_finish();

                if (doSend) {
                        send();
                } else {
                        throw new Error("full reset unimplemented");
                }

		/* Calling finish() here i.s.o. replicating the code above
		 * has an extra advantage (besides code nonmultiplication):
		 * a subclass may override finish. This happens e.g. in
		 * multi | NetBufferedOutput.
		 */
		finish();
                //System.err.println("NetOutput: reset <--");
        }

        /**
         * Unimplemented.
         *
         * @return 0.
         */
        public long getCount() {
                __.unimplemented__("getCount");
                return 0;
        }

        /**
         * Unimplemented.
         */
        public void resetCount() {
                __.unimplemented__("resetCount");
        }

	protected void handleEmptyMsg() throws IOException {
	    writeByte((byte)255);
	}


	/**
	 * Create a {@link NetSendBuffer} using the installed factory.
	 *
	 * This is only valid for a Factory with MTU.
	 *
	 * @throws an {@link IllegalArgumentException} if the factory has no default MTU
         * @return the new {@link NetSendBuffer}.
	 */
	public NetSendBuffer createSendBuffer() {
	    return (NetSendBuffer)createBuffer();
	}

	/**
	 * Utility function to get a NetSendBuffer from our NetBuffer factory
	 *
	 * @param length the length of the data stored in the buffer
         * @return the new {@link NetSendBuffer}.
	 */
	public NetSendBuffer createSendBuffer(int length) {
	    NetSendBuffer b = (NetSendBuffer)createBuffer(length);
            return b;
	}

	public ibis.ipl.SendPort localPort() {
		// what the @#@ should we do here --Rob
		throw new ibis.ipl.IbisError("AAAAA");
	}

        /**
         * Atomic packet write function.
         * @param b the buffer to write.
         * @exception IOException in case of trouble.
         */
        public void writeByteBuffer(NetSendBuffer b) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeByteBuffer");
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeBoolean(boolean value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeBoolean");
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
         * @exception IOException in case of trouble.
	 */
        public abstract void writeByte(byte value) throws IOException;

        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeChar(char value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeChar");
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeShort(short value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeShort");
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeInt(int value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeInt");
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeLong(long value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeLong");
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeFloat(float value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeFloat");
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeDouble(double value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeDouble");
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeString(String value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeString");
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
         * @exception IOException in case of trouble.
	 */
        public void writeObject(Object value) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeObject");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(boolean [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(boolean[])");
        }

        /**
         * Append some elements to the current message. WARNING: If a driver
	 * ovverrides the writeObject function, it must als override this one.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(byte [] b, int o, int l) throws IOException {
	    for(int i = o; i < (o + l);i++) {
		writeByte(b[i]);
	    }
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(char [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(char[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(short [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(short[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(int [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(int[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(long [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(long[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(float [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(float[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(double [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(double[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception IOException in case of trouble. 
         */
        public void writeArray(Object [] b, int o, int l) throws IOException {
	    throw new IbisConfigurationException("NetIbis driver \"" + context +
		"\" does not support writeArray(Object[])");
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(boolean [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(byte [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(char [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(short [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(int [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(long [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(float [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(double [] b) throws IOException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         *
         * @exception IOException in case of trouble. 
         */
        public final void writeArray(Object [] b) throws IOException {
                writeArray(b, 0, b.length);
        }
}
