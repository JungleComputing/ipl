package ibis.ipl;

import ibis.util.Input;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Properties;

/**
 * This class defines the Ibis API, which can be implemented by an Ibis
 * implementation. Every JVM may run multiple Ibis implementations.
 * The user can request a list of available implementations, query their
 * properties, and then load the desired Ibis implementation at runtime.
 * An Ibis implementation offers certain PortType properties.
 *
 * On startup, Ibis tries to load properties files in the following order:
 * - ibis.property.file;
 * - current_dir/ibis_properties;
 * - home_dir/ibis_properties.
 *
 */

public abstract class Ibis { 

	/**
	 * A user-defined (or system-invented) name for this Ibis.
	 */
	protected String name;

	/**
	 * The implementation name, for instance ibis.tcp.TcpIbis.
	 */
	protected String implName;

	/**
	 * A user-supplied resize handler, with join/leave upcalls.
	 */
	protected ResizeHandler resizeHandler;

	/**
	 * A list of available ibis implementations.
	 */
	private static ArrayList implList;

	/**
	 * Properties of available ibis implementations.
	 */
	private static ArrayList implProperties; /* StaticProperties list */

	/**
	 * The currently loaded Ibises.
	 */
	private static ArrayList loadedIbises = new ArrayList();


	static {
	    try {
		readGlobalProperties();
	    } catch(IOException e) {
		System.err.println("exception while trying to read properties: " + e);
		e.printStackTrace();
		System.exit(1);
	    }
	}


	/** 
	    Loads a native library with ibis.
	    It might not be possible to load libraries the normal way,
	    because Ibis applications might override the bootclasspath
	    when the classlibraries have been rewritten.
	    In that case, the classloader will use the sun.boot.library.path 
	    which is not portable.

	    @param name the name of the library to be loaded.
	    @exception SecurityException may be thrown by loadLibrary.
	    @exception UnsatisfiedLinkError may be thrown by loadLibrary.
	**/
	public static void loadLibrary(String name) throws SecurityException, UnsatisfiedLinkError {
		Properties p = System.getProperties();
		String libPath = p.getProperty("ibis.library.path");

		if(libPath != null) {
			String s = System.mapLibraryName(name);

//			System.err.println("LOADING IBIS LIB: " + libPath + "/" + s);

			System.load(libPath + "/" + s);
			return;
		} 

		// Fall back to regular loading.
		// This might not work, or it might not :-)
//		System.err.println("LOADING NON IBIS LIB: " + name);
		System.loadLibrary(name);
	}

	/** 
	 * Creates a new Ibis instance. Instances must be given a unique name,
	 * which identifies the instance. Lookups are done using this name. If
	 * the user tries to create two instances with the same name, an
	 * IbisException will be thrown.
	 *
	 * @param name a unique name, identifying this Ibis instance.
	 * @param implName the name of the implementation.
	 * @param resizeHandler will be invoked when Ibises join and leave, and
	 * may be null to indicate that resize notifications are not wanted.
	 * @return the new Ibis instance.
	 *
	 * @exception ibis.ipl.IbisException two Ibis instances with the same
	 * 	implName are created, or any IbisException the implementation
	 * 	throws at its initialization
	 * @exception IllegalArgumentException name or implName are null, or
	 * 	do not correspond to an existing Ibis implementation
	 *  @exception ConnectionRefusedException is thrown when the name turns out to
	 *   be not unique.
	 */
	public static Ibis createIbis(String name, String implName, ResizeHandler resizeHandler)
			throws IbisException, ConnectionRefusedException {
		Ibis impl;

		try {
			loadLibrary("conversion");
		} catch (Throwable t) {
			System.err.println("WARNING: Could not load conversion library");
			System.err.println("This might be a problem if you did not rewrite the class libraries");
			System.err.println("If you did, or if you don't use Ibis serialization, you can safely ignore this warning");
		}

		if (implName == null) { 
			throw new IllegalArgumentException("Implementation name is null");
		} 
		
		if (name == null) { 
			throw new IllegalArgumentException("Ibis name is null");
		} 

		Class c;
		try { 
			c = Class.forName(implName);
		} catch (ClassNotFoundException t) { 			 
			throw new IllegalArgumentException("Could not initialize Ibis" + t);
		}

		try {
			impl = (Ibis) c.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Could not initialize Ibis" + e);
		} catch (IllegalAccessException e2) {
			throw new IllegalArgumentException("Could not initialize Ibis" + e2);
		}
		impl.name = name;
		impl.resizeHandler = resizeHandler;

		try {
			impl.init();
		} catch(ConnectionRefusedException e) {
			throw e;
		} catch (IOException e3) {
			throw new IbisException("Could not initialize Ibis", e3);
		}

//System.err.println("Create Ibis " + impl);

		synchronized(Ibis.class) {
			loadedIbises.add(impl);
		}

		return impl;
	}

	/** Returns a list of all Ibis implementations that are currently loaded.
	    When no Ibises are loaded, this method returns null

	    @return the list of loaded Ibis implementations.
	**/
	public static synchronized Ibis[] loadedIbises() {
		if(loadedIbises == null || loadedIbises.size() == 0) {
			return null;
		}

		Ibis[] res = new Ibis[loadedIbises.size()];
		for(int i=0; i<res.length; i++) {
			res[i] = (Ibis) loadedIbises.get(i);
		}

		return res;
	}


	/**
	 * Creates a new Ibis instance, based on the property ibis.name.
	 * The currently recognized Ibis names are:
	 * <br>
	 * panda	Ibis built on top of Panda.
	 * <br>
	 * tcp		Ibis built on top of TCP (the current default).
	 * <br>
	 * mpi		Ibis built on top of MPI.
	 * <br>
	 * net.*	The future version, for tcp, udp, GM, ...
	 * <br>
	 *
	 * @param  r a {@link ibis.ipl.ResizeHandler ResizeHandler} instance if upcalls
	 *   for joining or leaving ibis instances are required, or <code>null</code>.
	 * @return the new Ibis instance.
	 *
	 * @exception ConnectionRefusedException is thrown when the name turns out to
	 *  be not unique.
	 */
	public static Ibis createIbis(ResizeHandler r)
	    throws IbisException, ConnectionRefusedException
	{
	    Properties p = System.getProperties();
	    String hostname;

	    // @@@ start of horrible code
//	    System.err.println("AARG! This code completely violates the whole Ibis philosophy!!!! please fix me! --Rob & Jason");
//	    new Exception().printStackTrace();
//	    But HOW?   -- Ceriel

	    try {
		hostname = InetAddress.getLocalHost().getHostName();
		InetAddress adres = InetAddress.getByName(hostname);

		adres = InetAddress.getByName(adres.getHostAddress());
		hostname = adres.getHostName();
	    } catch(Exception e) {
		hostname = "unknown";
	    }

	    String name = null;
	    while(true) {
		    try {
			    name = "ibis@" + hostname + "_" + System.currentTimeMillis();
			    
			    String ibisname = p.getProperty("ibis.name");
			    
			    if (ibisname == null) {
				    // Create a default Ibis.
				    ibisname = "tcp";
			    }

			    if (ibisname.equals("panda")) {
				    return createIbis(name, "ibis.impl.messagePassing.PandaIbis", r);
			    } else if (ibisname.equals("mpi")) {
				    return createIbis(name, "ibis.impl.messagePassing.MPIIbis", r);
			    } else if (ibisname.startsWith("net")) {
				    return createIbis(name, "ibis.impl.net.NetIbis", r);
			    } else {
				    // The default: tcp.
				    if (! ibisname.equals("tcp")) {
					    System.err.println("Warning: name '" + ibisname +
							       "' not recognized, using TCP version");
				    }
				    return createIbis(name, "ibis.impl.tcp.TcpIbis", r);
			    }
			    // @@@ end of horrible code
		    } catch (ConnectionRefusedException e) {
			    // retry
		    }
	    }
	}

	/**
	 * Reads a string from the input.
	 * The string is delimited by end-of-file, end-of-line, or whitespace.
	 * @param in the input from which the string is to be read.
	 * @return the string.
	 */
	private static String readString(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && !Character.isWhitespace(in.nextChar())) {
			char c = in.readChar();
			s.append(c);
		}

		return s.toString();
	}

	/**
	 * Reads a string that is to be used as property key from the input.
	 * The string is delimited by end-of-file, end-of-line, or an '=' sign.
	 * @param in the input from which the string is to be read.
	 * @return the string.
	 */
	private static String readKey(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && in.nextChar() != '=') {
			s.append(in.readChar());
		}

		return s.toString();
	}

	/**
	 * Reads a string that is to be used as property value from the input.
	 * The string is delimited by end-of-file or end-of-line.
	 * @param in the input from which the string is to be read.
	 * @return the string.
	 */
	private static String readVal(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln()) {
			s.append(in.readChar());
		}

		return s.toString();
	}

	/**
	 * Reads an implementation name + its properties from the input.
	 * @param in	from which the input is read.
	 */
	private static void readImpl(Input in) {
		String name = readString(in);
		in.readln();

		StaticProperties sp = new StaticProperties();

		while(!in.eof() && !in.eoln()) {
			String file = readString(in);
			String fileName = null;
			in.readln();
			Input in2 = null;
			
			// file is relative to properties file.
			try {
				Properties p = System.getProperties();
				String sep = p.getProperty("file.separator");
				if(sep == null) {
					System.err.println("Could not get file separator property");
					System.exit(1);
				}
				int index = in.name().lastIndexOf(sep) + 1;
				String path = in.name().substring(0, index);
				fileName = path + file;
			} catch (Exception e) {
				System.out.println("could not open " + in.name());
			}

			try {
				in2 = new Input(fileName);
			} catch (Exception e) {
				System.err.println("Could not open file: " + file);
				System.exit(1);
			}

			readProperties(in2, sp);
		}
		if(!in.eof()) in.readln();

		synchronized(Ibis.class) {
			implList.add(name);
			implProperties.add(sp);
		}
	}

	/**
	 * Reads the static properties of an ibis.
	 * @param in where to read the properties from.
	 * @param sp where to store the properties.
	 */
	private static void readProperties(Input in, StaticProperties sp) {
		while(!in.eof()) {
			String key = readKey(in);
			in.readChar();
			in.skipWhiteSpace();
			String val = readVal(in);
			in.readln();

			try {
				sp.add(key, val);
			} catch (Exception e) {
				System.err.println("error adding property (" + key + "," + val + ")");
				System.exit(1);
			}
		}
	}

	/**
	 * Reads the properties of the ibis implementations available on the current machine.
	 * @exception IOException is thrown when a property file could not be opened.
	 */
	private static void readGlobalProperties() throws IOException {
		Input in = openProperties();

		implList = new ArrayList();
		implProperties = new ArrayList();

		while(!in.eof()) {
			readImpl(in);
		}
	}

	/**
	 * Tries to find and open a property file.
	 * The file is searched for as described below:
	 * <br>
	 * First, the system property ibis.property.file is tried.
	 * <br>
	 * Next, current_dir/ibis_properties is tried, where current_dir indicates
	 * the value of the system property user.dir.
	 * <br>
	 * Next, home_dir/ibis_properties is tried, where home_dir indicates
	 * the value of the system property user.home.
	 * <br>
	 * If any of this fails, a message is printed, and an exception is thrown.
	 * @return descriptor from which properties can be read.
	 * @exception IOException is thrown when a property file could not be opened.
	 */
	private static Input openProperties() throws IOException {
		Input in = null;
		Properties p = System.getProperties();
		String s = p.getProperty("ibis.property.file");
		if(s != null) {
			try {
				in = new Input(s);
				return in;
			} catch (Exception e) {
				System.err.println("ibis.property.file set, but could not read file " + s);
			}
		}

		String sep = p.getProperty("file.separator");
		if(sep == null) {
			throw new IOException("Could not get file separator property");
		}

		/* try current dir */
		s = p.getProperty("user.dir");
		if(s != null) {
			s += sep + "ibis_properties";
			try {
				in = new Input(s);
				return in;
			} catch (Exception e) {
				// Ignore.
			}
		}

		/* try users home dir */
		s = p.getProperty("user.home");
		if(s != null) {
			s += sep + "ibis_properties";
			try {
				in = new Input(s);
				return in;
			} catch (Exception e) {
				// Ignore.
			}
		}

		/* try install dir */
/*              // This is a bug. It makes it impossible to just copy a IPL.jar to another location and use it. --Rob
		s = ibis.ipl.InstallConfiguration.path;
		s += sep + "properties";
		try {
			in = new Input(s);
			return in;
		} catch (Exception e) {
			System.err.println("Fail to open " + s);
			// Ignore.
		}
*/
		throw new IOException("Could not find property file");
	}

	/**
	 * Returns a list of available Ibis implementation names for this system.
	 * @return the list of available Ibis implementations.
	 */
	public static synchronized String[] list() {
		String[] res = new String[implList.size()];
		for(int i=0; i<res.length; i++) {
			res[i] = (String) implList.get(i);
		}

		return res;
	}

	/** Returns the static properties for a certain implementation.
	 * @param implName implementation name of an Ibis for which properties are requested.
	 * @return the static properties for a given implementation, or <code>null</code>
	 * if not present.
	 */
	public static synchronized StaticProperties staticProperties(String implName) {
		int index = implList.indexOf(implName);
		if (index < 0) return null;
		return (StaticProperties) implProperties.get(index);
	}

	/**
	 * Allows for join and leave calls to be received.
	 */
	public abstract void openWorld();

	/**
	 * Disables reception of join/leave calls.
	 */
	public abstract void closeWorld();

	/**
	 * Returns all Ibis recources to the system.
	 */
	public abstract void end() throws IOException;

	/** Creates a {@link ibis.ipl.PortType PortType}.
	    A name is given to the <code>PortType</code> (e.g. "satin porttype" or "RMI porttype"), and
	    Port properties are specified (for example ports are "totally-ordered" and 
	    "reliable" and support "NWS"). The name and properties <strong>together</strong>
	    define the <code>PortType</code>.
	    If two Ibis implementations want to communicate, they must both create a <code>PortType</code>
	    with the same name and properties.
	    If multiple implementations try to create a <code>PortType</code> with the same name but
	    different properties, an IbisException will be thrown.
	    A <code>PortType</code> can be used to create {@link ibis.ipl.ReceivePort ReceivePorts}
	    and {@link ibis.ipl.SendPort SendPorts}.
	    Only <code>ReceivePort</code>s and <code>SendPort</code>s of the same <code>PortType</code> can communicate.
	    Any number of <code>ReceivePort</code>s and <code>SendPort</code>s can be created on a JVM
	    (even of the same <code>PortType</code>).
	    @param name name of the porttype.
	    @param p properties of the porttype.
	    @return the porttype.
	    @exception IbisException is thrown when Ibis configuration, name or p are misconfigured
	    @exception IOException may be thrown for instance when communication
	    with a nameserver fails.
	 **/
	public abstract PortType createPortType(String name, StaticProperties p) throws IOException, IbisException;

	/**
	 * Returns the {@link ibis.ipl.PortType PortType} corresponding to the given name.
	 *
	 * @param name the name of the requested port type.
	 * @return a reference to the port type, or <code>null</code> if the given name is not the name of a valid port type.
	 */
	public abstract PortType getPortType(String name);

	/** 
	 * Returns the Ibis {@linkplain ibis.ipl.Registry Registry}.
	 * @return the Ibis registry.
	 */
	public abstract Registry registry();

	/**
	 * Returns the properties of this Ibis implementation.
	 * @return the properties of this Ibis implementation.
	 */
	public StaticProperties properties() {
		return staticProperties(implName);
	}

	/**
	 * Polls the network for new messages.
	 * An upcall may be generated by the poll. 
	 * There is one poll for the entire Ibis, as this
	 * can sometimes be implemented more efficiently than polling per
	 * port. Polling per port is provided in the receiveport itself.
	 * @exception IOException is thrown when a communication error occurs.
	 */
	public abstract void poll() throws IOException;
	
	/**
	 * Returns the user-specified name of this Ibis instance.
	 * @return the name of this Ibis instance.
	 */
	public String name() { 
		return name;
	} 

	/**
	 * Returns an Ibis {@linkplain ibis.ipl.IbisIdentifier identifier} for
	 * this Ibis instance.
	 * An Ibis identifier identifies an Ibis instance in the network.
	 * @return the Ibis identifier of this Ibis instance.
	 */
	public abstract IbisIdentifier identifier();

	/**
	 * Ibis-implementation-specific initialization.
	 */
	protected abstract void init() throws IbisException, IOException;

	/**
	 * Notifies this Ibis instance that another Ibis instance has
	 * joined the run.
	 * <B>Note: used by the nameserver, do not call from outside Ibis.</B>
	 * @param joinIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier identifier}
	 * of the Ibis instance joining the run.
	 */
	public abstract void join(IbisIdentifier joinIdent);

	/**
	 * Notifies this Ibis instance that another Ibis instance has
	 * left the run.
	 * <B>Note: used by the nameserver, do not call from outside Ibis.</B>
	 * @param leaveIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier identifier}
	 * of the Ibis instance leaving the run.
	 */
	public abstract void leave(IbisIdentifier leaveIdent);
} 
