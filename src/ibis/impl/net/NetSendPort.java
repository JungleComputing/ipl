package ibis.ipl.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.WriteMessage;

import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Provides an implementation of the {@link SendPort} and {@link
 * WriteMessage} interfaces of the IPL.
 */
public final class NetSendPort implements SendPort, WriteMessage {

	/**
	 * The type of the port.
	 */
	private NetPortType           type       	     = null;

	/**
	 * The name of the port, or <code>null</code> if the port is anonymous.
	 */
	private String                name       	     = null;

	/**
	 * The port identifier.
	 */
	private NetSendPortIdentifier identifier 	     = null;

	/**
	 * The topmost driver.
	 */
	private NetDriver             driver     	     = null;

	/**
	 * The topmost network output.
	 */
	private NetOutput             output     	     = null;

	/**
	 * The network output synchronization lock.
	 */
	private NetMutex              outputLock   	     = null;
	
	/**
	 * The next integer remote port number.
	 *
	 * Note: the use of the <code>Integer</code> type instead of <code>int</code>
	 * allows for:
	 * <UL>
	 * <LI> The use of the <code>null</code> value.
	 * <LI> The use of the number as an {@link Hashtable} key.
	 * </UL>
	 */
	private Integer   	      nextReceivePortNum     = new Integer(0);

	/**
	 * The table of remote port identifiers indexed by their number.
	 */
	private Hashtable 	      receivePortIdentifiers = null;

	/**
	 * The table of remote port service sockets indexed by their number.
	 */
	private Hashtable    	      receivePortSockets     = null;

	/**
	 * The table of remote port service TCP inputs indexed by their number.
	 */
	private Hashtable             receivePortIs          = null;

	/**
	 * The table of remote port service TCP outputs indexed by their number.
	 */
	private Hashtable             receivePortOs          = null;

	/**
	 * The current memory block allocator.
	 */
	private NetAllocator          allocator              = null;

	/**
	 * The current maximum transfert unit.
	 */
	private int           	      mtu        	     =    0;

	/**
	 * The current buffer offset of the payload area.
	 */
	private int           	      dataOffset 	     =    0;

	/**
	 * The current buffer offset for appending user data.
	 */
	private int           	      bufferOffset 	     =    0;

	/**
	 * The current buffer.
	 */
	private NetSendBuffer 	      buffer       	     = null;

	/**
	 * The empty message detection flag.
	 *
	 * The flag is set on each new {@link #newMessage} call and should
	 * be cleared as soon as at least a byte as been added to the living message.
	 */
	private boolean       	      emptyMsg     	     = true;


	/* --- SendPort part --- */

	/**
	 * Constructor for a anonymous send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 */
	public NetSendPort(NetPortType type) throws IbisIOException {
		this(type, null);
	}

	/**
	 * Constructor for a named send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 * @param name the name of the port.
	 */
	public NetSendPort(NetPortType type, String name)
		throws IbisIOException {
		this.name	  	 = name;
		this.type	  	 = type;
		NetIbis           ibis   = type.getIbis();
		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();
		identifier               = new NetSendPortIdentifier(name, type.name(), ibisId);
		outputLock 		 = new NetMutex(false);
		driver   		 = type.getDriver();
		output   		 = driver.newOutput(type.properties(), null);
		receivePortIdentifiers 	 = new Hashtable();
		receivePortSockets     	 = new Hashtable();
		receivePortIs          	 = new Hashtable();
		receivePortOs          	 = new Hashtable();
	}

	/**
	 * Starts the construction of a new message.
	 *
	 * @return The message instance.
	 */	
	public WriteMessage newMessage() throws IbisIOException {
		emptyMsg     = true;
		bufferOffset = 0;
		outputLock.lock();
                output.initSend();
		return this;
	}

	/**
	 * Returns an instance to the current memory block allocator.
	 *
	 * @return The allocator instance.
	 */
	protected NetAllocator getAllocator() {
		int mtu = output.getMaximumTransfertUnit();

		if (mtu == 0)
			return null;

		if (allocator == null || allocator.getBlockSize() != mtu) {
			allocator = new NetAllocator(mtu);
		}

		return allocator;
	}	

	public DynamicProperties properties() {
		return null;
	}

	/**
	 * Returns the port {@linkplain NetSendPortIdentifier identifier}.
	 *
	 * @return The identifier instance.
	 */
	public SendPortIdentifier identifier() {
		return identifier;
	}

	/**
	 * Attempts to connect the send port to a specified receive port.
	 *
	 * @param rpi the identifier of the peer port.
	 */
	public void connect(ReceivePortIdentifier rpi)
		throws IbisIOException {
		outputLock.lock();
		NetReceivePortIdentifier nrpi	       = (NetReceivePortIdentifier)rpi;
		Hashtable                info	       = nrpi.connectionInfo();
		InetAddress              remoteAddress = (InetAddress) info.get("accept_address");
		int                      remotePort    = ((Integer) info.get("accept_port")).intValue();
		Socket             	 s 	       = null;
		ObjectInputStream  	 is	       = null;
		ObjectOutputStream 	 os	       = null;

		try {
			s   = new Socket(remoteAddress, remotePort);
			os  = new ObjectOutputStream(s.getOutputStream());
			is  = new ObjectInputStream(s.getInputStream());
			os.writeObject(identifier);
		} catch (IOException e) {
			throw new IbisIOException(e);
		}

		Integer rpn = nextReceivePortNum;
		nextReceivePortNum = new Integer(rpn.intValue()+1);
		receivePortIdentifiers.put(rpn, nrpi);
		receivePortSockets.put(rpn, s);
		receivePortIs.put(rpn, is);
		receivePortOs.put(rpn, os);
		output.setupConnection(rpn, is, os);
				
		mtu = output.getMaximumTransfertUnit();

		if (mtu != 0) {
			if (allocator == null || allocator.getBlockSize() != mtu) {
				allocator = new NetAllocator(mtu);
			}
		}

		/*
		 * TODO: update receivePorts when dataOffset changes
		 */
		dataOffset = output.getHeadersLength();

		outputLock.unlock();
	}

	/**
	 * Interruptible connect.
	 *
	 * <strong>Not implemented.</strong>
	 * @param rpi the identifier of the peer port.
	 * @param timeout_millis the connection timeout in milliseconds.
	 */
	public void connect(ReceivePortIdentifier rpi,
			    int                   timeout_millis)
		throws IbisIOException {
		__.unimplemented__("connect");
	}

	/**
	 * Closes the port.
	 *
	 * Note: this function might block until the living message is finalized.
	 */
	public synchronized void free()
		throws IbisIOException {
		try {
			if (outputLock != null) {
				outputLock.lock();
			}
		
			if (output != null) {
				output.free();
			}

			if (receivePortOs != null) {
				Enumeration e = receivePortOs.keys();

				while (e.hasMoreElements()) {
					Object             key   = e.nextElement();
					Object             value = receivePortOs.remove(key);
					ObjectOutputStream os    = (ObjectOutputStream)value;

                                        os.close();
				}	
			}
		
			if (receivePortIs != null) {
				Enumeration e = receivePortIs.keys();

				while (e.hasMoreElements()) {
					Object            key   = e.nextElement();
					Object            value = receivePortIs.remove(key);
					ObjectInputStream is    = (ObjectInputStream)value;

                                        is.close();                                        
				}	
			}
		
			if (receivePortSockets != null) {
				Enumeration e = receivePortSockets.keys();

				while (e.hasMoreElements()) {
					Object key   = e.nextElement();
					Object value = receivePortSockets.remove(key);
					Socket s     = (Socket)value;
                                        
                                        if (!s.isClosed()) {
                                                s.close();
                                        }
				}	
			}
		
			/*
			 * just to be sure that nobody is going to use that
			 * send port again...
			 */
			type       	       = null;
			name       	       = null;
			identifier 	       = null;
			driver     	       = null;
			output     	       = null;
			outputLock   	       = null;
			nextReceivePortNum     = null;
			receivePortIdentifiers = null;
			receivePortSockets     = null;
			receivePortIs          = null;
			receivePortOs          = null;
		} catch (Exception e) {
                        __.fwdAbort__(e);
		}
	}
	
	/**
	 * Ensures correct clean-up.
	 */
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}


	/* --- WriteMessage part --- */

	/**
	 * Sends the current buffer over the network.
	 */
	private void flush() throws IbisIOException{
		if (buffer != null) {
			output.sendBuffer(buffer);
			/*
			try {
				Thread.sleep(500); // temporisation test for UDP
			} catch (Exception e) {
				// ignore
			}
			*/
			buffer.free();
			buffer = null;
		}

		bufferOffset = 0;
	}

	/**
	 * Allocate a new buffer.
	 *
	 * @param the preferred length. This is just a hint. The
	 * actual buffer length may differ.
	 */
	private void allocateBuffer(int length) {
		if (buffer != null) {
			buffer.free();
		}
		
		if (allocator != null) {
			buffer = new NetSendBuffer(allocator.allocate(), dataOffset, allocator);
		} else {
			if (mtu != 0) {
				length = mtu;
			} else {
				length += dataOffset;
			}		
			buffer = new NetSendBuffer(new byte[length], dataOffset);
		}
		
		bufferOffset = dataOffset;
	}

	// TODO: ensure that send is non-blocking
	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws IbisIOException{
		flush();
	}

	// TODO: ensure that data has actually been sent
	/**
	 * Completes the message transmission.
	 *
	 * Note: if it is detected that the message is actually empty,
	 * a single byte is forced to be sent over the network.
	 */
	private void _finish() throws  IbisIOException{
		if (emptyMsg) {
			writeByte((byte)0);
		}
		
		flush();
	}
	
	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IbisIOException{
		_finish();
		output.release();
		outputLock.unlock();
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws IbisIOException {
		if (doSend) {
			send();
		}
		_finish();
		output.reset();
	}

	public int getCount() {
		return 0;
	}

	public void resetCount() {
		//
	}

	public void writeBoolean(boolean value) throws IbisIOException {
		//
	}

	/**
	 * Appends a byte to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param value the byte to append to the message.
	 */
	public void writeByte(byte value) throws IbisIOException {
		emptyMsg = false;
		
		if (buffer == null) {
			allocateBuffer(1);
		}

		buffer.data[bufferOffset] = value;
		buffer.length++;
		bufferOffset++;

		if (bufferOffset >= buffer.data.length) {
			flush();
		}
	}

	public void writeChar(char value) throws IbisIOException {
		//
	}

	public void writeShort(short value) throws IbisIOException {
		//
	}

	public void writeInt(int value) throws IbisIOException {
		//
	}

	public void writeLong(long value) throws IbisIOException {
		//
	}
	
	public void writeFloat(float value) throws IbisIOException {
		//
	}

	public void writeDouble(double value) throws IbisIOException {
		//
	}

	public void writeString(String value) throws IbisIOException {
		//
	}

	public void writeObject(Object value) throws IbisIOException {
		//
	}

	public void writeArrayBoolean(boolean [] destination) throws IbisIOException {
		//
	}
	
	/**
	 * Appends a byte array to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param userBuffer the byte array to append to the message.
	 */
	public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
		writeSubArrayByte(userBuffer, 0, userBuffer.length);
	}
	
	public void writeArrayChar(char [] destination) throws IbisIOException {
		//
	}
	
	public void writeArrayShort(short [] destination) throws IbisIOException {
		//
	}
	
	public void writeArrayInt(int [] destination) throws IbisIOException {
		//
	}
	
	public void writeArrayLong(long [] destination) throws IbisIOException {
		//
	}
	
	public void writeArrayFloat(float [] destination) throws IbisIOException {
		//
	}
	
	public void writeArrayDouble(double [] destination) throws IbisIOException {
		//
	}
	

	public void writeSubArrayBoolean(boolean [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
		// System.err.println("write: "+offset+", "+length);
		if (length == 0)
			return;
		
		emptyMsg = false;

                if (dataOffset == 0) {
                        if (buffer != null) {
                                flush();
                        }

                        if (mtu != 0) {
                                int base = offset;

                                do {
                                        int copyLength = Math.min(mtu, length);
                                        buffer = new NetSendBuffer(userBuffer, base, copyLength);
                                        flush();

                                        base   += copyLength;
                                        length -= copyLength;
                                } while (length != 0);
                                        
                        } else {
                                buffer = new NetSendBuffer(userBuffer, offset, length);
                                flush();
                        }
                } else {
                        if (buffer != null) {
                                int availableLength = buffer.data.length - bufferOffset;
                                int copyLength      = Math.min(availableLength, length);

                                System.arraycopy(userBuffer, offset, buffer.data, bufferOffset, copyLength);

                                bufferOffset  	 += copyLength;
                                buffer.length 	 += copyLength;
                                availableLength  -= copyLength;
                                offset        	 += copyLength;
                                length        	 -= copyLength;

                                if (availableLength == 0) {
                                        flush();
                                }
                        }
		
                        while (length > 0) {
                                allocateBuffer(length);

                                int availableLength = buffer.data.length - bufferOffset;
                                int copyLength   = Math.min(availableLength, length);

                                System.arraycopy(userBuffer, offset, buffer.data, bufferOffset, copyLength);

                                bufferOffset  	+= copyLength;
                                buffer.length 	+= copyLength;
                                availableLength -= copyLength;
                                offset        	+= copyLength;
                                length        	-= copyLength;

                                if (availableLength == 0) {
                                        flush();
                                }
                        }
                }
                
		// System.err.println("write: "+offset+", "+length+": ok");
	}
	
	public void writeSubArrayChar(char [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayShort(short [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayInt(int [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayLong(long [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayFloat(float [] destination, int offset, int size) throws IbisIOException {
		//
	}
	
	public void writeSubArrayDouble(double [] destination, int offset, int size) throws IbisIOException {
		//
	}
	

}
