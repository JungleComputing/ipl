package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;

import java.util.Hashtable;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Hashtable;

/**
 * Provide an abstraction of a network input.
 */
public abstract class NetInput extends NetIO implements ReadMessage, NetInputUpcall {
	/**
	 * Active {@link NetConnection connection} number or <code>null</code> if
	 * no connection is active.
	 */
	protected volatile Integer        activeNum  = null;

        /**
         * Upcall interface for incoming messages.
         */
        protected          NetInputUpcall upcallFunc = null;

	/**
	 * Constructor.
	 *
	 * @param portType the port {@link NetPortType} type.
	 * @param driver the driver.
	 * @param context the context.
	 */
	protected NetInput(NetPortType portType,
			   NetDriver   driver,
                           String      context) {
		super(portType, driver, context);
		// setBufferFactory(new NetBufferFactory(new NetReceiveBufferFactoryDefaultImpl()));
	}

        /**
         * Default incoming message upcall method.
         *
         * Note: this method is only useful for filtering drivers.
         *
         * @param input the {@link NetInput sub-input} that generated the upcall.
         * @param num   the active connection number
         * @exception NetIbisException in case of trouble.
         */
        public synchronized void inputUpcall(NetInput input, Integer num) throws NetIbisException {
                activeNum = num;
                upcallFunc.inputUpcall(this, num);
                activeNum = null;
        }

	/**
	 * Test for incoming data.
	 *
	 * Note: if {@linkplain #poll} is called again immediately
	 * after a successful {@linkplain #poll} without extracting the message and
	 * {@linkplain #finish finishing} the input, the result is
	 * undefined and data might get lost. Use {@link
	 * #getActiveSendPortNum} instead.
	 *
	 * @param blockForMessage indicates whether this method must block until
	 *        a message has arrived, or just query the input one.
	 * @return the {@link NetConnection connection} identifier or <code>null</code> if no data is available.
	 * @exception NetIbisException if the polling fails (!= the
	 * polling is unsuccessful).
	 */
	public abstract Integer poll(boolean blockForMessage) throws NetIbisException;

	/**
	 * Unblockingly test for incoming data.
	 *
	 * Note: if {@linkplain #poll} is called again immediately
	 * after a successful {@linkplain #poll} without extracting the message and
	 * {@linkplain #finish finishing} the input, the result is
	 * undefined and data might get lost. Use {@link
	 * #getActiveSendPortNum} instead.
	 *
	 * @return the {@link NetConnection connection} identifier or <code>null</code> if no data is available.
	 * @exception NetIbisException if the polling fails (!= the
	 * polling is unsuccessful).
         */
	public Integer poll() throws NetIbisException {
	    return poll(false);
	}

	/**
	 * Return the active {@link NetConnection connection} identifier or <code>null</code> if no {@link NetConnection connection} is active.
	 *
	 * @return the active {@link NetConnection connection} identifier or <code>null</code> if no {@link NetConnection connection} is active.
	 */
	public final Integer getActiveSendPortNum() {
		return activeNum;
	}

        /**
	 * Actually establish a connection with a remote port and register an upcall function for incoming message notification.
	 *
	 * @param cnx the connection attributes.
         * @param inputUpcall the upcall function for incoming message notification.
	 * @exception NetIbisException if the connection setup fails.
	 */
	public void setupConnection(NetConnection  cnx,
                                    NetInputUpcall inputUpcall) throws NetIbisException {
                this.upcallFunc = inputUpcall;
                setupConnection(cnx);
        }

	/**
	 * Utility function to get a {@link NetReceiveBuffer} from our
	 * {@link NetBufferFactory}.
	 * This is only valid for a Factory with MTU.
	 *
         * @param contentsLength indicates how many bytes of data must be received.
         * 0 indicates that any length is fine and that the buffer.length field
         * should be filled with the length actually read.
	 * @throws an {@link NetIbisException} if the factory has no default MTU
         * @return the new {@link NetReceiveBuffer}.
	 */
	public NetReceiveBuffer createReceiveBuffer(int contentsLength) throws NetIbisException {
                NetReceiveBuffer b = (NetReceiveBuffer)createBuffer();
                b.length = contentsLength;
                return b;
	}

	/**
	 * Utility function to get a {@link NetReceiveBuffer} from our
	 * {@link NetBufferFactory}.
	 *
	 * @param length the length of the data stored in the buffer
         * @param contentsLength indicates how many bytes of data must be received.
         * 0 indicates that any length is fine and that the buffer.length field
         * should be filled with the length actually read.
         * @return the new {@link NetReceiveBuffer}.
	 */
	public NetReceiveBuffer createReceiveBuffer(int length, int contentsLength)
		throws NetIbisException {
                NetReceiveBuffer b = (NetReceiveBuffer)createBuffer(length);
                b.length = contentsLength;
                return b;
	}


	/*
         * Closes the I/O.
	 *
	 * Note: methods redefining this one should also call it, just in case
         *       we need to add something here
         * @exception NetIbisException if this operation fails.
	 */
	public void free()
		throws NetIbisException {
		activeNum = null;
		super.free();
	}

	/**
         * {@inheritDoc} 
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}


        /* ReadMessage Interface */

	/**
         * Complete the current incoming message extraction.
         *
         * Only one message is alive at one time for a given
         * receiveport. This is done to prevent flow control
         * problems. when a message is alive, and a new messages is
         * requested with a receive, the requester is blocked until
         * the live message is finished.
         *
         * @exception NetIbisException in case of trouble.
         */
       	public void finish() throws NetIbisException {
                if (_inputConvertStream != null) {
                        try {
                                _inputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }

                        _inputConvertStream = null;
                }
        }

        /**
         * Unimplemented.
         *
         * @return 0.
         */
	public long sequenceNumber() {
                return 0;
        }


        /**
         * Unimplemented.
         *
         * @return <code>null</code>.
         */
	public SendPortIdentifier origin() {
                return null;
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
        private ObjectInputStream        _inputConvertStream = null;



        /**
         * Check whether the convert stream should be initialized, and
         * initialize it when needed.
	 */
        private final void checkConvertStream() throws IOException {
                if (_inputConvertStream == null) {
                        DummyInputStream dis = new DummyInputStream();
                        _inputConvertStream = new ObjectInputStream(dis);
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>boolean</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final boolean defaultReadBoolean() throws NetIbisException {
                boolean result = false;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readBoolean();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readChar}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>char</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final char defaultReadChar() throws NetIbisException {
                char result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readChar();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readShort}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>short</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final short defaultReadShort() throws NetIbisException {
                short result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readShort();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readInt}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>int</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final int defaultReadInt() throws NetIbisException {
                int result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readInt();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readLong}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>long</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final long defaultReadLong() throws NetIbisException {
                long result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readLong();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readFloat}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>float</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final float defaultReadFloat() throws NetIbisException {
                float result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readFloat();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readDouble}.
         *
         * Note: this method must not be changed.
         *
         * @return the <code>double</code> value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final double defaultReadDouble() throws NetIbisException {
                double result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readDouble();
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readString}.
         *
         * Note: this method must not be changed.
         *
         * @return the {@link String string} value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final String defaultReadString() throws NetIbisException {
                String result = null;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readUTF();
                } catch (Exception e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readObject}.
         *
         * Note: this method must not be changed.
         *
         * @return the {@link Object object} value just read.
         * @exception NetIbisException in case of trouble.
         */
        private final Object defaultReadObject() throws NetIbisException {
                Object result = null;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readObject();
                } catch (Exception e) {
                        throw new NetIbisException(e.getMessage());
                }

                return result;
        }

        /**
         * Default implementation of {@link #readArraySliceBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readBoolean();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readByte();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readChar();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readShort();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readInt();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readLong();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readFloat();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readDouble();
                        }
                } catch (IOException e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Default implementation of {@link #readBoolean}.
         *
         * Note: this method must not be changed.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to read.
         * @exception NetIbisException in case of trouble.
         */
        private final void defaultReadArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readObject();
                        }
                } catch (Exception e) {
                        throw new NetIbisException(e.getMessage());
                }
        }

        /**
         * Atomic packet read function.
         *
         * @param expectedLength a hint about how many bytes are expected.
         * @exception NetIbisException in case of trouble.
         */
        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                int len = defaultReadInt();
		NetReceiveBuffer buffer = createReceiveBuffer(len);
                defaultReadArraySliceByte(buffer.data, 0, len);
                return buffer;
        }

        /**
         * Atomic packet read function.
         *
         * @param b the buffer to fill.
         * @exception NetIbisException in case of trouble.
         */
        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                int len = defaultReadInt();
                defaultReadArraySliceByte(buffer.data, 0, len);
                buffer.length = len;
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public boolean readBoolean() throws NetIbisException {
                return defaultReadBoolean();
        }

        /**
         * Extract a byte from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public abstract byte readByte() throws NetIbisException;

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public char readChar() throws NetIbisException {
                return defaultReadChar();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public short readShort() throws NetIbisException {
                return defaultReadShort();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public int readInt() throws NetIbisException {
                return defaultReadInt();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public long readLong() throws NetIbisException {
                return defaultReadLong();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public float readFloat() throws NetIbisException {
                return defaultReadFloat();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public double readDouble() throws NetIbisException {
                return defaultReadDouble();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public String readString() throws NetIbisException {
                return (String)defaultReadString();
        }

        /**
         * Extract an element from the current message.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public Object readObject() throws NetIbisException {
                return defaultReadObject();
        }


        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceBoolean(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceByte(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceChar(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceShort(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceInt(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceLong(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceFloat(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceDouble(b, o, l);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         * @param o the offset.
         * @param l the number of elements to extract.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceObject(b, o, l);
        }


        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayBoolean(boolean [] b) throws NetIbisException {
                readArraySliceBoolean(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayByte(byte [] b) throws NetIbisException {
                readArraySliceByte(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayChar(char [] b) throws NetIbisException {
                readArraySliceChar(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayShort(short [] b) throws NetIbisException {
                readArraySliceShort(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayInt(int [] b) throws NetIbisException {
                readArraySliceInt(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayLong(long [] b) throws NetIbisException {
                readArraySliceLong(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayFloat(float [] b) throws NetIbisException {
                readArraySliceFloat(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayDouble(double [] b) throws NetIbisException {
                readArraySliceDouble(b, 0, b.length);
        }

        /**
         * Extract some elements from the current message.
         *
         * @param b the array.
         *
         * @exception NetIbisException in case of trouble. 
         */
	public final void readArrayObject(Object [] b) throws NetIbisException {
                readArraySliceObject(b, 0, b.length);
        }


        /**
         * Internal dummy {@link InputStream} to be used as a byte stream source for
         * the {@link ObjectInputStream} based fallback serialization.
         */
        private final class DummyInputStream extends InputStream {

                /**
                 * {@inheritDoc}
                 *
                 * Note: the other read methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 *
                 */
                public int read() throws IOException {
                        int result = 0;

                        try {
                                result = readByte();
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }

                        return (result & 255);
                }
        }
}
