package ibis.ipl.impl.net;

import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
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
 * Provides an abstraction of a network output.
 */
public abstract class NetOutput extends NetIO implements WriteMessage {
	/**
	 * Constructor.
	 *
	 * @param staticProperties the properties of the corresponding
	 *                         {@linkplain NetReceivePort receive port}.
	 * @param driver the output's driver.
	 * @param output the controlling output or <code>null</code>
	 *               if this output is a root output.
	 */
	protected NetOutput(NetPortType      portType,
			   NetDriver 	    driver,
			   NetIO  	    up,
                           String           context) {
		super(portType, driver, up, context);
	}

        /**
         * Prepare the output for a new message transmission.
         */
	public void initSend() throws IbisIOException {
                //
        }


        /* WriteMessage Interface */                

        /**
	 * Start sending the message to all ReceivePorts this SendPort is connected to.
	 * Data may be streamed, so the user is not allowed to touch the data, as the send is NON-blocking.
	 * @exception IbisIOException       an error occurred 
	 **/
        public void send() throws IbisIOException {
                //
        }

        /**
	   Block until the entire message has been sent and clean up the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished. **/
        public void finish() throws IbisIOException{
                //(new Throwable()).printStackTrace();
                //System.err.println("NetOutput: finish -->");
                if (_outputConvertStream != null) {
                        try {
                                _outputConvertStream.close();
                        } catch (IOException e) {
                                throw new IbisIOException(e.getMessage());
                        }

                        _outputConvertStream = null;
                }
                //System.err.println("NetOutput: finish <--");
        }

        /**
	   If doSend, invoke send().
           Then block until the entire message has been sent and clear data within the message. Only after finish() or reset(), the data that was written
	   may be touched. Only one message is alive at one time for a given sendport. This is done to prevent flow control problems. 
	   When a message is alive and a new messages is requested, the requester is blocked until the
	   live message is finished.
	   The message stays alive for subsequent writes and sends.
	   reset can be seen as a shorthand for (possibly send();) finish(); sendPort.newMessage() **/
        public void reset(boolean doSend) throws IbisIOException {
                //System.err.println("NetOutput: reset("+doSend+") -->");
                if (doSend) {
                        send();
                } else {
                        throw new Error("full reset unimplemented");
                }
                
                if (_outputConvertStream != null) {
                        try {
                                _outputConvertStream.close();
                        } catch (IOException e) {
                                throw new IbisIOException(e.getMessage());
                        }

                        _outputConvertStream = null;
                }
                //System.err.println("NetOutput: reset <--");
        }

        /**
	   Return the number of bytes that was written to the message, in the stream dependant format.
	   This is the number of bytes that will be sent over the network **/
        public int getCount() {
                return 0;
        }

        /** Reset the counter */
        public void resetCount() {
                __.unimplemented__("resetCount");
        }





        /* fallback serialization implementation */

        /**
         * Object stream for the internal fallback serialization.
         */
        private ObjectOutputStream _outputConvertStream = null;

        private final void checkConvertStream() throws IOException {
                if (_outputConvertStream == null) {
                        DummyOutputStream dos = new DummyOutputStream();
                        _outputConvertStream = new ObjectOutputStream(dos);
                        _outputConvertStream.flush();
                }
        }        

        private final void defaultWriteBoolean(boolean value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeBoolean(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteChar(char value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeChar(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteShort(short value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeShort(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteInt(int value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeInt(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteLong(long value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeLong(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteFloat(float value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeFloat(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteDouble(double value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeDouble(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteString(String value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeUTF(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteObject(Object value) throws IbisIOException {
                try {
                        checkConvertStream();
                        _outputConvertStream.writeObject(value);
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeBoolean(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeByte(userBuffer[offset++]);
                                _outputConvertStream.flush();
                        }
                        // _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }
        
        private final void defaultWriteSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeChar(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeShort(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeInt(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeLong(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeFloat(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (length-- > 0) {
                                _outputConvertStream.writeDouble(userBuffer[offset++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        public abstract void writeByteBuffer(NetSendBuffer buffer) throws IbisIOException;
        

        /**
	 * Writes a boolean value to the message.
	 * @param     value             The boolean value to write.
	 */
        public void writeBoolean(boolean value) throws IbisIOException {
                defaultWriteBoolean(value);
        }

        /**
	 * Writes a byte value to the message.
	 * @param     value             The byte value to write.
	 */
        public abstract void writeByte(byte value) throws IbisIOException;

        /**
	 * Writes a char value to the message.
	 * @param     value             The char value to write.
	 */
        public void writeChar(char value) throws IbisIOException {
                defaultWriteChar(value);
        }

        /**
	 * Writes a short value to the message.
	 * @param     value             The short value to write.
	 */
        public void writeShort(short value) throws IbisIOException {
                defaultWriteShort(value);
        }

        /**
	 * Writes a int value to the message.
	 * @param     value             The int value to write.
	 */
        public void writeInt(int value) throws IbisIOException {
                defaultWriteInt(value);
        }


        /**
	 * Writes a long value to the message.
	 * @param     value             The long value to write.
	 */
        public void writeLong(long value) throws IbisIOException {
                defaultWriteLong(value);
        }

        /**
	 * Writes a float value to the message.
	 * @param     value             The float value to write.
	 */
        public void writeFloat(float value) throws IbisIOException {
                defaultWriteFloat(value);
        }

        /**
	 * Writes a double value to the message.
	 * @param     value             The double value to write.
	 */
        public void writeDouble(double value) throws IbisIOException {
                defaultWriteDouble(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeString(String value) throws IbisIOException {
                defaultWriteString(value);
        }

        /**
	 * Writes a Serializable object to the message.
	 * @param     value             The object value to write.
	 */
        public void writeObject(Object value) throws IbisIOException {
                defaultWriteObject(value);
        }

        public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayBoolean(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayByte(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayChar(char [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayChar(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayShort(short [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayShort(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayInt(int [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayInt(userBuffer, 0, userBuffer.length);
        }


        public void writeArrayLong(long [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayLong(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayFloat(float [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayFloat(userBuffer, 0, userBuffer.length);
        }

        public void writeArrayDouble(double [] userBuffer) throws IbisIOException {
                defaultWriteSubArrayDouble(userBuffer, 0, userBuffer.length);
        }

        public void writeSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayBoolean(userBuffer, offset, length);
        }

        public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayByte(userBuffer, offset, length);
        }
        public void writeSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayChar(userBuffer, offset, length);
        }

        public void writeSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayShort(userBuffer, offset, length);
        }

        public void writeSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayInt(userBuffer, offset, length);
        }

        public void writeSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayLong(userBuffer, offset, length);
        }

        public void writeSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayFloat(userBuffer, offset, length);
        }

        public void writeSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                defaultWriteSubArrayDouble(userBuffer, offset, length);
        }


        private class DummyOutputStream extends OutputStream {
                private long seq = 0;
                public void write(int b) throws IOException {
                        try {
                                writeByte((byte)b);
                                //System.err.println("Sent a byte: ["+ seq++ +"] unsigned = "+(b & 255)+", signed =" + (byte)b);
                        } catch (IbisIOException e) {
                                throw new IOException(e.getMessage());
                        }
                }

                /*
                 * Note: the other write methods must _not_ be overloaded
                 *       because the ObjectInput/OutputStream do not guaranty
                 *       symmetrical transactions.
                 */
        }

}
