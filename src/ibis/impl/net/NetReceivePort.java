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
public final class NetReceivePort implements ReceivePort, ReadMessage, NetInputUpcall {
	
	/**
	 * Flag indicating whether the port use a polling thread.
	 */
        private boolean                  useUpcallThreadPool = true;

	private boolean                  usePollingThread    = false;

	private boolean                  useUpcallThread     = true;

	private boolean                  useUpcall           = false;

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

        private NetMutex                 finishMutex         =  null;
        private volatile boolean         finishNotify        =  false;
        private volatile boolean         pollingNotify       =  false;
        private volatile Runnable        currentThread       =  null;
        private final int                threadStackSize     = 256;
        private int                      threadStackPtr      = 0;
        private UpcallThread[]           threadStack         = new UpcallThread[threadStackSize];
        private int                      upcallThreadNum     = 0;

        private final class UpcallThread extends Thread {
                private NetMutex         sleep = new NetMutex(true);
                private ReadMessage      rm    = null;
                private volatile boolean end   = false;

                
                public UpcallThread(String name) {
                        super("NetReceivePort.UpcallThread: ");
                        start();
                }
                
                public void run() {
                        while (!end) {
                                try {
                                        sleep.ilock();
                                } catch (InterruptedException e) {
                                        continue;
                                }
                                
                                upcall.upcall(rm);

                                if (currentThread == this) {
                                        try {
                                                //System.err.println("finish was not called");
                                                finish();
                                        } catch (Exception e) {
                                                throw new Error(e.getMessage());
                                        }
                                }

                                synchronized (threadStack) {
                                        if (threadStackPtr < threadStackSize) {
                                                threadStack[threadStackPtr++] = this;
                                        }
                                        else
                                                return;
                                }
                        }
                }

                public void exec(ReadMessage rm) {
                        this.rm = rm;
                        sleep.unlock();
                }
                
        }
        

	/* --- Upcall from main input object -- */
        public synchronized void inputUpcall(NetInput input, Integer spn) {
                //System.err.println("NetReceivePort: inputUpcall-->");
                if (this.input == null) {
                        __.warning__("message lost");
                        return;
                }

                activeSendPortNum = spn;
                if (upcall != null && upcallsEnabled) {
                        final ReadMessage rm = _receive();
                        if (!useUpcallThread) {
                                //System.err.println("NetReceivePort: upcall-->");
                                upcall.upcall(rm);
                                //System.err.println("NetReceivePort: upcall<--");
                                if (emptyMsg) {
                                        try {
                                                readByte();
                                        } catch (Exception e) {
                                                throw new Error(e.getMessage());
                                        }
                                        
                                        emptyMsg = false;
                                }
                        } else {
                                finishNotify = true;

                                if (!useUpcallThreadPool) {
                                        Runnable r = new Runnable() {
                                                        public void run() {
                                                                //System.err.println("NetReceivePort: threaded upcall-->");
                                                                upcall.upcall(rm);
                                                                //System.err.println("NetReceivePort: threaded upcall<--");

                                                                if (currentThread == this) {
                                                                        try {
                                                                                finish();
                                                                        } catch (Exception e) {
                                                                                throw new Error(e.getMessage());
                                                                        }
                                                                }
                                                        }
                                                };

                                        currentThread = r;
                                        (new Thread(r)).start();
                                                
                                        finishMutex.lock();

                                } else {
                                        UpcallThread ut = null;
                                        
                                        synchronized(threadStack) {
                                                if (threadStackPtr > 0) {
                                                        ut = threadStack[--threadStackPtr];
                                                } else {
                                                        ut = new UpcallThread("no "+upcallThreadNum++);
                                                }        
                                        }

                                        currentThread = ut;
                                        ut.exec(rm);
                                        finishMutex.lock();
                                }
                        }
                } else {
                        finishNotify = true;
                        polledLock.unlock();
                        finishMutex.lock();
                }
                //System.err.println("NetReceivePort: inputUpcall<--");
        }
        


	/* --- incoming connection manager thread -- */

	/**
	 * The incoming connection management thread class.
	 */
	private final class AcceptThread extends Thread {

		/**
		 * Flag indicating whether thread termination was requested.
		 */
		private volatile boolean stop = false;

                public AcceptThread(String name) {
                        super("NetReceivePort.AcceptThread: "+name);
                }

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
                                                NetServiceListener nls = new NetServiceListener(is);
                                                sendPortNLS.put(spn, nls);

                                                if (useUpcall || (upcall != null && !usePollingThread)) {
                                                        input.setupConnection(spn, is, os, nls, NetReceivePort.this);
                                                } else {
                                                        input.setupConnection(spn, is, os, nls, null);
                                                } 
                                                
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

                public PollingThread(String name) {
                        super("NetReceivePort.PollingThread: "+name);
                }

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
                                        pollingNotify = true;
                                        
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
                                                final ReadMessage rm = _receive();

                                                if (!useUpcallThread) {
                                                        upcall.upcall(rm);
                                                } else {
                                                        if (!useUpcallThreadPool) {
                                                                Runnable r = new Runnable() {
                                                                                public void run() {
                                                                                        upcall.upcall(rm);
                                                                                        if (currentThread == this) {
                                                                                                try {
                                                                                                        finish();
                                                                                                } catch (Exception e) {
                                                                                                        throw new Error(e.getMessage());
                                                                                                }
                                                                                        }
                                                                                }
                                                                        };
                                                                currentThread = r;
                                                                (new Thread(r)).start();
                                                        } else {
                                                                UpcallThread ut = null;
                                        
                                                                synchronized(threadStack) {
                                                                        if (threadStackPtr > 0) {
                                                                                ut = threadStack[--threadStackPtr];
                                                                        } else {
                                                                                ut = new UpcallThread("no "+upcallThreadNum++);
                                                                        }        
                                                                }

                                                                currentThread = ut;
                                                                ut.exec(rm);
                                                        }
                                                }                                                
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
		finishMutex 	       = new NetMutex(true);

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

		usePollingThread       = (type.getBooleanStringProperty(null, "UsePollingThread",     new Boolean(usePollingThread))).booleanValue();
		useUpcallThread        = (type.getBooleanStringProperty(null, "UseUpcallThread",      new Boolean(useUpcallThread))).booleanValue();
		useUpcallThreadPool    = (type.getBooleanStringProperty(null, "UseUpcallThreadPool",  new Boolean(useUpcallThreadPool))).booleanValue();
		useYield               = (type.getBooleanStringProperty(null, "UseYield",             new Boolean(useYield))).booleanValue();
		useUpcall              = (type.getBooleanStringProperty(null, "UseUpcall",            new Boolean(useUpcall))).booleanValue();

                if (usePollingThread) {
                        useUpcall = false;
                }
                else if (!useUpcall && upcall != null) {
                        useUpcall = true;
                }

		try {
			serverSocket = new ServerSocket(0, 1, InetAddress.getLocalHost());
		} catch (IOException e) {
			throw new IbisIOException(e);
		}
			
		Hashtable info = new Hashtable();

                InetAddress addr = serverSocket.getInetAddress();
                int         port = serverSocket.getLocalPort();
		
		info.put("accept_address", addr);
		info.put("accept_port",    new Integer(port));

		NetIbisIdentifier ibisId = (NetIbisIdentifier)ibis.identifier();
		identifier    = new NetReceivePortIdentifier(name, type.name(), ibisId, info);
		
		acceptThread  = new AcceptThread(addr+"["+port+"]");
		if (usePollingThread) {
			pollingThread = new PollingThread(addr+"["+port+"]");
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

		return true;
	}

	/**
	 * Internally initializes a new reception.
	 */
	private ReadMessage _receive() {
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
                //System.err.println("NetReceivePort: receive-->");
		if (usePollingThread || useUpcall || upcall != null) {
                        //System.err.println("NetReceivePort: receive - blocking wait");
			polledLock.lock();
		} else {
                        //System.err.println("NetReceivePort: receive - active wait");
			if (useYield) {
				while (!_doPoll())
					Thread.currentThread().yield();
			} else {
				while (!_doPoll());
			}
			
		}
		
                //System.err.println("NetReceivePort: receive<--");
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
                //System.err.println("NetReceivePort: poll-->");
		if (usePollingThread || useUpcall || upcall != null) {
			if (!polledLock.trylock())
                                //System.err.println("NetReceivePort: poll<-- failed 1");
				return null;
		} else {
			if (!_doPoll()) {
                                //System.err.println("NetReceivePort: poll<-- failed 2");
				return null;
			}
		}
		
                //System.err.println("NetReceivePort: poll<--");
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
	public synchronized void enableUpcalls() {
		upcallsEnabled = true;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void disableUpcalls() {
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
	public void free() {
                //System.err.println("NetReceivePort["+this+"]: free-->");
                synchronized(this) {
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
                                e.printStackTrace();
                                __.fwdAbort__(e);
                        }
                }
                //System.err.println("NetReceivePort["+this+"]: free<--");
	}
	
	protected void finalize()
		throws Throwable {
		free();
		super.finalize();
	}

	/* --- ReadMessage Part --- */
	public void finish() throws IbisIOException {
                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]: NetReceivePort finish-->");
                //System.err.println("NetReceivePort: finish-->");
		if (emptyMsg) {
			readByte();
                        emptyMsg = false;
		}
		activeSendPortNum = null;
		input.finish();
                currentThread = null;
                
                if (finishNotify) {
                        finishNotify = false;
                        finishMutex.unlock();
                }

                if (pollingNotify) {
                        pollingNotify = false;
                        pollingLock.unlock();
                }

                //System.err.println("NetReceivePort: finish<--");
                //System.err.println("["+ibis.util.nativeCode.Rdtsc.rdtsc()+"]: NetReceivePort finish<--");
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
                return v;
	}
	
	public byte readByte() throws IbisIOException {
		emptyMsg = false;
		byte v = input.readByte();
                return v;
	}
	
	public char readChar() throws IbisIOException {
		emptyMsg = false;
		char v = input.readChar();
                return v;
	}
	
	public short readShort() throws IbisIOException {
		emptyMsg = false;
		short v = input.readShort();
                return v;
	}
	
	public int readInt() throws IbisIOException {
		emptyMsg = false;
		int v = input.readInt();
                return v;
	}
	
	public long readLong() throws IbisIOException {
		emptyMsg = false;
		long v = input.readLong();
                return v;
	}
	
	public float readFloat() throws IbisIOException {
		emptyMsg = false;
		float v = input.readFloat();
                return v;
	}
	
	public double readDouble() throws IbisIOException {
		emptyMsg = false;
		double v = input.readDouble();
                return v;
	}
	
	public String readString() throws IbisIOException {
		emptyMsg = false;
		String v = input.readString();
                return v;
	}
	
	public Object readObject()
		throws IbisIOException {
		Object v = input.readObject();
                return v;
	}
	

	public void readArrayBoolean(boolean [] b) throws IbisIOException {
		readArraySliceBoolean(b, 0, b.length);
	}

	public void readArrayByte(byte [] b) throws IbisIOException {
                readArraySliceByte(b, 0, b.length);
	}

	public void readArrayChar(char [] b) throws IbisIOException {
		readArraySliceChar(b, 0, b.length);
	}

	public void readArrayShort(short [] b) throws IbisIOException {
		readArraySliceShort(b, 0, b.length);
	}

	public void readArrayInt(int [] b) throws IbisIOException {
		readArraySliceInt(b, 0, b.length);
	}

	public void readArrayLong(long [] b) throws IbisIOException {
                readArraySliceLong(b, 0, b.length);
	}

	public void readArrayFloat(float [] b) throws IbisIOException {
                readArraySliceFloat(b, 0, b.length);
	}

	public void readArrayDouble(double [] b) throws IbisIOException {
		readArraySliceDouble(b, 0, b.length);
	}

	public void readArrayObject(Object [] b) throws IbisIOException {
		readArraySliceObject(b, 0, b.length);
	}


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceBoolean(b, o, l);
	}

	public void readArraySliceByte(byte [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
                input.readArraySliceByte(b, o, l);
	}

	public void readArraySliceChar(char [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceChar(b, o, l);
	}

	public void readArraySliceShort(short [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceShort(b, o, l);
	}

	public void readArraySliceInt(int [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceInt(b, o, l);
	}

	public void readArraySliceLong(long [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceLong(b, o, l);
	}

	public void readArraySliceFloat(float [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceFloat(b, o, l);
	}

	public void readArraySliceDouble(double [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceDouble(b, o, l);
	}

	public void readArraySliceObject(Object [] b, int o, int l) throws IbisIOException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceObject(b, o, l);
	}
	
} 
