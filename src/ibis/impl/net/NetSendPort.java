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
import ibis.io.Replacer;

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

	private Hashtable             receivePortNLS         = null;

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

                {
                        String mainDriverName = type.getStringProperty("/", "Driver");

                        if (mainDriverName == null) {
                                throw new IbisIOException("root driver not specified");
                        }
                        
                        driver   		 = ibis.getDriver(mainDriverName);
                        if (driver == null) {
                                throw new IbisIOException("driver not found");
                        }
                }
                
		output   		 = driver.newOutput(type, null, null);
		receivePortIdentifiers 	 = new Hashtable();
		receivePortSockets     	 = new Hashtable();
		receivePortIs          	 = new Hashtable();
		receivePortOs          	 = new Hashtable();
		receivePortNLS         	 = new Hashtable();
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
                //System.err.println("NetSendPort: newMessage-->");
		outputLock.lock();
		emptyMsg     = true;
                output.initSend();
                //System.err.println("NetSendPort: newMessage<--");
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
                NetServiceListener nls = new NetServiceListener(is);
		receivePortNLS.put(rpn, nls);
		output.setupConnection(rpn, is, os, nls);
                nls.start();
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

			if (receivePortNLS != null) {
				Enumeration e = receivePortNLS.keys();

				while (e.hasMoreElements()) {
					Object             key   = e.nextElement();
					Object             value = receivePortNLS.remove(key);
					NetServiceListener nls   = (NetServiceListener)value;

                                        nls.free();
				}	
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
                        receivePortNLS         = null;
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
                //System.err.println("NetSendPort["+identifier+"]: _finish-->");
		if (emptyMsg) {
			writeByte((byte)0);
		}
                //System.err.println("NetSendPort["+identifier+"]: _finish<--");
	}
	
	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws IbisIOException{
                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]: NetSendPort finish-->");
                //System.err.println("NetSendPort["+identifier+"]: finish-->");
                //System.err.println("NetSendPort: finish-->");
		_finish();
		output.finish();
		outputLock.unlock();
                //System.err.println("NetSendPort: finish<--");
                //System.err.println("NetSendPort["+identifier+"]: finish<--");
                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]: NetSendPort finish<--");
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

	public void writeBoolean(boolean v) throws IbisIOException {
		emptyMsg = false;
		output.writeBoolean(v);
	}

	public void writeByte(byte v) throws IbisIOException {
		emptyMsg = false;
		output.writeByte(v);
	}

	public void writeChar(char v) throws IbisIOException {
		emptyMsg = false;
		output.writeChar(v);
	}

	public void writeShort(short v) throws IbisIOException {
		emptyMsg = false;
		output.writeShort(v);
	}

	public void writeInt(int v) throws IbisIOException {
		emptyMsg = false;
		output.writeInt(v);
	}

	public void writeLong(long v) throws IbisIOException {
		emptyMsg = false;
		output.writeLong(v);
	}
	
	public void writeFloat(float v) throws IbisIOException {
		emptyMsg = false;
		output.writeFloat(v);
	}

	public void writeDouble(double v) throws IbisIOException {
		emptyMsg = false;
		output.writeDouble(v);
	}

	public void writeString(String v) throws IbisIOException {
		emptyMsg = false;
		output.writeString(v);
	}

	public void writeObject(Object v) throws IbisIOException {
		emptyMsg = false;
		output.writeObject(v);
	}

	public void writeArrayBoolean(boolean [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
	}

	public void writeArrayByte(byte [] b) throws IbisIOException {
		writeArraySliceByte(b, 0, b.length);
	}
	
	public void writeArrayChar(char [] b) throws IbisIOException {
                writeArraySliceChar(b, 0, b.length);
	}
	
	public void writeArrayShort(short [] b) throws IbisIOException {
                writeArraySliceShort(b, 0, b.length);
	}
	
	public void writeArrayInt(int [] b) throws IbisIOException {
                writeArraySliceInt(b, 0, b.length);
	}
	
	public void writeArrayLong(long [] b) throws IbisIOException {
                writeArraySliceLong(b, 0, b.length);
	}
	
	public void writeArrayFloat(float [] b) throws IbisIOException {
                writeArraySliceFloat(b, 0, b.length);
	}
	
	public void writeArrayDouble(double [] b) throws IbisIOException {
                writeArraySliceDouble(b, 0, b.length);
	}

	public void writeArrayObject(Object [] b) throws IbisIOException {
                writeArraySliceObject(b, 0, b.length);
	}

	public void writeArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceBoolean(b, o, l);
	}
	
	public void writeArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceByte(b, o, l);
	}
	
	public void writeArraySliceChar(char [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceChar(b, o, l);
	}
	
	public void writeArraySliceShort(short [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceShort(b, o, l);
	}
	
	public void writeArraySliceInt(int [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceInt(b, o, l);
	}
	
	public void writeArraySliceLong(long [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceLong(b, o, l);
	}
	
	public void writeArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
                if (l == 0)
			return;
                
                emptyMsg = false;
                output.writeArraySliceFloat(b, o, l);
	}
	
	public void writeArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceDouble(b, o, l);
	}
	public void writeArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceObject(b, o, l);
	}
}
