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
	
        final private boolean            streamChecking      = false;

	/**
	 * Flag indicating whether the port use a polling thread.
	 */
	private boolean                  usePollingThread    = false;


	private boolean                  useUpcallThread     = true;

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

	private Hashtable                sendPortNLS         =  null;

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
                                                //System.err.println("NetReceivePort: <accept>-->");
						connectionLock.ilock();
						inputLock.lock();
						sendPortIdentifiers.put(spn, spi);
						sendPortSockets.put(spn, s);
						sendPortIs.put(spn, is);
						sendPortOs.put(spn, os);
                                                NetServiceListener nls = new NetServiceListener(is);
                                                sendPortNLS.put(spn, nls);

						input.setupConnection(spn, is, os, nls);	
                                                nls.start();
                                                
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
                                        //System.err.println("NetReceivePort: <accept><--");
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
                                                //System.err.println("NetReceivePort["+identifier+"] - "+Thread.currentThread()+": poll success");
                                                final ReadMessage rm = _receive();

                                                if (useUpcallThread) {
                                                        upcall.upcall(rm);
                                                } else {
                                                        Runnable r = new Runnable() {
                                                                        public void run() {
                                                                                upcall.upcall(rm);
                                                                        }
                                                                };
                                                        (new Thread(r)).start();
                                                }
                                                
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
                //System.err.println("NetReceivePort: <init>-->");
		this.type   	       = type;
		this.name   	       = name;
		this.upcall 	       = upcall;
		upcallsEnabled         = false;
		sendPortIdentifiers    = new Hashtable();
		sendPortSockets        = new Hashtable();
		sendPortIs             = new Hashtable();
		sendPortOs             = new Hashtable();
		sendPortNLS            = new Hashtable();
		polledLock     	       = new NetMutex(true);
		pollingLock    	       = new NetMutex(false);
		connectionLock 	       = new NetMutex(true);
		inputLock      	       = new NetMutex(false);

		NetIbis ibis           = type.getIbis();

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
                
		input                  = driver.newInput(type, null, null);
		usePollingThread       = (type.getBooleanStringProperty(null, "UsePollingThread", new Boolean(usePollingThread))).booleanValue();
		useYield               = (type.getBooleanStringProperty(null, "UseYield",         new Boolean(useYield))).booleanValue();

		try {
			serverSocket = new ServerSocket(0, 1, InetAddress.getLocalHost());
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
			
		Hashtable info = new Hashtable();
		
		info.put("accept_address", serverSocket.getInetAddress());
		info.put("accept_port",    new Integer(serverSocket.getLocalPort()));

		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();
		identifier    = new NetReceivePortIdentifier(name, type.name(), ibisId, info);
		
		acceptThread  = new AcceptThread();
		if (usePollingThread) {
			pollingThread = new PollingThread();
		}
		
		acceptThread.start();
                //System.err.println("NetReceivePort: <init><--");
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
                //System.err.println("NetReceivePort: free-->");
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

			if (sendPortNLS != null) {
				Enumeration e = sendPortNLS.keys();

				while (e.hasMoreElements()) {
					Object             key   = e.nextElement();
					Object             value = sendPortNLS.remove(key);
					NetServiceListener nls   = (NetServiceListener)value;

                                        nls.free();
				}	
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
                        sendPortNLS         =  null;
			sendPortSockets     =  null;
			sendPortIs          =  null;
			sendPortOs          =  null;
		} catch (Exception e) {
			__.fwdAbort__(e);
		}
                //System.err.println("NetReceivePort: free<--");
	}
	
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}

	/* --- ReadMessage Part --- */
	public void finish() throws IbisIOException {
                //System.err.println("NetReceivePort["+identifier+"]: finish-->");
		if (emptyMsg) {
			readByte();
		}
		activeSendPortNum = null;
		input.finish();
		pollingLock.unlock();
                //System.err.println("NetReceivePort["+identifier+"]: finish<--");
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
		boolean v = input.readBoolean();
                if (streamChecking) {
                        debugReadBoolean(v);
                }
                return v;
	}
	
	public byte readByte() throws IbisIOException {
		emptyMsg = false;
		byte v = input.readByte();
                if (streamChecking) {
                        debugReadByte(v);
                }
                return v;
	}
	
	public char readChar() throws IbisIOException {
		emptyMsg = false;
		char v = input.readChar();
                if (streamChecking) {
                        debugReadChar(v);
                }
                return v;
	}
	
	public short readShort() throws IbisIOException {
		emptyMsg = false;
		short v = input.readShort();
                if (streamChecking) {
                        debugReadShort(v);
                }
                return v;
	}
	
	public int readInt() throws IbisIOException {
		emptyMsg = false;
		int v = input.readInt();
                if (streamChecking) {
                        debugReadInt(v);
                }
                return v;
	}
	
	public long readLong() throws IbisIOException {
		emptyMsg = false;
		long v = input.readLong();
                if (streamChecking) {
                        debugReadLong(v);
                }
                return v;
	}
	
	public float readFloat() throws IbisIOException {
		emptyMsg = false;
		float v = input.readFloat();
                if (streamChecking) {
                        debugReadFloat(v);
                }
                return v;
	}
	
	public double readDouble() throws IbisIOException {
		emptyMsg = false;
		double v = input.readDouble();
                if (streamChecking) {
                        debugReadDouble(v);
                }
                return v;
	}
	
	public String readString() throws IbisIOException {
		emptyMsg = false;
		String v = input.readString();
                if (streamChecking) {
                        debugReadString(v);
                }
                return v;
	}
	
	public Object readObject()
		throws IbisIOException {
		Object v = input.readObject();
                if (streamChecking) {
                        debugReadObject(v);
                }
                return v;
	}
	

	public void readArrayBoolean(boolean [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayBoolean(userBuffer);
                if (streamChecking) {
                        debugReadArrayBoolean(userBuffer);
                }
	}

	public void readArrayByte(byte [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

                emptyMsg = false;
                input.readArrayByte(userBuffer);
                if (streamChecking) {
                        debugReadArrayByte(userBuffer);
                }
	}

	public void readArrayChar(char [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayChar(userBuffer);
                if (streamChecking) {
                        debugReadArrayChar(userBuffer);
                }
	}

	public void readArrayShort(short [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayShort(userBuffer);
                if (streamChecking) {
                        debugReadArrayShort(userBuffer);
                }
	}

	public void readArrayInt(int [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayInt(userBuffer);
                if (streamChecking) {
                        debugReadArrayInt(userBuffer);
                }
	}

	public void readArrayLong(long [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                input.readArrayLong(userBuffer);
                if (streamChecking) {
                        debugReadArrayLong(userBuffer);
                }
	}

	public void readArrayFloat(float [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
                input.readArrayFloat(userBuffer);
                if (streamChecking) {
                        debugReadArrayFloat(userBuffer);
                }
	}

	public void readArrayDouble(double [] userBuffer) throws IbisIOException {
		if (userBuffer.length == 0)
			return;

		emptyMsg = false;
		input.readArrayDouble(userBuffer);
                if (streamChecking) {
                        debugReadArrayDouble(userBuffer);
                }
	}


	public void readSubArrayBoolean(boolean [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayBoolean(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayBoolean(userBuffer, offset, length);
                }
	}

	public void readSubArrayByte(byte [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
                input.readSubArrayByte(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayByte(userBuffer, offset, length);
                }
	}

	public void readSubArrayChar(char [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayChar(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayChar(userBuffer, offset, length);
                }
	}

	public void readSubArrayShort(short [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayShort(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayShort(userBuffer, offset, length);
                }
	}

	public void readSubArrayInt(int [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayInt(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayInt(userBuffer, offset, length);
                }
	}

	public void readSubArrayLong(long [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayLong(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayLong(userBuffer, offset, length);
                }
	}

	public void readSubArrayFloat(float [] userBuffer, int offset, int length) throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayFloat(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayFloat(userBuffer, offset, length);
                }
	}

	public void readSubArrayDouble(double [] userBuffer, int offset, int length)
		throws IbisIOException {
		if (length == 0)
			return;

		emptyMsg = false;
		input.readSubArrayDouble(userBuffer, offset, length);
                if (streamChecking) {
                        debugReadSubArrayDouble(userBuffer, offset, length);
                }
	}
	
        /*
         * - Debug section -
         */
	public void debugReadBoolean(boolean v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        boolean _v = is.readBoolean();
                        if (v != _v) {
                                 throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadByte(byte v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        byte _v = is.readByte();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadChar(char v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        char _v = is.readChar();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadShort(short v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        short _v = is.readShort();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadInt(int v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        int _v = is.readInt();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadLong(long v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        long _v = is.readLong();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}
	
	public void debugReadFloat(float v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        float _v = is.readFloat();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadDouble(double v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        double _v = is.readDouble();
                        if (v != _v) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadString(String v) throws IbisIOException {
                ObjectInputStream is = (ObjectInputStream)sendPortIs.get(activeSendPortNum);
                try {
                        String _v = is.readUTF();
                        if (!v.equals(_v)) {
                                throw new Error("data mismatch: "+v+", should be:"+_v);
                        }

                } catch(Exception x) {
                        throw new Error(x.getMessage());
                }

	}

	public void debugReadObject(Object v) throws IbisIOException {
                System.err.println("debugReadObject:"+v.getClass().getName());
                debugReadString(v.getClass().getName());
	}

	public void debugReadArrayBoolean(boolean [] b) throws IbisIOException {
		debugReadSubArrayBoolean(b, 0, b.length);
	}
	
	public void debugReadArrayByte(byte [] b) throws IbisIOException {
		debugReadSubArrayByte(b, 0, b.length);
	}
	
	public void debugReadArrayChar(char [] b) throws IbisIOException {
                debugReadSubArrayChar(b, 0, b.length);
	}
	
	public void debugReadArrayShort(short [] b) throws IbisIOException {
                debugReadSubArrayShort(b, 0, b.length);
	}
	
	public void debugReadArrayInt(int [] b) throws IbisIOException {
                debugReadSubArrayInt(b, 0, b.length);
	}
	
	public void debugReadArrayLong(long [] b) throws IbisIOException {
                debugReadSubArrayLong(b, 0, b.length);
	}
	
	public void debugReadArrayFloat(float [] b) throws IbisIOException {
                debugReadSubArrayFloat(b, 0, b.length);
	}
	
	public void debugReadArrayDouble(double [] b) throws IbisIOException {
                debugReadSubArrayDouble(b, 0, b.length);
	}
	

	public void debugReadSubArrayBoolean(boolean [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadBoolean(b[o+i]);
                }

	}
	
	public void debugReadSubArrayByte(byte [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadByte(b[o+i]);
                }

	}
	
	public void debugReadSubArrayChar(char [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadChar(b[o+i]);
                }

	}
	
	public void debugReadSubArrayShort(short [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadShort(b[o+i]);
                }

	}
	
	public void debugReadSubArrayInt(int [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadInt(b[o+i]);
                }

	}
	
	public void debugReadSubArrayLong(long [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadLong(b[o+i]);
                }

	}
	
	public void debugReadSubArrayFloat(float [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadFloat(b[o+i]);
                }

	}
	
	public void debugReadSubArrayDouble(double [] b, int o, int l) throws IbisIOException {
                debugReadInt(l);
                for (int i = 0; i < l; i++) {
                        debugReadDouble(b[o+i]);
                }

        }
} 
