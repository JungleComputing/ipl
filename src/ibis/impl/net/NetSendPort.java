package ibis.ipl.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;
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

import java.util.Iterator;
import java.util.Hashtable;

/**
 * Provides an implementation of the {@link SendPort} and {@link
 * WriteMessage} interfaces of the IPL.
 */
public final class NetSendPort implements SendPort, WriteMessage, NetPort {





        /* ___ INTERNAL CLASSES ____________________________________________ */

        /*
        protected final class ReceivePortState {
                Integer                  num  = null;
                NetReceivePortIdentifier id   = null;
                NetServiceLink           link = null;
        }
        */



        /* ___ LESS-IMPORTANT OBJECTS ______________________________________ */

        private NetIbis               ibis                   = null;

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
         * Replacer ???.
         */
        private Replacer              replacer               = null;
        
	private int       	      nextReceivePortNum     = 0;





        /* ___ IMPORTANT OBJECTS ___________________________________________ */

	private Hashtable 	      connectionTable        = null;        

	/**
	 * The topmost driver.
	 */
	private NetDriver             driver     	     = null;

	/**
	 * The topmost network output.
	 */
	private NetOutput             output     	     = null;





        /* ___ STATE _______________________________________________________ */

	/**
	 * The empty message detection flag.
	 *
	 * The flag is set on each new {@link #newMessage} call and should
	 * be cleared as soon as at least a byte as been added to the living message.
	 */
	private boolean       	      emptyMsg     	     = true;





        /* ___ EVENT QUEUE _________________________________________________ */

	private NetEventQueue         eventQueue             = null;
        private NetEventQueueListener eventQueueListener     = null;






        /* ___ LOCKS _______________________________________________________ */

	/**
	 * The network output synchronization lock.
	 */
	private NetMutex              outputLock   	     = null;






        /* ................................................................. */





        

        /* ___ NETPORT RELATED FUNCTIONS ___________________________________ */



        public NetPortType getPortType() {
                return type;
        }
        /* ................................................................. */





        

        /* ___ SENDPORT RELATED FUNCTIONS __________________________________ */



        /* ----- CONSTRUCTORS ______________________________________________ */

	/**
	 * Constructor for a anonymous send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 */
	public NetSendPort(NetPortType type) throws NetIbisException {
		this(type, null, null);
	}

	/**
	 * Constructor for a anonymous send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
         * @param replacer the replacer for this object.
	 */
	public NetSendPort(NetPortType type, Replacer replacer) throws NetIbisException {
		this(type, replacer, null);
	}

	/**
	 * Constructor for a named send port.
	 *
	 * @param type the {@linkplain NetPortType port type}.
         * @param replacer the replacer for this object.
	 * @param name the name of the port.
	 */
	public NetSendPort(NetPortType type, Replacer replacer, String name)
		throws NetIbisException {
		this.name     = name;
		this.type     = type;
                this.replacer = replacer;
                this.ibis     = type.getIbis();

                initPassiveState();
                initActiveState();
	}





        /* ----- CLEAN-UP __________________________________________________ */

	/**
	 * Ensures correct clean-up.
	 */
	protected void finalize() throws Throwable {
                // System.err.println("SendPort: finalize-->");
		free();

                if (eventQueueListener != null) {
                        eventQueueListener.end();
                
                        //System.err.println("waiting for SendPort eventQueue thread to join");
                        while (true) {
                                try {
                                        eventQueueListener.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("SendPort eventQueue thread joined");
                }

		super.finalize();
                // System.err.println("SendPort: finalize<--");
	}


        /* ----- PASSIVE STATE INITIALIZATION ______________________________ */

        private void initIdentifier() throws NetIbisException {
                if (this.identifier != null)
                        throw new NetIbisException("identifier already initialized");

		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();

		this.identifier = new NetSendPortIdentifier(name, type.name(), ibisId);
        }

        private void initPassiveState() throws NetIbisException {
                initIdentifier();
        }
        
        



        /* ----- ACTIVE STATE INITIALIZATION _______________________________ */

        private void initEventQueue() {
                eventQueue         = new NetEventQueue();
                eventQueueListener = new NetEventQueueListener(this, "SendPort: " + ((name != null)?name:"anonymous"), eventQueue);
                eventQueueListener.start();
        }
        

        private void loadMainDriver() throws NetIbisException {
                if (this.driver != null)
                        throw new NetIbisException("driver already loaded");

                String mainDriverName = type.getStringProperty("/", "Driver");
                
                if (mainDriverName == null) {
                        throw new NetIbisException("root driver not specified");
                }
                
                NetDriver driver = ibis.getDriver(mainDriverName);

                if (driver == null) {
                        throw new NetIbisException("driver not found");
                }

                this.driver = driver;
        }

        private void initCommunicationEngine() throws NetIbisException {
		this.connectionTable = new Hashtable();
                loadMainDriver();
		this.outputLock = new NetMutex(false);
		this.output     = driver.newOutput(type, null, null);
        }        

        private void initActiveState() throws NetIbisException {
                initEventQueue();
                initCommunicationEngine();
        }





        /* ----- INTERNAL MANAGEMENT FUNCTIONS _____________________________ */

        private NetConnection establishServiceConnection(NetReceivePortIdentifier nrpi) throws NetIbisException {
                Hashtable      info = nrpi.connectionInfo();
                NetServiceLink link = new NetServiceLink(eventQueue, info);

		try {
                        ObjectOutputStream os = new ObjectOutputStream(link.getOutputSubStream("__port__"));
                        os.writeObject(identifier);
                        os.flush();
                        os.close();
		} catch (IOException e) {
			throw new NetIbisException(e.getMessage());
		}

                Integer num = null;
                
                synchronized(this) {
                        num = new Integer(nextReceivePortNum++);
                }
                
                NetConnection cnx = new NetConnection(this, num, identifier, nrpi, link);

                return cnx;
        }
        
        private void establishApplicationConnection(NetConnection cnx) throws NetIbisException {
		output.setupConnection(cnx);
        }
        
        private void close(NetConnection cnx) throws NetIbisException {                
                if (cnx == null)
                        return;

                // System.err.println("NetSendPort: close-->");
                try {
                        output.close(cnx.getNum());
                } catch (Exception e) {
                        throw new Error(e.getMessage());
                }

                cnx.close();
                // System.err.println("NetSendPort: close<--");
        }        


        


        /* ----- PUBLIC SendPort API _______________________________________ */

	/**
	 * Starts the construction of a new message.
	 *
	 * @return The message instance.
	 */	
	public WriteMessage newMessage() throws NetIbisException {
                // System.err.println("NetSendPort: newMessage-->");
		outputLock.lock();
		emptyMsg = true;
                output.initSend();
                // System.err.println("NetSendPort: newMessage<--");
		return this;
	}

	/**
	 * Unimplemented.
	 *
	 * @return null.
	 */	
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
	public synchronized void connect(ReceivePortIdentifier rpi) throws NetIbisException {
                // System.err.println("sendport: connect-->");
		outputLock.lock();
		NetReceivePortIdentifier nrpi = (NetReceivePortIdentifier)rpi;
                // System.err.println("sendport: connect - service connection");
                NetConnection cnx = establishServiceConnection(nrpi);
                // System.err.println("sendport: connect - service connection ok");

                synchronized(connectionTable) {
                        connectionTable.put(cnx.getNum(), cnx);
                }
                
                // System.err.println("sendport: connect - application connection");
                establishApplicationConnection(cnx);
                // System.err.println("sendport: connect - application connection ok");
		outputLock.unlock();
                // System.err.println("sendport: connect<--");
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
		throws NetIbisException {
		__.unimplemented__("connect");
	}

	/**
	 * Closes the port.
	 *
	 * Note: this function might block until the living message is finalized.
	 */
	public void free()
		throws NetIbisException {
                // System.err.println("NetSendPort: free-->");
                synchronized(this) {
                        try {
                                if (outputLock != null) {
                                        outputLock.lock();
                                }
		
                                if (connectionTable != null) {
                                        while (true) {
                                                NetConnection cnx = null;
                                        
                                                synchronized(connectionTable) {
                                                        Iterator i = connectionTable.values().iterator();
                                                        if (!i.hasNext())
                                                                break;

                                                        cnx = (NetConnection)i.next();
                                                        i.remove();
                                                }
                                 
                                                if (cnx != null) {
                                                        close(cnx);
                                                }
                                        }
                                }
		
                                if (output != null) {
                                        output.free();
                                }

                                if (outputLock != null) {
                                        outputLock.unlock();
                                }
		
                        } catch (Exception e) {
                                __.fwdAbort__(e);
                        }
                }
                // System.err.println("NetSendPort: free<--");
	}
	




        /* ----- PUBLIC WriteMessage API ___________________________________ */

	/**
	 * Sends what remains to be sent.
	 */
	public void send() throws NetIbisException{
		output.send();
	}

	/**
	 * Completes the message transmission.
	 *
	 * Note: if it is detected that the message is actually empty,
	 * a single byte is forced to be sent over the network.
	 */
	private void _finish() throws  NetIbisException{
		if (emptyMsg) {
			writeByte((byte)0);
		}
	}
	
	/**
	 * Completes the message transmission and releases the send port.
	 */
	public void finish() throws NetIbisException{
                // System.err.println("NetSendPort: finish-->");
		_finish();
		output.finish();
		outputLock.unlock();
                // System.err.println("NetSendPort: finish<--");
	}

	/**
	 * Unconditionnaly completes the message transmission and
	 * releases the send port.
	 *
	 * @param doSend {@inheritDoc}
	 */
	public void reset(boolean doSend) throws NetIbisException {
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

	public void writeBoolean(boolean v) throws NetIbisException {
		emptyMsg = false;
		output.writeBoolean(v);
	}

	public void writeByte(byte v) throws NetIbisException {
		emptyMsg = false;
		output.writeByte(v);
	}

	public void writeChar(char v) throws NetIbisException {
		emptyMsg = false;
		output.writeChar(v);
	}

	public void writeShort(short v) throws NetIbisException {
		emptyMsg = false;
		output.writeShort(v);
	}

	public void writeInt(int v) throws NetIbisException {
		emptyMsg = false;
		output.writeInt(v);
	}

	public void writeLong(long v) throws NetIbisException {
		emptyMsg = false;
		output.writeLong(v);
	}
	
	public void writeFloat(float v) throws NetIbisException {
		emptyMsg = false;
		output.writeFloat(v);
	}

	public void writeDouble(double v) throws NetIbisException {
		emptyMsg = false;
		output.writeDouble(v);
	}

	public void writeString(String v) throws NetIbisException {
		emptyMsg = false;
		output.writeString(v);
	}

	public void writeObject(Object v) throws NetIbisException {
		emptyMsg = false;
		output.writeObject(v);
	}

	public void writeArrayBoolean(boolean [] userBuffer) throws NetIbisException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
	}

	public void writeArrayByte(byte [] b) throws NetIbisException {
		writeArraySliceByte(b, 0, b.length);
	}
	
	public void writeArrayChar(char [] b) throws NetIbisException {
                writeArraySliceChar(b, 0, b.length);
	}
	
	public void writeArrayShort(short [] b) throws NetIbisException {
                writeArraySliceShort(b, 0, b.length);
	}
	
	public void writeArrayInt(int [] b) throws NetIbisException {
                writeArraySliceInt(b, 0, b.length);
	}
	
	public void writeArrayLong(long [] b) throws NetIbisException {
                writeArraySliceLong(b, 0, b.length);
	}
	
	public void writeArrayFloat(float [] b) throws NetIbisException {
                writeArraySliceFloat(b, 0, b.length);
	}
	
	public void writeArrayDouble(double [] b) throws NetIbisException {
                writeArraySliceDouble(b, 0, b.length);
	}

	public void writeArrayObject(Object [] b) throws NetIbisException {
                writeArraySliceObject(b, 0, b.length);
	}

	public void writeArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceBoolean(b, o, l);
	}
	
	public void writeArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceByte(b, o, l);
	}
	
	public void writeArraySliceChar(char [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceChar(b, o, l);
	}
	
	public void writeArraySliceShort(short [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceShort(b, o, l);
	}
	
	public void writeArraySliceInt(int [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceInt(b, o, l);
	}
	
	public void writeArraySliceLong(long [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceLong(b, o, l);
	}
	
	public void writeArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
                if (l == 0)
			return;
                
                emptyMsg = false;
                output.writeArraySliceFloat(b, o, l);
	}
	
	public void writeArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceDouble(b, o, l);
	}

	public void writeArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                output.writeArraySliceObject(b, o, l);
	}
}
