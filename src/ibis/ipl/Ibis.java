package ibis.ipl;

/**
   This class defines the Ibis API, which can be implemented by an Ibis implementation.
   Every JVM may run multiple Ibis implementations.
   The user can request a list of available implementations, query their properties, and
   than load the desired Ibis implementation at runtime.
   An Ibis implementation offers certain PortType properties.

   On startup, Ibis tries to load properties files in the following order:
   - ibis.property.file;
   - current_dir/ibis_properties;
   - home_dir/ibis_properties.

**/

import java.util.Vector;
import java.util.Properties;
import ibis.util.Input;
import java.io.File;
import java.net.InetAddress;

public abstract class Ibis { 

	protected String name;
	protected String implName;
	protected ResizeHandler resizeHandler;
	protected static Vector implList; /* string vector */
	protected static Vector implProperties; /* StaticProperties vector */

	static {
		readGlobalProperties();
	}

	/** 
	    Creates a new Ibis instance. Instances must be given a unique name, which identifies the
	    instance. Lookups are done using this name. If the user tries to create two instances
	    with the same name, an IbisException will be thrown.
	    The resizeHandler will be invoked when Ibises join and leave, and may be null to indicate that
	    resize notifications are not wanted. **/
	public static Ibis createIbis(String name, String implName, ResizeHandler resizeHandler) throws IbisException {
		Ibis impl;

		try { 
			if (implName == null) { 
				throw new NullPointerException("Implementation name is null");
			} 
			
			if (name == null) { 
				throw new NullPointerException("Ibis name is null");
			} 
			
			Class c = Class.forName(implName);
			impl = (Ibis) c.newInstance();
			impl.name = name;
			impl.resizeHandler = resizeHandler;
			impl.init();
		} catch (Throwable t) { 			 
			throw new IbisException("Could not initialize Ibis", t);
		}
		
		return impl;
	}

	/** Create a new Ibis instance, based on the property ibis.name.
	    The currently recognized Ibis names are:
	    panda	Ibis built on top of Panda.
	    tcp		Ibis built on top of TCP (the current default).
	    mpi		Ibis built on top of MPI.
	    net.*	The future version, for tcp, udp, GM, ...
	*/
	public static Ibis createIbis(ResizeHandler r)
	    throws IbisException
	{
	    Properties p = System.getProperties();
	    String ibisname = p.getProperty("ibis.name");
	    String hostname;

	    try {
		hostname = InetAddress.getLocalHost().getHostName();
		InetAddress adres = InetAddress.getByName(hostname);

		adres = InetAddress.getByName(adres.getHostAddress());
		hostname = adres.getHostName();
	    } catch(Exception e) {
		hostname = "unknown";
	    }

	    if (ibisname == null) {
		// Create a default Ibis.
		ibisname = "tcp";
	    }

	    // Is this name unique enough?
	    String name = "ibis:" + hostname + "@" + System.currentTimeMillis();

	    if (ibisname.equals("panda")) {
		return createIbis(name, "ibis.ipl.impl.messagePassing.PandaIbis", r);
	    } else if (ibisname.equals("mpi")) {
		return createIbis(name, "ibis.ipl.impl.messagePassing.MPIIbis", r);
	    } else if (ibisname.startsWith("net.")) {
		return createIbis(name, "ibis.ipl.impl.net.NetIbis", r);
	    } else if (ibisname.startsWith("net")) {
		return createIbis(name, "ibis.ipl.impl.net.NetIbis", r);
	    } else {
		// The default: tcp.
		if (! ibisname.equals("tcp")) {
		    System.err.println("Warning: name '" + ibisname +
				       "' not recognized, using TCP version");
		}
		return createIbis(name, "ibis.ipl.impl.tcp.TcpIbis", r);
	    }
	}

	private static String readString(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && !Character.isWhitespace(in.nextChar())) {
			char c = in.readChar();
			s.append(c);
		}

		return s.toString();
	}

	private static String readKey(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln() && in.nextChar() != '=') {
			s.append(in.readChar());
		}

		return s.toString();
	}

	private static String readVal(Input in) {
		StringBuffer s = new StringBuffer();
		while (!in.eof() && !in.eoln()) {
			s.append(in.readChar());
		}

		return s.toString();
	}

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

		implList.add(name);
		implProperties.add(sp);
	}

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

	private static void readGlobalProperties() {
		Input in = openProperties();

		implList = new Vector();
		implProperties = new Vector();

		while(!in.eof()) {
			readImpl(in);
		}
	}

	// order: ibis.property.file, current_dir/ibis_properties, 
	// home_dir/ibis_properties, install_dir/ibis_properties
	private static Input openProperties() {
		Input in = null;
		Properties p = System.getProperties();
		String s = p.getProperty("ibis.property.file");
		if(s != null) {
			try {
				in = new Input(s);
				return in;
			} catch (Exception e) {
				System.err.println("ibis.property.file set, but could not read file");
			}
		}

		String sep = p.getProperty("file.separator");
		if(sep == null) {
			System.err.println("Could not get file separator property");
			System.exit(1);
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
		s = ibis.ipl.InstallConfiguration.path;
		s += sep + "properties";
		try {
			in = new Input(s);
			return in;
		} catch (Exception e) {
			System.err.println("Fail to open " + s);
			// Ignore.
		}

		System.err.println("Could not find property file");
		System.exit(1);
		return null;
	}

	/** Return a list of available Ibis implementation names for this system. **/
	public static String[] list() {
		String[] res = new String[implList.size()];
		for(int i=0; i<res.length; i++) {
			res[i] = (String) implList.get(i);
		}

		return res;
	}

	/** Return the static properties for a certain implementation. **/
	public static StaticProperties staticProperties(String implName) {
		int index = implList.indexOf(implName);
		return (StaticProperties) implProperties.get(index);
	}

	/** After openWorld, join and leave calls may be received. **/
	public abstract void openWorld();

	/** After closeWorld, no join or leave calls are received. **/
	public abstract void closeWorld();

	/** Returns all Ibis recources to the system. **/
	public abstract void end();

	/** A PortType can be created using this method.
	    A name is given to the PortType (e.g. "satin porttype" or "RMI porttype"), and
	    Port properties are specified (for example ports are "totally-ordered" and 
	    "reliable" and support "NWS"). The name and properties <strong>together</strong>
	    define the PortType.
	    If two Ibis implementations want to communicate, they must both create a PortType
	    with the same name and properties.
	    If multiple implementations try to create a PortType with the same name but
	    different properties, an IbisException will be thrown.
	    PortTypes can be used to create ReceivePorts and SendPorts.
	    Only ReceivePorts and Sendports of the same PortType can communicate.
	    Any number of ReceivePorts and Sendports can be created on a JVM
	    (even of the same PortType). **/
	public abstract PortType createPortType(String name, StaticProperties p) throws IbisException, IbisIOException;
	public abstract PortType getPortType(String name) throws IbisException;

	/** Returns the Ibis Registry. **/
	public abstract Registry registry();

	/** Returns the properties of the underlying Ibis implementation. **/
	public StaticProperties properties() {
		return staticProperties(implName);
	}

	/** Polls the network for new messages. An upcall may be generated by the poll.
	    There is one poll for the entire Ibis, as this can be implemented more efficiently than
	    polling per port. Polling per port is also provided in the receiveport itself. **/
	public abstract void poll() throws IbisIOException;
	
	/** Returns the user-specified name of this Ibis instance. **/
	public String name() { 
		return name;
	} 

	public abstract IbisIdentifier identifier();


	/** returns null when the implementation could not be loaded **/
	public static Timer newTimer(String impl) {
		try {
			Class c = Class.forName(impl);
			return (Timer) c.newInstance();
		} catch (Throwable t) {
			return null;
		}
	}

	protected abstract void init() throws IbisException, IbisIOException;

	/* Used by the nameserver, do not call from outside Ibis */
	public abstract void join(IbisIdentifier joinIdent);
	public abstract void leave(IbisIdentifier leaveIdent);

} 
