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
	 * Current reception buffer.
	 */
	private NetReceiveBuffer      	 buffer       	     = 	null;

	/**
	 * The current buffer offset of the payload area.
	 */
	private int                   	 dataOffset   	     = 	   0;

	/**
	 * The current buffer offset for extracting user data.
	 */
	private int                   	 bufferOffset 	     = 	   0;

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
		private boolean stop = false;

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
				}
				catch (Exception e) {
					// TODO: pass the exception to
					//       the application
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
			this.interrupt();
			try {
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
		private boolean stop = false;

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
				
					if (upcallsEnabled && upcall != null) {
						upcall.upcall(_receive());
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
		dataOffset = input.getHeadersLength();
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
		upcallsEnabled = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void disableUpcalls() {
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
					s.shutdownOutput();
					s.shutdownInput();
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
        void receiveBuffer(NetReceiveBuffer buffer) throws IbisIOException {
                input.receiveBuffer(buffer);
                buffer.free();
        }

	void receiveBuffer(int length) throws IbisIOException {
		buffer       = input.receiveBuffer(length);
		bufferOffset = dataOffset;
	}

	void freeBuffer() {
		if (buffer != null) {
			buffer.free();
			buffer = null;
		}
		
		bufferOffset =    0;
	}

	public void finish() throws IbisIOException {
		if (emptyMsg) {
			readByte();
		}
		freeBuffer();
		activeSendPortNum = null;
		input.release();
		pollingLock.unlock();
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
		return false;
	}
	
	public byte readByte() throws IbisIOException {
		emptyMsg = false;
		byte value = 0;

		if (buffer == null) {
			receiveBuffer(1);
		}

		value = buffer.data[bufferOffset++];

		if ((buffer.length - bufferOffset) == 0) {
			freeBuffer();
		}
		
		return value;
	}
	
	public char readChar() throws IbisIOException {
		return 0;
	}
	
	public short readShort() throws IbisIOException {
		return 0;
	}
	
	public int readInt() throws IbisIOException {
		return 0;
	}
	
	public long readLong() throws IbisIOException {
		return 0;
	}
	
	public float readFloat() throws IbisIOException {
		return 0;
	}
	
	public double readDouble() throws IbisIOException {
		return 0;
	}
	
	public String readString() throws IbisIOException {
		return "";
	}
	
	public Object readObject()
		throws IbisIOException {
		return null;
	}
	

	public void readArrayBoolean(boolean [] destination)
		throws IbisIOException {
		//
	}

	public void readArrayByte(byte [] userBuffer)
		throws IbisIOException {
		readSubArrayByte(userBuffer, 0, userBuffer.length);
	}

	public void readArrayChar(char [] destination) throws IbisIOException {
		//
	}

	public void readArrayShort(short [] destination)
		throws IbisIOException {
		//
	}

	public void readArrayInt(int [] destination) throws IbisIOException {
		//
	}

	public void readArrayLong(long [] destination) throws IbisIOException {
		//
	}

	public void readArrayFloat(float [] destination)
		throws IbisIOException {
		//
	}

	public void readArrayDouble(double [] destination)
		throws IbisIOException {
		//
	}


	public void readSubArrayBoolean(boolean [] destination,
					int        offset,
					int        size)
		throws IbisIOException {
		//
	}

	public void readSubArrayByte(byte [] userBuffer,
				     int     offset,
				     int     length)
		throws IbisIOException {
		// System.err.println("read: "+offset+", "+length);
		if (length == 0)
			return;

		emptyMsg = false;

                if (dataOffset == 0) {
                        if (buffer != null) {
                                freeBuffer();
                        }

                        receiveBuffer(new NetReceiveBuffer(userBuffer, offset, length));
                } else {
                        if (buffer != null) {
                                int bufferLength = buffer.length - bufferOffset;
                                int copyLength   = Math.min(bufferLength, length);

                                System.arraycopy(buffer.data, bufferOffset, userBuffer, offset, copyLength);

                                bufferOffset += copyLength;
                                bufferLength -= copyLength;
                                offset       += copyLength;
                                length       -= copyLength;

                                if (bufferLength == 0) {
                                        freeBuffer();
                                }
                        }

                        while (length > 0) {
                                receiveBuffer(length);

                                int bufferLength = buffer.length - bufferOffset;
                                int copyLength   = Math.min(bufferLength, length);

                                System.arraycopy(buffer.data, bufferOffset, userBuffer, offset, copyLength);

                                bufferOffset += copyLength;
                                bufferLength -= copyLength;
                                offset       += copyLength;
                                length       -= copyLength;

                                if (bufferLength == 0) {
                                        freeBuffer();
                                }
                        }
                }
                
		// System.err.println("read: "+offset+", "+length+": ok");
	}

	public void readSubArrayChar(char [] destination,
				     int     offset,
				     int     size)
		throws IbisIOException {
		//
	}

	public void readSubArrayShort(short [] destination,
				      int      offset,
				      int      size)
		throws IbisIOException {
		//
	}

	public void readSubArrayInt(int [] destination,
				    int    offset,
				    int    size)
		throws IbisIOException {
		//
	}

	public void readSubArrayLong(long [] destination,
				     int     offset,
				     int     size)
		throws IbisIOException {
		//
	}

	public void readSubArrayFloat(float [] destination,
				      int      offset,
				      int      size)
		throws IbisIOException {
		//
	}

	public void readSubArrayDouble(double [] destination,
				       int 	 offset,
				       int 	 size)
		throws IbisIOException {
		//
	}

} 
