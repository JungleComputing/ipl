package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReadMessage;
import ibis.ipl.SendPortIdentifier;

import ibis.ipl.StaticProperties;

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
public abstract class NetInput extends NetIO implements ReadMessage {
	/**
	 * Active {@linkplain NetSendPort send port} number or <code>null</code> if
	 * no send port is active.
	 */
	protected Integer activeNum = null;

	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponing
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the input's driver.
	 * @param input the controlling input or <code>null</code> if this input is a
	 *              root input.
	 */
	protected NetInput(StaticProperties staticProperties,
			   NetDriver 	    driver,
			   NetInput  	    input) {
		super(staticProperties, driver, input);
	}

	/**
	 * Unblockingly tests for incoming data.
	 *
	 * Note: if <code>doPoll</code> is called again immediately
	 * after a successful doPoll without extracting the message and
	 * {@linkplain #release releasing} the input, the result is
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

        private final void defaultReadSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readBoolean();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readByte();
                        }
                } catch (IOException e) {
                        e.printStackTrace();
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readChar();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readShort();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readInt();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readLong();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readFloat();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultReadSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                userBuffer[offset++] = _inputConvertStream.readDouble();
                        }
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
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

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                defaultReadSubArrayBoolean(userBuffer, 0, userBuffer.length);
        }


	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
                defaultReadSubArrayByte(userBuffer, 0, userBuffer.length);
        }


	public void readArrayChar(char [] userBuffer) throws IbisIOException {
                defaultReadSubArrayChar(userBuffer, 0, userBuffer.length);
        }


	public void readArrayShort(short [] userBuffer) throws IbisIOException {
                defaultReadSubArrayShort(userBuffer, 0, userBuffer.length);
        }


	public void readArrayInt(int [] userBuffer) throws IbisIOException {
                defaultReadSubArrayInt(userBuffer, 0, userBuffer.length);
        }


	public void readArrayLong(long [] userBuffer) throws IbisIOException {
                defaultReadSubArrayLong(userBuffer, 0, userBuffer.length);
        }


	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
                defaultReadSubArrayFloat(userBuffer, 0, userBuffer.length);
        }


	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
                defaultReadSubArrayDouble(userBuffer, 0, userBuffer.length);
        }



	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayBoolean(userBuffer, offset, length);
        }


	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayByte(userBuffer, offset, length);
        }


	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayChar(userBuffer, offset, length);
        }


	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayShort(userBuffer, offset, length);
        }


	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayInt(userBuffer, offset, length);
        }


	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayLong(userBuffer, offset, length);
        }


	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayFloat(userBuffer, offset, length);
        }


	public void readSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                defaultReadSubArrayDouble(userBuffer, offset, length);
        }

        private class DummyInputStream extends InputStream {
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
