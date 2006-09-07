/* $Id$ */

package ibis.ipl;

import ibis.util.ClassLister;
import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * This class defines the Ibis API, which can be implemented by an Ibis
 * implementation. Every JVM may run multiple Ibis implementations.
 * The user can request a list of available implementations, query their
 * properties, and then load the desired Ibis implementation at runtime.
 * An Ibis implementation offers certain PortType properties.
 *
 * During initialization, this class determines which Ibis implementations
 * are available. It does so, by finding all jar files in either the
 * class path, or all jar files in the directories indicated by the
 * ibis.impl.path property.
 * All Ibis implementations should be mentioned in the main
 * attributes of the manifest of the jar file containing it, in the
 * "Ibis-Implementation" entry. This entry should contain a
 * comma- or space-separated list of class names, where each class named
 * provides an Ibis implementation. In addition, a jar-entry named
 * "properties" should be present in the package of this Ibis implementation,
 * and describe the specific properties of this Ibis implementation.
 */

public abstract class Ibis {

    private static final String ldpath = "ibis.library.path";

    private static final String implpath = "ibis.impl.path";

    private static final String[] sysprops = { ldpath, implpath };

    private static final String[] excludes = { "ibis.util.", "ibis.connect.",
            "ibis.pool.", "ibis.io.", "ibis.net.", "ibis.mp.", "ibis.nio.",
            "ibis.tcp.", "ibis.name_server.", "ibis.name", "ibis.verbose",
            "ibis.communication", "ibis.serialization", "ibis.worldmodel" };

    private static final String implPathValue
        = TypedProperties.stringProperty(implpath);

    /** A name for this Ibis. */
    protected String name;

    /** The implementation name, for instance ibis.impl.tcp.TcpIbis. */
    protected String implName;

    /** A user-supplied resize handler, with join/leave upcalls. */
    protected ResizeHandler resizeHandler;

    /**
     * Properties, as given to
     * {@link #createIbis(StaticProperties, ResizeHandler)}
     */
    private StaticProperties requiredprops;

    /** User properties, combined with required properties. */
    protected StaticProperties combinedprops;

    /** A list of available ibis implementations. */
    private static Class[] implList;

    /** Properties of available ibis implementations. */
    private static StaticProperties[] implProperties;

    /** The currently loaded Ibises. */
    private static ArrayList loadedIbises = new ArrayList();

    static {
        // Check properties
        TypedProperties.checkProperties("ibis.", sysprops, excludes);

        // Obtain a list of Ibis implementations
        ClassLister clstr = ClassLister.getClassLister(implPathValue);
        List compnts = clstr.getClassList("Ibis-Implementation", Ibis.class);
        implList = (Class[]) compnts.toArray(new Class[0]);
        implProperties = new StaticProperties[implList.length];
        for (int i = 0; i < implProperties.length; i++) {
            try {
                addIbis(i);
            } catch(IOException e) {
                System.err.println("Error while reading properties of "
                        + implList[i].getName() + ": " + e);
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /** Don't allow public creation. */
    protected Ibis() {
    	// nothing here
    }

    /** 
     * Loads a native library with ibis.
     * It might not be possible to load libraries the normal way,
     * because Ibis applications might override the bootclasspath
     * when the classlibraries have been rewritten.
     * In that case, the classloader will use the sun.boot.library.path 
     * which is not portable.
     *
     * @param name the name of the library to be loaded.
     * @exception SecurityException may be thrown by loadLibrary.
     * @exception UnsatisfiedLinkError may be thrown by loadLibrary.
     */
    public static void loadLibrary(String name) throws SecurityException,
            UnsatisfiedLinkError {
        Properties p = System.getProperties();
        String libPath = p.getProperty(ldpath);
        String sep = p.getProperty("file.separator");

        if (libPath != null) {
            String s = System.mapLibraryName(name);

            // System.err.println("LOADING IBIS LIB: " + libPath + sep + s);

            System.load(libPath + sep + s);
            return;
        }

        // Fall back to regular loading.
        // This might not work, or it might not :-)
        // System.err.println("LOADING NON IBIS LIB: " + name);

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
     *  may be null to indicate that resize notifications are not wanted.
     * @return the new Ibis instance.
     *
     * @exception ibis.ipl.IbisException two Ibis instances with the same
     *  implName are created, or any IbisException the implementation
     *  throws at its initialization
     * @exception IllegalArgumentException name or implName are null, or
     *  do not correspond to an existing Ibis implementation
     * @exception ConnectionRefusedException is thrown when the name turns
     *  out to be not unique.
     * @deprecated The prefered method for creating Ibis instances is
     *   {@link #createIbis(ibis.ipl.StaticProperties, ibis.ipl.ResizeHandler)}.
     */
    public static Ibis createIbis(String name, String implName,
            ResizeHandler resizeHandler) throws IbisException,
            ConnectionRefusedException {

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

        return createIbis(name, c, null, null, resizeHandler);
    }

    private static Ibis createIbis(String name, Class c,
            StaticProperties prop, StaticProperties reqprop,
            ResizeHandler resizeHandler) throws IbisException,
            ConnectionRefusedException {

        Ibis impl;

        try {
            impl = (Ibis) c.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Could not initialize Ibis" + e);
        } catch (IllegalAccessException e2) {
            throw new IllegalArgumentException("Could not initialize Ibis"
                                               + e2);
        }

        try {
            loadLibrary("uninitialized_object");
        } catch (Throwable t) {
            /* handled elsewhere */
        }

        impl.name = name;
        impl.implName = c.getName();
        impl.resizeHandler = resizeHandler;
        impl.requiredprops = reqprop;
        impl.combinedprops = prop;
        if (reqprop == null) {
            impl.requiredprops = impl.properties();
        } else if (reqprop.isProp("serialization", "object")) {
            /*
             * required properties had "object", but if we later
             * ask for "sun" or "ibis", these may not be in the
             * required properties, so put the original serialization
             * specs back.
             */
            impl.requiredprops = new StaticProperties(reqprop);
            impl.requiredprops.add("serialization",
                    impl.properties().find("serialization"));
        }
        if (impl.combinedprops == null) {
            impl.combinedprops = impl.requiredprops.combineWithUserProps();
        }

        try {
            impl.init();
        } catch (ConnectionRefusedException e) {
            throw e;
        } catch (IOException e3) {
            throw new IbisException("Could not initialize Ibis", e3);
        }

        //System.err.println("Create Ibis " + impl);

        synchronized (Ibis.class) {
            loadedIbises.add(impl);
        }
        return impl;
    }

    /**
     * Returns a list of all Ibis implementations that are currently loaded.
     * When no Ibises are loaded, this method returns an array with no
     * elements.
     * @return the list of loaded Ibis implementations.
     */
    public static synchronized Ibis[] loadedIbises() {
        Ibis[] res = new Ibis[loadedIbises.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (Ibis) loadedIbises.get(i);
        }

        return res;
    }

    /**
     * Creates a new Ibis instance, based on the required properties,
     * or on the system property "ibis.name",
     * or on the staticproperty "name".
     * If the system property "ibis.name" is set, the corresponding
     * Ibis implementation is chosen.
     * Else, if the staticproperty "name" is set in the specified
     * required properties, the corresponding Ibis implementation is chosen.
     * Else, an Ibis implementation is chosen that matches the
     * required properties.
     *
     * The currently recognized Ibis names are:
     * <br>
     * panda	Ibis built on top of Panda.
     * <br>
     * tcp	Ibis built on top of TCP (the current default).
     * <br>
     * nio	Ibis built on top of Java NIO.
     * <br>
     * mpi	Ibis built on top of MPI.
     * <br>
     * net.*	The future version, for tcp, udp, GM.
     * <br>
     * @param reqprop static properties required by the application,
     *  or <code>null</code>.
     * @param  r a {@link ibis.ipl.ResizeHandler ResizeHandler} instance
     *  if upcalls for joining or leaving ibis instances are required,
     *  or <code>null</code>.
     * @return the new Ibis instance.
     *
     * @exception NoMatchingIbisException is thrown when no Ibis was
     *  found that matches the properties required.
     * @exception IbisException is thrown when no Ibis could be
     *  instantiated.
     */
    public static Ibis createIbis(StaticProperties reqprop, ResizeHandler r)
            throws IbisException {
        String hostname;

        try {
            hostname = IPUtils.getLocalHostAddress().getHostName();
        } catch (Exception e) {
            hostname = "unknown";
        }

        StaticProperties combinedprops;

        if (reqprop == null) {
            combinedprops = (new StaticProperties()).combineWithUserProps();
        } else {
            combinedprops = reqprop.combineWithUserProps();
        }

        if (combinedprops.find("verbose") != null) {
            System.out.println("Looking for an Ibis with properties: ");
            System.out.println("" + combinedprops);
        }

        String ibisname = combinedprops.find("name");

        ArrayList implementations = new ArrayList();

        if (ibisname == null) {
            NestedException nested = new NestedException(
                    "Could not find a matching Ibis");
            for (int i = 0; i < implProperties.length; i++) {
                StaticProperties ibissp = implProperties[i];
                Class cl = implList[i];
                // System.out.println("try " + cl.getName());
                if (combinedprops.matchProperties(ibissp)) {
                    // System.out.println("match!");
                    implementations.add(cl);
                }
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(ibissp);
                nested.add(cl.getName(),
                        new IbisException("Unmatched properties: "
                            + clashes.toString()));
            }
            if (implementations.size() == 0) {
                // System.err.println("Properties:");
                // System.err.println(combinedprops.toString());
                throw new NoMatchingIbisException(nested);
            }
        } else {
            StaticProperties ibissp = null;
            Class cl = null;
            boolean found = false;
            for (int i = 0; i < implProperties.length; i++) {
                ibissp = implProperties[i];
                cl = implList[i];

                String name = ibisname;
                if (name.startsWith("net")) {
                    name = "net";
                }
                String n = ibissp.getProperty("nickname");
                if (n == null) {
                    n = cl.getName().toLowerCase();
                }

                if (name.equals(n) || name.equals(cl.getName().toLowerCase())) {
                    found = true;
                    implementations.add(cl);
                    break;
                }
            }

            if (! found) {
                throw new IbisException("Nickname " + ibisname + " not matched");
            }

            if (!combinedprops.matchProperties(ibissp)) {
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(ibissp);
                System.err.println("WARNING: the " + ibisname
                       + " version of Ibis does not match the required "
                       + "properties.\nThe unsupported properties are:\n"
                       + clashes.toString()
                       + "This Ibis version was explicitly requested, "
                       + "so the run continues ...");
            }
            if (ibisname.startsWith("net")) {
                ibissp.add("IbisName", ibisname);
            }
        }

        int n = implementations.size();

        if (combinedprops.find("verbose") != null) {
            System.out.print("Matching Ibis implementations:");
            for (int i = 0; i < n; i++) {
                Class cl = (Class) implementations.get(i);
                System.out.print(" " + cl.getName());
            }
            System.out.println();
        }

        NestedException nested = new NestedException("Ibis creation failed");
        
        for (int i = 0; i < n; i++) {
            Class cl = (Class) implementations.get(i);
            if (combinedprops.find("verbose") != null) {
                System.out.println("trying " + cl.getName());
            }
            while (true) {
                try {
                    String name = "ibis@" + hostname + "_"
                            + System.currentTimeMillis();
                    return createIbis(name, cl, combinedprops, reqprop, r);
                } catch (ConnectionRefusedException e) {
                    // retry
                } catch (IbisException e) {
                	nested.add(cl.getName(), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }

                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + cl.getName() + ", got exception:" + e);
                        e.printStackTrace();
                    }
                    break;
                } catch (RuntimeException e) {
                	nested.add(cl.getName(), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }
                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + cl.getName() + ", got exception:" + e);
                        e.printStackTrace();
                    }
                    break;
                } catch (Error e) {
                	nested.add(cl.getName(), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }
                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + cl.getName() + ", got exception:" + e);
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }
        throw nested;
    }

    private static void addIbis(int index) throws IOException {
        Class cl = implList[index];
        String packagename = cl.getPackage().getName();
        // Note: getResourceAsStream wants '/', not File.separatorChar!
        String propertyFile = packagename.replace('.', '/')
                    + "/" + "properties";
        StaticProperties sp = new StaticProperties();
        InputStream in = cl.getClassLoader().getResourceAsStream(propertyFile);
        if (in == null) {
            throw new IOException("Could not open " + propertyFile);
        }
        sp.load(in);
        in.close();

        sp.addImpliedProperties();

        implProperties[index] = sp;
    }

    /**
     * Returns a list of available Ibis implementation names for this system.
     * @return the list of available Ibis implementations.
     */
    public static synchronized String[] list() {
        String[] res = new String[implList.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = implList[i].getName();
        }

        return res;
    }

    /**
     * Returns the static properties for a certain implementation.
     * @param implName implementation name of an Ibis for which
     * properties are requested.
     * @return the static properties for a given implementation,
     *  or <code>null</code> if not present.
     */
    public static synchronized StaticProperties staticProperties(
            String implName) {
        for (int i = 0; i < implList.length; i++) {
            if (implList[i].getName().equals(implName)) {
                return implProperties[i];
            }
        }
        return null;
    }

    /**
     * When running closed-world, returns the total number of Ibis instances
     * involved in the run.
     * @return the number of Ibis instances
     * @exception IbisError is thrown when running open-world.
     * @exception NumberFormatException is thrown when the property
     *   ibis.pool.total_hosts is not defined or does not represent a number.
     */
    public int totalNrOfIbisesInPool() {
        if (combinedprops.isProp("worldmodel", "closed")) {
            return TypedProperties.intProperty("ibis.pool.total_hosts");
        }
        throw new IbisError("totalNrOfIbisesInPool() called but open world");
    }

    /**
     * Allows reception of {@link ibis.ipl.ResizeHandler ResizeHandler}
     * upcalls.
     * If a {@link ibis.ipl.ResizeHandler ResizeHandler} is installed,
     * this call blocks until its
     * {@link ibis.ipl.ResizeHandler#joined(IbisIdentifier) joined()}
     * upcall for this Ibis is invoked.
     */
    public abstract void enableResizeUpcalls();

    /**
     * Disables reception of
     * {@link ibis.ipl.ResizeHandler ResizeHandler} upcalls.
     */
    public abstract void disableResizeUpcalls();

    /**
     * Returns all Ibis recources to the system.
     * @exception IOException is thrown when an error occurs.
     */
    public abstract void end() throws IOException;

    /**
     * Creates a {@link ibis.ipl.PortType PortType}.
     * A name is given to the <code>PortType</code> (e.g. "satin porttype"
     * or "RMI porttype"), and Port properties are specified (for example
     * ports are "totally-ordered" and "reliable" and support "NWS").
     * If no static properties are given, the properties that were
     * requested from the Ibis implementation are used, possibly combined
     * with properties specified by the user (using the
     * -Dibis.&#60category&#62="..." mechanism).
     * If static properties <strong>are</strong> given,
     * the default properties described above are used for categories 
     * not specifiedby the given properties.
     * <p>
     * The name and properties <strong>together</strong> define the
     * <code>PortType</code>.
     * If two Ibis instances want to communicate, they must both
     * create a <code>PortType</code> with the same name and properties.
     * If multiple implementations try to create a <code>PortType</code>
     * with the same name but different properties, an IbisException will
     * be thrown.
     * A <code>PortType</code> can be used to create
     * {@link ibis.ipl.ReceivePort ReceivePorts} and
     * {@link ibis.ipl.SendPort SendPorts}.
     * Only <code>ReceivePort</code>s and <code>SendPort</code>s of
     * the same <code>PortType</code> can communicate.
     * Any number of <code>ReceivePort</code>s and <code>SendPort</code>s
     * can be created on a JVM (even of the same <code>PortType</code>).
     * </p>
     * @param nm name of the porttype.
     * @param p properties of the porttype.
     * @return the porttype.
     * @exception IbisException is thrown when Ibis configuration,
     *  name or p are misconfigured
     * @exception IOException may be thrown for instance when communication
     *  with a nameserver fails.
     */
    public PortType createPortType(String nm, StaticProperties p)
            throws IOException, IbisException {
        if (p == null) {
            p = combinedprops;
        } else {
            /*
             * The properties given as parameter have preference.
             * It is not clear to me if the user properties should have
             * preference here. The user could say that he wants Ibis
             * serialization, but the parameter could say: sun serialization.
             * On the other hand, the parameter could just say: object
             * serialization, in which case the user specification is
             * more specific.
             * The {@link StaticProperties#combine} method should deal
             * with that.
             */
            p = new StaticProperties(combinedprops.combine(p));
            p.add("worldmodel", ""); // not significant for port type,
            // and may conflict with the ibis prop.
            checkPortProperties(p);
        }
        if (nm == null) {
            throw new IbisException("anonymous name for port type not allowed");
        }
        if (combinedprops.find("verbose") != null) {
            System.out.println("Creating port type " + nm
                    + " with properties\n" + p);
        }
        if (p.isProp("communication", "manytoone") &&
                p.isProp("communication", "onetomany")) {
            System.err.println("WARNING: combining ManyToOne and OneToMany in "
                    + "a port type may result in\ndeadlocks! Most systems "
                    + "don't have a working flow control when multiple\n"
                    + "senders do multicasts.");
        }
        return newPortType(nm, p);
    }

    /**
     * See {@link ibis.ipl.Ibis#createPortType(String, StaticProperties)}.
     */
    protected abstract PortType newPortType(String nm, StaticProperties p)
            throws IOException, IbisException;

    /**
     * This method is used to check if the properties for a PortType
     * match the properties of this Ibis.
     * @param p the properties for the PortType.
     * @exception IbisException is thrown when this Ibis cannot provide
     * the properties requested for the PortType.
     */
    private void checkPortProperties(StaticProperties p) throws IbisException {
        if (!p.matchProperties(requiredprops)) {
            System.err.println("Ibis required properties: " + requiredprops);
            System.err.println("Port required properties: " + p);
            throw new IbisException(
                    "Port properties don't match the Ibis required properties");
        }
    }

    /**
     * Returns the {@link ibis.ipl.PortType PortType} corresponding to
     * the given name.
     * @param nm the name of the requested port type.
     * @return a reference to the port type, or <code>null</code>
     * if the given name is not the name of a valid port type.
     */
    public abstract PortType getPortType(String nm);

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
     * Returns the name of this Ibis instance. This is a shorthand for
     * <code>identifier().name()</code> (See {@link IbisIdentifier#name()}).
     * @return the name of this Ibis instance.
     */
    public String name() {
        return identifier().name;
    }

    /**
     * Returns the implementation name of this Ibis instance.
     * @return the implementation name of this Ibis instance.
     */
    public String implementationName() {
        return implName;
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
     * Returns the current Ibis version.
     * @return the ibis version.
     */
    public String getVersion() {
        InputStream in
            = ClassLoader.getSystemClassLoader().getResourceAsStream("VERSION");
        String version = "Unknown Ibis Version ID";
        if (in != null) {
            byte[] b = new byte[512];
            int l = 0;
            try {
                l = in.read(b);
            } catch (Exception e) {
                // Ignored
            }
            if (l > 0) {
                version = "Ibis Version ID " + new String(b, 0, l);
            }
        }
        return version + ", implementation = " + implName;
    }

    /**
     * Notifies this Ibis instance that another Ibis instance has
     * joined the run.
     * <strong>
     * Note: used by the nameserver, do not call from outside Ibis.
     * </strong>
     * @param joinIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier
     * identifier} of the Ibis instance joining the run.
     */
    public abstract void joined(IbisIdentifier joinIdent);

    /**
     * Notifies this Ibis instance that another Ibis instance has
     * left the run.
     * <strong>
     * Note: used by the nameserver, do not call from outside Ibis.
     * </strong>
     * @param leaveIdent the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifier} of the Ibis instance leaving the run.
     */
    public abstract void left(IbisIdentifier leaveIdent);

    /**
     * Notifies this Ibis instance that another Ibis instance has died.
     * <strong>
     * Note: used by the nameserver, do not call from outside Ibis.
     * </strong>
     * @param corpses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that died.
     */
    public abstract void died(IbisIdentifier[] corpses);

    /**
     * Notifies this Ibis instance that some Ibis instances are requested
     * to leave.
     * <strong>
     * Note: used by the nameserver, do not call from outside Ibis.
     * </strong>
     * @param ibisses the Ibis {@linkplain ibis.ipl.IbisIdentifier
     *  identifiers} of the Ibis instances that are requested to leave.
     */
    public abstract void mustLeave(IbisIdentifier[] ibisses);
}
