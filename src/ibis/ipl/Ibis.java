/* $Id$ */

package ibis.ipl;

import ibis.util.IPUtils;
import ibis.util.TypedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * This class defines the Ibis API, which can be implemented by an Ibis
 * implementation. Every JVM may run multiple Ibis implementations.
 * The user can request a list of available implementations, query their
 * properties, and then load the desired Ibis implementation at runtime.
 * An Ibis implementation offers certain PortType properties.
 *
 * On startup, Ibis tries to load properties files in the following order:
 * <br>
 * - ibis.property.file;
 * <br>
 * - a file named "properties" in the classpath.
 * <br>
 * - current_dir/ibis_properties;
 * <br>
 * - home_dir/ibis_properties.
 * <br>
 */

public abstract class Ibis {

    private static final String ldpath = "ibis.library.path";

    private static final String propfile = "ibis.property.file";

    private static final String[] sysprops = { ldpath, propfile };

    private static final String[] excludes = { "ibis.util.", "ibis.connect.",
            "ibis.pool.", "ibis.io.", "ibis.net.", "ibis.mp.", "ibis.nio.",
            "ibis.tcp.", "ibis.name_server.", "ibis.name", "ibis.verbose",
            "ibis.communication", "ibis.serialization", "ibis.worldmodel" };

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
    private static ArrayList implList;

    /** A list of nicknames for available ibis implementations. */
    private static ArrayList nicknameList;

    /** Properties of available ibis implementations. */
    private static ArrayList implProperties; /* StaticProperties list */

    /** The currently loaded Ibises. */
    private static ArrayList loadedIbises = new ArrayList();

    /** The default Ibis nickname. */
    private static String defaultIbisNickname;

    /** The default Ibis classname. */
    private static String defaultIbisName;

    static {
        TypedProperties.checkProperties("ibis.", sysprops, excludes);
        try {
            readGlobalProperties();
        } catch (IOException e) {
            System.err.println("exception in readGlobalProperties: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }

    /** Don't allow public creation. */
    protected Ibis() {
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
        return createIbis(name, implName, null, null, resizeHandler);
    }

    private static Ibis createIbis(String name, String implName,
            StaticProperties prop, StaticProperties reqprop,
            ResizeHandler resizeHandler) throws IbisException,
            ConnectionRefusedException {
        Ibis impl;

        try {
            loadLibrary("uninitialized_object");
        } catch (Throwable t) {
            /* handled elsewhere */
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
            throw new IllegalArgumentException("Could not initialize Ibis"
                                               + e2);
        }
        impl.name = name;
        impl.implName = implName;
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

        if (ibisname == null && reqprop == null) {
            // default Ibis
            ibisname = defaultIbisNickname;
        }

        String[] impls = list();

        ArrayList implementation_names = new ArrayList();


        if (ibisname == null) {
            NestedException nested = new NestedException(
                    "Could not find a matching Ibis");
            for (int i = 0; i < impls.length; i++) {
                StaticProperties ibissp = staticProperties(impls[i]);
                // System.out.println("try " + impls[i]);
                if (combinedprops.matchProperties(ibissp)) {
                    // System.out.println("match!");
                    implementation_names.add(impls[i]);
                }
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(ibissp);
                nested.add(impls[i],
                        new IbisException("Unmatched properties: "
                            + clashes.toString()));
            }
            if (implementation_names.size() == 0) {
                // System.err.println("Properties:");
                // System.err.println(combinedprops.toString());
                throw new NoMatchingIbisException(nested);
            }
        } else {
            String[] nicks = nicknames();
            String name = ibisname;
            if (name.startsWith("net")) {
                name = "net";
            }
            for (int i = 0; i < nicks.length; i++) {
                if (name.equals(nicks[i])) {
                    implementation_names.add(impls[i]);
                    break;
                }
            }

            if (implementation_names.size() == 0) {
                name = System.getProperty("ibis.name");
                if (name != null) {
                    try {
                        String n = addIbis(name, name, null);
                        if (n != null) {
                            // Unknown Ibis, but there is one.
                            implementation_names.add(n);
                        }
                    } catch(IOException e) {
                        // ignored
                    }
                }
            }

            if (implementation_names.size() == 0) {
                System.err.println("Warning: name '" + ibisname
                        + "' not recognized, using " + defaultIbisName);
                implementation_names.add(defaultIbisName);
                ibisname = defaultIbisName;
            }
            StaticProperties sp = staticProperties(
                    (String) implementation_names.get(0));
            if (!combinedprops.matchProperties(sp)) {
                StaticProperties clashes
                        = combinedprops.unmatchedProperties(sp);
                System.err.println("WARNING: the " + ibisname
                       + " version of Ibis does not match the required "
                       + "properties.\nThe unsupported properties are:\n"
                       + clashes.toString()
                       + "This Ibis version was explicitly requested, "
                       + "so the run continues ...");
            }
            if (ibisname.startsWith("net")) {
                sp.add("IbisName", ibisname);
            }
        }

        int n = implementation_names.size();

        if (combinedprops.find("verbose") != null) {
            System.out.println("Matching Ibis implementations:");
            for (int i = 0; i < n; i++) {
                System.out.println((String) implementation_names.get(i));
            }
            System.out.println();
        }

        NestedException nested = new NestedException("Ibis creation failed");
        
        for (int i = 0; i < n; i++) {
            if (combinedprops.find("verbose") != null) {
                System.out.println("trying "
                        + (String) implementation_names.get(i));
            }
            while (true) {
                try {
                    String name = "ibis@" + hostname + "_"
                            + System.currentTimeMillis();
                    return createIbis(name,
                            (String) implementation_names.get(i), combinedprops,
                            reqprop, r);
                } catch (ConnectionRefusedException e) {
                    // retry
                } catch (IbisException e) {
                	nested.add((String) implementation_names.get(i), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }

                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + (String) implementation_names.get(i)
                                + ", got exception:" + e);
                        e.printStackTrace();
                        System.err.println("Trying "
                                + (String) implementation_names.get(i + 1));
                    }
                    break;
                } catch (RuntimeException e) {
                	nested.add((String) implementation_names.get(i), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }
                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + (String) implementation_names.get(i)
                                + ", got exception:" + e);
                        e.printStackTrace();
                        System.err.println("Trying "
                                + (String) implementation_names.get(i + 1));
                    }
                    break;
                } catch (Error e) {
                	nested.add((String) implementation_names.get(i), e);
                    if (i == n - 1) {
                        // No more Ibis to try.
                        throw nested;
                    }
                    if (combinedprops.find("verbose") != null) {
                        System.err.println("Warning: could not create "
                                + (String) implementation_names.get(i)
                                + ", got exception:" + e);
                        e.printStackTrace();
                        System.err.println("Trying "
                                + (String) implementation_names.get(i + 1));
                    }
                    break;
                }
            }
        }
        throw nested;
    }

    /**
     * Reads the properties of an ibis implementation.
     */
    private static void addIbisNick(String nickname, Properties p)
            throws IOException {
        String name = p.getProperty(nickname);
        if (name == null) {
            throw new IOException("no implementation given for nickname "
                    + nickname);
        }

        addIbis(nickname, name, p.getProperty(name));
    }

    private static String addIbis(String nickname, String name,
            String propertyFiles) throws IOException  {
        Class cl = null;

        try {
            // See if this Ibis actually exists.
            cl = Class.forName(name, false, Ibis.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }

        if (nickname == null) {
            nickname = name;
        }

        if (propertyFiles == null) {
            String packagename = cl.getPackage().getName();
            propertyFiles = packagename.replace('.', File.separatorChar)
                    + File.separatorChar + "properties";
        }

        StaticProperties sp = new StaticProperties();
        StringTokenizer st = new StringTokenizer(propertyFiles,
                " ,\t\n\r\f");
        while (st.hasMoreTokens()) {
            String file = st.nextToken();
            InputStream in = ClassLoader.getSystemClassLoader()
                    .getResourceAsStream(file);
            if (in == null) {
                System.err.println("could not open " + file);
                System.exit(1);
            }
            sp.load(in);
            in.close();
        }

        sp.addImpliedProperties();

        synchronized (Ibis.class) {
            if (nickname.equals(defaultIbisNickname)) {
                defaultIbisName = name;
            }
            nicknameList.add(nickname);
            implList.add(name);
            implProperties.add(sp);
        }

        return nickname;
    }

    /**
     * Reads the properties of the ibis implementations available on the
     * current machine.
     * @exception IOException is thrown when a property file could not
     *  be opened, or the "names" property could not be found.
     */
    private static void readGlobalProperties() throws IOException {
        InputStream in = openProperties();

        implList = new ArrayList();
        nicknameList = new ArrayList();
        implProperties = new ArrayList();

        Properties p = new Properties();
        p.load(in);
        in.close();

        String order = p.getProperty("names");

        defaultIbisNickname = p.getProperty("default");

        if (defaultIbisNickname == null) {
            throw new IOException("Error in properties file: no default ibis!");
        }

        if (order != null) {
            StringTokenizer st = new StringTokenizer(order, " ,\t\n\r\f");
            while (st.hasMoreTokens()) {
                addIbisNick(st.nextToken(), p);
            }
            if (defaultIbisName == null) {
                throw new IOException(
                        "Error in properties file: could not find the "
                        + "default Ibis (" + defaultIbisNickname + ")");
            }
        } else {
            throw new IOException(
                    "Error in properties file: no property \"names\"");
        }
    }

    /**
     * Tries to find and open a property file.
     * The file is searched for as described below:
     * <br>
     * First, the system property ibis.property.file is tried.
     * <br>
     * Next, a file named properties is tried using the system classloader.
     * <br>
     * Next, current_dir/ibis_properties is tried, where current_dir indicates
     * the value of the system property user.dir.
     * <br>
     * Next, home_dir/ibis_properties is tried, where home_dir indicates
     * the value of the system property user.home.
     * <br>
     * If any of this fails, a message is printed, and an exception is thrown.
     * <br>
     * @return input stream from which properties can be read.
     * @exception IOException is thrown when a property file could not
     *  be opened.
     */
    private static InputStream openProperties() throws IOException {
        Properties p = System.getProperties();
        String s = p.getProperty(propfile);
        InputStream in;
        if (s != null) {
            try {
                return new FileInputStream(s);
            } catch (FileNotFoundException e) {
                System.err.println("" + propfile + " set, "
                        + "but could not read file " + s);
            }
        }

        in = ClassLoader.getSystemClassLoader().getResourceAsStream(
                "properties");
        if (in != null) {
            return in;
        }

        String sep = p.getProperty("file.separator");
        if (sep == null) {
            throw new IOException("Could not get file separator property");
        }

        /* try current dir */
        s = p.getProperty("user.dir");
        if (s != null) {
            s += sep + "ibis_properties";
            try {
                return new FileInputStream(s);
            } catch (FileNotFoundException e) {
                /* do nothing */
            }
        }

        /* try users home dir */
        s = p.getProperty("user.home");
        if (s != null) {
            s += sep + "ibis_properties";
            try {
                return new FileInputStream(s);
            } catch (FileNotFoundException e) {
                /* do nothing */
            }
        }

        throw new IOException("Could not find property file");
    }

    /**
     * Returns a list of available Ibis implementation names for this system.
     * @return the list of available Ibis implementations.
     */
    public static synchronized String[] list() {
        String[] res = new String[implList.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (String) implList.get(i);
        }

        return res;
    }

    /**
     * Returns a list of available Ibis nicknames for this system.
     * @return the list of available Ibis implementations.
     */
    private static synchronized String[] nicknames() {
        String[] res = new String[nicknameList.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (String) nicknameList.get(i);
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
        int index = implList.indexOf(implName);
        if (index < 0) {
            return null;
        }
        return (StaticProperties) implProperties.get(index);
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
