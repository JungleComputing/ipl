package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
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
	protected Integer        activeNum  = null;
        protected NetInputUpcall upcallFunc = null;

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
	}

        public synchronized void inputUpcall(NetInput input, Integer spn) {
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
	 * @exception IbisIOException if the polling fails (!= the
	 * polling is unsuccessful).
	 */
	public abstract Integer poll() throws IbisIOException;

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

	public void setupConnection(Integer            rpn,
                                    ObjectInputStream  is,
                                    ObjectOutputStream os,
                                    NetServiceListener nsl,
                                    NetInputUpcall     inputUpcall) throws IbisIOException {
                this.upcallFunc = inputUpcall;
                setupConnection(rpn, is, os, nsl);
        }
        


	/**
	 * Closes this input.
	 *
	 * Node: methods redefining this one should call it at the end.
	 */
	public void free() 
		throws IbisIOException {
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
       	public void finish() throws IbisIOException {
                //System.err.println("NetInput: finish -->");
                if (_inputConvertStream != null) {
                        try {
                                _inputConvertStream.close();
                        } catch (IOException e) {
                                throw new IbisIOException(e.getMessage());
                        }
                        
                        _inputConvertStream = null;
                }

                activeNum = null;
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

        private final boolean defaultReadBoolean() throws IbisIOException {
                boolean result = false;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readBoolean();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final char defaultReadChar() throws IbisIOException {
                char result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readChar();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final short defaultReadShort() throws IbisIOException {
                short result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readShort();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final int defaultReadInt() throws IbisIOException {
                int result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readInt();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final long defaultReadLong() throws IbisIOException {
                long result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readLong();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final float defaultReadFloat() throws IbisIOException {
                float result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readFloat();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final double defaultReadDouble() throws IbisIOException {
                double result = 0;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readDouble();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final String defaultReadString() throws IbisIOException {
                String result = null;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readUTF();
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final Object defaultReadObject() throws IbisIOException {
                Object result = null;

                try {
                        checkConvertStream();
                        result = _inputConvertStream.readObject();
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
                
                return result;
        }

        private final void defaultReadArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readBoolean();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readByte();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readChar();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readShort();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readInt();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readLong();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readFloat();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readDouble();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                b[o++] = _inputConvertStream.readObject();
                        }
                } catch (Exception e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        
        public NetReceiveBuffer readByteBuffer(int expectedLength) throws IbisIOException {
                int len = defaultReadInt();
                byte [] b = new byte[len];
                defaultReadArraySliceByte(b, 0, len);
                return new NetReceiveBuffer(b, len);
        }
        

        public void readByteBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                int len = defaultReadInt();
                defaultReadArraySliceByte(buffer.data, 0, len);
                buffer.length = len;
        }
        

	public boolean readBoolean() throws IbisIOException {
                return defaultReadBoolean();
        }

	public abstract byte readByte() throws IbisIOException;

	public char readChar() throws IbisIOException {
                return defaultReadChar();
        }

	public short readShort() throws IbisIOException {
                return defaultReadShort();
        }

	public int readInt() throws IbisIOException {
                return defaultReadInt();
        }

	public long readLong() throws IbisIOException {
                return defaultReadLong();
        }
	
	public float readFloat() throws IbisIOException {
                return defaultReadFloat();
        }

	public double readDouble() throws IbisIOException {
                return defaultReadDouble();
        }

	public String readString() throws IbisIOException {
                return (String)defaultReadString();
        }

	public Object readObject() throws IbisIOException {
                return defaultReadObject();
        }


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceBoolean(b, o, l);
        }

	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceByte(b, o, l);
        }

	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceChar(b, o, l);
        }

	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceShort(b, o, l);
        }

	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceInt(b, o, l);
        }

	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceLong(b, o, l);
        }

	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceFloat(b, o, l);
        }

	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceDouble(b, o, l);
        }

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                defaultReadArraySliceObject(b, o, l);
        }


	public final void readArrayBoolean(boolean [] b) throws IbisIOException {
                readArraySliceBoolean(b, 0, b.length);
        }

	public final void readArrayByte(byte [] b) throws IbisIOException {
                readArraySliceByte(b, 0, b.length);
        }

	public final void readArrayChar(char [] b) throws IbisIOException {
                readArraySliceChar(b, 0, b.length);
        }

	public final void readArrayShort(short [] b) throws IbisIOException {
                readArraySliceShort(b, 0, b.length);
        }

	public final void readArrayInt(int [] b) throws IbisIOException {
                readArraySliceInt(b, 0, b.length);
        }

	public final void readArrayLong(long [] b) throws IbisIOException {
                readArraySliceLong(b, 0, b.length);
        }

	public final void readArrayFloat(float [] b) throws IbisIOException {
                readArraySliceFloat(b, 0, b.length);
        }

	public final void readArrayDouble(double [] b) throws IbisIOException {
                readArraySliceDouble(b, 0, b.length);
        }

	public final void readArrayObject(Object [] b) throws IbisIOException {
                readArraySliceObject(b, 0, b.length);
        }


        private final class DummyInputStream extends InputStream {
                private long seq = 0;


                public int read() throws IOException {
                        int result = 0;
                        
                        try {
                                result = readByte();
                                //System.err.println("Received a byte: ["+ seq++ +"] unsigned = "+(result & 255)+", signed =" + result);
                        } catch (IbisIOException e) {
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
