package ibis.ipl.impl.net;

import ibis.ipl.Ibis;
import ibis.ipl.IbisException;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.StaticProperties;
import ibis.ipl.PortType;
import ibis.ipl.Registry;

import ibis.ipl.impl.generic.IbisIdentifierTable;

import ibis.ipl.impl.nameServer.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Provides a generic {@link Ibis} implementation for pluggable network driver
 * support.
 */
public final class NetIbis extends Ibis {
	
	/**
	 * Selects the driver loading mode.
	 *
	 * <DL>
	 * <DT><CODE>true</CODE><DD>known drivers are statically loaded at initialization time.
	 * <DT><CODE>false</CODE><DD>every driver is loaded dynamically.
	 * </DL>
	 */

	private static final String compiler = java.lang.System.getProperty("java.lang.compiler");
	// private static final boolean staticDriverLoading = compiler != null && compiler.equals("manta");
	private static final boolean staticDriverLoading = false;

	/**
	 * Caches the previously created port types.
	 */
	private   Hashtable         portTypeTable    = new Hashtable();

	/**
	 * Caches the previously loaded drivers.
	 */
	private   Hashtable         driverTable      = new Hashtable();

	/**
	 * Provides information needed by other NetIbis instances to connect with this one.
	 */
	private   NetIbisIdentifier identifier       = null;

	/**
	 * Stores the Ibis Name Server host name.
	 */
	private   String 	    nameServerName   = null;

	/**
	 * Stores the Ibis Name Server pool name for the NetIbis instance.
	 */
	private   String      	    nameServerPool   = null;

	/**
	 * Stores the Ibis Name Server IP address.
	 */
	private   InetAddress 	    nameServerInet   = null;

	/**
	 * Stores the Ibis Name Server IP port.
	 */
	private   int               nameServerPort   = 0;

	/**
	 * Stores the NetIbis instance's Name Server client.
	 */
	protected NameServerClient  nameServerClient = null;

	/**
	 * Indicates whether our <I>world</I> is open or not.
	 */
	private   boolean           open             = false;

	/**
	 * Stores the number of NetIbis instances in this instance's pool.
	 */
	private   int 	       	    poolSize         = 0;

	/**
	 * Stores asynchronous Name Server pool join events.
	 */
	private   Vector 	    joinedIbises     = new Vector();

	/**
	 * Stores asynchronous Name Server pool leave events.
	 */
	private   Vector 	    leftIbises       = new Vector();

	/**
	 * The NetIbis bank.
	 */
	private   NetBank           bank             = new NetBank();

	static NetIbis globalIbis;
	IbisIdentifierTable identTable = new IbisIdentifierTable();

	/**
	 * Default constructor.
	 *
	 * Loads compile-time known drivers if {@link #staticDriverLoading} is set.
	 */
	public NetIbis() {
// System.err.println("NetIbis ...");
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
			    System.err.println("Cannot instantiate class " + drivers[i]);
			}
		    }
		}
// System.err.println("NetIbis created...");
	}

	/**
	 * Returns the NetIbis bank instance.
	 *
	 * @return The NetIbis bank.
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
	 * @exception NetIbisException if the requested driver class could not be loaded or the requested driver instance could not be initialized.
	 */
	public NetDriver getDriver(String name) throws NetIbisException{
		NetDriver driver = (NetDriver)driverTable.get(name);

		if (driver == null) {
			try {
				String      clsName  =
					getClass().getPackage().getName()
					+ "."
					+ name
					+ ".Driver";
				Class       cls      = Class.forName(clsName);
				Class []    clsArray = { getClass() };
				Constructor cons     = cls.getConstructor(clsArray);
				Object []   objArray = { this };

				driver = (NetDriver)cons.newInstance(objArray);
			} catch (Exception e) {
				throw new NetIbisException(e);
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
	 * @return The port type.
	 * @exception NetIbisException if the name server refused to register the new type.
	 */
	public PortType createPortType(String name, StaticProperties sp)
		throws NetIbisException {
		NetPortType newPortType = new NetPortType(this, name, sp);
		sp = newPortType.properties();

		PortTypeNameServerClient client = nameServerClient.tcpPortTypeNameServerClient;
                try {
                        if (client.newPortType(name, sp)) { 
                                portTypeTable.put(name, newPortType);
                        }
                } catch (ibis.ipl.IbisIOException e) {
                        throw new NetIbisException(e);
                }
                
		return newPortType;	
	}

	/**
	 * Returns the NetIbis instance's access to the name server {@linkplain Registry registry}.
	 *
	 * @return A reference to the instance's registry access.
	 */
	public Registry registry() {
		return nameServerClient.tcpRegistry;
	} 

	/**
	 * Returns the {@linkplain StaticProperties properties} of the NetIbis instance.
	 *
	 * @return <CODE>null</CODE>
	 */
	public StaticProperties properties() {
		return null;
	}

	/**
	 * Returns the {@linkplain IbisIdentifier identifier} of the NetIbis instance.
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
	 * This function is not automatically called by the constructor.
	 *
	 * @exception IbisException if the system-wide Ibis properties where not correctly set.
	 * @exception NetIbisException if the local host name cannot be found or if the Name Server cannot be reached.
	 */
	protected void init() throws IbisException, NetIbisException { 
		try {
			InetAddress addr = InetAddress.getLocalHost();
			identifier = new NetIbisIdentifier(name, addr);
		} catch (UnknownHostException e) {
			throw new NetIbisException(e);
		}
		

		{
			Properties p = System.getProperties();
		
			nameServerName = p.getProperty("name_server");
                        if (nameServerName == null) {
                                throw new IbisException("property name_server is not specified");
                        }

			nameServerPool = p.getProperty("name_server_pool");
                        if (nameServerPool == null) {
                                throw new IbisException("property name_server_pool is not specified");
                        }

                        String nameServerPortString = p.getProperty("name_server_port");

                        if (nameServerPortString == null) {
                                nameServerPort = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
                        } else {
                                try {
                                        nameServerPort = Integer.parseInt(nameServerPortString);
                                } catch (Exception e) {
                                        System.err.println("illegal nameserver port: " + nameServerPortString + ", using default");
                                        nameServerPort = NameServer.TCP_IBIS_NAME_SERVER_PORT_NR;
                                }
                        }
		}
		
		try {
			nameServerInet = InetAddress.getByName(nameServerName);
		} catch (UnknownHostException e) {
			throw new NetIbisException(e);
		}

                try {
                        nameServerClient = new NameServerClient(this,
                                                                identifier,
                                                                nameServerPool,
                                                                nameServerInet, 
                                                                nameServerPort);
                } catch (ibis.ipl.IbisIOException e) {
                        throw new NetIbisException(e);
                }
	}

	/**
	 * Returns the NetIbis instance's access to the name server receive-port name {@linkplain ReceivePortNameServerClient registry}.
	 *
	 * @return A reference to the instance's receive-port name registry access.
	 */
	public ReceivePortNameServerClient receivePortNameServerClient() {
		return nameServerClient.tcpReceivePortNameServerClient;
	}
	
	/**
	 * Handles synchronous/asynchronous Ibises pool joins.
	 *
	 * @param joinIdent the identifier of the joining Ibis instance.
	 */
	public void join(IbisIdentifier joinIdent) { 
                //System.err.println(this + ": join--> " + joinIdent);
		synchronized (this) {
			if(!open && resizeHandler != null) {
				joinedIbises.add(joinIdent);
                                // System.err.println(this + ": join<XX");
				return;
			}
			
			poolSize++;
		}

		if(resizeHandler != null) {
			resizeHandler.join(joinIdent);
		}
                //System.err.println(this + ": join<--");
	}

	/**
	 * Handles synchronous/asynchronous Ibises pool leaves.
	 *
	 * @param leaveIdent the identifier of the leaving Ibis instance.
	 */
	public void leave(IbisIdentifier leaveIdent) { 
                //System.err.println(this + ": leave-->");
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
                //System.err.println(this + ": leave<--");
	}

	public void openWorld() {
                //System.err.println(this + ": openWorld-->");
		if(resizeHandler != null) {
			while(joinedIbises.size() > 0) {
// System.err.println(this+ ": join/later " + (NetIbisIdentifier)joinedIbises.elementAt(0));
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
                //System.err.println(this + ": openWorld<--");
	}

	public synchronized void closeWorld() {
                //System.err.println("NetIbis: closeWorld-->");
		synchronized (this) {
			open = false;
		}
                //System.err.println("NetIbis: closeWorld<--");
	}

	/**
	 * Returns the {@linkplain PortType port type} corresponding to the given type name.
	 *
	 * @param  name the name of the requested port type.
	 * @return A reference to the port type or <code>null</CODE> if the given name is not the name of a valid port type.
	 */
	public PortType getPortType(String name) { 
		return (PortType) portTypeTable.get(name);
	} 

	/** Requests the NetIbis instance to leave the Name Server pool.
	 */
	public void end() {
                //System.err.println("NetIbis: end-->");
		try {
			nameServerClient.leave();
		} catch (Exception e) {
			__.fwdAbort__(e);
		}
                //System.err.println("NetIbis: end<--");
	}

	public void poll() {
		__.unimplemented__("poll");
	}
}
