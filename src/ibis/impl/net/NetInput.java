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
 * Provides an abstraction of a network input.
 */
public abstract class NetInput extends NetIO implements ReadMessage, NetInputUpcall {
	/**
	 * Active {@linkplain NetSendPort send port} number or <code>null</code> if
	 * no send port is active.
	 */
	protected volatile Integer        activeNum  = null;
        protected          NetInputUpcall upcallFunc = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponing
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the input's driver.
	 * @param input the controlling input or <code>null</code> if this input is a
	 *              root input.
	 */
	protected NetInput(NetPortType      portType,
			   NetDriver 	    driver,
			   NetIO  	    up,
                           String           context) {
		super(portType, driver, up, context);
		// setBufferFactory(new NetBufferFactory(new NetReceiveBufferFactoryDefaultImpl()));
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) throws NetIbisException {
                activeNum = spn;
                upcallFunc.inputUpcall(this, spn);
                activeNum = null;
        }

	/**
	 * Unblockingly tests for incoming data.
	 *
	 * Note: if <code>doPoll</code> is called again immediately
	 * after a successful doPoll without extracting the message and
	 * {@linkplain #finish finishing} the input, the result is
	 * undefined and data might get lost. Use {@link
	 * #getActiveSendPortNum} instead.
	 *
	 * @return the send port's corresponding integer or <code>null</code> if
	 * no data is available.
	 * @exception NetIbisException if the polling fails (!= the
	 * polling is unsuccessful).
	 */
	public abstract Integer poll() throws NetIbisException;

	/**
	 * Returns the active send port related integer or <code>null</code> if
	 * no send port is active.
	 *
	 * @return the send port's corresponding local integer or <code>null</code> if
	 * no send port is active.
	 */
	public final Integer getActiveSendPortNum() {
		return activeNum;
	}

	public void setupConnection(NetConnection  cnx,
                                    NetInputUpcall inputUpcall) throws NetIbisException {
                this.upcallFunc = inputUpcall;
                setupConnection(cnx);
        }

	/**
	 * Utility function to get a {@link NetReceiveBuffer} from our
	 * {@link NetBufferFactory}.
	 * This is only valid for a Factory with MTU.
         * 'contentsLength' indicates how many bytes of data must be received.
         * 0 indicates that any length is fine and that the buffer.length field
         * should be filled with the length actually read.
	 *
	 * @throws an {@link NetIbisException} if the factory has no default MTU
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
	 */
	public NetReceiveBuffer createReceiveBuffer(int length, int contentsLength)
		throws NetIbisException {
                NetReceiveBuffer b = (NetReceiveBuffer)createBuffer(length);
                b.length = contentsLength;
                return b;
	}


	/**
	 * Closes this input.
	 *
	 * Node: methods redefining this one should call it at the end.
	 */
	public void free()
		throws NetIbisException {
		activeNum = null;
		super.free();
	}

	/**
	 * Finalizes this input.
	 *
	 * Node: methods redefining this one should call it at the end.
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}


        /* ReadMessage Interface */

	/** Only one message is alive at one time for a given receiveport.
	    This is done to prevent flow control problems.
	    when a message is alive, and a new messages is requested with a receive, the requester is blocked until the
	    live message is finished. **/
       	public void finish() throws NetIbisException {
                //System.err.println("NetInput: finish -->");
                if (_inputConvertStream != null) {
                        try {
                                _inputConvertStream.close();
                        } catch (IOException e) {
                                throw new NetIbisException(e.getMessage());
                        }

                        _inputConvertStream = null;
                }
                //System.err.println("NetInput: finish <--");
        }

	public long sequenceNumber() {
                return 0;
        }


	public SendPortIdentifier origin() {
                return null;
        }




        /* fallback serialization implementation */

        /**
         * Object stream for the internal fallback serialization.
	 */
        private ObjectInputStream        _inputConvertStream = null;



        private final void checkConvertStream() throws IOException {
                if (_inputConvertStream == null) {
                        DummyInputStream dis = new DummyInputStream();
                        _inputConvertStream = new ObjectInputStream(dis);
                }
        }

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


        public NetReceiveBuffer readByteBuffer(int expectedLength) throws NetIbisException {
                int len = defaultReadInt();
		NetReceiveBuffer buffer = createReceiveBuffer(len);
                defaultReadArraySliceByte(buffer.data, 0, len);
                return buffer;
        }


        public void readByteBuffer(NetReceiveBuffer buffer) throws NetIbisException {
                int len = defaultReadInt();
                defaultReadArraySliceByte(buffer.data, 0, len);
                buffer.length = len;
        }


	public boolean readBoolean() throws NetIbisException {
                return defaultReadBoolean();
        }

	public abstract byte readByte() throws NetIbisException;

	public char readChar() throws NetIbisException {
                return defaultReadChar();
        }

	public short readShort() throws NetIbisException {
                return defaultReadShort();
        }

	public int readInt() throws NetIbisException {
                return defaultReadInt();
        }

	public long readLong() throws NetIbisException {
                return defaultReadLong();
        }

	public float readFloat() throws NetIbisException {
                return defaultReadFloat();
        }

	public double readDouble() throws NetIbisException {
                return defaultReadDouble();
        }

	public String readString() throws NetIbisException {
                return (String)defaultReadString();
        }

	public Object readObject() throws NetIbisException {
                return defaultReadObject();
        }


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceBoolean(b, o, l);
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceByte(b, o, l);
        }

	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceChar(b, o, l);
        }

	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceShort(b, o, l);
        }

	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceInt(b, o, l);
        }

	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceLong(b, o, l);
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceFloat(b, o, l);
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
                defaultReadArraySliceObject(b, o, l);
        }


	public final void readArrayBoolean(boolean [] b) throws NetIbisException {
                readArraySliceBoolean(b, 0, b.length);
        }

	public final void readArrayByte(byte [] b) throws NetIbisException {
                readArraySliceByte(b, 0, b.length);
        }

	public final void readArrayChar(char [] b) throws NetIbisException {
                readArraySliceChar(b, 0, b.length);
        }

	public final void readArrayShort(short [] b) throws NetIbisException {
                readArraySliceShort(b, 0, b.length);
        }

	public final void readArrayInt(int [] b) throws NetIbisException {
                readArraySliceInt(b, 0, b.length);
        }

	public final void readArrayLong(long [] b) throws NetIbisException {
                readArraySliceLong(b, 0, b.length);
        }

	public final void readArrayFloat(float [] b) throws NetIbisException {
                readArraySliceFloat(b, 0, b.length);
        }

	public final void readArrayDouble(double [] b) throws NetIbisException {
                readArraySliceDouble(b, 0, b.length);
        }

	public final void readArrayObject(Object [] b) throws NetIbisException {
                readArraySliceObject(b, 0, b.length);
        }


        private final class DummyInputStream extends InputStream {
                private long seq = 0;


                public int read() throws IOException {
                        int result = 0;

                        try {
                                result = readByte();
                                //System.err.println("Received a byte: ["+ seq++ +"] unsigned = "+(result & 255)+", signed =" + result);
                        } catch (NetIbisException e) {
                                throw new IOException(e.getMessage());
                        }

                        return (result & 255);
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }
}
