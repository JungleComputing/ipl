package ibis.ipl.impl.net;

import ibis.ipl.DynamicProperties;
import ibis.ipl.IbisException;
import ibis.ipl.ReadMessage;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPortIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.Upcall;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import java.util.Iterator;
import java.util.Hashtable;


/**
 * Provides an implementation of the {@link ReceivePort} and {@link
 * ReadMessage} interfaces of the IPL.
 */
public final class NetReceivePort implements ReceivePort, ReadMessage, NetInputUpcall, NetPort, NetEventQueueConsumer {





        /* ___ INTERNAL CLASSES ____________________________________________ */




        private final class UpcallThread extends Thread {
                private NetMutex         sleep = new NetMutex(true);
                private ReadMessage      rm    = null;
                private volatile boolean end  = false;
                
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
                                                finish();
                                        } catch (Exception e) {
                                                e.printStackTrace();
                                                throw new Error(e.getMessage());
                                        }
                                }

                                synchronized (threadStack) {
                                        if (threadStackPtr < threadStackSize) {
                                                threadStack[threadStackPtr++] = this;
                                        } else {
                                                return;
                                        }
                                }
                        }
                }

                public void exec(ReadMessage rm) {
                        this.rm = rm;
                        sleep.unlock();
                }
                
		protected void end() {
			end = true;
                        this.interrupt();
		}
        }


        
	/* --- incoming connection manager thread -- */

	/**
	 * The incoming connection management thread class.
	 */
	private final class AcceptThread extends Thread {

		/**
		 * Flag indicating whether thread termination was requested.
		 */
		private volatile boolean end = false;

                public AcceptThread(String name) {
                        super("NetReceivePort.AcceptThread: "+name);
                        setDaemon(true);
                }

		/**
		 * The incoming connection management function.
		 * Note: the thread is <strong>uninterruptible</strong>
		 * during the network input locking.
		 */
		public void run() {
                        
		accept_loop:
			while (!end) {
                                NetServiceLink link = null;
                                // System.err.println("receivePort: accept-->");
				try {
                                        link = new NetServiceLink(eventQueue, serverSocket);
				} catch (NetIbisInterruptedException e) {
					continue accept_loop;
                                } catch (NetIbisException e) {
					__.fwdAbort__(e);
				}
                                // System.err.println("receivePort: accept - ok");

				Integer               num = null;
				NetSendPortIdentifier spi = null;

                                num = new Integer(nextSendPortNum++);
                                
                                try {
                                        link.init(num);
                                } catch (NetIbisException e) {
					__.fwdAbort__(e);
				}

                                try {
                                        ObjectInputStream is = new ObjectInputStream(link.getInputSubStream("__port__"));
                                        spi = (NetSendPortIdentifier)is.readObject();
                                        is.close();
                                } catch (IOException e) {
                                        __.fwdAbort__(e);
				} catch (ClassNotFoundException e) {
                                        __.fwdAbort__(e);
				}

			connect_loop:
				while (!end) {
					try {
						connectionLock.ilock();
						inputLock.lock();
                                                
                                                NetConnection cnx  = new NetConnection(NetReceivePort.this, num, spi, identifier, link);

                                                synchronized(connectionTable) {
                                                        connectionTable.put(num, cnx);
                                                }

                                                // System.err.println("receivePort: accept - connection");
System.err.println(NetReceivePort.this + ": accept - connection");
                                                if (useUpcall || (upcall != null && !usePollingThread)) {
                                                        input.setupConnection(cnx, NetReceivePort.this);
                                                } else {
                                                        input.setupConnection(cnx, null);
                                                }
                                                
                                                // System.err.println("receivePort: accept - connection ok");
						if (num.intValue() == 0) {
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
		protected void end() {
			end = true;
                        this.interrupt();
		}
	}

	/**
	 * The optional asynchronous polling thread class.
	 */
	private final class PollingThread extends Thread {
		private volatile boolean end = false;

                public PollingThread(String name) {
                        super("NetReceivePort.PollingThread: "+name);
                        setDaemon(true);
                }

		/**
		 * The asynchronous polling function.
		 * Note: the thread is <strong>uninterruptible</strong>
		 * during the network input locking.
		 */
		public void run() {
		polling_loop:
			while (!end) {
				try {
					pollingLock.ilock();
                                        pollingNotify = true;
                                        
					inputLock.lock();
					activeSendPortNum = input.poll(false);
					inputLock.unlock();

					if (activeSendPortNum == null) {
						pollingLock.unlock();
						if (useYield) {
							yield();
                                                }
                                                
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
				} catch (NetIbisException e) {
					// TODO: pass the exception back
					//       to the application
					__.fwdAbort__(e);
				}
			}
		}

		/**
		 * Requests for the thread completion.
		 */
		protected void end() {
			end = true;
			this.interrupt();
		}
	}
	





        /* ___ CONFIGURATION FLAGS _________________________________________ */


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
	 * Flag indicating whether receive should block in the poll()
	 * that necessarily precedes it, or whether we want to poll in a
	 * busy-wait style from this.
	 */
	public final static  boolean     useBlockingPoll     = true;





        /* ___ EVENT QUEUE _________________________________________________ */

	private NetEventQueue         eventQueue             = null;
        private NetEventQueueListener eventQueueListener     = null;






        /* ___ LESS-IMPORTANT OBJECTS ______________________________________ */

	/**
	 * The name of the port.
	 */
	private String                   name          	     =  null;

	/**
	 * The type of the port.
	 */
	private NetPortType              type          	     =  null;

	/**
	 * The upcall callback function.
	 */
	private Upcall                   upcall        	     =  null;

	/**
	 * The port identifier.
	 */
	private NetReceivePortIdentifier identifier    	     =  null;

	/**
	 * The TCP server socket.
	 */
	private ServerSocket 	         serverSocket        =  null;

	/**
	 * The next send port integer number.
	 */
	private int          	         nextSendPortNum     =  0;

	/**
	 * Performance statistic
	 */
	// private int			n_yield;




        /* ___ IMPORTANT OBJECTS ___________________________________________ */

        private Hashtable    	         connectionTable     =  null;

	/**
	 * The port's topmost driver.
	 */
	private NetDriver                driver        	     =  null;

	/**
	 * The port's topmost input.
	 */
	private NetInput                 input        	     =  null;





        /* ___ THREADS _____________________________________________________ */

	/**
	 * The incoming connection management thread.
	 */
	private AcceptThread             acceptThread  	     =  null;

	/**
	 * The optionnal asynchronous polling thread.
	 */
	private PollingThread            pollingThread       =  null;

        private final int                threadStackSize     = 256;

        private UpcallThread[]           threadStack         = new UpcallThread[threadStackSize];





        /* ___ STATE _______________________________________________________ */

	/**
	 * The current active peer port.
	 */
	private volatile Integer         activeSendPortNum   =  null;

	/**
	 * Flag indicating whether incoming connections are currently enabled.
	 */
	private boolean                  connectionEnabled   =  false;

	/**
	 * Flag indicating whether successful polling operation should
	 * generate an upcall or not.
	 */
	private boolean                  upcallsEnabled      = false;

	/**
	 * The empty message detection flag.
	 *
	 * The flag is set on each new {@link #_receive} call and should
	 * be cleared as soon as at least a byte as been added to the living message.
	 */
	private boolean               	 emptyMsg     	     = 	true;

        private volatile boolean         finishNotify        =  false;
        private volatile boolean         pollingNotify       =  false;
        private volatile Runnable        currentThread       =  null;

        private int                      threadStackPtr      = 0;
        private int                      upcallThreadNum     = 0;





        /* ___ LOCKS _______________________________________________________ */

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

        private NetMutex                 finishMutex         =  null;

	/* --- Upcall from main input object -- */
        public synchronized void inputUpcall(NetInput input, Integer spn) {
                // System.err.println("NetReceivePort: inputUpcall-->");
                if (this.input == null) {
                        __.warning__("message lost");
                        return;
                }

                if (spn == null) {
                        throw new Error("invalid state");
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
                // System.err.println("NetReceivePort: inputUpcall<--");
        }
        


	/* --- NetEventQueueConsumer part --- */
        public void event(NetEvent e) {
                NetPortEvent event = (NetPortEvent)e;

                switch (event.code()) {
                        case NetPortEvent.CLOSE_EVENT: 
                                {
                                        Integer num = (Integer)event.arg();
                                        NetConnection cnx = null;

                                        /*
                                         * Potential race condition here:
                                         * The event can be triggered _before_
                                         * the connection is added to the table.
                                         */
                                        synchronized(connectionTable) {
                                                cnx = (NetConnection)connectionTable.remove(num);
                                        }
                                                
                                        if (cnx != null) {
                                                try {
                                                        close(cnx);
                                                } catch (NetIbisException nie) {
                                                        throw new Error(nie);
                                                }
                                        }
                                }
                        break;

                default:
                        throw new Error("invalid event code");
                }
                
        }
        

	/* --- NetPort part --- */
        public NetPortType getPortType() {
                return type;
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
		throws NetIbisException {
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
		throws NetIbisException {
                //System.err.println("NetReceivePort: <init>-->");
		this.type      = type;
		this.name      = name;
		this.upcall    = upcall;
		upcallsEnabled = false;
		connectionTable  = new Hashtable();
		polledLock     = new NetMutex(true);
		pollingLock    = new NetMutex(false);
		connectionLock = new NetMutex(true);
		inputLock      = new NetMutex(false);
		finishMutex    = new NetMutex(true);

		NetIbis ibis   = type.getIbis();

                {
                        String mainDriverName = type.getStringProperty("/", "Driver");

                        if (mainDriverName == null) {
                                throw new NetIbisException("root driver not specified");
                        }
                        
                        driver   		 = ibis.getDriver(mainDriverName);
                        if (driver == null) {
                                throw new NetIbisException("driver not found");
                        }
                }
                
		input                  = driver.newInput(type, null);

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

                if (! usePollingThread) {
		    System.err.println("Run NetReceivePort " + this + " without PollingThread");
		}
                if (! useUpcall) {
		    System.err.println("Run NetReceivePort " + this + " without Upcall");
		}
                if (useBlockingPoll) {
		    System.err.println("Run NetReceivePort " + this + " with useBlockingPoll");
		}

		try {
			serverSocket = new ServerSocket(0, 1, InetAddress.getLocalHost());
		} catch (IOException e) {
			throw new NetIbisException(e);
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
		
                eventQueue         = new NetEventQueue();
                eventQueueListener = new NetEventQueueListener(this, "ReceivePort: "+addr+"["+port+"]", eventQueue);
                eventQueueListener.start();

		acceptThread.start();

                //System.err.println("NetReceivePort settings");
                //System.err.println("useUpcallThreadPool = " + useUpcallThreadPool);
                //System.err.println("usePollingThread    = " + usePollingThread   );
                //System.err.println("useUpcallThread     = " + useUpcallThread    );
                //System.err.println("useUpcall           = " + useUpcall          );
                //System.err.println("useYield            = " + useYield           );
                //System.err.println("_______________________");

                //System.err.println("NetReceivePort: <init><--");
	}

	/**
	 * The internal synchronous polling function.
	 *
	 * The calling thread is <strong>uninterruptible</strong> during
	 * the network input locking operation. The function may block
	 * if the {@linkplain #inputLock network input lock} is not available.
	 */
	private boolean _doPoll(boolean block) throws NetIbisException {
// System.err.println("NetReceivePort._doPoll() attempts to grab inputLock");
// System.err.print("[");
		inputLock.lock();
		activeSendPortNum = input.poll(block);
                //System.err.println("activeSendPortNum = "+activeSendPortNum);
		inputLock.unlock();
// System.err.print("]");

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
	public ReadMessage receive() throws NetIbisException {
		//System.err.println(this + ".receive()-->: usePollingThread " + usePollingThread + " useUpcall " + useUpcall + " upcall " + upcall);
		if (usePollingThread || useUpcall || upcall != null) {
                        //System.err.println("NetReceivePort: receive - blocking wait");
			polledLock.lock();
		} else {
                        //System.err.println("NetReceivePort: receive - active wait");
			if (useYield) {
				while (!_doPoll(useBlockingPoll)) {
					Thread.currentThread().yield();
					// n_yield++;
				}
			} else {
				while (!_doPoll(useBlockingPoll));
			}
			
		}
		
                //System.err.println("NetReceivePort: receive<--");
		return _receive();
	}

	/**
	 * {@inheritDoc}
	 */
	public ReadMessage receive(ReadMessage finishMe)
		throws NetIbisException {
		if (finishMe != null) {
                        ((NetReceivePort)finishMe).finish();
                }
                
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
	public ReadMessage poll()  throws NetIbisException {
                //System.err.println("NetReceivePort: poll-->");
		if (usePollingThread || useUpcall || upcall != null) {
			if (!polledLock.trylock())
                                //System.err.println("NetReceivePort: poll<-- failed 1");
				return null;
		} else {
			if (!_doPoll(false)) {
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
		throws NetIbisException {
		if (finishMe != null) {
                        ((NetReceivePort)finishMe).finish();
                }

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
                /*
		if (activeSendPortNum == null)
			return null;
                */

		if (activeSendPortNum == null)
                        throw new Error("no active sendPort");
                
                NetConnection cnx = (NetConnection)connectionTable.get(activeSendPortNum);
                if (cnx.getSendId() == null) {
                        throw new Error("invalid state");
                }
                
		return cnx.getSendId();
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



        private void close(NetConnection cnx) throws NetIbisException {
                if (cnx == null)
                        return;

                // System.err.println("NetReceivePort: close-->");
                input.close(cnx.getNum());
                try {
                        cnx.close();
                } catch (Exception e) {
                        throw new Error(e.getMessage());
                }
                // System.err.println("NetReceivePort: close<--");
        }
        
                

	/**
	 * Closes the port. 
	 *
	 * Note: surprinsingly, ReceivePort.free is not declared to
	 * throw NetIbisException while SendPort.free does.
	 */
	public void free() {
                // System.err.println("NetReceivePort: free-->");
                //System.err.println("NetReceivePort["+this+"]: free-->");
                synchronized(this) {
                        try {
                                if (inputLock != null) {
                                        inputLock.lock();
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

                                if (input != null) {
                                        input.free();
                                }

                                if (inputLock != null) {
                                        inputLock.unlock();
                                }
                        } catch (Exception e) {
                                e.printStackTrace();
                                __.fwdAbort__(e);
                        }
                }

		// System.err.println(this + ": #yield = " + n_yield);
                //System.err.println("NetReceivePort["+this+"]: free<--");
                // System.err.println("NetReceivePort: free<--");
	}
	
	protected void finalize() throws Throwable {
                // System.err.println("ReceivePort: finalize-->");
                if (serverSocket != null) {
                        serverSocket.close();
                }

                free();

                for (int i = 0; i < threadStackSize; i++) {
                        UpcallThread ut = threadStack[i];
                        
                        if (ut != null) {
                                ut.end();
                                //System.err.println("waiting for ReceivePort upcall thread "+i+" to join");
                                while (true) {
                                        try {
                                                ut.join();
                                                break;
                                        } catch (InterruptedException e) {
                                                //
                                        }
                                }
                                //System.err.println("ReceivePort upcall thread "+i+" joined");
                        }
                }

                // System.err.println("ReceivePort: finalize - 2");

                if (acceptThread != null) {
                        acceptThread.end();
                        //System.err.println("waiting for ReceivePort accept thread to join");
                        while (true) {
                                try {
                                        acceptThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("ReceivePort accept thread joined");
                }

                // System.err.println("ReceivePort: finalize - 3");

                if (pollingThread != null) {
                        pollingThread.end();
                        //System.err.println("waiting for ReceivePort polling thread to join");
                        while (true) {
                                try {
                                        pollingThread.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("ReceivePort polling thread joined");
                }

                // System.err.println("ReceivePort: finalize - 4");

                if (eventQueueListener != null) {
                        eventQueueListener.end();
                        //System.err.println("waiting for ReceivePort eventQueue thread to join");
                        while (true) {
                                try {
                                        eventQueueListener.join();
                                        break;
                                } catch (InterruptedException e) {
                                        //
                                }
                        }
                        //System.err.println("ReceivePort eventQueue thread joined");
                }
                        
		super.finalize();
                //System.err.println("ReceivePort: finalize<--");
	}

	/* --- ReadMessage Part --- */
	public void finish() throws NetIbisException {
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
                return getActiveSendPortIdentifier();
	}


	public boolean readBoolean() throws NetIbisException {
		emptyMsg = false;
		boolean v = input.readBoolean();
                return v;
	}
	
	public byte readByte() throws NetIbisException {
		emptyMsg = false;
		byte v = input.readByte();
                return v;
	}
	
	public char readChar() throws NetIbisException {
		emptyMsg = false;
		char v = input.readChar();
                return v;
	}
	
	public short readShort() throws NetIbisException {
		emptyMsg = false;
		short v = input.readShort();
                return v;
	}
	
	public int readInt() throws NetIbisException {
		emptyMsg = false;
		int v = input.readInt();
                return v;
	}
	
	public long readLong() throws NetIbisException {
		emptyMsg = false;
		long v = input.readLong();
                return v;
	}
	
	public float readFloat() throws NetIbisException {
		emptyMsg = false;
		float v = input.readFloat();
                return v;
	}
	
	public double readDouble() throws NetIbisException {
		emptyMsg = false;
		double v = input.readDouble();
                return v;
	}
	
	public String readString() throws NetIbisException {
		emptyMsg = false;
		String v = input.readString();
                return v;
	}
	
	public Object readObject()
		throws NetIbisException {
		emptyMsg = false;
		Object v = input.readObject();
                return v;
	}
	

	public void readArrayBoolean(boolean [] b) throws NetIbisException {
		readArraySliceBoolean(b, 0, b.length);
	}

	public void readArrayByte(byte [] b) throws NetIbisException {
                readArraySliceByte(b, 0, b.length);
	}

	public void readArrayChar(char [] b) throws NetIbisException {
		readArraySliceChar(b, 0, b.length);
	}

	public void readArrayShort(short [] b) throws NetIbisException {
		readArraySliceShort(b, 0, b.length);
	}

	public void readArrayInt(int [] b) throws NetIbisException {
		readArraySliceInt(b, 0, b.length);
	}

	public void readArrayLong(long [] b) throws NetIbisException {
                readArraySliceLong(b, 0, b.length);
	}

	public void readArrayFloat(float [] b) throws NetIbisException {
                readArraySliceFloat(b, 0, b.length);
	}

	public void readArrayDouble(double [] b) throws NetIbisException {
		readArraySliceDouble(b, 0, b.length);
	}

	public void readArrayObject(Object [] b) throws NetIbisException {
		readArraySliceObject(b, 0, b.length);
	}


	public void readArraySliceBoolean(boolean [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceBoolean(b, o, l);
	}

	public void readArraySliceByte(byte [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
                input.readArraySliceByte(b, o, l);
	}

	public void readArraySliceChar(char [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceChar(b, o, l);
	}

	public void readArraySliceShort(short [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceShort(b, o, l);
	}

	public void readArraySliceInt(int [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceInt(b, o, l);
	}

	public void readArraySliceLong(long [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceLong(b, o, l);
	}

	public void readArraySliceFloat(float [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceFloat(b, o, l);
	}

	public void readArraySliceDouble(double [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceDouble(b, o, l);
	}

	public void readArraySliceObject(Object [] b, int o, int l) throws NetIbisException {
		if (l == 0)
			return;

		emptyMsg = false;
		input.readArraySliceObject(b, o, l);
	}
	
} 
