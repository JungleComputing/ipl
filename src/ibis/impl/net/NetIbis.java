package ibis.impl.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.PortType;
import ibis.ipl.Registry;
import ibis.ipl.IbisConfigurationException;

import ibis.util.IbisIdentifierTable;

import ibis.impl.nameServer.NameServer;

import java.io.IOException;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import java.lang.reflect.Constructor;

/**
 * Provides a generic {@link Ibis} implementation for pluggable network driver
 * support.
 */
public final class NetIbis extends Ibis {

	/**
         * The compiler name.
         */
	private static final String COMPILER = java.lang.System.getProperty("java.lang.compiler");

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

	/**
	 * This {@link NetIbis} instance <I>name server</I> pool name.
	 */
	private   String      	    nameServerPool   = null;

	/**
	 * The <I>name server</I> host name.
	 */
	private   String 	    nameServerName   = null;

	/**
	 * The <I>name server</I> IP address.
	 */
	private   InetAddress 	    nameServerInet   = null;

	/**
	 * The <I>name server</I> IP port.
	 */
	private   int               nameServerPort   = 0;

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
	 * The number of {@link NetIbis} instances in our <I>name server</I> {@linkplain #nameServerPool pool}.
	 */
	private   int 	       	    poolSize         = 0;

	/**
	 * The {@link NetIbis} instances that attempted to join our {@linkplain #nameServerPool pool} while our world was {@linkplain #open closed}.
	 */
	private   Vector 	    joinedIbises     = new Vector();

	/**
	 * The {@link NetIbis} instances that attempted to leave our {@linkplain #nameServerPool pool} while our world was {@linkplain #open closed}.
	 */
	private   Vector 	    leftIbises       = new Vector();

	/**
	 * The {@link NetIbis} {@linkplain NetBank bank}.
         *
         * This {@linkplain NetBank bank} can be used as general
         * purpose and relatively safe repository for global object
         * instances.
         */
	private   NetBank           bank             = new NetBank();

        private   int               closedPoolRank   = -1;
        private   int               closedPoolSize   =  0;

        /**
         * The master {@link Ibis} instance for this process.
         */
	static volatile NetIbis globalIbis;

        /**
         * The {@link Ibis Ibises} instance identifiers.
         */
	IbisIdentifierTable identTable = new IbisIdentifierTable();

	/**
	 * Default constructor.
	 *
	 * Loads compile-time known drivers if {@link #staticDriverLoading} is set.
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
			    Class clazz = Class.forName("ibis.impl.net." + drivers[i] + ".Driver");
			    NetDriver d = (NetDriver)clazz.newInstance();
			    d.setIbis(this);
			} catch (java.lang.Exception e) {
			    throw new IbisConfigurationException("Cannot instantiate class " + drivers[i], e);
			}
		    }
		}

                Properties p = System.getProperties();

                String pool_total_hosts = p.getProperty("ibis.pool.total_hosts");
                if (pool_total_hosts != null) {
                        closedPoolSize = Integer.parseInt(pool_total_hosts);
                }

                String pool_host_number = p.getProperty("ibis.pool.host_number");
                if (pool_total_hosts != null) {
                        closedPoolRank = Integer.parseInt(pool_host_number);
                }


	}

	/**
	 * Returns the {@link #bank}.
	 *
	 * @return The {@link #bank}.
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
	 * Creates a {@linkplain PortType port type} from a name and a set of {@linkplain StaticProperties properties}.
	 *
	 * @param name the name of the type.
	 * @param sp   the properties of the type.
	 * @return     The port type.
	 * @exception  IbisException if the name server refused to register the new type.
	 */
	synchronized
	public PortType createPortType(String name, StaticProperties sp)
		throws IOException, IbisException {
		NetPortType newPortType = new NetPortType(this, name, sp);
		sp = newPortType.properties();

		if (nameServer.newPortType(name, sp)) {
			portTypeTable.put(name, newPortType);
		}

		return newPortType;
	}

	/**
	 * Returns the <I>name server</I> {@linkplain #nameServerClient client} {@linkplain Registry registry}.
	 *
	 * @return A reference to the instance's registry access.
	 */
	public Registry registry() {
		return nameServer;
	}

	/**
	 * Returns the {@linkplain StaticProperties properties} of the {@link NetIbis} instance.
	 * Note: currently unimplemented.
	 * @return <CODE>null</CODE>
	 */
	public StaticProperties properties() {
		return null;
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
			if(!open && resizeHandler != null) {
				joinedIbises.add(joinIdent);
				return;
			}

			poolSize++;
		}

		if(resizeHandler != null) {
			resizeHandler.join(joinIdent);
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
			if(!open && resizeHandler != null) {
				leftIbises.add(leaveIdent);
				return;
			}

			poolSize--;
		}

		if(resizeHandler != null) {
			resizeHandler.leave(leaveIdent);
		}
	}

	public void openWorld() {
		if(resizeHandler != null) {
			while(joinedIbises.size() > 0) {
				resizeHandler.join((NetIbisIdentifier)joinedIbises.remove(0));
				poolSize++;
			}

			while(leftIbises.size() > 0) {
				resizeHandler.leave((NetIbisIdentifier)leftIbises.remove(0));
				poolSize--;
			}
		}

		synchronized (this) {
			open = true;
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

	/** Requests the NetIbis instance to leave the Name Server pool.
	 */
	public void end() throws IOException {
		nameServer.leave();
	}

	public ibis.ipl.ReadMessage poll() throws IOException {
		__.unimplemented__("poll");
		return null;
	}

        protected int _closedPoolRank() {
                return closedPoolRank;
        }

        protected int _closedPoolSize() {
                return closedPoolSize;
        }
}
