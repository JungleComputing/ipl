package ibis.rmi;

import ibis.ipl.Ibis;
import ibis.ipl.IbisIdentifier;
import ibis.ipl.PortType;
import ibis.ipl.ReceivePort;
import ibis.ipl.ReceivePortIdentifier;
import ibis.ipl.SendPort;
import ibis.ipl.IbisException;
import ibis.ipl.WriteMessage;
import ibis.ipl.ReadMessage;
import ibis.ipl.StaticProperties;

import ibis.rmi.server.Stub;
import ibis.rmi.server.RemoteStub;
import ibis.rmi.server.Skeleton;
import ibis.rmi.server.ExportException;

import java.util.Properties;
import java.util.HashMap;
import java.util.Vector;

import java.net.InetAddress;

import java.io.IOException;

public final class RTS {

    public final static boolean DEBUG = false;

    //keys - impl objects, values - skeletons for those objects
    private static HashMap skeletons;
    private static HashMap stubs;
    private static HashMap sendports;

    private static Vector receiveports;

    protected static String hostname;
    protected static PortType portType;

    private static Ibis ibis;
    private static IbisIdentifier localID;
    private static ibis.ipl.Registry ibisRegistry;

    private static ThreadLocal clientHost;

    static {
	try {
	    skeletons = new HashMap();
	    stubs = new HashMap();
	    sendports = new HashMap();
	    receiveports = new Vector();

	    hostname = InetAddress.getLocalHost().getHostName();
	    InetAddress adres = InetAddress.getByName(hostname);
	    adres = InetAddress.getByName(adres.getHostAddress());
	    hostname = adres.getHostName();

	    if (DEBUG) {
		System.out.println(hostname + ": init RMI RTS");
		System.out.println(hostname + ": creating ibis");
	    }

	    // @@@ start of horrible code
	    //			System.err.println("AARG! This code completely violates the whole Ibis philosophy!!!! please fix me! --Rob & Jason");
	    //			new Exception().printStackTrace();
	    // But HOW??? --Ceriel

	    ibis = Ibis.createIbis(null);

	    Properties p = System.getProperties();
	    String ibis_name = p.getProperty("ibis.name");

	    StaticProperties s = new StaticProperties();
	    String ibis_serialization = p.getProperty("ibis.serialization");
	    if (ibis_serialization != null) {
		System.out.println("Setting Serialization to " + ibis_serialization);
		s.add("Serialization", ibis_serialization);
	    } else {
		System.out.println("Setting Serialization to ibis");
		s.add("Serialization", "ibis");
	    }

	    if (ibis_name != null && ibis_name.startsWith("net.")) {
		//				System.err.println("AARG! This code completely violates the whole Ibis philosophy!!!! please fix me! --Rob & Jason");
		//				new Exception().printStackTrace();
		// But HOW??? --Ceriel
		String driver = ibis_name.substring("net.".length());
		String path = "/";
		if (ibis_serialization != null && ! ibis_serialization.equals("none")) {
		    String top = "s_" + ibis_serialization;
		    // System.err.println("Now register static property \"" + (path + ":Driver") + "\" as \"" + top + "\"");
		    s.add(path + ":Driver", top);
		    path = path + top;
		}
		while (true) {
		    int dot = driver.indexOf('.');
		    int end = dot;
		    if (end == -1) {
			end = driver.length();
		    }
		    String top = driver.substring(0, end);
		    // System.err.println("Now register static property \"" + (path + ":Driver") + "\" as \"" + top + "\"");
		    s.add(path + ":Driver", top);
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

	    // @@@ end of horrible code

	    if (DEBUG) {
		System.out.println(hostname + ": ibis created");
	    }

	    localID      = ibis.identifier();
	    ibisRegistry = ibis.registry();

	    portType = ibis.createPortType("RMI", s);

	    clientHost = new ThreadLocal();

	    if(DEBUG) {
		System.out.println(hostname + ": RMI RTS init done");
	    }

	} catch (Exception e) {
	    System.err.println(hostname + ": Could not init RMI RTS " + e);
	    e.printStackTrace();
	    System.exit(1);
	}

	/****
	 * This is only supported in SDK 1.4 and upwards. Comment out
	 * if you run an older SDK.
	 */
	Runtime.getRuntime().addShutdownHook(new Thread() {
	    public void run() {
		try {
		    ibis.end();
		    // System.err.println("Ended Ibis");
		} catch (IOException e) {
		    System.err.println("ibis.end throws " + e);
		}
	    }
	});
	/* End of 1.4-specific code */
    }

    private static String get_skel_name(Class c) {
	String class_name = c.getName();
	Package pkg = c.getPackage();
	String package_name = pkg != null ? pkg.getName() : null;
	if (package_name == null || package_name.equals("")) {
	    return "rmi_skeleton_" + class_name;
	}
	return package_name + ".rmi_skeleton_" + 
	    class_name.substring(class_name.lastIndexOf('.') + 1);
    }

    private static String get_stub_name(Class c) {
	String class_name = c.getName();
	Package pkg = c.getPackage();
	String package_name = pkg != null ? pkg.getName() : null;
	if (package_name == null || package_name.equals("")) {
	    return "rmi_stub_" + class_name;
	}
	return package_name + ".rmi_stub_" + 
	    class_name.substring(class_name.lastIndexOf('.') + 1);
    }

    private static Skeleton createSkel(Remote obj) throws IOException {
	try {
	    Skeleton skel;
	    Class c = obj.getClass();
	    ReceivePort rec;

	    String skel_name = get_skel_name(c);
	    // System.out.println("skel_name = " + skel_name);

	    // Use the classloader of the original class!
	    // Fix is by Fabrice Huet.
	    ClassLoader loader = c.getClassLoader();

	    Class skel_c = null;
	    if (loader != null) {
		skel_c = loader.loadClass(skel_name);
	    }
	    else {
		skel_c = Class.forName(skel_name);
	    }
	    skel = (Skeleton) skel_c.newInstance();

	    String skel_rec_port_name = "//" + hostname + "/rmi_skeleton" + (new java.rmi.server.UID()).toString();
	    rec = portType.createReceivePort(skel_rec_port_name, skel, skel);

	    skel.init(rec, obj);

	    rec.enableConnections();
	    rec.enableUpcalls();

	    skeletons.put(obj, skel);

	    return skel;
	} catch (ClassNotFoundException ec) {
	    throw new RemoteException("Cannot create skeleton", ec);
	} catch (InstantiationException en) {
	    throw new RemoteException("Cannot create skeleton", en);
	} catch (IllegalAccessException el) {
	    throw new RemoteException("Cannot create skeleton", el);
	}
    }

    public static void removeSkeleton(Skeleton skel) {
System.out.println("removeSkeleton called!");
	skeletons.remove(skel.destination);
	stubs.remove(skel.destination);
    }

    public static RemoteStub exportObject(Remote obj)
	throws Exception
    {
	Stub stub;
	Class c = obj.getClass();
	Skeleton skel;
	ReceivePort rec;
	String classname = c.getName();

	String class_name = classname.substring(classname.lastIndexOf('.') + 1);
	synchronized(RTS.class) {
	    skel = (Skeleton) skeletons.get(obj);
	}
	if (skel == null) {
	    //create a skeleton
	    skel = createSkel(obj);
	    rec = skel.receivePort();
	} else {
	    throw new ExportException("object already exported");
	}

	//create a stub
	// Use the classloader of the original class!
	// Fix is by Fabrice Huet.
	ClassLoader loader = obj.getClass().getClassLoader();

	Class stub_c = null;
	if (loader != null) {
	    stub_c = loader.loadClass(get_stub_name(c));
	}
	else {
	    stub_c = Class.forName(get_stub_name(c));
	}
	stub = (Stub) stub_c.newInstance();

	stub.init(null, null, 0, rec.identifier(), false);

	if (DEBUG) {
	    System.out.println(hostname + ": Created stub of type rmi_stub_" + classname);
	}

	stubs.put(obj, stub);

	return (RemoteStub) stub;
    }

    public static synchronized Object getStub(Object o) {
	return stubs.get(o);
    }



    public static synchronized void bind(String url, Remote o)
	throws AlreadyBoundException, IbisException, IOException, InstantiationException, IllegalAccessException
    {
	//	String url = "//" + RTS.hostname + "/" + name;

	if (DEBUG) {
	    System.out.println(hostname + ": Trying to bind object to " + url);
	}

	ReceivePortIdentifier dest = null;

	try {
	    dest = ibisRegistry.lookup(url, 1);
	} catch(IOException e) {
	}

	if (dest != null) {
	    throw new AlreadyBoundException(url + " already bound");
	}

	Skeleton skel = (Skeleton) skeletons.get(o);
	if (skel == null) {
	    //		    throw new RemoteException("object not exported");
	    //or just export it???

	    skel = createSkel(o);
	}

	//new method
	ibisRegistry.bind(url, skel.receivePort().identifier());

	if (DEBUG) {
	    System.out.println(hostname + ": Bound to object " + url);
	}

    }

    public static synchronized void rebind(String url, Remote o)
	throws IbisException, IOException, InstantiationException, IllegalAccessException
    {
	if (DEBUG) {
	    System.out.println(hostname + ": Trying to rebind object to " + url);
	}

	Skeleton skel = (Skeleton) skeletons.get(o);
	if (skel == null) {
	    //		    throw new RemoteException("object not exported");
	    //or just export it???
	    skel = createSkel(o);
	}

	//new method
	ibisRegistry.rebind(url, skel.receivePort().identifier());
    }

    public static void unbind(String url)
	throws NotBoundException, ClassNotFoundException, IOException
    {
	if (DEBUG) {
	    System.out.println(hostname + ": Trying to unbind object from " + url);
	}

	ReceivePortIdentifier dest = null;

	try {
	    dest = ibisRegistry.lookup(url, 1);
	} catch (IOException e) {
	}

	if (dest == null) {
	    throw new NotBoundException(url + " not bound");
	}

	//new method
	ibisRegistry.unbind(url);
    }


    public static Remote lookup(String url)  throws NotBoundException, IOException {

	Stub result;
	SendPort s = null;

	if (DEBUG) {
	    System.out.println(hostname + ": Trying to lookup object " + url);
	}

	ReceivePortIdentifier dest = null;

	synchronized(RTS.class) {
	    try {
		dest = ibisRegistry.lookup(url, 1);
		// System.err.println("ibisRegistry.lookup(" + url + ". 1) is " + dest);
	    } catch(IOException e) {
		// System.err.println("ibisRegistry.lookup(" + url + ". 1) throws " + e);
	    }
	}

	if (dest == null) {
	    throw new NotBoundException(url + " not bound");
	}

	if (DEBUG) {
	    System.out.println(hostname + ": Found skeleton " + url + " connecting");
	}

	s = getSendPort(dest);

	if (DEBUG) {
	    System.out.println(hostname + ": Got sendport");
	}

	ReceivePort r = getReceivePort();

	if (DEBUG) {
	    System.out.println(hostname + ": Created receiveport for stub  -> id = " + r.identifier());
	}

	WriteMessage wm = s.newMessage();

	if (DEBUG) {
	    System.out.println(hostname + ": Created new WriteMessage");
	}

	wm.writeInt(-1);
	wm.writeInt(0);
	wm.writeObject(r.identifier());
	wm.send();
	wm.finish();

	if (DEBUG) {
	    System.out.println(hostname + ": Sent new WriteMessage");
	}

	ReadMessage rm = r.receive();

	if (DEBUG) {
	    System.out.println(hostname + ": Received readMessage");
	}

	int stubID = rm.readInt();
	String stubType;
	try {
	    stubType = (String) rm.readObject();
	} catch (ClassNotFoundException e) {
	    throw new RemoteException("Unmarshall error", e);
	}
	rm.finish();

	if (DEBUG) {
	    System.out.println(hostname + ": read typename " + stubType);
	}

	try {
	    result = (Stub) Class.forName(stubType).newInstance();
	} catch (Exception e) {
	    // The loading of the class has failed.
	    // maybe Ibis was loaded using the primordial classloader
	    // and the needed class was not.
	    // Fix is by Fabrice Huet.
	    try {
		result = (Stub) Thread.currentThread().getContextClassLoader()
				.loadClass(stubType).newInstance();
	    } catch(Exception e2) {
		// s.free();
		// r.forcedClose();
		throw new RemoteException("stub class " + stubType + " not found", e2);
	    }
	}

	if (DEBUG) {
	    System.out.println(hostname + ": Loaded class " + stubType);
	}

	result.init(s, r, stubID, dest, true);

	if (DEBUG) {
	    System.out.println(hostname + ": Found object " + url);
	}
	return (Remote) result;
    }

    public static String[] list(String url) throws IOException
    {
	int urlLength = url.length();
	String[] names = ibisRegistry.list(url /*+ ".*"*/);
	for (int i=0; i<names.length; i++) {
	    names[i] = names[i].substring(urlLength);
	}
	return names;
    }

    public static SendPort createSendPort()
	throws IOException
    {
	return portType.createSendPort(new RMIReplacer());
    }

    public static synchronized SendPort getSendPort(ReceivePortIdentifier rpi)
	    throws IOException {
	SendPort s = (SendPort) sendports.get(rpi);
	if (s == null) {
	    s = createSendPort();
	    s.connect(rpi);
	    sendports.put(rpi, s);
	    if (DEBUG) {
		System.out.println(hostname + ": New sendport for receiport: " + rpi);
	    }
	}
	else if (DEBUG) {
	    System.out.println(hostname + ": Reuse sendport for receiport: " + rpi);
	}
	return s;
    }

    public static synchronized ReceivePort getReceivePort()
	throws IOException
    {
	ReceivePort r;
	int len = receiveports.size();

	if (len > 0) {
	    r = (ReceivePort) receiveports.remove(len-1);
	    if (DEBUG) {
		System.out.println(hostname + ": Reuse receiveport: " + r.identifier());
	    }
	}
	else {
	    r = portType.createReceivePort("//" + hostname + "/rmi_stub" + (new java.rmi.server.UID()).toString());
	    if (DEBUG) {
		System.out.println(hostname + ": New receiveport: " + r.identifier());
	    }
	}
	r.enableConnections();
	return r;
    }

    public static synchronized void putReceivePort(ReceivePort r) {
	r.disableConnections();
	if (DEBUG) {
	    System.out.println(hostname + ": receiveport returned");
	}
	receiveports.add(r);
    }

    public static void createRegistry(int port) throws RemoteException
    {
	String url = "registry://" + hostname + ":" + port;
	try {
	    portType.createReceivePort(url);
	} catch (IOException e) {
	    throw new RemoteException("there already is a registry running on port " + port);
	}
    }


    public static String getHostname() {
	return hostname;
    }

    public static void setClientHost(String s) {
	clientHost.set(s);
    }

    public static String getClientHost() {
	Object o = clientHost.get();
	if (o == null) return "UNKNOWN_HOST";
	String s = (String) o;
	return s;
    }
}
