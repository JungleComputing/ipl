package ibis.ipl.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIOException;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Enumeration;
import java.util.Hashtable;


/**
 * Provides an implementation of the {@link ReceivePort} and {@link
 * ReadMessage} interfaces of the IPL.
 */
public final class NetReceivePort implements ReceivePort, ReadMessage {
	
	/**
	 * Flag indicating whether the port use a polling thread.
	 */
	private boolean                  usePollingThread    = false;

	/**
	 * Flag indicating whether unsuccessful active polling should be followed by
	 * a yield.
	 *
	 * Note: this flag only affects asynchronous multithreaded
	 * polling or synchronous {@link #receive} operations. In
	 * particular, the synchronous {@link #poll} operation is not
	 * affected.
	 */
	private boolean                  useYield            =  true;

	/**
	 * The name of the port.
	 */
	private String                   name          	     =  null;

	/**
	 * The type of the port.
	 */
	private NetPortType              type          	     =  null;

	/**
	 * Flag indicating whether successful polling operation should
	 * generate an upcall or not.
	 */
	private boolean                  upcallsEnabled      = false;

	/**
	 * The upcall callback function.
	 */
	private Upcall                   upcall        	     =  null;

	/**
	 * The port identifier.
	 */
	private NetReceivePortIdentifier identifier    	     =  null;

	/**
	 * The port's topmost driver.
	 */
	private NetDriver                driver        	     =  null;

	/**
	 * The port's topmost input.
	 */
	private NetInput                 input        	     =  null;

	/**
	 * The incoming connection management thread.
	 */
	private AcceptThread             acceptThread  	     =  null;

	/**
	 * The optionnal asynchronous polling thread.
	 */
	private PollingThread            pollingThread       =  null;

	/**
	 * The polling autorisation lock.
	 */
	private NetMutex                 pollingLock         =  null;

	/**
	 * The message extraction autorisation lock.
	 */
	private NetMutex                 polledLock          =  null;

	/**
	 * The incoming connection acceptation lock.
	 */
	private NetMutex                 connectionLock      =  null;

	/**
	 * The network input synchronization lock.
	 */
	private NetMutex                 inputLock           =  null;

	/**
	 * The TCP server socket.
	 */
	private ServerSocket 	         serverSocket        =  null;

	/**
	 * The current active peer port.
	 */
	private Integer                  activeSendPortNum   =  null;

	/**
	 * The next send port integer number.
	 *
	 * @see NetSendPort#nextReceivePortNum
	 */
	private Integer      	         nextSendPortNum     =  new Integer(0);

	/**
	 * The table of remote port identifiers indexed by their number.
	 */
	private Hashtable    	         sendPortIdentifiers =  null;

	/**
	 * The table of remote port service sockets indexed by their number.
	 */
	private Hashtable    	         sendPortSockets     =  null;

	/**
	 * The table of remote port service TCP inputs indexed by their number.
	 */
	private Hashtable                sendPortIs          =  null;

	/**
	 * The table of remote port service TCP outputs indexed by their number.
	 */
	private Hashtable                sendPortOs          =  null;

	/**
	 * Flag indicating whether incoming connections are currently enabled.
	 */
	private boolean                  connectionEnabled   =  false;

	/**
	 * The empty message detection flag.
	 *
	 * The flag is set on each new {@link #_receive} call and should
	 * be cleared as soon as at least a byte as been added to the living message.
	 */
	private boolean               	 emptyMsg     	     = 	true;

	/* --- incoming connection manager thread -- */

	/**
	 * The incoming connection management thread class.
	 */
	private final class AcceptThread extends Thread {

		/**
		 * Flag indicating whether thread termination was requested.
		 */
		private volatile boolean stop = false;

		/**
		 * The incoming connection management function.
		 * Note: the thread is <strong>uninterruptible</strong>
		 * during the network input locking.
		 */
		public void run() {
                        
		accept_loop:
			while (!stop) {
				Socket                s   = null;
				ObjectInputStream     is  = null;
				ObjectOutputStream    os  = null;
				Integer               spn = null;
				NetSendPortIdentifier spi = null;

				try {
					s  		= serverSocket.accept();
					is 		= new ObjectInputStream(s.getInputStream());
					os 		= new ObjectOutputStream(s.getOutputStream());
					spn             = nextSendPortNum;
					nextSendPortNum = new Integer(spn.intValue()+1);
					spi             = (NetSendPortIdentifier)is.readObject();
				} catch (SocketException e) {
					/*
					 * "System call interrupted" is a SocketException.
					 * We need to catch it in order to handle the termination of
					 * the AcceptThread correctly
					 */
					continue accept_loop;
                                } catch (Exception e) {
					// TODO: pass the exception to
					//       the application
                                        e.printStackTrace();
					__.fwdAbort__(e);
				}

				// TODO: implement
				// connection_accepted/connection_refused
				// support
			connect_loop:
				while (!stop) {
					try {
						connectionLock.ilock();
						inputLock.lock();
						sendPortIdentifiers.put(spn, spi);
						sendPortSockets.put(spn, s);
						sendPortIs.put(spn, is);
						sendPortOs.put(spn, os);
						input.setupConnection(spn, is, os);	

						/*
						 * if (connectionUpcall) {
						 *	// not implemented
						 * }
						 */
						if (spn.intValue() == 0) {
							/*
							 * We got our first connection,
							 * let's start polling it
							 */
							if (pollingThread != null) {
								pollingThread.start();
							}
						}
						inputLock.unlock();
						connectionLock.unlock();
					} catch (InterruptedException e) {
                                                System.err.println("Accept thread interrupted");
                                                
						continue connect_loop;
					} catch (Exception e) {
						System.err.println(e.getMessage());
						e.printStackTrace();
					} 

					break connect_loop;
				}
			}
		}

		/**
		 * Requests for the thread completion.
		 */
		protected void terminate() {
			stop = true;
                        try {
                                serverSocket.close();
                                this.interrupt();
				this.join();
			} catch (Exception e) {
				__.fwdAbort__(e);
			}
		}
	}

	/**
	 * The optional asynchronous polling thread class.
	 */
	private final class PollingThread extends Thread {
		private volatile boolean stop = false;

		/**
		 * The asynchronous polling function.
		 * Note: the thread is <strong>uninterruptible</strong>
		 * during the network input locking.
		 */
		public void run() {
		polling_loop:
			while (!stop) {
				try {
					pollingLock.ilock();
					inputLock.lock();
					activeSendPortNum = input.poll();
					inputLock.unlock();

					if (activeSendPortNum == null) {
						pollingLock.unlock();
						if (useYield)
							yield();
						continue polling_loop;
					}
                                        //System.err.println("NetReceivePort: polling success");
					if (upcallsEnabled && upcall != null) {
                                                //System.err.println("NetReceivePort: upcall-->");
						upcall.upcall(_receive());
                                                //System.err.println("NetReceivePort: upcall<--");
					} else {
						polledLock.unlock();
					}
				} catch (InterruptedException e) {
					pollingLock.unlock();
					continue polling_loop;
				} catch (IbisIOException e) {
					// TODO: pass the exception back
					//       to the application
					__.fwdAbort__(e);
				}
			}
		}

		/**
		 * Requests for the thread completion.
		 */
		protected void terminate() {
			stop = true;
			this.interrupt();
			try {
				this.join();
			} catch (Exception e) {
				__.fwdAbort__(e);
			}
		}

	}
	
	/* --- NetReceivePort part --- */
	/*
	 * Constructor.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 * @param name the name of the port.
	 */
	public NetReceivePort(NetPortType type,
			      String      name)
		throws IbisIOException {
		this(type, name, null);
	}

	/*
	 * Constructor.
	 *
	 * @param type the {@linkplain NetPortType port type}.
	 * @param name the name of the port.
	 * @param upcall the reception upcall callback.
	 */
	public NetReceivePort(NetPortType type,
			      String      name,
			      Upcall      upcall)
		throws IbisIOException {
		this.type   	       = type;
		this.name   	       = name;
		this.upcall 	       = upcall;
		upcallsEnabled         = false;
		sendPortIdentifiers    = new Hashtable();
		sendPortSockets        = new Hashtable();
		sendPortIs             = new Hashtable();
		sendPortOs             = new Hashtable();
		polledLock     	       = new NetMutex(true);
		pollingLock    	       = new NetMutex(false);
		connectionLock 	       = new NetMutex(true);
		inputLock      	       = new NetMutex(false);
		driver                 = type.getDriver();
		input                  = driver.newInput(type.properties(), null);
		usePollingThread       = type.getBooleanProperty("UsePollingThread", usePollingThread);
		useYield               = type.getBooleanProperty("UseYield", useYield);

		try {
			serverSocket = new ServerSocket(0, 1, InetAddress.getLocalHost());
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
			
		Hashtable info = new Hashtable();
		
		info.put("accept_address", serverSocket.getInetAddress());
		info.put("accept_port",    new Integer(serverSocket.getLocalPort()));

		NetIbis           ibis   = type.getIbis();
		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();
		identifier    = new NetReceivePortIdentifier(name, type.name(), ibisId, info);
		
		acceptThread  = new AcceptThread();
		if (usePollingThread) {
			pollingThread = new PollingThread();
		}
		
		acceptThread.start();
	}

	/**
	 * The internal synchronous polling function.
	 *
	 * The calling thread is <strong>uninterruptible</strong> during
	 * the network input locking operation. The function may block
	 * if the {@linkplain #inputLock network input lock} is not available.
	 */
	private boolean _doPoll() throws IbisIOException {
		inputLock.lock();
		activeSendPortNum = input.poll();
		inputLock.unlock();

		if (activeSendPortNum == null) {
			return false;
		}
				
		if (upcallsEnabled && upcall != null) {
			upcall.upcall(_receive());
			return false;
		}

		return true;
	}

	/**
	 * Internally initializes a new reception.
	 */
	private ReadMessage _receive() throws IbisIOException {
		emptyMsg   = true;
		return this;
	}

	/**
	 * Blockingly attempts to receive a message.
	 *
	 * Note: if upcalls are currently enabled, this function is bypassed
	 * by the upcall callback unless no callback has been specified.
	 *
	 * @return A {@link ReadMessage} instance.
	 */
	public ReadMessage receive() throws IbisIOException {
		if (usePollingThread) {
			polledLock.lock();
		} else {
			if (useYield) {
				while (!_doPoll())
					Thread.currentThread().yield();
			} else {
				while (!_doPoll());
			}
			
		}
		
		return _receive();
	}

	/**
	 * {@inheritDoc}
	 */
	public ReadMessage receive(ReadMessage finishMe)
		throws IbisIOException {
		if (finishMe != null) finishMe.finish();
		return receive();
	}
	
	/**
	 * Unblockingly attempts to receive a message.
	 *
	 * Note: if upcalls are currently enabled, this function is bypassed
	 * by the upcall callback unless no callback has been specified.
	 *
	 * @return A {@link ReadMessage} instance or <code>null</code> if polling
	 * was unsuccessful.
	 */
	public ReadMessage poll()  throws IbisIOException {
		if (usePollingThread) {
			if (!polledLock.trylock())
				return null;
		} else {
			if (!_doPoll()) {
				return null;
			}
		}
		
		return _receive();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public ReadMessage poll(ReadMessage finishMe)
		throws IbisIOException {
		if (finishMe != null) finishMe.finish();
		return poll();
	}
	
	public DynamicProperties properties() {
		return null;
	}	

	/**
	 * {@inheritDoc}
	 */
	public ReceivePortIdentifier identifier() {
		return identifier;
	}

	/**
	 * Returns the identifier of the current active port peer or <code>null</code>
	 * if no peer port is active.
	 *
	 * @return The identifier of the port.
	 */
	protected NetSendPortIdentifier getActiveSendPortIdentifier() {
		if (activeSendPortNum == null)
			return null;
		
		return (NetSendPortIdentifier)sendPortIdentifiers.get(activeSendPortNum);
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void enableConnections() {
		if (!connectionEnabled) {
			connectionEnabled = true;
			connectionLock.unlock();
		}
		
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void disableConnections() {
		if (connectionEnabled) {
			connectionEnabled = false;
			connectionLock.lock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void enableUpcalls() {
                //System.err.println("NetReceivePort: enabling upcalls");
		upcallsEnabled = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void disableUpcalls() {
                //System.err.println("NetReceivePort: disabling upcalls");
		upcallsEnabled = false;
	}

	/**
	 * Closes the port. 
	 *
	 * This function might block until no messages are alive.
	 *
	 * Note: surprinsingly, ReceivePort.free is not declared to
	 * throw IbisIOException while SendPort.free does.
	 *
	 * Note: the function prints a warning if it detects that an
	 * incoming message was ignored.
	 */
	public synchronized void free() {
		try {
                        int i = 0;

			disableConnections();

			if (usePollingThread) {
				if (polledLock != null
				    &&
				    pollingLock != null)
					{
						boolean polled  = false;
						boolean polling = false;
						
						do {
							polled  = polledLock.trylock();
							polling = pollingLock.trylock();
						} while (!(polled|polling));

						if (polled) {
							__.warning__("incoming message ignored");
						}
					}
				else if (polledLock != null
					 ||
					 pollingLock != null) {
					__.abort__("invalid state");
				}
			}

			if (inputLock != null) {
				inputLock.lock();
			}

			if (acceptThread != null) {
				acceptThread.terminate();
			}

			if (pollingThread != null) {
				pollingThread.terminate();
			}

			if (input != null) {
				input.free();
			}

			if (serverSocket != null) {
				serverSocket.close();
			}
			
			if (sendPortOs != null) {
				Enumeration e = sendPortOs.keys();

				while (e.hasMoreElements()) {
					Object             key   = e.nextElement();
					Object             value = sendPortOs.remove(key);
					ObjectOutputStream os    = (ObjectOutputStream)value;
					os.close();
				}	
			}
		
			if (sendPortIs != null) {
				Enumeration e = sendPortIs.keys();

				while (e.hasMoreElements()) {
					Object            key   = e.nextElement();
					Object            value = sendPortIs.remove(key);
					ObjectInputStream is    = (ObjectInputStream)value;
					is.close();
				}	
			}

			if (sendPortSockets != null) {
				Enumeration e = sendPortSockets.keys();

				while (e.hasMoreElements()) {
					Object key   = e.nextElement();
					Object value = sendPortSockets.remove(key);
					Socket s     = (Socket)value;

                                        s.close();
				}	
			}

			name          	    =  null;
			type          	    =  null;
			upcallsEnabled      = false;
			upcall        	    =  null;
			identifier    	    =  null;
			driver        	    =  null;
			input        	    =  null;
			acceptThread  	    =  null;
			pollingThread       =  null;
			pollingLock         =  null;
			polledLock          =  null;
			connectionLock      =  null;
			inputLock           =  null;
			serverSocket        =  null;
			activeSendPortNum   =  null;
			nextSendPortNum     =  null;
			sendPortIdentifiers =  null;
			sendPortSockets     =  null;
			sendPortIs          =  null;
			sendPortOs          =  null;
		} catch (Exception e) {
			__.fwdAbort__(e);
		}
	}
	
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}

	/* --- ReadMessage Part --- */
	public void finish() throws IbisIOException {
                //System.err.println("NetReceivePort: finish-->");
		if (emptyMsg) {
			readByte();
		}
		activeSendPortNum = null;
		input.finish();
		pollingLock.unlock();
                //System.err.println("NetReceivePort: finish<--");
	}

	public long sequenceNumber() {
		return 0;
	}
	
	public SendPortIdentifier origin() {
		if (activeSendPortNum == null)
			return null;
		
		return (NetSendPortIdentifier)sendPortIdentifiers.get(activeSendPortNum);
	}

	public boolean readBoolean() throws IbisIOException {
		emptyMsg = false;
		return input.readBoolean();
	}
	
	public byte readByte() throws IbisIOException {
		emptyMsg = false;
                return input.readByte();
	}
	
	public char readChar() throws IbisIOException {
		emptyMsg = false;
		return input.readChar();
	}
	
	public short readShort() throws IbisIOException {
		emptyMsg = false;
		return input.readShort();
	}
	
	public int readInt() throws IbisIOException {
		emptyMsg = false;
		return input.readInt();
	}
	
	public long readLong() throws IbisIOException {
		emptyMsg = false;
		return input.readLong();
	}
	
	public float readFloat() throws IbisIOException {
		emptyMsg = false;
		return input.readFloat();
	}
	
	public double readDouble() throws IbisIOException {
		emptyMsg = false;
		return input.readDouble();
	}
	
	public String readString() throws IbisIOException {
		emptyMsg = false;
		return input.readString();
	}
	
	public Object readObject()
		throws IbisIOException {
		emptyMsg = false;
		return input.readObject();
	}
	

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayBoolean(userBuffer);
	}

	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

                emptyMsg = false;
                input.readArrayByte(userBuffer);
	}

	public void readArrayChar(char [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayChar(userBuffer);
	}

	public void readArrayShort(short [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayShort(userBuffer);
	}

	public void readArrayInt(int [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayInt(userBuffer);
	}

	public void readArrayLong(long [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                input.readArrayLong(userBuffer);
	}

	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                input.readArrayFloat(userBuffer);
	}

	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayDouble(userBuffer);
	}


	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayBoolean(userBuffer, offset, length);
	}

	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
                input.readSubArrayByte(userBuffer, offset, length);
	}

	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayChar(userBuffer, offset, length);
	}

	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayShort(userBuffer, offset, length);
	}

	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayInt(userBuffer, offset, length);
	}

	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayLong(userBuffer, offset, length);
	}

	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayFloat(userBuffer, offset, length);
	}

	public void readSubArrayDouble(double [] userBuffer, int offset, int length)
		throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		readSubArrayDouble(userBuffer, offset, length);
	}

} 
