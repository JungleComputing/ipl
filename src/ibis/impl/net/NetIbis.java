package ibis.impl.net;

import ibis.impl.nameServer.NameServer;
import ibis.ipl.Ibis;
import ibis.ipl.IbisConfigurationException;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.IbisRuntimeException;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.ipl.StaticProperties;
import ibis.util.IbisSocketFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Provides a generic {@link Ibis} implementation for pluggable network driver
 * support.
 */
public final class NetIbis extends Ibis {

	/**
         * The compiler name.
         */
	private static final String COMPILER = java.lang.System.getProperty("java.lang.compiler");

	static IbisSocketFactory socketFactory = IbisSocketFactory.createFactory();

	/**
	 * The driver loading mode.
	 *
	 * <DL>
	 * <DT><CODE>true</CODE><DD>known drivers are statically loaded at initialization time.
	 * <DT><CODE>false</CODE><DD>every driver is loaded dynamically.
	 * </DL>
         * Note: if {@linkplain #COMPILER} is set and equals <code>manta</code>, this
         * flag is automatically set to <code>true</code>
	 */
	private static final boolean staticDriverLoading = (COMPILER != null && COMPILER.equals("manta")) || false;

	/**
	 * The cache for previously created port types.
	 */
	private   Hashtable         portTypeTable    = new Hashtable();

	/**
	 * The cache for previously loaded drivers.
	 */
	private   Hashtable         driverTable      = new Hashtable();

	/**
	 * This {@link NetIbis} instance identifier for the <I>name server</I>.
	 */
	private   NetIbisIdentifier identifier       = null;

	private	  boolean i_joined = false;

	/**
	 * This {@link NetIbis} instance <I>name server</I> client.
	 */
	protected NameServer  nameServer = null;

	/**
	 * The openness of our <I>world</I>.
	 * <DL>
	 * <DT><CODE>true</CODE><DD>Any {@link NetIbis} instance can connect to this {@link NetIbis} instance
	 * <DT><CODE>false</CODE><DD>No {@link NetIbis} instance can connect to this {@link NetIbis} instance
	 * </DL>
	 */
	private   boolean           open             = false;

	/**
	 * The number of {@link NetIbis} instances in our <I>name server</I> nameServer pool.
	 */
	private   int 	       	    poolSize         = 0;

	/**
	 * The {@link NetIbis} instances that attempted to join our nameServer pool while our world was {@linkplain #open closed}.
	 */
	private   Vector 	    joinedIbises     = new Vector();

	/**
	 * The {@link NetIbis} instances that attempted to leave our nameServer pool while our world was {@linkplain #open closed}.
	 */
	private   Vector 	    leftIbises       = new Vector();

	/**
	 * The {@link NetIbis} instances that attempted to be deleted from our pool while our world was {@linkplain #open closed}.
	 */
	private   Vector 	    toBeDeletedIbises       = new Vector();


	/**
	 * Maintain linked lists of our send and receive ports so they can
	 * be forced-close at Ibis end.
	 */
	private NetSendPort	sendPortList	= null;
	private NetReceivePort	receivePortList	= null;


	/**
	 * The {@link NetIbis} {@linkplain NetBank bank}.
         *
         * This {@linkplain NetBank bank} can be used as general
         * purpose and relatively safe repository for global object
         * instances.
         */
	private   NetBank           bank             = new NetBank();

	public static ibis.util.PoolInfo poolInfo;
	static {
	    try {
		poolInfo = ibis.util.PoolInfo.createPoolInfo();
		hostName = poolInfo.hostName();
	    } catch (ibis.ipl.IbisException e) {
		// OK, no pool, so this better not be closed world
		try {
		    InetAddress addr = InetAddress.getLocalHost();
		    addr = InetAddress.getByName(addr.getHostAddress());
		    hostName = addr.getHostName();
		} catch (UnknownHostException e1) {
		    // Ignore if it won't work
		}
	    }
	}

        private   int               closedPoolRank   = -1;
        private   int               closedPoolSize   =  0;

        /**
         * The master {@link Ibis} instance for this process.
         */
	static volatile NetIbis globalIbis;

	private static String hostName;

	public static String hostName() {
	    return hostName;
	}

	/**
	 * Default constructor.
	 *
	 * May load compile-time known drivers.
	 *
	 * @exception IbisConfigurationException if the requested driver class
	 * 		could not be loaded or the requested driver instance
	 * 		could not be initialized.
	 */
	public NetIbis() {
		if (globalIbis == null) {
			globalIbis = this;
		}

		if (staticDriverLoading) {
		    String[] drivers = {
					"gen"
					, "bytes"
					, "id"
					, "pipe"
					, "udp"
					, "muxer"
					, "tcp"
					, "tcp_blk"
					, "rel"
					// , "gm"
					};
		    for (int i = 0; i < drivers.length; i++) {
			try {
			    Class clazz = Class.forName("ibis.ipl.impl.net." + drivers[i] + ".Driver");
			    NetDriver d = (NetDriver)clazz.newInstance();
			    d.setIbis(this);
			} catch (java.lang.Exception e) {
			    throw new IbisConfigurationException("Cannot instantiate class " + drivers[i], e);
			}
		    }
		}

		if (poolInfo == null) {
		    closedPoolRank = joinedIbises.indexOf(identifier());
		} else {
		    closedPoolSize = poolInfo.size();
		    closedPoolRank = poolInfo.rank();
		    // System.err.println("I am node " + closedPoolRank + " out of " + closedPoolSize);
		}
	}

	/**
	 * Returns the {@link NetBank}.
	 *
	 * @return The {@link NetBank}.
	 */
	public NetBank getBank() {
		return bank;
	}

	/**
	 * Returns an instance of the driver corresponding to the given name.
	 *
	 * If the driver has not been loaded, it is instanciated on the fly. The
	 * driver's name is the suffix to append to the NetIbis package to get
	 * access to the driver's package. The driver's package must contain a
	 * class named <CODE>Driver</CODE> which extends {@link NetDriver}.
	 *
	 * @param name the driver's name.
	 * @return The driver instance.
	 * @exception IbisConfigurationException if the requested driver class
	 * 		could not be loaded or the requested driver instance
	 * 		could not be initialized.
	 */
	/* This must be synchronized: two threads may attempt to create a
	 * port concurrently; if not synch, the driver may be created twice
	 *							RFHH
	 */
	synchronized
	public NetDriver getDriver(String name) {
		NetDriver driver = (NetDriver)driverTable.get(name);

		if (driver == null) {
			String clsName  = getClass().getPackage().getName()
					    + "."
					    + name
					    + ".Driver";
			try {
				Class       cls      = Class.forName(clsName);
				Class []    clsArray = { getClass() };
				Constructor cons     = cls.getConstructor(clsArray);
				Object []   objArray = { this };

				driver = (NetDriver)cons.newInstance(objArray);
			} catch (Exception e) {
				throw new IbisConfigurationException("Cannot create NetIbis driver " + clsName, e);
			}

			driverTable.put(name, driver);
		}

		return driver;
	}

	/**
	 * Extracts a driver stack from the properties, and create a new
	 * property set including this driver stack.
	 * @param sp the properties.
	 * @return the resulting properties.
	 */
	private StaticProperties extractDriverStack(StaticProperties sp) {
	    StaticProperties s = (StaticProperties) sp.clone();
	    String serialization = s.find("Serialization");
	    String path = "/";
	    if (serialization != null && ! serialization.equals("byte")) {
		if (serialization.equals("object")) {
		    serialization = "sun";
		}
		String top = "s_" + serialization;
		try {
		    s.add(path + ":Driver", top);
		} catch(IbisRuntimeException e) {
		    return sp;
		}
		path = path + top;
	    }
	    String driver = sp.find("IbisName");
	    if (driver == null) {
		StaticProperties p = properties();
		if (p != null) {
		    driver = p.find("IbisName");
		}
	    }
	    if (driver != null && driver.startsWith("net.")) {
		driver = driver.substring("net.".length());
		while (true) {
		    int dot = driver.indexOf('.');
		    int end = dot;
		    if (end == -1) {
			end = driver.length();
		    }
		    String top = driver.substring(0, end);
		    // System.err.println("Now register static property \"" + (path + ":Driver") + "\" as \"" + top + "\"");
		    try {
			s.add(path + ":Driver", top);
		    } catch(IbisRuntimeException e) {
			return sp;
		    }
		    if (dot == -1) {
			break;
		    }
		    if (path.equals("/")) {
			path = path + top;
		    } else {
			path = path + "/" + top;
		    }
		    driver = driver.substring(dot + 1);
		}
	    }
	    return s;
	}

	/**
	 * Creates a {@linkplain PortType port type} from a name and a set of {@linkplain StaticProperties properties}.
	 *
	 * @param name the name of the type.
	 * @param sp   the properties of the type.
	 * @return     The port type.
	 * @exception  IbisException if the name server refused to register the new type.
	 */
	synchronized
	protected PortType newPortType(String name, StaticProperties sp)
		throws IOException, IbisException {
		sp = extractDriverStack(sp);
		NetPortType newPortType = new NetPortType(this, name, sp);
		sp = newPortType.properties();

		if (nameServer.newPortType(name, sp)) {
			portTypeTable.put(name, newPortType);
		}

		return newPortType;
	}

	/**
	 * Returns the <I>name server</I> {@linkplain #nameServer client} {@linkplain Registry registry}.
	 *
	 * @return A reference to the instance's registry access.
	 */
	public Registry registry() {
		return nameServer;
	}
	
	public void sendDelete(IbisIdentifier ident) throws IOException {
		nameServer.delete(ident);
	}
	
	public void sendReconfigure() throws IOException {
		nameServer.reconfigure();
	}

	/**
	 * Returns the {@linkplain Ibis} instance {@link #identifier}.
	 *
	 * @return The instance's identifier
	 */
	public IbisIdentifier identifier() {
		return identifier;
	}

	/**
	 * Initializes the NetIbis instance.
	 *
	 * This function should be called before any attempt to use the NetIbis instance.
	 * <B>This function is not automatically called by the constructor</B>.
	 *
	 * @exception IbisConfigurationException if the system-wide Ibis properties where not correctly set.
	 * @exception IOException if the local host name cannot be found or if the <I>name server</I> cannot be reached.
	 */
	protected void init() throws IbisException, IOException {

                /* Builds the instance identifier out of our {@link InetAddress}. */
		InetAddress addr = InetAddress.getLocalHost();
		identifier = new NetIbisIdentifier(name, addr);

                /* Connects to the <I>name server<I> */
		nameServer = NameServer.loadNameServer(this);
	}

        /*
	 * Handles synchronous/asynchronous Ibises pool joins.
	 *
	 * @param joinIdent the identifier of the joining Ibis instance.
	 */
	/**
         * {@inheritDoc}
         */
	public void join(IbisIdentifier joinIdent) {
		synchronized (this) {
			joinedIbises.add(joinIdent);
			if(!open || resizeHandler == null) {
				return;
			}

			poolSize++;
		}

		if(resizeHandler != null) {
			resizeHandler.join(joinIdent);
			if (! i_joined && joinIdent.equals(identifier)) {
			    synchronized(this) {
				i_joined = true;
				notifyAll();
			    }
			}
		}
	}

	/*
	 * Handles synchronous/asynchronous Ibises pool leaves.
	 *
	 * @param leaveIdent the identifier of the leaving Ibis instance.
	 */
	/**
         * {@inheritDoc}
         */
	public void leave(IbisIdentifier leaveIdent) {
		synchronized (this) {
			if(!open && resizeHandler == null) {
				leftIbises.add(leaveIdent);
				return;
			}

			poolSize--;
		}

		if(resizeHandler != null) {
			resizeHandler.leave(leaveIdent);
		}
	}
	
	public void delete(IbisIdentifier deleteIdent) {
		synchronized (this) {
		    if (!open && resizeHandler != null) {
			toBeDeletedIbises.add(deleteIdent);
			return;
		    }
		    
		    if (resizeHandler != null) {
			resizeHandler.delete(deleteIdent);
		    }
		}
	}
	
	public void reconfigure() {
		if (resizeHandler != null) {
		    resizeHandler.reconfigure();
		}
	}

	public void openWorld() {
		if(resizeHandler != null) {
			while(joinedIbises.size() > 0) {
				NetIbisIdentifier id = (NetIbisIdentifier)joinedIbises.remove(0);
				resizeHandler.join(id);
				if (id.equals(identifier)) {
				    i_joined = true;
				}
				poolSize++;
			}

			while(leftIbises.size() > 0) {
				resizeHandler.leave((NetIbisIdentifier)leftIbises.remove(0));
				poolSize--;
			}
			
			while(toBeDeletedIbises.size() > 0) {
				resizeHandler.delete((NetIbisIdentifier)toBeDeletedIbises.remove(0));
			}
		}

		synchronized (this) {
			open = true;
			if (resizeHandler != null && ! i_joined) {
			    while (! i_joined) {
				try {
				    wait();
				} catch(Exception e) {
				}
			    }
			}
		}
	}

	public synchronized void closeWorld() {
		synchronized (this) {
			open = false;
		}
	}

	/**
	 * Returns the {@linkplain PortType port type} corresponding to the given type name.
	 *
	 * @param  name the name of the requested port type.
	 * @return A reference to the port type or <code>null</CODE> if the given name is not the name of a valid port type.
	 */
	synchronized
	public PortType getPortType(String name) {
		return (PortType) portTypeTable.get(name);
	}

	synchronized void register(NetSendPort p) {
	    p.next = sendPortList;
	    sendPortList = p;
	}

	synchronized void register(NetReceivePort p) {
	    p.next = receivePortList;
	    receivePortList = p;
	}

	synchronized void unregister(NetSendPort p) {
	    NetSendPort prev = null;
	    NetSendPort scan = sendPortList;
	    while (scan != null && scan != p) {
		prev = scan;
		scan = scan.next;
	    }
	    if (scan == null) {
		throw new Error("Unregister a NetSendPort that is not registered");
	    }
	    if (prev == null) {
		sendPortList = p.next;
	    } else {
		prev.next = p.next;
	    }
	}

	synchronized void unregister(NetReceivePort p) {
	    NetReceivePort prev = null;
	    NetReceivePort scan = receivePortList;
	    while (scan != null && scan != p) {
		prev = scan;
		scan = scan.next;
	    }
	    if (scan == null) {
		throw new Error("Unregister a NetReceivePort that is not registered");
	    }
	    if (prev == null) {
		receivePortList = p.next;
	    } else {
		prev.next = p.next;
	    }
	}

	/** Requests the NetIbis instance to leave the Name Server pool.
	 */
	public void end() throws IOException {
		synchronized (this) {
		    while (sendPortList != null) {
			// System.err.println("Ibis.end(): Invoke forcedClose() of " + sendPortList);
			sendPortList.close();
		    }
		    while (receivePortList != null) {
			// System.err.println("Ibis.end(): Invoke forcedClose() of " + receivePortList);
			receivePortList.forcedClose();
		    }
		}

		nameServer.leave();
		socketFactory.shutdown();
	}

	public void poll() throws IOException {
		__.unimplemented__("poll");
	}

        public int closedPoolRank() {
	    if (closedPoolRank == -1) {
		closedPoolRank = joinedIbises.indexOf(identifier());
	    }
	    if (closedPoolRank == -1) {
		throw new Error("closedPoolRank only defined in closed world");
	    }

	    return closedPoolRank;
        }

        public int closedPoolSize() {
	    if (closedPoolSize == 0) {
		throw new Error("closedPoolSize only defined in closed world");
	    } else {
                return closedPoolSize;
	    }
        }
}
