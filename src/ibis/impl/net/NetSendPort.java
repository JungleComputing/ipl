package ibis.ipl.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.Replacer;
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
         * Replacer ???.
         */
        private Replacer              replacer               = null;
        
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
		this(type, null, null);
	}

	/**
	 * Constructor for a anonymous send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 */
	public NetSendPort(NetPortType type, Replacer replacer) throws IbisIOException {
		this(type, replacer, null);
	}

	/**
	 * Constructor for a named send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 * @param name the name of the port.
	 */
	public NetSendPort(NetPortType type, Replacer replacer, String name)
		throws IbisIOException {
                //System.err.println("NetSendPort: <init>-->");
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
                this.replacer = replacer;
                //System.err.println("NetSendPort: <init><--");
	}

	/**
	 * Starts the construction of a new message.
	 *
	 * @return The message instance.
	 */	
	public WriteMessage newMessage() throws IbisIOException {
                //System.err.println("NetSendPort["+identifier+"]: newMessage-->");
		outputLock.lock();
		emptyMsg     = true;
                output.initSend();
                //System.err.println("NetSendPort["+identifier+"]: newMessage<--");
		return this;
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
                //System.err.println("NetSendPort: connect-->");
		outputLock.lock();
		NetReceivePortIdentifier nrpi	       = (NetReceivePortIdentifier)rpi;
		Hashtable                info	       = nrpi.connectionInfo();
		InetAddress              remoteAddress = (InetAddress) info.get("accept_address");
		int                      remotePort    = ((Integer) info.get("accept_port")).intValue();
		Socket             	 s 	       = null;
		ObjectInputStream  	 is	       = null;
		ObjectOutputStream 	 os	       = null;

		try {
                        //System.err.println("attempting "+identifier+" to connect to '"+remoteAddress+"'["+remotePort+"]");
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
		outputLock.unlock();
                //System.err.println("NetSendPort: connect<--");
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
                //System.err.println("NetSendPort: free-->");
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
                                        
                                        s.close();
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
                //System.err.println("NetSendPort: free<--");
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

	// TODO: ensure that send is non-blocking
	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws IbisIOException{
		output.send();
	}

	// TODO: ensure that data has actually been sent
	/**
	 * Completes the message transmission.
	 *
	 * Note: if it is detected that the message is actually empty,
	 * a single byte is forced to be sent over the network.
	 */
	private void _finish() throws  IbisIOException{
                //System.err.println("NetSendPort["+identifier+"]: finish-->");
		if (emptyMsg) {
                        //System.err.println("NetSendPort["+identifier+"]: finish- empty message -");
			writeByte((byte)0);
		}
                //System.err.println("NetSendPort["+identifier+"]: finish<--");
	}
	
	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IbisIOException{
                //System.err.println("NetSendPort["+identifier+"]: finish-->");
		_finish();
		output.finish();
		outputLock.unlock();
                //System.err.println("NetSendPort["+identifier+"]: finish<--");
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws IbisIOException {
                //System.err.println("NetSendPort: reset-->");
		if (doSend) {
			send();
		} else {
                        throw new Error("full reset unimplemented");
                }
		_finish();
		output.reset(doSend);
                //System.err.println("NetSendPort: reset<--");
	}

	public int getCount() {
		return 0;
	}

	public void resetCount() {
		//
	}

	public void writeBoolean(boolean value) throws IbisIOException {
                //System.err.println("NetSendPort: write");
		emptyMsg = false;
		output.writeBoolean(value);
	}

	/**
	 * Appends a byte to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param value the byte to append to the message.
	 */
	public void writeByte(byte value) throws IbisIOException {
                //System.err.println("NetSendPort: writeByte-->");
		emptyMsg = false;
		output.writeByte(value);
	}

	public void writeChar(char value) throws IbisIOException {
                //System.err.println("NetSendPort: writeChar-->");
		emptyMsg = false;
		output.writeChar(value);
	}

	public void writeShort(short value) throws IbisIOException {
                //System.err.println("NetSendPort: writeShort-->");
		emptyMsg = false;
		output.writeShort(value);
	}

	public void writeInt(int value) throws IbisIOException {
                //System.err.println("NetSendPort: writeInt-->");
		emptyMsg = false;
		output.writeInt(value);
	}

	public void writeLong(long value) throws IbisIOException {
                //System.err.println("NetSendPort: writeLong-->");
		emptyMsg = false;
		output.writeLong(value);
	}
	
	public void writeFloat(float value) throws IbisIOException {
                //System.err.println("NetSendPort: writeFloat-->");
		emptyMsg = false;
		output.writeFloat(value);
	}

	public void writeDouble(double value) throws IbisIOException {
                //System.err.println("NetSendPort: writeDouble-->");
		emptyMsg = false;
		output.writeDouble(value);
	}

	public void writeString(String value) throws IbisIOException {
                //System.err.println("NetSendPort: writeString-->");
		emptyMsg = false;
		output.writeString(value);
	}

	public void writeObject(Object value) throws IbisIOException {
                //System.err.println("NetSendPort: writeObject-->");
		emptyMsg = false;
		output.writeObject(value);
	}

	public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayBoolean-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		output.writeArrayBoolean(userBuffer);
	}
	
	/**
	 * Appends a byte array to the current message.
	 *
	 * Note: this function might block.
	 *
	 * @param userBuffer the byte array to append to the message.
	 */
	public void writeArrayByte(byte [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayByte-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		output.writeArrayByte(userBuffer);
	}
	
	public void writeArrayChar(char [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayChar-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayChar(userBuffer);
	}
	
	public void writeArrayShort(short [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayShort-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayShort(userBuffer);
	}
	
	public void writeArrayInt(int [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayInt-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayInt(userBuffer);
	}
	
	public void writeArrayLong(long [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayLong-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayLong(userBuffer);
	}
	
	public void writeArrayFloat(float [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayFloat-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayFloat(userBuffer);
	}
	
	public void writeArrayDouble(double [] userBuffer) throws IbisIOException {
                //System.err.println("NetSendPort: writeArrayDouble-->");
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                output.writeArrayDouble(userBuffer);
	}
	

	public void writeSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayBoolean-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayBoolean(userBuffer, offset, length);
	}
	
	public void writeSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayByte-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayByte(userBuffer, offset, length);
	}
	
	public void writeSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayChar-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayChar(userBuffer, offset, length);
	}
	
	public void writeSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayShort-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayShort(userBuffer, offset, length);
	}
	
	public void writeSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayInt-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayInt(userBuffer, offset, length);
	}
	
	public void writeSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayLong-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayLong(userBuffer, offset, length);
	}
	
	public void writeSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayFloat-->");
                if (length == 0)
			return;
                
                emptyMsg = false;
                output.writeSubArrayFloat(userBuffer, offset, length);
	}
	
	public void writeSubArrayDouble(double [] userBuffer, int offset, int length) throws IbisIOException {
                //System.err.println("NetSendPort: writeSubArrayDouble-->");
		if (length == 0)
			return;

		emptyMsg = false;
                output.writeSubArrayDouble(userBuffer, offset, length);
	}
	

}
