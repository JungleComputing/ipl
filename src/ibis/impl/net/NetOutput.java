package ibis.ipl.impl.net;

import ibis.ipl.WriteMessage;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.Socket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Hashtable;
import java.util.Iterator;


/**
 * Provide an abstraction of a network output.
 */
public abstract class NetOutput extends NetIO implements WriteMessage {
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
	}

        /**
         * Prepare the output for a new message transmission.
         */
	public void initSend() throws NetIbisException {
                //
        }


        /* WriteMessage Interface */                

        /**
         * Unimplemented.
         *
         * Does nothing.
         *
	 * @exception NetIbisException an error occurred 
	 **/
        public void send() throws NetIbisException {
                //
        }

        /**
         * Completes the current outgoing message.
         *
         * @exception NetIbisException in case of trouble.
         */
        public void finish() throws NetIbisException{
                if (_outputConvertStream != null) {
                        try {
                                _outputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }

                        _outputConvertStream = null;
                }
        }

        /**
         * Reset the output state.
         *
         * The full reset functionality is not implemented. When {#doSend} is true, it is equivalent to the {@link #finish} method. Otherwise, an {@link Error error} is thrown.
         *
         * @param doSend if <code>true</code> 
         * @exception NetIbisException in case of trouble.
         */
        public void reset(boolean doSend) throws NetIbisException {
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
        public int getCount() {
                return 0;
        }

        /**
         * Unimplemented.
         */
        public void resetCount() {
                __.unimplemented__("resetCount");
        }


	/**
	 * Create a {@link NetSendBuffer} using the installed factory.
	 *
	 * This is only valid for a Factory with MTU.
	 *
	 * @throws an {@link NetIbisException} if the factory has no default MTU
         * @return the new {@link NetSendBuffer}.
	 */
	public NetSendBuffer createSendBuffer() throws NetIbisException {
	    return (NetSendBuffer)createBuffer();
	}

	/**
	 * Utility function to get a NetSendBuffer from our NetBuffer factory
	 *
	 * @param length the length of the data stored in the buffer
         * @return the new {@link NetSendBuffer}.
	 */
	public NetSendBuffer createSendBuffer(int length)
		throws NetIbisException {
	    NetSendBuffer b = (NetSendBuffer)createBuffer(length);
            return b;
	}


        /* fallback serialization implementation */

        /**
         * Object stream for the internal fallback serialization.
         *
         * Note: the fallback serialization implementation internally uses
         * a JVM {@link ObjectOutputStream}/{@link ObjectInputStream} pair.
         * The stream pair is closed upon each message completion to ensure data
         * consistency.
         */
        private ObjectOutputStream _outputConvertStream = null;

        /**
         * Check whether the convert stream should be initialized, and
         * initialize it when needed.
	 */
        private final void checkConvertStream() throws IOException {
                if (_outputConvertStream == null) {
                        DummyOutputStream dos = new DummyOutputStream();
                        _outputConvertStream = new ObjectOutputStream(dos);
                        _outputConvertStream.flush();
                }
        }        

        /**
         * Default implementation of {@link #writeBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteBoolean(boolean value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeBoolean(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeChar}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteChar(char value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeChar(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeShort}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteShort(short value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeShort(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeInt}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteInt(int value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeInt(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeLong}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteLong(long value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeLong(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeFloat}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteFloat(float value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeFloat(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeDouble}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteDouble(double value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeDouble(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeString}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteString(String value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeUTF(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeObject}.
         *
         * Note: this method must not be changed.
         *
         * @param value the value to write.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteObject(Object value) throws NetIbisException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeObject(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(boolean [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeBoolean(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(byte [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeByte(b[o++]);
                                _outputConvertStream.flush();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }
        
        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(char [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeChar(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(short [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeShort(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(int [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeInt(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(long [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeLong(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(float [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeFloat(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(double [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeDouble(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #writeArray}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultWriteArray(Object [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeObject(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Atomic packet write function.
         * @param b the buffer to write.
         * @exception NetIbisException in case of trouble.
         */
        public void writeByteBuffer(NetSendBuffer b) throws NetIbisException {
                defaultWriteInt(b.length);
                defaultWriteArray(b.data, 0, b.length);
		if (! b.ownershipClaimed) {
		    b.free();
		}
        }

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeBoolean(boolean value) throws NetIbisException {
                defaultWriteBoolean(value);
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public abstract void writeByte(byte value) throws NetIbisException;

        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeChar(char value) throws NetIbisException {
                defaultWriteChar(value);
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeShort(short value) throws NetIbisException {
                defaultWriteShort(value);
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeInt(int value) throws NetIbisException {
                defaultWriteInt(value);
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeLong(long value) throws NetIbisException {
                defaultWriteLong(value);
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeFloat(float value) throws NetIbisException {
                defaultWriteFloat(value);
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeDouble(double value) throws NetIbisException {
                defaultWriteDouble(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeString(String value) throws NetIbisException {
                defaultWriteString(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
         * @exception NetIbisException in case of trouble.
	 */
        public void writeObject(Object value) throws NetIbisException {
                defaultWriteObject(value);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(boolean [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(byte [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(char [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(short [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(int [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(long [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(float [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(double [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public void writeArray(Object [] b, int o, int l) throws NetIbisException {
                defaultWriteArray(b, o, l);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(boolean [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(byte [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(char [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(short [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(int [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(long [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(float [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(double [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Append some elements to the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
        public final void writeArray(Object [] b) throws NetIbisException {
                writeArray(b, 0, b.length);
        }

        /**
         * Internal dummy {@link OutputStream} to be used as a byte stream sink for
         * the {@link ObjectOutputStream} based fallback serialization.
         */
        private final class DummyOutputStream extends OutputStream {

                /**
                 * {@inheritDoc}
                 *
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 *
                 */
                public void write(int b) throws IOException {
                        try {
                                writeByte((byte)b);
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }
                }
        }
}
