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
		setBufferFactory(new NetBufferFactory(new NetSendBufferFactoryDefaultImpl()));
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


	/**
	 * Create a {@link NetSendBuffer} using the installed factory.
	 *
	 * This is only valid for a Factory with MTU.
	 *
	 * @throws an {@link IbisIOException} if the factory has no default MTU
	 */
	public NetSendBuffer createSendBuffer() throws IbisIOException {
	    return (NetSendBuffer)createBuffer();
	}

	/**
	 * Utility function to get a NetSendBuffer from our NetBuffer factory
	 *
	 * @param length the length of the data stored in the buffer
	 */
	public NetSendBuffer createSendBuffer(int length)
		throws IbisIOException {
	    return (NetSendBuffer)createBuffer(length);
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

        private final void defaultWriteArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeBoolean(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeByte(b[o++]);
                                _outputConvertStream.flush();
                        }
                        // _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }
        
        private final void defaultWriteArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeChar(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeShort(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeInt(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeLong(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeFloat(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeDouble(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        private final void defaultWriteArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                try {
                        checkConvertStream();
                        while (l-- > 0) {
                                _outputConvertStream.writeObject(b[o++]);
                        }
                        _outputConvertStream.flush();
                } catch (IOException e) {
                        throw new IbisIOException(e.getMessage());
                }
        }

        public void writeByteBuffer(NetSendBuffer b) throws IbisIOException {
                defaultWriteInt(b.length);
                defaultWriteArraySliceByte(b.data, 0, b.length);
		if (! b.ownershipClaimed) {
		    b.free();
		}
        }

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

        public void writeArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceBoolean(b, o, l);
        }

        public void writeArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceByte(b, o, l);
        }
        public void writeArraySliceChar(char [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceChar(b, o, l);
        }

        public void writeArraySliceShort(short [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceShort(b, o, l);
        }

        public void writeArraySliceInt(int [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceInt(b, o, l);
        }

        public void writeArraySliceLong(long [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceLong(b, o, l);
        }

        public void writeArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceFloat(b, o, l);
        }

        public void writeArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceDouble(b, o, l);
        }

        public void writeArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
                defaultWriteArraySliceObject(b, o, l);
        }


        public final void writeArrayBoolean(boolean [] b) throws IbisIOException {
                writeArraySliceBoolean(b, 0, b.length);
        }

        public final void writeArrayByte(byte [] b) throws IbisIOException {
                writeArraySliceByte(b, 0, b.length);
        }

        public final void writeArrayChar(char [] b) throws IbisIOException {
                writeArraySliceChar(b, 0, b.length);
        }

        public final void writeArrayShort(short [] b) throws IbisIOException {
                writeArraySliceShort(b, 0, b.length);
        }

        public final void writeArrayInt(int [] b) throws IbisIOException {
                writeArraySliceInt(b, 0, b.length);
        }


        public final void writeArrayLong(long [] b) throws IbisIOException {
                writeArraySliceLong(b, 0, b.length);
        }

        public final void writeArrayFloat(float [] b) throws IbisIOException {
                writeArraySliceFloat(b, 0, b.length);
        }

        public final void writeArrayDouble(double [] b) throws IbisIOException {
                writeArraySliceDouble(b, 0, b.length);
        }

        public final void writeArrayObject(Object [] b) throws IbisIOException {
                writeArraySliceObject(b, 0, b.length);
        }

        private final class DummyOutputStream extends OutputStream {
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
